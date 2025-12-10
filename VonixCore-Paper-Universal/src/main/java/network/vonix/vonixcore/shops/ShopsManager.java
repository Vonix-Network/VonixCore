package network.vonix.vonixcore.shops;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import network.vonix.vonixcore.shops.chest.ChestShopManager;
import network.vonix.vonixcore.shops.gui.ServerShopManager;
import network.vonix.vonixcore.shops.player.PlayerMarketManager;
import network.vonix.vonixcore.shops.sign.SignShopManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Main orchestrator for all shop systems in VonixCore.
 * Manages initialization, coordination, and lifecycle of all shop types.
 * 
 * Inspired by QuickShop-Hikari's modular architecture.
 */
public class ShopsManager {

    private static ShopsManager instance;
    private final VonixCore plugin;

    private ChestShopManager chestShopManager;
    private SignShopManager signShopManager;
    private ServerShopManager serverShopManager;
    private PlayerMarketManager playerMarketManager;

    private boolean initialized = false;

    public ShopsManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public static ShopsManager getInstance() {
        if (instance == null) {
            instance = new ShopsManager(VonixCore.getInstance());
        }
        return instance;
    }

    /**
     * Initialize all shop systems
     */
    public void initialize(Connection conn) throws SQLException {
        if (!ShopsConfig.enabled) {
            plugin.getLogger().info("[Shops] Shops module is disabled in config");
            return;
        }

        plugin.getLogger().info("[Shops] Initializing shop systems...");

        // Initialize Chest Shops
        if (ShopsConfig.chestShopsEnabled) {
            chestShopManager = new ChestShopManager(plugin);
            chestShopManager.initialize(conn);
            plugin.getLogger().info("[Shops] Chest Shops enabled");
        }

        // Initialize Sign Shops
        if (ShopsConfig.signShopsEnabled) {
            signShopManager = new SignShopManager(plugin);
            signShopManager.initialize(conn);
            plugin.getLogger().info("[Shops] Sign Shops enabled");
        }

        // Initialize Server GUI Shop
        if (ShopsConfig.guiShopEnabled) {
            serverShopManager = new ServerShopManager(plugin);
            serverShopManager.initialize();
            plugin.getLogger().info("[Shops] Server GUI Shop enabled");
        }

        // Initialize Player Market
        if (ShopsConfig.playerMarketEnabled) {
            playerMarketManager = new PlayerMarketManager(plugin);
            playerMarketManager.initialize(conn);
            plugin.getLogger().info("[Shops] Player Market enabled");
        }

        initialized = true;
        plugin.getLogger().info("[Shops] All shop systems initialized successfully");
    }

    /**
     * Shutdown all shop systems gracefully
     */
    public void shutdown() {
        if (!initialized)
            return;

        plugin.getLogger().info("[Shops] Shutting down shop systems...");

        if (chestShopManager != null) {
            chestShopManager.shutdown();
        }

        if (signShopManager != null) {
            signShopManager.shutdown();
        }

        if (serverShopManager != null) {
            serverShopManager.shutdown();
        }

        if (playerMarketManager != null) {
            playerMarketManager.shutdown();
        }

        initialized = false;
        plugin.getLogger().info("[Shops] All shop systems shut down");
    }

    /**
     * Reload all shop configurations
     */
    public void reload() {
        ShopsConfig.reload();

        if (chestShopManager != null) {
            chestShopManager.reload();
        }

        if (signShopManager != null) {
            signShopManager.reload();
        }

        if (serverShopManager != null) {
            serverShopManager.reload();
        }

        if (playerMarketManager != null) {
            playerMarketManager.reload();
        }

        plugin.getLogger().info("[Shops] All shop configurations reloaded");
    }

    // Getters for individual managers
    public ChestShopManager getChestShopManager() {
        return chestShopManager;
    }

    public SignShopManager getSignShopManager() {
        return signShopManager;
    }

    public ServerShopManager getServerShopManager() {
        return serverShopManager;
    }

    public PlayerMarketManager getPlayerMarketManager() {
        return playerMarketManager;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isChestShopsEnabled() {
        return chestShopManager != null && ShopsConfig.chestShopsEnabled;
    }

    public boolean isSignShopsEnabled() {
        return signShopManager != null && ShopsConfig.signShopsEnabled;
    }

    public boolean isGuiShopEnabled() {
        return serverShopManager != null && ShopsConfig.guiShopEnabled;
    }

    public boolean isPlayerMarketEnabled() {
        return playerMarketManager != null && ShopsConfig.playerMarketEnabled;
    }
}
