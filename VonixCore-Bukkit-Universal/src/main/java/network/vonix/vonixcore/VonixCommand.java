package network.vonix.vonixcore;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class VonixCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "VonixCore " + ChatColor.GRAY + "v"
                + VonixCore.getInstance().getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "Universal Bukkit Port");
        return true;
    }
}
