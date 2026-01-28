package network.vonix.vonixcore.listener;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.teleport.TeleportManager;

/**
 * Handles player-specific events like death for /back location.
 */
@Mod.EventBusSubscriber(modid = VonixCore.MODID)
public class PlayerEventListener {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!EssentialsConfig.getInstance().isEnabled()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // Save location for /back
            TeleportManager.getInstance().saveLastLocation(player, true);
        }
    }
}
