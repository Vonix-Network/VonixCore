package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import network.vonix.vonixcore.VonixCore;

import java.io.File;

public class AuthConfig {

    public static boolean enabled = true;
    public static boolean enableSessions = true;
    public static int sessionTimeout = 86400; // Seconds (24 hours)
    public static boolean encryptionEnabled = true; // Use SHA-256 or BCrypt
    public static int minPasswordLength = 8;

    // Limits
    public static int maxLoginAttempts = 5;
    public static int lockoutDuration = 300; // 5 minutes

    public static void load() {
        File configFile = new File(VonixCore.getInstance().getDataFolder(), "vonixcore-auth.yml");
        if (!configFile.exists()) {
            VonixCore.getInstance().saveResource("vonixcore-auth.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("enabled", true);
        enableSessions = config.getBoolean("sessions.enabled", true);
        sessionTimeout = config.getInt("sessions.timeout", 86400);
        minPasswordLength = config.getInt("security.min-password-length", 8);
        maxLoginAttempts = config.getInt("security.max-login-attempts", 5);
        lockoutDuration = config.getInt("security.lockout-duration", 300);
    }
}
