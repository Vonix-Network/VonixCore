package network.vonix.vonixcore.discord;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;

/**
 * Discord event handler for Fabric API events.
 */
public class DiscordEventHandler {

    public static void register() {
        // Player join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            DiscordManager.getInstance().sendPlayerJoin(player);
            DiscordManager.getInstance().updateBotStatus();
        });

        // Player leave
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            DiscordManager.getInstance().sendPlayerLeave(player);
            DiscordManager.getInstance().updateBotStatus();
        });

        // Chat messages
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String content = message.signedContent();
            DiscordManager.getInstance().sendChatMessage(sender, content);
        });
    }
}
