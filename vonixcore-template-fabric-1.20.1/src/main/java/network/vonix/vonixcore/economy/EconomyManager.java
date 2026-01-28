package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;

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
 * Async + Caching Implementation
 */
public class EconomyManager {

    private static EconomyManager instance;
    private double startingBalance = 100.0;
    
    // Cache for online players (and recently offline)
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

        // Set starting balance from config
        startingBalance = EssentialsConfig.getInstance().getStartingBalance();
    }
    
    /**
     * Load player balance from DB to cache (Async).
     * Called on Join.
     */
    public void loadPlayerBalance(UUID uuid) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT balance FROM vc_economy WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    balanceCache.put(uuid, rs.getDouble("balance"));
                } else {
                    // New player
                    balanceCache.put(uuid, startingBalance);
                    savePlayerBalance(uuid, startingBalance);
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to load balance for {}: {}", uuid, e.getMessage());
                // Fallback
                balanceCache.put(uuid, startingBalance);
            }
        });
    }
    
    /**
     * Unload player from cache.
     * Called on Quit.
     */
    public void unloadPlayerBalance(UUID uuid) {
        balanceCache.remove(uuid);
    }

    /**
     * Get a player's balance.
     * Checks cache first, then DB.
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
                    double bal = rs.getDouble("balance");
                    balanceCache.put(uuid, bal);
                    return bal;
                } else {
                    // Initialize if not exists
                    savePlayerBalanceSync(uuid, startingBalance);
                    balanceCache.put(uuid, startingBalance);
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
    public CompletableFuture<Boolean> setBalance(UUID uuid, double balance) {
        double newBalance = Math.max(0, balance);
        balanceCache.put(uuid, newBalance);
        
        return CompletableFuture.supplyAsync(() -> {
            savePlayerBalanceSync(uuid, newBalance);
            return true;
        });
    }
    
    private void savePlayerBalance(UUID uuid, double balance) {
        VonixCore.executeAsync(() -> savePlayerBalanceSync(uuid, balance));
    }

    private void savePlayerBalanceSync(UUID uuid, double balance) {
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO vc_economy (uuid, balance) VALUES (?, ?)");
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, balance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to save balance for {}: {}", uuid, e.getMessage());
        }
    }

    /**
     * Add to a player's balance.
     */
    public CompletableFuture<Boolean> deposit(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);
        return getBalance(uuid).thenCompose(current -> setBalance(uuid, current + amount));
    }

    /**
     * Subtract from a player's balance.
     */
    public CompletableFuture<Boolean> withdraw(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);
        return getBalance(uuid).thenCompose(current -> {
            if (current < amount) return CompletableFuture.completedFuture(false);
            return setBalance(uuid, current - amount);
        });
    }

    /**
     * Transfer money between players.
     */
    public CompletableFuture<Boolean> transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);
        
        return getBalance(from).thenCompose(fromBal -> {
            if (fromBal < amount) return CompletableFuture.completedFuture(false);
            
            // Optimistic update
            return withdraw(from, amount).thenCompose(success -> {
                if (!success) return CompletableFuture.completedFuture(false);
                return deposit(to, amount);
            });
        });
    }

    /**
     * Check if player has enough money.
     */
    public CompletableFuture<Boolean> has(UUID uuid, double amount) {
        return getBalance(uuid).thenApply(bal -> bal >= amount);
    }

    /**
     * Get top balances asynchronously.
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
        String symbol = EssentialsConfig.getInstance().getCurrencySymbol();
        return String.format("%s%.2f", symbol, amount);
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
