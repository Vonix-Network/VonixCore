package network.vonix.vonixcore.shops.chest;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a chest shop in the world.
 * Based on QuickShop-Hikari's Shop interface with VonixCore optimizations.
 */
public class ChestShop {

    private long id;
    private UUID ownerUuid;
    private String ownerName;
    private String world;
    private int x;
    private int y;
    private int z;
    private String itemType;
    private String itemData; // NBT/meta data as JSON
    private double price;
    private ShopType shopType;
    private boolean isAdmin;
    private int stock;
    private long createdAt;
    private long lastTransaction;
    private boolean unlimited;
    private String shopName;
    private double taxRate;
    private UUID taxAccount; // For custom tax recipient

    // Staff members who can manage this shop
    private Map<UUID, StaffPermission> staff = new HashMap<>();

    // Transient fields (not stored in DB)
    private transient ItemStack cachedItem;
    private transient Location cachedLocation;
    private transient boolean dirty = false;

    public ChestShop() {
        this.createdAt = System.currentTimeMillis();
        this.lastTransaction = 0;
        this.shopType = ShopType.SELLING;
        this.isAdmin = false;
        this.unlimited = false;
        this.stock = 0;
    }

    /**
     * Create a new chest shop
     */
    public static ChestShop create(UUID owner, String ownerName, Location location,
            ItemStack item, double price, ShopType type) {
        ChestShop shop = new ChestShop();
        shop.ownerUuid = owner;
        shop.ownerName = ownerName;
        shop.world = location.getWorld().getName();
        shop.x = location.getBlockX();
        shop.y = location.getBlockY();
        shop.z = location.getBlockZ();
        shop.itemType = item.getType().name();
        shop.itemData = serializeItemMeta(item);
        shop.price = price;
        shop.shopType = type;
        shop.cachedItem = item.clone();
        shop.cachedLocation = location;
        return shop;
    }

    /**
     * Serialize item meta to JSON string
     */
    private static String serializeItemMeta(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        // Simple serialization - can be enhanced for complex items
        try {
            Map<String, Object> serialized = item.serialize();
            // Convert to JSON-like string (basic implementation)
            StringBuilder sb = new StringBuilder("{");
            for (Map.Entry<String, Object> entry : serialized.entrySet()) {
                if (sb.length() > 1)
                    sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":\"")
                        .append(entry.getValue().toString().replace("\"", "\\\"")).append("\"");
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // === Shop Operations ===

    /**
     * Check if shop has enough stock
     */
    public boolean hasStock(int amount) {
        if (unlimited || isAdmin)
            return true;
        return stock >= amount;
    }

    /**
     * Check if shop has space for more items
     */
    public boolean hasSpace(int amount) {
        if (unlimited || isAdmin)
            return true;
        // Check chest inventory (to be implemented with actual chest)
        return true;
    }

    /**
     * Add stock to shop
     */
    public void addStock(int amount) {
        if (!unlimited && !isAdmin) {
            this.stock += amount;
            this.dirty = true;
        }
    }

    /**
     * Remove stock from shop
     */
    public void removeStock(int amount) {
        if (!unlimited && !isAdmin) {
            this.stock = Math.max(0, this.stock - amount);
            this.dirty = true;
        }
    }

    /**
     * Record a transaction
     */
    public void recordTransaction() {
        this.lastTransaction = System.currentTimeMillis();
        this.dirty = true;
    }

    /**
     * Check if player is owner
     */
    public boolean isOwner(UUID player) {
        return ownerUuid.equals(player);
    }

    /**
     * Check if player is staff or owner
     */
    public boolean hasAccess(UUID player) {
        if (isOwner(player))
            return true;
        return staff.containsKey(player);
    }

    /**
     * Check if player can manage shop (owner or staff with MANAGER permission)
     */
    public boolean canManage(UUID player) {
        if (isOwner(player))
            return true;
        StaffPermission perm = staff.get(player);
        return perm == StaffPermission.MANAGER;
    }

    /**
     * Add staff member
     */
    public void addStaff(UUID player, StaffPermission permission) {
        staff.put(player, permission);
        dirty = true;
    }

    /**
     * Remove staff member
     */
    public void removeStaff(UUID player) {
        staff.remove(player);
        dirty = true;
    }

    /**
     * Get shop location
     */
    public Location getLocation() {
        if (cachedLocation == null) {
            org.bukkit.World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
            if (bukkitWorld != null) {
                cachedLocation = new Location(bukkitWorld, x, y, z);
            }
        }
        return cachedLocation;
    }

    /**
     * Calculate price with tax
     */
    public double getPriceWithTax() {
        return price * (1 + taxRate);
    }

    /**
     * Calculate tax amount
     */
    public double getTaxAmount() {
        return price * taxRate;
    }

    // === Getters and Setters ===

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

    public String getItemData() {
        return itemData;
    }

    public void setItemData(String itemData) {
        this.itemData = itemData;
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

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastTransaction() {
        return lastTransaction;
    }

    public void setLastTransaction(long lastTransaction) {
        this.lastTransaction = lastTransaction;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
        this.dirty = true;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
        this.dirty = true;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public UUID getTaxAccount() {
        return taxAccount;
    }

    public void setTaxAccount(UUID taxAccount) {
        this.taxAccount = taxAccount;
    }

    public Map<UUID, StaffPermission> getStaff() {
        return staff;
    }

    public void setStaff(Map<UUID, StaffPermission> staff) {
        this.staff = staff;
    }

    public ItemStack getCachedItem() {
        return cachedItem;
    }

    public void setCachedItem(ItemStack cachedItem) {
        this.cachedItem = cachedItem;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Shop type enum
     */
    public enum ShopType {
        SELLING, // Shop sells items to players (players buy)
        BUYING, // Shop buys items from players (players sell)
        DUAL // Both buy and sell
    }

    /**
     * Staff permission levels
     */
    public enum StaffPermission {
        VIEWER, // Can view shop info
        TRADER, // Can trade with shop as if owner
        MANAGER // Can modify shop settings
    }
}
