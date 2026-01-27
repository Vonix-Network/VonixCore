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

/**
 * Transaction logging system for comprehensive economy auditing.
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
     * Initialize the transaction log table
     */
    public void initializeTable(Connection conn) throws SQLException {
        String autoIncrement = "AUTOINCREMENT";
        if (VonixCore.getInstance().getDatabase().isMySQL())
            autoIncrement = "AUTO_INCREMENT";
        if (VonixCore.getInstance().getDatabase().isPostgreSQL())
            autoIncrement = "GENERATED ALWAYS AS IDENTITY";

        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS vonixcore_transactions (
                    id INTEGER PRIMARY KEY %s,
                    from_uuid TEXT,
                    to_uuid TEXT,
                    amount REAL NOT NULL,
                    transaction_type TEXT NOT NULL,
                    description TEXT,
                    shop_id INTEGER,
                    item_type TEXT,
                    item_amount INTEGER,
                    tax_amount REAL DEFAULT 0,
                    world TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    timestamp INTEGER NOT NULL
                )
                """, autoIncrement);
        conn.createStatement().execute(sql);

        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_tx_from ON vonixcore_transactions(from_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_tx_to ON vonixcore_transactions(to_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_tx_time ON vonixcore_transactions(timestamp)");
    }

    /**
     * Log a transaction asynchronously
     */
    public void log(TransactionRecord record) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        """
                                INSERT INTO vonixcore_transactions
                                (from_uuid, to_uuid, amount, transaction_type, description,
                                 shop_id, item_type, item_amount, tax_amount, world, x, y, z, timestamp)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """);
                stmt.setString(1, record.fromUuid != null ? record.fromUuid.toString() : null);
                stmt.setString(2, record.toUuid != null ? record.toUuid.toString() : null);
                stmt.setDouble(3, record.amount);
                stmt.setString(4, record.type.name());
                stmt.setString(5, record.description);
                stmt.setObject(6, record.shopId);
                stmt.setString(7, record.itemType);
                stmt.setObject(8, record.itemAmount);
                stmt.setDouble(9, record.taxAmount);
                stmt.setString(10, record.world);
                stmt.setObject(11, record.x);
                stmt.setObject(12, record.y);
                stmt.setObject(13, record.z);
                stmt.setLong(14, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.warn("Failed to log transaction: {}", e.getMessage());
            }
        });
    }

    public void logDeposit(UUID player, double amount, String source) {
        TransactionRecord record = new TransactionRecord();
        record.toUuid = player;
        record.amount = amount;
        record.type = TransactionType.DEPOSIT;
        record.description = source;
        log(record);
    }

    public void logWithdraw(UUID player, double amount, String reason) {
        TransactionRecord record = new TransactionRecord();
        record.fromUuid = player;
        record.amount = amount;
        record.type = TransactionType.WITHDRAWAL;
        record.description = reason;
        log(record);
    }

    public void logTransfer(UUID from, UUID to, double amount) {
        TransactionRecord record = new TransactionRecord();
        record.fromUuid = from;
        record.toUuid = to;
        record.amount = amount;
        record.type = TransactionType.TRANSFER;
        record.description = "Player transfer";
        log(record);
    }

    private TransactionRecord recordFromResultSet(ResultSet rs) throws SQLException {
        TransactionRecord record = new TransactionRecord();
        record.id = rs.getLong("id");
        String fromUuid = rs.getString("from_uuid");
        record.fromUuid = fromUuid != null ? UUID.fromString(fromUuid) : null;
        String toUuid = rs.getString("to_uuid");
        record.toUuid = toUuid != null ? UUID.fromString(toUuid) : null;
        record.amount = rs.getDouble("amount");
        record.type = TransactionType.valueOf(rs.getString("transaction_type"));
        record.description = rs.getString("description");
        record.shopId = rs.getLong("shop_id");
        record.itemType = rs.getString("item_type");
        record.itemAmount = rs.getInt("item_amount");
        record.taxAmount = rs.getDouble("tax_amount");
        record.world = rs.getString("world");
        record.x = (Integer) rs.getObject("x");
        record.y = (Integer) rs.getObject("y");
        record.z = (Integer) rs.getObject("z");
        record.timestamp = rs.getLong("timestamp");
        return record;
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, ADMIN_SET, SHOP_BUY, SHOP_SELL,
        MARKET_PURCHASE, MARKET_LISTING, DAILY_REWARD, KIT_COST, COMMAND_COST, OTHER
    }

    public static class TransactionRecord {
        public long id;
        public UUID fromUuid;
        public UUID toUuid;
        public double amount;
        public TransactionType type;
        public String description;
        public long shopId;
        public String itemType;
        public Integer itemAmount;
        public double taxAmount;
        public String world;
        public Integer x;
        public Integer y;
        public Integer z;
        public long timestamp;
    }
}
