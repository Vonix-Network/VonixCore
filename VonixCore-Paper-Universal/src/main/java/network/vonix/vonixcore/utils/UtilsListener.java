package network.vonix.vonixcore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.event.entity.EntityDamageEvent;

public class UtilsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player) {
            if (UtilsManager.getInstance().isGod(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        String nick = UtilsManager.getInstance().getNick(event.getPlayer().getUniqueId());
        if (nick != null) {
            // Validate nick format if needed, avoiding full adventure serialization for now
            // if simple
            // But usually we want color codes
            try {
                Component nickComponent = MiniMessage.miniMessage().deserialize(nick);
                event.getPlayer().displayName(nickComponent);
                event.getPlayer().playerListName(nickComponent);
            } catch (Exception e) {
                // Fallback or ignore invalid storage
                event.getPlayer().setDisplayName(nick);
                event.getPlayer().setPlayerListName(nick);
            }
        }
    }
}
