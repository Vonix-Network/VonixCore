package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * Configuration for the Shops module.
 * Covers: Chest Shops, Sign Shops, GUI Shop, Player Market
 * Stored in config/vonixcore-shops.yml
 */
public class ShopsConfig extends BaseConfig {

    private static ShopsConfig instance;

    public static ShopsConfig getInstance() {
        if (instance == null) {
            instance = new ShopsConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().loadConfig(configDir);
    }

    private ShopsConfig() {
        super("vonixcore-shops.yml");
    }

    private void loadConfig(Path configDir) {
        super.load(configDir);
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore Shops Configuration
                # Chest Shops, Sign Shops, GUI Shop, and Player Market
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("shops.enabled", true);

        // Chest Shops Section
        setDefault("chest_shops.enabled", true);
        setDefault("chest_shops.max_per_player", 50);
        setDefault("chest_shops.tax_rate", 0.05);
        setDefault("chest_shops.admin_only_create", false);
        setDefault("chest_shops.allow_buy_type", true);
        setDefault("chest_shops.allow_sell_type", true);
        setDefault("chest_shops.display_items", true);
        setDefault("chest_shops.protect_chests", true);
        setDefault("chest_shops.require_chest_access", true);
        setDefault("chest_shops.min_price", 0.01);
        setDefault("chest_shops.max_price", 1000000000.0);
        setDefault("chest_shops.allow_free_shops", false);
        setDefault("chest_shops.find_distance", 45);
        setDefault("chest_shops.show_out_of_stock", true);

        // Sign Shops Section
        setDefault("sign_shops.enabled", true);
        setDefault("sign_shops.max_per_player", 25);
        setDefault("sign_shops.require_permission", false);
        setDefault("sign_shops.tax_rate", 0.05);

        // GUI Shop Section
        setDefault("gui_shop.enabled", true);
        setDefault("gui_shop.menu_title", "§6§lServer Shop");
        setDefault("gui_shop.sell_all_enabled", true);
        setDefault("gui_shop.sell_all_tax_rate", 0.0);

        // Player Market Section
        setDefault("player_market.enabled", true);
        setDefault("player_market.max_listings", 10);
        setDefault("player_market.listing_duration_hours", 168);
        setDefault("player_market.tax_rate", 0.10);
        setDefault("player_market.min_price", 1.0);
        setDefault("player_market.max_price", 1000000.0);
        setDefault("player_market.notify_on_sale", true);

        // Transaction Logging
        setDefault("transactions.log_enabled", true);
        setDefault("transactions.retention_days", 30);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("shops.enabled", true);
    }

    // Chest Shops
    public boolean isChestShopsEnabled() {
        return getBoolean("chest_shops.enabled", true);
    }

    public int getChestShopsMaxPerPlayer() {
        return getInt("chest_shops.max_per_player", 50);
    }

    public double getChestShopsTaxRate() {
        return getDouble("chest_shops.tax_rate", 0.05);
    }

    public boolean isChestShopsAdminOnlyCreate() {
        return getBoolean("chest_shops.admin_only_create", false);
    }

    public boolean isChestShopsAllowBuyType() {
        return getBoolean("chest_shops.allow_buy_type", true);
    }

    public boolean isChestShopsAllowSellType() {
        return getBoolean("chest_shops.allow_sell_type", true);
    }

    public boolean isChestShopsDisplayItems() {
        return getBoolean("chest_shops.display_items", true);
    }

    public boolean isChestShopsProtectChests() {
        return getBoolean("chest_shops.protect_chests", true);
    }

    public boolean isChestShopsRequireChestAccess() {
        return getBoolean("chest_shops.require_chest_access", true);
    }

    public double getChestShopsMinPrice() {
        return getDouble("chest_shops.min_price", 0.01);
    }

    public double getChestShopsMaxPrice() {
        return getDouble("chest_shops.max_price", 1000000000.0);
    }

    public boolean isChestShopsAllowFreeShops() {
        return getBoolean("chest_shops.allow_free_shops", false);
    }

    public int getChestShopsFindDistance() {
        return getInt("chest_shops.find_distance", 45);
    }

    public boolean isChestShopsShowOutOfStock() {
        return getBoolean("chest_shops.show_out_of_stock", true);
    }

    // Sign Shops
    public boolean isSignShopsEnabled() {
        return getBoolean("sign_shops.enabled", true);
    }

    public int getSignShopsMaxPerPlayer() {
        return getInt("sign_shops.max_per_player", 25);
    }

    public boolean isSignShopsRequirePermission() {
        return getBoolean("sign_shops.require_permission", false);
    }

    public double getSignShopsTaxRate() {
        return getDouble("sign_shops.tax_rate", 0.05);
    }

    // GUI Shop
    public boolean isGuiShopEnabled() {
        return getBoolean("gui_shop.enabled", true);
    }

    public String getGuiShopMenuTitle() {
        return getString("gui_shop.menu_title", "§6§lServer Shop");
    }

    public boolean isGuiShopSellAllEnabled() {
        return getBoolean("gui_shop.sell_all_enabled", true);
    }

    public double getGuiShopSellAllTaxRate() {
        return getDouble("gui_shop.sell_all_tax_rate", 0.0);
    }

    // Player Market
    public boolean isPlayerMarketEnabled() {
        return getBoolean("player_market.enabled", true);
    }

    public int getPlayerMarketMaxListings() {
        return getInt("player_market.max_listings", 10);
    }

    public int getPlayerMarketListingDurationHours() {
        return getInt("player_market.listing_duration_hours", 168);
    }

    public double getPlayerMarketTaxRate() {
        return getDouble("player_market.tax_rate", 0.10);
    }

    public double getPlayerMarketMinPrice() {
        return getDouble("player_market.min_price", 1.0);
    }

    public double getPlayerMarketMaxPrice() {
        return getDouble("player_market.max_price", 1000000.0);
    }

    public boolean isPlayerMarketNotifyOnSale() {
        return getBoolean("player_market.notify_on_sale", true);
    }

    // Transactions
    public boolean isTransactionLogEnabled() {
        return getBoolean("transactions.log_enabled", true);
    }

    public int getTransactionLogRetentionDays() {
        return getInt("transactions.retention_days", 30);
    }
}
