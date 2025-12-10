package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Transaction logging system for complete audit trail of all economy
 * operations.
 * Inspired by QuickShop-Hikari's logging system and EconomyCore's transaction
 * management.
 */
public class TransactionLog {

    private static TransactionLog instance;
    private final VonixCore plugin;

    public TransactionLog(VonixCore plugin) {
        this.plugin = plugin;
    }

    public static TransactionLog getInstance() {
        if (instance == null) {
            instance = new TransactionLog(VonixCore.getInstance());
        }
        return instance;
    }

    /**
     * Initialize the transaction log table
     */
    public void initializeTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS vonixcore_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_uuid TEXT,
                    to_uuid TEXT,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT,
                    shop_id INTEGER,
                    item_type TEXT,
                    item_amount INTEGER,
                    tax_amount REAL DEFAULT 0,
                    timestamp INTEGER NOT NULL,
                    world TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER
                )
                """;
        conn.createStatement().execute(sql);

        // Create index for faster queries
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_transactions_from ON vonixcore_transactions(from_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_transactions_to ON vonixcore_transactions(to_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON vonixcore_transactions(timestamp)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_transactions_type ON vonixcore_transactions(type)");
    }

    /**
     * Log a transaction
     */
    public void log(TransactionRecord record) {
        if (!ShopsConfig.transactionLogEnabled)
            return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        """
                                INSERT INTO vonixcore_transactions
                                (from_uuid, to_uuid, amount, type, description, shop_id, item_type, item_amount, tax_amount, timestamp, world, x, y, z)
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
                stmt.setLong(10, record.timestamp);
                stmt.setString(11, record.world);
                stmt.setObject(12, record.x);
                stmt.setObject(13, record.y);
                stmt.setObject(14, record.z);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to log transaction", e);
            }
        });
    }

    /**
     * Log a simple balance transfer
     */
    public void logTransfer(UUID from, UUID to, double amount, String description) {
        log(new TransactionRecord(from, to, amount, TransactionType.TRANSFER, description));
    }

    /**
     * Log a deposit
     */
    public void logDeposit(UUID player, double amount, String description) {
        log(new TransactionRecord(null, player, amount, TransactionType.DEPOSIT, description));
    }

    /**
     * Log a withdrawal
     */
    public void logWithdraw(UUID player, double amount, String description) {
        log(new TransactionRecord(player, null, amount, TransactionType.WITHDRAW, description));
    }

    /**
     * Log an admin set balance operation
     */
    public void logAdminSet(UUID player, double amount, String adminName) {
        log(new TransactionRecord(null, player, amount, TransactionType.ADMIN_SET, "Set by " + adminName));
    }

    /**
     * Log a shop purchase (player bought from shop)
     */
    public void logShopBuy(UUID buyer, UUID shopOwner, double amount, double tax,
            long shopId, String itemType, int itemAmount, String world, int x, int y, int z) {
        TransactionRecord record = new TransactionRecord(buyer, shopOwner, amount, TransactionType.SHOP_BUY,
                "Bought " + itemAmount + "x " + itemType);
        record.shopId = shopId;
        record.itemType = itemType;
        record.itemAmount = itemAmount;
        record.taxAmount = tax;
        record.world = world;
        record.x = x;
        record.y = y;
        record.z = z;
        log(record);
    }

    /**
     * Log a shop sale (player sold to shop)
     */
    public void logShopSell(UUID seller, UUID shopOwner, double amount, double tax,
            long shopId, String itemType, int itemAmount, String world, int x, int y, int z) {
        TransactionRecord record = new TransactionRecord(shopOwner, seller, amount, TransactionType.SHOP_SELL,
                "Sold " + itemAmount + "x " + itemType);
        record.shopId = shopId;
        record.itemType = itemType;
        record.itemAmount = itemAmount;
        record.taxAmount = tax;
        record.world = world;
        record.x = x;
        record.y = y;
        record.z = z;
        log(record);
    }

    /**
     * Log a player market transaction
     */
    public void logMarketPurchase(UUID buyer, UUID seller, double amount, double tax,
            String itemType, int itemAmount) {
        TransactionRecord record = new TransactionRecord(buyer, seller, amount, TransactionType.MARKET_BUY,
                "Market purchase: " + itemAmount + "x " + itemType);
        record.itemType = itemType;
        record.itemAmount = itemAmount;
        record.taxAmount = tax;
        log(record);
    }

    /**
     * Get transaction history for a player
     */
    public List<TransactionRecord> getHistory(UUID player, int limit) {
        List<TransactionRecord> history = new ArrayList<>();
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    """
                            SELECT * FROM vonixcore_transactions
                            WHERE from_uuid = ? OR to_uuid = ?
                            ORDER BY timestamp DESC
                            LIMIT ?
                            """);
            stmt.setString(1, player.toString());
            stmt.setString(2, player.toString());
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                history.add(recordFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get transaction history", e);
        }
        return history;
    }

    /**
     * Get transactions for a specific shop
     */
    public List<TransactionRecord> getShopHistory(long shopId, int limit) {
        List<TransactionRecord> history = new ArrayList<>();
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM vonixcore_transactions WHERE shop_id = ? ORDER BY timestamp DESC LIMIT ?");
            stmt.setLong(1, shopId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                history.add(recordFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get shop history", e);
        }
        return history;
    }

    /**
     * Get total revenue for a player from their shops
     */
    public double getPlayerShopRevenue(UUID player) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    """
                            SELECT COALESCE(SUM(amount), 0) as total FROM vonixcore_transactions
                            WHERE to_uuid = ? AND (type = 'SHOP_BUY' OR type = 'MARKET_BUY')
                            """);
            stmt.setString(1, player.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get shop revenue", e);
        }
        return 0;
    }

    /**
     * Purge old transactions based on retention policy
     */
    public void purgeOldTransactions() {
        if (ShopsConfig.transactionLogRetentionDays <= 0)
            return;

        long cutoffTime = System.currentTimeMillis()
                - (ShopsConfig.transactionLogRetentionDays * 24L * 60L * 60L * 1000L);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM vonixcore_transactions WHERE timestamp < ?");
                stmt.setLong(1, cutoffTime);
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().info("Purged " + deleted + " old transaction records");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to purge old transactions", e);
            }
        });
    }

    private TransactionRecord recordFromResultSet(ResultSet rs) throws SQLException {
        TransactionRecord record = new TransactionRecord(
                rs.getString("from_uuid") != null ? UUID.fromString(rs.getString("from_uuid")) : null,
                rs.getString("to_uuid") != null ? UUID.fromString(rs.getString("to_uuid")) : null,
                rs.getDouble("amount"),
                TransactionType.valueOf(rs.getString("type")),
                rs.getString("description"));
        record.id = rs.getLong("id");
        record.shopId = rs.getObject("shop_id") != null ? rs.getLong("shop_id") : null;
        record.itemType = rs.getString("item_type");
        record.itemAmount = rs.getObject("item_amount") != null ? rs.getInt("item_amount") : null;
        record.taxAmount = rs.getDouble("tax_amount");
        record.timestamp = rs.getLong("timestamp");
        record.world = rs.getString("world");
        record.x = rs.getObject("x") != null ? rs.getInt("x") : null;
        record.y = rs.getObject("y") != null ? rs.getInt("y") : null;
        record.z = rs.getObject("z") != null ? rs.getInt("z") : null;
        return record;
    }

    /**
     * Transaction types
     */
    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        ADMIN_SET,
        ADMIN_GIVE,
        ADMIN_TAKE,
        SHOP_BUY, // Player bought from chest/sign shop
        SHOP_SELL, // Player sold to chest/sign shop
        GUI_SHOP_BUY, // Player bought from admin GUI shop
        GUI_SHOP_SELL, // Player sold to admin GUI shop
        MARKET_BUY, // Player bought from player market
        MARKET_LIST, // Player listed item on market
        MARKET_CANCEL, // Player cancelled market listing
        MARKET_EXPIRE // Market listing expired
    }

    /**
     * Transaction record
     */
    public static class TransactionRecord {
        public Long id;
        public UUID fromUuid;
        public UUID toUuid;
        public double amount;
        public TransactionType type;
        public String description;
        public Long shopId;
        public String itemType;
        public Integer itemAmount;
        public double taxAmount;
        public long timestamp;
        public String world;
        public Integer x;
        public Integer y;
        public Integer z;

        public TransactionRecord(UUID fromUuid, UUID toUuid, double amount, TransactionType type, String description) {
            this.fromUuid = fromUuid;
            this.toUuid = toUuid;
            this.amount = amount;
            this.type = type;
            this.description = description;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
