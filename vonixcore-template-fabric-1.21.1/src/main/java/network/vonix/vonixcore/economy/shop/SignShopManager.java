package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages sign-based shops.
 * Fabric 1.20.1 compatible version.
 */
public class SignShopManager {

    private static SignShopManager instance;

    // Pattern for price parsing
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$?([0-9]+\\.?[0-9]*)");

    // Common item name mappings
    private static final Map<String, String> ITEM_ALIASES = new HashMap<>();

    static {
        ITEM_ALIASES.put("diamond", "minecraft:diamond");
        ITEM_ALIASES.put("diamonds", "minecraft:diamond");
        ITEM_ALIASES.put("iron", "minecraft:iron_ingot");
        ITEM_ALIASES.put("iron ingot", "minecraft:iron_ingot");
        ITEM_ALIASES.put("gold", "minecraft:gold_ingot");
        ITEM_ALIASES.put("gold ingot", "minecraft:gold_ingot");
        ITEM_ALIASES.put("coal", "minecraft:coal");
        ITEM_ALIASES.put("emerald", "minecraft:emerald");
        ITEM_ALIASES.put("emeralds", "minecraft:emerald");
        ITEM_ALIASES.put("netherite", "minecraft:netherite_ingot");
        ITEM_ALIASES.put("redstone", "minecraft:redstone");
        ITEM_ALIASES.put("lapis", "minecraft:lapis_lazuli");
        ITEM_ALIASES.put("cobblestone", "minecraft:cobblestone");
        ITEM_ALIASES.put("cobble", "minecraft:cobblestone");
        ITEM_ALIASES.put("stone", "minecraft:stone");
        ITEM_ALIASES.put("wood", "minecraft:oak_log");
        ITEM_ALIASES.put("oak", "minecraft:oak_log");
        ITEM_ALIASES.put("wheat", "minecraft:wheat");
        ITEM_ALIASES.put("bread", "minecraft:bread");
    }

    public static SignShopManager getInstance() {
        if (instance == null) {
            instance = new SignShopManager();
        }
        return instance;
    }

    /**
     * Validate and process a new sign shop creation
     */
    public SignShopResult validateSignShop(ServerPlayer player, SignBlockEntity sign, SignText text) {
        String line1 = getPlainText(text, 0).toLowerCase().trim();
        String line2 = getPlainText(text, 1).trim();
        String line3 = getPlainText(text, 2).trim();
        String line4 = getPlainText(text, 3).trim();

        boolean isBuySign = line1.equals("[buy]");
        boolean isSellSign = line1.equals("[sell]");

        if (!isBuySign && !isSellSign) {
            return null;
        }

        // Parse quantity
        int quantity;
        try {
            quantity = Integer.parseInt(line2);
            if (quantity <= 0 || quantity > 64) {
                return new SignShopResult(false, "Invalid quantity! Use 1-64.");
            }
        } catch (NumberFormatException e) {
            return new SignShopResult(false, "Line 2 must be a number (quantity).");
        }

        // Parse item
        String itemId = resolveItemId(line3);
        if (itemId == null) {
            return new SignShopResult(false, "Unknown item: " + line3);
        }

        // Parse price
        double price;
        Matcher matcher = PRICE_PATTERN.matcher(line4);
        if (matcher.find()) {
            try {
                price = Double.parseDouble(matcher.group(1));
                if (price <= 0) {
                    return new SignShopResult(false, "Price must be positive!");
                }
            } catch (NumberFormatException e) {
                return new SignShopResult(false, "Invalid price format.");
            }
        } else {
            return new SignShopResult(false, "Line 4 must be a price (e.g., $100).");
        }

        return new SignShopResult(true, null, isBuySign, quantity, itemId, price);
    }

    /**
     * Handle interaction with a sign shop
     */
    public void handleInteraction(ServerPlayer player, BlockPos signPos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(signPos);

        if (!(state.getBlock() instanceof SignBlock)) {
            return;
        }

        var blockEntity = level.getBlockEntity(signPos);
        if (!(blockEntity instanceof SignBlockEntity sign)) {
            return;
        }

        SignText text = sign.getFrontText();
        SignShopResult result = validateSignShop(player, sign, text);

        if (result == null || !result.valid) {
            return;
        }

        String symbol = EssentialsConfig.getInstance().getCurrencySymbol();
        EconomyManager eco = EconomyManager.getInstance();

        if (result.isBuySign) {
            double totalPrice = result.price * result.quantity;
            
            eco.getBalance(player.getUUID()).thenAccept(balance -> {
                if (balance < totalPrice) {
                    player.sendSystemMessage(
                            Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", totalPrice)));
                    return;
                }

                eco.withdraw(player.getUUID(), totalPrice).thenAccept(success -> {
                    if (success) {
                        player.getServer().execute(() -> {
                            var leftover = ItemUtils.giveItems(player, result.itemId, result.quantity);
                            if (!leftover.isEmpty()) {
                                player.drop(leftover, false);
                            }
                            player.sendSystemMessage(Component.literal("§aPurchased " + result.quantity + "x " + result.itemId
                                    + " for " + symbol + String.format("%.2f", totalPrice)));
                        });
                    }
                });
            });

        } else {
            int playerHas = ItemUtils.countItems(player, result.itemId);

            if (playerHas < result.quantity) {
                player.sendSystemMessage(Component
                        .literal("§cYou don't have enough items! Need " + result.quantity + ", have " + playerHas));
                return;
            }

            double totalPrice = result.price * result.quantity;

            if (ItemUtils.removeItems(player, result.itemId, result.quantity)) {
                eco.deposit(player.getUUID(), totalPrice).thenAccept(v -> {
                    player.sendSystemMessage(Component.literal("§aSold " + result.quantity + "x " + result.itemId + " for "
                            + symbol + String.format("%.2f", totalPrice)));
                });
            }
        }
    }

    private String resolveItemId(String input) {
        String lower = input.toLowerCase().trim();

        if (ITEM_ALIASES.containsKey(lower)) {
            return ITEM_ALIASES.get(lower);
        }

        if (lower.contains(":")) {
            var stack = ItemUtils.createItemFromId(lower);
            if (!stack.isEmpty()) {
                return lower;
            }
        }

        String withNamespace = "minecraft:" + lower.replace(" ", "_");
        var stack = ItemUtils.createItemFromId(withNamespace);
        if (!stack.isEmpty()) {
            return withNamespace;
        }

        return null;
    }

    private String getPlainText(SignText text, int line) {
        if (line < 0 || line >= 4)
            return "";
        Component[] messages = text.getMessages(false);
        if (line < messages.length) {
            return messages[line].getString();
        }
        return "";
    }

    public static class SignShopResult {
        public final boolean valid;
        public final String error;
        public final boolean isBuySign;
        public final int quantity;
        public final String itemId;
        public final double price;

        public SignShopResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
            this.isBuySign = false;
            this.quantity = 0;
            this.itemId = null;
            this.price = 0;
        }

        public SignShopResult(boolean valid, String error, boolean isBuySign, int quantity, String itemId,
                double price) {
            this.valid = valid;
            this.error = error;
            this.isBuySign = isBuySign;
            this.quantity = quantity;
            this.itemId = itemId;
            this.price = price;
        }
    }
}
