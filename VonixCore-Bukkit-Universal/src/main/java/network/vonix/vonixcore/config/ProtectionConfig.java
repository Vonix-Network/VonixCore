package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.YamlConfiguration;

public class ProtectionConfig {

    public static boolean enabled;

    public static boolean logBlockBreak;
    public static boolean logBlockPlace;
    public static boolean logBlockExplode;
    public static boolean logContainerTransactions;
    public static boolean logEntityKills;
    public static boolean logEntitySpawn;
    public static boolean logPlayerInteractions;
    public static boolean logChat;
    public static boolean logCommands;
    public static boolean logSigns;

    public static int defaultRadius;
    public static int maxRadius;
    public static int defaultTime;
    public static int maxLookupResults;

    public static void load(YamlConfiguration config) {
        enabled = config.getBoolean("protection.enabled", true);

        logBlockBreak = config.getBoolean("protection.logging.block_break", true);
        logBlockPlace = config.getBoolean("protection.logging.block_place", true);
        logBlockExplode = config.getBoolean("protection.logging.block_explode", true);
        logContainerTransactions = config.getBoolean("protection.logging.container_transactions", true);
        logEntityKills = config.getBoolean("protection.logging.entity_kills", true);
        logEntitySpawn = config.getBoolean("protection.logging.entity_spawn", false);
        logPlayerInteractions = config.getBoolean("protection.logging.player_interactions", true);
        logChat = config.getBoolean("protection.logging.chat", false);
        logCommands = config.getBoolean("protection.logging.commands", false);
        logSigns = config.getBoolean("protection.logging.signs", true);

        defaultRadius = config.getInt("protection.rollback.default_radius", 10);
        maxRadius = config.getInt("protection.rollback.max_radius", 100);
        defaultTime = config.getInt("protection.rollback.default_time", 259200);
        maxLookupResults = config.getInt("protection.rollback.max_lookup_results", 1000);
    }
}
