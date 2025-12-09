package network.vonix.vonixcore.warps;

import network.vonix.vonixcore.config.EssentialsConfig;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class WarpCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!EssentialsConfig.warpsEnabled) {
            sender.sendMessage(ChatColor.RED + "Warp system is disabled.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setwarp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can set warps.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /setwarp <name>");
                return true;
            }
            if (WarpManager.getInstance().setWarp(args[0], player)) {
                sender.sendMessage(ChatColor.GREEN + "Warp '" + args[0] + "' set!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to set warp.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("delwarp")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /delwarp <name>");
                return true;
            }
            if (WarpManager.getInstance().deleteWarp(args[0])) {
                sender.sendMessage(ChatColor.GREEN + "Warp '" + args[0] + "' deleted!");
            } else {
                sender.sendMessage(ChatColor.RED + "Warp not found.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("warp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can warp.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /warp <name>");
                return true;
            }
            Location loc = WarpManager.getInstance().getWarp(args[0]);
            if (loc != null) {
                player.teleport(loc);
                sender.sendMessage(ChatColor.GREEN + "Teleported to '" + args[0] + "'!");
            } else {
                sender.sendMessage(ChatColor.RED + "Warp not found.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("warps")) {
            List<String> warps = WarpManager.getInstance().getWarps();
            if (warps.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No warps set.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Warps: " + ChatColor.WHITE + String.join(", ", warps));
            }
            return true;
        }

        return false;
    }
}
