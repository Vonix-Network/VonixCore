package network.vonix.vonixcore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Discord integration configuration for VonixCore.
 * Stored in config/vonixcore-discord.toml
 */
public class DiscordConfig {

        public static final ForgeConfigSpec SPEC;
        public static final DiscordConfig CONFIG;

        // Master toggle
        public final ForgeConfigSpec.BooleanValue enabled;

        // Connection settings
        public final ForgeConfigSpec.ConfigValue<String> botToken;
        public final ForgeConfigSpec.ConfigValue<String> channelId;
        public final ForgeConfigSpec.ConfigValue<String> webhookUrl;
        public final ForgeConfigSpec.ConfigValue<String> webhookId;
        public final ForgeConfigSpec.ConfigValue<String> inviteUrl;

        // Server identity
        public final ForgeConfigSpec.ConfigValue<String> serverPrefix;
        public final ForgeConfigSpec.ConfigValue<String> serverName;
        public final ForgeConfigSpec.ConfigValue<String> serverAvatarUrl;

        // Message formats
        public final ForgeConfigSpec.ConfigValue<String> discordToMinecraftFormat;
        public final ForgeConfigSpec.ConfigValue<String> minecraftToDiscordFormat;
        public final ForgeConfigSpec.ConfigValue<String> webhookUsernameFormat;
        public final ForgeConfigSpec.ConfigValue<String> avatarUrl;

        // Event settings
        public final ForgeConfigSpec.BooleanValue sendJoin;
        public final ForgeConfigSpec.BooleanValue sendLeave;
        public final ForgeConfigSpec.BooleanValue sendDeath;
        public final ForgeConfigSpec.BooleanValue sendAdvancement;
        public final ForgeConfigSpec.ConfigValue<String> eventChannelId;
        public final ForgeConfigSpec.ConfigValue<String> eventWebhookUrl;

        // Loop prevention
        public final ForgeConfigSpec.BooleanValue ignoreBots;
        public final ForgeConfigSpec.BooleanValue ignoreWebhooks;
        public final ForgeConfigSpec.BooleanValue filterByPrefix;
        public final ForgeConfigSpec.BooleanValue showOtherServerEvents;

        // Bot status
        public final ForgeConfigSpec.BooleanValue setBotStatus;
        public final ForgeConfigSpec.ConfigValue<String> botStatusFormat;

        // Account linking
        public final ForgeConfigSpec.BooleanValue enableAccountLinking;
        public final ForgeConfigSpec.IntValue linkCodeExpiry;

        // Advanced
        public final ForgeConfigSpec.BooleanValue debugLogging;
        public final ForgeConfigSpec.IntValue messageQueueSize;
        public final ForgeConfigSpec.IntValue rateLimitDelay;

        static {
                Pair<DiscordConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                                .configure(DiscordConfig::new);
                CONFIG = pair.getLeft();
                SPEC = pair.getRight();
        }

        private DiscordConfig(ForgeConfigSpec.Builder builder) {
                builder.comment(
                                "VonixCore Discord Integration",
                                "Bidirectional chat between Minecraft and Discord",
                                "",
                                "Setup Guide:",
                                "1. Create a Discord bot at https://discord.com/developers/applications",
                                "2. Enable MESSAGE CONTENT INTENT in Bot settings",
                                "3. Invite bot to your server with Message permissions",
                                "4. Copy bot token and paste below",
                                "5. Create a webhook in your Discord channel and paste URL below")
                                .push("discord");

                enabled = builder.comment("Enable Discord integration")
                                .define("enabled", false);

                builder.pop().comment(
                                "Connection Settings",
                                "Bot token and channel configuration")
                                .push("connection");

                botToken = builder.comment("Discord bot token")
                                .define("bot_token", "YOUR_BOT_TOKEN_HERE");

                channelId = builder.comment("Discord channel ID for chat")
                                .define("channel_id", "YOUR_CHANNEL_ID_HERE");

                webhookUrl = builder.comment("Discord webhook URL for sending messages")
                                .define("webhook_url", "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL");

                webhookId = builder.comment("Discord webhook ID (optional)")
                                .define("webhook_id", "");

                inviteUrl = builder.comment("Discord server invite URL")
                                .define("invite_url", "");

                builder.pop().comment(
                                "Server Identity",
                                "How the server appears in Discord")
                                .push("server_identity");

                serverPrefix = builder.comment("Prefix for server messages in Discord")
                                .define("prefix", "[MC]");

                serverName = builder.comment("Server name displayed in Discord")
                                .define("name", "Minecraft Server");

                serverAvatarUrl = builder.comment("Server avatar URL in Discord")
                                .define("avatar_url", "");

                builder.pop().comment(
                                "Message Formats",
                                "How messages are formatted between platforms")
                                .push("message_formats");

                discordToMinecraftFormat = builder.comment("Format for Discord messages in Minecraft")
                                .define("discord_to_minecraft", "§b[Discord] §f<{username}> {message}");

                minecraftToDiscordFormat = builder.comment("Format for Minecraft messages in Discord")
                                .define("minecraft_to_discord", "{message}");

                webhookUsernameFormat = builder.comment("Username format for webhook messages")
                                .define("webhook_username", "{prefix}{username}");

                avatarUrl = builder.comment("Avatar URL format for webhook messages")
                                .define("avatar_url", "https://minotar.net/armor/bust/{uuid}/100.png");

                builder.pop().comment(
                                "Event Notifications",
                                "Which events to send to Discord")
                                .push("events");

                sendJoin = builder.comment("Send player join messages")
                                .define("send_join", true);

                sendLeave = builder.comment("Send player leave messages")
                                .define("send_leave", true);

                sendDeath = builder.comment("Send player death messages")
                                .define("send_death", true);

                sendAdvancement = builder.comment("Send advancement messages")
                                .define("send_advancement", true);

                eventChannelId = builder.comment("Channel ID for event notifications (optional)")
                                .define("event_channel_id", "");

                eventWebhookUrl = builder.comment("Webhook URL for event notifications (optional)")
                                .define("event_webhook_url", "");

                builder.pop().comment(
                                "Loop Prevention",
                                "Prevent message loops between platforms")
                                .push("loop_prevention");

                ignoreBots = builder.comment("Ignore messages from Discord bots")
                                .define("ignore_bots", false);

                ignoreWebhooks = builder.comment("Ignore messages from Discord webhooks")
                                .define("ignore_webhooks", false);

                filterByPrefix = builder.comment("Filter messages by server prefix")
                                .define("filter_by_prefix", true);

                showOtherServerEvents = builder.comment("Show events from other servers")
                                .define("show_other_server_events", true);

                builder.pop().comment(
                                "Bot Status",
                                "Bot status display")
                                .push("bot_status");

                setBotStatus = builder.comment("Update bot status")
                                .define("update_status", true);

                botStatusFormat = builder.comment("Status format")
                                .define("format", "{online}/{max} players online");

                builder.pop().comment(
                                "Account Linking",
                                "Link Minecraft accounts to Discord")
                                .push("account_linking");

                enableAccountLinking = builder.comment("Enable account linking")
                                .define("enable_linking", true);

                linkCodeExpiry = builder.comment("Link code expiry time in seconds")
                                .defineInRange("code_expiry_seconds", 300, 60, 3600);

                builder.pop().comment(
                                "Advanced Settings",
                                "Advanced configuration options")
                                .push("advanced");

                debugLogging = builder.comment("Enable debug logging")
                                .define("debug_logging", false);

                messageQueueSize = builder.comment("Message queue size")
                                .defineInRange("message_queue_size", 100, 10, 1000);

                rateLimitDelay = builder.comment("Rate limit delay in milliseconds")
                                .defineInRange("rate_limit_delay", 1000, 100, 10000);

                builder.pop();
        }

