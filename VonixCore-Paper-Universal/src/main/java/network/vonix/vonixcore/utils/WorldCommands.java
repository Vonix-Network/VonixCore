package network.vonix.vonixcore.utils;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldCommands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        // --- Weather ---
        if (cmd.equals("weather") || cmd.equals("sun") || cmd.equals("rain") || cmd.equals("storm")) {
            if (!sender.hasPermission("vonixcore.weather")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            World world = (player != null) ? player.getWorld() : Bukkit.getWorlds().get(0);
            if (args.length > 0 && (cmd.equals("weather"))) {
                String type = args[0].toLowerCase();
                if (type.equals("clear") || type.equals("sun")) {
                    world.setStorm(false);
                    world.setThundering(false);
                    sender.sendMessage("§eWeather set to clear.");
                } else if (type.equals("rain")) {
                    world.setStorm(true);
                    world.setThundering(false);
                    sender.sendMessage("§eWeather set to rain.");
                } else if (type.equals("storm") || type.equals("thunder")) {
                    world.setStorm(true);
                    world.setThundering(true);
                    sender.sendMessage("§eWeather set to storm.");
                } else {
                    sender.sendMessage("§cUsage: /weather <clear|rain|storm>");
                }
            } else if (cmd.equals("sun")) {
                world.setStorm(false);
                world.setThundering(false);
                sender.sendMessage("§eWeather set to clear.");
            } else if (cmd.equals("rain")) {
                world.setStorm(true);
                world.setThundering(false);
                sender.sendMessage("§eWeather set to rain.");
            } else if (cmd.equals("storm")) {
                world.setStorm(true);
                world.setThundering(true);
                sender.sendMessage("§eWeather set to storm.");
            }
            return true;
        }

        // --- Time ---
        if (cmd.equals("time") || cmd.equals("day") || cmd.equals("night")) {
            if (!sender.hasPermission("vonixcore.time")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            World world = (player != null) ? player.getWorld() : Bukkit.getWorlds().get(0);
            if (cmd.equals("day")) {
                world.setTime(1000);
                sender.sendMessage("§eTime set to day.");
                return true;
            }
            if (cmd.equals("night")) {
                world.setTime(13000);
                sender.sendMessage("§eTime set to night.");
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage("§cUsage: /time <day|night|noon|midnight|ticks>");
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("set")) { // Support /time set day
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /time set <type>");
                    return true;
                }
                sub = args[1].toLowerCase();
            }

            long time;
            switch (sub) {
                case "day":
                    time = 1000;
                    break;
                case "night":
                    time = 13000;
                    break;
                case "noon":
                    time = 6000;
                    break;
                case "midnight":
                    time = 18000;
                    break;
                default:
                    try {
                        time = Long.parseLong(sub);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid time format.");
                        return true;
                    }
            }
            world.setTime(time);
            sender.sendMessage("§eTime set to " + time);
            return true;
        }

        // --- Player States ---

        if (cmd.equals("heal")) {
            if (!sender.hasPermission("vonixcore.heal")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            Player target = player;
            if (args.length > 0 && sender.hasPermission("vonixcore.heal.others")) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
            }
            if (target == null) {
                sender.sendMessage("§cUsage: /heal <player>"); // For console
                return true;
            }
            target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            target.setFoodLevel(20);
            target.setSaturation(20);
            target.setFireTicks(0);
            sender.sendMessage("§aHealed " + target.getName());
            return true;
        }

        if (cmd.equals("feed")) {
            if (!sender.hasPermission("vonixcore.feed")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            Player target = player;
            if (args.length > 0 && sender.hasPermission("vonixcore.feed.others")) {
                target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
            }
            if (target == null) {
                sender.sendMessage("§cUsage: /feed <player>");
                return true;
            }
            target.setFoodLevel(20);
            target.setSaturation(20);
            sender.sendMessage("§aFed " + target.getName());
            return true;
        }

        if (cmd.equals("fly")) {
            if (player == null)
                return true;
            if (!player.hasPermission("vonixcore.fly")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            // Toggle
            if (args.length > 0 && player.hasPermission("vonixcore.fly.others")) { // Optional: support others
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
                target.setAllowFlight(!target.getAllowFlight());
                player.sendMessage(
                        "§aToggled flight for " + target.getName() + ": " + (target.getAllowFlight() ? "ON" : "OFF"));
                return true;
            }

            player.setAllowFlight(!player.getAllowFlight());
            player.sendMessage("§aFlight: " + (player.getAllowFlight() ? "ON" : "OFF"));
            return true;
        }

        if (cmd.equals("god")) {
            if (player == null)
                return true;
            if (!player.hasPermission("vonixcore.god")) {
                player.sendMessage("§cNo permission.");
                return true;
            }
            boolean currentState = UtilsManager.getInstance().isGod(player.getUniqueId());
            UtilsManager.getInstance().setGod(player.getUniqueId(), !currentState);
            player.sendMessage("§aGod mode: " + (!currentState ? "ON" : "OFF"));
            if (!currentState)
                player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            return true;
        }

        if (cmd.equals("afk")) {
            if (player == null)
                return true;
            // Simple toggle
            boolean isAfk = UtilsManager.getInstance().isAfk(player.getUniqueId());
            UtilsManager.getInstance().setAfk(player.getUniqueId(), !isAfk);
            Bukkit.broadcastMessage("§7* " + player.getName() + " is now " + (!isAfk ? "AFK" : "no longer AFK"));
            return true;
        }

        return true;
    }
}
