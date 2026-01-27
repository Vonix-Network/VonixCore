package network.vonix.vonixcore.claims;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import network.vonix.vonixcore.VonixCore;

/**
 * Event listener for claims protection.
 */
@Mod.EventBusSubscriber(modid = VonixCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClaimsListener {

    // Block break protection
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getWorld().isClientSide() && event.getPlayer() instanceof ServerPlayer serverPlayer) {
            String worldName = serverPlayer.getLevel().dimension().location().toString();
            int x = event.getPos().getX();
            int z = event.getPos().getZ();

            // Check if user has permission
            if (!ClaimsManager.getInstance().canPlayerBuild(serverPlayer.getUUID(), worldName, x, z)) {
                serverPlayer.sendMessage(new TextComponent("§cYou don't have permission to break blocks here!"),
                        net.minecraft.Util.NIL_UUID);
                event.setCanceled(true);
            }
        }
    }

    // Block place protection
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && !event.getWorld().isClientSide()) {
            String worldName = serverPlayer.getLevel().dimension().location().toString();
            int x = event.getPos().getX();
            int z = event.getPos().getZ();

            if (ClaimsManager.getInstance().isLocationClaimed(worldName, x, z) &&
                    !ClaimsManager.getInstance().canPlayerBuild(serverPlayer.getUUID(), worldName, x, z)) {
                serverPlayer.sendMessage(new TextComponent("§cYou don't have permission to build here!"),
                        net.minecraft.Util.NIL_UUID);
                event.setCanceled(true);
            }
        }
    }

    // Interaction protection
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer && !event.getWorld().isClientSide()) {
            String worldName = serverPlayer.getLevel().dimension().location().toString();
            int x = event.getPos().getX();
            int z = event.getPos().getZ();

            if (ClaimsManager.getInstance().isLocationClaimed(worldName, x, z) &&
                    !ClaimsManager.getInstance().canPlayerBuild(serverPlayer.getUUID(), worldName, x, z)) {
                // Determine if interaction should be blocked (simplified for now, blocking all
                // right clicks on claimed land if untrusted)
                // In a real implementation we might separate container access from button
                // pressing etc.
                // Assuming canPlayerBuild implies full trust for now.
                serverPlayer.sendMessage(new TextComponent("§cYou don't have permission to interact here!"),
                        net.minecraft.Util.NIL_UUID);
                event.setCanceled(true);
            }
        }
    }
}
