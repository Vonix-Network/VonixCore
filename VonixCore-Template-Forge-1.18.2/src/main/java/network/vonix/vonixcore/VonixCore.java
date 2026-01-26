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

    public VonixCore() {
        instance = this;

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EssentialsConfig.SPEC,
                "vonixcore-essentials.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, network.vonix.vonixcore.config.XPSyncConfig.SPEC,
                "vonixcore-xpsync.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, network.vonix.vonixcore.config.ClaimsConfig.SPEC,
                "vonixcore-claims.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, network.vonix.vonixcore.config.ShopsConfig.SPEC,
                "vonixcore-shops.toml");

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[VonixCore] VonixCore Forge 1.18.2 initialized");
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
        // Initialize Configs
        java.nio.file.Path configDir = getConfigPath();
        network.vonix.vonixcore.config.DatabaseConfig.init(configDir);
        network.vonix.vonixcore.config.AuthConfig.init(configDir);
        network.vonix.vonixcore.config.DiscordConfig.init(configDir);

        // Initialize Database
        try {
            database = new network.vonix.vonixcore.database.Database(event.getServer());
            database.initialize();
        } catch (java.sql.SQLException e) {
            LOGGER.error("Failed to initialize database", e);
        }

        // Initialize Managers
        network.vonix.vonixcore.auth.AuthenticationManager.updateFreezeCache();
        network.vonix.vonixcore.discord.DiscordManager.getInstance().initialize(event.getServer());

        new network.vonix.vonixcore.xpsync.XPSyncManager(event.getServer()).start();
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


