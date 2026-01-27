package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * Essentials configuration for VonixCore.
 * Stored in config/vonixcore-essentials.yml
 */
public class EssentialsConfig extends BaseConfig {

    private static EssentialsConfig instance;

    public static EssentialsConfig getInstance() {
        if (instance == null) {
            instance = new EssentialsConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().loadConfig(configDir);
    }

    private EssentialsConfig() {
        super("vonixcore-essentials.yml");
    }

    private void loadConfig(Path configDir) {
        super.load(configDir);
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore Essentials Configuration
                # Homes, warps, teleportation, economy, and kits
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("essentials.enabled", true);

        // Feature toggles
        setDefault("features.homes_enabled", true);
        setDefault("features.warps_enabled", true);
        setDefault("features.tpa_enabled", true);
        setDefault("features.economy_enabled", true);
        setDefault("features.kits_enabled", true);
        setDefault("features.shops_enabled", true);
        setDefault("features.jobs_enabled", true);
        setDefault("features.jobs_enabled", true);
        setDefault("features.rtp_enabled", true);
        setDefault("features.chat_formatting_enabled", true);

        // RTP settings
        setDefault("rtp.cooldown", 600);
        setDefault("rtp.max_range", 10000);
        setDefault("rtp.min_range", 500);

        // Homes settings
        setDefault("homes.max_homes", 5);
        setDefault("homes.cooldown", 5);

        // TPA settings
        setDefault("tpa.cooldown", 30);
        setDefault("tpa.cooldown", 30);
        setDefault("tpa.cooldown", 30);
        setDefault("tpa.timeout", 120);

        // Back command settings
        setDefault("teleport.back_timeout", 300);
        setDefault("teleport.death_back_timeout", 60);
        setDefault("teleport.death_back_delay", 0);

        // Economy settings
        setDefault("economy.starting_balance", 250.0);
        setDefault("economy.currency_symbol", "$");
        setDefault("economy.currency_name", "Coins");

        // Kits settings
        setDefault("kits.default_cooldown", 86400);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("essentials.enabled", true);
    }

    // Feature toggles
    public boolean isHomesEnabled() {
        return getBoolean("features.homes_enabled", true);
    }

    public boolean isWarpsEnabled() {
        return getBoolean("features.warps_enabled", true);
    }

    public boolean isTpaEnabled() {
        return getBoolean("features.tpa_enabled", true);
    }

    public boolean isEconomyEnabled() {
        return getBoolean("features.economy_enabled", true);
    }

    public boolean isKitsEnabled() {
        return getBoolean("features.kits_enabled", true);
    }

    public boolean isShopsEnabled() {
        return getBoolean("features.shops_enabled", true);
    }

    public boolean isJobsEnabled() {
        return getBoolean("features.jobs_enabled", true);
    }

    public boolean isChatFormattingEnabled() {
        return getBoolean("features.chat_formatting_enabled", true);
    }

    // Homes
    public int getMaxHomes() {
        return getInt("homes.max_homes", 5);
    }

    public int getHomeCooldown() {
        return getInt("homes.cooldown", 5);
    }

    // TPA
    public int getTpaCooldown() {
        return getInt("tpa.cooldown", 30);
    }

    public int getTpaTimeout() {
        return getInt("tpa.timeout", 120);
    }

    public int getBackTimeout() {
        return getInt("teleport.back_timeout", 300);
    }

    public int getDeathBackDelay() {
        return getInt("teleport.death_back_delay", 60);
    }

    // Economy
    public double getStartingBalance() {
        return getDouble("economy.starting_balance", 250.0);
    }

    public String getCurrencySymbol() {
        return getString("economy.currency_symbol", "$");
    }

    public String getCurrencyName() {
        return getString("economy.currency_name", "Coins");
    }

    // Kits
    public int getDefaultKitCooldown() {
        return getInt("kits.default_cooldown", 86400);
    }

    // RTP
    public boolean isRtpEnabled() {
        return getBoolean("features.rtp_enabled", true);
    }

    public int getRtpCooldown() {
        return getInt("rtp.cooldown", 600);
    }

    public int getRtpMaxRange() {
        return getInt("rtp.max_range", 10000);
    }

    public int getRtpMinRange() {
        return getInt("rtp.min_range", 500);
    }
}
