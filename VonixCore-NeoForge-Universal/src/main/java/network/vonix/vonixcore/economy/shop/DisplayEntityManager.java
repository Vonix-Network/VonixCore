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
 * Note: Display entity configuration requires NBT data access.
 * This is a simplified implementation that tracks display positions.
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
     * Note: Full display entity creation requires more complex setup in 1.21+
     * This is a placeholder that tracks the location
     */
    public void spawnDisplay(ServerLevel level, BlockPos pos, ItemStack displayItem) {
        String key = locationKey(level, pos);

        // Remove existing display if any
        removeDisplay(level, pos);

        try {
            // For now, just track that a display should exist here
            // Full implementation would spawn an ItemDisplay entity
            // but the API requires reflection or access to protected methods
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
            // Register new location
            spawnDisplay(level, pos, newItem);
        }
        // In a full implementation, this would update the entity's item
    }

    /**
     * Check if a display exists at a location
     */
    public boolean hasDisplay(ServerLevel level, BlockPos pos) {
        String key = locationKey(level, pos);
        return displayEntities.containsKey(key);
    }

    /**
     * Respawn all displays in a chunk (called on chunk load)
     */
    public void respawnDisplaysInChunk(ServerLevel level, int chunkX, int chunkZ) {
        // This would query database for shops in this chunk and respawn displays
        VonixCore.LOGGER.debug("[Shop] Chunk loaded: {},{}", chunkX, chunkZ);
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
