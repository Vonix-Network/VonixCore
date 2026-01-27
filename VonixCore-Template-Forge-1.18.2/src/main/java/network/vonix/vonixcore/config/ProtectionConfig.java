package network.vonix.vonixcore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Protection/Logging configuration for VonixCore.
 * Stored in config/vonixcore-protection.toml
 */
public class ProtectionConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ProtectionConfig CONFIG;

    // Master toggle
    public final ForgeConfigSpec.BooleanValue enabled;

    // Logging settings
    public final ForgeConfigSpec.BooleanValue logBlockBreak;
    public final ForgeConfigSpec.BooleanValue logBlockPlace;
    public final ForgeConfigSpec.BooleanValue logBlockExplode;
    public final ForgeConfigSpec.BooleanValue logContainerTransactions;
    public final ForgeConfigSpec.BooleanValue logEntityKills;
    public final ForgeConfigSpec.BooleanValue logEntitySpawn;
    public final ForgeConfigSpec.BooleanValue logPlayerInteractions;
    public final ForgeConfigSpec.BooleanValue logChat;
    public final ForgeConfigSpec.BooleanValue logCommands;
    public final ForgeConfigSpec.BooleanValue logSigns;

    // Rollback settings
    public final ForgeConfigSpec.IntValue defaultRadius;
    public final ForgeConfigSpec.IntValue maxRadius;
    public final ForgeConfigSpec.IntValue defaultTime;
    public final ForgeConfigSpec.IntValue maxLookupResults;

    static {
        Pair<ProtectionConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                .configure(ProtectionConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private ProtectionConfig(ForgeConfigSpec.Builder builder) {
        builder.comment(
                "VonixCore Protection Configuration",
                "Block logging and rollback features (similar to CoreProtect)")
                .push("protection");

        enabled = builder.comment(
                "Enable the protection/logging module",
                "Set to false to completely disable block logging and rollback features")
                .define("enabled", true);

        builder.pop().comment(
                "Event Logging",
                "Choose which events to log to the database")
                .push("logging");

        logBlockBreak = builder.comment("Log when players break blocks")
                .define("block_break", true);

        logBlockPlace = builder.comment("Log when players place blocks")
                .define("block_place", true);

        logBlockExplode = builder.comment("Log explosions (TNT, creepers, etc.)")
                .define("block_explode", true);

        logContainerTransactions = builder.comment(
                "Log container transactions (chests, furnaces, etc.)",
                "Warning: Can generate a lot of data on busy servers")
                .define("container_transactions", true);

        logEntityKills = builder.comment("Log entity kills (players killing mobs/animals)")
                .define("entity_kills", true);

        logEntitySpawn = builder.comment(
                "Log entity spawns",
                "Warning: Very spammy, not recommended for production")
                .define("entity_spawn", false);

        logPlayerInteractions = builder.comment("Log player interactions (buttons, levers, doors)")
                .define("player_interactions", true);

        logChat = builder.comment("Log chat messages")
                .define("chat", false);

        logCommands = builder.comment("Log commands executed by players")
                .define("commands", false);

        logSigns = builder.comment("Log sign text changes")
                .define("signs", true);

        builder.pop().comment(
                "Rollback Settings",
                "Configure rollback behavior")
                .push("rollback");

        defaultRadius = builder.comment("Default rollback radius when no radius specified")
                .defineInRange("default_radius", 10, 1, 100);

        maxRadius = builder.comment("Maximum rollback radius allowed")
                .defineInRange("max_radius", 100, 1, 500);

        defaultTime = builder.comment("Default rollback time in seconds when no time specified")
                .defineInRange("default_time", 259200, 60, 2592000);

        maxLookupResults = builder.comment("Maximum number of results to return for lookups")
                .defineInRange("max_lookup_results", 1000, 10, 10000);

        builder.pop();
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return CONFIG.enabled.get();
    }

    // Logging
    public boolean isLogBlockBreak() {
        return CONFIG.logBlockBreak.get();
    }

    public boolean isLogBlockPlace() {
        return CONFIG.logBlockPlace.get();
    }

    public boolean isLogBlockExplode() {
        return CONFIG.logBlockExplode.get();
    }

    public boolean isLogContainerTransactions() {
        return CONFIG.logContainerTransactions.get();
    }

    public boolean isLogEntityKills() {
        return CONFIG.logEntityKills.get();
    }

    public boolean isLogEntitySpawn() {
        return CONFIG.logEntitySpawn.get();
    }

    public boolean isLogPlayerInteractions() {
        return CONFIG.logPlayerInteractions.get();
    }

    public boolean isLogChat() {
        return CONFIG.logChat.get();
    }

    public boolean isLogCommands() {
        return CONFIG.logCommands.get();
    }

    public boolean isLogSigns() {
        return CONFIG.logSigns.get();
    }

    // Rollback
    public int getDefaultRadius() {
        return CONFIG.defaultRadius.get();
    }

    public int getMaxRadius() {
        return CONFIG.maxRadius.get();
    }

    public int getDefaultTime() {
        return CONFIG.defaultTime.get();
    }

    public int getMaxLookupResults() {
        return CONFIG.maxLookupResults.get();
    }
}
