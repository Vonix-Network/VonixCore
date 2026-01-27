package network.vonix.vonixcore.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.ShopManager;

/**
 * Shop commands for the economy system.
 * Supports chest shops, admin shop, player shop, and daily rewards.
 */
public class ShopCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
    }

    // ===== CHEST SHOP COMMANDS =====

    private static int chestShopCreate(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ShopManager.getInstance().startShopCreation(player);

            player.sendMessage(new TextComponent("§6[Shop] §eRight-click a chest to create your shop."),
                    player.getUUID());
            player.sendMessage(new TextComponent("§7Type §c/chestshop cancel §7to cancel."), player.getUUID());

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int chestShopRemove(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            player.sendMessage(new TextComponent("§6[Shop] §eRight-click the chest shop you want to remove."),
                    player.getUUID());
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int chestShopInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            player.sendMessage(new TextComponent("§6[Shop] §eRight-click a chest shop to see its info."),
                    player.getUUID());
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== PLAYER SHOP COMMANDS =====

    private static int openPlayerShop(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            // Open player market GUI
            player.sendMessage(new TextComponent("§6[Shop] §ePlayer market GUI opening..."), player.getUUID());
            // ShopGUIManager.getInstance().openPlayerMarket(player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int playerShopSell(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                player.sendMessage(new TextComponent("§c[Shop] Hold the item you want to sell!"), player.getUUID());
                return 0;
            }

            String itemId = ShopManager.getItemId(held);
            player.sendMessage(new TextComponent("§6[Shop] §eYou're selling: §f" + held.getDisplayName().getString()),
                    player.getUUID());
            player.sendMessage(new TextComponent("§7Type the price in chat (e.g., §e100§7):"), player.getUUID());

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== SERVER SHOP COMMANDS =====

    private static int openServerShop(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            // Open admin shop GUI
            player.sendMessage(new TextComponent("§6[Shop] §eServer shop GUI opening..."), player.getUUID());
            // ShopGUIManager.getInstance().openAdminShop(player);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int serverShopSell(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                player.sendMessage(new TextComponent("§c[Shop] Hold an item to sell!"), player.getUUID());
                return 0;
            }

            String itemId = ShopManager.getItemId(held);
            var priceInfo = ShopManager.getInstance().getAdminPrice(itemId);

            if (priceInfo == null || priceInfo.sellPrice() == null) {
                player.sendMessage(new TextComponent("§c[Shop] The server doesn't buy this item."), player.getUUID());
                return 0;
            }

            double totalPrice = priceInfo.sellPrice() * held.getCount();

            // Clickable confirmation
            TextComponent yesButton = new TextComponent("§a§l[YES]");
            yesButton.setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sell hand"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Click to confirm sale"))));

            TextComponent noButton = new TextComponent("§c§l[NO]");
            noButton.setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop"))
                    .withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to cancel"))));

            player.sendMessage(
                    new TextComponent("§6[Shop] §eSell " + held.getCount() + "x " + held.getDisplayName().getString() +
                            " for §a" + EconomyManager.getInstance().format(totalPrice) + "§e?"),
                    player.getUUID());
            player.sendMessage(new TextComponent("").append(yesButton).append(new TextComponent(" ")).append(noButton),
                    player.getUUID());

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int adminShopSet(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            double price = DoubleArgumentType.getDouble(ctx, "price");
            ItemStack held = player.getMainHandItem();

            if (held.isEmpty()) {
                player.sendMessage(new TextComponent("§c[Shop] Hold the item to set price for!"), player.getUUID());
                return 0;
            }

            String itemId = ShopManager.getItemId(held);
            ShopManager.getInstance().setAdminPrice(itemId, price, null);

            ctx.getSource().sendSuccess(new TextComponent("§a[Shop] Set buy price for " + itemId + " to " +
                    EconomyManager.getInstance().format(price)), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int adminShopSetSell(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            double price = DoubleArgumentType.getDouble(ctx, "price");
            ItemStack held = player.getMainHandItem();

            if (held.isEmpty()) {
                player.sendMessage(new TextComponent("§c[Shop] Hold the item to set sell price for!"),
                        player.getUUID());
                return 0;
            }

            String itemId = ShopManager.getItemId(held);
            var existing = ShopManager.getInstance().getAdminPrice(itemId);
            Double buyPrice = existing != null ? existing.buyPrice() : null;
            ShopManager.getInstance().setAdminPrice(itemId, buyPrice, price);

            ctx.getSource().sendSuccess(new TextComponent("§a[Shop] Set sell price for " + itemId + " to " +
                    EconomyManager.getInstance().format(price)), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== SELL COMMANDS =====

    private static int sellHand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                player.sendMessage(new TextComponent("§c[Shop] You're not holding anything!"), player.getUUID());
                return 0;
            }

            String itemId = ShopManager.getItemId(held);
            var priceInfo = ShopManager.getInstance().getAdminPrice(itemId);

            if (priceInfo == null || priceInfo.sellPrice() == null) {
                player.sendMessage(new TextComponent("§c[Shop] This item cannot be sold to the server."),
                        player.getUUID());
                return 0;
            }

            double totalPrice = priceInfo.sellPrice() * held.getCount();
            EconomyManager.getInstance().deposit(player.getUUID(), totalPrice);

            int count = held.getCount();
            player.getMainHandItem().setCount(0);

            player.sendMessage(new TextComponent("§a[Shop] Sold " + count + " items for " +
                    EconomyManager.getInstance().format(totalPrice) + "!"), player.getUUID());
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int sellAll(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            double total = 0;
            int itemsSold = 0;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty())
                    continue;

                String itemId = ShopManager.getItemId(stack);
                var priceInfo = ShopManager.getInstance().getAdminPrice(itemId);

                if (priceInfo != null && priceInfo.sellPrice() != null) {
                    double price = priceInfo.sellPrice() * stack.getCount();
                    total += price;
                    itemsSold += stack.getCount();
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }

            if (itemsSold > 0) {
                EconomyManager.getInstance().deposit(player.getUUID(), total);
                player.sendMessage(new TextComponent("§a[Shop] Sold " + itemsSold + " items for " +
                        EconomyManager.getInstance().format(total) + "!"), player.getUUID());
            } else {
                player.sendMessage(new TextComponent("§c[Shop] No sellable items in your inventory."),
                        player.getUUID());
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== DAILY REWARDS =====

    private static int claimDaily(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            var result = ShopManager.getInstance().claimDailyReward(player.getUUID());

            if (result.success()) {
                player.sendMessage(new TextComponent("§6§l✦ DAILY REWARD ✦"), player.getUUID());
                player.sendMessage(
                        new TextComponent(
                                "§aYou received " + EconomyManager.getInstance().format(result.amount()) + "!"),
                        player.getUUID());
                player.sendMessage(new TextComponent("§7Current streak: §e" + result.streak() + " days"),
                        player.getUUID());
                if (result.streak() < 7) {
                    player.sendMessage(new TextComponent("§7Come back tomorrow for a bigger reward!"),
                            player.getUUID());
                } else {
                    player.sendMessage(new TextComponent("§6You've reached the maximum streak bonus!"),
                            player.getUUID());
                }
            } else {
                player.sendMessage(new TextComponent("§c[Daily] " + result.message()), player.getUUID());
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ===== HELP =====

    private static int shopHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(new TextComponent("§6§l----- Shop Help -----"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/shop player §7- Browse player market"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/shop player sell §7- List item for sale"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/shop server §7- Browse server shop"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/chestshop create §7- Create a chest shop"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/sell hand §7- Sell held item to server"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/sell all §7- Sell all sellable items"), false);
        ctx.getSource().sendSuccess(new TextComponent("§e/daily §7- Claim daily reward"), false);
        return 1;
    }
}
