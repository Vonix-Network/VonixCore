package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Authentication commands: /login, /register
 */
public class AuthCommands implements CommandExecutor, TabCompleter {

    private final VonixCore plugin;

    public AuthCommands(VonixCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("login")) {
            return handleLogin(player, args);
        } else if (cmdName.equals("register")) {
            return handleRegister(player, args);
        }

        return false;
    }

    private boolean handleLogin(Player player, String[] args) {
        if (AuthenticationManager.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(AuthConfig.alreadyAuthenticatedMessage);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /login <password>");
            return true;
        }

        String password = String.join(" ", args);
        String username = player.getName();
        String uuid = player.getUniqueId().toString();

        player.sendMessage(AuthConfig.authenticatingMessage);

        VonixNetworkAPI.loginPlayer(username, uuid, password)
                .thenAccept(response -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (response.success) {
                            AuthenticationManager.setAuthenticated(player.getUniqueId(), response.token);
                            String msg = AuthConfig.authenticationSuccessMessage.replace("{username}", username);
                            player.sendMessage(msg);

                            if (response.user != null && response.user.donation_rank != null) {
                                player.sendMessage("§6★ §7Rank: §e" + response.user.donation_rank.name);
                            }
                        } else {
                            String error = response.error != null ? response.error : "Unknown error";
                            String msg = AuthConfig.loginFailedMessage.replace("{error}", error);
                            player.sendMessage(msg);
                        }
                    });
                });

        return true;
    }

    private boolean handleRegister(Player player, String[] args) {
        if (AuthenticationManager.isAuthenticated(player.getUniqueId())) {
            player.sendMessage(AuthConfig.alreadyAuthenticatedMessage);
            return true;
        }

        String username = player.getName();
        String uuid = player.getUniqueId().toString();

        if (args.length == 0) {
            // Generate registration code
            player.sendMessage(AuthConfig.generatingCodeMessage);
            AuthenticationManager.setPendingRegistration(player.getUniqueId());

            VonixNetworkAPI.generateRegistrationCode(username, uuid)
                    .thenAccept(response -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (response.code != null) {
                                String msg = AuthConfig.registrationCodeMessage.replace("{code}", response.code);
                                player.sendMessage(msg);
                                player.sendMessage("§a§l[CLICK HERE] §6" + AuthConfig.registrationUrl);
                                player.sendMessage("§7Or use: §e/register <password>");
                            } else if (response.already_registered) {
                                player.sendMessage("§eAlready registered! Use §a/login <password>");
                            } else {
                                player.sendMessage("§cRegistration failed: " +
                                        (response.error != null ? response.error : "Unknown error"));
                            }
                        });
                    });
        } else {
            // Register with password
            String password = String.join(" ", args);
            player.sendMessage("§6⏳ §7Registering account...");
            AuthenticationManager.setPendingRegistration(player.getUniqueId());

            VonixNetworkAPI.registerPlayerWithPassword(username, uuid, password)
                    .thenAccept(response -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (response.success) {
                                AuthenticationManager.setAuthenticated(player.getUniqueId(), response.token);
                                player.sendMessage("§a§l✓ §7Account created! Welcome, §e" + username);
                            } else {
                                String error = response.error != null ? response.error : "Unknown error";
                                if (error.toLowerCase().contains("already registered")) {
                                    player.sendMessage("§eAlready registered! Use §a/login <password>");
                                } else {
                                    player.sendMessage("§c§l✗ §7Registration failed: §c" + error);
                                }
                            }
                        });
                    });
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>(); // Don't suggest passwords
    }
}
