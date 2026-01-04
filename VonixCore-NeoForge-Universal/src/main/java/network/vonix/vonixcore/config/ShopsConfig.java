package network.vonix.vonixcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuration for the Shops module.
 * Covers: Chest Shops, Sign Shops, GUI Shop, Player Market
 * Stored in config/vonixcore-shops.toml
 */
public class ShopsConfig {

    public static final ModConfigSpec SPEC;
    public static final ShopsConfig CONFIG;

    // Master toggle
    public final ModConfigSpec.BooleanValue enabled;

    // Chest Shop settings
    public final ModConfigSpec.BooleanValue chestShopsEnabled;
    public final ModConfigSpec.IntValue chestShopsMaxPerPlayer;
    public final ModConfigSpec.DoubleValue chestShopsTaxRate;
    public final ModConfigSpec.BooleanValue chestShopsAdminOnlyCreate;
    public final ModConfigSpec.BooleanValue chestShopsAllowBuyType;
    public final ModConfigSpec.BooleanValue chestShopsAllowSellType;
    public final ModConfigSpec.BooleanValue chestShopsDisplayItems;
    public final ModConfigSpec.BooleanValue chestShopsProtectChests;
    public final ModConfigSpec.BooleanValue chestShopsRequireChestAccess;
    public final ModConfigSpec.DoubleValue chestShopsMinPrice;
    public final ModConfigSpec.DoubleValue chestShopsMaxPrice;
    public final ModConfigSpec.BooleanValue chestShopsAllowFreeShops;
    public final ModConfigSpec.IntValue chestShopsFindDistance;
    public final ModConfigSpec.BooleanValue chestShopsShowOutOfStock;

    // Sign Shop settings
    public final ModConfigSpec.BooleanValue signShopsEnabled;
    public final ModConfigSpec.IntValue signShopsMaxPerPlayer;
    public final ModConfigSpec.BooleanValue signShopsRequirePermission;
    public final ModConfigSpec.DoubleValue signShopsTaxRate;

    // GUI Shop settings (Server Admin Shop)
    public final ModConfigSpec.BooleanValue guiShopEnabled;
    public final ModConfigSpec.ConfigValue<String> guiShopMenuTitle;
    public final ModConfigSpec.BooleanValue guiShopSellAllEnabled;
    public final ModConfigSpec.DoubleValue guiShopSellAllTaxRate;

    // Player Market settings
    public final ModConfigSpec.BooleanValue playerMarketEnabled;
    public final ModConfigSpec.IntValue playerMarketMaxListings;
    public final ModConfigSpec.IntValue playerMarketListingDurationHours;
    public final ModConfigSpec.DoubleValue playerMarketTaxRate;
    public final ModConfigSpec.DoubleValue playerMarketMinPrice;
    public final ModConfigSpec.DoubleValue playerMarketMaxPrice;
    public final ModConfigSpec.BooleanValue playerMarketNotifyOnSale;

    // Transaction logging
    public final ModConfigSpec.BooleanValue transactionLogEnabled;
    public final ModConfigSpec.IntValue transactionLogRetentionDays;

