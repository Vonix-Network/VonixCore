package network.vonix.vonixcore.economy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import network.vonix.vonixcore.VonixCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chest-based block shops with holograms.
 * Players can create shops by clicking a chest after using /chestshop create.
 * Async + Caching Implementation
 */
public class ShopManager {

    private static ShopManager instance;

    // Players in shop creation mode: UUID -> creation state
    private final Map<UUID, ShopCreationState> creatingShop = new ConcurrentHashMap<>();

    // Cached shops for quick lookup
    private final Map<String, ChestShop> shopCache = new ConcurrentHashMap<>();

    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }

    /**
     * Initialize shop tables in database.
     */
    public void initializeTable(Connection conn) throws SQLException {
        // Chest shops table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_chest_shops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner_uuid TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        item_id TEXT NOT NULL,
                        buy_price REAL,
                        sell_price REAL,
                        stock INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        UNIQUE(world, x, y, z)
                    )
                """);
        conn.createStatement()
                .execute("CREATE INDEX IF NOT EXISTS idx_shops_location ON vc_chest_shops (world, x, y, z)");
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_shops_owner ON vc_chest_shops (owner_uuid)");

        // Admin shop prices table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_admin_shop (
                        item_id TEXT PRIMARY KEY,
                        buy_price REAL,
                        sell_price REAL
                    )
                """);

        // Player shop listings table (GUI shop)
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_player_listings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        seller_uuid TEXT NOT NULL,
                        item_id TEXT NOT NULL,
                        item_nbt TEXT,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        listed_at INTEGER NOT NULL
                    )
                """);
        conn.createStatement()
                .execute("CREATE INDEX IF NOT EXISTS idx_listings_seller ON vc_player_listings (seller_uuid)");
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_listings_item ON vc_player_listings (item_id)");

        // Daily rewards table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_daily_rewards (
                        uuid TEXT PRIMARY KEY,
                        last_claim INTEGER NOT NULL,
                        streak INTEGER NOT NULL DEFAULT 1
                    )
                """);
    }

    // ===== CHEST SHOP CREATION =====

    public void startShopCreation(ServerPlayer player) {
        creatingShop.put(player.getUUID(), new ShopCreationState());
    }

    public boolean isCreatingShop(UUID uuid) {
        return creatingShop.containsKey(uuid);
    }

    public ShopCreationState getCreationState(UUID uuid) {
        return creatingShop.get(uuid);
    }

    public void cancelShopCreation(UUID uuid) {
        creatingShop.remove(uuid);
    }

    public boolean createChestShop(ServerPlayer player, BlockPos pos, String itemId, Double buyPrice, Double sellPrice,
            int quantity) {
        UUID uuid = player.getUUID();
        String world = player.level().dimension().location().toString();

        // Update Cache Immediately
        String key = shopKey(world, pos);
        shopCache.put(key, new ChestShop(uuid, itemId, buyPrice, sellPrice, quantity));
        creatingShop.remove(uuid);

        // Async DB Save
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO vc_chest_shops (owner_uuid, world, x, y, z, item_id, buy_price, sell_price, stock, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, world);
                stmt.setInt(3, pos.getX());
                stmt.setInt(4, pos.getY());
                stmt.setInt(5, pos.getZ());
                stmt.setString(6, itemId);
                stmt.setObject(7, buyPrice);
                stmt.setObject(8, sellPrice);
                stmt.setInt(9, quantity);
                stmt.setLong(10, System.currentTimeMillis() / 1000L);
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to create chest shop: {}", e.getMessage());
                // In a real robust system, we might revert cache or mark as failed
            }
        });

        return true;
    }

    public ChestShop getShopAt(String world, BlockPos pos) {
        String key = shopKey(world, pos);

        // Check cache first
        if (shopCache.containsKey(key)) {
            return shopCache.get(key);
        }

        // Load from database (Sync fallback - unavoidable without changing return type)
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT owner_uuid, item_id, buy_price, sell_price, stock FROM vc_chest_shops WHERE world = ? AND x = ? AND y = ? AND z = ?");
            stmt.setString(1, world);
            stmt.setInt(2, pos.getX());
            stmt.setInt(3, pos.getY());
            stmt.setInt(4, pos.getZ());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ChestShop shop = new ChestShop(
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("item_id"),
                        rs.getObject("buy_price") != null ? rs.getDouble("buy_price") : null,
                        rs.getObject("sell_price") != null ? rs.getDouble("sell_price") : null,
                        rs.getInt("stock"));
                shopCache.put(key, shop);
                return shop;
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to get shop: {}", e.getMessage());
        }
        return null;
    }

    public boolean deleteShop(String world, BlockPos pos) {
        String key = shopKey(world, pos);
        shopCache.remove(key); // Instant update

        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM vc_chest_shops WHERE world = ? AND x = ? AND y = ? AND z = ?");
                stmt.setString(1, world);
                stmt.setInt(2, pos.getX());
                stmt.setInt(3, pos.getY());
                stmt.setInt(4, pos.getZ());
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to delete shop: {}", e.getMessage());
            }
        });
        return true;
    }

    public boolean updateStock(String world, BlockPos pos, int delta) {
        String key = shopKey(world, pos);
        
        // Update cache if present
        if (shopCache.containsKey(key)) {
            ChestShop old = shopCache.get(key);
            shopCache.put(key, new ChestShop(old.owner(), old.itemId(), old.buyPrice(), old.sellPrice(),
                    Math.max(0, old.stock() + delta)));
        }

        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE vc_chest_shops SET stock = stock + ? WHERE world = ? AND x = ? AND y = ? AND z = ?");
                stmt.setInt(1, delta);
                stmt.setString(2, world);
                stmt.setInt(3, pos.getX());
                stmt.setInt(4, pos.getY());
                stmt.setInt(5, pos.getZ());
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to update shop stock: {}", e.getMessage());
            }
        });
        return true;
    }

    public List<ChestShopLocation> getShopsInChunk(String world, int chunkX, int chunkZ) {
        List<ChestShopLocation> shops = new ArrayList<>();
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;

        // Sync read (usually called during chunk load/scan)
        // Can be made async if caller supports it, but keeping sync for safety
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT x, y, z, item_id FROM vc_chest_shops WHERE world = ? AND x >= ? AND x <= ? AND z >= ? AND z <= ?");
            stmt.setString(1, world);
            stmt.setInt(2, minX);
            stmt.setInt(3, maxX);
            stmt.setInt(4, minZ);
            stmt.setInt(5, maxZ);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                shops.add(new ChestShopLocation(
                        new BlockPos(rs.getInt("x"), rs.getInt("y"), rs.getInt("z")),
                        rs.getString("item_id")));
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to get shops in chunk: {}", e.getMessage());
        }
        return shops;
    }

    public record ChestShopLocation(BlockPos pos, String itemId) {
    }

    // ===== ADMIN SHOP =====

    public boolean setAdminPrice(String itemId, Double buyPrice, Double sellPrice) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO vc_admin_shop (item_id, buy_price, sell_price) VALUES (?, ?, ?)");
                stmt.setString(1, itemId);
                stmt.setObject(2, buyPrice);
                stmt.setObject(3, sellPrice);
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to set admin price: {}", e.getMessage());
            }
        });
        return true;
    }

    public AdminShopItem getAdminPrice(String itemId) {
        // Caching for admin prices would be good too, but skipping for brevity
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT buy_price, sell_price FROM vc_admin_shop WHERE item_id = ?");
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new AdminShopItem(
                        itemId,
                        rs.getObject("buy_price") != null ? rs.getDouble("buy_price") : null,
                        rs.getObject("sell_price") != null ? rs.getDouble("sell_price") : null);
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to get admin price: {}", e.getMessage());
        }
        return null;
    }

    public List<AdminShopItem> getAllAdminItems() {
        List<AdminShopItem> items = new ArrayList<>();
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT item_id, buy_price, sell_price FROM vc_admin_shop ORDER BY item_id");
            while (rs.next()) {
                items.add(new AdminShopItem(
                        rs.getString("item_id"),
                        rs.getObject("buy_price") != null ? rs.getDouble("buy_price") : null,
                        rs.getObject("sell_price") != null ? rs.getDouble("sell_price") : null));
            }
        } catch (SQLException e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to list admin items: {}", e.getMessage());
        }
        return items;
    }

    // ===== PLAYER LISTINGS (GUI SHOP) =====

    public boolean createListing(UUID seller, String itemId, String nbt, double price, int quantity) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO vc_player_listings (seller_uuid, item_id, item_nbt, price, quantity, listed_at) VALUES (?, ?, ?, ?, ?, ?)");
                stmt.setString(1, seller.toString());
                stmt.setString(2, itemId);
                stmt.setString(3, nbt);
                stmt.setDouble(4, price);
                stmt.setInt(5, quantity);
                stmt.setLong(6, System.currentTimeMillis() / 1000L);
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to create listing: {}", e.getMessage());
            }
        });
        return true;
    }

    public CompletableFuture<List<PlayerListing>> getAllListings() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerListing> listings = new ArrayList<>();
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT id, seller_uuid, item_id, item_nbt, price, quantity FROM vc_player_listings ORDER BY listed_at DESC");
                while (rs.next()) {
                    listings.add(new PlayerListing(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("seller_uuid")),
                            rs.getString("item_id"),
                            rs.getString("item_nbt"),
                            rs.getDouble("price"),
                            rs.getInt("quantity")));
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to get listings: {}", e.getMessage());
            }
            return listings;
        }, VonixCore::executeAsync);
    }

    public CompletableFuture<Boolean> buyListing(int listingId, UUID buyer) {
        return CompletableFuture.supplyAsync(() -> getListing(listingId), VonixCore::executeAsync)
            .thenCompose(listing -> {
                if (listing == null) return CompletableFuture.completedFuture(false);
                
                return EconomyManager.getInstance().transfer(buyer, listing.seller(), listing.price())
                    .thenCompose(success -> {
                        if (!success) return CompletableFuture.completedFuture(false);
                        
                        return CompletableFuture.supplyAsync(() -> {
                            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                                PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM vc_player_listings WHERE id = ?");
                                deleteStmt.setInt(1, listingId);
                                deleteStmt.executeUpdate();
                                return true;
                            } catch (SQLException e) {
                                VonixCore.LOGGER.error("[VonixCore] Failed to delete listing after purchase: {}", e.getMessage());
                                return true; // Still return true as purchase technically succeeded
                            }
                        }, VonixCore::executeAsync);
                    });
            });
    }
    
    private PlayerListing getListing(int id) {
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT seller_uuid, item_id, item_nbt, price, quantity FROM vc_player_listings WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PlayerListing(
                    id,
                    UUID.fromString(rs.getString("seller_uuid")),
                    rs.getString("item_id"),
                    rs.getString("item_nbt"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                );
            }
        } catch (SQLException e) {
             VonixCore.LOGGER.error("[VonixCore] Failed to get listing: {}", e.getMessage());
        }
        return null;
    }

    // ===== DAILY REWARDS =====

    public CompletableFuture<DailyRewardResult> claimDailyReward(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                long now = System.currentTimeMillis() / 1000L;
                long dayStart = (now / 86400) * 86400;
    
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT last_claim, streak FROM vc_daily_rewards WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
    
                int streak = 1;
                if (rs.next()) {
                    long lastClaim = rs.getLong("last_claim");
                    long lastDayStart = (lastClaim / 86400) * 86400;
    
                    if (lastDayStart == dayStart) {
                        return new DailyRewardResult(false, 0, rs.getInt("streak"), "Already claimed today!");
                    }
    
                    if (lastDayStart == dayStart - 86400) {
                        streak = rs.getInt("streak") + 1;
                    }
                }
    
                int effectiveStreak = Math.min(streak, 7);
                double reward = 100 + ((effectiveStreak - 1) * 10);
    
                // Async Save
                int finalStreak = streak;
                
                PreparedStatement saveStmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO vc_daily_rewards (uuid, last_claim, streak) VALUES (?, ?, ?)");
                saveStmt.setString(1, uuid.toString());
                saveStmt.setLong(2, now);
                saveStmt.setInt(3, finalStreak);
                saveStmt.executeUpdate();
                
                EconomyManager.getInstance().deposit(uuid, reward); // Chain this? deposit is now async.
                
                return new DailyRewardResult(true, reward, streak, null);
            } catch (SQLException e) {
                VonixCore.LOGGER.error("[VonixCore] Failed to claim daily: {}", e.getMessage());
                return new DailyRewardResult(false, 0, 0, "Database error");
            }
        }, VonixCore::executeAsync);
    }

    // ===== UTILITY =====

    private String shopKey(String world, BlockPos pos) {
        return world + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String getItemId(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null ? key.toString() : "minecraft:air";
    }

    // ===== DATA CLASSES =====

    public static class ShopCreationState {
        public BlockPos chestPos;
        public String itemId;
        public Double buyPrice;
        public Double sellPrice;
        public int step = 0;
    }

    public record ChestShop(UUID owner, String itemId, Double buyPrice, Double sellPrice, int stock) {
    }

    public record AdminShopItem(String itemId, Double buyPrice, Double sellPrice) {
    }

    public record PlayerListing(int id, UUID seller, String itemId, String nbt, double price, int quantity) {
    }

    public record DailyRewardResult(boolean success, double amount, int streak, String message) {
    }
}
