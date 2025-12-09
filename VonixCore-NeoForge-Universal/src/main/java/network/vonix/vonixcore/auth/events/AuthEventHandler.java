package network.vonix.vonixcore.auth.events;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.auth.AuthenticationManager;
import network.vonix.vonixcore.auth.AuthCommands;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player events for authentication - freezing, command registration,
 * etc.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class AuthEventHandler {
    private static final Map<UUID, Boolean> frozenPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastChatReminder = new ConcurrentHashMap<>();

    private static boolean isFrozen(UUID uuid) {
        return frozenPlayers.computeIfAbsent(uuid, AuthenticationManager::shouldFreeze);
    }

    public static void updateFreezeState(UUID uuid) {
        if (AuthenticationManager.isAuthenticated(uuid)) {
            frozenPlayers.remove(uuid);
        } else {
            frozenPlayers.put(uuid, AuthenticationManager.shouldFreeze(uuid));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AuthCommands.register(event.getDispatcher());
        VonixCore.LOGGER.info("[VonixCore] Auth commands registered");
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AuthenticationManager.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        AuthenticationManager.onPlayerLeave(uuid);
        frozenPlayers.remove(uuid);
        lastChatReminder.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            player.teleportTo(player.getX(), player.getY(), player.getZ());
            player.setDeltaMovement(0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            event.setCanceled(true);
            ItemEntity itemEntity = event.getEntity();
            ItemStack item = itemEntity.getItem();
            if (player.getInventory().add(item)) {
                itemEntity.discard();
            } else {
                event.setCanceled(false);
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && isFrozen(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID uuid = player.getUUID();
        if (isFrozen(uuid)) {
            event.setCanceled(true);
            long now = System.currentTimeMillis();
            Long last = lastChatReminder.get(uuid);
            if (last == null || (now - last) >= 5000) {
                player.sendSystemMessage(
                        Component.literal("§cYou must authenticate! Use §e/login <password>§c or §e/register"));
                lastChatReminder.put(uuid, now);
            }
        }
    }
}
