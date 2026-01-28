package network.vonix.vonixcore.teleport;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimized Asynchronous Random Teleport Manager for Bukkit/Paper.
 * Uses Paper's async chunk loading API for performance.
 */
public class AsyncRtpManager {

    private static final int MAX_ATTEMPTS = 50;
    
    // Dangerous blocks
    private static final Set<Material> DANGEROUS_BLOCKS = Set.of(
            Material.LAVA, Material.MAGMA_BLOCK, Material.CACTUS, Material.FIRE,
            Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.WATER,
            Material.POINTED_DRIPSTONE, Material.COBWEB, Material.TNT
    );

    // Queue system
    private static final Queue<RtpRequest> requestQueue = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> pendingPlayers = new HashSet<>();
    private static final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private record RtpRequest(Player player, World world, Location center, long timestamp) {}

    public static void rtp(Player player) {
        if (pendingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an RTP queued!", NamedTextColor.RED));
            return;
        }

        pendingPlayers.add(player.getUniqueId());
        requestQueue.offer(new RtpRequest(player, player.getWorld(), player.getLocation(), System.currentTimeMillis()));

        int pos = requestQueue.size();
        if (pos == 1) {
            player.sendMessage(Component.text("Searching for a safe location...", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Queued for RTP. Position: #" + pos, NamedTextColor.GOLD));
        }

        startProcessor();
    }

    private static void startProcessor() {
        if (isProcessing.compareAndSet(false, true)) {
            processNext();
        }
    }

    private static void processNext() {
        RtpRequest request = requestQueue.poll();
        if (request == null) {
            isProcessing.set(false);
            return;
        }

        if (!request.player.isOnline()) {
            pendingPlayers.remove(request.player.getUniqueId());
            processNext();
            return;
        }

        processRequest(request).thenRun(() -> {
            pendingPlayers.remove(request.player.getUniqueId());
            processNext(); // Process next in queue
        });
    }

    private static CompletableFuture<Void> processRequest(RtpRequest request) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        int min = EssentialsConfig.rtpMinRange;
        int max = EssentialsConfig.rtpMaxRange;

        findSafeLocation(request.player, request.world, request.center, min, max, 1)
                .thenAccept(loc -> {
                    if (loc != null) {
                        Bukkit.getScheduler().runTask(VonixCore.getInstance(), () -> {
                            if (request.player.isOnline()) {
                                TeleportManager.getInstance().saveLastLocation(request.player);
                                request.player.teleport(loc);
                                request.player.sendMessage(Component.text("Teleported to safe location!", NamedTextColor.GREEN));
                            }
                            future.complete(null);
                        });
                    } else {
                        request.player.sendMessage(Component.text("Could not find a safe location.", NamedTextColor.RED));
                        future.complete(null);
                    }
                });

        return future;
    }

    private static CompletableFuture<Location> findSafeLocation(Player player, World world, Location center, int min, int max, int attempt) {
        if (attempt > MAX_ATTEMPTS) {
            return CompletableFuture.completedFuture(null);
        }

        if (attempt % 10 == 0) {
            player.sendMessage(Component.text("Searching... (" + attempt + "/" + MAX_ATTEMPTS + ")", NamedTextColor.GRAY));
        }

        // Generate random coords
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = Math.sqrt(random.nextDouble() * (max * max - min * min) + min * min);
        int x = center.getBlockX() + (int) (dist * Math.cos(angle));
        int z = center.getBlockZ() + (int) (dist * Math.sin(angle));

        // Load chunk async
        return world.getChunkAtAsync(x >> 4, z >> 4).thenCompose(chunk -> {
            // Check safety on main thread (Bukkit API limitation for block checks unless using snapshot)
            // But we can use snapshot for async check
            ChunkSnapshot snapshot = chunk.getChunkSnapshot();
            
            return CompletableFuture.supplyAsync(() -> findSafeY(snapshot, world, x, z), 
                    ex -> Bukkit.getScheduler().runTaskAsynchronously(VonixCore.getInstance(), ex));
        }).thenCompose(safeLoc -> {
            if (safeLoc != null) {
                return CompletableFuture.completedFuture(safeLoc);
            } else {
                return findSafeLocation(player, world, center, min, max, attempt + 1);
            }
        });
    }

    private static Location findSafeY(ChunkSnapshot snapshot, World world, int x, int z) {
        int localX = x & 15;
        int localZ = z & 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        // Start from top solid block
        int highestY = snapshot.getHighestBlockYAt(localX, localZ);
        if (highestY <= minY) return null;

        // Check downward from highest Y
        for (int y = Math.min(highestY, maxY - 2); y > minY + 1; y--) {
            Material ground = snapshot.getBlockType(localX, y - 1, localZ);
            Material feet = snapshot.getBlockType(localX, y, localZ);
            Material head = snapshot.getBlockType(localX, y + 1, localZ);

            if (ground.isSolid() && !DANGEROUS_BLOCKS.contains(ground) &&
                feet.isAir() && head.isAir()) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }
}
