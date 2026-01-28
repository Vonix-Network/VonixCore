package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player economy - balances, transactions, and baltop.
 * Refactored for Caching and Async Persistence.
 */
public class EconomyManager {

    private static EconomyManager instance;
    private double startingBalance = 100.0;
    
    // Balance cache
    private final ConcurrentHashMap<UUID, Double> balanceCache = new ConcurrentHashMap<>();

    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }

    /**
     * Initialize economy table in database.
     */
    public void initializeTable(Connection conn) throws SQLException {
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_economy (
                        uuid TEXT PRIMARY KEY,
                        balance REAL NOT NULL DEFAULT 0
                    )
                """);
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_economy_balance ON vc_economy (balance DESC)");
    }

    /**
     * Get a player's balance. Uses cache if available.
     */
    public CompletableFuture<Double> getBalance(UUID uuid) {
        if (balanceCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(balanceCache.get(uuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT balance FROM vc_economy WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    balanceCache.put(uuid, balance);
                    return balance;
                } else {
                    // Create new account
                    try (PreparedStatement insertStmt = conn.prepareStatement(
                            "INSERT INTO vc_economy (uuid, balance) VALUES (?, ?)")) {
                        insertStmt.setString(1, uuid.toString());
                        insertStmt.setDouble(2, startingBalance);
                        insertStmt.executeUpdate();
                        balanceCache.put(uuid, startingBalance);
                        return startingBalance;
                    }
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to get/create balance for {}: {}", uuid, e.getMessage());
                balanceCache.put(uuid, 0.0);
                return 0.0;
            }
        });
    }

    /**
     * Preload balance asynchronously (e.g. on Join).
     */
    public void loadBalanceAsync(UUID uuid) {
        getBalance(uuid); // Now just call getBalance, it will load and cache.
    }
    
    /**
     * Unload balance (e.g. on Quit) to save memory.
     */
    public void unloadBalance(UUID uuid) {
        balanceCache.remove(uuid);
    }

    /**
     * Set a player's balance. Updates cache immediately, DB async.
     */
    public CompletableFuture<Void> setBalance(UUID uuid, double balance) {
        double safeBalance = Math.max(0, balance);
        balanceCache.put(uuid, safeBalance);
        
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO vc_economy (uuid, balance) VALUES (?, ?)");
                stmt.setString(1, uuid.toString());
                stmt.setDouble(2, safeBalance);
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to set balance for {}: {}", uuid, e.getMessage());
            }
        });
    }

    /**
     * Add to a player's balance.
     */
    public CompletableFuture<Boolean> deposit(UUID uuid, double amount) {
        if (amount <= 0)
            return CompletableFuture.completedFuture(false);

        return getBalance(uuid).thenCompose(current -> 
            setBalance(uuid, current + amount).thenApply(v -> true)
        );
    }

    /**
     * Subtract from a player's balance.
     */
    public CompletableFuture<Boolean> withdraw(UUID uuid, double amount) {
        if (amount <= 0)
            return CompletableFuture.completedFuture(false);

        return getBalance(uuid).thenCompose(current -> {
            if (current < amount) {
                return CompletableFuture.completedFuture(false);
            }
            return setBalance(uuid, current - amount).thenApply(v -> true);
        });
    }

    /**
     * Transfer money between players.
     */
    public CompletableFuture<Boolean> transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        return getBalance(from).thenCompose(fromBalance -> {
            if (fromBalance < amount) {
                return CompletableFuture.completedFuture(false);
            }

            return withdraw(from, amount).thenCompose(withdrew -> {
                if (withdrew) {
                    return deposit(to, amount).thenCompose(deposited -> {
                        if (deposited) {
                            return CompletableFuture.completedFuture(true);
                        } else {
                            // Rollback
                            return deposit(from, amount).thenApply(v -> false);
                        }
                    });
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            });
        });
    }

    /**
     * Check if player has enough money.
     */
    public CompletableFuture<Boolean> has(UUID uuid, double amount) {
        return getBalance(uuid).thenApply(balance -> balance >= amount);
    }

    /**
     * Get top balances. Always async-friendly return or cached?
     * Usually /baltop is infrequent. Sync DB is acceptable or Future.
     * Let's keep it sync for compatibility but recommend async call.
     */
    public List<BalanceEntry> getTopBalances(int limit) {
        List<BalanceEntry> top = new ArrayList<>();
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT uuid, balance FROM vc_economy ORDER BY balance DESC LIMIT ?");
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                top.add(new BalanceEntry(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getDouble("balance")));
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to get top balances: {}", e.getMessage());
        }
        return top;
    }

    /**
     * Format currency for display.
     */
    public String format(double amount) {
        return String.format("$%.2f", amount);
    }

    public void setStartingBalance(double balance) {
        this.startingBalance = balance;
    }

    /**
     * Balance entry for baltop.
     */
    public record BalanceEntry(UUID uuid, double balance) {
    }
}
