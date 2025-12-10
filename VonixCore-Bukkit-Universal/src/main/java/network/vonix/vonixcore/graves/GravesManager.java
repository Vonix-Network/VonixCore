package network.vonix.vonixcore.graves;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player graves - creating, tracking, and cleanup
 */
public class GravesManager {
    private final VonixCore plugin;
    private final Map<UUID, Grave> graves = new ConcurrentHashMap<>();
    private final Map<Location, UUID> graveLocations = new ConcurrentHashMap<>();
    private final File dataFile;
    private BukkitTask cleanupTask;

    // Configuration
    private boolean enabled = true;
    private int expirationTime = 3600; // seconds
    private double xpRetention = 0.8; // 80% of XP stored
    private boolean protectionEnabled = true;
    private int protectionTime = 300; // seconds
    private int maxGravesPerPlayer = 5;

    public GravesManager(VonixCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "graves-data.yml");
        loadConfig();
        loadGraves();
        startCleanupTask();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "vonixcore-graves.yml");
        if (!configFile.exists()) {
            saveDefaultConfig(configFile);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        expirationTime = config.getInt("expiration-time", 3600);
        xpRetention = config.getDouble("xp-retention", 0.8);
        protectionEnabled = config.getBoolean("protection.enabled", true);
        protectionTime = config.getInt("protection.time", 300);
        maxGravesPerPlayer = config.getInt("max-graves-per-player", 5);
    }

    private void saveDefaultConfig(File configFile) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("expiration-time", 3600);
        config.set("xp-retention", 0.8);
        config.set("protection.enabled", true);
        config.set("protection.time", 300);
        config.set("max-graves-per-player", 5);
        config.set("hologram.enabled", false);
        config.set("hologram.lines", Arrays.asList(
                "&c☠ &f{player}'s Grave &c☠",
                "&7Items: &e{items}",
                "&7XP: &e{xp}",
                "&7Expires: &e{time}"));

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save graves config", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Creates a grave for a player at their death location
     */
    public Grave createGrave(Player player, List<ItemStack> items, int totalXp) {
        if (!enabled)
            return null;

        Location location = findSafeLocation(player.getLocation());
        if (location == null) {
            plugin.getLogger().warning("Could not find safe grave location for " + player.getName());
            return null;
        }

        // Calculate stored XP
        int storedXp = (int) (totalXp * xpRetention);

        // Calculate expiration time
        long expiresAt = System.currentTimeMillis() + (expirationTime * 1000L);

        // Create grave
        UUID graveId = UUID.randomUUID();
        Grave grave = new Grave(graveId, player.getUniqueId(), player.getName(),
                location, items, storedXp, expiresAt);

        // Limit graves per player
        cleanupExcessGraves(player.getUniqueId());

        // Store grave
        graves.put(graveId, grave);
        graveLocations.put(location, graveId);

        // Place chest block
        placeGraveBlock(location, grave);

        // Save to disk
        saveGraves();

        plugin.getLogger().info("Created grave for " + player.getName() + " at " +
                location.getWorld().getName() + " " +
                location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ());

        return grave;
    }

    private Location findSafeLocation(Location origin) {
        World world = origin.getWorld();
        if (world == null)
            return null;

        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();

        // Try original location first
        if (isValidGraveLocation(world, x, y, z)) {
            return new Location(world, x, y, z);
        }

        // Search upward
        for (int dy = 1; dy <= 10; dy++) {
            if (y + dy < world.getMaxHeight() && isValidGraveLocation(world, x, y + dy, z)) {
                return new Location(world, x, y + dy, z);
            }
        }

        // Search downward
        for (int dy = 1; dy <= 10; dy++) {
            if (y - dy >= world.getMinHeight() && isValidGraveLocation(world, x, y - dy, z)) {
                return new Location(world, x, y - dy, z);
            }
        }

        // Try surface
        int surfaceY = world.getHighestBlockYAt(x, z);
        if (isValidGraveLocation(world, x, surfaceY + 1, z)) {
            return new Location(world, x, surfaceY + 1, z);
        }

        return null;
    }

    private boolean isValidGraveLocation(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        Block below = world.getBlockAt(x, y - 1, z);

        // Check if block is air or replaceable
        Material type = block.getType();
        if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR &&
                !type.name().contains("WATER") && !type.name().contains("LAVA")) {
            return false;
        }

        // Check if there's solid ground below
        return below.getType().isSolid();
    }

    private void placeGraveBlock(Location location, Grave grave) {
        Block block = location.getBlock();
        block.setType(Material.CHEST);

        // Store items in chest
        BlockState state = block.getState();
        if (state instanceof Chest) {
            Chest chest = (Chest) state;
            Inventory inv = chest.getInventory();
            for (ItemStack item : grave.getItems()) {
                if (item != null) {
                    inv.addItem(item);
                }
            }
        }
    }

    /**
     * Gets a grave by its location
     */
    public Grave getGraveAt(Location location) {
        UUID graveId = graveLocations.get(location.getBlock().getLocation());
        return graveId != null ? graves.get(graveId) : null;
    }

    /**
     * Gets all graves for a player
     */
    public List<Grave> getPlayerGraves(UUID playerId) {
        List<Grave> playerGraves = new ArrayList<>();
        for (Grave grave : graves.values()) {
            if (grave.getOwnerId().equals(playerId)) {
                playerGraves.add(grave);
            }
        }
        return playerGraves;
    }

    /**
     * Checks if a player can loot this grave
     */
    public boolean canLoot(Player player, Grave grave) {
        if (grave.isLooted())
            return false;
        if (grave.isExpired())
            return true; // Anyone can loot expired graves
        if (grave.isOwner(player))
            return true;
        if (player.hasPermission("vonixcore.graves.bypass"))
            return true;

        // Check protection time
        if (protectionEnabled) {
            long protectionEndsAt = grave.getCreatedAt() + (protectionTime * 1000L);
            if (System.currentTimeMillis() < protectionEndsAt) {
                return false; // Still protected
            }
        }

        return true;
    }

    /**
     * Loots a grave - gives items and XP to player
     */
    public void lootGrave(Player player, Grave grave) {
        if (grave.isLooted())
            return;

        // Give XP
        if (grave.getExperience() > 0) {
            player.giveExp(grave.getExperience());
        }

        // Mark as looted
        grave.setLooted(true);

        // Remove grave block
        removeGrave(grave);

        plugin.getLogger().info(player.getName() + " looted grave of " + grave.getOwnerName());
    }

    /**
     * Removes a grave completely
     */
    public void removeGrave(Grave grave) {
        // Remove block
        Block block = grave.getLocation().getBlock();
        if (block.getType() == Material.CHEST) {
            block.setType(Material.AIR);
        }

        // Remove from tracking
        graveLocations.remove(grave.getLocation());
        graves.remove(grave.getGraveId());

        // Save
        saveGraves();
    }

    private void cleanupExcessGraves(UUID playerId) {
        List<Grave> playerGraves = getPlayerGraves(playerId);
        while (playerGraves.size() >= maxGravesPerPlayer) {
            // Remove oldest grave
            Grave oldest = playerGraves.stream()
                    .min(Comparator.comparingLong(Grave::getCreatedAt))
                    .orElse(null);
            if (oldest != null) {
                removeGrave(oldest);
                playerGraves.remove(oldest);
            } else {
                break;
            }
        }
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<Grave> expired = new ArrayList<>();
            for (Grave grave : graves.values()) {
                if (grave.isExpired() || grave.isLooted()) {
                    expired.add(grave);
                }
            }
            for (Grave grave : expired) {
                removeGrave(grave);
            }
            if (!expired.isEmpty()) {
                plugin.getLogger().info("Cleaned up " + expired.size() + " expired/looted graves");
            }
        }, 20 * 60, 20 * 60); // Every minute
    }

    private void loadGraves() {
        if (!dataFile.exists())
            return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : data.getKeys(false)) {
            try {
                UUID graveId = UUID.fromString(key);
                UUID ownerId = UUID.fromString(data.getString(key + ".owner-id"));
                String ownerName = data.getString(key + ".owner-name");
                String worldName = data.getString(key + ".world");
                World world = Bukkit.getWorld(worldName);
                if (world == null)
                    continue;

                int x = data.getInt(key + ".x");
                int y = data.getInt(key + ".y");
                int z = data.getInt(key + ".z");
                Location location = new Location(world, x, y, z);

                @SuppressWarnings("unchecked")
                List<ItemStack> items = (List<ItemStack>) data.getList(key + ".items", new ArrayList<>());
                int xp = data.getInt(key + ".xp");
                long expiresAt = data.getLong(key + ".expires-at");

                Grave grave = new Grave(graveId, ownerId, ownerName, location, items, xp, expiresAt);
                graves.put(graveId, grave);
                graveLocations.put(location, graveId);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load grave " + key + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + graves.size() + " graves");
    }

    private void saveGraves() {
        YamlConfiguration data = new YamlConfiguration();
        for (Grave grave : graves.values()) {
            String key = grave.getGraveId().toString();
            data.set(key + ".owner-id", grave.getOwnerId().toString());
            data.set(key + ".owner-name", grave.getOwnerName());
            data.set(key + ".world", grave.getLocation().getWorld().getName());
            data.set(key + ".x", grave.getLocation().getBlockX());
            data.set(key + ".y", grave.getLocation().getBlockY());
            data.set(key + ".z", grave.getLocation().getBlockZ());
            data.set(key + ".items", grave.getItems());
            data.set(key + ".xp", grave.getExperience());
            data.set(key + ".expires-at", grave.getExpiresAt());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save graves data", e);
        }
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        saveGraves();
    }

    public int getGraveCount() {
        return graves.size();
    }
}
