package network.vonix.vonixcore.jobs;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Commands for the jobs system.
 */
public class JobsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jobs")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§6Jobs Help:"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/jobs list §7- List available jobs"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/jobs join <job> §7- Join a job"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/jobs leave §7- Leave your current job"),
                            false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/jobs info <job> §7- Get job information"),
                            false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/jobs stats §7- View your job stats"),
                            false);
                    return 1;
                })
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§6Available Jobs:"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§e- Miner §7(Mine ores for money)"),
                                    false);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§e- Farmer §7(Harvest crops for money)"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§e- Hunter §7(Kill mobs for money)"),
                                    false);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§e- Woodcutter §7(Chop trees for money)"), false);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§e- Fisherman §7(Catch fish for money)"), false);
                            return 1;
                        })));
    }
}
