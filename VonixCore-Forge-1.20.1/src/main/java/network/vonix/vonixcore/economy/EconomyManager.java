package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages player economy - balances, transactions, and baltop.
 */
public class EconomyManager {

    private static EconomyManager instance;
    private double startingBalance = 100.0;
    private final java.util.concurrent.ConcurrentHashMap<UUID, Double> balanceCache = new java.util.concurrent.ConcurrentHashMap<>();

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
     * Load player balance asynchronously on join.
     */
    public void loadBalanceAsync(UUID uuid) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT balance FROM vc_economy WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    balanceCache.put(uuid, rs.getDouble("balance"));
                } else {
                    // New player, insert default balance
                    setBalance(uuid, startingBalance);
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to load balance for {}: {}", uuid, e.getMessage());
                // Fallback to starting balance in cache to prevent errors
                balanceCache.put(uuid, startingBalance);
            }
        });
    }

    /**
     * Unload player balance on quit.
     */
    public void unloadBalance(UUID uuid) {
        balanceCache.remove(uuid);
    }

    /**
     * Get a player's balance.
     */
    public java.util.concurrent.CompletableFuture<Double> getBalance(UUID uuid) {
        // Check cache first
        if (balanceCache.containsKey(uuid)) {
            return java.util.concurrent.CompletableFuture.completedFuture(balanceCache.get(uuid));
        }

        // Load asynchronously
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
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
                    // Create new account with starting balance
                    setBalance(uuid, startingBalance);
                    return startingBalance;
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to get balance: {}", e.getMessage());
                return 0.0;
            }
        });
    }

    /**
     * Set a player's balance.
     */
    public java.util.concurrent.CompletableFuture<Boolean> setBalance(UUID uuid, double balance) {
        double newBalance = Math.max(0, balance);
        
        // Update cache immediately
        balanceCache.put(uuid, newBalance);
        
        // Persist to DB asynchronously
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO vc_economy (uuid, balance) VALUES (?, ?)");
                stmt.setString(1, uuid.toString());
                stmt.setDouble(2, newBalance);
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to save balance for {}: {}", uuid, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Add to a player's balance.
     */
    public java.util.concurrent.CompletableFuture<Boolean> deposit(UUID uuid, double amount) {
        if (amount <= 0)
            return java.util.concurrent.CompletableFuture.completedFuture(false);
            
        return getBalance(uuid).thenCompose(current -> setBalance(uuid, current + amount));
    }

    /**
     * Subtract from a player's balance.
     */
    public java.util.concurrent.CompletableFuture<Boolean> withdraw(UUID uuid, double amount) {
        if (amount <= 0)
            return java.util.concurrent.CompletableFuture.completedFuture(false);
            
        return getBalance(uuid).thenCompose(current -> {
            if (current < amount)
                return java.util.concurrent.CompletableFuture.completedFuture(false);
            return setBalance(uuid, current - amount);
        });
    }

    /**
     * Transfer money between players.
     */
    public java.util.concurrent.CompletableFuture<Boolean> transfer(UUID from, UUID to, double amount) {
        if (amount <= 0)
            return java.util.concurrent.CompletableFuture.completedFuture(false);
            
        return getBalance(from).thenCompose(fromBalance -> {
            if (fromBalance < amount)
                return java.util.concurrent.CompletableFuture.completedFuture(false);
                
            return withdraw(from, amount).thenCompose(success -> {
                if (success) {
                    return deposit(to, amount).thenApply(depositSuccess -> {
                        if (!depositSuccess) {
                            // Rollback (best effort)
                            deposit(from, amount);
                            return false;
                        }
                        return true;
                    });
                }
                return java.util.concurrent.CompletableFuture.completedFuture(false);
            });
        });
    }

    /**
     * Check if player has enough money.
     */
    public java.util.concurrent.CompletableFuture<Boolean> has(UUID uuid, double amount) {
        return getBalance(uuid).thenApply(balance -> balance >= amount);
    }

    /**
     * Get top balances.
     */
    public java.util.concurrent.CompletableFuture<List<BalanceEntry>> getTopBalances(int limit) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
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
        });
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
