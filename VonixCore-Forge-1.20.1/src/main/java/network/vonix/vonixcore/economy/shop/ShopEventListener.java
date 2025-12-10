package network.vonix.vonixcore.economy.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;

/**
 * Event listener for shop interactions.
 * Forge 1.20.1 compatible version.
 */
@Mod.EventBusSubscriber(modid = VonixCore.MODID)
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
            return;
        }

        // Check for chest shop interaction
        if (state.getBlock() instanceof ChestBlock || state.is(Blocks.BARREL)) {
            String world = level.dimension().location().toString();
            ShopManager.ChestShop shop = ShopManager.getInstance().getShopAt(world, pos);

            if (shop != null) {
                event.setCanceled(true);

                if (shop.owner().equals(player.getUUID())) {
                    handleOwnerClick(player, pos, shop);
                } else {
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

    private static void handleCustomerClick(ServerPlayer player, BlockPos pos, ShopManager.ChestShop shop) {
        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        EconomyManager eco = EconomyManager.getInstance();

        boolean isSneaking = player.isShiftKeyDown();

        if (!isSneaking && shop.buyPrice() != null && shop.buyPrice() > 0) {
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

            if (eco.withdraw(player.getUUID(), price)) {
                eco.deposit(shop.owner(), price);
                var leftover = ItemUtils.giveItems(player, shop.itemId(), 1);
                if (!leftover.isEmpty()) {
                    player.drop(leftover, false);
                }
                player.sendSystemMessage(Component
                        .literal("§aPurchased 1x " + shop.itemId() + " for " + symbol + String.format("%.2f", price)));
            }

        } else if (isSneaking && shop.sellPrice() != null && shop.sellPrice() > 0) {
            int playerHas = ItemUtils.countItems(player, shop.itemId());

            if (playerHas < 1) {
                player.sendSystemMessage(Component.literal("§cYou don't have any " + shop.itemId() + " to sell!"));
                return;
            }

            double price = shop.sellPrice();
            double ownerBalance = eco.getBalance(shop.owner());
            if (ownerBalance < price) {
                player.sendSystemMessage(Component.literal("§cThe shop owner doesn't have enough money!"));
                return;
            }

            if (ItemUtils.removeItems(player, shop.itemId(), 1)) {
                eco.withdraw(shop.owner(), price);
                eco.deposit(player.getUUID(), price);
                player.sendSystemMessage(Component
                        .literal("§aSold 1x " + shop.itemId() + " for " + symbol + String.format("%.2f", price)));
            }
        } else {
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

    private static void handleShopCreation(ServerPlayer player, BlockPos pos, BlockState state) {
        ShopManager.ShopCreationState creation = ShopManager.getInstance().getCreationState(player.getUUID());

        if (creation == null)
            return;

        if (creation.step == 0) {
            String world = player.level().dimension().location().toString();
            if (ShopManager.getInstance().getShopAt(world, pos) != null) {
                player.sendSystemMessage(Component.literal("§cThis chest is already a shop!"));
                ShopManager.getInstance().cancelShopCreation(player.getUUID());
                return;
            }

            creation.chestPos = pos;
            creation.step = 1;
            player.sendSystemMessage(
                    Component.literal("§aChest selected! Now type the item ID in chat (e.g., minecraft:diamond)"));
            player.sendSystemMessage(Component.literal("§7Or type 'cancel' to cancel."));
        }
    }

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
            if (!shop.owner().equals(player.getUUID()) && !player.hasPermissions(2)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§cYou cannot break someone else's shop!"));
            } else {
                ShopManager.getInstance().deleteShop(world, pos);
                if (player.level() instanceof ServerLevel serverLevel) {
                    DisplayEntityManager.getInstance().removeDisplay(serverLevel, pos);
                }
                player.sendSystemMessage(Component.literal("§eShop removed."));
            }
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShopGUIManager.getInstance().onPlayerCloseMenu(player.getUUID());
        }
    }
}
