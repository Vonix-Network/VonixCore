package network.vonix.vonixcore.listener;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import network.vonix.vonixcore.consumer.Consumer;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.chat.ChatFormatter;
import network.vonix.vonixcore.xpsync.XPSyncManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Player event listener for logging chat and commands.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class PlayerEventListener {

    /**
     * Handle chat events.
     */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String user = player.getName().getString();
        String message = event.getRawText();
        long time = System.currentTimeMillis() / 1000L;

        // Handle chat formatting
        if (EssentialsConfig.CONFIG.chatFormattingEnabled.get()) {
            event.setCanceled(true);
            net.minecraft.network.chat.Component formatted = ChatFormatter.formatChatMessage(player, message);
            player.server.getPlayerList().broadcastSystemMessage(formatted, false);
            // Log to console manually since event is canceled
            VonixCore.LOGGER.info("[Chat] " + formatted.getString());
        }

        if (ProtectionConfig.CONFIG.logChat.getAsBoolean()) {
            Consumer.getInstance().queueEntry(new ChatLogEntry(time, user, message));
        }
    }

    /**
     * Handle command events.
     */
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (!ProtectionConfig.CONFIG.logCommands.getAsBoolean()) {
            return;
        }

        var source = event.getParseResults().getContext().getSource();
        if (source.getPlayer() == null) {
            return;
        }

        String user = source.getPlayer().getName().getString();
        String command = event.getParseResults().getReader().getString();
        long time = System.currentTimeMillis() / 1000L;

        // Don't log vonixcore lookup commands to avoid spam
        if (command.startsWith("vp lookup") || command.startsWith("vonixcore lookup")) {
            return;
        }

        Consumer.getInstance().queueEntry(new CommandLogEntry(time, user, command));
    }

    /**
     * Handle player death to save back location.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            network.vonix.vonixcore.teleport.TeleportManager.getInstance().saveLastLocation(player, true);
        }
    }

    // XP sync on join/leave removed - now using batch sync on intervals only
    // This is more efficient and reduces API calls

    /**
     * Chat log entry for the consumer queue.
     */
    public static class ChatLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String message;

        public ChatLogEntry(long time, String user, String message) {
            this.time = time;
            this.user = user;
            this.message = message;
        }

        @Override
        public void execute(Connection conn) throws SQLException {
            String sql = "INSERT INTO vp_chat (time, user, message) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, message);
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Command log entry for the consumer queue.
     */
    public static class CommandLogEntry implements Consumer.QueueEntry {
        private final long time;
        private final String user;
        private final String command;

        public CommandLogEntry(long time, String user, String command) {
            this.time = time;
            this.user = user;
            this.command = command;
        }

        @Override
        public void execute(Connection conn) throws SQLException {
            String sql = "INSERT INTO vp_command (time, user, command) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, command);
                stmt.executeUpdate();
            }
        }
    }
}
