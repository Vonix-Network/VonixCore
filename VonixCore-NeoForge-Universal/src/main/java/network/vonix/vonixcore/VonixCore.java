package network.vonix.vonixcore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import network.vonix.vonixcore.admin.AdminManager;
import network.vonix.vonixcore.auth.AuthenticationManager;
import network.vonix.vonixcore.auth.AuthConfig;
import network.vonix.vonixcore.config.DatabaseConfig;
import network.vonix.vonixcore.config.DiscordConfig;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.config.ProtectionConfig;
import network.vonix.vonixcore.config.XPSyncConfig;
import network.vonix.vonixcore.config.GravesConfig;
import network.vonix.vonixcore.config.ClaimsConfig;
import network.vonix.vonixcore.graves.GravesManager;
import network.vonix.vonixcore.graves.GravesCommands;
import network.vonix.vonixcore.claims.ClaimsManager;
import network.vonix.vonixcore.claims.ClaimsCommands;
import network.vonix.vonixcore.consumer.Consumer;
import network.vonix.vonixcore.database.Database;
import network.vonix.vonixcore.discord.DiscordManager;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;
import network.vonix.vonixcore.homes.HomeManager;
import network.vonix.vonixcore.kits.KitManager;
import network.vonix.vonixcore.jobs.JobsManager;
import network.vonix.vonixcore.warps.WarpManager;
import network.vonix.vonixcore.xpsync.XPSyncManager;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * VonixCore - All-in-one essentials mod for NeoForge 1.21.x
 * 
 * Features (all toggleable):
 * - Protection: Block logging and rollback (CoreProtect-like)
 * - Essentials: Homes, warps, TPA, economy, kits
 * - Discord: Bidirectional chat integration
 * - XPSync: XP synchronization to external API
 */
@Mod(VonixCore.MODID)
public class VonixCore {

    public static final String MODID = "vonixcore";
    public static final String MOD_NAME = "VonixCore";
    public static final String VERSION = "21.1.1";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static VonixCore instance;
    private Database database;
    private XPSyncManager xpSyncManager;

    // Track enabled modules
    private boolean protectionEnabled = false;
    private boolean essentialsEnabled = false;
    private boolean discordEnabled = false;
    private boolean xpsyncEnabled = false;
    private boolean gravesEnabled = false;
    private boolean claimsEnabled = false;
    private GravesManager gravesManager;

