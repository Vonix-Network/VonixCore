package network.vonix.vonixcore.config;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager {

    private final VonixCore plugin;

    public ConfigManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public YamlConfiguration loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);

        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Load default config from jar to check for missing keys
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);
            try {
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
            }
        }

        return config;
    }

}
