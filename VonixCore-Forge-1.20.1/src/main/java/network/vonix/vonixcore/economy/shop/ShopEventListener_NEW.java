package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;

/**
 * Event listener for shop interactions (chest shops, sign shops, GUI cleanup)
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class ShopEventListener {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!EssentialsConfig.CONFIG.shopsEnabled.get())
            return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // Check for sign shop interaction
        if (state.is(BlockTags.STANDING_SIGNS) || state.is(BlockTags.WALL_SIGNS)) {
            SignShopManager.getInstance().handleInteraction(player, pos);
            // Don't cancel - let sign open for editing if needed
            return;
        }

        // Check for chest shop interaction
        if (state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL)) {
            String world = level.dimension().location().toString();
            ShopManager.ChestShop shop = ShopManager.getInstance().getShopAt(world, pos);

            if (shop != null) {
                event.setCanceled(true); // Prevent normal chest opening

                if (shop.owner().equals(player.getUUID())) {
                    // Owner clicked - show management
                    handleOwnerClick(player, pos, shop);
                } else {
                    // Customer clicked - process transaction
                    handleCustomerClick(player, pos, shop);
                }
            }
        }

        // Check if player is creating a chest shop
        if (ShopManager.getInstance().isCreatingShop(player.getUUID())) {
            if (state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL)) {
                handleShopCreation(player, pos, state);
                event.setCanceled(true);
            }
        }
    }

    /**
     * Handle owner clicking their shop
     */
    private static void handleOwnerClick(ServerPlayer player, BlockPos pos, ShopManager.ChestShop shop) {
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

        player.sendSystemMessage(Component.literal("§6=== Your Shop ==="));
        player.sendSystemMessage(Component.literal("§7Item: §f" + shop.itemId()));
        player.sendSystemMessage(Component.literal("§7Stock: §f" + shop.stock()));
        if (shop.buyPrice() != null) {
            player.sendSystemMessage(
                    Component.literal("§7Buy Price: §a" + symbol + String.format("%.2f", shop.buyPrice())));
        }
        if (shop.sellPrice() != null) {
            player.sendSystemMessage(
                    Component.literal("§7Sell Price: §c" + symbol + String.format("%.2f", shop.sellPrice())));
        }
        player.sendSystemMessage(Component.literal("§7Use §e/chestshop remove §7to delete this shop."));
    }

    /**
     * Handle customer clicking a shop
     */
    private static void handleCustomerClick(ServerPlayer player, BlockPos pos, ShopManager.ChestShop shop) {
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        EconomyManager eco = EconomyManager.getInstance();

        // For now, left-click = buy, sneak-click = sell
        boolean isSneaking = player.isShiftKeyDown();

        if (!isSneaking && shop.buyPrice() != null && shop.buyPrice() > 0) {
            // Buy one item
            if (shop.stock() <= 0) {
                player.sendSystemMessage(Component.literal("§cThis shop is out of stock!"));
                return;
            }

            double price = shop.buyPrice();
            double balance = eco.getBalance(player.getUUID());

            if (balance < price) {
                player.sendSystemMessage(
                        Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", price)));
                return;
            }

            // Process buy
            if (eco.withdraw(player.getUUID(), price)) {
                eco.deposit(shop.owner(), price);
                var leftover = ItemUtils.giveItems(player, shop.itemId(), 1);
                if (!leftover.isEmpty()) {
                    player.drop(leftover, false);
                }
                // TODO: Decrease stock in database
                player.sendSystemMessage(Component
                        .literal("§aPurchased 1x " + shop.itemId() + " for " + symbol + String.format("%.2f", price)));
            }

        } else if (isSneaking && shop.sellPrice() != null && shop.sellPrice() > 0) {
            // Sell one item to the shop
            int playerHas = ItemUtils.countItems(player, shop.itemId());

            if (playerHas < 1) {
                player.sendSystemMessage(Component.literal("§cYou don't have any " + shop.itemId() + " to sell!"));
                return;
            }

            double price = shop.sellPrice();

            // Check if shop owner can afford
            double ownerBalance = eco.getBalance(shop.owner());
            if (ownerBalance < price) {
                player.sendSystemMessage(Component.literal("§cThe shop owner doesn't have enough money!"));
                return;
            }

            // Process sell
            if (ItemUtils.removeItems(player, shop.itemId(), 1)) {
                eco.withdraw(shop.owner(), price);
                eco.deposit(player.getUUID(), price);
                // TODO: Increase stock in database
                player.sendSystemMessage(Component
                        .literal("§aSold 1x " + shop.itemId() + " for " + symbol + String.format("%.2f", price)));
            }
        } else {
            // Show shop info
            player.sendSystemMessage(Component.literal("§6=== Shop ==="));
            player.sendSystemMessage(Component.literal("§7Item: §f" + shop.itemId()));
            player.sendSystemMessage(Component.literal("§7Stock: §f" + shop.stock()));
            if (shop.buyPrice() != null) {
                player.sendSystemMessage(
                        Component.literal("§aClick to buy for " + symbol + String.format("%.2f", shop.buyPrice())));
            }
            if (shop.sellPrice() != null) {
                player.sendSystemMessage(Component
                        .literal("§cSneak-click to sell for " + symbol + String.format("%.2f", shop.sellPrice())));
            }
        }
    }

    /**
     * Handle shop creation mode - reads item from chest inventory
     */
    private static void handleShopCreation(ServerPlayer player, BlockPos pos, BlockState state) {
        ShopManager.ShopCreationState creation = ShopManager.getInstance().getCreationState(player.getUUID());

        if (creation == null)
            return;

        // First step - select chest and read inventory
        if (creation.step == 0) {
            // Verify it's not already a shop
            String world = player.level().dimension().location().toString();
            if (ShopManager.getInstance().getShopAt(world, pos) != null) {
                player.sendSystemMessage(Component.literal("§cThis chest is already a shop!"));
                ShopManager.getInstance().cancelShopCreation(player.getUUID());
                return;
            }

            // Read chest inventory to detect item type
            if (player.level().getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                String itemId = null;

                // Find first non-empty slot
                for (int i = 0; i < chest.getContainerSize(); i++) {
                    ItemStack stack = chest.getItem(i);
                    if (!stack.isEmpty()) {
                        itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        break; // Only need first item
                    }
                }

                if (itemId == null) {
                    player.sendSystemMessage(Component.literal("§cThe chest is empty! Put items in the chest first."));
                    ShopManager.getInstance().cancelShopCreation(player.getUUID());
                    return;
                }

                // Store detected item
                creation.chestPos = pos;
                creation.itemId = itemId;
                creation.step = 1;

                player.sendSystemMessage(Component.literal("§aChest shop creation started!"));
                player.sendSystemMessage(Component.literal("§7Item detected: §f" + itemId));
                player.sendSystemMessage(Component.literal("§7Stock will be managed via chest inventory"));
                player.sendSystemMessage(Component.literal("§eNow type the price in chat:"));
                player.sendSystemMessage(Component.literal("§7Or type 'cancel' to cancel."));
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to read chest inventory!"));
                ShopManager.getInstance().cancelShopCreation(player.getUUID());
            }
        }
    }

    /**
     * Prevent breaking shop chests
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;
        if (!EssentialsConfig.CONFIG.shopsEnabled.get())
            return;

        BlockState state = event.getState();
        if (!(state.getBlock() instanceof ChestBlock) && !state.is(Blocks.BARREL)) {
            return;
        }

        String world = player.level().dimension().location().toString();
        BlockPos pos = event.getPos();
        ShopManager.ChestShop shop = ShopManager.getInstance().getShopAt(world, pos);

        if (shop != null) {
            // Only owner or admin can break
            if (!shop.owner().equals(player.getUUID()) && !player.hasPermissions(2)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cYou cannot break someone else's shop!"));
            } else {
                // Remove shop and display
                ShopManager.getInstance().deleteShop(world, pos);
                if (player.level() instanceof ServerLevel serverLevel) {
                    DisplayEntityManager.getInstance().removeDisplay(serverLevel, pos);
                }
                player.sendSystemMessage(Component.literal("§eShop removed."));
            }
        }
    }

    /**
     * Handle chat input during shop creation
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerChat(ServerChatEvent event) {
        if (!EssentialsConfig.CONFIG.shopsEnabled.get())
            return;

        ServerPlayer player = event.getPlayer();
        ShopManager.ShopCreationState creation = ShopManager.getInstance().getCreationState(player.getUUID());

        if (creation == null)
            return;

        String message = event.getRawText().trim();
        event.setCanceled(true); // Cancel the chat message

        // Allow cancel at any step
        if (message.equalsIgnoreCase("cancel")) {
            ShopManager.getInstance().cancelShopCreation(player.getUUID());
            player.sendSystemMessage(Component.literal("§cShop creation cancelled."));
            return;
        }

        try {
            // Only step 1: Price input (item already detected from chest)
            if (creation.step == 1) {
                double price = Double.parseDouble(message);
                if (price < 0) {
                    player.sendSystemMessage(Component.literal("§cPrice cannot be negative!"));
                    return;
                }
                creation.price = price;

                // Create the shop (stock is managed via chest inventory, pass 0 to DB)
                boolean success = ShopManager.getInstance().createChestShop(
                        player, creation.chestPos, creation.itemId, creation.price, 0);

                if (success) {
                    player.sendSystemMessage(Component.literal("§a§l✓ §7Chest shop created successfully!"));
                    // TODO: Re-enable display entity when method signature is fixed
                    // DisplayEntityManager display creation
                } else {
                    player.sendSystemMessage(Component.literal("§cFailed to create shop!"));
                }

                ShopManager.getInstance().cancelShopCreation(player.getUUID());
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cInvalid price! Please enter a number."));
        }
    }

    /**
     * Clean up shop sessions when menu closes
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShopGUIManager.getInstance().onPlayerCloseMenu(player.getUUID());
        }
    }
}
