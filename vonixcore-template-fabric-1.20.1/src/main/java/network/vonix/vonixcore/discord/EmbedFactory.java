package network.vonix.vonixcore.discord;

import com.google.gson.JsonObject;
import java.util.function.Consumer;

/**
 * Factory for creating Discord embed objects.
 */
public class EmbedFactory {

    public static Consumer<JsonObject> createServerStatusEmbed(String title, String description, int color, 
            String serverName, String footer) {
        return embed -> {
            embed.addProperty("title", title);
            embed.addProperty("description", description);
            embed.addProperty("color", color);
            
            JsonObject footerObj = new JsonObject();
            footerObj.addProperty("text", footer);
            embed.add("footer", footerObj);
        };
    }

    public static Consumer<JsonObject> createPlayerEventEmbed(String title, String description, int color,
            String username, String serverName, String footer, String thumbnailUrl) {
        return embed -> {
            embed.addProperty("title", title);
            embed.addProperty("description", "**" + username + "** " + description);
            embed.addProperty("color", color);
            
            JsonObject footerObj = new JsonObject();
            footerObj.addProperty("text", footer);
            embed.add("footer", footerObj);
            
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", thumbnailUrl);
                embed.add("thumbnail", thumbnail);
            }
        };
    }

    public static Consumer<JsonObject> createAdvancementEmbed(String emoji, int color, String username,
            String advancementTitle, String advancementDescription) {
        return embed -> {
            embed.addProperty("title", emoji + " Advancement!");
            embed.addProperty("description", "**" + username + "** unlocked **" + advancementTitle + "**\n" + advancementDescription);
            embed.addProperty("color", color);
        };
    }
}
