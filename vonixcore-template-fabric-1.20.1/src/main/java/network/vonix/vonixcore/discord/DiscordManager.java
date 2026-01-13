package network.vonix.vonixcore.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.DiscordConfig;
import okhttp3.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Discord integration manager using Javacord + Webhooks.
 * - Minecraft → Discord: Webhooks for messages and embeds
 * - Discord → Minecraft: Javacord gateway for message reception
 * - Bot Status: Javacord API for real-time player count updates
 * - Slash Commands: Javacord API for /list command
 */
public class DiscordManager {

    private static DiscordManager instance;
    private MinecraftServer server;
    private final OkHttpClient httpClient;
    private final BlockingQueue<WebhookMessage> messageQueue;
    private Thread messageQueueThread;
    private boolean running = false;
    private String ourWebhookId = null;
    private DiscordApi discordApi = null;

    private DiscordManager() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.messageQueue = new LinkedBlockingQueue<>(
                DiscordConfig.getInstance().getMessageQueueSize());
    }

    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        if (!DiscordConfig.getInstance().isEnabled()) {
            VonixCore.LOGGER.info("[Discord] Discord integration is disabled in config");
            return;
        }

        this.server = server;
        String token = DiscordConfig.getInstance().getBotToken();

        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            VonixCore.LOGGER.warn("[Discord] Bot token not configured, Discord integration disabled");
            return;
        }

        String webhookUrl = DiscordConfig.getInstance().getWebhookUrl();
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
                VonixCore.LOGGER.error("[Discord] Invalid webhook URL format: '{}'", webhookUrl);
                return;
            }
        }

        VonixCore.LOGGER.info("[Discord] Starting Discord integration...");

        extractWebhookId();
        running = true;
        startMessageQueueThread();

        try {
            initializeJavacord(token);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Failed to initialize Discord", e);
            running = false;
            return;
        }

        String serverName = DiscordConfig.getInstance().getServerName();
        sendStartupEmbed(serverName);

        VonixCore.LOGGER.info("[Discord] Discord integration initialized successfully!");
    }

    public void shutdown() {
        if (!running) {
            return;
        }

        VonixCore.LOGGER.info("[Discord] Shutting down Discord integration...");
        running = false;

        if (messageQueueThread != null && messageQueueThread.isAlive()) {
            messageQueueThread.interrupt();
            try {
                messageQueueThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (discordApi != null) {
            try {
                discordApi.disconnect().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                VonixCore.LOGGER.warn("[Discord] Javacord disconnect timeout");
            } finally {
                discordApi = null;
            }
        }

        if (httpClient != null) {
            try {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            } catch (Exception e) {
                // Ignore
            }
        }

        VonixCore.LOGGER.info("[Discord] Discord integration shut down");
    }

    public boolean isRunning() {
        return running;
    }

    private void extractWebhookId() {
        String webhookUrl = DiscordConfig.getInstance().getWebhookUrl();
        ourWebhookId = DiscordConfig.getInstance().getWebhookId();
        
        if ((ourWebhookId == null || ourWebhookId.isEmpty()) && webhookUrl != null) {
            try {
                String[] parts = webhookUrl.split("/");
                for (int i = 0; i < parts.length - 1; i++) {
                    if ("webhooks".equals(parts[i]) && i + 1 < parts.length) {
                        ourWebhookId = parts[i + 1];
                        break;
                    }
                }
            } catch (Exception e) {
                VonixCore.LOGGER.error("[Discord] Error extracting webhook ID", e);
            }
        }
    }

    private void initializeJavacord(String botToken) {
        try {
            String channelId = DiscordConfig.getInstance().getChannelId();
            if (channelId == null || channelId.equals("YOUR_CHANNEL_ID_HERE")) {
                VonixCore.LOGGER.warn("[Discord] Channel ID not configured");
                return;
            }

            VonixCore.LOGGER.info("[Discord] Connecting to Discord (async)...");

            new DiscordApiBuilder()
                    .setToken(botToken)
                    .setIntents(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT)
                    .login()
                    .orTimeout(15, TimeUnit.SECONDS)
                    .whenComplete((api, error) -> {
                        if (error != null) {
                            VonixCore.LOGGER.error("[Discord] Failed to connect: {}", error.getMessage());
                            return;
                        }
                        discordApi = api;
                        onJavacordConnected(channelId);
                    });
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Failed to initialize Javacord", e);
            discordApi = null;
        }
    }

    private void onJavacordConnected(String channelId) {
        try {
            VonixCore.LOGGER.info("[Discord] Connected as: {}", discordApi.getYourself().getName());

            long channelIdLong = Long.parseLong(channelId);
            discordApi.addMessageCreateListener(event -> {
                if (event.getChannel().getId() == channelIdLong) {
                    processJavacordMessage(event);
                }
            });

            registerListCommandAsync();
            updateBotStatus();

        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error during post-connection setup", e);
        }
    }

    private void processJavacordMessage(org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            boolean isBot = event.getMessageAuthor().asUser().map(user -> user.isBot()).orElse(false);
            boolean isWebhook = !event.getMessageAuthor().asUser().isPresent();
            String content = event.getMessageContent();
            String authorName = event.getMessageAuthor().getDisplayName();

            if (content.trim().equalsIgnoreCase("!list")) {
                handleTextListCommand(event);
                return;
            }

            if (isWebhook) {
                String authorId = String.valueOf(event.getMessageAuthor().getId());
                if (ourWebhookId != null && ourWebhookId.equals(authorId)) {
                    return;
                }
            }

            if (DiscordConfig.getInstance().isIgnoreWebhooks() && isWebhook) {
                return;
            }

            if (DiscordConfig.getInstance().isIgnoreBots() && isBot && !isWebhook) {
                return;
            }

            if (content.isEmpty()) {
                return;
            }

            String formattedMessage = DiscordConfig.getInstance().getDiscordToMinecraftFormat()
                    .replace("{username}", authorName)
                    .replace("{message}", content);

            if (server != null) {
                net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component.literal(formattedMessage);
                server.getPlayerList().getPlayers().forEach(player -> {
                    player.sendSystemMessage(component);
                });
            }
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error processing message", e);
        }
    }

    // ========= Public API for sending messages =========

    public void sendChatMessage(ServerPlayer player, String message) {
        if (!running) return;
        sendMinecraftMessage(player.getName().getString(), message);
    }

    public void sendPlayerJoin(ServerPlayer player) {
        if (!running || !DiscordConfig.getInstance().isSendJoin()) return;
        sendJoinEmbed(player.getName().getString());
    }

    public void sendPlayerLeave(ServerPlayer player) {
        if (!running || !DiscordConfig.getInstance().isSendLeave()) return;
        sendLeaveEmbed(player.getName().getString());
    }

    public void sendMinecraftMessage(String username, String message) {
        if (!running) return;

        String webhookUrl = DiscordConfig.getInstance().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        String prefix = DiscordConfig.getInstance().getServerPrefix();
        String formattedUsername = DiscordConfig.getInstance().getWebhookUsernameFormat()
                .replace("{prefix}", prefix)
                .replace("{username}", username);

        String formattedMessage = DiscordConfig.getInstance().getMinecraftToDiscordFormat()
                .replace("{message}", message);

        String avatarUrl = DiscordConfig.getInstance().getAvatarUrl();
        if (!avatarUrl.isEmpty() && server != null) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(username);
            if (player != null) {
                String uuid = player.getUUID().toString().replace("-", "");
                avatarUrl = avatarUrl
                        .replace("{uuid}", uuid)
                        .replace("{username}", username);
            }
        }

        WebhookMessage webhookMessage = new WebhookMessage(
                webhookUrl,
                formattedMessage,
                formattedUsername,
                avatarUrl.isEmpty() ? null : avatarUrl);

        if (!messageQueue.offer(webhookMessage)) {
            VonixCore.LOGGER.warn("[Discord] Message queue full, dropping message");
        }
    }

    public void sendStartupEmbed(String serverName) {
        sendEventEmbed("Server Online", "The server is now online.", 0x43B581, serverName);
    }

    public void sendShutdownEmbed(String serverName) {
        sendEventEmbed("Server Shutting Down", "The server is shutting down...", 0xF04747, serverName);
    }

    private void sendJoinEmbed(String username) {
        String serverName = DiscordConfig.getInstance().getServerName();
        sendPlayerEventEmbed("Player Joined", username, 0x5865F2, serverName);
    }

    private void sendLeaveEmbed(String username) {
        String serverName = DiscordConfig.getInstance().getServerName();
        sendPlayerEventEmbed("Player Left", username, 0x99AAB5, serverName);
    }

    private void sendEventEmbed(String title, String description, int color, String serverName) {
        String webhookUrl = DiscordConfig.getInstance().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        JsonObject payload = new JsonObject();
        String prefix = DiscordConfig.getInstance().getServerPrefix();
        String formattedUsername = DiscordConfig.getInstance().getWebhookUsernameFormat()
                .replace("{prefix}", prefix)
                .replace("{username}", serverName);
        payload.addProperty("username", formattedUsername);

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", color);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhookPayload(webhookUrl, payload);
    }

    private void sendPlayerEventEmbed(String title, String username, int color, String serverName) {
        String webhookUrl = DiscordConfig.getInstance().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        JsonObject payload = new JsonObject();
        String prefix = DiscordConfig.getInstance().getServerPrefix();
        String formattedServerName = DiscordConfig.getInstance().getWebhookUsernameFormat()
                .replace("{prefix}", prefix)
                .replace("{username}", serverName);
        payload.addProperty("username", formattedServerName);

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", "**" + username + "**");
        embed.addProperty("color", color);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        sendWebhookPayload(webhookUrl, payload);
    }

    private void sendWebhookPayload(String webhookUrl, JsonObject payload) {
        if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
            return;
        }

        RequestBody body = RequestBody.create(
                payload.toString(),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() && DiscordConfig.getInstance().isDebugLogging()) {
                VonixCore.LOGGER.error("[Discord] Failed to send webhook: {}", response.code());
            }
        } catch (IOException e) {
            VonixCore.LOGGER.error("[Discord] Error sending webhook", e);
        }
    }

    public void updateBotStatus() {
        if (discordApi == null || !DiscordConfig.getInstance().isSetBotStatus()) {
            return;
        }

        try {
            if (server == null) return;

            int onlinePlayers = server.getPlayerList().getPlayerCount();
            int maxPlayers = server.getPlayerList().getMaxPlayers();

            String statusText = DiscordConfig.getInstance().getBotStatusFormat()
                    .replace("{online}", String.valueOf(onlinePlayers))
                    .replace("{max}", String.valueOf(maxPlayers));

            discordApi.updateActivity(ActivityType.PLAYING, statusText);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error updating bot status", e);
        }
    }

    private void registerListCommandAsync() {
        if (discordApi == null) return;

        discordApi.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            if (interaction.getCommandName().equals("list")) {
                handleListCommand(interaction);
            }
        });

        SlashCommand.with("list", "Show online players")
                .createGlobal(discordApi)
                .whenComplete((cmd, error) -> {
                    if (error != null) {
                        VonixCore.LOGGER.error("[Discord] Failed to register /list command: {}", error.getMessage());
                    }
                });
    }

    private void handleListCommand(SlashCommandInteraction interaction) {
        try {
            if (server == null) {
                interaction.createImmediateResponder()
                        .setContent("❌ Server is not available")
                        .respond();
                return;
            }

            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            int onlinePlayers = players.size();
            int maxPlayers = server.getPlayerList().getMaxPlayers();

            String serverName = DiscordConfig.getInstance().getServerName();

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📋 " + serverName)
                    .setColor(Color.GREEN)
                    .setFooter("VonixCore · Player List");

            if (onlinePlayers == 0) {
                embed.setDescription("No players are currently online.");
            } else {
                StringBuilder playerList = new StringBuilder();
                for (int i = 0; i < players.size(); i++) {
                    if (i > 0) playerList.append("\n");
                    playerList.append("• ").append(players.get(i).getName().getString());
                }
                embed.addField("Players " + onlinePlayers + "/" + maxPlayers, playerList.toString(), false);
            }

            interaction.createImmediateResponder()
                    .addEmbed(embed)
                    .respond();
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error handling /list command", e);
            interaction.createImmediateResponder()
                    .setContent("❌ An error occurred")
                    .respond();
        }
    }

    private void handleTextListCommand(org.javacord.api.event.message.MessageCreateEvent event) {
        try {
            if (server == null) return;

            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            int onlinePlayers = players.size();
            int maxPlayers = server.getPlayerList().getMaxPlayers();

            String serverName = DiscordConfig.getInstance().getServerName();

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📋 " + serverName)
                    .setColor(Color.GREEN)
                    .setFooter("VonixCore · Player List");

            if (onlinePlayers == 0) {
                embed.setDescription("No players are currently online.");
            } else {
                StringBuilder playerList = new StringBuilder();
                for (int i = 0; i < players.size(); i++) {
                    if (i > 0) playerList.append("\n");
                    playerList.append("• ").append(players.get(i).getName().getString());
                }
                embed.addField("Players " + onlinePlayers + "/" + maxPlayers, playerList.toString(), false);
            }

            event.getChannel().sendMessage(embed);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error handling !list command", e);
        }
    }

    private void startMessageQueueThread() {
        messageQueueThread = new Thread(() -> {
            while (running) {
                try {
                    WebhookMessage message = messageQueue.poll(1, TimeUnit.SECONDS);
                    if (message != null) {
                        sendWebhookMessage(message);
                        Thread.sleep(DiscordConfig.getInstance().getRateLimitDelay());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    VonixCore.LOGGER.error("[Discord] Error in message queue thread", e);
                }
            }
        }, "VonixCore-Discord-Queue");
        messageQueueThread.setDaemon(true);
        messageQueueThread.start();
    }

    private void sendWebhookMessage(WebhookMessage message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("content", message.content);
        payload.addProperty("username", message.username);
        if (message.avatarUrl != null) {
            payload.addProperty("avatar_url", message.avatarUrl);
        }

        sendWebhookPayload(message.webhookUrl, payload);
    }

    private record WebhookMessage(String webhookUrl, String content, String username, String avatarUrl) {}
}
