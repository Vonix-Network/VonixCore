package network.vonix.vonixcore.shops.sign;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.TransactionLog;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages sign shops - shops created by placing signs with [Buy] or [Sell]
 * headers.
 */
public class SignShopManager implements Listener {

    private final VonixCore plugin;

    private final Map<String, SignShop> shopsByLocation = new ConcurrentHashMap<>();
    private final Map<Long, SignShop> shopsById = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> shopsByOwner = new ConcurrentHashMap<>();

    public SignShopManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public void initialize(Connection conn) throws SQLException {
        createTable(conn);
        loadAllShops();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void createTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS vonixcore_sign_shops (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL,
                    owner_name TEXT,
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    item_type TEXT NOT NULL,
                    quantity INTEGER DEFAULT 1,
                    price REAL NOT NULL,
                    shop_type TEXT DEFAULT 'BUY',
                    is_admin INTEGER DEFAULT 0,
                    created_at INTEGER,
                    UNIQUE(world, x, y, z)
                )
                """;
        conn.createStatement().execute(sql);
    }

    private void loadAllShops() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vonixcore_sign_shops");
                ResultSet rs = stmt.executeQuery();

                int loaded = 0;
                while (rs.next()) {
                    SignShop shop = shopFromResultSet(rs);
                    registerShop(shop);
                    loaded++;
                }
                plugin.getLogger().info("[Shops] Loaded " + loaded + " sign shops");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load sign shops", e);
            }
        });
    }

    private void registerShop(SignShop shop) {
        String key = getLocationKey(shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());
        shopsByLocation.put(key, shop);
        shopsById.put(shop.getId(), shop);
        shopsByOwner.computeIfAbsent(shop.getOwnerUuid(), k -> new ArrayList<>()).add(shop.getId());
    }

    private void unregisterShop(SignShop shop) {
        String key = getLocationKey(shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());
        shopsByLocation.remove(key);
        shopsById.remove(shop.getId());
        List<Long> ownerShops = shopsByOwner.get(shop.getOwnerUuid());
        if (ownerShops != null) {
            ownerShops.remove(shop.getId());
        }
    }

    /**
     * Handle sign creation
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String line0 = event.line(0) != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.line(0)).trim().toLowerCase() : "";

        // Check for shop signs
        boolean isBuySign = line0.equals("[buy]");
        boolean isSellSign = line0.equals("[sell]");

        if (!isBuySign && !isSellSign)
            return;

        // Check permission
        if (ShopsConfig.signShopsRequirePermission && !player.hasPermission("vonixcore.shops.sign.create")) {
            player.sendMessage(
                    Component.text("You don't have permission to create sign shops!").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Check max shops
        int current = getPlayerShopCount(player.getUniqueId());
        if (current >= ShopsConfig.signShopsMaxPerPlayer && !player.hasPermission("vonixcore.shops.unlimited")) {
            player.sendMessage(
                    Component.text("You have reached the maximum number of sign shops!").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Parse sign lines
        // Line 1: Quantity
        // Line 2: Price
        // Line 3: Item
        String line1 = event.line(1) != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.line(1)).trim() : "";
        String line2 = event.line(2) != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.line(2)).trim() : "";
        String line3 = event.line(3) != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.line(3)).trim() : "";

        int quantity;
        double price;
        String itemName;

        try {
            quantity = line1.isEmpty() ? 1 : Integer.parseInt(line1);
            price = Double.parseDouble(line2);
            itemName = line3.toUpperCase().replace(" ", "_");
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid sign format! Use:").color(NamedTextColor.RED));
            player.sendMessage(Component.text("Line 1: [Buy] or [Sell]").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Line 2: Quantity").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Line 3: Price").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Line 4: Item name").color(NamedTextColor.GRAY));
            event.setCancelled(true);
            return;
        }

        // Validate item
        Material material = Material.getMaterial(itemName);
        if (material == null) {
            player.sendMessage(Component.text("Unknown item: " + itemName).color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Create shop
        SignShop.ShopType shopType = isBuySign ? SignShop.ShopType.BUY : SignShop.ShopType.SELL;
        SignShop shop = SignShop.create(player.getUniqueId(), player.getName(),
                event.getBlock().getLocation(), itemName, quantity, price, shopType);

        // Save to database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                long id = insertShop(conn, shop);
                shop.setId(id);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    registerShop(shop);

                    // Update sign appearance
                    event.line(0, Component.text(isBuySign ? "[Buy]" : "[Sell]").color(NamedTextColor.DARK_BLUE));
                    event.line(1, Component.text(String.valueOf(quantity)).color(NamedTextColor.BLACK));
                    event.line(2, Component.text(EconomyManager.getInstance().format(price))
                            .color(NamedTextColor.DARK_GREEN));
                    event.line(3, Component.text(itemName).color(NamedTextColor.BLACK));

                    player.sendMessage(Component.text("Sign shop created!").color(NamedTextColor.GREEN));
                });
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create sign shop", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Failed to create shop!").color(NamedTextColor.RED));
                });
            }
        });
    }

    /**
     * Handle sign click
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign))
            return;

        SignShop shop = getShopAt(block.getLocation());
        if (shop == null)
            return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (shop.getShopType() == SignShop.ShopType.BUY) {
            processBuy(player, shop);
        } else {
            processSell(player, shop);
        }
    }

    /**
     * Process buy transaction
     */
    private void processBuy(Player player, SignShop shop) {
        int quantity = shop.getQuantity();
        double price = shop.getPrice();

        EconomyManager eco = EconomyManager.getInstance();

        // Check player has money
        if (!eco.has(player.getUniqueId(), price)) {
            player.sendMessage(Component.text("You don't have enough money! Need: " +
                    eco.format(price)).color(NamedTextColor.RED));
            return;
        }

        // Check inventory space
        Material mat = Material.getMaterial(shop.getItemType());
        if (mat == null) {
            player.sendMessage(Component.text("Item no longer exists!").color(NamedTextColor.RED));
            return;
        }

        // Process payment
        if (!shop.isAdmin()) {
            eco.withdraw(shop.getOwnerUuid(), price * (1 - ShopsConfig.signShopsTaxRate));
        }
        eco.withdraw(player.getUniqueId(), price);

        // Give items
        ItemStack item = new ItemStack(mat, quantity);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }

        // Log
        TransactionLog.getInstance().logShopBuy(
                player.getUniqueId(), shop.getOwnerUuid(), price, price * ShopsConfig.signShopsTaxRate,
                shop.getId(), shop.getItemType(), quantity,
                shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());

        player.sendMessage(Component.text("Purchased " + quantity + "x " + shop.getItemType() +
                " for " + eco.format(price)).color(NamedTextColor.GREEN));
    }

    /**
     * Process sell transaction
     */
    private void processSell(Player player, SignShop shop) {
        int quantity = shop.getQuantity();
        double price = shop.getPrice();

        EconomyManager eco = EconomyManager.getInstance();
        Material mat = Material.getMaterial(shop.getItemType());
        if (mat == null)
            return;

        ItemStack template = new ItemStack(mat);

        // Check player has items
        if (!hasItem(player, template, quantity)) {
            player.sendMessage(Component.text("You don't have enough items!").color(NamedTextColor.RED));
            return;
        }

        // Check shop owner has money (if not admin)
        if (!shop.isAdmin() && !eco.has(shop.getOwnerUuid(), price)) {
            player.sendMessage(Component.text("Shop owner cannot afford this purchase!").color(NamedTextColor.RED));
            return;
        }

        // Process
        removeItem(player, template, quantity);

        double tax = price * ShopsConfig.signShopsTaxRate;
        double sellerReceives = price - tax;

        eco.deposit(player.getUniqueId(), sellerReceives);
        if (!shop.isAdmin()) {
            eco.withdraw(shop.getOwnerUuid(), price);
        }

        // Log
        TransactionLog.getInstance().logShopSell(
                player.getUniqueId(), shop.getOwnerUuid(), price, tax,
                shop.getId(), shop.getItemType(), quantity,
                shop.getWorld(), shop.getX(), shop.getY(), shop.getZ());

        player.sendMessage(Component.text("Sold " + quantity + "x " + shop.getItemType() +
                " for " + eco.format(sellerReceives)).color(NamedTextColor.GREEN));
    }

    /**
     * Handle sign break
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign))
            return;

        SignShop shop = getShopAt(block.getLocation());
        if (shop == null)
            return;

        Player player = event.getPlayer();
        if (!shop.isOwner(player.getUniqueId()) && !player.hasPermission("vonixcore.shops.admin.remove")) {
            player.sendMessage(Component.text("You cannot remove this shop!").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        deleteShop(shop);
        player.sendMessage(Component.text("Sign shop removed.").color(NamedTextColor.YELLOW));
    }

    // === Helper methods ===

    public SignShop getShopAt(Location location) {
        String key = getLocationKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return shopsByLocation.get(key);
    }

    public int getPlayerShopCount(UUID owner) {
        List<Long> ids = shopsByOwner.get(owner);
        return ids != null ? ids.size() : 0;
    }

    private String getLocationKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
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

    // === Database ===

    private long insertShop(Connection conn, SignShop shop) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
                """
                        INSERT INTO vonixcore_sign_shops
                        (owner_uuid, owner_name, world, x, y, z, item_type, quantity, price, shop_type, is_admin, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, shop.getOwnerUuid().toString());
        stmt.setString(2, shop.getOwnerName());
        stmt.setString(3, shop.getWorld());
        stmt.setInt(4, shop.getX());
        stmt.setInt(5, shop.getY());
        stmt.setInt(6, shop.getZ());
        stmt.setString(7, shop.getItemType());
        stmt.setInt(8, shop.getQuantity());
        stmt.setDouble(9, shop.getPrice());
        stmt.setString(10, shop.getShopType().name());
        stmt.setInt(11, shop.isAdmin() ? 1 : 0);
        stmt.setLong(12, shop.getCreatedAt());
        stmt.executeUpdate();

        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next())
            return rs.getLong(1);
        throw new SQLException("Failed to get generated ID");
    }

    private void deleteShop(SignShop shop) {
        unregisterShop(shop);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM vonixcore_sign_shops WHERE id = ?");
                stmt.setLong(1, shop.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete sign shop", e);
            }
        });
    }

    private SignShop shopFromResultSet(ResultSet rs) throws SQLException {
        SignShop shop = new SignShop();
        shop.setId(rs.getLong("id"));
        shop.setOwnerUuid(UUID.fromString(rs.getString("owner_uuid")));
        shop.setOwnerName(rs.getString("owner_name"));
        shop.setWorld(rs.getString("world"));
        shop.setX(rs.getInt("x"));
        shop.setY(rs.getInt("y"));
        shop.setZ(rs.getInt("z"));
        shop.setItemType(rs.getString("item_type"));
        shop.setQuantity(rs.getInt("quantity"));
        shop.setPrice(rs.getDouble("price"));
        shop.setShopType(SignShop.ShopType.valueOf(rs.getString("shop_type")));
        shop.setAdmin(rs.getInt("is_admin") == 1);
        shop.setCreatedAt(rs.getLong("created_at"));
        return shop;
    }

    public void shutdown() {
        shopsByLocation.clear();
        shopsById.clear();
        shopsByOwner.clear();
    }

    public void reload() {
        // Config reload handled by ShopsConfig
    }
}
