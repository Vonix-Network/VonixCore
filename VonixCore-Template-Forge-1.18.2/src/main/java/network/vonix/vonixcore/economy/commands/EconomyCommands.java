package network.vonix.vonixcore.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.economy.EconomyManager;

/**
 * Economy admin commands.
 */
public class EconomyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
                .requires(src -> src.hasPermission(3))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(new TextComponent("§6Economy Admin Commands:"), false);
                    ctx.getSource().sendSuccess(new TextComponent("§e/eco give <player> <amount> §7- Give money"),
                            false);
                    ctx.getSource().sendSuccess(new TextComponent("§e/eco take <player> <amount> §7- Take money"),
                            false);
                    ctx.getSource().sendSuccess(new TextComponent("§e/eco set <player> <amount> §7- Set balance"),
                            false);
                    ctx.getSource().sendSuccess(new TextComponent("§e/eco reset <player> §7- Reset balance"), false);
                    return 1;
                })
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            double amount = DoubleArgumentType.getDouble(ctx, "amount");

                                            EconomyManager.getInstance().deposit(target.getUUID(), amount);
                                            String formatted = EconomyManager.getInstance().format(amount);

                                            ctx.getSource().sendSuccess(new TextComponent(
                                                    "§aGave " + formatted + " to " + target.getName().getString()),
                                                    false);
                                            target.sendMessage(
                                                    new TextComponent("§aYou received " + formatted + " from an admin"),
                                                    net.minecraft.Util.NIL_UUID);
                                            return 1;
                                        }))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            double amount = DoubleArgumentType.getDouble(ctx, "amount");

                                            if (EconomyManager.getInstance().withdraw(target.getUUID(), amount)) {
                                                String formatted = EconomyManager.getInstance().format(amount);
                                                ctx.getSource().sendSuccess(new TextComponent("§aTook " + formatted
                                                        + " from " + target.getName().getString()), false);
                                                return 1;
                                            } else {
                                                ctx.getSource().sendFailure(
                                                        new TextComponent("§cPlayer doesn't have enough money"));
                                                return 0;
                                            }
                                        }))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            double amount = DoubleArgumentType.getDouble(ctx, "amount");

                                            EconomyManager.getInstance().setBalance(target.getUUID(), amount);
                                            String formatted = EconomyManager.getInstance().format(amount);

                                            ctx.getSource().sendSuccess(new TextComponent("§aSet "
                                                    + target.getName().getString() + "'s balance to " + formatted),
                                                    false);
                                            return 1;
                                        }))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

                                    EconomyManager.getInstance().setBalance(target.getUUID(), 100.0);

                                    ctx.getSource().sendSuccess(
                                            new TextComponent("§aReset " + target.getName().getString() + "'s balance"),
                                            false);
                                    return 1;
                                }))));
    }
}
