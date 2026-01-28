package network.vonix.vonixcore;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.database.Database;
import org.slf4j.Logger;

/**
 * VonixCore - Universal Minecraft Server Plugin
 * Forge 1.18.2 Edition
 */
@Mod(VonixCore.MODID)
public class VonixCore {

    public static final String MODID = "vonixcore";
    public static final String VERSION = "1.18.2-1.0.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static VonixCore instance;
    private Database database;
    private network.vonix.vonixcore.xpsync.XPSyncManager xpSyncManager;
    private boolean protectionEnabled = false;
    private boolean essentialsEnabled = false;
    private boolean xpsyncEnabled = false;
    private boolean claimsEnabled = false;
    private boolean discordEnabled = false;

    public VonixCore() {
        try {
            instance = this;

            // Register config
            try {
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.DatabaseConfig.SPEC,
                        "vonixcore-database.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.AuthConfig.SPEC,
                        "vonixcore-auth.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.DiscordConfig.SPEC,
                        "vonixcore-discord.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.ProtectionConfig.SPEC,
                        "vonixcore-protection.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EssentialsConfig.SPEC,
                        "vonixcore-essentials.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.XPSyncConfig.SPEC,
                        "vonixcore-xpsync.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.ClaimsConfig.SPEC,
                        "vonixcore-claims.toml");
                ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, network.vonix.vonixcore.config.ShopsConfig.SPEC,
                        "vonixcore-shops.toml");
            } catch (Throwable t) {
                LOGGER.error("[VonixCore] Failed to register configs", t);
                throw t;
            }

            // Register ourselves for server and other game events
            MinecraftForge.EVENT_BUS.register(this);

