package network.vonix.vonixcore.warps;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class WarpManager {

    private static WarpManager instance;
    private final VonixCore plugin;

    public WarpManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public static WarpManager getInstance() {
        if (instance == null) {
            instance = new WarpManager(VonixCore.getInstance());
        }
        return instance;
    }

    public boolean setWarp(String name, Player player) {
        Location loc = player.getLocation();
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();

        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO vonixcore_warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, name.toLowerCase());
            stmt.setString(2, world);
            stmt.setDouble(3, x);
            stmt.setDouble(4, y);
            stmt.setDouble(5, z);
            stmt.setFloat(6, yaw);
            stmt.setFloat(7, pitch);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set warp", e);
            return false;
        }
    }

    public boolean deleteWarp(String name) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM vonixcore_warps WHERE name = ?");
            stmt.setString(1, name.toLowerCase());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete warp", e);
            return false;
        }
    }

    public Location getWarp(String name) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT world, x, y, z, yaw, pitch FROM vonixcore_warps WHERE name = ?");
            stmt.setString(1, name.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world");
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world == null)
                    return null;

                return new Location(
                        world,
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get warp", e);
        }
        return null;
    }

    public List<String> getWarps() {
        List<String> warps = new ArrayList<>();
        try (Connection conn = plugin.getDatabase().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT name FROM vonixcore_warps ORDER BY name");
            while (rs.next()) {
                warps.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to list warps", e);
        }
        return warps;
    }
}
