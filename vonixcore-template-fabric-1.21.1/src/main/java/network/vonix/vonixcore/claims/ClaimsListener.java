package network.vonix.vonixcore.claims;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import network.vonix.vonixcore.VonixCore;

/**
 * Event listener for claims protection.
 */
public class ClaimsListener {

    public static void register() {
        // Block break protection
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                String worldName = world.dimension().location().toString();
                if (!ClaimsManager.getInstance().canPlayerBuild(serverPlayer.getUUID(), worldName, pos.getX(), pos.getZ())) {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYou don't have permission to break blocks here!"));
                    return false;
                }
            }
            return true;
        });

        // Block place protection
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                String worldName = world.dimension().location().toString();
                int x = hitResult.getBlockPos().getX();
                int z = hitResult.getBlockPos().getZ();
                if (ClaimsManager.getInstance().isLocationClaimed(worldName, x, z) &&
                    !ClaimsManager.getInstance().canPlayerBuild(serverPlayer.getUUID(), worldName, x, z)) {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYou don't have permission to build here!"));
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
    }
}