    static {
        Pair<ShopsConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(ShopsConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private ShopsConfig(ModConfigSpec.Builder builder) {
        builder.comment(
                "VonixCore Shops Configuration",
                "Chest Shops, Sign Shops, GUI Shop, and Player Market")
                .push("shops");

        enabled = builder.comment(
                "Enable the shops module",
                "Set to false to completely disable all shop features")
                .define("enabled", true);

        // Chest Shops Section
        builder.pop().comment(
                "Chest Shop Settings",
                "Player-created shops using chests")
                .push("chest_shops");

        chestShopsEnabled = builder.comment("Enable chest shop system")
                .define("enabled", true);

        chestShopsMaxPerPlayer = builder.comment("Maximum chest shops per player")
                .defineInRange("max_per_player", 50, 1, 500);

        chestShopsTaxRate = builder.comment("Tax rate on chest shop transactions (0.05 = 5%)")
                .defineInRange("tax_rate", 0.05, 0.0, 1.0);

        chestShopsAdminOnlyCreate = builder.comment("Only admins can create chest shops")
                .define("admin_only_create", false);

        chestShopsAllowBuyType = builder.comment("Allow 'buy' type chest shops (players buy from shop)")
                .define("allow_buy_type", true);

        chestShopsAllowSellType = builder.comment("Allow 'sell' type chest shops (players sell to shop)")
                .define("allow_sell_type", true);

        chestShopsDisplayItems = builder.comment("Display floating items above chest shops")
                .define("display_items", true);

        chestShopsProtectChests = builder.comment("Protect chest shop chests from breaking")
                .define("protect_chests", true);

        chestShopsRequireChestAccess = builder.comment("Require chest access to create a shop on it")
                .define("require_chest_access", true);

        chestShopsMinPrice = builder.comment("Minimum price for items in chest shops")
                .defineInRange("min_price", 0.01, 0.0, 1000000.0);

        chestShopsMaxPrice = builder.comment("Maximum price for items in chest shops")
                .defineInRange("max_price", 1000000000.0, 1.0, Double.MAX_VALUE);

        chestShopsAllowFreeShops = builder.comment("Allow shops with price of 0")
                .define("allow_free_shops", false);

        chestShopsFindDistance = builder.comment("Distance to search for nearby shops with /shop find")
                .defineInRange("find_distance", 45, 10, 200);

        chestShopsShowOutOfStock = builder.comment("Show out of stock message on empty shops")
                .define("show_out_of_stock", true);

        // Sign Shops Section
        builder.pop().comment(
                "Sign Shop Settings",
                "Shops created with signs")
                .push("sign_shops");

        signShopsEnabled = builder.comment("Enable sign shop system")
                .define("enabled", true);

        signShopsMaxPerPlayer = builder.comment("Maximum sign shops per player")
                .defineInRange("max_per_player", 25, 1, 250);

        signShopsRequirePermission = builder.comment("Require permission to create sign shops")
                .define("require_permission", false);

        signShopsTaxRate = builder.comment("Tax rate on sign shop transactions (0.05 = 5%)")
                .defineInRange("tax_rate", 0.05, 0.0, 1.0);

        // GUI Shop Section
        builder.pop().comment(
                "GUI Shop Settings",
                "Server admin shop with GUI interface")
                .push("gui_shop");

        guiShopEnabled = builder.comment("Enable GUI shop system")
                .define("enabled", true);

        guiShopMenuTitle = builder.comment("Title for the GUI shop menu")
                .define("menu_title", "§6§lServer Shop");

        guiShopSellAllEnabled = builder.comment("Enable 'sell all' button in GUI shop")
                .define("sell_all_enabled", true);

        guiShopSellAllTaxRate = builder.comment("Tax rate on 'sell all' transactions")
                .defineInRange("sell_all_tax_rate", 0.0, 0.0, 1.0);

        // Player Market Section
        builder.pop().comment(
                "Player Market Settings",
                "Global auction house / player market")
                .push("player_market");

        playerMarketEnabled = builder.comment("Enable player market system")
                .define("enabled", true);

        playerMarketMaxListings = builder.comment("Maximum active listings per player")
                .defineInRange("max_listings", 10, 1, 100);

        playerMarketListingDurationHours = builder.comment("How long listings stay active (hours)")
                .defineInRange("listing_duration_hours", 168, 1, 720);

        playerMarketTaxRate = builder.comment("Tax rate on market sales (0.10 = 10%)")
                .defineInRange("tax_rate", 0.10, 0.0, 1.0);

        playerMarketMinPrice = builder.comment("Minimum listing price")
                .defineInRange("min_price", 1.0, 0.0, 1000000.0);

        playerMarketMaxPrice = builder.comment("Maximum listing price")
                .defineInRange("max_price", 1000000.0, 1.0, Double.MAX_VALUE);

        playerMarketNotifyOnSale = builder.comment("Notify sellers when their items sell")
                .define("notify_on_sale", true);

        // Transaction Logging Section
        builder.pop().comment(
                "Transaction Logging",
                "Log all shop transactions")
                .push("transactions");

        transactionLogEnabled = builder.comment("Enable transaction logging")
                .define("log_enabled", true);

        transactionLogRetentionDays = builder.comment("Days to keep transaction logs")
                .defineInRange("retention_days", 30, 1, 365);

        builder.pop();
    }
}
