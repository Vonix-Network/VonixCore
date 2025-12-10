package network.vonix.vonixcore.shops.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.TransactionLog;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * Server GUI Shop - Admin-configured shop with categories and fixed prices.
 * Players use /shop to open a menu to buy/sell items.
 */
public class ServerShopManager implements Listener {

    private final VonixCore plugin;

    // Categories and items
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    // Track open inventories
    private final Map<UUID, ShopSession> openSessions = new HashMap<>();

    public ServerShopManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadShopConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Load shop configuration from YAML
     */
    private void loadShopConfig() {
        File configFile = new File(plugin.getDataFolder(), "shop-items.yml");

        if (!configFile.exists()) {
            createDefaultShopConfig(configFile);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        categories.clear();

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null)
            return;

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection catSection = categoriesSection.getConfigurationSection(categoryId);
            if (catSection == null)
                continue;

            ShopCategory category = new ShopCategory();
            category.id = categoryId;
            category.name = ChatColor.translateAlternateColorCodes('&', catSection.getString("name", categoryId));

            String iconName = catSection.getString("icon", "CHEST");
            category.icon = Material.getMaterial(iconName);
            if (category.icon == null)
                category.icon = Material.CHEST;

            // Load items
            List<Map<?, ?>> itemsList = catSection.getMapList("items");
            for (Map<?, ?> itemMap : itemsList) {
                ShopItem item = new ShopItem();
                item.type = Material.getMaterial((String) itemMap.get("type"));
                if (item.type == null)
                    item.type = Material.STONE;

                Object buyObj = itemMap.get("buy");
                item.buyPrice = buyObj != null ? ((Number) buyObj).doubleValue() : -1;
                Object sellObj = itemMap.get("sell");
                item.sellPrice = sellObj != null ? ((Number) sellObj).doubleValue() : -1;
                item.displayName = itemMap.containsKey("name")
                        ? ChatColor.translateAlternateColorCodes('&', (String) itemMap.get("name"))
                        : null;

                category.items.add(item);
            }

            categories.put(categoryId, category);
        }

