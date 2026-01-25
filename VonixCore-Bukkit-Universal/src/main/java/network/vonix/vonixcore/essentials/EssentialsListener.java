package network.vonix.vonixcore.essentials;

import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.teleport.TeleportManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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

    /**
     * Save death location for /back command.
     * This allows players to return to their death location using /back.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!EssentialsConfig.enabled)
            return;

        // Save death location for /back command
        TeleportManager.getInstance().saveLastLocation(event.getEntity(), true);
    }
}
