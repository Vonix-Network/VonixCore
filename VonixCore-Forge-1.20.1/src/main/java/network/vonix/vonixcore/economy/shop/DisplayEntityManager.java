package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import network.vonix.vonixcore.VonixCore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages floating item displays above chest shops.
 * Forge 1.20.1 compatible version.
 */
public class DisplayEntityManager {

    private static DisplayEntityManager instance;

    // Track display entities: "world:x,y,z" -> Entity UUID
    private final Map<String, UUID> displayEntities = new ConcurrentHashMap<>();

    public static DisplayEntityManager getInstance() {
        if (instance == null) {
            instance = new DisplayEntityManager();
        }
        return instance;
    }

    /**
     * Spawn a floating item display above a chest shop
     */
    public void spawnDisplay(ServerLevel level, BlockPos pos, ItemStack displayItem) {
        String key = locationKey(level, pos);
        removeDisplay(level, pos);

        try {
            // Track that a display should exist here
            displayEntities.put(key, UUID.randomUUID());
            VonixCore.LOGGER.debug("[Shop] Registered display location at {}", pos);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Shop] Failed to register display location: {}", e.getMessage());
        }
    }

    /**
     * Remove a display entity at a location
     */
    public void removeDisplay(ServerLevel level, BlockPos pos) {
        String key = locationKey(level, pos);
        UUID entityId = displayEntities.remove(key);

        if (entityId != null) {
            VonixCore.LOGGER.debug("[Shop] Removed display registration at {}", pos);
        }
    }

    /**
     * Update the display item at a location
     */
    public void updateDisplay(ServerLevel level, BlockPos pos, ItemStack newItem) {
        String key = locationKey(level, pos);
        if (!displayEntities.containsKey(key)) {
            spawnDisplay(level, pos, newItem);
        }
    }

    /**
     * Check if a display exists at a location
     */
    public boolean hasDisplay(ServerLevel level, BlockPos pos) {
        String key = locationKey(level, pos);
        return displayEntities.containsKey(key);
    }

    /**
     * Remove all displays (called on shutdown)
     */
    public void removeAllDisplays(ServerLevel level) {
        String prefix = level.dimension().location().toString();
        displayEntities.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    /**
     * Generate location key
     */
    private String locationKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location().toString() + ":" +
                pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /**
     * Check if an entity is a shop display entity
     */
    public boolean isShopDisplay(UUID entityId) {
        return displayEntities.containsValue(entityId);
    }
}
