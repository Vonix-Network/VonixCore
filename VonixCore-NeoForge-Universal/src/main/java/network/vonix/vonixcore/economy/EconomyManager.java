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
     * Get a player's balance.
     */
    public double getBalance(UUID uuid) {
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT balance FROM vc_economy WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            } else {
                // Create new account with starting balance
                setBalance(uuid, startingBalance);
                return startingBalance;
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to get balance: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Set a player's balance.
     */
    public boolean setBalance(UUID uuid, double balance) {
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO vc_economy (uuid, balance) VALUES (?, ?)");
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, Math.max(0, balance));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to set balance: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Add to a player's balance.
     */
    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0)
            return false;
        double current = getBalance(uuid);
        return setBalance(uuid, current + amount);
    }

    /**
     * Subtract from a player's balance.
     */
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0)
            return false;
        double current = getBalance(uuid);
        if (current < amount)
            return false;
        return setBalance(uuid, current - amount);
    }

    /**
     * Transfer money between players.
     */
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
                // Rollback
                deposit(from, amount);
            }
        }
        return false;
    }

    /**
     * Check if player has enough money.
     */
    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    /**
     * Get top balances.
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
