package network.vonix.vonixcore.listener;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.economy.EconomyManager;

@Mod.EventBusSubscriber(modid = VonixCore.MODID)
public class EconomyEventListener {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // Preload balance asynchronously
            EconomyManager.getInstance().loadBalanceAsync(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            // Unload balance to free memory
            EconomyManager.getInstance().unloadBalance(player.getUUID());
        }
    }
}
