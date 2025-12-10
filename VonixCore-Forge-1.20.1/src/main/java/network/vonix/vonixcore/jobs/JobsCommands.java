package network.vonix.vonixcore.jobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Jobs commands for NeoForge.
 */
public class JobsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jobs")
                .then(Commands.literal("list")
                        .executes(JobsCommands::listJobs))
                .then(Commands.literal("join")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    JobsManager.getInstance().getAllJobs().forEach(job -> builder.suggest(job.getId()));
                                    return builder.buildFuture();
                                })
                                .executes(JobsCommands::joinJob)))
                .then(Commands.literal("leave")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    if (ctx.getSource().getPlayer() != null) {
                                        JobsManager.getInstance().getPlayerJobs(
                                                ctx.getSource().getPlayer().getUUID())
                                                .forEach(pj -> builder.suggest(pj.getJobId()));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(JobsCommands::leaveJob)))
                .then(Commands.literal("stats")
                        .executes(JobsCommands::showStats))
                .then(Commands.literal("info")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    JobsManager.getInstance().getAllJobs().forEach(job -> builder.suggest(job.getId()));
                                    return builder.buildFuture();
                                })
                                .executes(JobsCommands::showJobInfo)))
                .executes(JobsCommands::showHelp));

        // Alias
        dispatcher.register(Commands.literal("job")
                .redirect(dispatcher.getRoot().getChild("jobs")));
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("=== Jobs Commands ===")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("/jobs list")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - View available jobs").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/jobs join <job>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Join a job").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/jobs leave <job>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Leave a job").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/jobs stats")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - View your job progress").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("/jobs info <job>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - View job details").withStyle(ChatFormatting.GRAY)), false);
        return 1;
    }

    private static int listJobs(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        JobsManager manager = JobsManager.getInstance();

        source.sendSuccess(() -> Component.literal("=== Available Jobs ===")
                .withStyle(ChatFormatting.GOLD), false);

        for (Job job : manager.getAllJobs()) {
            source.sendSuccess(() -> Component.literal("• " + job.getName())
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" - " + job.getDescription())
                            .withStyle(ChatFormatting.GRAY)),
                    false);
        }

        source.sendSuccess(() -> Component.literal("Use /jobs join <job> to join!")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int joinJob(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command is for players only!"));
            return 0;
        }

        String jobId = StringArgumentType.getString(ctx, "job");
        JobsManager.getInstance().joinJob(player, jobId);
        return 1;
    }

    private static int leaveJob(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command is for players only!"));
            return 0;
        }

        String jobId = StringArgumentType.getString(ctx, "job");
        JobsManager.getInstance().leaveJob(player, jobId);
        return 1;
    }

    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("This command is for players only!"));
            return 0;
        }

        JobsManager manager = JobsManager.getInstance();
        List<PlayerJob> myJobs = manager.getPlayerJobs(player.getUUID());

        if (myJobs.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "You don't have any jobs! Use /jobs list to see available jobs.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== Your Jobs ===")
                .withStyle(ChatFormatting.GOLD), false);

        for (PlayerJob pj : myJobs) {
            Job job = manager.getJob(pj.getJobId());
            if (job == null)
                continue;

            double progress = pj.getProgress(job) * 100;
            int progressBars = (int) (progress / 5); // 20 bars total

            String progressBar = "§a" + "█".repeat(progressBars) +
                    "§7" + "█".repeat(20 - progressBars);

            ctx.getSource().sendSuccess(() -> Component.literal(job.getName() + " ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("Lvl " + pj.getLevel())
                            .withStyle(ChatFormatting.GREEN)),
                    false);
            ctx.getSource().sendSuccess(() -> Component.literal("  [" + progressBar + "§r] " +
                    String.format("%.1f%%", progress)).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int showJobInfo(CommandContext<CommandSourceStack> ctx) {
        String jobId = StringArgumentType.getString(ctx, "job");
        JobsManager manager = JobsManager.getInstance();
        Job job = manager.getJob(jobId);

        if (job == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown job: " + jobId));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== " + job.getName() + " ===")
                .withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(() -> Component.literal(job.getDescription())
                .withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Max Level: " + job.getMaxLevel())
                .withStyle(ChatFormatting.YELLOW), false);

        ctx.getSource().sendSuccess(() -> Component.literal("Actions:")
                .withStyle(ChatFormatting.AQUA), false);
        for (Job.ActionType actionType : job.getActions().keySet()) {
            ctx.getSource().sendSuccess(() -> Component.literal("  • " + actionType.name())
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}
