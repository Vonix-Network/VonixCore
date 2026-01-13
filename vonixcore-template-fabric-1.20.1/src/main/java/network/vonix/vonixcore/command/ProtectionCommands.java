package network.vonix.vonixcore.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Protection-related commands like /co lookup, /co rollback.
 */
public class ProtectionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("co")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§6VonixCore Protection Commands:"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/co i §7- Toggle inspector mode"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/co l §7- Lookup block history"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/co rollback §7- Rollback changes"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/co restore §7- Restore changes"), false);
                    return 1;
                })
                .then(Commands.literal("i")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aInspector mode toggled (coming soon)"), false);
                            return 1;
                        }))
                .then(Commands.literal("l")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aLookup mode (coming soon)"), false);
                            return 1;
                        }))
                .then(Commands.literal("lookup")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aLookup (coming soon)"), false);
                            return 1;
                        }))
                .then(Commands.literal("rollback")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aRollback (coming soon)"), false);
                            return 1;
                        }))
                .then(Commands.literal("restore")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aRestore (coming soon)"), false);
                            return 1;
                        })));
    }
}