        plugin.getLogger().info("[Shops] Loaded " + categories.size() + " shop categories");
    }

    /**
     * Create default shop configuration
     */
    private void createDefaultShopConfig(File file) {
        try {
            file.getParentFile().mkdirs();

            FileConfiguration config = new YamlConfiguration();

            // Tools category
            ConfigurationSection tools = config.createSection("categories.tools");
            tools.set("name", "&6Tools");
            tools.set("icon", "DIAMOND_PICKAXE");
            List<Map<String, Object>> toolItems = new ArrayList<>();
            toolItems.add(Map.of("type", "WOODEN_PICKAXE", "buy", 10, "sell", 2));
            toolItems.add(Map.of("type", "STONE_PICKAXE", "buy", 25, "sell", 5));
            toolItems.add(Map.of("type", "IRON_PICKAXE", "buy", 100, "sell", 25));
            toolItems.add(Map.of("type", "DIAMOND_PICKAXE", "buy", 500, "sell", 100));
            toolItems.add(Map.of("type", "WOODEN_AXE", "buy", 10, "sell", 2));
            toolItems.add(Map.of("type", "STONE_AXE", "buy", 25, "sell", 5));
            toolItems.add(Map.of("type", "IRON_AXE", "buy", 100, "sell", 25));
            toolItems.add(Map.of("type", "DIAMOND_AXE", "buy", 400, "sell", 80));
            tools.set("items", toolItems);

            // Blocks category
            ConfigurationSection blocks = config.createSection("categories.blocks");
            blocks.set("name", "&aBlocks");
            blocks.set("icon", "GRASS_BLOCK");
            List<Map<String, Object>> blockItems = new ArrayList<>();
            blockItems.add(Map.of("type", "COBBLESTONE", "buy", 1, "sell", 0.5));
            blockItems.add(Map.of("type", "STONE", "buy", 2, "sell", 1));
            blockItems.add(Map.of("type", "DIRT", "buy", 1, "sell", 0.25));
            blockItems.add(Map.of("type", "SAND", "buy", 2, "sell", 0.5));
            blockItems.add(Map.of("type", "GRAVEL", "buy", 2, "sell", 0.5));
            blockItems.add(Map.of("type", "OAK_LOG", "buy", 5, "sell", 2));
            blockItems.add(Map.of("type", "GLASS", "buy", 3, "sell", 1));
            blocks.set("items", blockItems);

            // Food category
            ConfigurationSection food = config.createSection("categories.food");
            food.set("name", "&cFood");
            food.set("icon", "COOKED_BEEF");
            List<Map<String, Object>> foodItems = new ArrayList<>();
            foodItems.add(Map.of("type", "BREAD", "buy", 5, "sell", 2));
            foodItems.add(Map.of("type", "COOKED_BEEF", "buy", 10, "sell", 4));
            foodItems.add(Map.of("type", "COOKED_PORKCHOP", "buy", 10, "sell", 4));
            foodItems.add(Map.of("type", "APPLE", "buy", 5, "sell", 1));
            foodItems.add(Map.of("type", "GOLDEN_APPLE", "buy", 500, "sell", 100));
            food.set("items", foodItems);

            // Ores category
            ConfigurationSection ores = config.createSection("categories.ores");
            ores.set("name", "&bOres & Minerals");
            ores.set("icon", "DIAMOND");
            List<Map<String, Object>> oreItems = new ArrayList<>();
            oreItems.add(Map.of("type", "COAL", "buy", 5, "sell", 2));
            oreItems.add(Map.of("type", "IRON_INGOT", "buy", 20, "sell", 10));
            oreItems.add(Map.of("type", "GOLD_INGOT", "buy", 50, "sell", 25));
            oreItems.add(Map.of("type", "DIAMOND", "buy", 200, "sell", 100));
            oreItems.add(Map.of("type", "EMERALD", "buy", 150, "sell", 75));
            oreItems.add(Map.of("type", "LAPIS_LAZULI", "buy", 10, "sell", 5));
            oreItems.add(Map.of("type", "REDSTONE", "buy", 5, "sell", 2));
            ores.set("items", oreItems);

            // Mob drops category
            ConfigurationSection mobDrops = config.createSection("categories.mob_drops");
            mobDrops.set("name", "&5Mob Drops");
            mobDrops.set("icon", "ROTTEN_FLESH");
            List<Map<String, Object>> mobItems = new ArrayList<>();
            mobItems.add(Map.of("type", "ROTTEN_FLESH", "buy", -1, "sell", 1));
            mobItems.add(Map.of("type", "BONE", "buy", 5, "sell", 2));
            mobItems.add(Map.of("type", "STRING", "buy", 5, "sell", 2));
            mobItems.add(Map.of("type", "SPIDER_EYE", "buy", 10, "sell", 3));
            mobItems.add(Map.of("type", "GUNPOWDER", "buy", 15, "sell", 5));
            mobItems.add(Map.of("type", "ENDER_PEARL", "buy", 100, "sell", 25));
            mobItems.add(Map.of("type", "BLAZE_ROD", "buy", 50, "sell", 15));
            mobItems.add(Map.of("type", "GHAST_TEAR", "buy", 200, "sell", 50));
            mobDrops.set("items", mobItems);

            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create shop config", e);
        }
    }

    /**
     * Open the main shop menu for a player
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(ChatColor.translateAlternateColorCodes('&', ShopsConfig.guiShopMenuTitle)));

        // Add category items
        int slot = 10;
        for (ShopCategory category : categories.values()) {
            if (slot > 16)
                break; // Max 7 categories in center row

            ItemStack item = new ItemStack(category.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(category.name).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(category.items.size() + " items").color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click to browse").color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        // Add sell all button
        if (ShopsConfig.guiShopSellAllEnabled) {
            ItemStack sellAll = new ItemStack(Material.HOPPER);
            ItemMeta meta = sellAll.getItemMeta();
            meta.displayName(Component.text("Sell All").color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Sell all sellable items").color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("in your inventory").color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            sellAll.setItemMeta(meta);
            inv.setItem(22, sellAll);
        }

        // Track session
        ShopSession session = new ShopSession();
        session.currentView = "main";
        openSessions.put(player.getUniqueId(), session);

        player.openInventory(inv);
    }

    /**
     * Open a category menu
     */
    private void openCategoryMenu(Player player, ShopCategory category) {
        int size = Math.min(54, ((category.items.size() / 9) + 1) * 9 + 9);
        Inventory inv = Bukkit.createInventory(null, size,
                Component.text(category.name));

        // Add items
        for (int i = 0; i < category.items.size() && i < size - 9; i++) {
            ShopItem shopItem = category.items.get(i);
            ItemStack display = createDisplayItem(shopItem);
            inv.setItem(i, display);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(Component.text("Back").color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(meta);
        inv.setItem(size - 5, back);

        // Update session
        ShopSession session = openSessions.get(player.getUniqueId());
        if (session == null)
            session = new ShopSession();
        session.currentView = "category";
        session.currentCategory = category;
        openSessions.put(player.getUniqueId(), session);

        player.openInventory(inv);
    }

    /**
     * Create display item with buy/sell info
     */
    private ItemStack createDisplayItem(ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.type);
        ItemMeta meta = item.getItemMeta();

        if (shopItem.displayName != null) {
            meta.displayName(Component.text(shopItem.displayName)
                    .decoration(TextDecoration.ITALIC, false));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (shopItem.buyPrice > 0) {
            lore.add(Component.text("Buy: ").color(NamedTextColor.GREEN)
                    .append(Component.text(EconomyManager.getInstance().format(shopItem.buyPrice))
                            .color(NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Buy: ").color(NamedTextColor.GREEN)
                    .append(Component.text("Not available").color(NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (shopItem.sellPrice > 0) {
            lore.add(Component.text("Sell: ").color(NamedTextColor.GOLD)
                    .append(Component.text(EconomyManager.getInstance().format(shopItem.sellPrice))
                            .color(NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Sell: ").color(NamedTextColor.GOLD)
                    .append(Component.text("Not available").color(NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Left-click: Buy 1").color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Left: Buy stack").color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right-click: Sell 1").color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Right: Sell all").color(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ShopSession session = openSessions.get(player.getUniqueId());
        if (session == null)
            return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        if (session.currentView.equals("main")) {
            handleMainMenuClick(player, event.getSlot(), clicked);
        } else if (session.currentView.equals("category")) {
            handleCategoryClick(player, event.getSlot(), clicked, event.getClick(), session);
        }
    }

    private void handleMainMenuClick(Player player, int slot, ItemStack clicked) {
        // Check for sell all
        if (clicked.getType() == Material.HOPPER && slot == 22) {
            processSellAll(player);
            return;
        }

        // Find category by slot
        int catIndex = slot - 10;
        if (catIndex >= 0 && catIndex < categories.size()) {
            ShopCategory category = new ArrayList<>(categories.values()).get(catIndex);
            openCategoryMenu(player, category);
        }
    }

    private void handleCategoryClick(Player player, int slot, ItemStack clicked,
            ClickType clickType, ShopSession session) {
        // Back button
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }

        ShopCategory category = session.currentCategory;
        if (category == null || slot >= category.items.size())
            return;

        ShopItem shopItem = category.items.get(slot);
        EconomyManager eco = EconomyManager.getInstance();

        if (clickType == ClickType.LEFT) {
            // Buy 1
            if (shopItem.buyPrice > 0) {
                processBuy(player, shopItem, 1);
            }
        } else if (clickType == ClickType.SHIFT_LEFT) {
            // Buy stack
            if (shopItem.buyPrice > 0) {
                processBuy(player, shopItem, shopItem.type.getMaxStackSize());
            }
        } else if (clickType == ClickType.RIGHT) {
            // Sell 1
            if (shopItem.sellPrice > 0) {
                processSell(player, shopItem, 1);
            }
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            // Sell all
            if (shopItem.sellPrice > 0) {
                int count = countItems(player, shopItem.type);
                if (count > 0) {
                    processSell(player, shopItem, count);
                }
            }
        }
    }

    private void processBuy(Player player, ShopItem item, int amount) {
        EconomyManager eco = EconomyManager.getInstance();
        double totalCost = item.buyPrice * amount;

        if (!eco.has(player.getUniqueId(), totalCost)) {
            player.sendMessage(Component.text("Not enough money! Need: " + eco.format(totalCost))
                    .color(NamedTextColor.RED));
            return;
        }

        eco.withdraw(player.getUniqueId(), totalCost);

        ItemStack purchasedItem = new ItemStack(item.type, amount);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(purchasedItem);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }

        TransactionLog.getInstance().log(new TransactionLog.TransactionRecord(
                player.getUniqueId(), null, totalCost,
                TransactionLog.TransactionType.GUI_SHOP_BUY,
                "Bought " + amount + "x " + item.type.name()));

        player.sendMessage(Component.text("Purchased " + amount + "x " + item.type.name() +
                " for " + eco.format(totalCost)).color(NamedTextColor.GREEN));
    }

    private void processSell(Player player, ShopItem item, int amount) {
        int available = countItems(player, item.type);
        int toSell = Math.min(amount, available);

        if (toSell <= 0) {
            player.sendMessage(Component.text("You don't have any " + item.type.name() + " to sell!")
                    .color(NamedTextColor.RED));
            return;
        }

        EconomyManager eco = EconomyManager.getInstance();
        double totalEarnings = item.sellPrice * toSell;

        removeItems(player, item.type, toSell);
        eco.deposit(player.getUniqueId(), totalEarnings);

        TransactionLog.getInstance().log(new TransactionLog.TransactionRecord(
                player.getUniqueId(), null, totalEarnings,
                TransactionLog.TransactionType.GUI_SHOP_SELL,
                "Sold " + toSell + "x " + item.type.name()));

        player.sendMessage(Component.text("Sold " + toSell + "x " + item.type.name() +
                " for " + eco.format(totalEarnings)).color(NamedTextColor.GREEN));
    }

    private void processSellAll(Player player) {
        double totalEarnings = 0;
        int totalItems = 0;

        for (ShopCategory category : categories.values()) {
            for (ShopItem item : category.items) {
                if (item.sellPrice <= 0)
                    continue;

                int count = countItems(player, item.type);
                if (count > 0) {
                    removeItems(player, item.type, count);
                    double earnings = item.sellPrice * count;
                    totalEarnings += earnings;
                    totalItems += count;
                }
            }
        }

        if (totalItems > 0) {
            EconomyManager eco = EconomyManager.getInstance();
            eco.deposit(player.getUniqueId(), totalEarnings);

            TransactionLog.getInstance().log(new TransactionLog.TransactionRecord(
                    player.getUniqueId(), null, totalEarnings,
                    TransactionLog.TransactionType.GUI_SHOP_SELL,
                    "Sell All: " + totalItems + " items"));

            player.sendMessage(Component.text("Sold " + totalItems + " items for " +
                    eco.format(totalEarnings)).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("No sellable items in your inventory!")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private int countItems(Player player, Material type) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == type) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material type, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == type) {
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openSessions.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        openSessions.clear();
    }

    public void reload() {
        loadShopConfig();
    }

    // Data classes
    private static class ShopCategory {
        String id;
        String name;
        Material icon;
        List<ShopItem> items = new ArrayList<>();
    }

    private static class ShopItem {
        Material type;
        double buyPrice = -1;
        double sellPrice = -1;
        String displayName;
    }

    private static class ShopSession {
        String currentView = "main";
        ShopCategory currentCategory;
    }
}
