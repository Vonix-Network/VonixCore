package network.vonix.vonixcore.xpsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.XPSyncConfig;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class XPSyncManager {

    private static XPSyncManager instance;

    private final VonixCore plugin;
    private final String apiEndpoint;
    private final String apiKey;
    private final String serverName;
    private final Gson gson;
    private ScheduledExecutorService scheduler;

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int SHUTDOWN_READ_TIMEOUT = 10000;

    public XPSyncManager(VonixCore plugin) {
        instance = this;
        this.plugin = plugin;
        this.apiEndpoint = XPSyncConfig.apiEndpoint;
        this.apiKey = XPSyncConfig.apiKey;
        this.serverName = XPSyncConfig.serverName;
        this.gson = new GsonBuilder().setLenient().create();
    }

    public static XPSyncManager getInstance() {
        return instance;
    }

    public void start() {
        int intervalSeconds = XPSyncConfig.syncInterval;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VonixCore-XPSync-Thread");
            t.setDaemon(false);
            return t;
        });

        // We skip offline sync in the universal bukkit port to avoid NMS dependencies
        // for NBT reading.
        // Instead, we ensure all online players are synced regularly.

        scheduler.scheduleAtFixedRate(
                this::syncOnlinePlayers,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS);

        plugin.getLogger().info("[XPSync] Service started, syncing every " + intervalSeconds + " seconds.");
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            plugin.getLogger().info("[XPSync] Stopping service...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            instance = null;
        }
    }

    private void syncOnlinePlayers() {
        try {
            List<Player> players = (List<Player>) Bukkit.getOnlinePlayers();
            if (players.isEmpty()) {
                if (XPSyncConfig.verboseLogging) {
                    plugin.getLogger().info("[XPSync] No players online, skipping sync.");
                }
                return;
            }

            JsonObject payload = buildPayload(players);
            sendToAPI(payload, false);

            if (XPSyncConfig.verboseLogging) {
                plugin.getLogger().info("[XPSync] Synced " + players.size() + " online players.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[XPSync] Error syncing online players", e);
        }
    }

    // Batch sync only - single-player sync removed for optimization
    // All players are now synced together on intervals

    private JsonObject buildPayload(List<Player> players) {
        JsonObject root = new JsonObject();
        root.addProperty("serverName", serverName);
        JsonArray playersArray = new JsonArray();
        for (Player player : players) {
            playersArray.add(buildPlayerData(player));
        }
        root.add("players", playersArray);
        return root;
    }

    private JsonObject buildPlayerData(Player player) {
        JsonObject data = new JsonObject();
        data.addProperty("uuid", player.getUniqueId().toString());
        data.addProperty("username", player.getName());
        data.addProperty("level", player.getLevel());
        data.addProperty("totalExperience", getTotalExperience(player));

        if (XPSyncConfig.trackHealth) {
            data.addProperty("currentHealth", player.getHealth());
        }

        if (XPSyncConfig.trackPlaytime) {
            // Paper/Spigot statistic API
            int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE); // This is actually ticks played
            data.addProperty("playtimeSeconds", ticks / 20);
        }

        return data;
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int totalXP = 0;

        // Same formula as NeoForge/Minecraft wiki
        if (level >= 32) {
            totalXP = (int) (4.5 * level * level - 162.5 * level + 2220);
        } else if (level >= 17) {
            totalXP = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalXP = level * level + 6 * level;
        }

        // Add progress
        // Bukkit: getExp() is a float 0..1 representing progress to next level
        // We need XP for next level to calculate actual points
        int xpForNextLevel;
        if (level >= 31) {
            xpForNextLevel = 9 * level - 158;
        } else if (level >= 16) {
            xpForNextLevel = 5 * level - 38;
        } else {
            xpForNextLevel = 2 * level + 7;
        }

        totalXP += Math.round(player.getExp() * xpForNextLevel);
        return totalXP;
    }

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private void sendToAPI(JsonObject payload, boolean isShutdown) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            if (trySendToAPI(payload, isShutdown, attempt)) {
                return; // Success
            }
            if (attempt < MAX_RETRIES && !isShutdown) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        plugin.getLogger().severe("[XPSync] Failed to sync after " + MAX_RETRIES + " attempts");
    }

    private boolean trySendToAPI(JsonObject payload, boolean isShutdown, int attempt) {
        HttpURLConnection conn = null;
        try {
            String jsonPayload = gson.toJson(payload);
            URL url = new URL(apiEndpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("User-Agent", "VonixCore-XPSync-Bukkit");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(isShutdown ? SHUTDOWN_READ_TIMEOUT : READ_TIMEOUT);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                if (XPSyncConfig.verboseLogging) {
                    plugin.getLogger().info("[XPSync] Sync successful (attempt " + attempt + ")");
                }
                return true;
            } else if (code >= 500) {
                // Server error, worth retrying
                plugin.getLogger()
                        .warning("[XPSync] Server error " + code + " (attempt " + attempt + "/" + MAX_RETRIES + ")");
                return false;
            } else {
                // Client error (4xx), don't retry
                plugin.getLogger().warning("[XPSync] API Error: " + code);
                return true; // Don't retry client errors
            }

        } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
            plugin.getLogger().warning(
                    "[XPSync] Connection failed (attempt " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("[XPSync] Failed to send data: " + e.getMessage());
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }
}