        // ============ Getters ============

        public boolean isEnabled() {
                return CONFIG.enabled.get();
        }

        // Connection
        public String getBotToken() {
                return CONFIG.botToken.get();
        }

        public String getChannelId() {
                return CONFIG.channelId.get();
        }

        public String getWebhookUrl() {
                return CONFIG.webhookUrl.get();
        }

        public String getWebhookId() {
                return CONFIG.webhookId.get();
        }

        public String getInviteUrl() {
                return CONFIG.inviteUrl.get();
        }

        // Server identity
        public String getServerPrefix() {
                return CONFIG.serverPrefix.get();
        }

        public String getServerName() {
                return CONFIG.serverName.get();
        }

        public String getServerAvatarUrl() {
                return CONFIG.serverAvatarUrl.get();
        }

        // Message formats
        public String getDiscordToMinecraftFormat() {
                return CONFIG.discordToMinecraftFormat.get();
        }

        public String getMinecraftToDiscordFormat() {
                return CONFIG.minecraftToDiscordFormat.get();
        }

        public String getWebhookUsernameFormat() {
                return CONFIG.webhookUsernameFormat.get();
        }

        public String getAvatarUrl() {
                return CONFIG.avatarUrl.get();
        }

        // Events
        public boolean isSendJoin() {
                return CONFIG.sendJoin.get();
        }

        public boolean isSendLeave() {
                return CONFIG.sendLeave.get();
        }

        public boolean isSendDeath() {
                return CONFIG.sendDeath.get();
        }

        public boolean isSendAdvancement() {
                return CONFIG.sendAdvancement.get();
        }

        public String getEventChannelId() {
                return CONFIG.eventChannelId.get();
        }

        public String getEventWebhookUrl() {
                return CONFIG.eventWebhookUrl.get();
        }

        // Loop prevention
        public boolean isIgnoreBots() {
                return CONFIG.ignoreBots.get();
        }

        public boolean isIgnoreWebhooks() {
                return CONFIG.ignoreWebhooks.get();
        }

        public boolean isFilterByPrefix() {
                return CONFIG.filterByPrefix.get();
        }

        public boolean isShowOtherServerEvents() {
                return CONFIG.showOtherServerEvents.get();
        }

        // Bot status
        public boolean isSetBotStatus() {
                return CONFIG.setBotStatus.get();
        }

        public String getBotStatusFormat() {
                return CONFIG.botStatusFormat.get();
        }

        // Account linking
        public boolean isEnableAccountLinking() {
                return CONFIG.enableAccountLinking.get();
        }

        public int getLinkCodeExpiry() {
                return CONFIG.linkCodeExpiry.get();
        }

        // Advanced
        public boolean isDebugLogging() {
                return CONFIG.debugLogging.get();
        }

        public int getMessageQueueSize() {
                return CONFIG.messageQueueSize.get();
        }

        public int getRateLimitDelay() {
                return CONFIG.rateLimitDelay.get();
        }
}
