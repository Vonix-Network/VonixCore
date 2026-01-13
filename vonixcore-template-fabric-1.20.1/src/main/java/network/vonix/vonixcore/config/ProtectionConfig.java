package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * Protection (block logging) configuration for VonixCore.
 * Stored in config/vonixcore-protection.yml
 */
public class ProtectionConfig extends BaseConfig {

    private static ProtectionConfig instance;

    public static ProtectionConfig getInstance() {
        if (instance == null) {
            instance = new ProtectionConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().loadConfig(configDir);
    }

    private ProtectionConfig() {
        super("vonixcore-protection.yml");
    }

    private void loadConfig(Path configDir) {
        super.load(configDir);
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore Protection Configuration
                # Block logging and rollback features (CoreProtect-like)
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("protection.enabled", true);

        // Logging toggles
        setDefault("logging.log_block_break", true);
        setDefault("logging.log_block_place", true);
        setDefault("logging.log_container_access", true);
        setDefault("logging.log_entity_kills", true);
        setDefault("logging.log_sign_text", true);
        setDefault("logging.log_explosions", true);
        setDefault("logging.log_fire", true);
        setDefault("logging.log_water_flow", false);
        setDefault("logging.log_lava_flow", false);

        // Rollback settings
        setDefault("rollback.max_radius", 50);
        setDefault("rollback.max_time_days", 30);
        setDefault("rollback.require_confirm", true);

        // Performance
        setDefault("performance.async_lookups", true);
        setDefault("performance.batch_writes", true);
        setDefault("performance.cache_size", 10000);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("protection.enabled", true);
    }

    // Logging
    public boolean isLogBlockBreak() {
        return getBoolean("logging.log_block_break", true);
    }

    public boolean isLogBlockPlace() {
        return getBoolean("logging.log_block_place", true);
    }

    public boolean isLogContainerAccess() {
        return getBoolean("logging.log_container_access", true);
    }

    public boolean isLogEntityKills() {
        return getBoolean("logging.log_entity_kills", true);
    }

    public boolean isLogSignText() {
        return getBoolean("logging.log_sign_text", true);
    }

    public boolean isLogExplosions() {
        return getBoolean("logging.log_explosions", true);
    }

    public boolean isLogFire() {
        return getBoolean("logging.log_fire", true);
    }

    public boolean isLogWaterFlow() {
        return getBoolean("logging.log_water_flow", false);
    }

    public boolean isLogLavaFlow() {
        return getBoolean("logging.log_lava_flow", false);
    }

    // Rollback
    public int getMaxRadius() {
        return getInt("rollback.max_radius", 50);
    }

    public int getMaxTimeDays() {
        return getInt("rollback.max_time_days", 30);
    }

    public boolean isRequireConfirm() {
        return getBoolean("rollback.require_confirm", true);
    }

    // Performance
    public boolean isAsyncLookups() {
        return getBoolean("performance.async_lookups", true);
    }

    public boolean isBatchWrites() {
        return getBoolean("performance.batch_writes", true);
    }

    public int getCacheSize() {
        return getInt("performance.cache_size", 10000);
    }
}
