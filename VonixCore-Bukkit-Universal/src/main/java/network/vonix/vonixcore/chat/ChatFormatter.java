package network.vonix.vonixcore.chat;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.command.UtilityCommands;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles chat formatting with rank colors, prefixes, and nicknames.
 */
public class ChatFormatter implements Listener {

    private static final Map<UUID, String> playerPrefixes = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerSuffixes = new ConcurrentHashMap<>();
    private static final Map<UUID, ChatColor> playerColors = new ConcurrentHashMap<>();

    // Configuration
    public static boolean enabled = true;
    public static String chatFormat = "{prefix}{displayname}{suffix}: {message}";
    public static String defaultPrefix = "";
    public static String defaultSuffix = "";
    public static ChatColor defaultColor = ChatColor.WHITE;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Get nickname or display name
        String nickname = UtilityCommands.getNickname(uuid);
        String displayName = nickname != null ? nickname : player.getDisplayName();

        // Get prefix/suffix/color
        String prefix = playerPrefixes.getOrDefault(uuid, defaultPrefix);
        String suffix = playerSuffixes.getOrDefault(uuid, defaultSuffix);
        ChatColor color = playerColors.getOrDefault(uuid, defaultColor);

        // Format message
        String message = event.getMessage();

        // Apply color codes if player has permission
        if (player.hasPermission("vonixcore.chat.color")) {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        // Build formatted message
        String formatted = chatFormat
                .replace("{prefix}", prefix)
                .replace("{displayname}", color + displayName + ChatColor.RESET)
                .replace("{suffix}", suffix)
                .replace("{message}", message)
                .replace("{player}", player.getName())
                .replace("{world}", player.getWorld().getName());

        event.setFormat(formatted.replace("%", "%%"));
    }

    /**
     * Sets a player's chat prefix.
     */
    public static void setPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            playerPrefixes.remove(uuid);
        } else {
            playerPrefixes.put(uuid, ChatColor.translateAlternateColorCodes('&', prefix));
        }
    }

    /**
     * Sets a player's chat suffix.
     */
    public static void setSuffix(UUID uuid, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            playerSuffixes.remove(uuid);
        } else {
            playerSuffixes.put(uuid, ChatColor.translateAlternateColorCodes('&', suffix));
        }
    }

    /**
     * Sets a player's name color.
     */
    public static void setColor(UUID uuid, ChatColor color) {
        if (color == null) {
            playerColors.remove(uuid);
        } else {
            playerColors.put(uuid, color);
        }
    }

    /**
     * Gets a player's prefix.
     */
    public static String getPrefix(UUID uuid) {
        return playerPrefixes.getOrDefault(uuid, defaultPrefix);
    }

    /**
     * Gets a player's suffix.
     */
    public static String getSuffix(UUID uuid) {
        return playerSuffixes.getOrDefault(uuid, defaultSuffix);
    }

    /**
     * Clears all chat data for a player.
     */
    public static void clearPlayer(UUID uuid) {
        playerPrefixes.remove(uuid);
        playerSuffixes.remove(uuid);
        playerColors.remove(uuid);
    }
}
