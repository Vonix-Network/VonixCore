package network.vonix.vonixcore.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.economy.EconomyManager;

import java.util.List;

/**
 * Commands for managing player economy.
 * Forge 1.18.2 compatible version.
 */
public class EconomyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("balance")
                .executes(EconomyCommands::balanceCommand));
        dispatcher.register(Commands.literal("bal").executes(EconomyCommands::balanceCommand));

        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(EconomyCommands::payCommand))));

        dispatcher.register(Commands.literal("baltop")
                .executes(EconomyCommands::baltopCommand));

        dispatcher.register(Commands.literal("eco")
                .requires(src -> src.hasPermission(4))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EconomyCommands::ecoGiveCommand))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EconomyCommands::ecoTakeCommand))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(EconomyCommands::ecoSetCommand)))));
    }

    private static int balanceCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            EconomyManager.getInstance().getBalance(player.getUUID()).thenAccept(balance -> {
                player.sendMessage(
                        new TextComponent("§6[VC] Balance: §e" + EconomyManager.getInstance().format(balance)),
                        player.getUUID());
            });
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int payCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer sender = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            double amount = DoubleArgumentType.getDouble(ctx, "amount");

            EconomyManager.getInstance().transfer(sender.getUUID(), target.getUUID(), amount).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(new TextComponent("§a[VC] Sent " + EconomyManager.getInstance().format(amount)
                            + " to " + target.getName().getString()), sender.getUUID());
                    target.sendMessage(new TextComponent("§a[VC] Received " + EconomyManager.getInstance().format(amount)
                            + " from " + sender.getName().getString()), target.getUUID());
                } else {
                    sender.sendMessage(new TextComponent("§c[VC] Insufficient funds!"), sender.getUUID());
                }
            });
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int baltopCommand(CommandContext<CommandSourceStack> ctx) {
        var top = EconomyManager.getInstance().getTopBalances(10);
        ctx.getSource().sendSuccess(new TextComponent("§6§l----- Balance Top -----"), false);
        int rank = 1;
        for (var entry : top) {
            int r = rank++;
            var player = ctx.getSource().getServer().getPlayerList().getPlayer(entry.uuid());
            String name = player != null ? player.getName().getString() : entry.uuid().toString().substring(0, 8);
            ctx.getSource().sendSuccess(new TextComponent("§e" + r + ". §f" + name + " §7- §a" +
                    EconomyManager.getInstance().format(entry.balance())), false);
        }
        return 1;
    }

    private static int ecoGiveCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            EconomyManager.getInstance().deposit(target.getUUID(), amount);
            ctx.getSource().sendSuccess(new TextComponent(
                    "§a[VC] Gave " + EconomyManager.getInstance().format(amount) + " to " + target.getName().getString()),
                    true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int ecoTakeCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            EconomyManager.getInstance().withdraw(target.getUUID(), amount);
            ctx.getSource().sendSuccess(new TextComponent(
                    "§a[VC] Took " + EconomyManager.getInstance().format(amount) + " from " + target.getName().getString()),
                    true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int ecoSetCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            double amount = DoubleArgumentType.getDouble(ctx, "amount");
            EconomyManager.getInstance().setBalance(target.getUUID(), amount);
            ctx.getSource().sendSuccess(new TextComponent("§a[VC] Set " + target.getName().getString()
                    + "'s balance to " + EconomyManager.getInstance().format(amount)), true);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
