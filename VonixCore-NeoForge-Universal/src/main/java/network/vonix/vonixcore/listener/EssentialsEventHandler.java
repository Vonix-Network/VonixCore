package network.vonix.vonixcore.listener;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.chat.ChatFormatter;
import network.vonix.vonixcore.command.UtilityCommands;
import network.vonix.vonixcore.command.WorldCommands;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.permissions.PermissionCommands;
import network.vonix.vonixcore.permissions.PermissionManager;
import network.vonix.vonixcore.teleport.TeleportManager;

import java.sql.Connection;

/**
 * Event handler for essentials features: commands, permissions, chat
 * formatting.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class EssentialsEventHandler {

    /**
     * Register all essentials commands.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!EssentialsConfig.CONFIG.enabled.get()) {
            return;
        }

        VonixCore.LOGGER.info("[VonixCore] Registering essentials commands...");

        // Register utility commands (tp, rtp, msg, nick, etc.)
        UtilityCommands.register(event.getDispatcher());

        // Register world commands (weather, time, afk, etc.)
        WorldCommands.register(event.getDispatcher());

        // Register permission commands (if not using LuckPerms)
        PermissionCommands.register(event.getDispatcher());

        VonixCore.LOGGER.info("[VonixCore] Essentials commands registered");
    }

    /**
     * Initialize permission system on server start.
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        if (!EssentialsConfig.CONFIG.enabled.get()) {
            return;
        }

        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            PermissionManager.getInstance().initialize(conn);
            VonixCore.LOGGER.info("[VonixCore] Permission system initialized");
        } catch (Exception e) {
            VonixCore.LOGGER.error("[VonixCore] Failed to initialize permission system", e);
        }
    }

    /**
     * Format chat messages with prefix/suffix.
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onChatFormat(ServerChatEvent event) {
        if (!EssentialsConfig.CONFIG.enabled.get()) {
            return;
        }

        ServerPlayer player = event.getPlayer();
        String rawMessage = event.getRawText();

        // Format the message with prefix/suffix
        Component formatted = ChatFormatter.formatChatMessage(player, rawMessage);

        // Cancel the event to prevent default formatting
        event.setCanceled(true);

        // Manually broadcast to all players
        player.getServer().getPlayerList().getPlayers().forEach(p -> {
            p.sendSystemMessage(formatted);
        });
    }

    /**
     * Track player join for /seen and permission cache.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Track for /seen command
            UtilityCommands.onPlayerJoin(player.getUUID());

            // Pre-load permission data
            PermissionManager.getInstance().getUser(player.getUUID());
        }
    }

    /**
     * Track player leave for /seen and clear AFK/ignore state.
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Track for /seen command
            UtilityCommands.onPlayerLeave(player.getUUID());

            // Clear AFK status
            WorldCommands.clearAfk(player.getUUID());

            // Clear permission cache for this player
            PermissionManager.getInstance().clearUserCache(player.getUUID());
        }
    }

    /**
     * Save death location for /back command.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!EssentialsConfig.CONFIG.enabled.get()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // Save death location so /back can return to it (isDeath=true for cooldown)
            TeleportManager.getInstance().saveLastLocation(player, true);
        }
    }
}
