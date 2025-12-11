package network.vonix.vonixcore.protection;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Main protection event listener for block changes.
 * Logs block breaks, places, and explosions.
 */
public class ProtectionListener implements Listener {

    private final VonixCore plugin;

    // Action constants matching CoreProtect
    private static final int ACTION_BREAK = 0;
    private static final int ACTION_PLACE = 1;
    private static final int ACTION_EXPLODE = 2;

    public ProtectionListener(VonixCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle block inspector mode - intercept clicks
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInspectorClick(PlayerInteractEvent event) {
        if (!ProtectionConfig.enabled)
            return;

        Player player = event.getPlayer();
        if (!ProtectionCommands.isInspecting(player.getUniqueId()))
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        // Cancel the event to prevent block interaction
        event.setCancelled(true);

        // Inspect the block
        ProtectionCommands.inspectBlock(player, block);
    }

    /**
     * Log block breaks.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockBreak)
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        String user = player.getName();
        String world = block.getWorld().getName();
        Location loc = block.getLocation();
        long time = System.currentTimeMillis() / 1000L;
        String blockType = "minecraft:" + block.getType().name().toLowerCase();

        logBlockAction(time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                blockType, blockType, null, "minecraft:air", null, ACTION_BREAK);
    }

    /**
     * Log block places.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockPlace)
            return;

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Block replaced = event.getBlockReplacedState().getBlock();

        String user = player.getName();
        String world = block.getWorld().getName();
        Location loc = block.getLocation();
        long time = System.currentTimeMillis() / 1000L;
        String newType = "minecraft:" + block.getType().name().toLowerCase();
        String oldType = "minecraft:" + event.getBlockReplacedState().getType().name().toLowerCase();

        logBlockAction(time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                newType, oldType, null, newType, null, ACTION_PLACE);
    }

    /**
     * Log entity explosions (creepers, TNT, etc.)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockExplode)
            return;

        String user = "#" + event.getEntity().getType().name().toLowerCase();
        String world = event.getLocation().getWorld().getName();
        long time = System.currentTimeMillis() / 1000L;

        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            String blockType = "minecraft:" + block.getType().name().toLowerCase();

            logBlockAction(time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    blockType, blockType, null, "minecraft:air", null, ACTION_EXPLODE);
        }
    }

    /**
     * Log block explosions (from beds in nether, respawn anchors, etc.)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockExplode)
            return;

        String user = "#" + event.getBlock().getType().name().toLowerCase();
        String world = event.getBlock().getWorld().getName();
        long time = System.currentTimeMillis() / 1000L;

        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            String blockType = "minecraft:" + block.getType().name().toLowerCase();

            logBlockAction(time, user, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                    blockType, blockType, null, "minecraft:air", null, ACTION_EXPLODE);
        }
    }

    /**
     * Log a block action to the database asynchronously.
     */
    private void logBlockAction(long time, String user, String world, int x, int y, int z,
            String type, String oldType, String oldData, String newType, String newData, int action) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO vp_block (time, user, world, x, y, z, type, old_type, old_data, new_type, new_data, action) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setLong(1, time);
                stmt.setString(2, user);
                stmt.setString(3, world);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, type);
                stmt.setString(8, oldType);
                stmt.setString(9, oldData);
                stmt.setString(10, newType);
                stmt.setString(11, newData);
                stmt.setInt(12, action);
                stmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to log block action", e);
            }
        });
    }
}
