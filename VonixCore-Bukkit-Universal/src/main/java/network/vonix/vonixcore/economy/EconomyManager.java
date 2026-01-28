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
import java.util.logging.Level;

public class EconomyManager {

    private static EconomyManager instance;
    private final VonixCore plugin;
    private double startingBalance;
    private final java.util.Map<UUID, Double> balanceCache = new java.util.concurrent.ConcurrentHashMap<>();

    public EconomyManager(VonixCore plugin) {
        this.plugin = plugin;
        this.startingBalance = EssentialsConfig.startingBalance;
    }

    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager(VonixCore.getInstance());
        }
        return instance;
    }

    public void loadBalanceAsync(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT balance FROM vonixcore_economy WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    balanceCache.put(uuid, rs.getDouble("balance"));
                } else {
                    // Initialize if not exists
                    setBalance(uuid, startingBalance);
                    balanceCache.put(uuid, startingBalance);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load balance for " + uuid, e);
            }
        });
    }

    public void unloadBalance(UUID uuid) {
        balanceCache.remove(uuid);
    }

    public double getBalance(UUID uuid) {
        if (balanceCache.containsKey(uuid)) {
            return balanceCache.get(uuid);
        }
        
        // Fallback to sync load if not in cache (should be avoided by preloading)
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT balance FROM vonixcore_economy WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                balanceCache.put(uuid, balance);
                return balance;
            } else {
                setBalance(uuid, startingBalance);
                balanceCache.put(uuid, startingBalance);
                return startingBalance;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get balance", e);
            return 0;
        }
    }

    public boolean setBalance(UUID uuid, double balance) {
        double newBalance = Math.max(0, balance);
        balanceCache.put(uuid, newBalance);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO vonixcore_economy (uuid, balance, username) VALUES (?, ?, ?)");
                stmt.setString(1, uuid.toString());
                stmt.setDouble(2, newBalance);
                stmt.setString(3, "Unknown"); // We'll update username separately or on join
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set balance", e);
            }
        });
        return true;
    }

    // Helper to update username on join/transaction if possible, but for now we
    // skip extra query complexity.
    // Ideally we update username when they join.

    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0)
            return false;
        double current = getBalance(uuid);
        return setBalance(uuid, current + amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0)
            return false;
        double current = getBalance(uuid);
        if (current < amount)
            return false;
        return setBalance(uuid, current - amount);
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0)
            return false;
        double fromBalance = getBalance(from);
        if (fromBalance < amount)
            return false;

        if (withdraw(from, amount)) {
            if (deposit(to, amount)) {
                return true;
            } else {
                deposit(from, amount); // Rollback
            }
        }
        return false;
    }

    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public List<BalanceEntry> getTopBalances(int limit) {
        List<BalanceEntry> top = new ArrayList<>();
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT uuid, balance FROM vonixcore_economy ORDER BY balance DESC LIMIT ?");
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                top.add(new BalanceEntry(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getDouble("balance")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get top balances", e);
        }
        return top;
    }

    public String format(double amount) {
        return EssentialsConfig.currencySymbol + String.format("%.2f", amount);
    }

    public record BalanceEntry(UUID uuid, double balance) {
    }
}
