package network.vonix.vonixcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Protection/Logging configuration for VonixCore.
 * Stored in config/vonixcore-protection.toml
 */
public class ProtectionConfig {

    public static final ModConfigSpec SPEC;
    public static final ProtectionConfig CONFIG;

    // Master toggle
    public final ModConfigSpec.BooleanValue enabled;

    // Logging settings
    public final ModConfigSpec.BooleanValue logBlockBreak;
    public final ModConfigSpec.BooleanValue logBlockPlace;
    public final ModConfigSpec.BooleanValue logBlockExplode;
    public final ModConfigSpec.BooleanValue logContainerTransactions;
    public final ModConfigSpec.BooleanValue logEntityKills;
    public final ModConfigSpec.BooleanValue logEntitySpawn;
    public final ModConfigSpec.BooleanValue logPlayerInteractions;
    public final ModConfigSpec.BooleanValue logChat;
    public final ModConfigSpec.BooleanValue logCommands;
    public final ModConfigSpec.BooleanValue logSigns;

    // Rollback settings
    public final ModConfigSpec.IntValue defaultRadius;
    public final ModConfigSpec.IntValue maxRadius;
    public final ModConfigSpec.IntValue defaultTime;
    public final ModConfigSpec.IntValue maxLookupResults;

    static {
        Pair<ProtectionConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(ProtectionConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private ProtectionConfig(ModConfigSpec.Builder builder) {
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

        logPlayerInteractions = builder.comment(
                "Log player interactions (doors, buttons, levers, etc.)")
                .define("player_interactions", true);

        logChat = builder.comment(
                "Log player chat messages",
                "Note: Consider privacy implications before enabling")
                .define("chat", false);

        logCommands = builder.comment(
                "Log player commands",
                "Note: Consider privacy implications before enabling")
                .define("commands", false);

        logSigns = builder.comment("Log sign text when placed or edited")
                .define("signs", true);

        builder.pop().comment(
                "Rollback Settings",
                "Configure defaults and limits for rollback/restore operations")
                .push("rollback");

        defaultRadius = builder.comment(
                "Default radius for lookups/rollbacks when not specified",
                "Players can override this up to max_radius")
                .defineInRange("default_radius", 10, 1, 100);

        maxRadius = builder.comment(
                "Maximum allowed radius for lookups/rollbacks",
                "Prevent players from running very large operations")
                .defineInRange("max_radius", 100, 1, 500);

        defaultTime = builder.comment(
                "Default time in seconds for lookups (3 days = 259200)",
                "How far back to search by default")
                .defineInRange("default_time", 259200, 60, 2592000);

        maxLookupResults = builder.comment(
                "Maximum number of results to return in lookups",
                "Prevent performance issues from very large queries")
                .defineInRange("max_lookup_results", 1000, 100, 10000);

        builder.pop();
    }
}
