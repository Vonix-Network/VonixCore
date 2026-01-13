package network.vonix.vonixcore.auth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.auth.api.VonixNetworkAPI;
import network.vonix.vonixcore.config.AuthConfig;

/**
 * Authentication commands: /login, /register
 */
public class AuthCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /register [password]
        dispatcher.register(Commands.literal("register")
                .executes(AuthCommands::registerCode)
                .then(Commands.argument("password", StringArgumentType.greedyString())
                        .executes(AuthCommands::registerWithPassword)));

        // /login <password>
        dispatcher.register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.greedyString())
                        .executes(AuthCommands::login)));
    }

    private static int registerCode(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cPlayers only"));
            return 0;
        }

        if (AuthenticationManager.isAuthenticated(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cYou are already authenticated!"));
            return 0;
        }

        String username = player.getName().getString();
        String uuid = player.getUUID().toString();

        player.sendSystemMessage(Component.literal("§6Generating registration code..."));
        AuthenticationManager.setPendingRegistration(player.getUUID());

        VonixNetworkAPI.generateRegistrationCode(username, uuid)
                .thenAccept(response -> {
                    if (response.code != null) {
                        player.sendSystemMessage(Component.literal("§aYour code: §f" + response.code));

                        Component link = Component.literal("§a§l[CLICK HERE] §6Open Registration")
                                .setStyle(Style.EMPTY
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                                AuthConfig.getInstance().getVerificationApiUrl()))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("Open website"))));
                        player.sendSystemMessage(link);
                        player.sendSystemMessage(Component.literal("§7Or use: §e/register <password>"));
                    } else if (response.already_registered) {
                        player.sendSystemMessage(Component.literal("§eAlready registered! Use §a/login <password>"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cRegistration failed: "
                                + (response.error != null ? response.error : "Unknown error")));
                    }
                });

        return 1;
    }

    private static int registerWithPassword(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cPlayers only"));
            return 0;
        }

        if (AuthenticationManager.isAuthenticated(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cYou are already authenticated!"));
            return 0;
        }

        String password = StringArgumentType.getString(ctx, "password");
        String username = player.getName().getString();
        String uuid = player.getUUID().toString();

        player.sendSystemMessage(Component.literal("§6⏳ §7Registering account..."));
        AuthenticationManager.setPendingRegistration(player.getUUID());

        VonixNetworkAPI.registerPlayerWithPassword(username, uuid, password)
                .thenAccept(response -> {
                    if (response.success) {
                        AuthenticationManager.setAuthenticated(player.getUUID(), response.token);
                        player.sendSystemMessage(Component.literal("§a§l✓ §7Account created! Welcome, §e" + username));
                    } else {
                        String error = response.error != null ? response.error : "Unknown error";
                        if (error.toLowerCase().contains("already registered")) {
                            player.sendSystemMessage(
                                    Component.literal("§eAlready registered! Use §a/login <password>"));
                        } else {
                            player.sendSystemMessage(Component.literal("§c§l✗ §7Registration failed: §c" + error));
                        }
                    }
                });

        return 1;
    }

    private static int login(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cPlayers only"));
            return 0;
        }

        if (AuthenticationManager.isAuthenticated(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cYou are already authenticated!"));
            return 0;
        }

        String password = StringArgumentType.getString(ctx, "password");
        String username = player.getName().getString();
        String uuid = player.getUUID().toString();

        player.sendSystemMessage(Component.literal("§6Authenticating..."));

        VonixNetworkAPI.loginPlayer(username, uuid, password)
                .thenAccept(response -> {
                    if (response.success) {
                        AuthenticationManager.setAuthenticated(player.getUUID(), response.token);
                        player.sendSystemMessage(Component.literal("§a§l✓ §7Welcome back, §e" + username + "§7!"));
                    } else {
                        String error = response.error != null ? response.error : "Unknown error";
                        player.sendSystemMessage(Component.literal("§c§l✗ §7Login failed: §c" + error));
                    }
                });

        return 1;
    }
}
