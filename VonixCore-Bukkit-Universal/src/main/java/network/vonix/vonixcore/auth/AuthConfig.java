package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration for authentication system.
 */
public class AuthConfig {
    // LuckPerms sync
    public static boolean enableLuckPermsSync = true;
    public static String adminRankIds = "admin,owner,developer";

    // Authentication requirements
    public static boolean requireAuthentication = true;
    public static boolean freezeUnauthenticated = true;
    public static boolean warnOfAuth = false;
    public static int loginTimeout = 60;

    // URLs
    public static String apiBaseUrl = "https://vonix.network/api";
    public static String registrationApiKey = "YOUR_API_KEY_HERE";
    public static int apiTimeout = 5000;
    public static String registrationUrl = "https://vonix.network/register";

    // Messages
    public static String loginRequiredMessage = "§c§l⚠ §cYou must authenticate to play on this server.";
    public static String authWarningMessage = "§e⚠ It is recommended to authenticate with Vonix Network.";
    public static String generatingCodeMessage = "§6⏳ §7Generating registration code...";
    public static String registrationCodeMessage = "§aYour registration code is: §e{code}";
    public static String alreadyAuthenticatedMessage = "§aYou are already authenticated!";
    public static String authenticatingMessage = "§6⏳ §7Authenticating...";
    public static String authenticationSuccessMessage = "§a§l✓ §7Successfully authenticated as §e{username}";
    public static String loginFailedMessage = "§c§l✗ §7Login failed: §c{error}";

    public static void load(FileConfiguration config) {
        enableLuckPermsSync = config.getBoolean("auth.enable_luckperms_sync", true);
        adminRankIds = config.getString("auth.admin_rank_ids", "admin,owner,developer");

        requireAuthentication = config.getBoolean("auth.require_authentication", true);
        freezeUnauthenticated = config.getBoolean("auth.freeze_unauthenticated", true);
        warnOfAuth = config.getBoolean("auth.warn_of_auth", false);
        loginTimeout = config.getInt("auth.login_timeout", 60);

        apiBaseUrl = config.getString("auth.urls.api_base_url", "https://vonix.network/api");
        registrationApiKey = config.getString("auth.urls.registration_api_key", "YOUR_API_KEY_HERE");
        apiTimeout = config.getInt("auth.urls.api_timeout", 5000);
        registrationUrl = config.getString("auth.urls.registration_url", "https://vonix.network/register");

        loginRequiredMessage = config.getString("auth.messages.login_required_message", loginRequiredMessage);
        authWarningMessage = config.getString("auth.messages.auth_warning_message", authWarningMessage);
        generatingCodeMessage = config.getString("auth.messages.generating_code_message", generatingCodeMessage);
        registrationCodeMessage = config.getString("auth.messages.registration_code_message", registrationCodeMessage);
        alreadyAuthenticatedMessage = config.getString("auth.messages.already_authenticated_message",
                alreadyAuthenticatedMessage);
        authenticatingMessage = config.getString("auth.messages.authenticating_message", authenticatingMessage);
        authenticationSuccessMessage = config.getString("auth.messages.authentication_success_message",
                authenticationSuccessMessage);
        loginFailedMessage = config.getString("auth.messages.login_failed_message", loginFailedMessage);
    }
}
