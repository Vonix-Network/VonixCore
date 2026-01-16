package network.vonix.vonixcore.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.VonixCore;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous Random Teleport Manager.
 * Optimized for Forge servers - uses proper thread-safe async chunk loading
 * with ticket-based pre-generation to minimize main thread impact.
 */
public class AsyncRtpManager {

    private static final int MAX_ATTEMPTS = 50;
    private static final int CHUNK_LOAD_TIMEOUT_SECONDS = 30;

    // Custom ticket type for RTP chunk loading - short lifetime since we teleport
    // immediately
    private static final TicketType<ChunkPos> RTP_TICKET = TicketType.create("vonixcore_rtp", (a, b) -> 0, 20 * 10);

    // Pre-computed set of dangerous blocks for O(1) lookup
    private static final Set<Block> DANGEROUS_GROUND_BLOCKS = Set.of(
            Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.CACTUS, Blocks.FIRE,
            Blocks.SOUL_FIRE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.SWEET_BERRY_BUSH, Blocks.WITHER_ROSE, Blocks.WATER,
            Blocks.POINTED_DRIPSTONE, Blocks.SCAFFOLDING, Blocks.COBWEB,
            Blocks.HONEY_BLOCK, Blocks.SLIME_BLOCK, Blocks.TNT);

    // Blocks that are dangerous even when nearby
    private static final Set<Block> NEARBY_DANGER_BLOCKS = Set.of(
            Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE);

    private static int getMinDistance() {
        return EssentialsConfig.CONFIG.rtpMinRange.get();
    }

    private static int getMaxDistance() {
        return EssentialsConfig.CONFIG.rtpMaxRange.get();
    }

