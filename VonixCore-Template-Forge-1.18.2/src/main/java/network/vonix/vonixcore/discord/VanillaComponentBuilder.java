package network.vonix.vonixcore.discord;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

/**
 * Builder class for creating vanilla-style advancement message components.
 * Uses TextComponent for Forge 1.18.2 compatibility.
 */
public class VanillaComponentBuilder {
    
    private static final ChatFormatting SERVER_PREFIX_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting BRACKET_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting PLAYER_NAME_COLOR = ChatFormatting.WHITE;
    private static final ChatFormatting CONNECTOR_COLOR = ChatFormatting.WHITE;
    
    public MutableComponent buildAdvancementMessage(AdvancementData data, String serverPrefix) {
        if (data == null) throw new IllegalArgumentException("AdvancementData cannot be null");
        if (serverPrefix == null) throw new IllegalArgumentException("Server prefix cannot be null");
        
        MutableComponent message = new TextComponent("");
        
        if (!serverPrefix.trim().isEmpty()) {
            message.append(createServerPrefixComponent(serverPrefix.trim()));
            message.append(new TextComponent(" "));
        }
        
        message.append(new TextComponent(data.getPlayerName()).withStyle(PLAYER_NAME_COLOR));
        message.append(new TextComponent(" has made the advancement ").withStyle(CONNECTOR_COLOR));
        
        MutableComponent advancementComponent = new TextComponent("[" + data.getAdvancementTitle() + "]")
                .withStyle(Style.EMPTY
                        .withColor(data.getType().getColor())
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                new TextComponent(data.getAdvancementDescription()).withStyle(ChatFormatting.WHITE))));
        
        message.append(advancementComponent);
        return message;
    }
    
    private MutableComponent createServerPrefixComponent(String serverPrefix) {
        MutableComponent prefixComponent = new TextComponent("");
        prefixComponent.append(new TextComponent("[").withStyle(BRACKET_COLOR));
        prefixComponent.append(new TextComponent(serverPrefix).withStyle(SERVER_PREFIX_COLOR));
        prefixComponent.append(new TextComponent("]").withStyle(BRACKET_COLOR));
        return prefixComponent;
    }
    
    public MutableComponent buildAdvancementMessage(AdvancementData data) {
        return buildAdvancementMessage(data, "");
    }
    
    public MutableComponent createFallbackComponent(String playerName, String advancementTitle, String serverPrefix) {
        MutableComponent fallback = new TextComponent("");
        
        if (serverPrefix != null && !serverPrefix.trim().isEmpty()) {
            fallback.append(createServerPrefixComponent(serverPrefix.trim()));
            fallback.append(new TextComponent(" "));
        }
        
        String safePlayerName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Someone";
        fallback.append(new TextComponent(safePlayerName + " has made an advancement").withStyle(ChatFormatting.YELLOW));
        
        if (advancementTitle != null && !advancementTitle.trim().isEmpty()) {
            fallback.append(new TextComponent(": " + advancementTitle.trim()).withStyle(ChatFormatting.WHITE));
        }
        
        return fallback;
    }
    
    public MutableComponent buildEventMessage(EventData data, String serverPrefix) {
        if (data == null) throw new IllegalArgumentException("EventData cannot be null");
        if (serverPrefix == null) throw new IllegalArgumentException("Server prefix cannot be null");
        
        MutableComponent message = new TextComponent("");
        if (!serverPrefix.trim().isEmpty()) {
            message.append(createServerPrefixComponent(serverPrefix.trim()));
            message.append(new TextComponent(" "));
        }
        
        ChatFormatting playerColor = getEventPlayerColor(data.getEventType());
        message.append(new TextComponent(data.getPlayerName()).withStyle(playerColor));
        message.append(new TextComponent(" " + data.getActionString()).withStyle(ChatFormatting.YELLOW));
        return message;
    }
    
    private ChatFormatting getEventPlayerColor(EventEmbedDetector.EventType eventType) {
        switch (eventType) {
            case JOIN: return ChatFormatting.GREEN;
            case LEAVE: return ChatFormatting.GRAY;
            case DEATH: return ChatFormatting.RED;
            default: return ChatFormatting.WHITE;
        }
    }
    
    public MutableComponent createEventFallbackComponent(String playerName, String action, String serverPrefix) {
        MutableComponent fallback = new TextComponent("");
        if (serverPrefix != null && !serverPrefix.trim().isEmpty()) {
            fallback.append(createServerPrefixComponent(serverPrefix.trim()));
            fallback.append(new TextComponent(" "));
        }
        String safePlayerName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Someone";
        String safeAction = (action != null && !action.trim().isEmpty()) ? action.trim() : "performed an action";
        fallback.append(new TextComponent(safePlayerName + " " + safeAction).withStyle(ChatFormatting.YELLOW));
        return fallback;
    }
}
