package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.config.EssentialsConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!EssentialsConfig.economyEnabled) {
            sender.sendMessage(ChatColor.RED + "Economy system is disabled.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("balance") || command.getName().equalsIgnoreCase("bal")
                || command.getName().equalsIgnoreCase("money")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can check balance.");
                return true;
            }
            double bal = EconomyManager.getInstance().getBalance(player.getUniqueId());
            sender.sendMessage(
                    ChatColor.GOLD + "Balance: " + ChatColor.WHITE + EconomyManager.getInstance().format(bal));
            return true;
        }

        if (command.getName().equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can pay.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /pay <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You cannot pay yourself.");
                return true;
            }

            try {
                double amount = Double.parseDouble(args[1]);
                if (EconomyManager.getInstance().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
                    sender.sendMessage(ChatColor.GREEN + "Sent " + EconomyManager.getInstance().format(amount) + " to "
                            + target.getName());
                    target.sendMessage(ChatColor.GREEN + "Received " + EconomyManager.getInstance().format(amount)
                            + " from " + player.getName());
                } else {
                    sender.sendMessage(ChatColor.RED + "Transaction failed (insufficient funds?).");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount.");
            }
            return true;
        }

        return false;
    }
}
