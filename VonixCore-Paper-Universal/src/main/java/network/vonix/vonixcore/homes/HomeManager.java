package network.vonix.vonixcore.homes;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class HomeManager {

    private static HomeManager instance;
    private final VonixCore plugin;

    public HomeManager(VonixCore plugin) {
        this.plugin = plugin;
    }

    public static HomeManager getInstance() {
        if (instance == null) {
            instance = new HomeManager(VonixCore.getInstance());
        }
        return instance;
    }

    public boolean setHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        String world = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();

        try (Connection conn = plugin.getDatabase().getConnection()) {
            int homeCount = getHomeCount(uuid);
            int maxHomes = EssentialsConfig.maxHomes;
            if (homeCount >= maxHomes && !homeExists(uuid, name)) {
                return false;
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO vonixcore_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name.toLowerCase());
            stmt.setString(3, world);
            stmt.setDouble(4, x);
            stmt.setDouble(5, y);
            stmt.setDouble(6, z);
            stmt.setFloat(7, yaw);
            stmt.setFloat(8, pitch);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set home", e);
            return false;
        }
    }

    public boolean deleteHome(UUID uuid, String name) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM vonixcore_homes WHERE uuid = ? AND name = ?");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name.toLowerCase());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete home", e);
            return false;
        }
    }

    public Location getHome(UUID uuid, String name) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT world, x, y, z, yaw, pitch FROM vonixcore_homes WHERE uuid = ? AND name = ?");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name.toLowerCase());
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
            plugin.getLogger().log(Level.SEVERE, "Failed to get home", e);
        }
        return null;
    }

    public List<String> getHomes(UUID uuid) {
        List<String> homes = new ArrayList<>();
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT name FROM vonixcore_homes WHERE uuid = ? ORDER BY name");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                homes.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to list homes", e);
        }
        return homes;
    }

    public int getHomeCount(UUID uuid) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM vonixcore_homes WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to count homes", e);
        }
        return 0;
    }

    public boolean homeExists(UUID uuid, String name) {
        return getHome(uuid, name) != null;
    }
}
