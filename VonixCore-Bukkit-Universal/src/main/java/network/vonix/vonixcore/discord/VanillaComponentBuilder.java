package network.vonix.vonixcore.discord;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builder class for creating vanilla-style advancement message components.
 * Uses Adventure API for Bukkit compatibility.
 */
public class VanillaComponentBuilder {
    
    private static final NamedTextColor SERVER_PREFIX_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor BRACKET_COLOR = NamedTextColor.GREEN;
    private static final NamedTextColor PLAYER_NAME_COLOR = NamedTextColor.WHITE;
    private static final NamedTextColor CONNECTOR_COLOR = NamedTextColor.WHITE;
    
    public Component buildAdvancementMessage(AdvancementData data, String serverPrefix) {
        if (data == null) throw new IllegalArgumentException("AdvancementData cannot be null");
        if (serverPrefix == null) throw new IllegalArgumentException("Server prefix cannot be null");
        
        Component message = Component.empty();
        
        if (!serverPrefix.trim().isEmpty()) {
            message = message.append(createServerPrefixComponent(serverPrefix.trim()));
            message = message.append(Component.text(" "));
        }
        
        message = message.append(Component.text(data.getPlayerName(), PLAYER_NAME_COLOR));
        message = message.append(Component.text(" has made the advancement ", CONNECTOR_COLOR));
        
        Component advancementComponent = Component.text("[" + data.getAdvancementTitle() + "]", data.getType().getColor())
                .hoverEvent(HoverEvent.showText(Component.text(data.getAdvancementDescription(), NamedTextColor.WHITE)));
        
        message = message.append(advancementComponent);
        return message;
    }
    
    private Component createServerPrefixComponent(String serverPrefix) {
        return Component.empty()
                .append(Component.text("[", BRACKET_COLOR))
                .append(Component.text(serverPrefix, SERVER_PREFIX_COLOR))
                .append(Component.text("]", BRACKET_COLOR));
    }
    
    public Component buildAdvancementMessage(AdvancementData data) {
        return buildAdvancementMessage(data, "");
    }
    
    public Component createFallbackComponent(String playerName, String advancementTitle, String serverPrefix) {
        Component fallback = Component.empty();
        
        if (serverPrefix != null && !serverPrefix.trim().isEmpty()) {
            fallback = fallback.append(createServerPrefixComponent(serverPrefix.trim()));
            fallback = fallback.append(Component.text(" "));
        }
        
        String safePlayerName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Someone";
        fallback = fallback.append(Component.text(safePlayerName + " has made an advancement", NamedTextColor.YELLOW));
        
        if (advancementTitle != null && !advancementTitle.trim().isEmpty()) {
            fallback = fallback.append(Component.text(": " + advancementTitle.trim(), NamedTextColor.WHITE));
        }
        
        return fallback;
    }
    
    /**
     * Builds a simplified event message component with server prefix.
     * Format: [ServerPrefix] PlayerName action (e.g., "[MCSurvival] Steve joined")
     */
    public Component buildEventMessage(EventData data, String serverPrefix) {
        if (data == null) throw new IllegalArgumentException("EventData cannot be null");
        if (serverPrefix == null) throw new IllegalArgumentException("Server prefix cannot be null");
        
        Component message = Component.empty();
        
        if (!serverPrefix.trim().isEmpty()) {
            message = message.append(createServerPrefixComponent(serverPrefix.trim()));
            message = message.append(Component.text(" "));
        }
        
        NamedTextColor playerColor = getEventPlayerColor(data.getEventType());
        message = message.append(Component.text(data.getPlayerName(), playerColor));
        message = message.append(Component.text(" " + data.getActionString(), NamedTextColor.YELLOW));
        
        return message;
    }
    
    private NamedTextColor getEventPlayerColor(EventEmbedDetector.EventType eventType) {
        switch (eventType) {
            case JOIN: return NamedTextColor.GREEN;
            case LEAVE: return NamedTextColor.GRAY;
            case DEATH: return NamedTextColor.RED;
            default: return NamedTextColor.WHITE;
        }
    }
    
    public Component createEventFallbackComponent(String playerName, String action, String serverPrefix) {
        Component fallback = Component.empty();
        
        if (serverPrefix != null && !serverPrefix.trim().isEmpty()) {
            fallback = fallback.append(createServerPrefixComponent(serverPrefix.trim()));
            fallback = fallback.append(Component.text(" "));
        }
        
        String safePlayerName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Someone";
        String safeAction = (action != null && !action.trim().isEmpty()) ? action.trim() : "performed an action";
        fallback = fallback.append(Component.text(safePlayerName + " " + safeAction, NamedTextColor.YELLOW));
        
        return fallback;
    }
}
