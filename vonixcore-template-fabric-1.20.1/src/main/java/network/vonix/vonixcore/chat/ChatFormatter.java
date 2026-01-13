package network.vonix.vonixcore.chat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;

/**
 * Chat formatting utilities.
 */
public class ChatFormatter {

    /**
     * Format a chat message with player name and message.
     */
    public static Component formatChatMessage(ServerPlayer player, String message) {
        String format = "<%s> %s";
        return Component.literal(String.format(format, player.getName().getString(), message));
    }

    /**
     * Format a broadcast message.
     */
    public static Component formatBroadcast(String message) {
        return Component.literal("§c[Broadcast] §f" + message);
    }

    /**
     * Format a staff message.
     */
    public static Component formatStaffMessage(ServerPlayer player, String message) {
        return Component.literal("§c[Staff] §7" + player.getName().getString() + "§f: " + message);
    }

    /**
     * Strip color codes from a message.
     */
    public static String stripColors(String message) {
        if (message == null) return null;
        return message.replaceAll("§[0-9a-fk-or]", "");
    }
}
