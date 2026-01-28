package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EconomyEventListener implements Listener {

    private final EconomyManager economyManager;

    public EconomyEventListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        economyManager.loadBalanceAsync(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        economyManager.unloadBalance(event.getPlayer().getUniqueId());
    }
}
