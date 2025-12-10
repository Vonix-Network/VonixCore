package network.vonix.vonixcore;

import network.vonix.vonixcore.config.*;
import network.vonix.vonixcore.database.Database;
import network.vonix.vonixcore.economy.TransactionLog;
import network.vonix.vonixcore.graves.GravesCommands;
import network.vonix.vonixcore.graves.GravesListener;
import network.vonix.vonixcore.graves.GravesManager;
import network.vonix.vonixcore.jobs.JobsCommands;
import network.vonix.vonixcore.jobs.JobsManager;
import network.vonix.vonixcore.shops.ShopsCommands;
import network.vonix.vonixcore.shops.ShopsManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class VonixCore extends JavaPlugin {

    private static VonixCore instance;
    private Database database;
    private ConfigManager configManager;
    private GravesManager gravesManager;
    private ShopsManager shopsManager;
    private JobsManager jobsManager;

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

        // Initialize Graves system
        gravesManager = new GravesManager(this);
        GravesCommands gravesCommands = new GravesCommands(this, gravesManager);
        getCommand("graves").setExecutor(gravesCommands);
        getCommand("graves").setTabCompleter(gravesCommands);
        getServer().getPluginManager().registerEvents(new GravesListener(this, gravesManager), this);
        getLogger().info("Graves system initialized.");

        // Initialize Shops system
        try (Connection conn = database.getConnection()) {
            // Initialize transaction log table
            TransactionLog.getInstance().initializeTable(conn);

            // Initialize shops
            shopsManager = ShopsManager.getInstance();
            shopsManager.initialize(conn);

            // Register shop commands
            ShopsCommands shopsCmds = new ShopsCommands(this, shopsManager);
            if (getCommand("shop") != null) {
                getCommand("shop").setExecutor(shopsCmds);
                getCommand("shop").setTabCompleter(shopsCmds);
            }
            if (getCommand("cshop") != null) {
                getCommand("cshop").setExecutor(shopsCmds);
                getCommand("cshop").setTabCompleter(shopsCmds);
            }
            if (getCommand("market") != null) {
                getCommand("market").setExecutor(shopsCmds);
                getCommand("market").setTabCompleter(shopsCmds);
            }
            if (getCommand("pshop") != null) {
                getCommand("pshop").setExecutor(shopsCmds);
                getCommand("pshop").setTabCompleter(shopsCmds);
            }
            if (getCommand("shopadmin") != null) {
                getCommand("shopadmin").setExecutor(shopsCmds);
                getCommand("shopadmin").setTabCompleter(shopsCmds);
            }
            getLogger().info("Shops system initialized.");
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Failed to initialize shops system", e);
        }

        // Initialize Jobs system
        try (Connection conn = database.getConnection()) {
            jobsManager = new JobsManager(this);
            jobsManager.initialize(conn);

            JobsCommands jobsCmds = new JobsCommands(this, jobsManager);
            if (getCommand("jobs") != null) {
                getCommand("jobs").setExecutor(jobsCmds);
                getCommand("jobs").setTabCompleter(jobsCmds);
            }
            getLogger().info("Jobs system initialized.");
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "Failed to initialize jobs system", e);
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
        // Shutdown Jobs
        if (jobsManager != null) {
            jobsManager.shutdown();
        }

        // Shutdown Shops
        if (shopsManager != null) {
            shopsManager.shutdown();
        }

        // Shutdown Graves
        if (gravesManager != null) {
            gravesManager.shutdown();
        }

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
        ShopsConfig.load(getDataFolder());
    }

    public static VonixCore getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    public GravesManager getGravesManager() {
        return gravesManager;
    }

    public ShopsManager getShopsManager() {
        return shopsManager;
    }
}
