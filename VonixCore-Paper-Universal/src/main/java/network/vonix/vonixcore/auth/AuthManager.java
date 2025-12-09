package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.AuthConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class AuthManager {

    private static AuthManager instance;
    private final VonixCore plugin;

    private final Set<UUID> loggedInPlayers = new HashSet<>();
    // Map<UUID, Long> sessionExpiry
    private final Map<UUID, Long> sessions = new HashMap<>();
    // Map<IP, Attepmts>
    private final Map<String, Integer> loginAttempts = new HashMap<>();

    public AuthManager(VonixCore plugin) {
        this.plugin = plugin;
        instance = this;
        initializeDatabase();
    }

    public static AuthManager getInstance() {
        return instance;
    }

    private void initializeDatabase() {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            String sql = "CREATE TABLE IF NOT EXISTS vonixcore_auth (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "ip_address VARCHAR(45), " +
                    "last_login BIGINT, " +
                    "reg_date BIGINT" +
                    ");";
            conn.createStatement().execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize Auth database", e);
        }
    }

    public boolean isRegistered(UUID uuid) {
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM vonixcore_auth WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isLoggedIn(Player player) {
        return loggedInPlayers.contains(player.getUniqueId());
    }

    public void register(Player player, String password) {
        if (password.length() < AuthConfig.minPasswordLength) {
            player.sendMessage(
                    ChatColor.RED + "Password must be at least " + AuthConfig.minPasswordLength + " characters long.");
            return;
        }

        String hash = hashPassword(password);
        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO vonixcore_auth (uuid, password_hash, ip_address, last_login, reg_date) VALUES (?, ?, ?, ?, ?)");
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, hash);
            stmt.setString(3, player.getAddress().getAddress().getHostAddress());
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();

            login(player);
            player.sendMessage(ChatColor.GREEN + "Successfully registered and logged in!");
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while registering.");
            e.printStackTrace();
        }
    }

    public void login(Player player, String password) {
        if (isLoggedIn(player)) {
            player.sendMessage(ChatColor.RED + "You are already logged in.");
            return;
        }

        try (Connection conn = plugin.getDatabase().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT password_hash FROM vonixcore_auth WHERE uuid = ?");
            stmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (checkPassword(password, storedHash)) {
                    login(player);
                    player.sendMessage(ChatColor.GREEN + "Successfully logged in!");
                } else {
                    player.sendMessage(ChatColor.RED + "Incorrect password!");
                    // Handle attempts
                }
            } else {
                player.sendMessage(ChatColor.RED + "You are not registered! Use /register <password>");
            }
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "An error occurred while logging in.");
            e.printStackTrace();
        }
    }

    // Internal login (skip password check, used for session/registration)
    private void login(Player player) {
        loggedInPlayers.add(player.getUniqueId());
        if (AuthConfig.enableSessions) {
            sessions.put(player.getUniqueId(), System.currentTimeMillis() + (AuthConfig.sessionTimeout * 1000L));
        }
    }

    public void logout(Player player) {
        loggedInPlayers.remove(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Logged out.");
    }

    public void handleJoin(Player player) {
        if (!isRegistered(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Welcome! Please register using /register <password> <confirm>");
            return;
        }

        // Check session
        if (AuthConfig.enableSessions && sessions.containsKey(player.getUniqueId())) {
            long expiry = sessions.get(player.getUniqueId());
            if (System.currentTimeMillis() < expiry) {
                // Check IP match if we wanted to be strict, but simplified session for now
                loggedInPlayers.add(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Session restored. Welcome back!");
                return;
            } else {
                sessions.remove(player.getUniqueId());
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Please log in using /login <password>");
    }

    public void handleQuit(Player player) {
        loggedInPlayers.remove(player.getUniqueId());
        // Do NOT remove session here; session persists across re-joins
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }
}
