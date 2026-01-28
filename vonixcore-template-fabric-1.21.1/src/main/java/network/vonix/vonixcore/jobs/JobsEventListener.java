package network.vonix.vonixcore.jobs;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public class JobsEventListener {

    public static void register() {
        // Block Break
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player.isSpectator() || !world.isClientSide()) { // Only server-side
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                String targetId = id.getPath(); // e.g. "stone" or "coal_ore"
                
                if (player instanceof ServerPlayer serverPlayer) {
                    JobsManager.getInstance().processAction(serverPlayer, Job.ActionType.BREAK, targetId);
                }
            }
        });

        // Entity Kill
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer serverPlayer) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                String targetId = id.getPath(); // e.g. "zombie" or "cow"
                
                JobsManager.getInstance().processAction(serverPlayer, Job.ActionType.KILL, targetId);
            }
        });
        
        // TODO: Implement other actions (Place, Fish, Breed, Craft, Eat)
        // Fabric API doesn't have events for all of these easily accessible without more complex logic/mixins.
        // For Place: Use PlayerBlockBreakEvents (Wait, no, Use PlayerBlockPlaceEvents if available? No, Fabric API uses UseBlockCallback which is complex)
        // Actually, there isn't a simple "After Place" event in standard Fabric API that guarantees placement success easily.
        // For now, we'll stick to Break/Kill which are the most important.
    }
    
    private static String getTargetId(ResourceLocation id) {
        return id.getPath();
    }
}
