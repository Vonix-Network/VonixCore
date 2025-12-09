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

import java.awt.Color;
import java.io.IOException;
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

                updateBotStatus();

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[Discord] Failed to initialize Javacord", e);
                discordApi = null;
            }
        }).start();
    }

    private void processJavacordMessage(org.javacord.api.event.message.MessageCreateEvent event) {
        if (!event.getMessageAuthor().isRegularUser())
            return; // Ignore bots/webhooks

        String content = event.getMessageContent();
        String authorName = event.getMessageAuthor().getDisplayName();

        if (content.equalsIgnoreCase("!list")) {
            // Handle list command (simplified)
            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            event.getChannel().sendMessage("Online: " + online + "/" + max);
            return;
        }

        String formatted = DiscordConfig.discordToMinecraftFormat
                .replace("{username}", authorName)
                .replace("{message}", content);

        Bukkit.broadcast(toAdventureComponent(formatted));
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
