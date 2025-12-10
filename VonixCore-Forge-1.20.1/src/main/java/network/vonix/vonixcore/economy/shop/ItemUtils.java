package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;

import java.util.UUID;

/**
 * Utility class for item operations in the economy system.
 * Forge 1.20.1 compatible version.
 */
public class ItemUtils {

    /**
     * Create an ItemStack from a registry ID string (e.g., "minecraft:diamond")
     */
    public static ItemStack createItemFromId(String itemId) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(itemId);
            if (location != null && BuiltInRegistries.ITEM.containsKey(location)) {
                Item item = BuiltInRegistries.ITEM.get(location);
                return new ItemStack(item);
            }
        } catch (Exception e) {
            VonixCore.LOGGER.warn("[Shop] Failed to create item from ID '{}': {}", itemId, e.getMessage());
        }
        return ItemStack.EMPTY;
    }

    /**
     * Get the registry ID of an ItemStack
     */
    public static String getItemId(ItemStack stack) {
        if (stack.isEmpty())
            return "minecraft:air";
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null ? key.toString() : "minecraft:air";
    }

    /**
     * Serialize an ItemStack to NBT string for database storage
     */
    public static String serializeItemStack(ItemStack stack) {
        if (stack.isEmpty())
            return "";
        try {
            // Simple format: itemId#count
            return getItemId(stack) + "#" + stack.getCount();
        } catch (Exception e) {
            VonixCore.LOGGER.warn("[Shop] Failed to serialize ItemStack: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Deserialize an ItemStack from NBT string
     */
    public static ItemStack deserializeItemStack(String nbtString) {
        if (nbtString == null || nbtString.isEmpty())
            return ItemStack.EMPTY;
        try {
            // Simple format: "itemId#count"
            if (nbtString.contains("#")) {
                String[] parts = nbtString.split("#");
                ItemStack stack = createItemFromId(parts[0]);
                if (!stack.isEmpty() && parts.length > 1) {
                    stack.setCount(Integer.parseInt(parts[1]));
                }
                return stack;
            }
            // Fallback: try as just item ID
            return createItemFromId(nbtString);
        } catch (Exception e) {
            VonixCore.LOGGER.warn("[Shop] Failed to deserialize ItemStack: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * Add price lore to an item display (Forge 1.20.1 uses NBT directly)
     */
    public static void addPriceLore(ItemStack stack, Double buyPrice, Double sellPrice) {
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();

        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        if (buyPrice != null && buyPrice > 0) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal("§a§lBUY: §f" + symbol + String.format("%.2f", buyPrice)))));
        }
        if (sellPrice != null && sellPrice > 0) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(
                    Component.literal("§c§lSELL: §f" + symbol + String.format("%.2f", sellPrice)))));
        }
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Left-click to buy"))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Right-click to sell"))));

        display.put("Lore", lore);
    }

    /**
     * Add listing lore to an item display
     */
    public static void addListingLore(ItemStack stack, double price, UUID seller) {
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();

        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal("§6§lPrice: §f" + symbol + String.format("%.2f", price)))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(
                Component.literal("§7Seller: §e" + seller.toString().substring(0, 8) + "..."))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§aClick to purchase"))));

        display.put("Lore", lore);
    }

    /**
     * Count how many of a specific item the player has
     */
    public static int countItems(ServerPlayer player, String itemId) {
        Inventory inventory = player.getInventory();
        int count = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && getItemId(stack).equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Count items matching an ItemStack (including NBT)
     */
    public static int countMatchingItems(ServerPlayer player, ItemStack target) {
        Inventory inventory = player.getInventory();
        int count = 0;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Remove items from player inventory
     * 
     * @return true if successfully removed the requested amount
     */
    public static boolean removeItems(ServerPlayer player, String itemId, int amount) {
        if (countItems(player, itemId) < amount) {
            return false;
        }

        Inventory inventory = player.getInventory();
        int remaining = amount;

        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && getItemId(stack).equals(itemId)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;

                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }

        return remaining == 0;
    }

    /**
     * Give items to a player
     * 
     * @return items that couldn't fit (empty if all fit)
     */
    public static ItemStack giveItems(ServerPlayer player, String itemId, int amount) {
        ItemStack toGive = createItemFromId(itemId);
        if (toGive.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int maxStack = toGive.getMaxStackSize();
        Inventory inventory = player.getInventory();

        while (amount > 0) {
            int giveAmount = Math.min(amount, maxStack);
            ItemStack stack = toGive.copy();
            stack.setCount(giveAmount);

            if (!inventory.add(stack)) {
                // Couldn't add to inventory, drop the rest
                ItemStack remaining = toGive.copy();
                remaining.setCount(amount);
                return remaining;
            }

            amount -= giveAmount;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Give an ItemStack to a player
     */
    public static ItemStack giveItemStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack.copy())) {
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
