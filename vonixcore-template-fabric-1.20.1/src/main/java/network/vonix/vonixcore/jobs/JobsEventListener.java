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
            if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isSpectator()) { 
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                String targetId = id.getPath(); // e.g. "stone" or "coal_ore"
                
                JobsManager.getInstance().processAction(serverPlayer, Job.ActionType.BREAK, targetId);
            }
        });

        // Entity Kill
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer player) {
                ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                String targetId = id.getPath(); // e.g. "zombie" or "cow"
                
                JobsManager.getInstance().processAction(player, Job.ActionType.KILL, targetId);
            }
        });
    }
    
    private static String getTargetId(ResourceLocation id) {
        return id.getPath();
    }
}
