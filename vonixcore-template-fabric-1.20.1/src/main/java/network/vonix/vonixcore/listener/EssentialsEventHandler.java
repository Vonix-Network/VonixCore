package network.vonix.vonixcore.listener;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.DiscordConfig;
import network.vonix.vonixcore.discord.DiscordManager;

/**
 * Handles essentials-related events like chat and advancements.
 */
public class EssentialsEventHandler {

    /**
     * Register all essentials event listeners.
     */
    public static void register() {
        // Chat message event - send to Discord
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (VonixCore.getInstance().isDiscordEnabled()) {
                VonixCore.executeAsync(() -> {
                    try {
                        String content = message.signedContent();
                        DiscordManager.getInstance().sendChatMessage(sender, content);
                    } catch (Exception e) {
                        VonixCore.LOGGER.error("[Discord] Failed to send chat message: {}", e.getMessage());
                    }
                });
            }
        });

        // Command message event - can be used for logging
        ServerMessageEvents.COMMAND_MESSAGE.register((message, source, params) -> {
            // Can log commands here if needed
        });
    }
}
