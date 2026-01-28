package network.vonix.vonixcore.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.EconomyPlanLoader;
import network.vonix.vonixcore.economy.ShopManager;
import network.vonix.vonixcore.economy.shop.ItemUtils;
import network.vonix.vonixcore.economy.shop.ShopGUIManager;

/**
 * Economy and shop commands.
 */
public class EconomyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /balance or /bal
        dispatcher.register(Commands.literal("balance")
                .executes(ctx -> showBalance(ctx.getSource())));
        dispatcher.register(Commands.literal("bal")
                .executes(ctx -> showBalance(ctx.getSource())));

        // /pay <player> <amount>
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(ctx -> pay(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        DoubleArgumentType.getDouble(ctx, "amount"))))));

        // /baltop
        dispatcher.register(Commands.literal("baltop")
                .executes(ctx -> showBalTop(ctx.getSource())));

        // /shop - open admin shop GUI
        dispatcher.register(Commands.literal("shop")
                .executes(ctx -> openShop(ctx.getSource())));

        // /market - open player market GUI
        dispatcher.register(Commands.literal("market")
                .executes(ctx -> openMarket(ctx.getSource())));

        // /sell <amount> [price] - sell held item to market
        dispatcher.register(Commands.literal("sell")
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                .executes(ctx -> sellToMarket(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        DoubleArgumentType.getDouble(ctx, "price"))))));

        // /daily - claim daily reward
        dispatcher.register(Commands.literal("daily")
                .executes(ctx -> claimDaily(ctx.getSource())));

        // /chestshop create - start chest shop creation
        dispatcher.register(Commands.literal("chestshop")
                .then(Commands.literal("create")
                        .executes(ctx -> startChestShopCreation(ctx.getSource())))
                .then(Commands.literal("remove")
                        .executes(ctx -> removeChestShop(ctx.getSource())))
                .then(Commands.literal("cancel")
                        .executes(ctx -> cancelChestShopCreation(ctx.getSource()))));

        // Admin commands
        // /adminshop setprice <item> <buy> <sell>
        dispatcher.register(Commands.literal("adminshop")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("setprice")
                        .then(Commands.argument("item", StringArgumentType.word())
                                .then(Commands.argument("buyPrice", DoubleArgumentType.doubleArg(0))
                                        .then(Commands.argument("sellPrice", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> setAdminPrice(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "item"),
                                                        DoubleArgumentType.getDouble(ctx, "buyPrice"),
                                                        DoubleArgumentType.getDouble(ctx, "sellPrice")))))))
                .then(Commands.literal("list")
                        .executes(ctx -> listAdminPrices(ctx.getSource()))));

        // /eco commands (admin)
        dispatcher.register(Commands.literal("eco")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(ctx -> ecoGive(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(ctx -> ecoTake(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> ecoSet(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(Commands.literal("import")
                        .executes(ctx -> ecoImport(ctx.getSource(), null))
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .executes(ctx -> ecoImport(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "file")))))
                .then(Commands.literal("export")
                        .executes(ctx -> ecoExport(ctx.getSource(), null))
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .executes(ctx -> ecoExport(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "file"))))));
    }

    private static int showBalance(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        EconomyManager.getInstance().getBalance(player.getUUID()).thenAccept(balance -> {
            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
            player.sendSystemMessage(Component.literal("§6Balance: §f" + symbol + String.format("%.2f", balance)));
        });
        return 1;
    }

    private static int pay(CommandSourceStack source, String targetName, double amount) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        var server = source.getServer();
        var targetPlayer = server.getPlayerList().getPlayerByName(targetName);

        if (targetPlayer == null) {
            source.sendFailure(Component.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        if (targetPlayer.getUUID().equals(player.getUUID())) {
            source.sendFailure(Component.literal("§cYou can't pay yourself!"));
            return 0;
        }

        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

        EconomyManager.getInstance().transfer(player.getUUID(), targetPlayer.getUUID(), amount).thenAccept(success -> {
            if (success) {
                player.sendSystemMessage(
                        Component.literal("§aYou sent " + symbol + String.format("%.2f", amount) + " to " + targetName));
                targetPlayer.sendSystemMessage(Component.literal("§aYou received " + symbol + String.format("%.2f", amount)
                        + " from " + player.getName().getString()));
            } else {
                source.sendFailure(Component.literal("§cInsufficient funds!"));
            }
        });
        return 1;
    }

    private static int showBalTop(CommandSourceStack source) {
        EconomyManager.getInstance().getTopBalances(10).thenAccept(top -> {
            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

            source.sendSuccess(() -> Component.literal("§6=== Balance Top ==="), false);
            int rank = 1;
            for (var entry : top) {
                var server = source.getServer();
                String name = entry.uuid().toString().substring(0, 8) + "...";
                var profile = server.getProfileCache();
                if (profile != null) {
                    var optional = profile.get(entry.uuid());
                    if (optional.isPresent()) {
                        name = optional.get().getName();
                    }
                }
                String finalName = name;
                int finalRank = rank;
                source.sendSuccess(() -> Component.literal("§e" + finalRank + ". §f" + finalName + " §7- §a" + symbol
                        + String.format("%.2f", entry.balance())), false);
                rank++;
            }
        });
        return 1;
    }

    private static int openShop(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ShopGUIManager.getInstance().openAdminShop(player);
        return 1;
    }

    private static int openMarket(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ShopGUIManager.getInstance().openPlayerMarket(player);
        return 1;
    }

    private static int sellToMarket(CommandSourceStack source, int amount, double price) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal("§cYou must hold an item to sell!"));
            return 0;
        }

        String itemId = ItemUtils.getItemId(heldItem);
        int playerHas = ItemUtils.countItems(player, itemId);

        if (playerHas < amount) {
            source.sendFailure(Component.literal("§cYou don't have " + amount + " of this item!"));
            return 0;
        }

        // Remove items and create listing
        if (ItemUtils.removeItems(player, itemId, amount)) {
            String nbt = ItemUtils.serializeItemStack(heldItem);
            ShopManager.getInstance().createListing(player.getUUID(), itemId, nbt, price, amount);

            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
            player.sendSystemMessage(Component
                    .literal("§aListed " + amount + "x " + itemId + " for " + symbol + String.format("%.2f", price)));
            return 1;
        }

        return 0;
    }

    private static int claimDaily(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ShopManager.getInstance().claimDailyReward(player.getUUID()).thenAccept(result -> {
            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

            if (result.success()) {
                player.sendSystemMessage(Component.literal("§a§l✓ Daily Reward Claimed!"));
                player.sendSystemMessage(
                        Component.literal("§7Amount: §a" + symbol + String.format("%.2f", result.amount())));
                player.sendSystemMessage(Component.literal("§7Streak: §e" + result.streak() + " days"));
            } else {
                source.sendFailure(Component.literal("§c" + result.message()));
            }
        });
        return 1;
    }

    private static int startChestShopCreation(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        ShopManager.getInstance().startShopCreation(player);
        player.sendSystemMessage(Component.literal("§aShop creation started! Click a chest or barrel to continue."));
        player.sendSystemMessage(Component.literal("§7Use §e/chestshop cancel §7to cancel."));
        return 1;
    }

    private static int removeChestShop(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        // TODO: Implement looking at chest to remove
        player.sendSystemMessage(Component.literal("§7To remove a shop, break the chest while sneaking."));
        return 1;
    }

    private static int cancelChestShopCreation(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players."));
            return 0;
        }

        if (ShopManager.getInstance().isCreatingShop(player.getUUID())) {
            ShopManager.getInstance().cancelShopCreation(player.getUUID());
            player.sendSystemMessage(Component.literal("§eShop creation cancelled."));
        } else {
            player.sendSystemMessage(Component.literal("§cYou're not creating a shop."));
        }
        return 1;
    }

    private static int setAdminPrice(CommandSourceStack source, String itemId, double buyPrice, double sellPrice) {
        // Normalize item ID
        String normalizedItemId = itemId;
        if (!normalizedItemId.contains(":")) {
            normalizedItemId = "minecraft:" + normalizedItemId;
        }

        // Validate item exists
        var testStack = ItemUtils.createItemFromId(normalizedItemId);
        if (testStack.isEmpty()) {
            source.sendFailure(Component.literal("§cInvalid item ID: " + normalizedItemId));
            return 0;
        }

        Double buy = buyPrice > 0 ? buyPrice : null;
        Double sell = sellPrice > 0 ? sellPrice : null;
        final String finalItemId = normalizedItemId;

        ShopManager.getInstance().setAdminPrice(finalItemId, buy, sell).thenAccept(success -> {
            if (success) {
                String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
                source.sendSuccess(() -> Component.literal("§aSet prices for " + finalItemId + ": Buy=" +
                        (buy != null ? symbol + String.format("%.2f", buy) : "N/A") + ", Sell=" +
                        (sell != null ? symbol + String.format("%.2f", sell) : "N/A")), true);
            } else {
                source.sendFailure(Component.literal("§cFailed to set price."));
            }
        });
        
        return 1;
    }

    private static int listAdminPrices(CommandSourceStack source) {
        ShopManager.getInstance().getAllAdminItems().thenAccept(items -> {
            String symbol = EssentialsConfig.CONFIG.currencySymbol.get();

            source.sendSuccess(() -> Component.literal("§6=== Admin Shop Prices ==="), false);
            for (var item : items) {
                String buy = item.buyPrice() != null ? symbol + String.format("%.2f", item.buyPrice()) : "N/A";
                String sell = item.sellPrice() != null ? symbol + String.format("%.2f", item.sellPrice()) : "N/A";
                source.sendSuccess(
                        () -> Component.literal("§f" + item.itemId() + " §7- Buy: §a" + buy + " §7Sell: §c" + sell), false);
            }
        });
        return 1;
    }

    private static int ecoGive(CommandSourceStack source, String targetName, double amount) {
        var server = source.getServer();
        var targetPlayer = server.getPlayerList().getPlayerByName(targetName);

        if (targetPlayer == null) {
            source.sendFailure(Component.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        EconomyManager.getInstance().deposit(targetPlayer.getUUID(), amount).thenAccept(success -> {
            source.sendSuccess(
                    () -> Component.literal("§aGave " + symbol + String.format("%.2f", amount) + " to " + targetName),
                    true);
            targetPlayer.sendSystemMessage(Component.literal("§aYou received " + symbol + String.format("%.2f", amount)));
        });
        return 1;
    }

    private static int ecoTake(CommandSourceStack source, String targetName, double amount) {
        var server = source.getServer();
        var targetPlayer = server.getPlayerList().getPlayerByName(targetName);

        if (targetPlayer == null) {
            source.sendFailure(Component.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        EconomyManager.getInstance().withdraw(targetPlayer.getUUID(), amount).thenAccept(success -> {
            if (success) {
                source.sendSuccess(
                        () -> Component.literal("§cTook " + symbol + String.format("%.2f", amount) + " from " + targetName),
                        true);
            } else {
                source.sendFailure(Component.literal("§cPlayer doesn't have enough money."));
            }
        });
        return 1;
    }

    private static int ecoSet(CommandSourceStack source, String targetName, double amount) {
        var server = source.getServer();
        var targetPlayer = server.getPlayerList().getPlayerByName(targetName);

        if (targetPlayer == null) {
            source.sendFailure(Component.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        String symbol = EssentialsConfig.CONFIG.currencySymbol.get();
        EconomyManager.getInstance().setBalance(targetPlayer.getUUID(), amount).thenAccept(success -> {
            source.sendSuccess(
                    () -> Component
                            .literal("§aSet " + targetName + "'s balance to " + symbol + String.format("%.2f", amount)),
                    true);
        });
        return 1;
    }

    private static int ecoImport(CommandSourceStack source, String fileName) {
        java.nio.file.Path filePath;
        if (fileName == null || fileName.isEmpty()) {
            filePath = EconomyPlanLoader.getDefaultPath();
        } else {
            // If just a filename, look in config folder
            if (!fileName.contains("/") && !fileName.contains("\\")) {
                filePath = network.vonix.vonixcore.VonixCore.getInstance().getConfigPath().resolve(fileName);
            } else {
                filePath = java.nio.file.Path.of(fileName);
            }
        }

        if (!java.nio.file.Files.exists(filePath)) {
            source.sendFailure(Component.literal("§cFile not found: " + filePath));
            return 0;
        }

        EconomyPlanLoader.EconomyPlan plan = EconomyPlanLoader.loadFromFile(filePath);
        if (plan == null) {
            source.sendFailure(Component.literal("§cFailed to load economy plan from file."));
            return 0;
        }

        final java.nio.file.Path finalFilePath = filePath;
        EconomyPlanLoader.importToDatabase(plan).thenAccept(imported -> {
            source.sendSuccess(
                    () -> Component.literal("§aImported " + imported + " items from " + finalFilePath.getFileName()), true);
        });
        return 1;
    }

    private static int ecoExport(CommandSourceStack source, String fileName) {
        java.nio.file.Path filePath;
        if (fileName == null || fileName.isEmpty()) {
            filePath = EconomyPlanLoader.getDefaultPath();
        } else {
            // If just a filename, save in config folder
            if (!fileName.contains("/") && !fileName.contains("\\")) {
                filePath = network.vonix.vonixcore.VonixCore.getInstance().getConfigPath().resolve(fileName);
            } else {
                filePath = java.nio.file.Path.of(fileName);
            }
        }

        // Ensure .json extension
        if (!filePath.toString().endsWith(".json")) {
            filePath = java.nio.file.Path.of(filePath.toString() + ".json");
        }

        final java.nio.file.Path finalFilePath = filePath;
        EconomyPlanLoader.exportToFile(filePath).thenAccept(success -> {
            if (success) {
                source.sendSuccess(
                        () -> Component.literal("§aExported economy plan to: " + finalFilePath.getFileName()), true);
            } else {
                source.sendFailure(Component.literal("§cFailed to export economy plan."));
            }
        });
        return 1;
    }
}
