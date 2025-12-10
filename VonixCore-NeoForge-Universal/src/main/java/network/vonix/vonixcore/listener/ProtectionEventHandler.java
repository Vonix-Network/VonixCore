package network.vonix.vonixcore.listener;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.command.ProtectionCommands;
import network.vonix.vonixcore.config.ProtectionConfig;

/**
 * Event handler for protection module events.
 * Handles inspector clicks and command registration.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class ProtectionEventHandler {

    /**
     * Register protection commands.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!ProtectionConfig.CONFIG.enabled.get()) {
            return;
        }

        ProtectionCommands.register(event.getDispatcher());
        VonixCore.LOGGER.info("[VonixCore] Protection commands registered (/co, /vp)");
    }

    /**
     * Handle left-click block in inspector mode.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;

        if (ProtectionCommands.isInspecting(player.getUUID())) {
            event.setCanceled(true);
            BlockPos pos = event.getPos();
            ProtectionCommands.inspectBlock(player, pos);
        }
    }

    /**
     * Handle right-click block in inspector mode.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!ProtectionConfig.CONFIG.enabled.get())
            return;

        if (ProtectionCommands.isInspecting(player.getUUID())) {
            event.setCanceled(true);
            BlockPos pos = event.getPos();
            ProtectionCommands.inspectBlock(player, pos);
        }
    }
}
