package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import network.vonix.vonixcore.VonixCore;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages floating item displays above chest shops.
 * Uses ItemEntity with NoGravity NBT tag for visual representation.
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

        // Remove existing display if any
        removeDisplay(level, pos);

        try {
            // Create a floating item entity
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.25;
            double z = pos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(level, x, y, z, displayItem.copy());

            // Configure as display - use available methods
            itemEntity.setNoGravity(true);
            itemEntity.setUnlimitedLifetime();
            itemEntity.setNeverPickUp();
            itemEntity.setGlowingTag(true);

            // Make invulnerable to prevent any interaction
            itemEntity.setInvulnerable(true);
            itemEntity.setSilent(true);

            // Set pickup delay to max to prevent any pickup attempts
            itemEntity.setPickUpDelay(Integer.MAX_VALUE);

            // Stop any velocity
            itemEntity.setDeltaMovement(0, 0, 0);

            // Add custom tag for identification
            itemEntity.addTag("vonix_shop_display");

            // Spawn the entity
            level.addFreshEntity(itemEntity);

            // Track the entity
            displayEntities.put(key, itemEntity.getUUID());

            VonixCore.LOGGER.debug("[Shop] Spawned display at {} with UUID {}", pos, itemEntity.getUUID());

        } catch (Exception e) {
            VonixCore.LOGGER.error("[Shop] Failed to spawn display entity: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Remove a display entity at a location
     */
    public void removeDisplay(ServerLevel level, BlockPos pos) {
        String key = locationKey(level, pos);
        UUID entityId = displayEntities.remove(key);

        if (entityId != null) {
            // Find and remove the entity
            var entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
                VonixCore.LOGGER.debug("[Shop] Removed display entity at {}", pos);
            } else {
                // Entity might have been unloaded, try to find by position
                removeDisplayByPosition(level, pos);
            }
        } else {
            // No tracked entity, try to find by position anyway
            removeDisplayByPosition(level, pos);
        }
    }

    /**
     * Remove display entity by searching near a position
     */
    private void removeDisplayByPosition(ServerLevel level, BlockPos pos) {
        AABB searchBox = new AABB(
                pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 2.0, pos.getZ() + 1.5);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox);

        for (ItemEntity item : items) {
            if (item.getTags().contains("vonix_shop_display")) {
                item.discard();
                VonixCore.LOGGER.debug("[Shop] Removed display entity by position at {}", pos);
            }
        }
    }

    /**
     * Update the display item at a location
     */
    public void updateDisplay(ServerLevel level, BlockPos pos, ItemStack newItem) {
        // For ItemEntity, just remove and respawn
        spawnDisplay(level, pos, newItem);
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
        String world = level.dimension().location().toString();

        // Query database asynchronously
        network.vonix.vonixcore.economy.ShopManager.getInstance().loadShopsInChunkAsync(world, chunkX, chunkZ, (shops) -> {
            if (shops.isEmpty()) {
                return;
            }

            // Schedule spawning on main thread
            level.getServer().execute(() -> {
                 // Double check if level is still loaded/valid if needed
                 if (level.getServer().getLevel(level.dimension()) == null) return;

                VonixCore.LOGGER.debug("[Shop] Respawning {} display(s) in chunk {},{}", shops.size(), chunkX, chunkZ);

                // Respawn each shop's display
                for (var shopLoc : shops) {
                    String key = locationKey(level, shopLoc.pos());

                    // Clean up potential duplicates from disk (e.g. after crash/restart)
                    removeDisplayByPosition(level, shopLoc.pos());

                    // Skip if already tracked (might have been loaded via entity load)
                    if (displayEntities.containsKey(key)) {
                        continue;
                    }

                    // Create display item from item ID
                    ItemStack displayItem = ItemUtils.createItemFromId(shopLoc.itemId());
                    if (!displayItem.isEmpty()) {
                        spawnDisplay(level, shopLoc.pos(), displayItem);
                    }
                }
            });
        });
    }

    /**
     * Remove all displays in a level (called on shutdown)
     */
    public void removeAllDisplays(ServerLevel level) {
        String prefix = level.dimension().location().toString();
        displayEntities.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                var entity = level.getEntity(entry.getValue());
                if (entity != null) {
                    entity.discard();
                }
                return true;
            }
            return false;
        });
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
