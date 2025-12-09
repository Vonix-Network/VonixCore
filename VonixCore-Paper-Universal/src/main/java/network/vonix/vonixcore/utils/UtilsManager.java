package network.vonix.vonixcore.utils;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class UtilsManager {

    private static UtilsManager instance;
    private final VonixCore plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private final java.util.Set<UUID> godPlayers = new java.util.HashSet<>();
    private final java.util.Set<UUID> afkPlayers = new java.util.HashSet<>();

    public UtilsManager(VonixCore plugin) {
        this.plugin = plugin;
        instance = this;
        loadData();
    }

    public static UtilsManager getInstance() {
        return instance;
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "vonixcore-data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setNick(UUID uuid, String nick) {
        dataConfig.set("nicks." + uuid.toString(), nick);
        saveData();
    }

    public String getNick(UUID uuid) {
        return dataConfig.getString("nicks." + uuid.toString());
    }

    public void removeNick(UUID uuid) {
        dataConfig.set("nicks." + uuid.toString(), null);
        saveData();
    }

    public boolean isGod(UUID uuid) {
        return godPlayers.contains(uuid);
    }

    public void setGod(UUID uuid, boolean god) {
        if (god)
            godPlayers.add(uuid);
        else
            godPlayers.remove(uuid);
    }

    public boolean isAfk(UUID uuid) {
        return afkPlayers.contains(uuid);
    }

    public void setAfk(UUID uuid, boolean afk) {
        if (afk)
            afkPlayers.add(uuid);
        else
            afkPlayers.remove(uuid);
    }
}
