package network.vonix.vonixcore.shops.chest;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages floating item displays above chest shops.
 * Based on QuickShop-Hikari's display system with optimizations.
 */
public class ChestShopDisplay {

    private static final String METADATA_KEY = "vonixcore_shop_display";

    private final VonixCore plugin;
    private final ChestShopManager shopManager;

    // Track display entities by shop ID
    private final Map<Long, org.bukkit.entity.Entity> displays = new ConcurrentHashMap<>();

    private BukkitTask updateTask;

    public ChestShopDisplay(VonixCore plugin, ChestShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
    }

    /**
     * Start the display update task
     */
    public void start() {
        // Spawn displays for loaded shops
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Long shopId : displays.keySet()) {
                ChestShop shop = shopManager.getShopById(shopId);
                if (shop != null) {
                    createDisplay(shop);
                }
            }
        }, 20L); // Wait 1 second for world to load

        // Start periodic check task
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkDisplays,
                20L * 30, 20L * 30); // Every 30 seconds
    }

    /**
     * Create a display for a shop
     */
    public void createDisplay(ChestShop shop) {
        if (!ShopsConfig.chestShopsDisplayItems)
            return;

        // Remove existing display
        removeDisplay(shop);

        Location loc = shop.getLocation();
        if (loc == null || loc.getWorld() == null)
            return;

        // Position above chest
        Location displayLoc = loc.clone().add(0.5, 1.2, 0.5);

        ItemStack displayItem = shop.getCachedItem();
        if (displayItem == null || displayItem.getType() == Material.AIR) {
            Material mat = Material.getMaterial(shop.getItemType());
            if (mat == null)
                return;
            displayItem = new ItemStack(mat);
        }
        final ItemStack finalDisplayItem = displayItem;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                // Create dropped item display
                Item item = loc.getWorld().dropItem(displayLoc, finalDisplayItem.clone());
                item.setPickupDelay(Integer.MAX_VALUE);
                item.setVelocity(new Vector(0, 0, 0));
                item.setGravity(false);
                item.setInvulnerable(true);
                item.setCustomNameVisible(false);
                item.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, shop.getId()));

                // Prevent despawn
                item.setPersistent(false);

                displays.put(shop.getId(), item);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create shop display: " + e.getMessage());
            }
        });
    }

    /**
     * Remove a display for a shop
     */
    public void removeDisplay(ChestShop shop) {
        org.bukkit.entity.Entity entity = displays.remove(shop.getId());
        if (entity != null && entity.isValid()) {
            plugin.getServer().getScheduler().runTask(plugin, entity::remove);
        }
    }

    /**
     * Check and fix displays
     */
    private void checkDisplays() {
        // Check for despawned displays and respawn them
        for (Map.Entry<Long, org.bukkit.entity.Entity> entry : displays.entrySet()) {
            org.bukkit.entity.Entity entity = entry.getValue();
            if (entity == null || !entity.isValid() || entity.isDead()) {
                ChestShop shop = shopManager.getShopById(entry.getKey());
                if (shop != null) {
                    createDisplay(shop);
                }
            }
        }

        // Check for orphaned display items and remove them
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    if (item.hasMetadata(METADATA_KEY)) {
                        long shopId = item.getMetadata(METADATA_KEY).get(0).asLong();
                        if (!displays.containsKey(shopId) || shopManager.getShopById(shopId) == null) {
                            item.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * Refresh display for a shop (e.g., after item change)
     */
    public void refreshDisplay(ChestShop shop) {
        removeDisplay(shop);
        createDisplay(shop);
    }

    /**
     * Shutdown and cleanup all displays
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        // Remove all display entities
        for (org.bukkit.entity.Entity entity : displays.values()) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        displays.clear();
    }

    /**
     * Check if an entity is a shop display
     */
    public boolean isShopDisplay(org.bukkit.entity.Entity entity) {
        return entity.hasMetadata(METADATA_KEY);
    }

    /**
     * Get shop ID from display entity
     */
    public long getShopIdFromDisplay(org.bukkit.entity.Entity entity) {
        if (entity.hasMetadata(METADATA_KEY)) {
            return entity.getMetadata(METADATA_KEY).get(0).asLong();
        }
        return -1;
    }
}
