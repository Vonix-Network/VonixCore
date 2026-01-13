package network.vonix.vonixcore.listener;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.DiscordConfig;
import network.vonix.vonixcore.discord.DiscordManager;

/**
 * Handles player connection events.
 */
public class PlayerEventListener {

    /**
     * Register all player event listeners.
     */
    public static void register() {
        // Player join event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            VonixCore.LOGGER.debug("[Essentials] Player joined: {}", player.getName().getString());

            // Send to Discord if enabled
            if (VonixCore.getInstance().isDiscordEnabled() && DiscordConfig.getInstance().isSendJoin()) {
                VonixCore.executeAsync(() -> {
                    try {
                        DiscordManager.getInstance().sendPlayerJoin(player);
                    } catch (Exception e) {
                        VonixCore.LOGGER.error("[Discord] Failed to send join message: {}", e.getMessage());
                    }
                });
            }
        });

        // Player leave event
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            VonixCore.LOGGER.debug("[Essentials] Player left: {}", player.getName().getString());

            // Send to Discord if enabled
            if (VonixCore.getInstance().isDiscordEnabled() && DiscordConfig.getInstance().isSendLeave()) {
                VonixCore.executeAsync(() -> {
                    try {
                        DiscordManager.getInstance().sendPlayerLeave(player);
                    } catch (Exception e) {
                        VonixCore.LOGGER.error("[Discord] Failed to send leave message: {}", e.getMessage());
                    }
                });
            }
        });
    }
}
