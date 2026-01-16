package network.vonix.vonixcore.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous Random Teleport Manager.
 * Handles chunk loading off the main thread to prevent server freezes.
 */
public class AsyncRtpManager {

    private static final Random RANDOM = new Random();
    private static final int MAX_ATTEMPTS = 50;

    private static int getMinDistance() {
        return EssentialsConfig.getInstance().getRtpMinRange();
    }

    private static int getMaxDistance() {
        return EssentialsConfig.getInstance().getRtpMaxRange();
    }

    /**
     * Initiates an async RTP for the given player.
     * This method returns immediately and handles teleportation via callbacks.
     */
    public static void randomTeleport(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();

        player.sendSystemMessage(Component.literal("§eSearching for a safe location..."));

        // Start the async search
        findSafeLocationAsync(level, center, 0).thenAccept(safePos -> {
            // This runs on the async thread, schedule teleport on main thread
            player.getServer().execute(() -> {
                if (safePos != null) {
                    player.teleportTo(level, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                            player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.literal(String.format(
                            "§aTeleported to §eX: %d, Y: %d, Z: %d",
                            safePos.getX(), safePos.getY(), safePos.getZ())));
                } else {
                    player.sendSystemMessage(Component.literal(
                            "§cCould not find a safe location after " + MAX_ATTEMPTS + " attempts!"));
                }
            });
        }).exceptionally(ex -> {
            VonixCore.LOGGER.error("[RTP] Error during async RTP", ex);
            player.getServer().execute(() -> {
                player.sendSystemMessage(Component.literal("§cAn error occurred during RTP."));
            });
            return null;
        });
    }

    /**
     * Recursively finds a safe location asynchronously.
     */
    private static CompletableFuture<BlockPos> findSafeLocationAsync(ServerLevel level, BlockPos center, int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return CompletableFuture.completedFuture(null);
        }

        // Generate random coordinates
        double angle = RANDOM.nextDouble() * 2 * Math.PI;
        int minDist = getMinDistance();
        int maxDist = getMaxDistance();
        int dist = minDist + RANDOM.nextInt(Math.max(1, maxDist - minDist));
        int x = center.getX() + (int) (Math.cos(angle) * dist);
        int z = center.getZ() + (int) (Math.sin(angle) * dist);

        ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);

        // Load chunk asynchronously
        return loadChunkAsync(level, chunkPos).thenCompose(chunk -> {
            // Once chunk is loaded, check for safe spot on main thread
            CompletableFuture<BlockPos> result = new CompletableFuture<>();
            
            level.getServer().execute(() -> {
                BlockPos safePos = findSafeY(level, x, z);
                if (safePos != null && isSafeSpot(level, safePos)) {
                    result.complete(safePos);
                } else {
                    // Try next attempt
                    findSafeLocationAsync(level, center, attempt + 1)
                            .thenAccept(result::complete)
                            .exceptionally(ex -> {
                                result.completeExceptionally(ex);
                                return null;
                            });
                }
            });
            
            return result;
        });
    }

    /**
     * Loads a chunk asynchronously using the chunk source.
     */
    private static CompletableFuture<ChunkAccess> loadChunkAsync(ServerLevel level, ChunkPos pos) {
        return CompletableFuture.supplyAsync(() -> {
            // This blocks on the async thread, not main thread
            return level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
        }, net.minecraft.Util.backgroundExecutor());
    }

    /**
     * Finds a safe Y coordinate at the given X/Z.
     */
    private static BlockPos findSafeY(ServerLevel level, int x, int z) {
        boolean isNether = level.dimension() == Level.NETHER;
        boolean isEnd = level.dimension() == Level.END;

        if (isNether) {
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
        } else if (isEnd) {
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
        } else {
            // Overworld: use heightmap
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
        }
        return null;
    }

    /**
     * Checks if a position is safe (comprehensive checks).
     */
    private static boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockPos below2 = pos.below(2);

        // Must have at least 2 solid blocks below (prevents spawning on single floating blocks)
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

        // Check for dangerous blocks below
        var belowState = level.getBlockState(below);
        if (belowState.is(Blocks.LAVA) ||
                belowState.is(Blocks.MAGMA_BLOCK) ||
                belowState.is(Blocks.CACTUS) ||
                belowState.is(Blocks.FIRE) ||
                belowState.is(Blocks.SOUL_FIRE) ||
                belowState.is(Blocks.CAMPFIRE) ||
                belowState.is(Blocks.SOUL_CAMPFIRE) ||
                belowState.is(Blocks.SWEET_BERRY_BUSH) ||
                belowState.is(Blocks.WITHER_ROSE) ||
                belowState.is(Blocks.WATER) ||
                belowState.is(Blocks.POINTED_DRIPSTONE) ||
                belowState.is(Blocks.SCAFFOLDING) ||
                belowState.is(Blocks.COBWEB) ||
                belowState.is(Blocks.HONEY_BLOCK) ||
                belowState.is(Blocks.SLIME_BLOCK) ||
                belowState.is(Blocks.TNT)) {
            return false;
        }

        // Check for powder snow at player position
        if (level.getBlockState(pos).is(Blocks.POWDER_SNOW) ||
                level.getBlockState(pos.above()).is(Blocks.POWDER_SNOW)) {
            return false;
        }

        // Check for nearby lava (3 block radius)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    var state = level.getBlockState(checkPos);
                    if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
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
