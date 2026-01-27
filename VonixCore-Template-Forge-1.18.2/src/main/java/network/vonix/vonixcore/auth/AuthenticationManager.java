package network.vonix.vonixcore.auth;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.auth.api.VonixNetworkAPI;
import network.vonix.vonixcore.config.AuthConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages player authentication state, freeze status, and session timeouts.
 * Ported from Forge for 1:1 parity.
 */
public class AuthenticationManager {
    private static final Map<UUID, PlayerAuthState> playerStates = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerTokens = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VonixCore-Auth-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean freezeEnabled = false;

    public enum PlayerAuthState {
        UNAUTHENTICATED, AUTHENTICATED, PENDING_REGISTRATION
    }

    public static void updateFreezeCache() {
        try {
            freezeEnabled = AuthConfig.REQUIRE_AUTHENTICATION.get() && AuthConfig.FREEZE_UNAUTHENTICATED.get();
        } catch (IllegalStateException e) {
            VonixCore.LOGGER.warn("Cannot update freeze cache - config not yet initialized: {}", e.getMessage());
            freezeEnabled = false; // Default to false if config is not ready
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String username = player.getName().getString();
        playerStates.put(uuid, PlayerAuthState.UNAUTHENTICATED);

        if (AuthConfig.REQUIRE_AUTHENTICATION.get()) {
            VonixCore.LOGGER.info("[Auth] Player {} joined - checking registration", username);

            VonixNetworkAPI.checkPlayerRegistration(username, uuid.toString())
                    .thenAccept(response -> {
                        if (response.registered) {
                            player.sendMessage(new TextComponent(AuthConfig.LOGIN_REQUIRED_MESSAGE.get()), uuid);
                        } else {
                            runAutoRegister(player, username, uuid);
                        }
                    })
                    .exceptionally(e -> {
                        runAutoRegister(player, username, uuid);
                        return null;
                    });

            int timeout = AuthConfig.LOGIN_TIMEOUT.get();
            if (timeout > 0) {
                ScheduledFuture<?> task = scheduler.schedule(() -> {
                    timeoutTasks.remove(uuid);
                    if (!isAuthenticated(uuid) && player.connection != null) {
                        player.connection.disconnect(new TextComponent("§c[VonixCore] Authentication timeout"));
                    }
                }, timeout, TimeUnit.SECONDS);
                timeoutTasks.put(uuid, task);
            }
        } else if (AuthConfig.WARN_OF_AUTH.get()) {
            player.sendMessage(new TextComponent(AuthConfig.AUTH_WARNING_MESSAGE.get()), uuid);
        }
    }

    private static void runAutoRegister(ServerPlayer player, String username, UUID uuid) {
        player.sendMessage(new TextComponent(AuthConfig.GENERATING_CODE_MESSAGE.get()), uuid);
        setPendingRegistration(uuid);

        VonixNetworkAPI.generateRegistrationCode(username, uuid.toString())
                .thenAccept(response -> {
                    if (response.code != null) {
                        String msg = AuthConfig.REGISTRATION_CODE_MESSAGE.get().replace("{code}", response.code);
                        player.sendMessage(new TextComponent(msg), uuid);

                        Component link = new TextComponent("§a§l[CLICK HERE] §6Open Registration")
                                .setStyle(Style.EMPTY
                                        .withColor(0x00FFFF)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, AuthConfig.REGISTRATION_URL.get()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to register"))));
                        player.sendMessage(link, uuid);
                        player.sendMessage(new TextComponent("§7Or use: §e/register <password>"), uuid);
                    } else if (response.already_registered) {
                        player.sendMessage(new TextComponent("§aAlready registered! Use §e/login <password>"), uuid);
                        playerStates.put(uuid, PlayerAuthState.UNAUTHENTICATED);
                    } else {
                        player.sendMessage(new TextComponent("§cRegistration failed. Try §e/register"), uuid);
                        playerStates.put(uuid, PlayerAuthState.UNAUTHENTICATED);
                    }
                });
    }

    public static void onPlayerLeave(UUID uuid) {
        playerStates.remove(uuid);
        playerTokens.remove(uuid);
        ScheduledFuture<?> task = timeoutTasks.remove(uuid);
        if (task != null)
            task.cancel(false);
    }

    public static void setAuthenticated(UUID uuid, String token) {
        playerStates.put(uuid, PlayerAuthState.AUTHENTICATED);
        if (token != null)
            playerTokens.put(uuid, token);
        ScheduledFuture<?> task = timeoutTasks.remove(uuid);
        if (task != null)
            task.cancel(false);
        VonixCore.LOGGER.info("[Auth] Player {} authenticated", uuid);
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
        timeoutTasks.values().forEach(t -> t.cancel(false));
        timeoutTasks.clear();
    }

    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
