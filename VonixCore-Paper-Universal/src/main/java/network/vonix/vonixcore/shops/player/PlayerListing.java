package network.vonix.vonixcore.shops.player;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a player listing on the market.
 */
public class PlayerListing {

    private long id;
    private UUID sellerUuid;
    private String sellerName;
    private String itemType;
    private String itemData; // Serialized item meta
    private int quantity;
    private double priceEach;
    private long createdAt;
    private long expiresAt;
    private int sold;
    private boolean collected;

    // Transient
    private transient ItemStack cachedItem;

    public PlayerListing() {
        this.createdAt = System.currentTimeMillis();
        this.sold = 0;
        this.collected = false;
    }

    public static PlayerListing create(UUID seller, String sellerName, ItemStack item,
            double priceEach, long expiresAt) {
        PlayerListing listing = new PlayerListing();
        listing.sellerUuid = seller;
        listing.sellerName = sellerName;
        listing.itemType = item.getType().name();
        listing.quantity = item.getAmount();
        listing.priceEach = priceEach;
        listing.expiresAt = expiresAt;
        listing.cachedItem = item.clone();

        // Serialize item data for special items
        if (item.hasItemMeta()) {
            try {
                listing.itemData = serializeItemData(item);
            } catch (Exception e) {
                listing.itemData = null;
            }
        }

        return listing;
    }

    private static String serializeItemData(ItemStack item) {
        // Simple serialization - can be enhanced for NBT data
        if (!item.hasItemMeta())
            return null;
        // For now, just store display name and enchantments as JSON-like string
        StringBuilder sb = new StringBuilder();
        if (item.getItemMeta().hasDisplayName()) {
            // Store display name info
            sb.append("name:");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isSoldOut() {
        return sold >= quantity;
    }

    public int getRemaining() {
        return quantity - sold;
    }

    public double getTotalValue() {
        return priceEach * quantity;
    }

    public double getEarnings() {
        return priceEach * sold;
    }

    public ItemStack getItem() {
        if (cachedItem == null) {
            Material mat = Material.getMaterial(itemType);
            if (mat != null) {
                cachedItem = new ItemStack(mat, quantity);
                // Apply item data if present
            }
        }
        return cachedItem != null ? cachedItem.clone() : null;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public void setSellerUuid(UUID sellerUuid) {
        this.sellerUuid = sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceEach() {
        return priceEach;
    }

    public void setPriceEach(double priceEach) {
        this.priceEach = priceEach;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getSold() {
        return sold;
    }

    public void setSold(int sold) {
        this.sold = sold;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }
}
