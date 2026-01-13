package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * XPSync configuration for VonixCore.
 * Stored in config/vonixcore-xpsync.yml
 */
public class XPSyncConfig extends BaseConfig {

    private static XPSyncConfig instance;

    public static XPSyncConfig getInstance() {
        if (instance == null) {
            instance = new XPSyncConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().loadConfig(configDir);
    }

    private XPSyncConfig() {
        super("vonixcore-xpsync.yml");
    }

    private void loadConfig(Path configDir) {
        super.load(configDir);
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore XPSync Configuration
                # Synchronize player XP to external API
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("xpsync.enabled", false);

        // API settings
        setDefault("api.url", "https://your-api.com/xp");
        setDefault("api.key", "YOUR_API_KEY_HERE");
        setDefault("api.timeout_ms", 5000);

        // Sync settings
        setDefault("sync.interval_seconds", 60);
        setDefault("sync.on_level_up", true);
        setDefault("sync.on_death", true);
        setDefault("sync.min_xp_change", 10);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("xpsync.enabled", false);
    }

    // API
    public String getApiUrl() {
        return getString("api.url", "https://your-api.com/xp");
    }

    public String getApiKey() {
        return getString("api.key", "YOUR_API_KEY_HERE");
    }

    public int getApiTimeout() {
        return getInt("api.timeout_ms", 5000);
    }

    // Sync
    public int getSyncInterval() {
        return getInt("sync.interval_seconds", 60);
    }

    public boolean isSyncOnLevelUp() {
        return getBoolean("sync.on_level_up", true);
    }

    public boolean isSyncOnDeath() {
        return getBoolean("sync.on_death", true);
    }

    public int getMinXpChange() {
        return getInt("sync.min_xp_change", 10);
    }
}
