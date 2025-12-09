package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import network.vonix.vonixcore.VonixCore;

import java.io.File;

public class ChatConfig {

    public static boolean enabled = true;
    public static String format = "<gray>[<prefix><gray>] <white><displayname><gray>: <white><message>";
    public static boolean formatChat = true;

    public static void load() {
        File configFile = new File(VonixCore.getInstance().getDataFolder(), "vonixcore-chat.yml");
        if (!configFile.exists()) {
            VonixCore.getInstance().saveResource("vonixcore-chat.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("enabled", true);
        format = config.getString("format", "<gray>[<prefix><gray>] <white><displayname><gray>: <white><message>");
        formatChat = config.getBoolean("format-chat", true);
    }
}
