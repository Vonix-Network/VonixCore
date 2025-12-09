package network.vonix.vonixcore;

import network.vonix.vonixcore.config.*;
import network.vonix.vonixcore.database.Database;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public class VonixCore extends JavaPlugin {

    private static VonixCore instance;
    private Database database;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config manager
        this.configManager = new ConfigManager(this);

        // Load configs
        loadConfigs();

        // Initialize database
        try {
            this.database = new Database(this);
            this.database.initialize();
            getLogger().info("Database initialized successfully.");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        getCommand("vonix").setExecutor(new VonixCommand());

        getCommand("sethome").setExecutor(new network.vonix.vonixcore.homes.HomeCommands());
        getCommand("home").setExecutor(new network.vonix.vonixcore.homes.HomeCommands());
        getCommand("delhome").setExecutor(new network.vonix.vonixcore.homes.HomeCommands());
        getCommand("homes").setExecutor(new network.vonix.vonixcore.homes.HomeCommands());

        getCommand("setwarp").setExecutor(new network.vonix.vonixcore.warps.WarpCommands());
        getCommand("warp").setExecutor(new network.vonix.vonixcore.warps.WarpCommands());
        getCommand("delwarp").setExecutor(new network.vonix.vonixcore.warps.WarpCommands());
        getCommand("warps").setExecutor(new network.vonix.vonixcore.warps.WarpCommands());

        getCommand("tpa").setExecutor(new network.vonix.vonixcore.teleport.TeleportCommands());
        getCommand("tpaccept").setExecutor(new network.vonix.vonixcore.teleport.TeleportCommands());
        getCommand("tpdeny").setExecutor(new network.vonix.vonixcore.teleport.TeleportCommands());

        getCommand("balance").setExecutor(new network.vonix.vonixcore.economy.EconomyCommands());
        getCommand("pay").setExecutor(new network.vonix.vonixcore.economy.EconomyCommands());

        // Register listeners
        getServer().getPluginManager().registerEvents(new network.vonix.vonixcore.essentials.EssentialsListener(),
                this);
        getServer().getPluginManager().registerEvents(new network.vonix.vonixcore.protection.ProtectionListener(this),
                this);
        getServer().getPluginManager().registerEvents(new network.vonix.vonixcore.discord.DiscordListener(this), this);
        // XPSyncListener removed - batch sync only, no per-player events needed

        // Initialize Discord
        network.vonix.vonixcore.discord.DiscordManager.getInstance().initialize();

        // Initialize XPSync
        new network.vonix.vonixcore.xpsync.XPSyncManager(this).start();

        getLogger().info("VonixCore v" + getDescription().getVersion() + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        // Shutdown Discord
        network.vonix.vonixcore.discord.DiscordManager.getInstance().shutdown();

        // Shutdown XPSync
        if (network.vonix.vonixcore.xpsync.XPSyncManager.getInstance() != null) {
            network.vonix.vonixcore.xpsync.XPSyncManager.getInstance().stop();
        }

        getLogger().info("VonixCore disabled.");
    }

    private void loadConfigs() {
        // Load configuration files
        DatabaseConfig.load(configManager.loadConfig("vonixcore-database.yml"));
        EssentialsConfig.load(configManager.loadConfig("vonixcore-essentials.yml"));
        DiscordConfig.load(configManager.loadConfig("vonixcore-discord.yml"));
        XPSyncConfig.load(configManager.loadConfig("vonixcore-xpsync.yml"));
        ProtectionConfig.load(configManager.loadConfig("vonixcore-protection.yml"));
    }

    public static VonixCore getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }
}
