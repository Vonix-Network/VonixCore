package network.vonix.vonixcore.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.DiscordConfig;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordManager {

    private static DiscordManager instance;
    private final VonixCore plugin;
    private final OkHttpClient httpClient;
    private final BlockingQueue<WebhookMessage> messageQueue;
    private Thread messageQueueThread;
    private boolean running = false;
    private String ourWebhookId = null;
    private String eventWebhookId = null;
    private DiscordApi discordApi = null;
    private ServerPrefixConfig serverPrefixConfig = null;
    
    // Advancement message formatting components
    private final AdvancementEmbedDetector advancementDetector;
    private final AdvancementDataExtractor advancementExtractor;
    private final VanillaComponentBuilder componentBuilder;
    
    // Event message formatting components
    private final EventEmbedDetector eventDetector;
    private final EventDataExtractor eventExtractor;

    private static final Pattern DISCORD_MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\((https?://[^)]+)\\)");

    private DiscordManager() {
        this.plugin = VonixCore.getInstance();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.messageQueue = new LinkedBlockingQueue<>(
                DiscordConfig.messageQueueSize);
        
        // Initialize advancement processing components
        this.advancementDetector = new AdvancementEmbedDetector();
        this.advancementExtractor = new AdvancementDataExtractor();
        this.componentBuilder = new VanillaComponentBuilder();
        
        // Initialize event processing components
        this.eventDetector = new EventEmbedDetector();
        this.eventExtractor = new EventDataExtractor();
    }

    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }

    public void initialize() {
        if (!DiscordConfig.enabled) {
            plugin.getLogger().info("[Discord] Discord integration is disabled in config");
            return;
        }

        String token = DiscordConfig.botToken;

        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("[Discord] Bot token not configured, Discord integration disabled");
            return;
        }

        plugin.getLogger().info("[Discord] Starting Discord integration (Javacord + Webhooks)...");

        extractWebhookId();

        // Initialize server prefix configuration
        try {
            serverPrefixConfig = new ServerPrefixConfig();
            String configPrefix = DiscordConfig.serverPrefix;
            if (configPrefix != null && !configPrefix.trim().isEmpty()) {
                String strippedPrefix = stripBracketsFromPrefix(configPrefix.trim());
                serverPrefixConfig.setFallbackPrefix(strippedPrefix);
            }
            plugin.getLogger().info("[Discord] Server prefix configuration system initialized");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Discord] Failed to initialize server prefix configuration", e);
            serverPrefixConfig = new ServerPrefixConfig();
        }

        running = true;
        startMessageQueueThread();

        // Initialize Javacord
        initializeJavacord(token);

        // Send startup embed
        String serverName = DiscordConfig.serverName;
        sendStartupEmbed(serverName);

        plugin.getLogger().info("[Discord] Discord integration initialized successfully!");
        if (ourWebhookId != null) {
            plugin.getLogger().info("[Discord] Chat Webhook ID: " + ourWebhookId);
        }
    }

    public void shutdown() {
        if (!running)
            return;

        plugin.getLogger().info("[Discord] Shutting down Discord integration...");
        running = false;

        if (discordApi != null) {
            sendShutdownEmbed(DiscordConfig.serverName);
        }

        if (messageQueueThread != null && messageQueueThread.isAlive()) {
            messageQueueThread.interrupt();
        }

        if (discordApi != null) {
            discordApi.disconnect().join();
            discordApi = null;
        }

        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdownNow();
            httpClient.connectionPool().evictAll();
        }

        plugin.getLogger().info("[Discord] Discord integration shut down");
    }

    private void extractWebhookId() {
        ourWebhookId = extractWebhookIdFromConfig(
                DiscordConfig.webhookId,
                DiscordConfig.webhookUrl,
                "chat");
        eventWebhookId = extractWebhookIdFromConfig(
                "",
                DiscordConfig.eventWebhookUrl,
                "event");
    }

    private String extractWebhookIdFromConfig(String manualId, String webhookUrl, String type) {
        if (manualId != null && !manualId.isEmpty())
            return manualId;
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return extractIdFromWebhookUrl(webhookUrl);
        }
        return null;
    }

    private String extractIdFromWebhookUrl(String webhookUrl) {
        try {
            String[] parts = webhookUrl.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("webhooks".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Discord] Error extracting webhook ID", e);
        }
        return null;
    }

    private void initializeJavacord(String botToken) {
        // Run in async thread to not block main thread
        new Thread(() -> {
            try {
                String channelId = DiscordConfig.channelId;
                if (channelId == null || channelId.equals("YOUR_CHANNEL_ID_HERE")) {
                    plugin.getLogger().warning("[Discord] Channel ID not configured");
                    return;
                }

                discordApi = new DiscordApiBuilder()
                        .setToken(botToken)
                        .setIntents(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT)
                        .login()
                        .join();

                plugin.getLogger().info("[Discord] Connected as: " + discordApi.getYourself().getName());

                long channelIdLong = Long.parseLong(channelId);
                String eventChannelId = DiscordConfig.eventChannelId;
                Long eventChannelIdLong = null;
                if (eventChannelId != null && !eventChannelId.isEmpty()) {
                    try {
                        eventChannelIdLong = Long.parseLong(eventChannelId);
                    } catch (NumberFormatException ignored) {
                    }
                }

                final Long finalEventChannelId = eventChannelIdLong;
                discordApi.addMessageCreateListener(event -> {
                    long msgChannelId = event.getChannel().getId();
                    if (msgChannelId == channelIdLong
                            || (finalEventChannelId != null && msgChannelId == finalEventChannelId)) {
                        processJavacordMessage(event);
                    }
                });

                // Register /list slash command
                registerListCommand();

                updateBotStatus();

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[Discord] Failed to initialize Javacord", e);
                discordApi = null;
            }
        }).start();
    }

    /**
     * Register the /list slash command with Discord
     */
    private void registerListCommand() {
        if (discordApi == null) {
            return;
        }

        try {
            SlashCommand.with("list", "Show online players on the Minecraft server")
                    .createGlobal(discordApi)
                    .join();

            discordApi.addSlashCommandCreateListener(event -> {
                SlashCommandInteraction interaction = event.getSlashCommandInteraction();
                if (interaction.getCommandName().equals("list")) {
                    handleSlashListCommand(interaction);
                }
            });

            plugin.getLogger().info("[Discord] Registered /list slash command");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Discord] Failed to register /list command", e);
        }
    }

    /**
     * Build a player list embed for Discord
     */
    private EmbedBuilder buildPlayerListEmbed() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        int onlinePlayers = players.size();
        int maxPlayers = Bukkit.getMaxPlayers();
        String serverName = DiscordConfig.serverName;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 " + serverName)
                .setColor(Color.GREEN)
                .setFooter("VonixCore · Player List");

        if (onlinePlayers == 0) {
            embed.setDescription("No players are currently online.");
        } else {
            StringBuilder playerListBuilder = new StringBuilder();
            int i = 0;
            for (Player player : players) {
                if (i > 0) {
                    playerListBuilder.append("\n");
                }
                playerListBuilder.append("• ").append(player.getName());
                i++;
            }
            embed.addField("Players " + onlinePlayers + "/" + maxPlayers, playerListBuilder.toString(), false);
        }

        return embed;
    }

    /**
     * Handle the /list slash command
     */
    private void handleSlashListCommand(SlashCommandInteraction interaction) {
        try {
            EmbedBuilder embed = buildPlayerListEmbed();
            interaction.createImmediateResponder()
                    .addEmbed(embed)
                    .respond();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Discord] Error handling /list command", e);
            interaction.createImmediateResponder()
                    .setContent("❌ An error occurred")
                    .respond();
        }
    }

    /**
     * Handle the !list text command with an embed
     */
    private void handleTextListCommand(org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            EmbedBuilder embed = buildPlayerListEmbed();
            event.getChannel().sendMessage(embed);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Discord] Error handling !list command", e);
        }
    }

    private void processJavacordMessage(org.javacord.api.event.message.MessageCreateEvent event) {
        boolean isBot = event.getMessageAuthor().asUser().map(user -> user.isBot()).orElse(false);
        boolean isWebhook = !event.getMessageAuthor().asUser().isPresent();

        String content = event.getMessageContent();
        String authorName = event.getMessageAuthor().getDisplayName();

        // Filter our own webhooks based on username prefix
        // The webhook username format is "[prefix]username", so check for bracket-wrapped prefix
        if (isWebhook) {
            String ourPrefix = "[" + getFallbackServerPrefix() + "]";
            if (authorName != null && authorName.startsWith(ourPrefix)) {
                return;
            }
        }

        // Filter other webhooks if configured
        if (DiscordConfig.ignoreWebhooks && isWebhook) {
            if (DiscordConfig.filterByPrefix) {
                // Use same prefix as sending to ensure accurate filtering
                String ourPrefix = "[" + getFallbackServerPrefix() + "]";
                if (authorName != null && authorName.startsWith(ourPrefix)) {
                    return;
                }
            } else {
                return;
            }
        }

        // Filter bots
        if (DiscordConfig.ignoreBots && isBot && !isWebhook) {
            return;
        }

        // Handle !list command with embed
        if (content.trim().equalsIgnoreCase("!list")) {
            handleTextListCommand(event);
            return;
        }

        // Check for empty content with embeds (often used for cross-server events)
        if (content.isEmpty()) {
            if (!event.getMessage().getEmbeds().isEmpty()) {
                // Check for advancement embeds and process them specially
                for (org.javacord.api.entity.message.embed.Embed embed : event.getMessage().getEmbeds()) {
                    if (advancementDetector.isAdvancementEmbed(embed)) {
                        processAdvancementEmbed(embed, event);
                        return;
                    }
                }
                
                // Check for event embeds (join/leave/death) and process them specially
                for (org.javacord.api.entity.message.embed.Embed embed : event.getMessage().getEmbeds()) {
                    if (eventDetector.isEventEmbed(embed)) {
                        processEventEmbed(embed, event);
                        return;
                    }
                }
            }
            return;
        }

        String formatted;
        if (isWebhook) {
            // Special formatting for cross-server messages (webhooks)
            String displayName = authorName;

            if (displayName.startsWith("[") && displayName.contains("]")) {
                int endBracket = displayName.indexOf("]");
                String serverPrefix = displayName.substring(0, endBracket + 1);
                String remainingName = displayName.substring(endBracket + 1).trim();

                if (remainingName.toLowerCase().contains("server")) {
                    // Event or generic server message: just prefix
                    displayName = serverPrefix;
                    formatted = displayName + " " + content;
                } else {
                    // Chat: [Prefix] Name
                    displayName = serverPrefix + " " + remainingName;
                    formatted = displayName + ": " + content;
                }
            } else {
                // No bracket prefix found - treat as cross-server player message
                displayName = "[Cross-Server] " + authorName;
                formatted = displayName + ": " + content;
            }
        } else {
            formatted = DiscordConfig.discordToMinecraftFormat
                    .replace("{username}", authorName)
                    .replace("{message}", content);
        }

        // For webhook messages, we need to parse and color the prefix
        Component finalComponent;
        if (isWebhook && formatted.startsWith("[")) {
            int endBracket = formatted.indexOf("]");
            if (endBracket > 0) {
                String prefixPart = formatted.substring(0, endBracket + 1);
                String rest = formatted.substring(endBracket + 1);
                
                // Build component with green prefix and white rest
                finalComponent = Component.text(prefixPart, NamedTextColor.GREEN)
                        .append(Component.text(rest, NamedTextColor.WHITE));
            } else {
                finalComponent = Component.text(formatted, NamedTextColor.WHITE);
            }
        } else {
            finalComponent = toAdventureComponent(formatted);
        }

        Bukkit.broadcast(finalComponent);
    }

    public void sendMinecraftMessage(String username, String message) {
        if (!running)
            return;

        String webhookUrl = DiscordConfig.webhookUrl;
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL"))
            return;

        String prefix = DiscordConfig.serverPrefix;
        String formattedUsername = DiscordConfig.webhookUsernameFormat
                .replace("{prefix}", prefix)
                .replace("{username}", username);

        String formattedMessage = DiscordConfig.minecraftToDiscordFormat
                .replace("{message}", message);

        String avatarUrl = DiscordConfig.avatarUrl;
        // Avatar logic simplified
        avatarUrl = avatarUrl.replace("{username}", username);

        WebhookMessage msg = new WebhookMessage(webhookUrl, formattedMessage, formattedUsername, avatarUrl);
        if (!messageQueue.offer(msg)) {
            plugin.getLogger().warning("[Discord] Queue full!");
        }
    }

    public void sendSystemMessage(String message) {
        if (!running)
            return;
        sendMinecraftMessage("Server", message);
    }

    public void sendJoinEmbed(String username) {
        if (!DiscordConfig.sendJoin)
            return;
        String serverName = DiscordConfig.serverName;
        sendEventEmbed(EmbedFactory.createPlayerEventEmbed(
                "Player Joined", "A player joined the server.", 0x5865F2, username, serverName, "VonixCore · Join",
                null));
    }

    public void sendLeaveEmbed(String username) {
        if (!DiscordConfig.sendLeave)
            return;
        String serverName = DiscordConfig.serverName;
        sendEventEmbed(EmbedFactory.createPlayerEventEmbed(
                "Player Left", "A player left the server.", 0x99AAB5, username, serverName, "VonixCore · Leave", null));
    }

    private void sendEventEmbed(java.util.function.Consumer<JsonObject> customize) {
        String url = DiscordConfig.eventWebhookUrl;
        if (url == null || url.isEmpty())
            url = DiscordConfig.webhookUrl;
        sendWebhookEmbedToUrl(url, customize);
    }

    private void sendStartupEmbed(String serverName) {
        sendEventEmbed(EmbedFactory.createServerStatusEmbed("Server Online", "The server is now online.", 0x43B581,
                serverName, "VonixCore · Startup"));
    }

    private void sendShutdownEmbed(String serverName) {
        sendEventEmbed(EmbedFactory.createServerStatusEmbed("Server Shutting Down", "The server is shutting down...",
                0xF04747, serverName, "VonixCore · Shutdown"));
    }

    private void sendWebhookEmbedToUrl(String webhookUrl, java.util.function.Consumer<JsonObject> customize) {
        if (webhookUrl == null || webhookUrl.isEmpty())
            return;

        JsonObject payload = new JsonObject();
        payload.addProperty("username", DiscordConfig.serverName);

        JsonObject embed = new JsonObject();
        customize.accept(embed);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(webhookUrl).post(body).build();

        try {
            httpClient.newCall(request).execute().close();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send webhook embed: " + e.getMessage());
        }
    }

    public void updateBotStatus() {
        if (discordApi == null || !DiscordConfig.setBotStatus)
            return;
        try {
            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            String status = DiscordConfig.botStatusFormat
                    .replace("{online}", String.valueOf(online))
                    .replace("{max}", String.valueOf(max));
            discordApi.updateActivity(ActivityType.PLAYING, status);
        } catch (Exception ignored) {
        }
    }

    private void startMessageQueueThread() {
        messageQueueThread = new Thread(() -> {
            while (running) {
                try {
                    WebhookMessage msg = messageQueue.poll(1, TimeUnit.SECONDS);
                    if (msg != null)
                        sendWebhookMessage(msg);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        messageQueueThread.setDaemon(true);
        messageQueueThread.start();
    }

    private void sendWebhookMessage(WebhookMessage msg) {
        JsonObject json = new JsonObject();
        json.addProperty("content", msg.content);
        json.addProperty("username", msg.username);
        if (msg.avatarUrl != null)
            json.addProperty("avatar_url", msg.avatarUrl);

        RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(msg.webhookUrl).post(body).build();
        try {
            httpClient.newCall(request).execute().close();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to send webhook: " + e.getMessage());
        }
    }

    private Component toAdventureComponent(String text) {
        // Simplified parser looking for Markdown links
        Matcher matcher = DISCORD_MARKDOWN_LINK.matcher(text);
        Component result = Component.empty();
        int lastEnd = 0;
        while (matcher.find()) {
            result = result.append(Component.text(text.substring(lastEnd, matcher.start())));
            String label = matcher.group(1);
            String url = matcher.group(2);
            result = result.append(Component.text(label)
                    .color(NamedTextColor.AQUA)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(url)));
            lastEnd = matcher.end();
        }
        result = result.append(Component.text(text.substring(lastEnd)));
        return result;
    }

    /**
     * Processes advancement embeds by extracting data and converting to vanilla-style components.
     */
    private void processAdvancementEmbed(org.javacord.api.entity.message.embed.Embed embed,
                                        org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            AdvancementData data = advancementExtractor.extractFromEmbed(embed);
            String serverPrefix = extractServerPrefixFromAuthor(event.getMessageAuthor().getDisplayName());
            Component advancementComponent = componentBuilder.buildAdvancementMessage(data, serverPrefix);
            
            Bukkit.broadcast(advancementComponent);
            return;
        } catch (ExtractionException e) {
            plugin.getLogger().log(Level.WARNING, "[Discord] Failed to extract advancement data: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Discord] Error processing advancement embed", e);
        }
        
        handleAdvancementFallback(embed, event);
    }
    
    private void handleAdvancementFallback(org.javacord.api.entity.message.embed.Embed embed,
                                          org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            String playerName = extractPlayerNameFallback(embed);
            String advancementTitle = extractAdvancementTitleFallback(embed);
            
            if (playerName != null && advancementTitle != null) {
                String serverPrefix = extractServerPrefixFromAuthor(event.getMessageAuthor().getDisplayName());
                Component fallbackComponent = componentBuilder.createFallbackComponent(
                        playerName, advancementTitle, serverPrefix);
                
                if (fallbackComponent != null) {
                    Bukkit.broadcast(fallbackComponent);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Discord] Fallback advancement processing failed", e);
        }
    }
    
    private String extractPlayerNameFallback(org.javacord.api.entity.message.embed.Embed embed) {
        if (embed == null) return null;
        
        for (org.javacord.api.entity.message.embed.EmbedField field : embed.getFields()) {
            String fieldName = field.getName();
            if (fieldName != null) {
                String lowerFieldName = fieldName.toLowerCase();
                if (lowerFieldName.contains("player") || lowerFieldName.contains("user") || lowerFieldName.contains("name")) {
                    String value = field.getValue();
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        }
        
        if (embed.getAuthor().isPresent()) {
            String authorName = embed.getAuthor().get().getName();
            if (authorName != null && !authorName.trim().isEmpty()) {
                return authorName.trim();
            }
        }
        
        return null;
    }
    
    private String extractAdvancementTitleFallback(org.javacord.api.entity.message.embed.Embed embed) {
        if (embed == null) return null;
        
        for (org.javacord.api.entity.message.embed.EmbedField field : embed.getFields()) {
            String fieldName = field.getName();
            if (fieldName != null) {
                String lowerFieldName = fieldName.toLowerCase();
                if (lowerFieldName.contains("advancement") || lowerFieldName.contains("achievement") || 
                    lowerFieldName.contains("title")) {
                    String value = field.getValue();
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
        }
        
        if (embed.getTitle().isPresent()) {
            String title = embed.getTitle().get().trim();
            if (!title.isEmpty()) {
                return title;
            }
        }
        
        return null;
    }
    
    private String extractServerPrefixFromAuthor(String authorName) {
        if (authorName != null && authorName.startsWith("[") && authorName.contains("]")) {
            int endBracket = authorName.indexOf("]");
            String prefix = authorName.substring(1, endBracket).trim();
            if (!prefix.isEmpty()) {
                return prefix;
            }
        }
        return stripBracketsFromPrefix(getFallbackServerPrefix());
    }
    
    private String extractServerPrefixFromAuthorForEvents(String authorName) {
        if (authorName != null && authorName.startsWith("[") && authorName.contains("]")) {
            int endBracket = authorName.indexOf("]");
            String prefix = authorName.substring(1, endBracket).trim();
            if (!prefix.isEmpty()) {
                return prefix;
            }
        }
        return "Cross-Server";
    }
    
    private String stripBracketsFromPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return prefix;
        }
        String stripped = prefix.trim();
        if (stripped.startsWith("[")) {
            stripped = stripped.substring(1);
        }
        if (stripped.endsWith("]")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped.trim();
    }
    
    public String getFallbackServerPrefix() {
        if (serverPrefixConfig == null) {
            return stripBracketsFromPrefix(DiscordConfig.serverPrefix);
        }
        return serverPrefixConfig.getFallbackPrefix();
    }

    /**
     * Processes event embeds (join/leave/death) by extracting data and converting to simplified format.
     */
    private void processEventEmbed(org.javacord.api.entity.message.embed.Embed embed,
                                  org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            EventData data = eventExtractor.extractFromEmbed(embed);
            String serverPrefix = extractServerPrefixFromAuthorForEvents(event.getMessageAuthor().getDisplayName());
            Component eventComponent = componentBuilder.buildEventMessage(data, serverPrefix);
            
            Bukkit.broadcast(eventComponent);
            return;
        } catch (ExtractionException e) {
            plugin.getLogger().log(Level.WARNING, "[Discord] Failed to extract event data: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Discord] Error processing event embed", e);
        }
        
        handleEventFallback(embed, event);
    }
    
    private void handleEventFallback(org.javacord.api.entity.message.embed.Embed embed,
                                    org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            String playerName = extractPlayerNameFallback(embed);
            String serverPrefix = extractServerPrefixFromAuthorForEvents(event.getMessageAuthor().getDisplayName());
            
            String action = "performed an action";
            EventEmbedDetector.EventType eventType = eventDetector.getEventType(embed);
            if (eventType != EventEmbedDetector.EventType.UNKNOWN) {
                action = eventType.getActionVerb();
            }
            
            if (playerName != null) {
                Component fallbackComponent = componentBuilder.createEventFallbackComponent(
                        playerName, action, serverPrefix);
                if (fallbackComponent != null) {
                    Bukkit.broadcast(fallbackComponent);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[Discord] Event fallback processing failed", e);
        }
    }

    private static class WebhookMessage {
        final String webhookUrl;
        final String content;
        final String username;
        final String avatarUrl;

        WebhookMessage(String webhookUrl, String content, String username, String avatarUrl) {
            this.webhookUrl = webhookUrl;
            this.content = content;
            this.username = username;
            this.avatarUrl = avatarUrl;
        }
    }
}
