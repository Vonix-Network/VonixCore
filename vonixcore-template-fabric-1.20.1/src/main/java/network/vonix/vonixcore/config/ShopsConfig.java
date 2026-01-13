package network.vonix.vonixcore.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shops configuration for VonixCore.
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
                # Admin shops, player shops, and sign shops
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("shops.enabled", true);

        // Admin shop settings
        setDefault("admin_shop.enabled", true);
        setDefault("admin_shop.title", "Server Shop");

        // Player shop settings
        setDefault("player_shop.enabled", true);
        setDefault("player_shop.max_per_player", 10);
        setDefault("player_shop.creation_cost", 100.0);
        setDefault("player_shop.tax_rate", 0.05);

        // Sign shop settings
        setDefault("sign_shop.enabled", true);
        setDefault("sign_shop.header_text", "[Shop]");

        // Default items (admin shop)
        List<Map<String, Object>> defaultItems = new ArrayList<>();

        Map<String, Object> diamondItem = new HashMap<>();
        diamondItem.put("item", "minecraft:diamond");
        diamondItem.put("buy_price", 100.0);
        diamondItem.put("sell_price", 50.0);
        defaultItems.add(diamondItem);

        Map<String, Object> ironItem = new HashMap<>();
        ironItem.put("item", "minecraft:iron_ingot");
        ironItem.put("buy_price", 10.0);
        ironItem.put("sell_price", 5.0);
        defaultItems.add(ironItem);

        Map<String, Object> goldItem = new HashMap<>();
        goldItem.put("item", "minecraft:gold_ingot");
        goldItem.put("buy_price", 20.0);
        goldItem.put("sell_price", 10.0);
        defaultItems.add(goldItem);

        Map<String, Object> breadItem = new HashMap<>();
        breadItem.put("item", "minecraft:bread");
        breadItem.put("buy_price", 5.0);
        breadItem.put("sell_price", 2.0);
        defaultItems.add(breadItem);

        setDefault("admin_shop.items", defaultItems);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("shops.enabled", true);
    }

    // Admin shop
    public boolean isAdminShopEnabled() {
        return getBoolean("admin_shop.enabled", true);
    }

    public String getAdminShopTitle() {
        return getString("admin_shop.title", "Server Shop");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAdminShopItems() {
        Object items = get("admin_shop.items", null);
        if (items instanceof List) {
            return (List<Map<String, Object>>) items;
        }
        return new ArrayList<>();
    }

    // Player shop
    public boolean isPlayerShopEnabled() {
        return getBoolean("player_shop.enabled", true);
    }

    public int getMaxPlayerShops() {
        return getInt("player_shop.max_per_player", 10);
    }

    public double getPlayerShopCreationCost() {
        return getDouble("player_shop.creation_cost", 100.0);
    }

    public double getPlayerShopTaxRate() {
        return getDouble("player_shop.tax_rate", 0.05);
    }

    // Sign shop
    public boolean isSignShopEnabled() {
        return getBoolean("sign_shop.enabled", true);
    }

    public String getSignShopHeader() {
        return getString("sign_shop.header_text", "[Shop]");
    }
}
