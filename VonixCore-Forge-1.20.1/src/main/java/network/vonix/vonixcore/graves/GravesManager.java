package network.vonix.vonixcore.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import network.vonix.vonixcore.VonixCore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages player graves for death item recovery.
 */
public class GravesManager {
    private static GravesManager INSTANCE;

    private final Map<UUID, Grave> graves = new ConcurrentHashMap<>();
    private final Map<String, UUID> graveLocations = new ConcurrentHashMap<>();
    private final Path dataFile;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VonixCore-Graves");
        t.setDaemon(true);
        return t;
    });

    // Configuration
    public static boolean enabled = true;
    public static int expirationTime = 3600;
    public static double xpRetention = 0.8;
    public static boolean protectionEnabled = true;
    public static int protectionTime = 300;
    public static int maxGravesPerPlayer = 5;

    public GravesManager(Path configDir) {
        INSTANCE = this;
        this.dataFile = configDir.resolve("graves-data.dat");
        loadGraves();
        startCleanupTask();
    }

    public static GravesManager getInstance() {
        return INSTANCE;
    }

    public Grave createGrave(ServerPlayer player, List<ItemStack> items, int totalXp) {
        if (!enabled || items.isEmpty())
            return null;

        ServerLevel level = player.serverLevel();
        BlockPos pos = findSafeLocation(level, player.blockPosition());
        if (pos == null) {
            VonixCore.LOGGER.warn("[Graves] Could not find safe location for " + player.getName().getString());
            return null;
        }

        int storedXp = (int) (totalXp * xpRetention);
        long expiresAt = System.currentTimeMillis() + (expirationTime * 1000L);

        UUID graveId = UUID.randomUUID();
        String world = level.dimension().location().toString();
        Grave grave = new Grave(graveId, player.getUUID(), player.getName().getString(),
                world, pos.getX(), pos.getY(), pos.getZ(), items, storedXp, expiresAt);

        cleanupExcessGraves(player.getUUID());

        graves.put(graveId, grave);
        graveLocations.put(locationKey(world, pos), graveId);

        placeGraveBlock(level, pos, grave);
        saveGravesAsync();

        VonixCore.LOGGER.info("[Graves] Created grave for " + player.getName().getString() +
                " at " + world + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());

        return grave;
    }

    private BlockPos findSafeLocation(ServerLevel level, BlockPos origin) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();

        if (isValidLocation(level, x, y, z))
            return new BlockPos(x, y, z);

        for (int dy = 1; dy <= 10; dy++) {
            if (y + dy < level.getMaxBuildHeight() && isValidLocation(level, x, y + dy, z)) {
                return new BlockPos(x, y + dy, z);
            }
        }

        for (int dy = 1; dy <= 10; dy++) {
            if (y - dy >= level.getMinBuildHeight() && isValidLocation(level, x, y - dy, z)) {
                return new BlockPos(x, y - dy, z);
            }
        }

        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        if (isValidLocation(level, x, surface + 1, z)) {
            return new BlockPos(x, surface + 1, z);
        }

        return null;
    }

    private boolean isValidLocation(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());

        return state.isAir() && below.isSolidRender(level, pos.below());
    }

    private void placeGraveBlock(ServerLevel level, BlockPos pos, Grave grave) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChestBlockEntity chest) {
            int slot = 0;
            for (ItemStack item : grave.getItems()) {
                if (slot >= chest.getContainerSize())
                    break;
                chest.setItem(slot++, item.copy());
            }
        }
    }

    public Grave getGraveAt(String world, BlockPos pos) {
        UUID graveId = graveLocations.get(locationKey(world, pos));
        return graveId != null ? graves.get(graveId) : null;
    }

    public List<Grave> getPlayerGraves(UUID playerId) {
        List<Grave> result = new ArrayList<>();
        for (Grave g : graves.values()) {
            if (g.getOwnerId().equals(playerId))
                result.add(g);
        }
        return result;
    }

    public boolean canLoot(ServerPlayer player, Grave grave) {
        if (grave.isLooted())
            return false;
        if (grave.isExpired())
            return true;
        if (grave.isOwner(player.getUUID()))
            return true;
        if (player.hasPermissions(2))
            return true;

        if (protectionEnabled) {
            long protectionEndsAt = grave.getCreatedAt() + (protectionTime * 1000L);
            if (System.currentTimeMillis() < protectionEndsAt)
                return false;
        }
        return true;
    }

    public void lootGrave(ServerPlayer player, Grave grave, ServerLevel level) {
        if (grave.isLooted())
            return;

        if (grave.getExperience() > 0) {
            player.giveExperiencePoints(grave.getExperience());
        }

        grave.setLooted(true);
        removeGrave(grave, level);
        VonixCore.LOGGER.info("[Graves] " + player.getName().getString() + " looted grave of " + grave.getOwnerName());
    }

    public void removeGrave(Grave grave, ServerLevel level) {
        BlockPos pos = new BlockPos(grave.getX(), grave.getY(), grave.getZ());
        if (level.getBlockState(pos).is(Blocks.CHEST)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        graveLocations.remove(locationKey(grave.getWorld(), pos));
        graves.remove(grave.getGraveId());
        saveGravesAsync();
    }

    private void cleanupExcessGraves(UUID playerId) {
        List<Grave> playerGraves = getPlayerGraves(playerId);
        while (playerGraves.size() >= maxGravesPerPlayer) {
            Grave oldest = playerGraves.stream()
                    .min(Comparator.comparingLong(Grave::getCreatedAt))
                    .orElse(null);
            if (oldest != null) {
                graveLocations.remove(locationKey(oldest.getWorld(),
                        new BlockPos(oldest.getX(), oldest.getY(), oldest.getZ())));
                graves.remove(oldest.getGraveId());
                playerGraves.remove(oldest);
            } else
                break;
        }
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            List<Grave> expired = new ArrayList<>();
            for (Grave g : graves.values()) {
                if (g.isExpired() || g.isLooted())
                    expired.add(g);
            }
            for (Grave g : expired) {
                graveLocations.remove(locationKey(g.getWorld(), new BlockPos(g.getX(), g.getY(), g.getZ())));
                graves.remove(g.getGraveId());
            }
            if (!expired.isEmpty()) {
                VonixCore.LOGGER.info("[Graves] Cleaned up " + expired.size() + " expired graves");
                saveGravesAsync();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private String locationKey(String world, BlockPos pos) {
        return world + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    private void saveGravesAsync() {
        scheduler.execute(this::saveGraves);
    }

    private void saveGraves() {
        try {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (Grave g : graves.values()) {
                list.add(g.toNbt());
            }
            root.put("graves", list);

            try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(dataFile))) {
                NbtIo.write(root, dos);
            }
        } catch (IOException e) {
            VonixCore.LOGGER.error("[Graves] Failed to save: " + e.getMessage());
        }
    }

    private void loadGraves() {
        if (!Files.exists(dataFile))
            return;

        try (DataInputStream dis = new DataInputStream(Files.newInputStream(dataFile))) {
            CompoundTag root = NbtIo.read(dis);
            ListTag list = root.getList("graves", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                Grave g = Grave.fromNbt(list.getCompound(i));
                if (g != null) {
                    graves.put(g.getGraveId(), g);
                    graveLocations.put(locationKey(g.getWorld(),
                            new BlockPos(g.getX(), g.getY(), g.getZ())), g.getGraveId());
                }
            }
            VonixCore.LOGGER.info("[Graves] Loaded " + graves.size() + " graves");
        } catch (IOException e) {
            VonixCore.LOGGER.error("[Graves] Failed to load: " + e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        saveGraves();
    }

    public int getGraveCount() {
        return graves.size();
    }
}
