package network.vonix.vonixcore.teleport;

import network.vonix.vonixcore.config.EssentialsConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeleportCommands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use TPA commands.");
            return true;
        }

        if (!EssentialsConfig.tpaEnabled) {
            sender.sendMessage(ChatColor.RED + "TPA system is disabled.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "You cannot TPA to yourself.");
                return true;
            }

            if (TeleportManager.getInstance().sendTpaRequest(player, target, false)) {
                sender.sendMessage(ChatColor.GREEN + "TPA request sent to " + target.getName());
                target.sendMessage(
                        ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " requested to teleport to you.");
                target.sendMessage(
                        ChatColor.YELLOW + "Type " + ChatColor.GREEN + "/tpaccept" + ChatColor.YELLOW + " to accept.");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to send request (already pending?).");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpaccept")) {
            if (TeleportManager.getInstance().acceptTpaRequest(player)) {
                sender.sendMessage(ChatColor.GREEN + "Request accepted.");
            } else {
                sender.sendMessage(ChatColor.RED + "No pending request expired.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpdeny")) {
            if (TeleportManager.getInstance().denyTpaRequest(player)) {
                sender.sendMessage(ChatColor.GREEN + "Request denied.");
            } else {
                sender.sendMessage(ChatColor.RED + "No pending request.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("back")) {
            network.vonix.vonixcore.teleport.TeleportManager.TeleportLocation lastInfo = TeleportManager.getInstance()
                    .getLastLocation(player.getUniqueId());

            if (lastInfo == null || lastInfo.location() == null) {
                sender.sendMessage(ChatColor.RED + "[VC] No location to return to.");
                return true;
            }

            // Check timeout
            int timeoutSeconds;
            if (lastInfo.isDeath()) {
                timeoutSeconds = EssentialsConfig.deathBackTimeout;
            } else {
                timeoutSeconds = EssentialsConfig.backTimeout;
            }

            if (timeoutSeconds > 0) {
                long elapsed = (System.currentTimeMillis() - lastInfo.timestamp()) / 1000;
                if (elapsed > timeoutSeconds) {
                    sender.sendMessage(ChatColor.RED + "[VC] Your previous location has expired (" + timeoutSeconds
                            + "s timeout).");
                    return true;
                }
            }

            // Save current location before teleporting (so /back can be used again) which
            // naturally has isDeath=false
            TeleportManager.getInstance().saveLastLocation(player);
            player.teleport(lastInfo.location());
            sender.sendMessage(ChatColor.GREEN + "[VC] Returned to previous location.");
            return true;
        }

        return false;
    }
}
