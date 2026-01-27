package network.vonix.vonixcore.discord;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.DiscordConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DiscordListener implements Listener {

    private final VonixCore plugin;

    public DiscordListener(VonixCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!DiscordConfig.enabled)
            return;
        // Add server prefix to username for webhook messages
        String serverPrefix = DiscordConfig.serverPrefix;
        String formattedUsername = serverPrefix + event.getPlayer().getName();
        DiscordManager.getInstance().sendMinecraftMessage(formattedUsername, event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!DiscordConfig.enabled)
            return;
        DiscordManager.getInstance().sendJoinEmbed(event.getPlayer().getName());
        DiscordManager.getInstance().updateBotStatus();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!DiscordConfig.enabled)
            return;
        DiscordManager.getInstance().sendLeaveEmbed(event.getPlayer().getName());

        // Delay status update slightly to ensure player count is accurate
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            DiscordManager.getInstance().updateBotStatus();
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!DiscordConfig.enabled)
            return;
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null) {
            DiscordManager.getInstance().sendSystemMessage("💀 " + deathMessage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (!DiscordConfig.enabled)
            return;
        // Simplified advancement logging for now
    }
}
