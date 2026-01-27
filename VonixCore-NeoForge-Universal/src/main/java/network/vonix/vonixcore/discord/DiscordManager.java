package network.vonix.vonixcore.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
import java.util.regex.Pattern;

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
    private String eventWebhookId = null;
    private DiscordApi discordApi = null;
    private LinkedAccountsManager linkedAccountsManager = null;
    private PlayerPreferences playerPreferences = null;

    private static final Pattern DISCORD_MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\((https?://[^)]+)\\)");

    private DiscordManager() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.messageQueue = new LinkedBlockingQueue<>(
                DiscordConfig.CONFIG.messageQueueSize.get());
    }

    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }

    public void initialize(MinecraftServer server) {
        if (!DiscordConfig.CONFIG.enabled.get()) {
            VonixCore.LOGGER.info("[Discord] Discord integration is disabled in config");
            return;
        }

        this.server = server;
        String token = DiscordConfig.CONFIG.botToken.get();

        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            VonixCore.LOGGER.warn("[Discord] Bot token not configured, Discord integration disabled");
            return;
        }

        // Validate webhook URL format
        String webhookUrl = DiscordConfig.CONFIG.webhookUrl.get();
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
                VonixCore.LOGGER.error(
                        "[Discord] Invalid webhook URL format: '{}'. Must start with http:// or https://", webhookUrl);
                VonixCore.LOGGER.error(
                        "[Discord] Example: https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN");
                return;
            }
        }

        VonixCore.LOGGER.info("[Discord] Starting Discord integration (Javacord + Webhooks)...");

        extractWebhookId();

        // Initialize player preferences
        try {
            playerPreferences = new PlayerPreferences(server.getServerDirectory().resolve("config"));
            VonixCore.LOGGER.info("[Discord] Player preferences system initialized");
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Failed to initialize player preferences", e);
        }

        running = true;
        startMessageQueueThread();

        // Initialize Javacord
        try {
            initializeJavacord(token);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Failed to initialize Discord", e);
            running = false;
            return;
        }

        // Send startup embed
        String serverName = DiscordConfig.CONFIG.serverName.get();
        sendStartupEmbed(serverName);

        VonixCore.LOGGER.info("[Discord] Discord integration initialized successfully!");
        if (ourWebhookId != null) {
            VonixCore.LOGGER.info("[Discord] Chat Webhook ID: {}", ourWebhookId);
        }
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
                VonixCore.LOGGER.info("[Discord] Javacord disconnected");
            } catch (Exception e) {
                VonixCore.LOGGER.warn("[Discord] Javacord disconnect timeout: {}", e.getMessage());
            } finally {
                discordApi = null;
            }
        }

        if (httpClient != null) {
            try {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
                httpClient.dispatcher().executorService().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                httpClient.dispatcher().executorService().shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        VonixCore.LOGGER.info("[Discord] Discord integration shut down");
    }

    public boolean isRunning() {
        return running;
    }

    // ========= Account Linking =========

    public String generateLinkCode(ServerPlayer player) {
        if (linkedAccountsManager == null || !DiscordConfig.CONFIG.enableAccountLinking.get()) {
            return null;
        }
        return linkedAccountsManager.generateLinkCode(player.getUUID(), player.getName().getString());
    }

    public boolean unlinkAccount(UUID uuid) {
        if (linkedAccountsManager == null || !DiscordConfig.CONFIG.enableAccountLinking.get()) {
            return false;
        }
        return linkedAccountsManager.unlinkMinecraft(uuid);
    }

    // ========= Player Preferences =========

    public boolean hasServerMessagesFiltered(UUID playerUuid) {
        if (playerPreferences == null) {
            return false;
        }
        return playerPreferences.hasServerMessagesFiltered(playerUuid);
    }

    public void setServerMessagesFiltered(UUID playerUuid, boolean filtered) {
        if (playerPreferences != null) {
            playerPreferences.setServerMessagesFiltered(playerUuid, filtered);
        }
    }

    public boolean hasEventsFiltered(UUID playerUuid) {
        if (playerPreferences == null) {
            return false;
        }
        return playerPreferences.hasEventsFiltered(playerUuid);
    }

    public void setEventsFiltered(UUID playerUuid, boolean filtered) {
        if (playerPreferences != null) {
            playerPreferences.setEventsFiltered(playerUuid, filtered);
        }
    }

    // ========= Webhook ID Extraction =========

    private void extractWebhookId() {
        ourWebhookId = extractWebhookIdFromConfig(
                DiscordConfig.CONFIG.webhookId.get(),
                DiscordConfig.CONFIG.webhookUrl.get(),
                "chat");
        eventWebhookId = extractWebhookIdFromConfig(
                "",
                DiscordConfig.CONFIG.eventWebhookUrl.get(),
                "event");
    }

    private String extractWebhookIdFromConfig(String manualId, String webhookUrl, String type) {
        if (manualId != null && !manualId.isEmpty()) {
            return manualId;
        }

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
            VonixCore.LOGGER.error("[Discord] Error extracting webhook ID", e);
        }
        return null;
    }

    // ========= Javacord Initialization =========

    private void initializeJavacord(String botToken) {
        try {
            String channelId = DiscordConfig.CONFIG.channelId.get();
            if (channelId == null || channelId.equals("YOUR_CHANNEL_ID_HERE")) {
                VonixCore.LOGGER.warn("[Discord] Channel ID not configured");
                return;
            }

            VonixCore.LOGGER.info("[Discord] Connecting to Discord (async)...");

            // Connect asynchronously to avoid blocking server startup
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

    /**
     * Called after Javacord successfully connects to Discord.
     * Runs asynchronously to avoid blocking server startup.
     */
    private void onJavacordConnected(String channelId) {
        try {
            VonixCore.LOGGER.info("[Discord] Connected as: {}", discordApi.getYourself().getName());

            // Register message listener
            long channelIdLong = Long.parseLong(channelId);
            String eventChannelIdStr = DiscordConfig.CONFIG.eventChannelId.get();
            Long eventChannelIdLong = null;

            if (eventChannelIdStr != null && !eventChannelIdStr.isEmpty()) {
                try {
                    eventChannelIdLong = Long.parseLong(eventChannelIdStr);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            final Long finalEventChannelId = eventChannelIdLong;
            discordApi.addMessageCreateListener(event -> {
                long msgChannelId = event.getChannel().getId();
                if (msgChannelId == channelIdLong ||
                        (finalEventChannelId != null && msgChannelId == finalEventChannelId)) {
                    processJavacordMessage(event);
                }
            });

            // Initialize account linking
            if (DiscordConfig.CONFIG.enableAccountLinking.get()) {
                try {
                    linkedAccountsManager = new LinkedAccountsManager(server.getServerDirectory().resolve("config"));
                    VonixCore.LOGGER.info("[Discord] Account linking initialized ({} accounts)",
                            linkedAccountsManager.getLinkedCount());
                } catch (Exception e) {
                    VonixCore.LOGGER.error("[Discord] Failed to initialize account linking", e);
                }
            }

            // Register slash commands ASYNCHRONOUSLY to avoid blocking
            registerListCommandAsync();
            if (DiscordConfig.CONFIG.enableAccountLinking.get() && linkedAccountsManager != null) {
                registerLinkCommandsAsync();
            }

            // Set bot status
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

            // Handle !list command
            if (content.trim().equalsIgnoreCase("!list")) {
                handleTextListCommand(event);
                return;
            }

            // Filter our own webhooks based on username prefix
            if (isWebhook) {
                String ourPrefix = DiscordConfig.CONFIG.serverPrefix.get();
                if (authorName != null && authorName.startsWith(ourPrefix)) {
                    return;
                }
            }

            // Filter other webhooks if configured
            if (DiscordConfig.CONFIG.ignoreWebhooks.get() && isWebhook) {
                if (DiscordConfig.CONFIG.filterByPrefix.get()) {
                    String ourPrefix = DiscordConfig.CONFIG.serverPrefix.get();
                    if (authorName != null && authorName.startsWith(ourPrefix)) {
                        return;
                    }
                } else {
                    return;
                }
            }

            // Filter bots
            if (DiscordConfig.CONFIG.ignoreBots.get() && isBot && !isWebhook) {
                return;
            }

            if (content.isEmpty()) {
                return;
            }

            // Strip duplicate username prefix from message content
            // This handles webhooks that include "Username: " in the message
            String cleanedContent = content;
            if (content.startsWith(authorName + ": ")) {
                cleanedContent = content.substring(authorName.length() + 2);
            } else if (content.startsWith(authorName + " ")) {
                cleanedContent = content.substring(authorName.length() + 1);
            }

            String formattedMessage;
            if (isWebhook) {
                // Special formatting for cross-server messages (webhooks)
                String displayName = authorName;

                if (displayName.startsWith("[") && displayName.contains("]")) {
                    int endBracket = displayName.indexOf("]");
                    String serverPrefix = displayName.substring(0, endBracket + 1);
                    String remainingName = displayName.substring(endBracket + 1).trim();

                    if (remainingName.toLowerCase().contains("server")) {
                        // Event or generic server message: just prefix
                        displayName = "§a" + serverPrefix;
                        formattedMessage = displayName + " §f" + cleanedContent;
                    } else {
                        // Chat: [Prefix] Name
                        displayName = "§a" + serverPrefix + " §f" + remainingName;
                        formattedMessage = displayName + "§7: §f" + cleanedContent;
                    }
                } else {
                    // No prefix found, just use raw name
                    formattedMessage = "§7[Discord] §f" + authorName + "§7: §f" + cleanedContent;
                }
            } else {
                // Standard Discord user message
                formattedMessage = DiscordConfig.CONFIG.discordToMinecraftFormat.get()
                        .replace("{username}", authorName)
                        .replace("{message}", cleanedContent);
            }

            if (server != null) {
                MutableComponent finalComponent = Component.empty();

                if (isWebhook) {
                    // Webhook logic (already established) using the pre-calculated formattedMessage
                    finalComponent.append(toMinecraftComponentWithLinks(formattedMessage));
                } else {
                    // Standard Discord message: Make [Discord] clickable
                    String inviteUrl = DiscordConfig.CONFIG.inviteUrl.get();
                    String rawFormat = DiscordConfig.CONFIG.discordToMinecraftFormat.get()
                            .replace("{username}", authorName)
                            .replace("{message}", cleanedContent);

                    if (rawFormat.contains("[Discord]") && inviteUrl != null && !inviteUrl.isEmpty()) {
                        String[] parts = rawFormat.split("\\[Discord\\]", 2);

                        // Part before [Discord]
                        if (parts.length > 0 && !parts[0].isEmpty()) {
                            finalComponent.append(toMinecraftComponentWithLinks(parts[0]));
                        }

                        // Clickable [Discord]
                        finalComponent.append(Component.literal("[Discord]")
                                .setStyle(Style.EMPTY
                                        .withColor(ChatFormatting.AQUA)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, inviteUrl))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Click to join our Discord!")))));

                        // Part after [Discord]
                        if (parts.length > 1 && !parts[1].isEmpty()) {
                            finalComponent.append(toMinecraftComponentWithLinks(parts[1]));
                        }
                    } else {
                        // Fallback if no tag or no invite URL
                        finalComponent.append(toMinecraftComponentWithLinks(rawFormat));
                    }
                }

                server.getPlayerList().getPlayers().forEach(player -> {
                    boolean isFilterableMessage = isBot || isWebhook;
                    if (isFilterableMessage && hasServerMessagesFiltered(player.getUUID())) {
                        return;
                    }
                    player.sendSystemMessage(finalComponent);
                });
            }
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error processing message", e);
        }
    }

    // ========= Minecraft → Discord =========

    public void sendMinecraftMessage(String username, String message) {
        if (!running) {
            return;
        }

        String webhookUrl = DiscordConfig.CONFIG.webhookUrl.get();
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        String prefix = DiscordConfig.CONFIG.serverPrefix.get();
        String formattedUsername = DiscordConfig.CONFIG.webhookUsernameFormat.get()
                .replace("{prefix}", prefix)
                .replace("{username}", username);

        String formattedMessage = DiscordConfig.CONFIG.minecraftToDiscordFormat.get()
                .replace("{message}", message);

        String avatarUrl = DiscordConfig.CONFIG.avatarUrl.get();
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

    public void sendSystemMessage(String message) {
        if (!running || message == null || message.isEmpty()) {
            return;
        }

        if (message.startsWith("💀")) {
            sendEventEmbed(embed -> {
                embed.addProperty("title", "Player Died");
                embed.addProperty("description", message);
                embed.addProperty("color", 0xF04747);
                JsonObject footer = new JsonObject();
                footer.addProperty("text", "VonixCore · Death");
                embed.add("footer", footer);
            });
        } else {
            sendMinecraftMessage("Server", message);
        }
    }

    // ========= Event Embeds =========

    private String getEventWebhookUrl() {
        String eventWebhookUrl = DiscordConfig.CONFIG.eventWebhookUrl.get();
        if (eventWebhookUrl != null && !eventWebhookUrl.isEmpty()) {
            return eventWebhookUrl;
        }
        return DiscordConfig.CONFIG.webhookUrl.get();
    }

    public void sendStartupEmbed(String serverName) {
        sendEventEmbed(EmbedFactory.createServerStatusEmbed(
                "Server Online",
                "The server is now online.",
                0x43B581,
                serverName,
                "VonixCore · Startup"));
    }

    public void sendShutdownEmbed(String serverName) {
        sendEventEmbed(EmbedFactory.createServerStatusEmbed(
                "Server Shutting Down",
                "The server is shutting down...",
                0xF04747,
                serverName,
                "VonixCore · Shutdown"));
    }

    private String getPlayerAvatarUrl(String username) {
        String avatarUrl = DiscordConfig.CONFIG.avatarUrl.get();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return null;
        }

        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(username);
            if (player != null) {
                String uuid = player.getUUID().toString().replace("-", "");
                return avatarUrl
                        .replace("{uuid}", uuid)
                        .replace("{username}", username);
            }
        }

        return avatarUrl.replace("{username}", username);
    }

    public void sendJoinEmbed(String username) {
        if (!DiscordConfig.CONFIG.sendJoin.get()) {
            return;
        }
        String serverName = DiscordConfig.CONFIG.serverName.get();
        String thumbnailUrl = getPlayerAvatarUrl(username);
        sendEventEmbed(EmbedFactory.createPlayerEventEmbed(
                "Player Joined",
                "A player joined the server.",
                0x5865F2,
                username,
                serverName,
                "VonixCore · Join",
                thumbnailUrl));
    }

    public void sendLeaveEmbed(String username) {
        if (!DiscordConfig.CONFIG.sendLeave.get()) {
            return;
        }
        String serverName = DiscordConfig.CONFIG.serverName.get();
        String thumbnailUrl = getPlayerAvatarUrl(username);
        sendEventEmbed(EmbedFactory.createPlayerEventEmbed(
                "Player Left",
                "A player left the server.",
                0x99AAB5,
                username,
                serverName,
                "VonixCore · Leave",
                thumbnailUrl));
    }

    public void sendAdvancementEmbed(String username, String advancementTitle, String advancementDescription,
            String type) {
        if (!DiscordConfig.CONFIG.sendAdvancement.get()) {
            return;
        }
        sendEventEmbed(EmbedFactory.createAdvancementEmbed(
                "🏆",
                0xFAA61A,
                username,
                advancementTitle,
                advancementDescription));
    }

    public void updateBotStatus() {
        if (discordApi == null || !DiscordConfig.CONFIG.setBotStatus.get()) {
            return;
        }

        try {
            if (server == null) {
                return;
            }

            int onlinePlayers = server.getPlayerList().getPlayerCount();
            int maxPlayers = server.getPlayerList().getMaxPlayers();

            String statusText = DiscordConfig.CONFIG.botStatusFormat.get()
                    .replace("{online}", String.valueOf(onlinePlayers))
                    .replace("{max}", String.valueOf(maxPlayers));

            discordApi.updateActivity(ActivityType.PLAYING, statusText);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error updating bot status", e);
        }
    }

    // ========= Slash Commands =========

    private void registerListCommandAsync() {
        if (discordApi == null) {
            return;
        }

        // Add listener first (doesn't require command to exist)
        discordApi.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            if (interaction.getCommandName().equals("list")) {
                handleListCommand(interaction);
            }
        });

        // Register command asynchronously - don't block with .join()
        SlashCommand.with("list", "Show online players")
                .createGlobal(discordApi)
                .whenComplete((cmd, error) -> {
                    if (error != null) {
                        VonixCore.LOGGER.error("[Discord] Failed to register /list command: {}", error.getMessage());
                    } else {
                        VonixCore.LOGGER.debug("[Discord] /list command registered");
                    }
                });
    }

    private EmbedBuilder buildPlayerListEmbed() {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int onlinePlayers = players.size();
        int maxPlayers = server.getPlayerList().getMaxPlayers();

        String serverName = DiscordConfig.CONFIG.serverName.get();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 " + serverName)
                .setColor(Color.GREEN)
                .setFooter("VonixCore · Player List");

        if (onlinePlayers == 0) {
            embed.setDescription("No players are currently online.");
        } else {
            StringBuilder playerListBuilder = new StringBuilder();
            for (int i = 0; i < players.size(); i++) {
                if (i > 0)
                    playerListBuilder.append("\n");
                playerListBuilder.append("• ").append(players.get(i).getName().getString());
            }
            embed.addField("Players " + onlinePlayers + "/" + maxPlayers, playerListBuilder.toString(), false);
        }

        return embed;
    }

    private void handleListCommand(SlashCommandInteraction interaction) {
        try {
            if (server == null) {
                interaction.createImmediateResponder()
                        .setContent("❌ Server is not available")
                        .respond();
                return;
            }

            EmbedBuilder embed = buildPlayerListEmbed();
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
            if (server == null) {
                return;
            }
            EmbedBuilder embed = buildPlayerListEmbed();
            event.getChannel().sendMessage(embed);
        } catch (Exception e) {
            VonixCore.LOGGER.error("[Discord] Error handling !list command", e);
        }
    }

    private void registerLinkCommandsAsync() {
        if (discordApi == null || linkedAccountsManager == null) {
            return;
        }

        // Add listener first (doesn't require commands to exist)
        discordApi.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();

            if (interaction.getCommandName().equals("link")) {
                String code = interaction.getArgumentStringValueByName("code").orElse("");
                String discordId = String.valueOf(interaction.getUser().getId());
                String discordUsername = interaction.getUser().getName();

                LinkedAccountsManager.LinkResult result = linkedAccountsManager.verifyAndLink(code, discordId,
                        discordUsername);

                interaction.createImmediateResponder()
                        .setContent((result.success ? "✅ " : "❌ ") + result.message)
                        .respond();
            } else if (interaction.getCommandName().equals("unlink")) {
                String discordId = String.valueOf(interaction.getUser().getId());
                boolean success = linkedAccountsManager.unlinkDiscord(discordId);

                interaction.createImmediateResponder()
                        .setContent(success ? "✅ Your Minecraft account has been unlinked."
                                : "❌ You don't have a linked Minecraft account.")
                        .respond();
            }
        });

        // Register commands asynchronously - don't block with .join()
        SlashCommand.with("link", "Link your Minecraft account to Discord",
                java.util.Arrays.asList(
                        org.javacord.api.interaction.SlashCommandOption.create(
                                org.javacord.api.interaction.SlashCommandOptionType.STRING,
                                "code",
                                "The 6-digit code from /vonix discord link in-game",
                                true)))
                .createGlobal(discordApi)
                .whenComplete((cmd, error) -> {
                    if (error != null) {
                        VonixCore.LOGGER.error("[Discord] Failed to register /link command: {}", error.getMessage());
                    }
                });

        SlashCommand.with("unlink", "Unlink your Discord account from Minecraft")
                .createGlobal(discordApi)
                .whenComplete((cmd, error) -> {
                    if (error != null) {
                        VonixCore.LOGGER.error("[Discord] Failed to register /unlink command: {}", error.getMessage());
                    }
                });
    }

    // ========= Webhook Sending =========

    private void sendEventEmbed(java.util.function.Consumer<JsonObject> customize) {
        String webhookUrl = getEventWebhookUrl();
        sendWebhookEmbedToUrl(webhookUrl, customize);
    }

    private void sendWebhookEmbedToUrl(String webhookUrl, java.util.function.Consumer<JsonObject> customize) {
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("YOUR_WEBHOOK_URL")) {
            return;
        }

        // Validate webhook URL format
        if (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
            VonixCore.LOGGER.error("[Discord] Invalid webhook URL format: '{}'. Skipping webhook send.", webhookUrl);
            VonixCore.LOGGER.error("[Discord] Webhook URLs must start with http:// or https://");
            VonixCore.LOGGER
                    .error("[Discord] Example: https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN");
            VonixCore.LOGGER
                    .error("[Discord] If you see a number like '144202...', that's a channel ID, not a webhook URL!");
            return;
        }

        // Execute entire embed send asynchronously to avoid blocking main thread
        VonixCore.executeAsync(() -> {
            JsonObject payload = new JsonObject();

            String prefix = DiscordConfig.CONFIG.serverPrefix.get();
            String serverName = DiscordConfig.CONFIG.serverName.get();
            String baseUsername = serverName == null ? "Server" : serverName;
            String formattedUsername = DiscordConfig.CONFIG.webhookUsernameFormat.get()
                    .replace("{prefix}", prefix)
                    .replace("{username}", baseUsername);

            payload.addProperty("username", formattedUsername);

            String avatarUrl = DiscordConfig.CONFIG.serverAvatarUrl.get();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                payload.addProperty("avatar_url", avatarUrl);
            }

            JsonObject embed = new JsonObject();
            customize.accept(embed);

            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            RequestBody body = RequestBody.create(
                    payload.toString(),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() && DiscordConfig.CONFIG.debugLogging.get()) {
                    VonixCore.LOGGER.error("[Discord] Failed to send embed: {}", response.code());
                }
            } catch (IOException e) {
                VonixCore.LOGGER.error("[Discord] Error sending embed", e);
            }
        });
    }

    private Component toMinecraftComponentWithLinks(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        var matcher = DISCORD_MARKDOWN_LINK.matcher(text);
        MutableComponent result = Component.empty();
        int lastEnd = 0;
        boolean hasLink = false;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start > lastEnd) {
                String before = text.substring(lastEnd, start);
                if (!before.isEmpty()) {
                    result.append(Component.literal(before));
                }
            }

            String label = matcher.group(1);
            String url = matcher.group(2);

            Component linkComponent = Component
                    .literal(label)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                            .withUnderlined(true)
                            .withColor(ChatFormatting.AQUA));

            result.append(linkComponent);
            lastEnd = end;
            hasLink = true;
        }

        if (lastEnd < text.length()) {
            String tail = text.substring(lastEnd);
            if (!tail.isEmpty()) {
                result.append(Component.literal(tail));
            }
        }

        if (!hasLink) {
            return Component.literal(text);
        }

        return result;
    }

    private void startMessageQueueThread() {
        messageQueueThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    WebhookMessage webhookMessage = messageQueue.poll(1, TimeUnit.SECONDS);
                    if (webhookMessage != null) {
                        sendWebhookMessage(webhookMessage);

                        int delay = DiscordConfig.CONFIG.rateLimitDelay.get();
                        if (delay > 0) {
                            Thread.sleep(delay);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    VonixCore.LOGGER.error("[Discord] Error processing message queue", e);
                }
            }
        }, "VonixCore-Discord-Queue");
        messageQueueThread.setDaemon(true);
        messageQueueThread.start();
    }

    private void sendWebhookMessage(WebhookMessage webhookMessage) {
        JsonObject json = new JsonObject();
        json.addProperty("content", webhookMessage.content);
        json.addProperty("username", webhookMessage.username);

        if (webhookMessage.avatarUrl != null) {
            json.addProperty("avatar_url", webhookMessage.avatarUrl);
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(webhookMessage.webhookUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() && DiscordConfig.CONFIG.debugLogging.get()) {
                VonixCore.LOGGER.error("[Discord] Failed to send message: {}", response.code());
            }
        } catch (IOException e) {
            VonixCore.LOGGER.error("[Discord] Error sending message", e);
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
