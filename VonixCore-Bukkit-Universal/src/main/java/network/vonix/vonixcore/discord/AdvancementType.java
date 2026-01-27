package network.vonix.vonixcore.discord;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Enumeration of advancement types with their display text and color mappings.
 * Uses Adventure API NamedTextColor for Bukkit compatibility.
 */
public enum AdvancementType {
    NORMAL("Advancement Made", NamedTextColor.YELLOW),
    GOAL("Goal Reached", NamedTextColor.YELLOW),
    CHALLENGE("Challenge Complete", NamedTextColor.LIGHT_PURPLE);
    
    private final String displayText;
    private final NamedTextColor color;
    
    AdvancementType(String displayText, NamedTextColor color) {
        this.displayText = displayText;
        this.color = color;
    }
    
    public String getDisplayText() { return displayText; }
    public NamedTextColor getColor() { return color; }
    
    public static AdvancementType fromTitle(String title) {
        if (title == null) return NORMAL;
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("challenge")) return CHALLENGE;
        else if (lowerTitle.contains("goal")) return GOAL;
        else return NORMAL;
    }
}