    public static VonixCore getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    public int getMaxHomes() {
        return EssentialsConfig.CONFIG.maxHomes.get();
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    public boolean isEssentialsEnabled() {
        return essentialsEnabled;
    }

    public VonixCore(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        // Register separate config files for each module
        modContainer.registerConfig(ModConfig.Type.COMMON, DatabaseConfig.SPEC, "vonixcore-database.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ProtectionConfig.SPEC, "vonixcore-protection.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, EssentialsConfig.SPEC, "vonixcore-essentials.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, DiscordConfig.SPEC, "vonixcore-discord.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, XPSyncConfig.SPEC, "vonixcore-xpsync.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, AuthConfig.SPEC, "vonixcore-auth.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, GravesConfig.SPEC, "vonixcore-graves.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ClaimsConfig.SPEC, "vonixcore-claims.toml");

        LOGGER.info("[{}] Loading v{}...", MOD_NAME, VERSION);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] Common setup complete", MOD_NAME);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[{}] Initializing modules...", MOD_NAME);

        List<String> enabledModules = new ArrayList<>();

        // Initialize Auth (ensure config is loaded)
        try {
            AuthenticationManager.updateFreezeCache();
            LOGGER.info("[{}] Auth module initialized", MOD_NAME);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to initialize Auth: {}", MOD_NAME, e.getMessage());
        }

        // Initialize database (always needed)
        try {
            database = new Database(event.getServer());
            database.initialize();
            LOGGER.info("[{}] Database initialized", MOD_NAME);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to initialize database: {}", MOD_NAME, e.getMessage());
            e.printStackTrace();
            return; // Cannot continue without database
        }

        // Initialize Protection module
        if (ProtectionConfig.CONFIG.enabled.get()) {
            try {
                // Consumer handles protection data batching
                Consumer.getInstance().start();
                protectionEnabled = true;
                enabledModules.add("Protection");
                LOGGER.info("[{}] Protection module enabled", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to initialize Protection: {}", MOD_NAME, e.getMessage());
            }
        }

        // Initialize Essentials module
        if (EssentialsConfig.CONFIG.enabled.get()) {
            try (Connection conn = database.getConnection()) {
                if (EssentialsConfig.CONFIG.homesEnabled.get()) {
                    HomeManager.getInstance().initializeTable(conn);
                }
                if (EssentialsConfig.CONFIG.warpsEnabled.get()) {
                    WarpManager.getInstance().initializeTable(conn);
                }
                if (EssentialsConfig.CONFIG.economyEnabled.get()) {
                    EconomyManager.getInstance().initializeTable(conn);
                    if (EssentialsConfig.CONFIG.shopsEnabled.get()) {
                        ShopManager.getInstance().initializeTable(conn);
                    }
                }
                if (EssentialsConfig.CONFIG.kitsEnabled.get()) {
                    KitManager.getInstance().initializeTable(conn);
                    KitManager.getInstance().loadDefaultKits();
                }
                AdminManager.getInstance().initializeTable(conn);
                JobsManager.getInstance().initialize(conn);

                essentialsEnabled = true;
                enabledModules.add("Essentials");
                LOGGER.info("[{}] Essentials module enabled", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to initialize Essentials: {}", MOD_NAME, e.getMessage());
            }
        }

        // Initialize XPSync module
        if (XPSyncConfig.CONFIG.enabled.get()) {
            String apiKey = XPSyncConfig.CONFIG.apiKey.get();
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
                LOGGER.warn("[{}] XPSync is enabled but API key not configured", MOD_NAME);
            } else {
                try {
                    xpSyncManager = new XPSyncManager(event.getServer());
                    xpSyncManager.start();
                    xpsyncEnabled = true;
                    enabledModules.add("XPSync");
                    LOGGER.info("[{}] XPSync module enabled", MOD_NAME);
                } catch (Exception e) {
                    LOGGER.error("[{}] Failed to initialize XPSync: {}", MOD_NAME, e.getMessage());
                }
            }
        }

        // Initialize Graves module
        if (GravesConfig.CONFIG.shouldBeEnabled()) {
            // Check for alternative grave mods
            String detectedMod = detectAlternativeGraveMod();
            if (detectedMod != null && GravesConfig.CONFIG.autoDisableIfAlternativeDetected.get()) {
                LOGGER.info("[{}] Graves auto-disabled: '{}' mod detected", MOD_NAME, detectedMod);
            } else {
                try {
                    gravesManager = new GravesManager();
                    GravesManager.enabled = true;
                    GravesManager.expirationTime = GravesConfig.CONFIG.expirationTime.get();
                    GravesManager.xpRetention = GravesConfig.CONFIG.xpRetention.get();
                    GravesManager.protectionTime = GravesConfig.CONFIG.protectionTime.get();
                    GravesManager.maxGravesPerPlayer = GravesConfig.CONFIG.maxGravesPerPlayer.get();

                    // GravesListener is auto-registered via @EventBusSubscriber annotation

                    // Register commands
                    GravesCommands.register(event.getServer().getCommands().getDispatcher());

                    gravesEnabled = true;
                    enabledModules.add("Graves");
                    LOGGER.info("[{}] Graves module enabled", MOD_NAME);
                } catch (Exception e) {
                    LOGGER.error("[{}] Failed to initialize Graves: {}", MOD_NAME, e.getMessage());
                }
            }
        }

        // Initialize Claims module
        if (ClaimsConfig.CONFIG.enabled.get()) {
            try (Connection conn = database.getConnection()) {
                ClaimsManager.getInstance().initializeTable(conn);
                ClaimsCommands.register(event.getServer().getCommands().getDispatcher());
                claimsEnabled = true;
                enabledModules.add("Claims");
                LOGGER.info("[{}] Claims module enabled", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to initialize Claims: {}", MOD_NAME, e.getMessage());
            }
        }

        // Log status
        if (enabledModules.isEmpty()) {
            LOGGER.warn("[{}] No modules enabled! Check your config files.", MOD_NAME);
        } else {
            LOGGER.info("[{}] ✓ Loaded successfully with modules: {}", MOD_NAME, String.join(", ", enabledModules));
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Initialize Discord module (requires server to be fully started)
        if (DiscordConfig.CONFIG.enabled.get()) {
            try {
                DiscordManager.getInstance().initialize(event.getServer());
                discordEnabled = true;
                LOGGER.info("[{}] Discord module enabled", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Failed to initialize Discord: {}", MOD_NAME, e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("[{}] Shutting down...", MOD_NAME);

        // Shutdown Discord with timeout
        if (discordEnabled) {
            try {
                if (DiscordManager.getInstance().isRunning()) {
                    String serverName = DiscordConfig.CONFIG.serverName.get();

                    // Send shutdown embed with timeout
                    CompletableFuture<Void> shutdownMessage = CompletableFuture.runAsync(() -> {
                        DiscordManager.getInstance().sendShutdownEmbed(serverName);
                    });

                    try {
                        shutdownMessage.get(2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        LOGGER.debug("[{}] Discord shutdown message timed out", MOD_NAME);
                    }
                }
                DiscordManager.getInstance().shutdown();
                LOGGER.debug("[{}] Discord shutdown complete", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Error during Discord shutdown", MOD_NAME, e);
            }
        }

        // Shutdown XPSync
        if (xpsyncEnabled && xpSyncManager != null) {
            try {
                xpSyncManager.stop();
                xpSyncManager = null;
                LOGGER.debug("[{}] XPSync shutdown complete", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Error during XPSync shutdown", MOD_NAME, e);
            }
        }

        // Shutdown Protection consumer
        if (protectionEnabled) {
            try {
                Consumer.getInstance().stop();
                LOGGER.debug("[{}] Protection consumer shutdown complete", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Error during Consumer shutdown", MOD_NAME, e);
            }
        }

        // Shutdown Graves
        if (gravesEnabled && gravesManager != null) {
            try {
                gravesManager.shutdown();
                gravesManager = null;
                LOGGER.debug("[{}] Graves shutdown complete", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Error during Graves shutdown", MOD_NAME, e);
            }
        }

        // Close database last
        if (database != null) {
            try {
                database.close();
                database = null;
                LOGGER.debug("[{}] Database closed", MOD_NAME);
            } catch (Exception e) {
                LOGGER.error("[{}] Error closing database", MOD_NAME, e);
            }
        }

        LOGGER.info("[{}] Shutdown complete", MOD_NAME);
    }

    // Executor service for async operations
    private static final java.util.concurrent.ExecutorService ASYNC_EXECUTOR = java.util.concurrent.Executors
            .newCachedThreadPool();

    /**
     * Execute a task asynchronously
     */
    public static void executeAsync(Runnable task) {
        ASYNC_EXECUTOR.submit(task);
    }

    /**
     * Get the config path
     */
    public java.nio.file.Path getConfigPath() {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
    }

    /**
     * Detect if an alternative grave mod is loaded.
     * 
     * @return The mod ID if detected, null otherwise.
     */
    private String detectAlternativeGraveMod() {
        String[] alternativeMods = {
                // Popular grave mods
                "corpse",
                "gravestone",
                "gravestone_mod",
                "gravestones",
                "universal_graves",
                "gravesx",
                "tombstone",
                // YIGD (You're In Grave Danger)
                "yigd",
                // Death chest mods
                "death_chest",
                "deathchest",
                "simple_death_chest",
                "your_items_are_safe",
                // Other
                "vanity", // Vanity mod has grave feature
                "gravestone-mod"
        };

        for (String modId : alternativeMods) {
            if (net.neoforged.fml.ModList.get().isLoaded(modId)) {
                return modId;
            }
        }
        return null;
    }
}
