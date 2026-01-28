package network.vonix.vonixcore.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
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
                ctx.getSource().sendSuccess(() -> Component.literal("§6Economy Admin Commands:"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§e/eco give <player> <amount> §7- Give money"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§e/eco take <player> <amount> §7- Take money"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§e/eco set <player> <amount> §7- Set balance"), false);
                ctx.getSource().sendSuccess(() -> Component.literal("§e/eco reset <player> §7- Reset balance"), false);
                return 1;
            })
            .then(Commands.literal("give")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");
                            
                            EconomyManager.getInstance().deposit(target.getUUID(), amount).thenAccept(v -> {
                                String formatted = EconomyManager.getInstance().format(amount);
                                
                                ctx.getSource().sendSuccess(() -> Component.literal("§aGave " + formatted + " to " + target.getName().getString()), false);
                                target.sendSystemMessage(Component.literal("§aYou received " + formatted + " from an admin"));
                            });
                            return 1;
                        }))))
            .then(Commands.literal("take")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");
                            
                            EconomyManager.getInstance().withdraw(target.getUUID(), amount).thenAccept(success -> {
                                if (success) {
                                    String formatted = EconomyManager.getInstance().format(amount);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aTook " + formatted + " from " + target.getName().getString()), false);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("§cPlayer doesn't have enough money"));
                                }
                            });
                            return 1;
                        }))))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            double amount = DoubleArgumentType.getDouble(ctx, "amount");
                            
                            EconomyManager.getInstance().setBalance(target.getUUID(), amount).thenAccept(v -> {
                                String formatted = EconomyManager.getInstance().format(amount);
                                
                                ctx.getSource().sendSuccess(() -> Component.literal("§aSet " + target.getName().getString() + "'s balance to " + formatted), false);
                            });
                            return 1;
                        }))))
            .then(Commands.literal("reset")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        
                        EconomyManager.getInstance().setBalance(target.getUUID(), 100.0).thenAccept(v -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aReset " + target.getName().getString() + "'s balance"), false);
                        });
                        return 1;
                    })))
        );
    }
}
