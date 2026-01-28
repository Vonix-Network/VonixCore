package network.vonix.vonixcore.jobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Jobs commands for Forge 1.18.2.
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
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        JobsManager.getInstance().getPlayerJobs(player.getUUID())
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
        source.sendSuccess(new TextComponent("=== Jobs Commands ===")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(new TextComponent("/jobs list")
                .withStyle(ChatFormatting.YELLOW)
                .append(new TextComponent(" - View available jobs").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(new TextComponent("/jobs join <job>")
                .withStyle(ChatFormatting.YELLOW)
                .append(new TextComponent(" - Join a job").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(new TextComponent("/jobs leave <job>")
                .withStyle(ChatFormatting.YELLOW)
                .append(new TextComponent(" - Leave a job").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(new TextComponent("/jobs stats")
                .withStyle(ChatFormatting.YELLOW)
                .append(new TextComponent(" - View your job progress").withStyle(ChatFormatting.GRAY)), false);
        source.sendSuccess(new TextComponent("/jobs info <job>")
                .withStyle(ChatFormatting.YELLOW)
                .append(new TextComponent(" - View job details").withStyle(ChatFormatting.GRAY)), false);
        return 1;
    }

    private static int listJobs(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        JobsManager manager = JobsManager.getInstance();

        source.sendSuccess(new TextComponent("=== Available Jobs ===")
                .withStyle(ChatFormatting.GOLD), false);

        for (Job job : manager.getAllJobs()) {
            source.sendSuccess(new TextComponent("• " + job.getName())
                    .withStyle(ChatFormatting.YELLOW)
                    .append(new TextComponent(" - " + job.getDescription())
                            .withStyle(ChatFormatting.GRAY)),
                    false);
        }

        source.sendSuccess(new TextComponent("Use /jobs join <job> to join!")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int joinJob(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(new TextComponent("This command is for players only!"));
            return 0;
        }

        String jobId = StringArgumentType.getString(ctx, "job");
        JobsManager.getInstance().joinJob(player, jobId);
        return 1;
    }

    private static int leaveJob(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(new TextComponent("This command is for players only!"));
            return 0;
        }

        String jobId = StringArgumentType.getString(ctx, "job");
        JobsManager.getInstance().leaveJob(player, jobId);
        return 1;
    }

    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(new TextComponent("This command is for players only!"));
            return 0;
        }

        JobsManager manager = JobsManager.getInstance();
        List<PlayerJob> myJobs = manager.getPlayerJobs(player.getUUID());

        if (myJobs.isEmpty()) {
            ctx.getSource().sendSuccess(new TextComponent(
                    "You don't have any jobs! Use /jobs list to see available jobs.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        ctx.getSource().sendSuccess(new TextComponent("=== Your Jobs ===")
                .withStyle(ChatFormatting.GOLD), false);

        for (PlayerJob pj : myJobs) {
            Job job = manager.getJob(pj.getJobId());
            if (job == null)
                continue;

            double progress = pj.getProgress(job) * 100;
            int progressBars = (int) (progress / 5); // 20 bars total

            String progressBar = "§a" + "█".repeat(progressBars) +
                    "§7" + "█".repeat(20 - progressBars);

            ctx.getSource().sendSuccess(new TextComponent(job.getName() + " ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(new TextComponent("Lvl " + pj.getLevel())
                            .withStyle(ChatFormatting.GREEN)),
                    false);
            ctx.getSource().sendSuccess(new TextComponent("  [" + progressBar + "§r] " +
                    String.format("%.1f%%", progress)).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int showJobInfo(CommandContext<CommandSourceStack> ctx) {
        String jobId = StringArgumentType.getString(ctx, "job");
        JobsManager manager = JobsManager.getInstance();
        Job job = manager.getJob(jobId);

        if (job == null) {
            ctx.getSource().sendFailure(new TextComponent("Unknown job: " + jobId));
            return 0;
        }

        ctx.getSource().sendSuccess(new TextComponent("=== " + job.getName() + " ===")
                .withStyle(ChatFormatting.GOLD), false);
        ctx.getSource().sendSuccess(new TextComponent(job.getDescription())
                .withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(new TextComponent("Max Level: " + job.getMaxLevel())
                .withStyle(ChatFormatting.YELLOW), false);

        ctx.getSource().sendSuccess(new TextComponent("Actions:")
                .withStyle(ChatFormatting.AQUA), false);
        for (Job.ActionType actionType : job.getActions().keySet()) {
            ctx.getSource().sendSuccess(new TextComponent("  • " + actionType.name())
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }
}
