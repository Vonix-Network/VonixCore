package network.vonix.vonixcore.shops.sign;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents a sign-based shop.
 * Simpler than chest shops - just a sign with buy/sell functionality.
 */
public class SignShop {

    private long id;
    private UUID ownerUuid;
    private String ownerName;
    private String world;
    private int x;
    private int y;
    private int z;
    private String itemType;
    private int quantity;
    private double price;
    private ShopType shopType;
    private boolean isAdmin;
    private long createdAt;

    // Transient
    private transient Location cachedLocation;
    private transient boolean dirty = false;

    public SignShop() {
        this.createdAt = System.currentTimeMillis();
        this.quantity = 1;
    }

    public static SignShop create(UUID owner, String ownerName, Location location,
            String itemType, int quantity, double price, ShopType type) {
        SignShop shop = new SignShop();
        shop.ownerUuid = owner;
        shop.ownerName = ownerName;
        shop.world = location.getWorld().getName();
        shop.x = location.getBlockX();
        shop.y = location.getBlockY();
        shop.z = location.getBlockZ();
        shop.itemType = itemType;
        shop.quantity = quantity;
        shop.price = price;
        shop.shopType = type;
        return shop;
    }

    public Location getLocation() {
        if (cachedLocation == null) {
            org.bukkit.World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
            if (bukkitWorld != null) {
                cachedLocation = new Location(bukkitWorld, x, y, z);
            }
        }
        return cachedLocation;
    }

    public boolean isOwner(UUID player) {
        return ownerUuid.equals(player);
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        this.dirty = true;
    }

    public ShopType getShopType() {
        return shopType;
    }

    public void setShopType(ShopType shopType) {
        this.shopType = shopType;
        this.dirty = true;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
        this.dirty = true;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public enum ShopType {
        BUY, // Players can buy from this sign
        SELL // Players can sell to this sign
    }
}
