package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.config.AuthConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class AuthListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!AuthConfig.enabled)
            return;
        AuthManager.getInstance().handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!AuthConfig.enabled)
            return;
        AuthManager.getInstance().handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (!AuthManager.getInstance().isLoggedIn(event.getPlayer())) {
            // Allow looking around (pitch/yaw changes) but not moving x/y/z
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (!AuthManager.getInstance().isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou must log in to chat!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (!AuthManager.getInstance().isLoggedIn(event.getPlayer())) {
            String label = event.getMessage().split(" ")[0].toLowerCase();
            if (!label.equals("/login") && !label.equals("/register") && !label.equals("/l") && !label.equals("/reg")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou must log in to use commands!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (!AuthManager.getInstance().isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (!AuthManager.getInstance().isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (!AuthManager.getInstance().isLoggedIn(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player
                && !AuthManager.getInstance().isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!AuthConfig.enabled)
            return;
        if (event.getEntity() instanceof org.bukkit.entity.Player player
                && !AuthManager.getInstance().isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }
}