            LOGGER.info("[VonixCore] VonixCore Forge 1.18.2 initialized");
        } catch (Throwable t) {
            LOGGER.error("[VonixCore] Failed during mod construction", t);
            throw t;
        }
    }

    public static VonixCore getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    /**
     * Get the config path
     */
    public java.nio.file.Path getConfigPath() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
    }

    // Executor service for async operations - bounded to prevent thread exhaustion
    private static final java.util.concurrent.ExecutorService ASYNC_EXECUTOR = new java.util.concurrent.ThreadPoolExecutor(
            2, // core threads
            16, // max threads
            60L, java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(1000), // bounded queue
            r -> {
                Thread t = new Thread(r, "VonixCore-Async");
                t.setDaemon(true);
                return t;
            },
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy() // backpressure: caller runs if queue full
    );

    /**
     * Execute a task asynchronously
     */
    public static void executeAsync(Runnable task) {
        ASYNC_EXECUTOR.submit(task);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        // Initialize Database
        try {
            database = new network.vonix.vonixcore.database.Database(event.getServer());
            database.initialize();
        } catch (java.sql.SQLException e) {
            LOGGER.error("Failed to initialize database", e);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        // Register Economy Event Listener
        MinecraftForge.EVENT_BUS.register(network.vonix.vonixcore.listener.EconomyEventListener.class);

        // Initialize Protection module
        if (network.vonix.vonixcore.config.ProtectionConfig.CONFIG.enabled.get()) {
            try {
                // Consumer handles protection data batching
                network.vonix.vonixcore.consumer.Consumer.getInstance().start();

                // Initialize Managers after configs are loaded
                network.vonix.vonixcore.auth.AuthenticationManager.updateFreezeCache();
                protectionEnabled = true;
                LOGGER.info("[VonixCore] Protection module enabled");
            } catch (Exception e) {
                LOGGER.error("[VonixCore] Failed to initialize Protection: {}", e.getMessage());
            }
        }

        // Initialize Essentials module
        if (EssentialsConfig.CONFIG.enabled.get()) {
            try (java.sql.Connection conn = database.getConnection()) {
                // Initialize all sub-modules since 1.18.2 config lacks granular toggles
                network.vonix.vonixcore.homes.HomeManager.getInstance().initializeTable(conn);
                network.vonix.vonixcore.warps.WarpManager.getInstance().initializeTable(conn);
                network.vonix.vonixcore.admin.AdminManager.getInstance().initializeTable(conn);

                // Economy (Initialize tables)
                network.vonix.vonixcore.economy.EconomyManager.getInstance().initializeTable(conn);
                network.vonix.vonixcore.economy.TransactionLog.getInstance().initializeTable(conn);

                // Shops
                if (network.vonix.vonixcore.config.EssentialsConfig.CONFIG.shopsEnabled.get()) {
                    network.vonix.vonixcore.economy.ShopManager.getInstance().initializeTable(conn);
                }

                // Jobs (Config managed internally, but table needs creating if enabled in main config)
                // Note: JobsManager handles its own init call below because it needs event registration
                if (network.vonix.vonixcore.config.EssentialsConfig.CONFIG.jobsEnabled.get()) {
                     network.vonix.vonixcore.jobs.JobsManager.getInstance().initialize(conn);
                }

                // Kits
                if (network.vonix.vonixcore.config.EssentialsConfig.CONFIG.kitsEnabled.get()) {
                    network.vonix.vonixcore.kits.KitManager.getInstance().initializeTable(conn);
                    network.vonix.vonixcore.kits.KitManager.getInstance().loadDefaultKits();
                }

                // Permissions (Always initialized as it handles groups for other modules)
                network.vonix.vonixcore.permissions.PermissionManager.getInstance().initialize(conn);

                essentialsEnabled = true;
                LOGGER.info("[VonixCore] Essentials module enabled");
            } catch (Exception e) {
                LOGGER.error("[VonixCore] Failed to initialize Essentials: {}", e.getMessage());
            }
        }

        // Initialize XPSync module
        if (network.vonix.vonixcore.config.XPSyncConfig.CONFIG.enabled.get()) {
            String apiKey = network.vonix.vonixcore.config.XPSyncConfig.CONFIG.apiKey.get();
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
                LOGGER.warn("[VonixCore] XPSync is enabled but API key not configured");
            } else {
                try {
                    xpSyncManager = new network.vonix.vonixcore.xpsync.XPSyncManager(event.getServer());
                    xpSyncManager.start();
                    xpsyncEnabled = true;
                    LOGGER.info("[VonixCore] XPSync module enabled");
                } catch (Exception e) {
                    LOGGER.error("[VonixCore] Failed to initialize XPSync: {}", e.getMessage());
                }
            }
        }

        // Initialize Claims module
        if (network.vonix.vonixcore.config.ClaimsConfig.CONFIG.enabled.get()) {
            try (java.sql.Connection conn = database.getConnection()) {
                network.vonix.vonixcore.claims.ClaimsManager.getInstance().initializeTable(conn);
                network.vonix.vonixcore.claims.ClaimsCommands.register(event.getServer().getCommands().getDispatcher());
                claimsEnabled = true;
                LOGGER.info("[VonixCore] Claims module enabled");
            } catch (Exception e) {
                LOGGER.error("[VonixCore] Failed to initialize Claims: {}", e.getMessage());
            }
        }

        // Initialize Discord module (requires server to be fully started)
        if (network.vonix.vonixcore.config.DiscordConfig.CONFIG.enabled.get()) {
            try {
                network.vonix.vonixcore.discord.DiscordManager.getInstance().initialize(event.getServer());
                discordEnabled = true;
                LOGGER.info("[VonixCore] Discord module enabled");
            } catch (Exception e) {
                LOGGER.error("[VonixCore] Failed to initialize Discord: {}", e.getMessage());
            }
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        network.vonix.vonixcore.command.TeleportCommands.register(event.getDispatcher());
        network.vonix.vonixcore.auth.AuthCommands.register(event.getDispatcher());
        network.vonix.vonixcore.claims.ClaimsCommands.register(event.getDispatcher());
        network.vonix.vonixcore.economy.commands.EconomyCommands.register(event.getDispatcher());
        network.vonix.vonixcore.homes.HomeCommands.register(event.getDispatcher());
        network.vonix.vonixcore.warps.WarpCommands.register(event.getDispatcher());
        network.vonix.vonixcore.economy.commands.ShopCommands.register(event.getDispatcher());
        network.vonix.vonixcore.jobs.JobsCommands.register(event.getDispatcher());
        network.vonix.vonixcore.kits.KitCommands.register(event.getDispatcher());
        network.vonix.vonixcore.permissions.PermissionCommands.register(event.getDispatcher());
        LOGGER.info("[VonixCore] Registered commands");
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        if (network.vonix.vonixcore.xpsync.XPSyncManager.getInstance() != null) {
            network.vonix.vonixcore.xpsync.XPSyncManager.getInstance().stop();
        }
        network.vonix.vonixcore.discord.DiscordManager.getInstance().shutdown();
        network.vonix.vonixcore.auth.api.VonixNetworkAPI.shutdown();
        network.vonix.vonixcore.auth.AuthenticationManager.shutdown();
        network.vonix.vonixcore.teleport.AsyncRtpManager.shutdown();
        
        // Shutdown async executor
        try {
            ASYNC_EXECUTOR.shutdown();
            if (!ASYNC_EXECUTOR.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (Throwable e) {
            ASYNC_EXECUTOR.shutdownNow();
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Close database
        if (database != null) {
            database.close();
        }
    }
}


