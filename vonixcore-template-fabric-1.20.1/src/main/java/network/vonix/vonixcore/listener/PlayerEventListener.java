package network.vonix.vonixcore.listener;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;

/**
 * Handles player connection events (non-Discord related).
 * Discord join/leave events are handled by DiscordEventHandler.
 */
public class PlayerEventListener {

    /**
     * Register all player event listeners.
     */
    public static void register() {
        // Player join event - just logging, Discord is handled by DiscordEventHandler
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            VonixCore.LOGGER.debug("[Essentials] Player joined: {}", player.getName().getString());
        });

        // Player leave event - just logging, Discord is handled by DiscordEventHandler
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            VonixCore.LOGGER.debug("[Essentials] Player left: {}", player.getName().getString());
        });
    }
}