    /**
     * Initiates an async RTP for the given player.
     * This method returns immediately and handles teleportation via callbacks.
     */
    public static void randomTeleport(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        player.sendSystemMessage(Component.literal("§eSearching for a safe location..."));

        AtomicInteger attemptCounter = new AtomicInteger(0);

        // Start iterative async search
        findSafeLocationIterative(level, center, player, attemptCounter)
                .orTimeout(CHUNK_LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenAccept(safePos -> {
                    // Schedule teleport on main thread
                    player.getServer().execute(() -> {
                        if (safePos != null) {
                            performTeleport(player, level, safePos, attemptCounter.get());
                        } else {
                            player.sendSystemMessage(Component.literal(
                                    "§cCould not find a safe location after " + MAX_ATTEMPTS + " attempts!"));
                        }
                    });
                })
                .exceptionally(ex -> {
                    handleRtpError(player, ex);
                    return null;
                });
    }

    /**
     * Performs the actual teleportation with proper ticket management.
     * Must be called on the main server thread.
     */
    private static void performTeleport(ServerPlayer player, ServerLevel level, BlockPos safePos, int attempts) {
        ChunkPos chunkPos = new ChunkPos(safePos);
        ServerChunkCache chunkSource = level.getChunkSource();

        // Add ticket to keep the chunk loaded during teleport
        chunkSource.addRegionTicket(RTP_TICKET, chunkPos, 2, chunkPos);

        try {
            player.teleportTo(level, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal(String.format(
                    "§aTeleported to §eX: %d, Y: %d, Z: %d §7(Attempts: %d)",
                    safePos.getX(), safePos.getY(), safePos.getZ(), attempts)));

            // Pre-load surrounding chunks for smooth player experience
            preloadSurroundingChunks(level, safePos);
        } finally {
            // Always remove the ticket, even if teleport fails
            chunkSource.removeRegionTicket(RTP_TICKET, chunkPos, 2, chunkPos);
        }
    }

    /**
     * Handles errors during RTP with proper exception type detection.
     */
    private static void handleRtpError(ServerPlayer player, Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        boolean isTimeout = cause instanceof TimeoutException;

        VonixCore.LOGGER.error("[RTP] Error during async RTP: {}", cause.getMessage());
        player.getServer().execute(() -> {
            if (isTimeout) {
                player.sendSystemMessage(Component.literal(
                        "§cRTP timed out - chunk generation took too long. Try again."));
            } else {
                player.sendSystemMessage(Component.literal("§cAn error occurred during RTP."));
            }
        });
    }

    /**
     * Iteratively finds a safe location asynchronously.
     * Uses an iterative approach to avoid stack growth from recursive
     * CompletableFuture chains.
     */
    private static CompletableFuture<BlockPos> findSafeLocationIterative(
            ServerLevel level, BlockPos center, ServerPlayer player, AtomicInteger attemptCounter) {

        CompletableFuture<BlockPos> result = new CompletableFuture<>();
        tryNextLocation(level, center, player, attemptCounter, result);
        return result;
    }

    /**
     * Tries the next random location. Completes the result future when a safe spot
     * is found
     * or max attempts are reached.
     */
    private static void tryNextLocation(
            ServerLevel level, BlockPos center, ServerPlayer player,
            AtomicInteger attemptCounter, CompletableFuture<BlockPos> result) {

        int attempt = attemptCounter.incrementAndGet();
        if (attempt > MAX_ATTEMPTS) {
            result.complete(null);
            return;
        }

        // Generate random coordinates using thread-safe random
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;
        int minDist = getMinDistance();
        int maxDist = getMaxDistance();
        int dist = minDist + random.nextInt(Math.max(1, maxDist - minDist));
        int x = center.getX() + (int) (Math.cos(angle) * dist);
        int z = center.getZ() + (int) (Math.sin(angle) * dist);

        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);

        // Load chunk asynchronously using Forge's proper async chunk system
        loadChunkAsync(level, chunkPos).thenAccept(chunk -> {
            if (chunk == null) {
                // Chunk load failed, try next location
                tryNextLocation(level, center, player, attemptCounter, result);
                return;
            }

            // Safety check must run on main thread (block state access)
            level.getServer().execute(() -> {
                try {
                    BlockPos safePos = findSafeY(level, x, z);
                    if (safePos != null && isSafeSpot(level, safePos)) {
                        result.complete(safePos);
                    } else {
                        // Try next location - chain continues iteratively
                        tryNextLocation(level, center, player, attemptCounter, result);
                    }
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        }).exceptionally(ex -> {
            result.completeExceptionally(ex);
            return null;
        });
    }

    /**
     * Loads a chunk asynchronously using Forge's thread-safe chunk future system.
     * All ticket operations are scheduled on the main thread for safety.
     */
    private static CompletableFuture<ChunkAccess> loadChunkAsync(ServerLevel level, ChunkPos pos) {
        CompletableFuture<ChunkAccess> future = new CompletableFuture<>();

        // Schedule on main thread - ticket operations must be thread-safe
        level.getServer().execute(() -> {
            try {
                ServerChunkCache chunkSource = level.getChunkSource();

                // Add temporary ticket to request chunk generation
                chunkSource.addRegionTicket(RTP_TICKET, pos, 0, pos);

                // Use Forge's async chunk loading - returns a future that completes when chunk
                // is ready
                chunkSource.getChunkFuture(pos.x, pos.z, ChunkStatus.FULL, true)
                        .thenAccept(either -> {
                            // Remove ticket after chunk loads (schedule on main thread)
                            level.getServer().execute(() -> {
                                chunkSource.removeRegionTicket(RTP_TICKET, pos, 0, pos);
                            });

                            ChunkAccess chunk = either.left().orElse(null);
                            future.complete(chunk);
                        })
                        .exceptionally(ex -> {
                            // Ensure ticket is removed on failure
                            level.getServer().execute(() -> {
                                chunkSource.removeRegionTicket(RTP_TICKET, pos, 0, pos);
                            });
                            VonixCore.LOGGER.warn("[RTP] Failed to load chunk at {}: {}", pos, ex.getMessage());
                            future.complete(null);
                            return null;
                        });
            } catch (Exception e) {
                VonixCore.LOGGER.warn("[RTP] Failed to initiate chunk load at {}: {}", pos, e.getMessage());
                future.complete(null);
            }
        });

        return future;
    }

    /**
     * Pre-loads surrounding chunks in a small radius for smooth player experience.
     * This queues chunk loads but doesn't wait for them.
     */
    private static void preloadSurroundingChunks(ServerLevel level, BlockPos center) {
        ChunkPos centerChunk = new ChunkPos(center);
        ServerChunkCache chunkSource = level.getChunkSource();

        // Pre-load a 3x3 area around the destination
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0)
                    continue; // Skip center, already loaded

                ChunkPos neighborPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                // Add short-lived ticket to queue these chunks for loading
                chunkSource.addRegionTicket(RTP_TICKET, neighborPos, 0, neighborPos);
            }
        }
    }

