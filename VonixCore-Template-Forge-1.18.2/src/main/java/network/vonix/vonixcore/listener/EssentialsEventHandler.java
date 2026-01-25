package network.vonix.vonixcore.listener;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.command.TeleportCommands;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.teleport.TeleportManager;

/**
 * Event handler for essentials features.
 */
@Mod.EventBusSubscriber(modid = VonixCore.MODID)
public class EssentialsEventHandler {

    /**
     * Register all essentials commands.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!EssentialsConfig.CONFIG.enabled.get()) {
            return;
        }

        VonixCore.LOGGER.info("[VonixCore] Registering teleport commands...");
        TeleportCommands.register(event.getDispatcher());
        VonixCore.LOGGER.info("[VonixCore] Teleport commands registered");
    }

    /**
     * Save death location for /back command.
     * This allows players to return to their death location using /back.
     * The isDeath flag ensures the death-specific cooldown is applied.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!EssentialsConfig.CONFIG.enabled.get()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // Save death location with isDeath=true so cooldown applies
            TeleportManager.getInstance().saveLastLocation(player, true);
            VonixCore.LOGGER.debug("[VonixCore] Saved death location for {}", player.getName().getString());
        }
    }
}

