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
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.auth.api.VonixNetworkAPI;
import network.vonix.vonixcore.config.AuthConfig;

/**
 * Authentication commands: /login, /register
 * Ported from Forge for 1:1 parity with Fabric-aware config handling.
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
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            if (AuthenticationManager.isAuthenticated(player.getUUID())) {
                player.sendMessage(new TextComponent(AuthConfig.ALREADY_AUTHENTICATED_MESSAGE.get()), player.getUUID());
                return 0;
            }

            String username = player.getName().getString();
            String uuid = player.getUUID().toString();

            player.sendMessage(new TextComponent(AuthConfig.GENERATING_CODE_MESSAGE.get()), player.getUUID());
            AuthenticationManager.setPendingRegistration(player.getUUID());

            VonixNetworkAPI.generateRegistrationCode(username, uuid)
                    .thenAccept(response -> {
                        if (response.code != null) {
                            String msg = AuthConfig.REGISTRATION_CODE_MESSAGE.get().replace("{code}", response.code);
                            player.sendMessage(new TextComponent(msg), player.getUUID());

                            Component link = new TextComponent(AuthConfig.REGISTRATION_URL.get())
                                    .withStyle(Style.EMPTY.withColor(0x00FFFF)
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, AuthConfig.REGISTRATION_URL.get()))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to register"))));
                            player.sendMessage(link, player.getUUID());
                            player.sendMessage(new TextComponent("§7Or use: §e/register <password>"), player.getUUID());
                        } else if (response.already_registered) {
                            player.sendMessage(new TextComponent("§eAlready registered! Use §a/login <password>"),
                                    player.getUUID());
                        } else {
                            player.sendMessage(new TextComponent("§cRegistration failed: "
                                    + (response.error != null ? response.error : "Unknown error")), player.getUUID());
                        }
                    });

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("§cPlayers only"));
            return 0;
        }
    }

    private static int registerWithPassword(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            if (AuthenticationManager.isAuthenticated(player.getUUID())) {
                player.sendMessage(new TextComponent(AuthConfig.ALREADY_AUTHENTICATED_MESSAGE.get()), player.getUUID());
                return 0;
            }

            String password = StringArgumentType.getString(ctx, "password");
            String username = player.getName().getString();
            String uuid = player.getUUID().toString();

            player.sendMessage(new TextComponent("§6⏳ §7Registering account..."), player.getUUID());
            AuthenticationManager.setPendingRegistration(player.getUUID());

            VonixNetworkAPI.registerPlayerWithPassword(username, uuid, password)
                    .thenAccept(response -> {
                        if (response.success) {
                            AuthenticationManager.setAuthenticated(player.getUUID(), response.token);
                            player.sendMessage(
                                    new TextComponent("§a§l✓ §7Account created! Welcome, §e" + username),
                                    player.getUUID());

                            // LuckPerms integration omitted for now or needs to be ported
                            /*
                             * if (response.user != null) {
                             * LuckPermsIntegration.synchronizeRank(player.getUUID(), response.user);
                             * }
                             */
                        } else {
                            String error = response.error != null ? response.error : "Unknown error";
                            if (error.toLowerCase().contains("already registered")) {
                                player.sendMessage(
                                        new TextComponent("§eAlready registered! Use §a/login <password>"),
                                        player.getUUID());
                            } else {
                                player.sendMessage(
                                        new TextComponent("§c§l✗ §7Registration failed: §c" + error),
                                        player.getUUID());
                            }
                        }
                    });

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("§cPlayers only"));
            return 0;
        }
    }

    private static int login(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();

            if (AuthenticationManager.isAuthenticated(player.getUUID())) {
                player.sendMessage(new TextComponent(AuthConfig.ALREADY_AUTHENTICATED_MESSAGE.get()), player.getUUID());
                return 0;
            }

            String password = StringArgumentType.getString(ctx, "password");
            String username = player.getName().getString();
            String uuid = player.getUUID().toString();

            player.sendMessage(new TextComponent(AuthConfig.AUTHENTICATING_MESSAGE.get()), player.getUUID());

            VonixNetworkAPI.loginPlayer(username, uuid, password)
                    .thenAccept(response -> {
                        if (response.success) {
                            AuthenticationManager.setAuthenticated(player.getUUID(), response.token);
                            String msg = AuthConfig.AUTHENTICATION_SUCCESS_MESSAGE.get().replace("{username}", username);
                            player.sendMessage(new TextComponent(msg), player.getUUID());

                            // LuckPerms integration omitted
                        } else {
                            String error = response.error != null ? response.error : "Unknown error";
                            String msg = AuthConfig.LOGIN_FAILED_MESSAGE.get().replace("{error}", error);
                            player.sendMessage(new TextComponent(msg), player.getUUID());
                        }
                    });

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("§cPlayers only"));
            return 0;
        }
    }
}
