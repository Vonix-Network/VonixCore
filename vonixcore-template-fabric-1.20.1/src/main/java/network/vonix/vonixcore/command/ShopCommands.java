package network.vonix.vonixcore.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Shop-related commands.
 */
public class ShopCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§6Shop Commands:"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/shop open §7- Open the server shop"),
                            false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/shop create §7- Create a player shop"),
                            false);
                    return 1;
                })
                .then(Commands.literal("open")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aServer shop GUI coming soon!"),
                                    false);
                            return 1;
                        }))
                .then(Commands.literal("create")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aPlayer shops coming soon!"), false);
                            return 1;
                        })));
    }
}
