package network.vonix.vonixcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Essentials configuration for VonixCore.
 * Stored in config/vonixcore-essentials.toml
 */
public class EssentialsConfig {

    public static final ModConfigSpec SPEC;
    public static final EssentialsConfig CONFIG;

    // Master toggle
    public final ModConfigSpec.BooleanValue enabled;

    // Sub-module toggles
    public final ModConfigSpec.BooleanValue homesEnabled;
    public final ModConfigSpec.BooleanValue warpsEnabled;
    public final ModConfigSpec.BooleanValue tpaEnabled;
    public final ModConfigSpec.BooleanValue economyEnabled;
    public final ModConfigSpec.BooleanValue kitsEnabled;
    public final ModConfigSpec.BooleanValue shopsEnabled;

    // Homes settings
    public final ModConfigSpec.IntValue maxHomes;
    public final ModConfigSpec.IntValue homeCooldown;

    // TPA settings
    public final ModConfigSpec.IntValue tpaCooldown;
    public final ModConfigSpec.IntValue tpaTimeout;

    // Economy settings
    public final ModConfigSpec.DoubleValue startingBalance;
    public final ModConfigSpec.ConfigValue<String> currencySymbol;
    public final ModConfigSpec.ConfigValue<String> currencyName;

    // Kits settings
    public final ModConfigSpec.IntValue defaultKitCooldown;

    static {
        Pair<EssentialsConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(EssentialsConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private EssentialsConfig(ModConfigSpec.Builder builder) {
        builder.comment(
                "VonixCore Essentials Configuration",
                "Homes, warps, teleportation, economy, and kits")
                .push("essentials");

        enabled = builder.comment(
                "Enable the essentials module",
                "Set to false to completely disable all essentials features")
                .define("enabled", true);

        builder.pop().comment(
                "Feature Toggles",
                "Enable or disable individual essentials features")
                .push("features");

        homesEnabled = builder.comment("Enable home commands (/sethome, /home, /delhome)")
                .define("homes_enabled", true);

        warpsEnabled = builder.comment("Enable warp commands (/setwarp, /warp, /delwarp)")
                .define("warps_enabled", true);

        tpaEnabled = builder.comment("Enable teleport request commands (/tpa, /tpaccept, /tpadeny)")
                .define("tpa_enabled", true);

        economyEnabled = builder.comment("Enable economy features (/balance, /pay, /baltop)")
                .define("economy_enabled", true);

        kitsEnabled = builder.comment("Enable kit system (/kit)")
                .define("kits_enabled", true);

        shopsEnabled = builder.comment("Enable shop system (GUI shops, sign shops)")
                .define("shops_enabled", true);

        builder.pop().comment(
                "Homes Settings",
                "Configure the player homes feature")
                .push("homes");

        maxHomes = builder.comment(
                "Maximum number of homes per player",
                "Can be overridden with permissions for VIPs")
                .defineInRange("max_homes", 5, 1, 100);

        homeCooldown = builder.comment(
                "Cooldown between home teleports in seconds",
                "Set to 0 to disable cooldown")
                .defineInRange("cooldown", 5, 0, 300);

        builder.pop().comment(
                "TPA Settings",
                "Configure teleport request feature")
                .push("tpa");

        tpaCooldown = builder.comment(
                "Cooldown between TPA requests in seconds")
                .defineInRange("cooldown", 30, 0, 3600);

        tpaTimeout = builder.comment(
                "How long TPA requests remain valid (seconds)",
                "After this time, the request expires")
                .defineInRange("timeout", 120, 30, 600);

        builder.pop().comment(
                "Economy Settings",
                "Configure the economy system")
                .push("economy");

        startingBalance = builder.comment(
                "Starting balance for new players")
                .defineInRange("starting_balance", 100.0, 0.0, 1000000.0);

        currencySymbol = builder.comment(
                "Currency symbol to display (e.g., $, €, £)")
                .define("currency_symbol", "$");

        currencyName = builder.comment(
                "Currency name (e.g., Coins, Credits, Gold)")
                .define("currency_name", "Coins");

        builder.pop().comment(
                "Kits Settings",
                "Configure the kit system")
                .push("kits");

        defaultKitCooldown = builder.comment(
                "Default cooldown for kits in seconds",
                "Individual kits can override this")
                .defineInRange("default_cooldown", 86400, 0, 604800);

        builder.pop();
    }
}
