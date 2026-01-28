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
    public CompletableFuture<Double> getBalance(UUID uuid) {
        // Check cache first
        if (balanceCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(balanceCache.get(uuid));
        }

        // Async load
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
                    // Create new account with starting balance
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT OR REPLACE INTO vc_economy (uuid, balance) VALUES (?, ?)")) {
                        insert.setString(1, uuid.toString());
                        insert.setDouble(2, startingBalance);
                        insert.executeUpdate();
                    }
                    balanceCache.put(uuid, startingBalance);
                    return startingBalance;
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to get balance: {}", e.getMessage());
                return 0.0;
            }
        }, VonixCore::executeAsync);
    }

    /**
     * Set a player's balance.
     */
    public CompletableFuture<Boolean> setBalance(UUID uuid, double balance) {
        double newBalance = Math.max(0, balance);
        
        // Update cache immediately
        balanceCache.put(uuid, newBalance);
        
        // Persist to DB asynchronously
        return CompletableFuture.supplyAsync(() -> {
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
        }, VonixCore::executeAsync);
    }

    /**
     * Add to a player's balance.
     */
    public CompletableFuture<Boolean> deposit(UUID uuid, double amount) {
        if (amount <= 0)
            return CompletableFuture.completedFuture(false);
        
        return getBalance(uuid).thenCompose(current -> 
            setBalance(uuid, current + amount)
        );
    }

    /**
     * Subtract from a player's balance.
     */
    public CompletableFuture<Boolean> withdraw(UUID uuid, double amount) {
        if (amount <= 0)
            return CompletableFuture.completedFuture(false);
            
        return getBalance(uuid).thenCompose(current -> {
            if (current < amount)
                return CompletableFuture.completedFuture(false);
            return setBalance(uuid, current - amount);
        });
    }

    /**
     * Transfer money between players.
     */
    public CompletableFuture<Boolean> transfer(UUID from, UUID to, double amount) {
        if (amount <= 0)
            return CompletableFuture.completedFuture(false);
            
        return getBalance(from).thenCompose(fromBalance -> {
            if (fromBalance < amount)
                return CompletableFuture.completedFuture(false);
            
            // Withdraw from sender
            return withdraw(from, amount).thenCompose(withdrawn -> {
                if (!withdrawn)
                    return CompletableFuture.completedFuture(false);
                
                // Deposit to receiver
                return deposit(to, amount).thenApply(deposited -> {
                    if (deposited)
                        return true;
                    else {
                        // Rollback
                        deposit(from, amount);
                        return false;
                    }
                });
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
     * Get top balances.
     */
    public CompletableFuture<List<BalanceEntry>> getTopBalances(int limit) {
        return CompletableFuture.supplyAsync(() -> {
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
        }, VonixCore::executeAsync);
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
