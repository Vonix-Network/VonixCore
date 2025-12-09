package network.vonix.vonixcore.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import network.vonix.vonixcore.config.ChatConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!ChatConfig.enabled || !ChatConfig.formatChat)
            return;

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String format = ChatConfig.format;

            // Simplified Prefix/Suffix (Placeholder for LuckPerms integration later)
            String prefix = ""; // Vault/LuckPerms lookup would go here
            String suffix = "";

            // In a real implementation, we would fetch prefix/suffix from a vault hook or
            // permission manager
            // For now, we'll leave them empty or use simple placeholders if needed

            TagResolver placeholders = TagResolver.resolver(
                    Placeholder.component("displayname", sourceDisplayName),
                    Placeholder.component("message", message),
                    Placeholder.parsed("prefix", prefix),
                    Placeholder.parsed("suffix", suffix),
                    Placeholder.parsed("player", source.getName()));

            return miniMessage.deserialize(format, placeholders);
        });
    }
}
