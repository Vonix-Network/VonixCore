package network.vonix.vonixcore.homes;

import network.vonix.vonixcore.config.EssentialsConfig;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use home commands.");
            return true;
        }

        if (!EssentialsConfig.homesEnabled) {
            sender.sendMessage(ChatColor.RED + "Home system is disabled.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("sethome")) {
            String name = args.length > 0 ? args[0] : "home";
            if (HomeManager.getInstance().setHome(player, name)) {
                sender.sendMessage(ChatColor.GREEN + "Home '" + name + "' set!");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to set home. Limit reached or name invalid.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("delhome")) {
            String name = args.length > 0 ? args[0] : "home";
            if (HomeManager.getInstance().deleteHome(player.getUniqueId(), name)) {
                sender.sendMessage(ChatColor.GREEN + "Home '" + name + "' deleted!");
            } else {
                sender.sendMessage(ChatColor.RED + "Home not found.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            String name = args.length > 0 ? args[0] : "home";
            Location loc = HomeManager.getInstance().getHome(player.getUniqueId(), name);
            if (loc != null) {
                player.teleport(loc);
                sender.sendMessage(ChatColor.GREEN + "Teleported to '" + name + "'!");
            } else {
                sender.sendMessage(ChatColor.RED + "Home '" + name + "' not found.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("homes")) {
            List<String> homes = HomeManager.getInstance().getHomes(player.getUniqueId());
            if (homes.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "You have no homes set.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "Homes: " + ChatColor.WHITE + String.join(", ", homes));
            }
            return true;
        }

        return false;
    }
}
