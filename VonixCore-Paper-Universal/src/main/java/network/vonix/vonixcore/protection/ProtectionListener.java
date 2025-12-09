package network.vonix.vonixcore.protection;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class ProtectionListener implements Listener {

    private final VonixCore plugin;

    public ProtectionListener(VonixCore plugin) {
        this.plugin = plugin;
    }

    private void logAction(String sql, Object... params) {
        // In a real implementation, this should use a Consumer/Producer pattern or
        // async queue
        // to avoid blocking the main thread during database writes.
        // For this port, we'll run it async immediately.
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockBreak)
            return;

        String user = event.getPlayer().getName();
        String world = event.getBlock().getWorld().getName();
        int x = event.getBlock().getX();
        int y = event.getBlock().getY();
        int z = event.getBlock().getZ();
        String type = event.getBlock().getType().name();
        long time = System.currentTimeMillis();

        logAction("INSERT INTO vp_block (time, user, world, x, y, z, type, action) VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                time, user, world, x, y, z, type);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockPlace)
            return;

        String user = event.getPlayer().getName();
        String world = event.getBlock().getWorld().getName();
        int x = event.getBlock().getX();
        int y = event.getBlock().getY();
        int z = event.getBlock().getZ();
        String type = event.getBlock().getType().name();
        long time = System.currentTimeMillis();

        logAction("INSERT INTO vp_block (time, user, world, x, y, z, type, action) VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                time, user, world, x, y, z, type);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!ProtectionConfig.enabled || !ProtectionConfig.logBlockExplode)
            return;

        // This is simplified. Proper logging needs to log every block destroyed.
    }
}
