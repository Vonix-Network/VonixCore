package network.vonix.vonixcore.claims;

import network.vonix.vonixcore.VonixCore;

import java.util.UUID;

/**
 * Represents a land claim.
 */
public class Claim {
    private final int id;
    private final UUID ownerUuid;
    private final String world;
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final long createdAt;

    public Claim(int id, UUID ownerUuid, String world, int minX, int minZ, int maxX, int maxZ, long createdAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getLength() {
        return maxZ - minZ + 1;
    }

    public int getArea() {
        return getWidth() * getLength();
    }

    public boolean contains(String checkWorld, int x, int z) {
        return world.equals(checkWorld) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(Claim other) {
        if (!world.equals(other.world))
            return false;
        return minX <= other.maxX && maxX >= other.minX && minZ <= other.maxZ && maxZ >= other.minZ;
    }
}
