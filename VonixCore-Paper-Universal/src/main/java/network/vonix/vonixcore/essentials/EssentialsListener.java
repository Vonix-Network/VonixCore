package network.vonix.vonixcore.essentials;

import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class EssentialsListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!EssentialsConfig.enabled)
            return;

        if (EssentialsConfig.economyEnabled) {
            // Ensure player has an economy account
            EconomyManager.getInstance().getBalance(event.getPlayer().getUniqueId());
        }
    }
}
