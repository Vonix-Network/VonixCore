package network.vonix.vonixcore.jobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import network.vonix.vonixcore.VonixCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commands for the jobs system.
 */
public class JobsCommands implements CommandExecutor, TabCompleter {

    private final VonixCore plugin;
    private final JobsManager manager;

    public JobsCommands(VonixCore plugin, JobsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only!");
            return true;
        }

        if (args.length == 0) {
            showJobsHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> listJobs(player);
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /jobs join <job>").color(NamedTextColor.RED));
                    return true;
                }
                manager.joinJob(player, args[1]);
            }
            case "leave" -> {
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /jobs leave <job>").color(NamedTextColor.RED));
                    return true;
                }
                manager.leaveJob(player, args[1]);
            }
            case "info" -> {
                if (args.length < 2) {
                    showMyJobs(player);
                } else {
                    showJobInfo(player, args[1]);
                }
            }
            case "stats" -> showMyJobs(player);
            case "browse", "gui" -> openJobsGUI(player);
            case "top" -> showTopPlayers(player, args.length > 1 ? args[1] : null);
            case "reload" -> {
                if (player.hasPermission("vonixcore.jobs.admin")) {
                    manager.reload();
                    player.sendMessage(Component.text("Jobs config reloaded!").color(NamedTextColor.GREEN));
                }
            }
            default -> showJobsHelp(player);
        }

        return true;
    }

    private void showJobsHelp(Player player) {
        player.sendMessage(Component.text("=== Jobs Commands ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/jobs list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - View available jobs").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/jobs join <job>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Join a job").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/jobs leave <job>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Leave a job").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/jobs stats").color(NamedTextColor.YELLOW)
                .append(Component.text(" - View your job progress").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/jobs info <job>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - View job details").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/jobs browse").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Open jobs GUI").color(NamedTextColor.GRAY)));
    }

    private void listJobs(Player player) {
        player.sendMessage(Component.text("=== Available Jobs ===").color(NamedTextColor.GOLD));

        for (Job job : manager.getAllJobs()) {
            player.sendMessage(Component.text("• " + job.getName()).color(NamedTextColor.YELLOW)
                    .append(Component.text(" - " + job.getDescription()).color(NamedTextColor.GRAY)));
        }

        player.sendMessage(Component.text("Use /jobs join <job> to join a job!").color(NamedTextColor.GREEN));
    }

    private void showMyJobs(Player player) {
        List<PlayerJob> myJobs = manager.getPlayerJobs(player.getUniqueId());

        if (myJobs.isEmpty()) {
            player.sendMessage(Component.text("You don't have any jobs! Use /jobs list to see available jobs.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== Your Jobs ===").color(NamedTextColor.GOLD));

        for (PlayerJob pj : myJobs) {
            Job job = manager.getJob(pj.getJobId());
            if (job == null)
                continue;

            double progress = pj.getProgress(job) * 100;
            int progressBars = (int) (progress / 5); // 20 bars total

            String progressBar = "§a" + "█".repeat(progressBars) +
                    "§7" + "█".repeat(20 - progressBars);

            player.sendMessage(Component.text(job.getName() + " ").color(NamedTextColor.YELLOW)
                    .append(Component.text("Lvl " + pj.getLevel()).color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("  [" + progressBar + "§r] " +
                    String.format("%.1f%%", progress)).color(NamedTextColor.GRAY));
        }
    }

    private void showJobInfo(Player player, String jobId) {
        Job job = manager.getJob(jobId);
        if (job == null) {
            player.sendMessage(Component.text("Unknown job: " + jobId).color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== " + job.getName() + " ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text(job.getDescription()).color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("Max Level: " + job.getMaxLevel()).color(NamedTextColor.YELLOW));

        // Show some example actions
        player.sendMessage(Component.text("Actions:").color(NamedTextColor.AQUA));
        for (Job.ActionType actionType : job.getActions().keySet()) {
            player.sendMessage(Component.text("  • " + actionType.name()).color(NamedTextColor.GRAY));
        }
    }

    private void openJobsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27,
                Component.text("Jobs").color(NamedTextColor.DARK_GREEN));

        int slot = 10;
        for (Job job : manager.getAllJobs()) {
            if (slot > 16)
                break;

            ItemStack item = new ItemStack(job.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(job.getName())
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(job.getDescription())
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Max Level: " + job.getMaxLevel())
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());

            // Check if player has this job
            boolean hasJob = manager.getPlayerJobs(player.getUniqueId()).stream()
                    .anyMatch(pj -> pj.getJobId().equalsIgnoreCase(job.getId()));

            if (hasJob) {
                PlayerJob pj = manager.getPlayerJobs(player.getUniqueId()).stream()
                        .filter(p -> p.getJobId().equalsIgnoreCase(job.getId()))
                        .findFirst().orElse(null);
                if (pj != null) {
                    lore.add(Component.text("Your Level: " + pj.getLevel())
                            .color(NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.text("Click to leave")
                        .color(NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Click to join")
                        .color(NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot++, item);
        }

        player.openInventory(gui);
    }

    private void showTopPlayers(Player player, String jobId) {
        // TODO: Implement leaderboard query
        player.sendMessage(Component.text("Job leaderboards coming soon!").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("list", "join", "leave", "stats", "info", "browse", "top", "reload")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") ||
                    args[0].equalsIgnoreCase("leave") ||
                    args[0].equalsIgnoreCase("info") ||
                    args[0].equalsIgnoreCase("top")) {
                return manager.getAllJobs().stream()
                        .map(Job::getId)
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