    /**
     * Finds a safe Y coordinate at the given X/Z.
     */
    private static BlockPos findSafeY(ServerLevel level, int x, int z) {
        boolean isNether = level.dimension() == Level.NETHER;
        boolean isEnd = level.dimension() == Level.END;

        if (isNether) {
            return findNetherSafeY(level, x, z);
        } else if (isEnd) {
            return findEndSafeY(level, x, z);
        } else {
            return findOverworldSafeY(level, x, z);
        }
    }

    private static BlockPos findNetherSafeY(ServerLevel level, int x, int z) {
        // Nether: search for cave openings
        for (int y = 32; y <= 120; y++) {
            BlockPos check = new BlockPos(x, y, z);
            if (level.getBlockState(check).isSolidRender(level, check)) {
                BlockPos spawn = check.above();
                if (level.getBlockState(spawn).isAir() && level.getBlockState(spawn.above()).isAir()) {
                    return spawn;
                }
            }
        }
        return null;
    }

    private static BlockPos findEndSafeY(ServerLevel level, int x, int z) {
        // End: search from low to high
        for (int y = 50; y <= 120; y++) {
            BlockPos check = new BlockPos(x, y, z);
            if (level.getBlockState(check).isSolidRender(level, check)) {
                BlockPos spawn = check.above();
                if (level.getBlockState(spawn).isAir() && level.getBlockState(spawn.above()).isAir()) {
                    return spawn;
                }
            }
        }
        return null;
    }

    private static BlockPos findOverworldSafeY(ServerLevel level, int x, int z) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

        if (surface <= level.getMinBuildHeight()) {
            // Void biome or similar, search manually
            for (int y = 64; y <= 256; y++) {
                BlockPos check = new BlockPos(x, y, z);
                if (level.getBlockState(check).isSolidRender(level, check)) {
                    BlockPos spawn = check.above();
                    if (level.getBlockState(spawn).isAir() && level.getBlockState(spawn.above()).isAir()) {
                        return spawn;
                    }
                }
            }
            return null;
        }

        // Check around surface level
        int startY = Math.max(level.getMinBuildHeight() + 1, surface - 10);
        int endY = Math.min(level.getMaxBuildHeight() - 2, surface + 10);

        for (int y = startY; y <= endY; y++) {
            BlockPos check = new BlockPos(x, y, z);
            if (level.getBlockState(check).isSolidRender(level, check)) {
                BlockPos spawn = check.above();
                if (level.getBlockState(spawn).isAir() && level.getBlockState(spawn.above()).isAir()) {
                    return spawn;
                }
            }
        }
        return null;
    }

    /**
     * Checks if a position is safe (comprehensive checks).
     */
    private static boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockPos below2 = pos.below(2);

        // Must have at least 2 solid blocks below
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }
        if (!level.getBlockState(below2).isSolidRender(level, below2)) {
            return false;
        }

        // Must have air for player (2 blocks tall)
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Check for air pockets below (could drop player into cave)
        int airBlocksBelow = 0;
        for (int i = 3; i <= 7; i++) {
            if (level.getBlockState(pos.below(i)).isAir()) {
                airBlocksBelow++;
            } else {
                break;
            }
        }
        if (airBlocksBelow >= 4) {
            return false; // Too much air below, likely a cave
        }

        // Check for dangerous blocks below using O(1) set lookup
        Block belowBlock = level.getBlockState(below).getBlock();
        if (DANGEROUS_GROUND_BLOCKS.contains(belowBlock)) {
            return false;
        }

        // Check for powder snow at player position
        if (level.getBlockState(pos).is(Blocks.POWDER_SNOW) ||
                level.getBlockState(pos.above()).is(Blocks.POWDER_SNOW)) {
            return false;
        }

        // Check for nearby dangerous blocks (reduced radius: 5x3x5 = 75 blocks vs
        // previous 245)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    Block block = level.getBlockState(checkPos).getBlock();
                    if (NEARBY_DANGER_BLOCKS.contains(block)) {
                        return false;
                    }
                }
            }
        }

        // End specific: avoid void and end portal area
        if (level.dimension() == Level.END) {
            if (pos.getY() < 50) {
                return false;
            }
            // Avoid main end island spawn area (0,0)
            double distFromCenter = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
            if (distFromCenter < 100) {
                return false;
            }
        }

        // Check world border
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        return true;
    }
}
