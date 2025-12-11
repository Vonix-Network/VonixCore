package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles authentication events - freezing unauthenticated players.
 */
public class AuthEventHandler implements Listener {
    private static final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastChatReminder = new ConcurrentHashMap<>();

    private static boolean isFrozen(UUID uuid) {
        return frozenPlayers.computeIfAbsent(uuid, AuthenticationManager::shouldFreeze);
    }

    public static void updateFreezeState(UUID uuid) {
        if (AuthenticationManager.isAuthenticated(uuid)) {
            frozenPlayers.remove(uuid);
        } else {
            frozenPlayers.put(uuid, AuthenticationManager.shouldFreeze(uuid));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        AuthenticationManager.onPlayerJoin(player);
        updateFreezeState(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        AuthenticationManager.onPlayerLeave(uuid);
        frozenPlayers.remove(uuid);
        lastChatReminder.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!AuthConfig.freezeUnauthenticated)
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isFrozen(uuid)) {
            // Allow head movement, prevent position change
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!AuthConfig.freezeUnauthenticated)
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isFrozen(uuid)) {
            event.setCancelled(true);
            sendAuthReminder(player, uuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!AuthConfig.freezeUnauthenticated)
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (isFrozen(uuid)) {
            event.setCancelled(true);
            sendAuthReminder(player, uuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!isFrozen(uuid))
            return;

        String cmd = event.getMessage().toLowerCase();
        // Allow auth commands
        if (cmd.startsWith("/login") || cmd.startsWith("/register") || cmd.startsWith("/l ")) {
            return;
        }

        event.setCancelled(true);
        sendAuthReminder(player, uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!AuthConfig.freezeUnauthenticated)
            return;

        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void sendAuthReminder(Player player, UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastChatReminder.get(uuid);
        if (last == null || (now - last) >= 5000) {
            player.sendMessage("§cYou must authenticate! Use §e/login <password>§c or §e/register");
            lastChatReminder.put(uuid, now);
        }
    }
}
