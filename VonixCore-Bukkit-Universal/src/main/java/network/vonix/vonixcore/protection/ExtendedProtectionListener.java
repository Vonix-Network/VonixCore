package network.vonix.vonixcore.protection;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Extended protection event listener for additional CoreProtect-style logging.
 * Logs containers, entity kills, interactions, chat, commands, and signs.
 */
public class ExtendedProtectionListener implements Listener {

    private final VonixCore plugin;

    // Action constants
    private static final int ACTION_KILL = 0;
    private static final int ACTION_REMOVE = 0;
    private static final int ACTION_ADD = 1;

    // Container snapshots for tracking changes
    private static final Map<UUID, ItemStack[]> containerSnapshots = new ConcurrentHashMap<>();
    private static final Map<UUID, Location> containerLocations = new ConcurrentHashMap<>();

    public ExtendedProtectionListener(VonixCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Log entity kills by players.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logEntityKills)
            return;

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null)
            return;

        String user = killer.getName();
        String world = entity.getWorld().getName();
        Location loc = entity.getLocation();
        long time = System.currentTimeMillis() / 1000L;
        String entityType = entity.getType().name().toLowerCase();

        logAsync(
                "INSERT INTO vp_entity (time, user, world, x, y, z, entity_type, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), entityType, ACTION_KILL);
    }

    /**
     * Log player interactions with blocks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logPlayerInteractions)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        // Don't log if player is inspecting
        if (ProtectionCommands.isInspecting(event.getPlayer().getUniqueId()))
            return;

        String blockType = block.getType().name().toLowerCase();

        // Only log interactive blocks
        if (!isInteractiveBlock(blockType))
            return;

        Player player = event.getPlayer();
        String user = player.getName();
        String world = block.getWorld().getName();
        Location loc = block.getLocation();
        long time = System.currentTimeMillis() / 1000L;

        logAsync("INSERT INTO vp_interaction (time, user, world, x, y, z, type) VALUES (?, ?, ?, ?, ?, ?, ?)",
                time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), blockType);
    }

    /**
     * Log chat messages.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logChat)
            return;

        Player player = event.getPlayer();
        String user = player.getName();
        String world = player.getWorld().getName();
        Location loc = player.getLocation();
        long time = System.currentTimeMillis() / 1000L;
        String message = event.getMessage();

        // Already async in this event
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO vp_chat (time, user, world, x, y, z, message) VALUES (?, ?, ?, ?, ?, ?, ?)");
            stmt.setLong(1, time);
            stmt.setString(2, user);
            stmt.setString(3, world);
            stmt.setInt(4, loc.getBlockX());
            stmt.setInt(5, loc.getBlockY());
            stmt.setInt(6, loc.getBlockZ());
            stmt.setString(7, message);
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to log chat", e);
        }
    }

    /**
     * Log commands.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logCommands)
            return;

        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Don't log sensitive commands
        if (command.contains("password") || command.contains("login") || command.contains("register"))
            return;

        String user = player.getName();
        String world = player.getWorld().getName();
        Location loc = player.getLocation();
        long time = System.currentTimeMillis() / 1000L;

        logAsync("INSERT INTO vp_command (time, user, world, x, y, z, command) VALUES (?, ?, ?, ?, ?, ?, ?)",
                time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), event.getMessage());
    }

    /**
     * Log sign text changes.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logSigns)
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        String user = player.getName();
        String world = block.getWorld().getName();
        Location loc = block.getLocation();
        long time = System.currentTimeMillis() / 1000L;

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String line = event.getLine(i);
            if (line != null && !line.isEmpty()) {
                if (text.length() > 0)
                    text.append("|");
                text.append(line);
            }
        }

        if (text.length() > 0) {
            logAsync("INSERT INTO vp_sign (time, user, world, x, y, z, text) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), text.toString());
        }
    }

    /**
     * Snapshot container on open.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logContainerTransactions)
            return;

        if (!(event.getPlayer() instanceof Player player))
            return;

        Inventory inv = event.getInventory();
        Location loc = inv.getLocation();
        if (loc == null)
            return;

        // Deep copy inventory contents
        ItemStack[] snapshot = new ItemStack[inv.getSize()];
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            snapshot[i] = item != null ? item.clone() : null;
        }

        containerSnapshots.put(player.getUniqueId(), snapshot);
        containerLocations.put(player.getUniqueId(), loc.clone());
    }

    /**
     * Compare and log on container close.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logContainerTransactions)
            return;

        if (!(event.getPlayer() instanceof Player player))
            return;

        ItemStack[] oldItems = containerSnapshots.remove(player.getUniqueId());
        Location loc = containerLocations.remove(player.getUniqueId());

        if (oldItems == null || loc == null)
            return;

        Inventory inv = event.getInventory();
        String user = player.getName();
        String world = loc.getWorld().getName();
        long time = System.currentTimeMillis() / 1000L;

        // Compare old vs new items
        for (int i = 0; i < Math.min(oldItems.length, inv.getSize()); i++) {
            ItemStack oldStack = oldItems[i];
            ItemStack newStack = inv.getItem(i);

            boolean oldEmpty = oldStack == null || oldStack.getType().isAir();
            boolean newEmpty = newStack == null || newStack.getType().isAir();

            if (oldEmpty && newEmpty)
                continue;

            // Check if items are the same
            boolean same = !oldEmpty && !newEmpty && oldStack.isSimilar(newStack);

            if (same && oldStack.getAmount() == newStack.getAmount())
                continue;

            // Item added or increased
            if (!newEmpty && (oldEmpty || !same || newStack.getAmount() > oldStack.getAmount())) {
                int amount = same ? newStack.getAmount() - oldStack.getAmount() : newStack.getAmount();
                String itemName = newStack.getType().name().toLowerCase();
                logAsync(
                        "INSERT INTO vp_container (time, user, world, x, y, z, type, item, amount, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                        "container", itemName, amount, ACTION_ADD);
            }

            // Item removed or decreased
            if (!oldEmpty && (newEmpty || !same || newStack.getAmount() < oldStack.getAmount())) {
                int amount = same ? oldStack.getAmount() - newStack.getAmount() : oldStack.getAmount();
                String itemName = oldStack.getType().name().toLowerCase();
                logAsync(
                        "INSERT INTO vp_container (time, user, world, x, y, z, type, item, amount, action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                        "container", itemName, amount, ACTION_REMOVE);
            }
        }
    }

    // Helper methods

    private void logAsync(String sql, Object... params) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to log protection event", e);
            }
        });
    }

    private boolean isInteractiveBlock(String blockType) {
        return blockType.contains("door") ||
                blockType.contains("button") ||
                blockType.contains("lever") ||
                blockType.contains("gate") ||
                blockType.contains("trapdoor") ||
                blockType.contains("chest") ||
                blockType.contains("furnace") ||
                blockType.contains("anvil") ||
                blockType.contains("crafting") ||
                blockType.contains("barrel") ||
                blockType.contains("shulker") ||
                blockType.contains("hopper") ||
                blockType.contains("dispenser") ||
                blockType.contains("dropper") ||
                blockType.contains("brewing") ||
                blockType.contains("enchant");
    }
}
