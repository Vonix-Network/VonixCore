package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * Authentication configuration for VonixCore.
 * Stored in config/vonixcore-auth.yml
 */
public class AuthConfig extends BaseConfig {

    private static AuthConfig instance;

    public static AuthConfig getInstance() {
        if (instance == null) {
            instance = new AuthConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().loadConfig(configDir);
    }

    private AuthConfig() {
        super("vonixcore-auth.yml");
    }

    private void loadConfig(Path configDir) {
        super.load(configDir);
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore Authentication Configuration
                # Player verification and freeze settings
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("auth.enabled", false);

        // Freeze settings
        setDefault("freeze.enabled", true);
        setDefault("freeze.message", "§cYou are frozen! Please verify your account.");
        setDefault("freeze.allow_chat", false);
        setDefault("freeze.allow_commands", false);

        // Verification
        setDefault("verification.api_url", "https://your-api.com/verify");
        setDefault("verification.api_key", "YOUR_API_KEY_HERE");
        setDefault("verification.timeout_seconds", 30);
        setDefault("verification.kick_on_fail", true);

        // Whitelist
        setDefault("whitelist.enabled", false);
        setDefault("whitelist.message", "§cYou are not whitelisted on this server.");
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("auth.enabled", false);
    }

    // Freeze
    public boolean isFreezeEnabled() {
        return getBoolean("freeze.enabled", true);
    }

    public String getFreezeMessage() {
        return getString("freeze.message", "§cYou are frozen! Please verify your account.");
    }

    public boolean isAllowFreezeChat() {
        return getBoolean("freeze.allow_chat", false);
    }

    public boolean isAllowFreezeCommands() {
        return getBoolean("freeze.allow_commands", false);
    }

    // Verification
    public String getVerificationApiUrl() {
        return getString("verification.api_url", "https://your-api.com/verify");
    }

    public String getVerificationApiKey() {
        return getString("verification.api_key", "YOUR_API_KEY_HERE");
    }

    public int getVerificationTimeout() {
        return getInt("verification.timeout_seconds", 30);
    }

    public boolean isKickOnVerificationFail() {
        return getBoolean("verification.kick_on_fail", true);
    }

    // Whitelist
    public boolean isWhitelistEnabled() {
        return getBoolean("whitelist.enabled", false);
    }

    public String getWhitelistMessage() {
        return getString("whitelist.message", "§cYou are not whitelisted on this server.");
    }
}
