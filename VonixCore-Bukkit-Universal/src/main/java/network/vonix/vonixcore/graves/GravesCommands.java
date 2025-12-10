package network.vonix.vonixcore.graves;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commands for the graves system
 */
public class GravesCommands implements CommandExecutor, TabCompleter {
    private final VonixCore plugin;
    private final GravesManager gravesManager;

    public GravesCommands(VonixCore plugin, GravesManager gravesManager) {
        this.plugin = plugin;
        this.gravesManager = gravesManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            showGraves(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> showGraves(player);
            case "teleport", "tp" -> teleportToGrave(player, args);
            case "info" -> showGraveInfo(player);
            default -> showHelp(player);
        }

        return true;
    }

    private void showGraves(Player player) {
        List<Grave> graves = gravesManager.getPlayerGraves(player.getUniqueId());

        if (graves.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no active graves.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "═══ Your Graves (" + graves.size() + ") ═══");

        int index = 1;
        for (Grave grave : graves) {
            String status = grave.isExpired() ? ChatColor.RED + "EXPIRED"
                    : ChatColor.GREEN + grave.getTimeRemainingFormatted();

            player.sendMessage(ChatColor.GRAY + "[" + index + "] " +
                    ChatColor.WHITE + formatLocation(grave.getLocation()) +
                    ChatColor.GRAY + " - " + status);
            index++;
        }

        player.sendMessage(ChatColor.GRAY + "Use /graves tp <number> to teleport");
    }

    private void teleportToGrave(Player player, String[] args) {
        if (!player.hasPermission("vonixcore.graves.teleport")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to teleport to graves.");
            return;
        }

        List<Grave> graves = gravesManager.getPlayerGraves(player.getUniqueId());

        if (graves.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no active graves.");
            return;
        }

        int index = 1;
        if (args.length > 1) {
            try {
                index = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid grave number.");
                return;
            }
        }

        if (index < 1 || index > graves.size()) {
            player.sendMessage(ChatColor.RED + "Grave number must be between 1 and " + graves.size());
            return;
        }

        Grave grave = graves.get(index - 1);
        Location teleportLoc = grave.getLocation().clone().add(0.5, 1, 0.5);
        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to your grave!");
    }

    private void showGraveInfo(Player player) {
        List<Grave> graves = gravesManager.getPlayerGraves(player.getUniqueId());
        int totalGraves = gravesManager.getGraveCount();

        player.sendMessage(ChatColor.GOLD + "═══ Graves Info ═══");
        player.sendMessage(ChatColor.GRAY + "Your graves: " + ChatColor.WHITE + graves.size());
        player.sendMessage(ChatColor.GRAY + "Total graves on server: " + ChatColor.WHITE + totalGraves);
        player.sendMessage(ChatColor.GRAY + "Graves enabled: " +
                (gravesManager.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══ Graves Commands ═══");
        player.sendMessage(ChatColor.GRAY + "/graves" + ChatColor.WHITE + " - List your graves");
        player.sendMessage(ChatColor.GRAY + "/graves list" + ChatColor.WHITE + " - List your graves");
        player.sendMessage(ChatColor.GRAY + "/graves tp [number]" + ChatColor.WHITE + " - Teleport to a grave");
        player.sendMessage(ChatColor.GRAY + "/graves info" + ChatColor.WHITE + " - Show graves info");
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + " " +
                location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("list", "teleport", "tp", "info"), args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
