package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the Shops module
 * Covers: Chest Shops, Sign Shops, GUI Shop, Player Market
 */
public class ShopsConfig {

    private static FileConfiguration config;
    private static File configFile;
    private static final Logger logger = Logger.getLogger("VonixCore");

    // Master toggle
    public static boolean enabled = true;

    // Chest Shop settings
    public static boolean chestShopsEnabled = true;
    public static int chestShopsMaxPerPlayer = 50;
    public static double chestShopsTaxRate = 0.05; // 5%
    public static boolean chestShopsAdminOnlyCreate = false;
    public static boolean chestShopsAllowBuyType = true;
    public static boolean chestShopsAllowSellType = true;
    public static boolean chestShopsDisplayItems = true;
    public static boolean chestShopsProtectChests = true;
    public static boolean chestShopsRequireChestAccess = true;
    public static double chestShopsMinPrice = 0.01;
    public static double chestShopsMaxPrice = 1000000000.0;
    public static boolean chestShopsAllowFreeShops = false;
    public static int chestShopsFindDistance = 45;
    public static boolean chestShopsShowOutOfStock = true;

    // Sign Shop settings
    public static boolean signShopsEnabled = true;
    public static int signShopsMaxPerPlayer = 25;
    public static boolean signShopsRequirePermission = false;
    public static double signShopsTaxRate = 0.05;

    // GUI Shop settings (Server Admin Shop)
    public static boolean guiShopEnabled = true;
    public static String guiShopMenuTitle = "&6&lServer Shop";
    public static boolean guiShopSellAllEnabled = true;
    public static double guiShopSellAllTaxRate = 0.0;

    // Player Market settings
    public static boolean playerMarketEnabled = true;
    public static int playerMarketMaxListings = 10;
    public static int playerMarketListingDurationHours = 168; // 1 week
    public static double playerMarketTaxRate = 0.10; // 10%
    public static double playerMarketMinPrice = 1.0;
    public static double playerMarketMaxPrice = 1000000.0;
    public static boolean playerMarketNotifyOnSale = true;

    // Transaction logging
    public static boolean transactionLogEnabled = true;
    public static int transactionLogRetentionDays = 30;

