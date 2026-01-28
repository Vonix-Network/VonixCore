package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for shop interactions (chest shops, sign shops, GUI cleanup)
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class ShopEventListener {

    // Track which players have a shop chest open for restocking: UUID -> (world,
    // pos, itemId)
    private static final Map<UUID, OpenShopChest> openShopChests = new ConcurrentHashMap<>();

    private record OpenShopChest(String world, BlockPos pos, String itemId) {
    }

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
            var future = ShopManager.getInstance().getShopAt(world, pos);
            
            if (future.isDone()) {
                // Check if shop exists in cache
                Optional<ShopManager.ChestShop> shopOpt = future.join();
                
                if (shopOpt.isPresent()) {
                    ShopManager.ChestShop shop = shopOpt.get();
                    if (shop.owner().equals(player.getUUID())) {
                        // Owner clicked - allow chest access for restocking (don't cancel)
                        // Just show a reminder on first click (non-sneaking)
                        if (!player.isShiftKeyDown()) {
                            handleOwnerClick(player, pos, shop);
                        } else {
                            // Track that owner is opening shop chest for restocking
                            openShopChests.put(player.getUUID(), new OpenShopChest(world, pos, shop.itemId()));
                        }
                        // Let the chest open normally for restocking
                        return;
                    } else {
                        // Customer clicked - process transaction
                        event.setCanceled(true); // Prevent chest opening for non-owners
                        handleCustomerClick(player, pos, shop);
                        return;
                    }
                }
            } else {
                // Shop data not loaded yet - cancel to prevent exploitation/confusion
                // and trigger load
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§7Loading shop data... please wait."));
                future.thenAccept(opt -> {
                     if (opt.isPresent()) {
                         VonixCore.execute(() -> player.sendSystemMessage(Component.literal("§aShop data loaded! Right-click again.")));
                     }
                });
                return;
            }
        }

        // Check if player is creating a chest shop
        if (ShopManager.getInstance().isCreatingShop(player.getUUID())) {
            if (state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL)) {
                event.setCanceled(true); // Stop chest from opening
                handleShopCreation(player, pos, state);
            }
        }
    }

    /**
     * Handle owner clicking their shop - shows stats reminder
     */
    private static void handleOwnerClick(ServerPlayer player, BlockPos pos, ShopManager.ChestShop shop) {
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

        // Get live stock from chest inventory
        int liveStock = ItemUtils.countChestItems(player.level(), pos, shop.itemId());

        player.sendSystemMessage(Component.literal("§6=== Your Shop ==="));
        player.sendSystemMessage(Component.literal("§7Item: §f" + shop.itemId()));
        player.sendSystemMessage(Component.literal("§7Stock: §f" + liveStock));
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
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        EconomyManager eco = EconomyManager.getInstance();

        // Get live stock from chest inventory
        int liveStock = ItemUtils.countChestItems(player.level(), pos, shop.itemId());

        // For now, left-click = buy, sneak-click = sell
        boolean isSneaking = player.isShiftKeyDown();

        if (!isSneaking && shop.buyPrice() != null && shop.buyPrice() > 0) {
            // Buy one item
            if (liveStock <= 0) {
                player.sendSystemMessage(Component.literal("§cThis shop is out of stock!"));
                return;
            }

            double price = shop.buyPrice();
            String world = player.level().dimension().location().toString();

            // Calculate tax
            double taxRate = network.vonix.vonixcore.config.ShopsConfig.CONFIG.chestShopsTaxRate.get();
            double taxAmount = price * taxRate;
            double totalPrice = price + taxAmount;

            // Async balance check
            eco.getBalance(player.getUUID()).thenAccept(balance -> {
                if (balance < totalPrice) {
                    player.sendSystemMessage(
                            Component.literal("§cInsufficient funds! Need " + symbol + String.format("%.2f", totalPrice)));
                    return;
                }

                // Proceed to main thread for item manipulation
                player.getServer().execute(() -> {
                    // Double check stock
                    if (ItemUtils.countChestItems(player.level(), pos, shop.itemId()) <= 0) {
                        player.sendSystemMessage(Component.literal("§cThis shop is out of stock!"));
                        return;
                    }

                    // Remove item from chest first (reserve it)
                    if (ItemUtils.removeChestItems(player.level(), pos, shop.itemId(), 1)) {
                        // Withdraw money
                        eco.withdraw(player.getUUID(), totalPrice).thenAccept(success -> {
                            if (success) {
                                // Deposit to owner
                                eco.deposit(shop.owner(), price); // Owner gets price without tax

                                // Give item to player (Main thread)
                                player.getServer().execute(() -> {
                                    var leftover = ItemUtils.giveItems(player, shop.itemId(), 1);
                                    if (!leftover.isEmpty()) {
                                        player.drop(leftover, false);
                                    }

                                    // Log transaction
                                    if (network.vonix.vonixcore.config.ShopsConfig.CONFIG.transactionLogEnabled.get()) {
                                        network.vonix.vonixcore.economy.TransactionLog.getInstance().logShopBuy(
                                                player.getUUID(), shop.owner(), price, taxAmount,
                                                0, shop.itemId(), 1, world, pos.getX(), pos.getY(), pos.getZ());
                                    }

                                    String taxInfo = taxAmount > 0.001
                                            ? String.format(" §7(+%s%.2f tax)", symbol, taxAmount)
                                            : "";
                                    player.sendSystemMessage(Component.literal("§aPurchased 1x " + shop.itemId()
                                            + " for " + symbol + String.format("%.2f", price)
                                            + taxInfo));
                                });
                            } else {
                                // Refund item to chest if payment failed
                                player.getServer().execute(() -> {
                                    ItemUtils.addChestItems(player.level(), pos, shop.itemId(), 1);
                                    player.sendSystemMessage(Component.literal("§cTransaction failed: Insufficient funds."));
                                });
                            }
                        });
                    } else {
                        player.sendSystemMessage(Component.literal("§cFailed to remove item from shop!"));
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
            String world = player.level().dimension().location().toString();

            // Calculate tax (deducted from seller's earnings)
            double taxRate = network.vonix.vonixcore.config.ShopsConfig.CONFIG.chestShopsTaxRate.get();
            double taxAmount = price * taxRate;
            double sellerReceives = price - taxAmount;

            // Check if shop owner can afford (Async)
            eco.getBalance(shop.owner()).thenAccept(ownerBalance -> {
                if (ownerBalance < price) {
                    player.sendSystemMessage(Component.literal("§cThe shop owner doesn't have enough money!"));
                    return;
                }

                // Proceed to main thread
                player.getServer().execute(() -> {
                    // Double check player has item
                    if (ItemUtils.countItems(player, shop.itemId()) < 1) {
                        player.sendSystemMessage(Component.literal("§cYou don't have any " + shop.itemId() + " to sell!"));
                        return;
                    }

                    // Remove from player
                    if (ItemUtils.removeItems(player, shop.itemId(), 1)) {
                        // Add to chest
                        int notAdded = ItemUtils.addChestItems(player.level(), pos, shop.itemId(), 1);
                        if (notAdded > 0) {
                            // Chest is full, refund player
                            ItemUtils.giveItems(player, shop.itemId(), 1);
                            player.sendSystemMessage(Component.literal("§cThe shop chest is full!"));
                            return;
                        }

                        // Transfer money
                        eco.withdraw(shop.owner(), price).thenAccept(success -> {
                            if (success) {
                                eco.deposit(player.getUUID(), sellerReceives); // Seller gets amount minus tax

                                player.getServer().execute(() -> {
                                    // Log transaction
                                    if (network.vonix.vonixcore.config.ShopsConfig.CONFIG.transactionLogEnabled.get()) {
                                        network.vonix.vonixcore.economy.TransactionLog.getInstance().logShopSell(
                                                player.getUUID(), shop.owner(), price, taxAmount,
                                                0, shop.itemId(), 1, world, pos.getX(), pos.getY(), pos.getZ());
                                    }

                                    String taxInfo = taxAmount > 0.001
                                            ? String.format(" §7(-%s%.2f tax)", symbol, taxAmount)
                                            : "";
                                    player.sendSystemMessage(Component.literal("§aSold 1x " + shop.itemId() + " for "
                                            + symbol + String.format("%.2f", sellerReceives)
                                            + taxInfo));
                                });
                            } else {
                                // Owner ran out of money? Revert item move
                                player.getServer().execute(() -> {
                                    ItemUtils.removeChestItems(player.level(), pos, shop.itemId(), 1);
                                    ItemUtils.giveItems(player, shop.itemId(), 1);
                                    player.sendSystemMessage(
                                            Component.literal("§cTransaction failed: Owner cannot afford this."));
                                });
                            }
                        });
                    }
                });
            });

        } else {
            // Show shop info
            player.sendSystemMessage(Component.literal("§6=== Shop ==="));
            player.sendSystemMessage(Component.literal("§7Item: §f" + shop.itemId()));
            player.sendSystemMessage(Component.literal("§7Stock: §f" + liveStock));
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
            
            ShopManager.getInstance().getShopAt(world, pos).thenAccept(shopOpt -> {
                VonixCore.execute(() -> {
                    if (shopOpt.isPresent()) {
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
                });
            });
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
        var future = ShopManager.getInstance().getShopAt(world, pos);

        if (future.isDone()) {
            Optional<ShopManager.ChestShop> shopOpt = future.join();
            if (shopOpt.isPresent()) {
                ShopManager.ChestShop shop = shopOpt.get();
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
        } else {
            // Not loaded. Prevent breaking to be safe.
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§7Loading shop data... please wait."));
            future.thenAccept(opt -> {
                if (opt.isPresent()) {
                    VonixCore.execute(() -> player.sendSystemMessage(Component.literal("§aShop data loaded! Try breaking again.")));
                }
            });
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

        String message = event.getRawText().trim().toLowerCase();
        event.setCanceled(true); // Cancel the chat message

        // Allow cancel at any step
        if (message.equals("cancel")) {
            ShopManager.getInstance().cancelShopCreation(player.getUUID());
            player.sendSystemMessage(Component.literal("§cShop creation cancelled."));
            return;
        }

        try {
            // Step 1: Buy price input
            if (creation.step == 1) {
                Double buyPrice = null;
                if (!message.equals("skip") && !message.equals("0")) {
                    buyPrice = Double.parseDouble(message);
                    if (buyPrice < 0) {
                        player.sendSystemMessage(Component.literal("§cPrice cannot be negative!"));
                        return;
                    }
                    if (buyPrice == 0)
                        buyPrice = null;
                }
                creation.buyPrice = buyPrice;
                creation.step = 2;

                player.sendSystemMessage(Component.literal("§a§l✓ §7Buy price set: "
                        + (buyPrice != null ? "§a$" + String.format("%.2f", buyPrice) : "§cDisabled")));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component
                        .literal("§eEnter the §cSELL §eprice in chat §7(price you pay players for their items):"));
                player.sendSystemMessage(Component.literal("§7Type §c0 §7or §cskip §7to disable selling."));
                player.sendSystemMessage(Component.literal("§7Type §ccancel §7to cancel."));
                return;
            }

            // Step 2: Sell price input - then finalize shop
            if (creation.step == 2) {
                Double sellPrice = null;
                if (!message.equals("skip") && !message.equals("0")) {
                    sellPrice = Double.parseDouble(message);
                    if (sellPrice < 0) {
                        player.sendSystemMessage(Component.literal("§cPrice cannot be negative!"));
                        return;
                    }
                    if (sellPrice == 0)
                        sellPrice = null;
                }
                creation.sellPrice = sellPrice;

                // Validate that at least one price is set
                if (creation.buyPrice == null && creation.sellPrice == null) {
                    player.sendSystemMessage(Component.literal("§cYou must set at least a buy or sell price!"));
                    creation.step = 1;
                    player.sendSystemMessage(Component.literal("§eEnter the §aBUY §eprice in chat:"));
                    return;
                }

                // Create the shop
                boolean success = ShopManager.getInstance().createChestShop(
                        player, creation.chestPos, creation.itemId, creation.buyPrice, creation.sellPrice, 0);

                if (success) {
                    player.sendSystemMessage(Component.literal("§a§l✓ Chest shop created successfully!"));

                    // Create sign on the front of the chest
                    if (player.level() instanceof ServerLevel serverLevel) {
                        createShopSign(serverLevel, player, creation);

                        // Create item display hologram above chest
                        ItemStack displayItem = ItemUtils.createItemFromId(creation.itemId);
                        if (!displayItem.isEmpty()) {
                            DisplayEntityManager.getInstance().spawnDisplay(serverLevel, creation.chestPos,
                                    displayItem);
                        }
                    }

                    String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
                    if (creation.buyPrice != null) {
                        player.sendSystemMessage(
                                Component.literal("§7Buy: §a" + symbol + String.format("%.2f", creation.buyPrice)));
                    }
                    if (creation.sellPrice != null) {
                        player.sendSystemMessage(
                                Component.literal("§7Sell: §c" + symbol + String.format("%.2f", creation.sellPrice)));
                    }
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
            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
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

    /**
     * Clean up shop sessions when menu closes and sync restocking
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShopGUIManager.getInstance().onPlayerCloseMenu(player.getUUID());

            // Check if player was restocking a shop chest
            OpenShopChest openShop = openShopChests.remove(player.getUUID());
            if (openShop != null && player.level() instanceof ServerLevel level) {
                // Sync inventory count to stock
                BlockPos pos = openShop.pos();
                var blockEntity = level.getBlockEntity(pos);

                int stockCount = 0;
                if (blockEntity instanceof ChestBlockEntity chest) {
                    for (int i = 0; i < chest.getContainerSize(); i++) {
                        ItemStack stack = chest.getItem(i);
                        if (!stack.isEmpty()) {
                            String stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                            if (stackItemId.equals(openShop.itemId())) {
                                stockCount += stack.getCount();
                            }
                        }
                    }
                } else if (blockEntity instanceof BarrelBlockEntity barrel) {
                    for (int i = 0; i < barrel.getContainerSize(); i++) {
                        ItemStack stack = barrel.getItem(i);
                        if (!stack.isEmpty()) {
                            String stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                            if (stackItemId.equals(openShop.itemId())) {
                                stockCount += stack.getCount();
                            }
                        }
                    }
                }

                // Get current shop to find old stock
                final int finalStockCount = stockCount;
                ShopManager.getInstance().getShopAt(openShop.world(), pos).thenAccept(shopOpt -> {
                    if (shopOpt.isPresent()) {
                        ShopManager.ChestShop shop = shopOpt.get();
                        int delta = finalStockCount - shop.stock();
                        if (delta != 0) {
                            ShopManager.getInstance().updateStock(openShop.world(), pos, delta);
                            VonixCore.execute(() -> player.sendSystemMessage(Component.literal("§a[Shop] Stock updated: " + finalStockCount + " items")));
                        }
                    }
                });
            }
        }
    }

    /**
     * Respawn shop holograms when chunks are loaded
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide())
            return;
        if (!EssentialsConfig.CONFIG.shopsEnabled.get())
            return;
        // Skip if mod or database not yet initialized (happens during world generation)
        if (VonixCore.getInstance() == null || VonixCore.getInstance().getDatabase() == null)
            return;

        if (event.getLevel() instanceof ServerLevel level) {
            var chunk = event.getChunk();
            // Schedule on next tick to avoid issues during chunk load
            level.getServer().execute(() -> {
                // Double-check database is still available
                if (VonixCore.getInstance() == null || VonixCore.getInstance().getDatabase() == null)
                    return;
                DisplayEntityManager.getInstance().respawnDisplaysInChunk(
                        level, chunk.getPos().x, chunk.getPos().z);
            });
        }
    }

    /**
     * Prevent interaction with shop display entities (hologram dupe prevention)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide())
            return;

        // Check if the entity is a shop display item
        if (event.getTarget() instanceof ItemEntity itemEntity) {
            if (itemEntity.getTags().contains("vonix_shop_display")) {
                // Cancel any interaction with shop display entities
                event.setCanceled(true);
            }
        }
    }
}
