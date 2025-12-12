package network.vonix.vonixcore.xpsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.XPSyncConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

/**
 * XPSyncManager - Handles syncing player XP data to the Vonix Network website.
 * Updated to sync ALL players from world data on startup/shutdown.
 */
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
        if (!XPSyncConfig.enabled) {
            plugin.getLogger().info("[XPSync] XPSync is disabled in config.");
            return;
        }

        int intervalSeconds = XPSyncConfig.syncInterval;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VonixCore-XPSync-Thread");
            t.setDaemon(false);
            return t;
        });

        // Sync ALL players from world data on startup
        plugin.getLogger().info("[XPSync] Running startup sync for ALL players from world data...");
        scheduler.execute(this::syncAllPlayersFromWorldData);

        // Schedule regular syncs for online players only
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

            // Sync ALL players from world data before shutdown
            plugin.getLogger().info("[XPSync] Running final sync for ALL players...");
            syncAllPlayersFromWorldDataSync();

            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("[XPSync] Scheduler did not terminate in time, forcing shutdown...");
                    scheduler.shutdownNow();
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        plugin.getLogger().severe("[XPSync] Scheduler could not be terminated!");
                    }
                }
            } catch (InterruptedException e) {
                plugin.getLogger().warning("[XPSync] Interrupted while waiting for shutdown");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            plugin.getLogger().info("[XPSync] Service stopped.");
            instance = null;
        }
    }

    /**
     * Sync ALL players from world data files (startup/shutdown)
     */
    private void syncAllPlayersFromWorldData() {
        try {
            List<JsonObject> allPlayers = getAllPlayersFromWorldData();

            if (allPlayers.isEmpty()) {
                plugin.getLogger().info("[XPSync] No player data found in world files");
                return;
            }

            plugin.getLogger()
                    .info("[XPSync] Found " + allPlayers.size() + " players in world data, syncing to API...");

            // Send in batches of 50 to avoid overwhelming the API
            int batchSize = 50;
            for (int i = 0; i < allPlayers.size(); i += batchSize) {
                int end = Math.min(i + batchSize, allPlayers.size());
                List<JsonObject> batch = allPlayers.subList(i, end);

                JsonObject payload = new JsonObject();
                payload.addProperty("serverName", serverName);
                JsonArray playersArray = new JsonArray();
                batch.forEach(playersArray::add);
                payload.add("players", playersArray);
                payload.addProperty("_playerCount", batch.size());

                plugin.getLogger().info(
                        "[XPSync] Syncing batch " + (i + 1) + "-" + end + " of " + allPlayers.size() + " players...");
                sendToAPI(payload, false);

                // Small delay between batches
                if (end < allPlayers.size()) {
                    Thread.sleep(500);
                }
            }

            plugin.getLogger().info("[XPSync] Completed syncing all " + allPlayers.size() + " players from world data");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[XPSync] Error syncing all player data from world files", e);
        }
    }

    /**
     * Synchronous version for shutdown
     */
    private void syncAllPlayersFromWorldDataSync() {
        try {
            List<JsonObject> allPlayers = getAllPlayersFromWorldData();

            if (allPlayers.isEmpty()) {
                plugin.getLogger().info("[XPSync] No player data found in world files for final sync");
                return;
            }

            plugin.getLogger().info("[XPSync] Final sync: " + allPlayers.size() + " players from world data");

            JsonObject payload = new JsonObject();
            payload.addProperty("serverName", serverName);
            JsonArray playersArray = new JsonArray();
            allPlayers.forEach(playersArray::add);
            payload.add("players", playersArray);
            payload.addProperty("_playerCount", allPlayers.size());

            sendToAPI(payload, true);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[XPSync] Error during final sync of all players", e);
        }
    }

    /**
     * Read all player data from world/playerdata/ folder
     */
    private List<JsonObject> getAllPlayersFromWorldData() {
        List<JsonObject> players = new ArrayList<>();

        try {
            // Get main world folder
            World mainWorld = Bukkit.getWorlds().get(0);
            if (mainWorld == null) {
                plugin.getLogger().warning("[XPSync] No main world found");
                return players;
            }

            File worldFolder = mainWorld.getWorldFolder();
            File playerDataFolder = new File(worldFolder, "playerdata");
            File statsFolder = new File(worldFolder, "stats");

            if (!playerDataFolder.exists()) {
                plugin.getLogger()
                        .warning("[XPSync] Player data folder not found: " + playerDataFolder.getAbsolutePath());
                return players;
            }

            // First, collect all online players (most accurate data)
            Set<String> onlineUuids = new HashSet<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                players.add(buildPlayerDataFromOnline(onlinePlayer));
                onlineUuids.add(onlinePlayer.getUniqueId().toString());
            }

            // Then read offline player data files
            File[] datFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".dat"));

            if (datFiles == null) {
                return players;
            }

            for (File datFile : datFiles) {
                try {
                    String fileName = datFile.getName();
                    String uuidStr = fileName.replace(".dat", "");

                    // Skip online players (already added with current data)
                    if (onlineUuids.contains(uuidStr)) {
                        continue;
                    }

                    UUID uuid;
                    try {
                        uuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }

                    // Read NBT data from .dat file (gzip compressed)
                    Map<String, Object> nbtData = readPlayerNBT(datFile);
                    if (nbtData == null)
                        continue;

                    JsonObject playerData = new JsonObject();
                    playerData.addProperty("uuid", uuid.toString());

                    // Extract XP data from NBT
                    int xpLevel = getIntFromNBT(nbtData, "XpLevel", 0);
                    float xpProgress = getFloatFromNBT(nbtData, "XpP", 0f);
                    int xpTotal = getIntFromNBT(nbtData, "XpTotal", 0);

                    if (xpTotal == 0 && xpLevel > 0) {
                        xpTotal = calculateTotalXP(xpLevel, xpProgress);
                    }

                    playerData.addProperty("level", xpLevel);
                    playerData.addProperty("totalExperience", xpTotal);

                    // Get username from OfflinePlayer or cache
                    String username = getPlayerUsername(uuid);
                    playerData.addProperty("username", username != null ? username : uuidStr);

                    // Always include playtime (even if 0) to ensure consistency
                    if (XPSyncConfig.trackPlaytime) {
                        int playtime = getPlaytimeFromStats(uuid, statsFolder);
                        playerData.addProperty("playtimeSeconds", Math.max(0, playtime));
                    }

                    players.add(playerData);

                } catch (Exception e) {
                    if (XPSyncConfig.verboseLogging) {
                        plugin.getLogger().warning("[XPSync] Failed to read player data file: " + datFile.getName()
                                + " - " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[XPSync] Error reading player data from world files", e);
        }

        return players;
    }

    /**
     * Read NBT data from a gzip-compressed .dat file
     * Uses a simple approach to extract just the XP values without full NBT parsing
     */
    private Map<String, Object> readPlayerNBT(File datFile) {
        Map<String, Object> result = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(datFile);
                GZIPInputStream gzis = new GZIPInputStream(fis);
                DataInputStream dis = new DataInputStream(gzis)) {

            // Read the NBT root compound tag
            byte tagType = dis.readByte();
            if (tagType != 10) { // 10 = TAG_Compound
                return null;
            }

            // Skip root tag name
            short nameLength = dis.readShort();
            dis.skipBytes(nameLength);

            // Parse compound contents looking for XP values
            parseCompound(dis, result, "");

        } catch (Exception e) {
            if (XPSyncConfig.verboseLogging) {
                plugin.getLogger()
                        .warning("[XPSync] Failed to parse NBT for " + datFile.getName() + ": " + e.getMessage());
            }
            return null;
        }

        return result;
    }

    /**
     * Parse NBT compound tag recursively
     */
    private void parseCompound(DataInputStream dis, Map<String, Object> result, String prefix) throws IOException {
        while (true) {
            byte tagType = dis.readByte();
            if (tagType == 0)
                break; // TAG_End

            short nameLength = dis.readShort();
            byte[] nameBytes = new byte[nameLength];
            dis.readFully(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            switch (tagType) {
                case 1: // TAG_Byte
                    dis.readByte();
                    break;
                case 2: // TAG_Short
                    dis.readShort();
                    break;
                case 3: // TAG_Int
                    int intVal = dis.readInt();
                    if (name.equals("XpLevel") || name.equals("XpTotal")) {
                        result.put(name, intVal);
                    }
                    break;
                case 4: // TAG_Long
                    dis.readLong();
                    break;
                case 5: // TAG_Float
                    float floatVal = dis.readFloat();
                    if (name.equals("XpP")) {
                        result.put(name, floatVal);
                    }
                    break;
                case 6: // TAG_Double
                    dis.readDouble();
                    break;
                case 7: // TAG_Byte_Array
                    int byteArrayLen = dis.readInt();
                    dis.skipBytes(byteArrayLen);
                    break;
                case 8: // TAG_String
                    short strLen = dis.readShort();
                    dis.skipBytes(strLen);
                    break;
                case 9: // TAG_List
                    byte listType = dis.readByte();
                    int listLen = dis.readInt();
                    skipListContents(dis, listType, listLen);
                    break;
                case 10: // TAG_Compound
                    parseCompound(dis, result, prefix + name + ".");
                    break;
                case 11: // TAG_Int_Array
                    int intArrayLen = dis.readInt();
                    dis.skipBytes(intArrayLen * 4);
                    break;
                case 12: // TAG_Long_Array
                    int longArrayLen = dis.readInt();
                    dis.skipBytes(longArrayLen * 8);
                    break;
            }

            // Early exit if we found all XP values
            if (result.containsKey("XpLevel") && result.containsKey("XpP") && result.containsKey("XpTotal")) {
                return;
            }
        }
    }

    /**
     * Skip list contents
     */
    private void skipListContents(DataInputStream dis, byte listType, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            switch (listType) {
                case 1:
                    dis.readByte();
                    break;
                case 2:
                    dis.readShort();
                    break;
                case 3:
                    dis.readInt();
                    break;
                case 4:
                    dis.readLong();
                    break;
                case 5:
                    dis.readFloat();
                    break;
                case 6:
                    dis.readDouble();
                    break;
                case 7:
                    int len = dis.readInt();
                    dis.skipBytes(len);
                    break;
                case 8:
                    short strLen = dis.readShort();
                    dis.skipBytes(strLen);
                    break;
                case 9:
                    byte innerType = dis.readByte();
                    int innerLen = dis.readInt();
                    skipListContents(dis, innerType, innerLen);
                    break;
                case 10:
                    parseCompound(dis, new HashMap<>(), "");
                    break;
                case 11:
                    int intArrLen = dis.readInt();
                    dis.skipBytes(intArrLen * 4);
                    break;
                case 12:
                    int longArrLen = dis.readInt();
                    dis.skipBytes(longArrLen * 8);
                    break;
            }
        }
    }

    private int getIntFromNBT(Map<String, Object> nbt, String key, int defaultValue) {
        Object val = nbt.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    private float getFloatFromNBT(Map<String, Object> nbt, String key, float defaultValue) {
        Object val = nbt.get(key);
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        return defaultValue;
    }

    /**
     * Build player data from an online player (most accurate)
     */
    private JsonObject buildPlayerDataFromOnline(Player player) {
        JsonObject data = new JsonObject();
        data.addProperty("uuid", player.getUniqueId().toString());
        data.addProperty("username", player.getName());
        data.addProperty("level", player.getLevel());
        data.addProperty("totalExperience", getTotalExperience(player));

        if (XPSyncConfig.trackHealth) {
            data.addProperty("currentHealth", player.getHealth());
        }

        // Always include playtime when tracking is enabled (even if 0)
        if (XPSyncConfig.trackPlaytime) {
            try {
                int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                int playTimeSeconds = Math.max(0, ticks / 20);
                data.addProperty("playtimeSeconds", playTimeSeconds);
            } catch (Exception e) {
                data.addProperty("playtimeSeconds", 0);
                if (XPSyncConfig.verboseLogging) {
                    plugin.getLogger()
                            .warning("[XPSync] Failed to get playtime for " + player.getName() + ": " + e.getMessage());
                }
            }
        }

        return data;
    }

    /**
     * Get player username from OfflinePlayer cache
     */
    private String getPlayerUsername(UUID uuid) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            return offlinePlayer.getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get playtime from stats JSON file
     */
    private int getPlaytimeFromStats(UUID uuid, File statsFolder) {
        try {
            File statsFile = new File(statsFolder, uuid.toString() + ".json");
            if (!statsFile.exists()) {
                return 0;
            }

            String content = new String(Files.readAllBytes(statsFile.toPath()), StandardCharsets.UTF_8);
            JsonObject stats = JsonParser.parseString(content).getAsJsonObject();

            if (stats.has("stats")) {
                JsonObject statsObj = stats.getAsJsonObject("stats");
                if (statsObj.has("minecraft:custom")) {
                    JsonObject custom = statsObj.getAsJsonObject("minecraft:custom");
                    if (custom.has("minecraft:play_time")) {
                        int ticks = custom.get("minecraft:play_time").getAsInt();
                        return ticks / 20;
                    }
                    // Fallback for older stat name
                    if (custom.has("minecraft:play_one_minute")) {
                        int ticks = custom.get("minecraft:play_one_minute").getAsInt();
                        return ticks / 20;
                    }
                }
            }
        } catch (Exception e) {
            if (XPSyncConfig.verboseLogging) {
                plugin.getLogger().warning("[XPSync] Failed to read stats for " + uuid + ": " + e.getMessage());
            }
        }
        return 0;
    }

    /**
     * Calculate total XP from level and progress
     */
    private int calculateTotalXP(int level, float progress) {
        int totalXP = 0;

        if (level >= 32) {
            totalXP = (int) (4.5 * level * level - 162.5 * level + 2220);
        } else if (level >= 17) {
            totalXP = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalXP = level * level + 6 * level;
        }

        int xpForNextLevel;
        if (level >= 31) {
            xpForNextLevel = 9 * level - 158;
        } else if (level >= 16) {
            xpForNextLevel = 5 * level - 38;
        } else {
            xpForNextLevel = 2 * level + 7;
        }

        totalXP += Math.round(progress * xpForNextLevel);
        return totalXP;
    }

    private void syncOnlinePlayers() {
        try {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            if (players.isEmpty()) {
                if (XPSyncConfig.verboseLogging) {
                    plugin.getLogger().info("[XPSync] No players online, skipping sync.");
                }
                return;
            }

            JsonObject payload = buildPayload(new ArrayList<>(players));
            sendToAPI(payload, false);

            if (XPSyncConfig.verboseLogging) {
                plugin.getLogger().info("[XPSync] Synced " + players.size() + " online players.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[XPSync] Error syncing online players", e);
        }
    }

    private JsonObject buildPayload(List<Player> players) {
        JsonObject root = new JsonObject();
        root.addProperty("serverName", serverName);
        JsonArray playersArray = new JsonArray();
        for (Player player : players) {
            playersArray.add(buildPlayerDataFromOnline(player));
        }
        root.add("players", playersArray);
        root.addProperty("_playerCount", players.size());
        return root;
    }

    private int getTotalExperience(Player player) {
        int level = player.getLevel();
        int totalXP = 0;

        if (level >= 32) {
            totalXP = (int) (4.5 * level * level - 162.5 * level + 2220);
        } else if (level >= 17) {
            totalXP = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalXP = level * level + 6 * level;
        }

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
                return;
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

    private String readResponse(HttpURLConnection conn, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null)
            return "";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(256);
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private boolean trySendToAPI(JsonObject payload, boolean isShutdown, int attempt) {
        HttpURLConnection conn = null;
        try {
            String jsonPayload = gson.toJson(payload);
            URL url = new URL(apiEndpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("User-Agent", "VonixCore-XPSync-Bukkit/1.0.0");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(isShutdown ? SHUTDOWN_READ_TIMEOUT : READ_TIMEOUT);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int statusCode = conn.getResponseCode();
            String responseBody = readResponse(conn, statusCode);

            if (statusCode == 200) {
                int expectedCount = payload.has("_playerCount") ? payload.get("_playerCount").getAsInt()
                        : (payload.has("players") ? payload.getAsJsonArray("players").size() : 0);
                handleSuccessResponse(responseBody, expectedCount);
                return true;
            } else if (statusCode == 401) {
                plugin.getLogger().severe("[XPSync] Authentication failed! Check your API key.");
                return true;
            } else if (statusCode == 403) {
                plugin.getLogger().severe("[XPSync] API key invalid or server not recognized.");
                return true;
            } else if (statusCode >= 500) {
                plugin.getLogger().warning("[XPSync] Server error " + statusCode + " (attempt " + attempt + "/"
                        + MAX_RETRIES + "): " + responseBody);
                return false;
            } else {
                plugin.getLogger().severe("[XPSync] Failed to sync. HTTP " + statusCode + ": " + responseBody);
                return true;
            }

        } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
            plugin.getLogger().warning(
                    "[XPSync] Connection failed (attempt " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
            return false;
        } catch (java.net.UnknownHostException e) {
            plugin.getLogger().severe("[XPSync] Unknown host: " + apiEndpoint);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[XPSync] Error sending to API (attempt " + attempt + "/" + MAX_RETRIES + "): " + e.getMessage());
            return false;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    private void handleSuccessResponse(String responseBody, int expectedCount) {
        if (responseBody == null || responseBody.isEmpty())
            return;

        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            if (response.has("success") && response.get("success").getAsBoolean()) {
                int synced = response.has("syncedCount") ? response.get("syncedCount").getAsInt() : 0;
                if (synced == expectedCount) {
                    plugin.getLogger().info("[XPSync] Successfully synced " + synced + " players");
                } else if (synced > 0) {
                    plugin.getLogger()
                            .warning("[XPSync] Partial sync: " + synced + " of " + expectedCount + " players synced");
                } else {
                    plugin.getLogger().warning(
                            "[XPSync] API returned success but syncedCount=0 (expected " + expectedCount + ")");
                }
            } else {
                String error = response.has("error") ? response.get("error").getAsString() : "Unknown";
                plugin.getLogger().warning("[XPSync] API response: " + error);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[XPSync] Failed to parse response: " + responseBody);
        }
    }
}
