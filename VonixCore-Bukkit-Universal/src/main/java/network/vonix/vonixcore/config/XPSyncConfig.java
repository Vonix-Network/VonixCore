package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.YamlConfiguration;

public class XPSyncConfig {

    public static boolean enabled = false;

    public static String apiEndpoint;
    public static String apiKey;
    public static String serverName;
    public static int syncInterval;

    public static boolean trackPlaytime;
    public static boolean trackHealth;
    public static boolean trackHunger;
    public static boolean trackPosition;

    public static boolean verboseLogging;
    public static int connectionTimeout;
    public static int maxRetries;

    public static void load(YamlConfiguration config) {
        enabled = config.getBoolean("xpsync.enabled", false);

        apiEndpoint = config.getString("xpsync.api.endpoint", "https://vonix.network/api/minecraft/sync/xp");
        apiKey = config.getString("xpsync.api.api_key", "YOUR_API_KEY_HERE");
        serverName = config.getString("xpsync.api.server_name", "Server-1");
        syncInterval = config.getInt("xpsync.api.sync_interval", 300);

        trackPlaytime = config.getBoolean("xpsync.data.track_playtime", true);
        trackHealth = config.getBoolean("xpsync.data.track_health", true);
        trackHunger = config.getBoolean("xpsync.data.track_hunger", false);
        trackPosition = config.getBoolean("xpsync.data.track_position", false);

        verboseLogging = config.getBoolean("xpsync.advanced.verbose_logging", false);
        connectionTimeout = config.getInt("xpsync.advanced.connection_timeout", 10000);
        maxRetries = config.getInt("xpsync.advanced.max_retries", 3);
    }
}