    public static void load(File dataFolder) {
        configFile = new File(dataFolder, "shops.yml");

        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            try {
                dataFolder.mkdirs();
                InputStream defaultConfig = ShopsConfig.class.getResourceAsStream("/shops.yml");
                if (defaultConfig != null) {
                    Files.copy(defaultConfig, configFile.toPath());
                } else {
                    // Create default config programmatically
                    createDefaultConfig();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to create shops.yml, using defaults", e);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }

    private static void createDefaultConfig() {
        try {
            configFile.createNewFile();
            config = YamlConfiguration.loadConfiguration(configFile);

            // Master toggle
            config.set("enabled", true);

            // Chest Shops
            config.set("chest-shops.enabled", true);
            config.set("chest-shops.max-per-player", 50);
            config.set("chest-shops.tax-rate", 0.05);
            config.set("chest-shops.admin-only-create", false);
            config.set("chest-shops.allow-buy-type", true);
            config.set("chest-shops.allow-sell-type", true);
            config.set("chest-shops.display-items", true);
            config.set("chest-shops.protect-chests", true);
            config.set("chest-shops.require-chest-access", true);
            config.set("chest-shops.min-price", 0.01);
            config.set("chest-shops.max-price", 1000000000.0);
            config.set("chest-shops.allow-free-shops", false);
            config.set("chest-shops.find-distance", 45);
            config.set("chest-shops.show-out-of-stock", true);

            // Sign Shops
            config.set("sign-shops.enabled", true);
            config.set("sign-shops.max-per-player", 25);
            config.set("sign-shops.require-permission", false);
            config.set("sign-shops.tax-rate", 0.05);

            // GUI Shop
            config.set("gui-shop.enabled", true);
            config.set("gui-shop.menu-title", "&6&lServer Shop");
            config.set("gui-shop.sell-all-enabled", true);
            config.set("gui-shop.sell-all-tax-rate", 0.0);

            // Player Market
            config.set("player-market.enabled", true);
            config.set("player-market.max-listings", 10);
            config.set("player-market.listing-duration-hours", 168);
            config.set("player-market.tax-rate", 0.10);
            config.set("player-market.min-price", 1.0);
            config.set("player-market.max-price", 1000000.0);
            config.set("player-market.notify-on-sale", true);

            // Transaction logging
            config.set("transactions.log-enabled", true);
            config.set("transactions.retention-days", 30);

            config.save(configFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create default shops.yml", e);
        }
    }

    private static void loadValues() {
        // Master toggle
        enabled = config.getBoolean("enabled", true);

        // Chest Shops
        chestShopsEnabled = config.getBoolean("chest-shops.enabled", true);
        chestShopsMaxPerPlayer = config.getInt("chest-shops.max-per-player", 50);
        chestShopsTaxRate = config.getDouble("chest-shops.tax-rate", 0.05);
        chestShopsAdminOnlyCreate = config.getBoolean("chest-shops.admin-only-create", false);
        chestShopsAllowBuyType = config.getBoolean("chest-shops.allow-buy-type", true);
        chestShopsAllowSellType = config.getBoolean("chest-shops.allow-sell-type", true);
        chestShopsDisplayItems = config.getBoolean("chest-shops.display-items", true);
        chestShopsProtectChests = config.getBoolean("chest-shops.protect-chests", true);
        chestShopsRequireChestAccess = config.getBoolean("chest-shops.require-chest-access", true);
        chestShopsMinPrice = config.getDouble("chest-shops.min-price", 0.01);
        chestShopsMaxPrice = config.getDouble("chest-shops.max-price", 1000000000.0);
        chestShopsAllowFreeShops = config.getBoolean("chest-shops.allow-free-shops", false);
        chestShopsFindDistance = config.getInt("chest-shops.find-distance", 45);
        chestShopsShowOutOfStock = config.getBoolean("chest-shops.show-out-of-stock", true);

        // Sign Shops
        signShopsEnabled = config.getBoolean("sign-shops.enabled", true);
        signShopsMaxPerPlayer = config.getInt("sign-shops.max-per-player", 25);
        signShopsRequirePermission = config.getBoolean("sign-shops.require-permission", false);
        signShopsTaxRate = config.getDouble("sign-shops.tax-rate", 0.05);

        // GUI Shop
        guiShopEnabled = config.getBoolean("gui-shop.enabled", true);
        guiShopMenuTitle = config.getString("gui-shop.menu-title", "&6&lServer Shop");
        guiShopSellAllEnabled = config.getBoolean("gui-shop.sell-all-enabled", true);
        guiShopSellAllTaxRate = config.getDouble("gui-shop.sell-all-tax-rate", 0.0);

        // Player Market
        playerMarketEnabled = config.getBoolean("player-market.enabled", true);
        playerMarketMaxListings = config.getInt("player-market.max-listings", 10);
        playerMarketListingDurationHours = config.getInt("player-market.listing-duration-hours", 168);
        playerMarketTaxRate = config.getDouble("player-market.tax-rate", 0.10);
        playerMarketMinPrice = config.getDouble("player-market.min-price", 1.0);
        playerMarketMaxPrice = config.getDouble("player-market.max-price", 1000000.0);
        playerMarketNotifyOnSale = config.getBoolean("player-market.notify-on-sale", true);

        // Transaction logging
        transactionLogEnabled = config.getBoolean("transactions.log-enabled", true);
        transactionLogRetentionDays = config.getInt("transactions.retention-days", 30);
    }

    public static void reload() {
        if (configFile != null) {
            config = YamlConfiguration.loadConfiguration(configFile);
            loadValues();
        }
    }

    public static FileConfiguration getConfig() {
        return config;
    }
}
