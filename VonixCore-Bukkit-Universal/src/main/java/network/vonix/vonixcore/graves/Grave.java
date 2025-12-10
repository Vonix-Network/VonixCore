package network.vonix.vonixcore.graves;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Represents a player grave containing their items and XP on death
 */
public class Grave {
    private final UUID graveId;
    private final UUID ownerId;
    private final String ownerName;
    private final Location location;
    private final List<ItemStack> items;
    private final int experience;
    private final long createdAt;
    private final long expiresAt;
    private boolean looted;

    public Grave(UUID graveId, UUID ownerId, String ownerName, Location location,
            List<ItemStack> items, int experience, long expiresAt) {
        this.graveId = graveId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.items = items;
        this.experience = experience;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = expiresAt;
        this.looted = false;
    }

    public UUID getGraveId() {
        return graveId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getExperience() {
        return experience;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isLooted() {
        return looted;
    }

    public void setLooted(boolean looted) {
        this.looted = looted;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isOwner(Player player) {
        return ownerId.equals(player.getUniqueId());
    }

    public long getTimeRemaining() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public String getTimeRemainingFormatted() {
        long remaining = getTimeRemaining();
        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
