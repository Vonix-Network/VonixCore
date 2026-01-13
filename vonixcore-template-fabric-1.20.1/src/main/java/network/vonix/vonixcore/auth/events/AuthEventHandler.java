package network.vonix.vonixcore.auth.events;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.auth.AuthenticationManager;
import network.vonix.vonixcore.config.AuthConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player events for authentication - freezing unauthenticated players.
 */
public class AuthEventHandler {
    private static final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastChatReminder = new ConcurrentHashMap<>();

    public static void register() {
        if (!AuthConfig.getInstance().isEnabled()) {
            return;
        }

        // Player join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            AuthenticationManager.onPlayerJoin(player);
            updateFreezeState(player.getUUID());
        });

        // Player leave event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            AuthenticationManager.onPlayerLeave(uuid);
            frozenPlayers.remove(uuid);
            lastChatReminder.remove(uuid);
        });

        // Block break event - prevent if frozen
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer && isFrozen(serverPlayer.getUUID())) {
                return false; // Cancel break
            }
            return true;
        });

        // Block interaction - prevent if frozen
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer && isFrozen(serverPlayer.getUUID())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

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
}
