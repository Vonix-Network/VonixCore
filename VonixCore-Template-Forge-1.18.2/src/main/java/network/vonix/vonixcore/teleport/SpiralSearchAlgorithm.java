package network.vonix.vonixcore.teleport;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import network.vonix.vonixcore.config.EssentialsConfig;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optimized Spiral Search for 1.18.2
 */
public class SpiralSearchAlgorithm {

    /**
     * Generates a list of chunk positions in a spiral pattern around a random center.
     */
    public static List<ChunkPos> generateSearchCandidates(ServerLevel level, int maxCandidates) {
        List<ChunkPos> candidates = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        WorldBorder border = level.getWorldBorder();

        // 1. Pick a random center point within the RTP range
        int minRadius = EssentialsConfig.getInstance().CONFIG.rtpMinRange.get();
        int maxRadius = EssentialsConfig.getInstance().CONFIG.rtpMaxRange.get();
        
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = minRadius + (random.nextDouble() * (maxRadius - minRadius));
        
        int centerX = (int) (Math.cos(angle) * distance);
        int centerZ = (int) (Math.sin(angle) * distance);

        // 2. Generate spiral around this center
        ChunkPos centerChunk = new ChunkPos(centerX >> 4, centerZ >> 4);
        int radius = 0;
        int maxSpiralRadius = 5; // Search up to 5 chunks outwards from center

        while (candidates.size() < maxCandidates && radius < maxSpiralRadius) {
            List<ChunkPos> ring = new ArrayList<>();
            
            // Simple expanding ring logic
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) {
                        ChunkPos pos = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                        
                        // World Border Check (Fast, Thread-Safe)
                        if (border.isWithinBounds(pos)) {
                            ring.add(pos);
                        }
                    }
                }
            }
            
            Collections.shuffle(ring, random); // Randomize check order within ring
            candidates.addAll(ring);
            radius++;
        }

        return candidates;
    }

    /**
     * Checks if a biome is allowed.
     */
    public static boolean isBiomeAllowed(Holder<Biome> biomeHolder) {
        List<? extends String> blocked = EssentialsConfig.getInstance().CONFIG.rtpBlockedBiomes.get();
        if (blocked.isEmpty()) return true;

        try {
            ResourceLocation key = biomeHolder.unwrapKey().map(k -> k.location()).orElse(null);
            if (key != null) {
                return !blocked.contains(key.toString());
            }
        } catch (Exception ignored) {}
        return true;
    }
}
