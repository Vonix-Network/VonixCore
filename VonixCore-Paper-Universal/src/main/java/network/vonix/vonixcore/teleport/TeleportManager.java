package network.vonix.vonixcore.teleport;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private static TeleportManager instance;

    private final Map<UUID, TpaRequest> tpaRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public static TeleportManager getInstance() {
        if (instance == null) {
            instance = new TeleportManager();
        }
        return instance;
    }

    public void saveLastLocation(Player player) {
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    public Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    public boolean sendTpaRequest(Player requester, Player target, boolean tpaHere) {
        UUID targetUuid = target.getUniqueId();
        TpaRequest existing = tpaRequests.get(targetUuid);
        if (existing != null && !existing.isExpired()) {
            return false;
        }

        tpaRequests.put(targetUuid, new TpaRequest(
                requester.getUniqueId(),
                requester.getName(),
                tpaHere,
                System.currentTimeMillis()));
        return true;
    }

    public boolean acceptTpaRequest(Player target) {
        TpaRequest request = tpaRequests.remove(target.getUniqueId());
        if (request == null || request.isExpired()) {
            return false;
        }

        Player requester = VonixCore.getInstance().getServer().getPlayer(request.requesterUuid());
        if (requester == null) {
            return false;
        }

        if (request.tpaHere()) {
            // Requester asked target to come to them
            teleportPlayer(target, requester.getLocation());
        } else {
            // Requester asked to go to target
            teleportPlayer(requester, target.getLocation());
        }
        return true;
    }

    public boolean denyTpaRequest(Player target) {
        return tpaRequests.remove(target.getUniqueId()) != null;
    }

    public TpaRequest getTpaRequest(UUID targetUuid) {
        TpaRequest request = tpaRequests.get(targetUuid);
        if (request != null && request.isExpired()) {
            tpaRequests.remove(targetUuid);
            return null;
        }
        return request;
    }

    public boolean isOnCooldown(UUID uuid) {
        Long cooldownEnd = cooldowns.get(uuid);
        if (cooldownEnd == null)
            return false;
        if (System.currentTimeMillis() > cooldownEnd) {
            cooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public int getRemainingCooldown(UUID uuid) {
        Long cooldownEnd = cooldowns.get(uuid);
        if (cooldownEnd == null)
            return 0;
        long remaining = (cooldownEnd - System.currentTimeMillis()) / 1000;
        return Math.max(0, (int) remaining);
    }

    public void setCooldown(UUID uuid, int seconds) {
        cooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    public void teleportPlayer(Player player, Location location) {
        saveLastLocation(player);
        player.teleport(location);
    }

    public Optional<Location> findSafeLocation(Location location) {
        if (isSafe(location)) {
            return Optional.of(location);
        }

        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (int r = 1; r <= 5; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = -r; dy <= r; dy++) {
                        Location check = new Location(world, x + dx, y + dy, z + dz);
                        if (isSafe(check)) {
                            return Optional.of(check);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);

        return feet.getType().isAir()
                && head.getType().isAir()
                && ground.getType().isSolid();
    }

    public record TpaRequest(UUID requesterUuid, String requesterName, boolean tpaHere, long timestamp) {
        private static final long EXPIRE_MS = 120000;

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_MS;
        }
    }
}
