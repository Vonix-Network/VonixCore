package network.vonix.vonixcore.auth;

import network.vonix.vonixcore.config.AuthConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AuthCommands implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!AuthConfig.enabled) {
            sender.sendMessage("§cAuthentication module is disabled.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is only for players.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "register":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /register <password> <confirm>");
                    return true;
                }
                if (!args[0].equals(args[1])) {
                    player.sendMessage("§cPasswords do not match!");
                    return true;
                }
                AuthManager.getInstance().register(player, args[0]);
                break;

            case "login":
                if (args.length < 1) {
                    player.sendMessage("§cUsage: /login <password>");
                    return true;
                }
                AuthManager.getInstance().login(player, args[0]);
                break;

            case "changepassword":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /changepassword <oldPassword> <newPassword>");
                    return true;
                }
                // Simplified for now - usually requires old password verification
                // For this port, we'll assume the user is logged in (handled by listener
                // usually, but logic needed here)
                if (!AuthManager.getInstance().isLoggedIn(player)) {
                    player.sendMessage("§cYou must be logged in to change your password.");
                    return true;
                }
                // TODO: Implement explicit change password logic in Manager
                player.sendMessage("§cChange password not fully implemented yet.");
                break;

            case "logout":
                AuthManager.getInstance().logout(player);
                break;
        }

        return true;
    }
}
