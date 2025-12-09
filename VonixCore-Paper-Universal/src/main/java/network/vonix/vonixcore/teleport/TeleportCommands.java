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

        if (command.getName().equalsIgnoreCase("rtp")) {
            if (!sender.hasPermission("vonixcore.rtp")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (TeleportManager.getInstance().isOnCooldown(player.getUniqueId())
                    && !player.hasPermission("vonixcore.bypass.cooldown")) {
                sender.sendMessage(ChatColor.RED + "You must wait "
                        + TeleportManager.getInstance().getRemainingCooldown(player.getUniqueId()) + "s.");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Finding safe location...");
            TeleportManager.getInstance().findSafeLocation(player.getLocation()).ifPresentOrElse(loc -> {
                TeleportManager.getInstance().teleportPlayer(player, loc);
                TeleportManager.getInstance().setCooldown(player.getUniqueId(), 30); // 30s cooldown default
                player.sendMessage(ChatColor.GREEN + "Teleported to random location!");
            }, () -> player.sendMessage(ChatColor.RED + "Could not find a safe location nearby."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("tphere")) {
            if (!sender.hasPermission("vonixcore.tphere")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /tphere <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            TeleportManager.getInstance().teleportPlayer(target, player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Teleported " + target.getName() + " to you.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpall")) {
            if (!sender.hasPermission("vonixcore.tpall")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getUniqueId().equals(player.getUniqueId())) {
                    TeleportManager.getInstance().teleportPlayer(p, player.getLocation());
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Teleported all players to you.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("tppos")) {
            if (!sender.hasPermission("vonixcore.tppos")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /tppos <x> <y> <z> [yaw] [pitch]");
                return true;
            }
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                float yaw = args.length > 3 ? Float.parseFloat(args[3]) : player.getLocation().getYaw();
                float pitch = args.length > 4 ? Float.parseFloat(args[4]) : player.getLocation().getPitch();

                org.bukkit.Location loc = new org.bukkit.Location(player.getWorld(), x, y, z, yaw, pitch);
                TeleportManager.getInstance().teleportPlayer(player, loc);
                sender.sendMessage(ChatColor.GREEN + "Teleported to coordinates.");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid coordinates.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!sender.hasPermission("vonixcore.setspawn")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            player.getWorld().setSpawnLocation(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "World spawn set to your location.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            TeleportManager.getInstance().teleportPlayer(player, player.getWorld().getSpawnLocation());
            sender.sendMessage(ChatColor.GREEN + "Teleported to spawn.");
            return true;
        }

        return false;
    }
}
