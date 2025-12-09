package network.vonix.vonixcore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import network.vonix.vonixcore.admin.AdminManager;
import network.vonix.vonixcore.config.DatabaseConfig;
import network.vonix.vonixcore.config.DiscordConfig;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.config.ProtectionConfig;
import network.vonix.vonixcore.config.XPSyncConfig;
import network.vonix.vonixcore.consumer.Consumer;
import network.vonix.vonixcore.database.Database;
import network.vonix.vonixcore.discord.DiscordManager;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;
import network.vonix.vonixcore.homes.HomeManager;
import network.vonix.vonixcore.kits.KitManager;
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
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogUtils.getLogger();

    private static VonixCore instance;
    private Database database;
    private XPSyncManager xpSyncManager;

    // Track enabled modules
    private boolean protectionEnabled = false;
    private boolean essentialsEnabled = false;
    private boolean discordEnabled = false;
    private boolean xpsyncEnabled = false;

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

    public VonixCore() {
        instance = this;

        // Get the mod event bus using Forge 1.20.1 approach
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        // Register separate config files for each module using Forge 1.20.1 API
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DatabaseConfig.SPEC, "vonixcore-database.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ProtectionConfig.SPEC,
                "vonixcore-protection.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EssentialsConfig.SPEC,
                "vonixcore-essentials.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DiscordConfig.SPEC, "vonixcore-discord.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, XPSyncConfig.SPEC, "vonixcore-xpsync.toml");

        LOGGER.info("[{}] Loading v{}...", MOD_NAME, VERSION);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] Common setup complete", MOD_NAME);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[{}] Initializing modules...", MOD_NAME);

        List<String> enabledModules = new ArrayList<>();

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
}
