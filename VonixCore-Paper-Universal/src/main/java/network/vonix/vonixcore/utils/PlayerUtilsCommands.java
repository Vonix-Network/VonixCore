package network.vonix.vonixcore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PlayerUtilsCommands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            // Allow console for some, but mostly player utilities
            if (command.getName().equalsIgnoreCase("seen") || command.getName().equalsIgnoreCase("whois")) {
                // handle console logic
            } else {
                sender.sendMessage("§cFor players only.");
                return true;
            }
        }

        Player p = (sender instanceof Player) ? (Player) sender : null;

        if (command.getName().equalsIgnoreCase("nick")) {
            if (p == null)
                return true;
            if (!p.hasPermission("vonixcore.nick")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 1) {
                p.sendMessage("§cUsage: /nick <name|off>");
                return true;
            }
            if (args[0].equalsIgnoreCase("off")) {
                UtilsManager.getInstance().removeNick(p.getUniqueId());
                p.displayName(Component.text(p.getName()));
                p.playerListName(Component.text(p.getName()));
                p.sendMessage("§aNickname removed.");
            } else {
                String nick = args[0];
                // Support color codes via MiniMessage or legacy ampersand
                // For simplicity, just store raw string and let listener deserialize
                UtilsManager.getInstance().setNick(p.getUniqueId(), nick);
                try {
                    Component nickComponent = MiniMessage.miniMessage().deserialize(nick);
                    p.displayName(nickComponent);
                    p.playerListName(nickComponent);
                    p.sendMessage("§aNickname set to " + nick);
                } catch (Exception e) {
                    p.sendMessage("§cInvalid format.");
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("hat")) {
            if (p == null)
                return true;
            if (!p.hasPermission("vonixcore.hat")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                p.sendMessage("§cYou must hold an item.");
                return true;
            }
            ItemStack head = p.getInventory().getHelmet();
            p.getInventory().setHelmet(hand);
            p.getInventory().setItemInMainHand(head);
            p.sendMessage("§aEnjoy your new hat!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("repair")) {
            if (p == null)
                return true;
            if (!p.hasPermission("vonixcore.repair")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                p.sendMessage("§cYou must hold an item.");
                return true;
            }
            // Basic repair (remove damage)
            if (hand.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
                damageable.setDamage(0);
                hand.setItemMeta(damageable);
                p.sendMessage("§aItem repaired.");
            } else {
                p.sendMessage("§cItem cannot be repaired.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("more")) {
            if (p == null)
                return true;
            if (!p.hasPermission("vonixcore.more")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                p.sendMessage("§cYou must hold an item.");
                return true;
            }
            hand.setAmount(hand.getMaxStackSize());
            p.sendMessage("§aStack filled.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("playtime")) {
            if (p == null)
                return true;
            // Can view others logic if args > 0
            Player target = p;
            if (args.length > 0 && p.hasPermission("vonixcore.playtime.others")) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    p.sendMessage("§cPlayer not found.");
                    return true;
                }
            }
            long ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long hours = ticks / 20 / 3600;
            long minutes = (ticks / 20 / 60) % 60;
            p.sendMessage("§aPlaytime: " + hours + "h " + minutes + "m");
            return true;
        }

        if (command.getName().equalsIgnoreCase("whois")) {
            if (!sender.hasPermission("vonixcore.whois")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("§cUsage: /whois <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            // Basic whois
            sender.sendMessage("§6--- Whois " + target.getName() + " ---");
            sender.sendMessage("§eUUID: §f" + target.getUniqueId());
            sender.sendMessage("§eIP: §f" + target.getAddress().getAddress().getHostAddress());
            sender.sendMessage("§eGamemode: §f" + target.getGameMode());
            sender.sendMessage("§eLocation: §f" + target.getLocation().getBlockX() + ", "
                    + target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ());
            sender.sendMessage("§eHealth: §f" + String.format("%.1f", target.getHealth()) + "/20");
            return true;
        }

        if (command.getName().equalsIgnoreCase("seen")) {
            if (args.length < 1) {
                sender.sendMessage("§cUsage: /seen <player>");
                return true;
            }
            // Use OfflinePlayer
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.isOnline()) {
                sender.sendMessage("§a" + target.getName() + " is online now!");
            } else {
                if (target.hasPlayedBefore()) {
                    long lastPlayed = target.getLastPlayed();
                    String date = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(lastPlayed));
                    sender.sendMessage("§e" + target.getName() + " was last seen: " + date);
                } else {
                    sender.sendMessage("§cPlayer never played before.");
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("clear")) {
            if (p != null && !p.hasPermission("vonixcore.clear")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            Player target = p;
            if (args.length > 0 && sender.hasPermission("vonixcore.clear.others")) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
            }
            if (target == null) { // Console calling clear without args
                sender.sendMessage("Usage: /clear <player>");
                return true;
            }
            target.getInventory().clear();
            target.getInventory().setArmorContents(null);
            sender.sendMessage("§aInventory cleared for " + target.getName());
            return true;
        }

        return true;
    }
}
