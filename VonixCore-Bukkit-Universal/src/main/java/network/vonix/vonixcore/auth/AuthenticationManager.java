package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player authentication state, freeze status, and session timeouts.
 */
public class AuthenticationManager {
    private static final Map<UUID, PlayerAuthState> playerStates = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerTokens = new ConcurrentHashMap<>();
    private static final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    private static volatile boolean freezeEnabled = false;

    public enum PlayerAuthState {
        UNAUTHENTICATED, AUTHENTICATED, PENDING_REGISTRATION
    }

    public static void updateFreezeCache() {
        freezeEnabled = AuthConfig.requireAuthentication && AuthConfig.freezeUnauthenticated;
    }

    public static void onPlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        playerStates.put(uuid, PlayerAuthState.UNAUTHENTICATED);

        if (AuthConfig.requireAuthentication) {
            VonixCore.getInstance().getLogger().info("[Auth] Player " + username + " joined - checking registration");

            VonixNetworkAPI.checkPlayerRegistration(username, uuid.toString())
                    .thenAccept(response -> {
                        Bukkit.getScheduler().runTask(VonixCore.getInstance(), () -> {
                            if (response.registered) {
                                player.sendMessage(AuthConfig.loginRequiredMessage);
                            } else {
                                runAutoRegister(player, username, uuid);
                            }
                        });
                    })
                    .exceptionally(e -> {
                        Bukkit.getScheduler().runTask(VonixCore.getInstance(),
                                () -> runAutoRegister(player, username, uuid));
                        return null;
                    });

            int timeout = AuthConfig.loginTimeout;
            if (timeout > 0) {
                BukkitTask task = Bukkit.getScheduler().runTaskLater(VonixCore.getInstance(), () -> {
                    timeoutTasks.remove(uuid);
                    if (!isAuthenticated(uuid) && player.isOnline()) {
                        player.kickPlayer("§c[VonixCore] Authentication timeout");
                    }
                }, timeout * 20L);
                timeoutTasks.put(uuid, task);
            }
        } else if (AuthConfig.warnOfAuth) {
            player.sendMessage(AuthConfig.authWarningMessage);
        }
    }

    private static void runAutoRegister(Player player, String username, UUID uuid) {
        player.sendMessage(AuthConfig.generatingCodeMessage);
        setPendingRegistration(uuid);

        VonixNetworkAPI.generateRegistrationCode(username, uuid.toString())
                .thenAccept(response -> {
                    Bukkit.getScheduler().runTask(VonixCore.getInstance(), () -> {
                        if (response.code != null) {
                            String msg = AuthConfig.registrationCodeMessage.replace("{code}", response.code);
                            player.sendMessage(msg);
                            player.sendMessage("§a§l[CLICK HERE] §6" + AuthConfig.registrationUrl);
                            player.sendMessage("§7Or use: §e/register <password>");
                        } else if (response.already_registered) {
                            player.sendMessage("§aAlready registered! Use §e/login <password>");
                            playerStates.put(uuid, PlayerAuthState.UNAUTHENTICATED);
                        } else {
                            player.sendMessage("§cRegistration failed. Try §e/register");
                            playerStates.put(uuid, PlayerAuthState.UNAUTHENTICATED);
                        }
                    });
                });
    }

    public static void onPlayerLeave(UUID uuid) {
        playerStates.remove(uuid);
        playerTokens.remove(uuid);
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null)
            task.cancel();
    }

    public static void setAuthenticated(UUID uuid, String token) {
        playerStates.put(uuid, PlayerAuthState.AUTHENTICATED);
        if (token != null)
            playerTokens.put(uuid, token);
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null)
            task.cancel();

        VonixCore.getInstance().getLogger().info("[Auth] Player " + uuid + " authenticated");
    }

    public static void setPendingRegistration(UUID uuid) {
        playerStates.put(uuid, PlayerAuthState.PENDING_REGISTRATION);
    }

    public static boolean isAuthenticated(UUID uuid) {
        return playerStates.getOrDefault(uuid, PlayerAuthState.UNAUTHENTICATED) == PlayerAuthState.AUTHENTICATED;
    }

    public static boolean shouldFreeze(UUID uuid) {
        return freezeEnabled && !isAuthenticated(uuid);
    }

    public static void clearAll() {
        playerStates.clear();
        playerTokens.clear();
        timeoutTasks.values().forEach(BukkitTask::cancel);
        timeoutTasks.clear();
    }
}
