package network.vonix.vonixcore.shops.chest;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.TransactionLog;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages all chest shops in the server.
 * Based on QuickShop-Hikari's ShopManager with optimizations for performance.
 */
public class ChestShopManager {

    private final VonixCore plugin;

    // Cache shops by location for O(1) lookup
    private final Map<String, ChestShop> shopsByLocation = new ConcurrentHashMap<>();

    // Cache shops by owner for quick player queries
    private final Map<UUID, List<Long>> shopsByOwner = new ConcurrentHashMap<>();

    // All shops by ID
    private final Map<Long, ChestShop> shopsById = new ConcurrentHashMap<>();

    // Shops organized by chunk for efficient loading/unloading
    private final Map<String, Set<Long>> shopsByChunk = new ConcurrentHashMap<>();

    private ChestShopListener listener;
    private ChestShopDisplay displayManager;

    public ChestShopManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the chest shop system
     */
    public void initialize(Connection conn) throws SQLException {
        // Create table
        createTable(conn);

        // Load all shops
        loadAllShops();

        // Register listener
        listener = new ChestShopListener(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // Initialize display manager if enabled
        if (ShopsConfig.chestShopsDisplayItems) {
            displayManager = new ChestShopDisplay(plugin, this);
            displayManager.start();
        }

        // Start save task
        startAutoSaveTask();
    }

    /**
     * Create database table
     */
    private void createTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS vonixcore_chest_shops (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL,
                    owner_name TEXT,
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    item_type TEXT NOT NULL,
                    item_data TEXT,
                    price REAL NOT NULL,
                    shop_type TEXT DEFAULT 'SELLING',
                    is_admin INTEGER DEFAULT 0,
                    is_unlimited INTEGER DEFAULT 0,
                    stock INTEGER DEFAULT 0,
                    shop_name TEXT,
                    tax_rate REAL DEFAULT 0,
                    tax_account TEXT,
                    staff_data TEXT,
                    created_at INTEGER,
                    last_transaction INTEGER,
                    UNIQUE(world, x, y, z)
                )
                """;
        conn.createStatement().execute(sql);

        // Create indexes
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_chest_shops_owner ON vonixcore_chest_shops(owner_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_chest_shops_location ON vonixcore_chest_shops(world, x, y, z)");
    }

    /**
     * Load all shops from database
     */
    private void loadAllShops() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM vonixcore_chest_shops");
                ResultSet rs = stmt.executeQuery();

                int loaded = 0;
                while (rs.next()) {
                    ChestShop shop = shopFromResultSet(rs);
                    registerShop(shop);
                    loaded++;
                }

                plugin.getLogger().info("[Shops] Loaded " + loaded + " chest shops");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load chest shops", e);
            }
        });
    }

    /**
     * Register a shop in memory caches
     */
    private void registerShop(ChestShop shop) {
        String locationKey = getLocationKey(shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());
        shopsByLocation.put(locationKey, shop);
        shopsById.put(shop.getId(), shop);

        shopsByOwner.computeIfAbsent(shop.getOwnerUuid(), k -> new ArrayList<>()).add(shop.getId());

        String chunkKey = getChunkKey(shop.getWorld(), shop.getX() >> 4, shop.getZ() >> 4);
        shopsByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(shop.getId());
    }

    /**
     * Unregister a shop from memory caches
     */
    private void unregisterShop(ChestShop shop) {
        String locationKey = getLocationKey(shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());
        shopsByLocation.remove(locationKey);
        shopsById.remove(shop.getId());

        List<Long> ownerShops = shopsByOwner.get(shop.getOwnerUuid());
        if (ownerShops != null) {
            ownerShops.remove(shop.getId());
        }

        String chunkKey = getChunkKey(shop.getWorld(), shop.getX() >> 4, shop.getZ() >> 4);
        Set<Long> chunkShops = shopsByChunk.get(chunkKey);
        if (chunkShops != null) {
            chunkShops.remove(shop.getId());
        }
    }

    /**
     * Create a new chest shop
     */
    public ChestShop createShop(Player owner, Location location, ItemStack item,
            double price, ChestShop.ShopType type) {
        // Check max shops limit
        int currentShops = getPlayerShopCount(owner.getUniqueId());
        if (currentShops >= ShopsConfig.chestShopsMaxPerPlayer && !owner.hasPermission("vonixcore.shops.unlimited")) {
            return null;
        }

        // Validate price
        if (price < ShopsConfig.chestShopsMinPrice || price > ShopsConfig.chestShopsMaxPrice) {
            return null;
        }

        // Check if shop already exists at location
        if (getShopAt(location) != null) {
            return null;
        }

        // Create shop object
        ChestShop shop = ChestShop.create(owner.getUniqueId(), owner.getName(), location, item, price, type);
        shop.setTaxRate(ShopsConfig.chestShopsTaxRate);

        // Save to database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                long id = insertShop(conn, shop);
                shop.setId(id);

                // Register in cache (on main thread)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    registerShop(shop);
                    if (displayManager != null) {
                        displayManager.createDisplay(shop);
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create chest shop", e);
            }
        });

        return shop;
    }

    /**
     * Delete a chest shop
     */
    public boolean deleteShop(ChestShop shop) {
        if (displayManager != null) {
            displayManager.removeDisplay(shop);
        }

        unregisterShop(shop);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM vonixcore_chest_shops WHERE id = ?");
                stmt.setLong(1, shop.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete chest shop", e);
            }
        });

        return true;
    }

    /**
     * Process a purchase transaction
     */
    public boolean processPurchase(Player buyer, ChestShop shop, int amount) {
        double totalPrice = shop.getPrice() * amount;
        double tax = totalPrice * shop.getTaxRate();
        double sellerReceives = totalPrice - tax;

        EconomyManager eco = EconomyManager.getInstance();

        // Check buyer has funds
        if (!eco.has(buyer.getUniqueId(), totalPrice)) {
            return false;
        }

        // Check shop has stock
        if (!shop.hasStock(amount)) {
            return false;
        }

        // Process transaction
        if (!eco.withdraw(buyer.getUniqueId(), totalPrice)) {
            return false;
        }

        // Pay seller (if not admin shop)
        if (!shop.isAdmin()) {
            eco.deposit(shop.getOwnerUuid(), sellerReceives);
        }

        // Handle tax
        if (tax > 0 && shop.getTaxAccount() != null) {
            eco.deposit(shop.getTaxAccount(), tax);
        }

        // Update shop stock
        shop.removeStock(amount);
        shop.recordTransaction();

        // Log transaction
        Location loc = shop.getLocation();
        TransactionLog.getInstance().logShopBuy(
                buyer.getUniqueId(), shop.getOwnerUuid(), totalPrice, tax,
                shop.getId(), shop.getItemType(), amount,
                shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());

        // Give items to buyer
        ItemStack item = shop.getCachedItem().clone();
        item.setAmount(amount);
        HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(item);

        // Drop leftover items at player's feet
        for (ItemStack leftoverItem : leftover.values()) {
            buyer.getWorld().dropItemNaturally(buyer.getLocation(), leftoverItem);
        }

        return true;
    }

    /**
     * Process a sale transaction (player selling to buying shop)
     */
    public boolean processSale(Player seller, ChestShop shop, int amount) {
        double totalPrice = shop.getPrice() * amount;
        double tax = totalPrice * shop.getTaxRate();
        double sellerReceives = totalPrice - tax;

        EconomyManager eco = EconomyManager.getInstance();

        // Check shop owner has funds (if not admin shop)
        if (!shop.isAdmin() && !eco.has(shop.getOwnerUuid(), totalPrice)) {
            return false;
        }

        // Check shop has space
        if (!shop.hasSpace(amount)) {
            return false;
        }

        // Check seller has items
        ItemStack item = shop.getCachedItem().clone();
        if (!hasItem(seller, item, amount)) {
            return false;
        }

        // Remove items from seller
        removeItem(seller, item, amount);

        // Process payment
        if (!shop.isAdmin()) {
            if (!eco.withdraw(shop.getOwnerUuid(), totalPrice)) {
                // Rollback - give items back
                item.setAmount(amount);
                seller.getInventory().addItem(item);
                return false;
            }
        }

        eco.deposit(seller.getUniqueId(), sellerReceives);

        // Handle tax
        if (tax > 0 && shop.getTaxAccount() != null) {
            eco.deposit(shop.getTaxAccount(), tax);
        }

        // Update shop stock
        shop.addStock(amount);
        shop.recordTransaction();

        // Log transaction
        TransactionLog.getInstance().logShopSell(
                seller.getUniqueId(), shop.getOwnerUuid(), totalPrice, tax,
                shop.getId(), shop.getItemType(), amount,
                shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());

        return true;
    }

    // === Query Methods ===

    public ChestShop getShopAt(Location location) {
        String key = getLocationKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return shopsByLocation.get(key);
    }

    public ChestShop getShopAt(Block block) {
        return getShopAt(block.getLocation());
    }

    public ChestShop getShopById(long id) {
        return shopsById.get(id);
    }

    public List<ChestShop> getPlayerShops(UUID owner) {
        List<Long> ids = shopsByOwner.get(owner);
        if (ids == null)
            return Collections.emptyList();
        return ids.stream()
                .map(shopsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public int getPlayerShopCount(UUID owner) {
        List<Long> ids = shopsByOwner.get(owner);
        return ids != null ? ids.size() : 0;
    }

    public List<ChestShop> getShopsInChunk(Chunk chunk) {
        String key = getChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Set<Long> ids = shopsByChunk.get(key);
        if (ids == null)
            return Collections.emptyList();
        return ids.stream()
                .map(shopsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<ChestShop> findShops(String itemType, int maxDistance, Location from) {
        List<ChestShop> results = new ArrayList<>();
        for (ChestShop shop : shopsById.values()) {
            if (shop.getItemType().equalsIgnoreCase(itemType)) {
                Location shopLoc = shop.getLocation();
                if (shopLoc != null && shopLoc.getWorld().equals(from.getWorld())) {
                    if (shopLoc.distance(from) <= maxDistance) {
                        results.add(shop);
                    }
                }
            }
        }
        return results;
    }

    public int getTotalShopCount() {
        return shopsById.size();
    }

    // === Helper Methods ===

    private String getLocationKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    private String getChunkKey(String world, int cx, int cz) {
        return world + ":" + cx + ":" + cz;
    }

    private boolean hasItem(Player player, ItemStack template, int amount) {
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(template)) {
                found += item.getAmount();
                if (found >= amount)
                    return true;
            }
        }
        return false;
    }

    private void removeItem(Player player, ItemStack template, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.isSimilar(template)) {
                int take = Math.min(remaining, item.getAmount());
                if (take >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - take);
                }
                remaining -= take;
            }
        }
    }

    // === Database Methods ===

    private long insertShop(Connection conn, ChestShop shop) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                """
                        INSERT INTO vonixcore_chest_shops
                        (owner_uuid, owner_name, world, x, y, z, item_type, item_data, price, shop_type,
                         is_admin, is_unlimited, stock, shop_name, tax_rate, tax_account, staff_data, created_at, last_transaction)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, shop.getOwnerUuid().toString());
        stmt.setString(2, shop.getOwnerName());
        stmt.setString(3, shop.getWorld());
        stmt.setInt(4, shop.getX());
        stmt.setInt(5, shop.getY());
        stmt.setInt(6, shop.getZ());
        stmt.setString(7, shop.getItemType());
        stmt.setString(8, shop.getItemData());
        stmt.setDouble(9, shop.getPrice());
        stmt.setString(10, shop.getShopType().name());
        stmt.setInt(11, shop.isAdmin() ? 1 : 0);
        stmt.setInt(12, shop.isUnlimited() ? 1 : 0);
        stmt.setInt(13, shop.getStock());
        stmt.setString(14, shop.getShopName());
        stmt.setDouble(15, shop.getTaxRate());
        stmt.setString(16, shop.getTaxAccount() != null ? shop.getTaxAccount().toString() : null);
        stmt.setString(17, serializeStaff(shop.getStaff()));
        stmt.setLong(18, shop.getCreatedAt());
        stmt.setLong(19, shop.getLastTransaction());
        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            return rs.getLong(1);
        }
        throw new SQLException("Failed to get generated ID");
    }

    private void updateShop(Connection conn, ChestShop shop) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                """
                        UPDATE vonixcore_chest_shops SET
                            price = ?, shop_type = ?, is_admin = ?, is_unlimited = ?, stock = ?,
                            shop_name = ?, tax_rate = ?, tax_account = ?, staff_data = ?, last_transaction = ?
                        WHERE id = ?
                        """);
        stmt.setDouble(1, shop.getPrice());
        stmt.setString(2, shop.getShopType().name());
        stmt.setInt(3, shop.isAdmin() ? 1 : 0);
        stmt.setInt(4, shop.isUnlimited() ? 1 : 0);
        stmt.setInt(5, shop.getStock());
        stmt.setString(6, shop.getShopName());
        stmt.setDouble(7, shop.getTaxRate());
        stmt.setString(8, shop.getTaxAccount() != null ? shop.getTaxAccount().toString() : null);
        stmt.setString(9, serializeStaff(shop.getStaff()));
        stmt.setLong(10, shop.getLastTransaction());
        stmt.setLong(11, shop.getId());
        stmt.executeUpdate();
    }

    private ChestShop shopFromResultSet(ResultSet rs) throws SQLException {
        ChestShop shop = new ChestShop();
        shop.setId(rs.getLong("id"));
        shop.setOwnerUuid(UUID.fromString(rs.getString("owner_uuid")));
        shop.setOwnerName(rs.getString("owner_name"));
        shop.setWorld(rs.getString("world"));
        shop.setX(rs.getInt("x"));
        shop.setY(rs.getInt("y"));
        shop.setZ(rs.getInt("z"));
        shop.setItemType(rs.getString("item_type"));
        shop.setItemData(rs.getString("item_data"));
        shop.setPrice(rs.getDouble("price"));
        shop.setShopType(ChestShop.ShopType.valueOf(rs.getString("shop_type")));
        shop.setAdmin(rs.getInt("is_admin") == 1);
        shop.setUnlimited(rs.getInt("is_unlimited") == 1);
        shop.setStock(rs.getInt("stock"));
        shop.setShopName(rs.getString("shop_name"));
        shop.setTaxRate(rs.getDouble("tax_rate"));
        String taxAccount = rs.getString("tax_account");
        shop.setTaxAccount(taxAccount != null ? UUID.fromString(taxAccount) : null);
        shop.setStaff(deserializeStaff(rs.getString("staff_data")));
        shop.setCreatedAt(rs.getLong("created_at"));
        shop.setLastTransaction(rs.getLong("last_transaction"));

        // Create cached item
        Material mat = Material.getMaterial(shop.getItemType());
        if (mat != null) {
            shop.setCachedItem(new ItemStack(mat));
            // TODO: Apply item data for complex items
        }

        return shop;
    }

    private String serializeStaff(Map<UUID, ChestShop.StaffPermission> staff) {
        if (staff == null || staff.isEmpty())
            return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<UUID, ChestShop.StaffPermission> entry : staff.entrySet()) {
            if (sb.length() > 0)
                sb.append(";");
            sb.append(entry.getKey().toString()).append(":").append(entry.getValue().name());
        }
        return sb.toString();
    }

    private Map<UUID, ChestShop.StaffPermission> deserializeStaff(String data) {
        Map<UUID, ChestShop.StaffPermission> staff = new HashMap<>();
        if (data == null || data.isEmpty())
            return staff;
        for (String entry : data.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                try {
                    staff.put(UUID.fromString(parts[0]), ChestShop.StaffPermission.valueOf(parts[1]));
                } catch (Exception ignored) {
                }
            }
        }
        return staff;
    }

    private void startAutoSaveTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                for (ChestShop shop : shopsById.values()) {
                    if (shop.isDirty()) {
                        updateShop(conn, shop);
                        shop.setDirty(false);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to auto-save shops", e);
            }
        }, 20 * 60 * 5, 20 * 60 * 5); // Every 5 minutes
    }

    public void shutdown() {
        // Save all dirty shops
        try (Connection conn = plugin.getDatabase().getConnection()) {
            for (ChestShop shop : shopsById.values()) {
                if (shop.isDirty()) {
                    updateShop(conn, shop);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save shops on shutdown", e);
        }

        if (displayManager != null) {
            displayManager.shutdown();
        }

        shopsByLocation.clear();
        shopsByOwner.clear();
        shopsById.clear();
        shopsByChunk.clear();
    }

    public void reload() {
        // Reload is handled by ShopsConfig
    }
}
