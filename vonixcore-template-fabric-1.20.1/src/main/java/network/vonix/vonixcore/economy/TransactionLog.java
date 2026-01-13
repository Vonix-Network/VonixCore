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
 * Logs economy transactions.
 */
public class TransactionLog {

    private static TransactionLog instance;

    public static TransactionLog getInstance() {
        if (instance == null) {
            instance = new TransactionLog();
        }
        return instance;
    }

    /**
     * Initialize transaction log table.
     */
    public void initializeTable(Connection conn) throws SQLException {
        conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS vc_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    balance_before REAL NOT NULL,
                    balance_after REAL NOT NULL,
                    description TEXT,
                    timestamp INTEGER NOT NULL
                )
            """);
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON vc_transactions (uuid)");
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_transactions_time ON vc_transactions (timestamp)");
    }

    /**
     * Log a transaction.
     */
    public void log(UUID uuid, String type, double amount, double balanceBefore, double balanceAfter, String description) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO vc_transactions (uuid, type, amount, balance_before, balance_after, description, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, type);
                stmt.setDouble(3, amount);
                stmt.setDouble(4, balanceBefore);
                stmt.setDouble(5, balanceAfter);
                stmt.setString(6, description);
                stmt.setLong(7, System.currentTimeMillis() / 1000);
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[Economy] Failed to log transaction: {}", e.getMessage());
            }
        });
    }

    /**
     * Get recent transactions for a player.
     */
    public List<Transaction> getRecentTransactions(UUID uuid, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM vc_transactions WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?");
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("type"),
                    rs.getDouble("amount"),
                    rs.getDouble("balance_before"),
                    rs.getDouble("balance_after"),
                    rs.getString("description"),
                    rs.getLong("timestamp")
                ));
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[Economy] Failed to get transactions: {}", e.getMessage());
        }
        return transactions;
    }

    public record Transaction(
        int id,
        UUID uuid,
        String type,
        double amount,
        double balanceBefore,
        double balanceAfter,
        String description,
        long timestamp
    ) {}
}
