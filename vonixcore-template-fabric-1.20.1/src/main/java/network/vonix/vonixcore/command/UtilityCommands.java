package network.vonix.vonixcore.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Utility commands like /spawn, /msg, etc.
 */
public class UtilityCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /spawn - placeholder
        dispatcher.register(Commands.literal("spawn")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§7Spawn command coming soon!"), false);
                    return 1;
                }));

        // /discord
        dispatcher.register(Commands.literal("discord")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(
                            () -> Component
                                    .literal("§bJoin our Discord: §f(configure invite URL in vonixcore-discord.yml)"),
                            false);
                    return 1;
                }));
    }
}
