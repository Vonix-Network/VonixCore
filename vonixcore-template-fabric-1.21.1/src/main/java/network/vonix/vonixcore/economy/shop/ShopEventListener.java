package network.vonix.vonixcore.economy.shop;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;

/**
 * Event listener for shop interactions (chest shops, sign shops, GUI cleanup)
 * Fabric 1.20.1 compatible version.
 */
public class ShopEventListener {

    public static void register() {
        // Chunk load event for respawning holograms
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (level.isClientSide())
                return;
            if (!EssentialsConfig.getInstance().isShopsEnabled())
                return;

            // Skip if mod or database not yet initialized (happens during world generation)
            if (VonixCore.getInstance() == null || VonixCore.getInstance().getDatabase() == null)
                return;

            // Schedule on next tick to avoid issues during chunk load
            level.getServer().execute(() -> {
                // Double-check database is still available
                if (VonixCore.getInstance() == null || VonixCore.getInstance().getDatabase() == null)
                    return;
                DisplayEntityManager.getInstance().respawnDisplaysInChunk(
                        level, chunk.getPos().x, chunk.getPos().z);
            });
        });
        // Right-click block events
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide())
                return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer))
                return InteractionResult.PASS;
            if (!EssentialsConfig.getInstance().isShopsEnabled())
                return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND)
                return InteractionResult.PASS;

            ServerLevel level = (ServerLevel) world;
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);

            // Check for sign shop interaction
            if (state.is(BlockTags.STANDING_SIGNS) || state.is(BlockTags.WALL_SIGNS)) {
                SignShopManager.getInstance().handleInteraction(serverPlayer, pos);
                // Don't cancel - let sign open for editing if needed
                return InteractionResult.PASS;
            }

            // Check for chest shop interaction
            if (state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL)) {
                String worldName = level.dimension().location().toString();
                ShopManager.ChestShop shop = ShopManager.getInstance().getShopAt(worldName, pos);

                if (shop != null) {
                    if (shop.owner().equals(serverPlayer.getUUID())) {
                        // Owner clicked - allow chest access for restocking (don't cancel)
                        // Just show a reminder on first click (non-sneaking)
                        if (!serverPlayer.isShiftKeyDown()) {
                            handleOwnerClick(serverPlayer, pos, shop);
                        }
                        // Let the chest open normally for restocking
                        return InteractionResult.PASS;
                    } else {
                        // Customer clicked - process transaction
                        handleCustomerClick(serverPlayer, pos, shop);
                        return InteractionResult.SUCCESS; // Prevent chest opening for non-owners
                    }
                }
            }

            // Check if player is creating a chest shop
            if (ShopManager.getInstance().isCreatingShop(serverPlayer.getUUID())) {
                if (state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL)) {
                    handleShopCreation(serverPlayer, pos, state);
                    return InteractionResult.SUCCESS;
                }
            }

            return InteractionResult.PASS;
        });

        // Chat input for shop creation - handled in EssentialsEventHandler
        // ALLOW_CHAT_MESSAGE
        // We register a secondary handler for shop creation chat input
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (!EssentialsConfig.getInstance().isShopsEnabled())
                return true;

            ShopManager.ShopCreationState creation = ShopManager.getInstance().getCreationState(sender.getUUID());
            if (creation == null)
                return true;

            String messageText = message.signedContent().trim().toLowerCase();

            // Allow cancel at any step
            if (messageText.equals("cancel")) {
                ShopManager.getInstance().cancelShopCreation(sender.getUUID());
                sender.sendSystemMessage(Component.literal("§cShop creation cancelled."));
                return false;
            }

            try {
                // Step 1: Buy price input
                if (creation.step == 1) {
                    Double buyPrice = null;
                    if (!messageText.equals("skip") && !messageText.equals("0")) {
                        buyPrice = Double.parseDouble(messageText);
                        if (buyPrice < 0) {
                            sender.sendSystemMessage(Component.literal("§cPrice cannot be negative!"));
                            return false;
                        }
                        if (buyPrice == 0)
                            buyPrice = null;
                    }
                    creation.buyPrice = buyPrice;
                    creation.step = 2;

                    sender.sendSystemMessage(Component.literal("§a§l✓ §7Buy price set: "
                            + (buyPrice != null ? "§a$" + String.format("%.2f", buyPrice) : "§cDisabled")));
                    sender.sendSystemMessage(Component.literal(""));
                    sender.sendSystemMessage(Component
                            .literal("§eEnter the §cSELL §eprice in chat §7(price you pay players for their items):"));
                    sender.sendSystemMessage(Component.literal("§7Type §c0 §7or §cskip §7to disable selling."));
                    sender.sendSystemMessage(Component.literal("§7Type §ccancel §7to cancel."));
                    return false;
                }

                // Step 2: Sell price input - then finalize shop
                if (creation.step == 2) {
                    Double sellPrice = null;
                    if (!messageText.equals("skip") && !messageText.equals("0")) {
                        sellPrice = Double.parseDouble(messageText);
                        if (sellPrice < 0) {
                            sender.sendSystemMessage(Component.literal("§cPrice cannot be negative!"));
                            return false;
                        }
                        if (sellPrice == 0)
                            sellPrice = null;
                    }
                    creation.sellPrice = sellPrice;

                    // Validate that at least one price is set
                    if (creation.buyPrice == null && creation.sellPrice == null) {
                        sender.sendSystemMessage(Component.literal("§cYou must set at least a buy or sell price!"));
                        creation.step = 1;
                        sender.sendSystemMessage(Component.literal("§eEnter the §aBUY §eprice in chat:"));
                        return false;
                    }

                    // Create the shop
                    boolean success = ShopManager.getInstance().createChestShop(
                            sender, creation.chestPos, creation.itemId, creation.buyPrice, creation.sellPrice, 0);

                    if (success) {
                        sender.sendSystemMessage(Component.literal("§a§l✓ Chest shop created successfully!"));

                        // Create sign on the front of the chest
                        if (sender.level() instanceof ServerLevel serverLevel) {
                            createShopSign(serverLevel, sender, creation);

                            // Create item display hologram above chest
                            ItemStack displayItem = ItemUtils.createItemFromId(creation.itemId);
                            if (!displayItem.isEmpty()) {
                                DisplayEntityManager.getInstance().spawnDisplay(serverLevel, creation.chestPos,
                                        displayItem);
                            }
                        }

                        String symbol = EssentialsConfig.getInstance().getCurrencySymbol();
                        if (creation.buyPrice != null) {
                            sender.sendSystemMessage(
                                    Component.literal("§7Buy: §a" + symbol + String.format("%.2f", creation.buyPrice)));
                        }
                        if (creation.sellPrice != null) {
                            sender.sendSystemMessage(
                                    Component.literal(
                                            "§7Sell: §c" + symbol + String.format("%.2f", creation.sellPrice)));
                        }
                    } else {
                        sender.sendSystemMessage(Component.literal("§cFailed to create shop!"));
                    }

                    ShopManager.getInstance().cancelShopCreation(sender.getUUID());
                    return false;
                }
            } catch (NumberFormatException e) {
                sender.sendSystemMessage(Component.literal("§cInvalid price! Please enter a number."));
                return false;
            }

            return true;
        });
    }

    /**
     * Handle owner clicking their shop - shows stats reminder
     */
    private static void handleOwnerClick(ServerPlayer player, BlockPos pos, ShopManager.ChestShop shop) {
        String symbol = EssentialsConfig.getInstance().getCurrencySymbol();

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
        player.sendSystemMessage(Component.literal("§7Sneak-click to open chest for restocking."));
        player.sendSystemMessage(Component.literal("§7Use §e/chestshop remove §7to delete this shop."));
    }

    /**
     * Handle customer clicking a shop
     */
    private static void handleCustomerClick(ServerPlayer player, BlockPos pos, ShopManager.ChestShop shop) {
        String symbol = EssentialsConfig.getInstance().getCurrencySymbol();
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
            
            eco.getBalance(player.getUUID()).thenAccept(balance -> {
                if (balance < price) {
                    player.sendSystemMessage(
                            Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", price)));
                    return;
                }

                // Process buy
                eco.withdraw(player.getUUID(), price).thenAccept(success -> {
                    if (success) {
                        eco.deposit(shop.owner(), price);
                        player.getServer().execute(() -> {
                            var leftover = ItemUtils.giveItems(player, shop.itemId(), 1);
                            if (!leftover.isEmpty()) {
                                player.drop(leftover, false);
                            }
                            // TODO: Decrease stock in database
                            player.sendSystemMessage(Component
                                    .literal("§aPurchased 1x " + shop.itemId() + " for " + symbol + String.format("%.2f", price)));
                        });
                    }
                });
            });

        } else if (isSneaking && shop.sellPrice() != null && shop.sellPrice() > 0) {
            // Sell one item to the shop
            int playerHas = ItemUtils.countItems(player, shop.itemId());

            if (playerHas < 1) {
                player.sendSystemMessage(Component.literal("§cYou don't have any " + shop.itemId() + " to sell!"));
                return;
            }

            double price = shop.sellPrice();

            // Check if shop owner can afford
            eco.getBalance(shop.owner()).thenAccept(ownerBalance -> {
                if (ownerBalance < price) {
                    player.sendSystemMessage(Component.literal("§cThe shop owner doesn't have enough money!"));
                    return;
                }

                // Process sell
                // Need to remove items on main thread before giving money? Or optimistically?
                // Better to remove items first on main thread, then process transaction.
                // But handleCustomerClick is running on server thread (main).
                // So we can remove items here.
                
                if (ItemUtils.removeItems(player, shop.itemId(), 1)) {
                    eco.withdraw(shop.owner(), price).thenAccept(success -> {
                        if (success) {
                            eco.deposit(player.getUUID(), price);
                            // TODO: Increase stock in database
                            player.sendSystemMessage(Component
                                    .literal("§aSold 1x " + shop.itemId() + " for " + symbol + String.format("%.2f", price)));
                        } else {
                            // Rollback items if withdraw failed (rare race condition)
                            player.getServer().execute(() -> {
                                ItemUtils.giveItems(player, shop.itemId(), 1);
                                player.sendSystemMessage(Component.literal("§cTransaction failed (Owner out of funds). Items returned."));
                            });
                        }
                    });
                }
            });
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

                player.sendSystemMessage(Component.literal("§a§l✓ §7Chest selected!"));
                player.sendSystemMessage(Component.literal("§7Item detected: §f" + itemId));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(
                        Component.literal("§eEnter the §aBUY §eprice in chat §7(price players pay to buy):"));
                player.sendSystemMessage(Component.literal("§7Type §c0 §7or §cskip §7to disable buying."));
                player.sendSystemMessage(Component.literal("§7Type §ccancel §7to cancel."));
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to read chest inventory!"));
                ShopManager.getInstance().cancelShopCreation(player.getUUID());
            }
        }
    }

    /**
     * Create a sign on the front of the chest shop
     */
    private static void createShopSign(ServerLevel level, ServerPlayer player, ShopManager.ShopCreationState creation) {
        BlockPos chestPos = creation.chestPos;
        BlockState chestState = level.getBlockState(chestPos);

        // Determine the front direction of the chest
        net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
        if (chestState.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            facing = chestState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        }

        // Place sign on the front of the chest
        BlockPos signPos = chestPos.relative(facing);

        // Check if the space is available (air or replaceable)
        if (!level.getBlockState(signPos).canBeReplaced()) {
            // Try placing on top if front is blocked
            signPos = chestPos.above();
            if (!level.getBlockState(signPos).canBeReplaced()) {
                player.sendSystemMessage(Component.literal("§7(No space for shop sign)"));
                return;
            }
        }

        // Place a wall sign or standing sign
        BlockState signState;
        if (signPos.equals(chestPos.above())) {
            // Standing sign on top
            signState = Blocks.OAK_SIGN.defaultBlockState();
        } else {
            // Wall sign on front
            signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.WallSignBlock.FACING, facing);
        }

        level.setBlock(signPos, signState, 3);

        // Set sign text
        if (level
                .getBlockEntity(signPos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity signEntity) {
            String symbol = EssentialsConfig.getInstance().getCurrencySymbol();
            String itemName = creation.itemId.contains(":") ? creation.itemId.split(":")[1] : creation.itemId;
            itemName = itemName.replace("_", " ");
            if (itemName.length() > 12)
                itemName = itemName.substring(0, 12);

            // Set sign text (front side)
            signEntity.setText(new net.minecraft.world.level.block.entity.SignText(
                    new Component[] {
                            Component.literal("§l[SHOP]"),
                            Component.literal("§f" + itemName),
                            Component.literal(creation.buyPrice != null
                                    ? "§aBuy: " + symbol + String.format("%.0f", creation.buyPrice)
                                    : ""),
                            Component.literal(creation.sellPrice != null
                                    ? "§cSell: " + symbol + String.format("%.0f", creation.sellPrice)
                                    : "")
                    },
                    new Component[] {
                            Component.literal("§l[SHOP]"),
                            Component.literal("§f" + itemName),
                            Component.literal(creation.buyPrice != null
                                    ? "§aBuy: " + symbol + String.format("%.0f", creation.buyPrice)
                                    : ""),
                            Component.literal(creation.sellPrice != null
                                    ? "§cSell: " + symbol + String.format("%.0f", creation.sellPrice)
                                    : "")
                    },
                    net.minecraft.world.item.DyeColor.BLACK,
                    false), true);
        }
    }
}
