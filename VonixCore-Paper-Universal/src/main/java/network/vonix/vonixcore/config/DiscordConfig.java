package network.vonix.vonixcore.config;

import org.bukkit.configuration.file.YamlConfiguration;

public class DiscordConfig {

    public static boolean enabled;

    public static String botToken;
    public static String channelId;
    public static String webhookUrl;
    public static String webhookId;
    public static String inviteUrl;

    public static String serverPrefix;
    public static String serverName;
    public static String serverAvatarUrl;

    public static String discordToMinecraftFormat;
    public static String minecraftToDiscordFormat;
    public static String webhookUsernameFormat;
    public static String avatarUrl;

    public static boolean sendJoin;
    public static boolean sendLeave;
    public static boolean sendDeath;
    public static boolean sendAdvancement;
    public static String eventChannelId;
    public static String eventWebhookUrl;

    public static boolean ignoreBots;
    public static boolean ignoreWebhooks;
    public static boolean filterByPrefix;
    public static boolean showOtherServerEvents;

    public static boolean setBotStatus;
    public static String botStatusFormat;

    public static boolean enableAccountLinking;
    public static int linkCodeExpiry;

    public static boolean debugLogging;
    public static int messageQueueSize;
    public static int rateLimitDelay;

    public static void load(YamlConfiguration config) {
        enabled = config.getBoolean("discord.enabled", false);

        botToken = config.getString("discord.connection.bot_token", "YOUR_BOT_TOKEN_HERE");
        channelId = config.getString("discord.connection.channel_id", "YOUR_CHANNEL_ID_HERE");
        webhookUrl = config.getString("discord.connection.webhook_url", "YOUR_WEBHOOK_URL_HERE");
        webhookId = config.getString("discord.connection.webhook_id", "");
        inviteUrl = config.getString("discord.connection.invite_url", "");

        serverPrefix = config.getString("discord.server_identity.prefix", "[MC]");
        serverName = config.getString("discord.server_identity.name", "Minecraft Server");
        serverAvatarUrl = config.getString("discord.server_identity.avatar_url", "");

        discordToMinecraftFormat = config.getString("discord.message_formats.discord_to_minecraft",
                "§b[Discord] §f<{username}> {message}");
        minecraftToDiscordFormat = config.getString("discord.message_formats.minecraft_to_discord", "{message}");
        webhookUsernameFormat = config.getString("discord.message_formats.webhook_username", "{prefix}{username}");
        avatarUrl = config.getString("discord.message_formats.avatar_url",
                "https://minotar.net/armor/bust/{uuid}/100.png");

        sendJoin = config.getBoolean("discord.events.send_join", true);
        sendLeave = config.getBoolean("discord.events.send_leave", true);
        sendDeath = config.getBoolean("discord.events.send_death", true);
        sendAdvancement = config.getBoolean("discord.events.send_advancement", true);
        eventChannelId = config.getString("discord.events.event_channel_id", "");
        eventWebhookUrl = config.getString("discord.events.event_webhook_url", "");

        ignoreBots = config.getBoolean("discord.loop_prevention.ignore_bots", false);
        ignoreWebhooks = config.getBoolean("discord.loop_prevention.ignore_webhooks", false);
        filterByPrefix = config.getBoolean("discord.loop_prevention.filter_by_prefix", true);
        showOtherServerEvents = config.getBoolean("discord.loop_prevention.show_other_server_events", true);

        setBotStatus = config.getBoolean("discord.bot_status.enabled", true);
        botStatusFormat = config.getString("discord.bot_status.format", "{online}/{max} players online");

        enableAccountLinking = config.getBoolean("discord.account_linking.enabled", true);
        linkCodeExpiry = config.getInt("discord.account_linking.code_expiry_seconds", 300);

        debugLogging = config.getBoolean("discord.advanced.debug_logging", false);
        messageQueueSize = config.getInt("discord.advanced.message_queue_size", 100);
        rateLimitDelay = config.getInt("discord.advanced.rate_limit_delay", 1000);
    }
}
