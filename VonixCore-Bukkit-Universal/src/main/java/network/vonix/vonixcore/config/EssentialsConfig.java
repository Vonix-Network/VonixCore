package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.YamlConfiguration;

public class EssentialsConfig {

    public static boolean enabled;

    public static boolean homesEnabled;
    public static boolean warpsEnabled;
    public static boolean tpaEnabled;
    public static boolean economyEnabled;
    public static boolean kitsEnabled;
    public static boolean shopsEnabled;

    public static int maxHomes;
    public static int homeCooldown;

    public static int tpaCooldown;
    public static int tpaTimeout;

    public static double startingBalance;
    public static String currencySymbol;
    public static String currencyName;

    public static int defaultKitCooldown;

    public static void load(YamlConfiguration config) {
        enabled = config.getBoolean("essentials.enabled", true);

        homesEnabled = config.getBoolean("essentials.features.homes_enabled", true);
        warpsEnabled = config.getBoolean("essentials.features.warps_enabled", true);
        tpaEnabled = config.getBoolean("essentials.features.tpa_enabled", true);
        economyEnabled = config.getBoolean("essentials.features.economy_enabled", true);
        kitsEnabled = config.getBoolean("essentials.features.kits_enabled", true);
        shopsEnabled = config.getBoolean("essentials.features.shops_enabled", true);

        maxHomes = config.getInt("essentials.homes.max_homes", 5);
        homeCooldown = config.getInt("essentials.homes.cooldown", 5);

        tpaCooldown = config.getInt("essentials.tpa.cooldown", 30);
        tpaTimeout = config.getInt("essentials.tpa.timeout", 120);

        startingBalance = config.getDouble("essentials.economy.starting_balance", 100.0);
        currencySymbol = config.getString("essentials.economy.currency_symbol", "$");
        currencyName = config.getString("essentials.economy.currency_name", "Coins");

        defaultKitCooldown = config.getInt("essentials.kits.default_cooldown", 86400);
    }
}
