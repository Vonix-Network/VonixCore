package network.vonix.vonixcore.graves;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player grave containing their items and XP.
 */
public class Grave {
    private final UUID graveId;
    private final UUID ownerId;
    private final String ownerName;
    private final String world;
    private final int x, y, z;
    private final List<ItemStack> items;
    private final int experience;
    private final long expiresAt;
    private final long createdAt;
    private boolean looted;

    public Grave(UUID graveId, UUID ownerId, String ownerName, String world,
            int x, int y, int z, List<ItemStack> items, int experience, long expiresAt) {
        this.graveId = graveId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.items = new ArrayList<>(items);
        this.experience = experience;
        this.expiresAt = expiresAt;
        this.createdAt = System.currentTimeMillis();
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

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getExperience() {
        return experience;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isLooted() {
        return looted;
    }

    public void setLooted(boolean looted) {
        this.looted = looted;
    }

    public boolean isOwner(UUID uuid) {
        return ownerId.equals(uuid);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("graveId", graveId);
        tag.putUUID("ownerId", ownerId);
        tag.putString("ownerName", ownerName);
        tag.putString("world", world);
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
        tag.putInt("xp", experience);
        tag.putLong("expiresAt", expiresAt);
        tag.putLong("createdAt", createdAt);
        tag.putBoolean("looted", looted);

        ListTag itemList = new ListTag();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                itemList.add(item.save(new CompoundTag()));
            }
        }
        tag.put("items", itemList);

        return tag;
    }

    public static Grave fromNbt(CompoundTag tag) {
        try {
            UUID graveId = tag.getUUID("graveId");
            UUID ownerId = tag.getUUID("ownerId");
            String ownerName = tag.getString("ownerName");
            String world = tag.getString("world");
            int x = tag.getInt("x");
            int y = tag.getInt("y");
            int z = tag.getInt("z");
            int xp = tag.getInt("xp");
            long expiresAt = tag.getLong("expiresAt");

            List<ItemStack> items = new ArrayList<>();
            ListTag itemList = tag.getList("items", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemList.size(); i++) {
                ItemStack item = ItemStack.of(itemList.getCompound(i));
                if (!item.isEmpty())
                    items.add(item);
            }

            Grave grave = new Grave(graveId, ownerId, ownerName, world, x, y, z, items, xp, expiresAt);
            grave.looted = tag.getBoolean("looted");
            return grave;
        } catch (Exception e) {
            return null;
        }
    }
}
