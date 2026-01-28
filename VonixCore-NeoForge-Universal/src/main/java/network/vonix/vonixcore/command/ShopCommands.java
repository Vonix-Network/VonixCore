package network.vonix.vonixcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;
import network.vonix.vonixcore.economy.shop.ShopGUIManager;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Shop commands for the economy system.
 * Supports chest shops, admin shop, player shop, and daily rewards.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class ShopCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Chest shop commands
        dispatcher.register(Commands.literal("chestshop")
                .then(Commands.literal("create")
                        .executes(ShopCommands::chestShopCreate))
                .then(Commands.literal("remove")
                        .executes(ShopCommands::chestShopRemove))
                .then(Commands.literal("info")
                        .executes(ShopCommands::chestShopInfo)));

        // Shop GUI commands
        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("player")
                        .then(Commands.literal("sell")
                                .executes(ShopCommands::playerShopSell))
                        .executes(ShopCommands::openPlayerShop))
                .then(Commands.literal("server")
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(3))
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                                        .executes(ShopCommands::adminShopSet)))
                        .then(Commands.literal("sell")
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                                        .requires(src -> src.hasPermission(3))
                                        .executes(ShopCommands::adminShopSetSell))
                                .executes(ShopCommands::serverShopSell))
                        .executes(ShopCommands::openServerShop))
                .executes(ShopCommands::shopHelp));

        // Daily rewards
        dispatcher.register(Commands.literal("daily")
                .executes(ShopCommands::claimDaily));

        // Sell command (quick sell to server)
        dispatcher.register(Commands.literal("sell")
                .then(Commands.literal("hand")
                        .executes(ShopCommands::sellHand))
                .then(Commands.literal("all")
                        .executes(ShopCommands::sellAll)));

        VonixCore.LOGGER.info("[VonixCore] Shop commands registered");
    }

    // ===== CHEST SHOP COMMANDS =====

    private static int chestShopCreate(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        ShopManager.getInstance().startShopCreation(player);

        player.sendSystemMessage(Component.literal("§6[Shop] §eRight-click a chest to create your shop."));
        player.sendSystemMessage(Component.literal("§7Type §c/chestshop cancel §7to cancel."));

        return 1;
    }

    private static int chestShopRemove(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        player.sendSystemMessage(Component.literal("§6[Shop] §eRight-click the chest shop you want to remove."));
        return 1;
    }

    private static int chestShopInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        player.sendSystemMessage(Component.literal("§6[Shop] §eRight-click a chest shop to see its info."));
        return 1;
    }

    // ===== PLAYER SHOP COMMANDS =====

    private static int openPlayerShop(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        // Open player market GUI
        ShopGUIManager.getInstance().openPlayerMarket(player);
        return 1;
    }

    private static int playerShopSell(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop] Hold the item you want to sell!"));
            return 0;
        }

        String itemId = ShopManager.getItemId(held);
        player.sendSystemMessage(Component.literal("§6[Shop] §eYou're selling: §f" + held.getHoverName().getString()));
        player.sendSystemMessage(Component.literal("§7Type the price in chat (e.g., §e100§7):"));

        // Store pending sale state
        // TODO: Implement chat listener for price input

        return 1;
    }

    // ===== SERVER SHOP COMMANDS =====

    private static int openServerShop(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        // Open admin shop GUI
        ShopGUIManager.getInstance().openAdminShop(player);
        return 1;
    }

    private static int serverShopSell(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop] Hold an item to sell!"));
            return 0;
        }

        String itemId = ShopManager.getItemId(held);
        ShopManager.getInstance().getAdminPrice(itemId).thenAccept(priceInfoOpt -> {
            VonixCore.execute(() -> {
                if (priceInfoOpt.isEmpty() || priceInfoOpt.get().sellPrice() == null) {
                    player.sendSystemMessage(Component.literal("§c[Shop] The server doesn't buy this item."));
                    return;
                }

                double totalPrice = priceInfoOpt.get().sellPrice() * held.getCount();

                // Clickable confirmation
                Component yesButton = Component.literal("§a§l[YES]")
                    .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sell hand"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click to confirm sale"))));

                Component noButton = Component.literal("§c§l[NO]")
                    .withStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop"))
                        .withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to cancel"))));

                player.sendSystemMessage(
                    Component.literal("§6[Shop] §eSell " + held.getCount() + "x " + held.getHoverName().getString() +
                        " for §a" + EconomyManager.getInstance().format(totalPrice) + "§e?"));
                player.sendSystemMessage(
                    Component.literal("").append(yesButton).append(Component.literal(" ")).append(noButton));
            });
        });

        return 1;
    }

    private static int adminShopSet(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        double price = DoubleArgumentType.getDouble(ctx, "price");
        ItemStack held = player.getMainHandItem();

        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop] Hold the item to set price for!"));
            return 0;
        }

        String itemId = ShopManager.getItemId(held);
        
        ShopManager.getInstance().getAdminPrice(itemId).thenAccept(opt -> {
            Double sellPrice = opt.map(ShopManager.AdminShopItem::sellPrice).orElse(null);
            ShopManager.getInstance().setAdminPrice(itemId, price, sellPrice).thenAccept(success -> {
                VonixCore.execute(() -> {
                    if (success) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Shop] Set buy price for " + itemId + " to " +
                                EconomyManager.getInstance().format(price)), true);
                    } else {
                        ctx.getSource().sendFailure(Component.literal("§c[Shop] Failed to set price."));
                    }
                });
            });
        });
        
        return 1;
    }

    private static int adminShopSetSell(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        double price = DoubleArgumentType.getDouble(ctx, "price");
        ItemStack held = player.getMainHandItem();

        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop] Hold the item to set sell price for!"));
            return 0;
        }

        String itemId = ShopManager.getItemId(held);
        ShopManager.getInstance().getAdminPrice(itemId).thenAccept(opt -> {
            Double buyPrice = opt.map(ShopManager.AdminShopItem::buyPrice).orElse(null);
            ShopManager.getInstance().setAdminPrice(itemId, buyPrice, price).thenAccept(success -> {
                VonixCore.execute(() -> {
                    if (success) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Shop] Set sell price for " + itemId + " to " +
                                EconomyManager.getInstance().format(price)), true);
                    } else {
                        ctx.getSource().sendFailure(Component.literal("§c[Shop] Failed to set price."));
                    }
                });
            });
        });

        return 1;
    }

    // ===== SELL COMMANDS =====

    private static int sellHand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[Shop] You're not holding anything!"));
            return 0;
        }

        String itemId = ShopManager.getItemId(held);
        
        ShopManager.getInstance().getAdminPrice(itemId).thenAccept(opt -> {
            VonixCore.execute(() -> {
                if (opt.isEmpty() || opt.get().sellPrice() == null) {
                    player.sendSystemMessage(Component.literal("§c[Shop] This item cannot be sold to the server."));
                    return;
                }

                double totalPrice = opt.get().sellPrice() * held.getCount();
                int count = held.getCount();
                player.getMainHandItem().setCount(0);

                EconomyManager.getInstance().deposit(player.getUUID(), totalPrice).thenAccept(success -> {
                    VonixCore.execute(() -> {
                        player.sendSystemMessage(Component.literal("§a[Shop] Sold " + count + " items for " +
                                EconomyManager.getInstance().format(totalPrice) + "!"));
                    });
                });
            });
        });
        
        return 1;
    }

    private static int sellAll(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        ShopManager.getInstance().getAllAdminItems().thenAccept(adminItems -> {
            VonixCore.execute(() -> {
                Map<String, Double> prices = adminItems.stream()
                        .filter(i -> i.sellPrice() != null)
                        .collect(Collectors.toMap(ShopManager.AdminShopItem::itemId, ShopManager.AdminShopItem::sellPrice));

                double total = 0;
                int itemsSold = 0;

                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.isEmpty())
                        continue;

                    String itemId = ShopManager.getItemId(stack);
                    if (prices.containsKey(itemId)) {
                        double price = prices.get(itemId) * stack.getCount();
                        total += price;
                        itemsSold += stack.getCount();
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                }

                if (itemsSold > 0) {
                    final double finalTotal = total;
                    final int finalItemsSold = itemsSold;
                    EconomyManager.getInstance().deposit(player.getUUID(), total).thenAccept(success -> {
                        VonixCore.execute(() -> {
                            player.sendSystemMessage(Component.literal("§a[Shop] Sold " + finalItemsSold + " items for " +
                                    EconomyManager.getInstance().format(finalTotal) + "!"));
                        });
                    });
                } else {
                    player.sendSystemMessage(Component.literal("§c[Shop] No sellable items in your inventory."));
                }
            });
        });
        
        return 1;
    }

    // ===== DAILY REWARDS =====

    private static int claimDaily(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null)
            return 0;

        ShopManager.getInstance().claimDailyReward(player.getUUID()).thenAccept(result -> {
            if (result.success()) {
                player.sendSystemMessage(Component.literal("§6§l✦ DAILY REWARD ✦"));
                player.sendSystemMessage(
                        Component.literal("§aYou received " + EconomyManager.getInstance().format(result.amount()) + "!"));
                player.sendSystemMessage(Component.literal("§7Current streak: §e" + result.streak() + " days"));
                if (result.streak() < 7) {
                    player.sendSystemMessage(Component.literal("§7Come back tomorrow for a bigger reward!"));
                } else {
                    player.sendSystemMessage(Component.literal("§6You've reached the maximum streak bonus!"));
                }
            } else {
                player.sendSystemMessage(Component.literal("§c[Daily] " + result.message()));
            }
        });
        return 1;
    }

    // ===== HELP =====

    private static int shopHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("§6§l----- Shop Help -----"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/shop player §7- Browse player market"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/shop player sell §7- List item for sale"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/shop server §7- Browse server shop"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/chestshop create §7- Create a chest shop"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/sell hand §7- Sell held item to server"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/sell all §7- Sell all sellable items"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§e/daily §7- Claim daily reward"), false);
        return 1;
    }
}
