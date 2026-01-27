package network.vonix.vonixcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.teleport.TeleportManager;

/**
 * Teleport commands: /tpa, /tpaccept, /tpdeny, /back
 */
public class TeleportCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tpa <player>
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpaCommand)));

        // /tpahere <player>
        dispatcher.register(Commands.literal("tpahere")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpaHereCommand)));

        // /tpaccept
        dispatcher.register(Commands.literal("tpaccept")
                .executes(TeleportCommands::tpAcceptCommand));

        // /tpdeny
        dispatcher.register(Commands.literal("tpdeny")
                .executes(TeleportCommands::tpDenyCommand));

        // /back
        dispatcher.register(Commands.literal("back")
                .executes(TeleportCommands::backCommand));
    }

    private static int tpaCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

            if (player.getUUID().equals(target.getUUID())) {
                ctx.getSource().sendFailure(new TextComponent("\u00A7c[VC] You cannot teleport to yourself."));
                return 0;
            }

            if (TeleportManager.getInstance().sendTpaRequest(player, target, false)) {
                player.sendMessage(
                        new TextComponent("\u00A7a[VC] TPA request sent to \u00A7e" + target.getName().getString()),
                        player.getUUID());
                target.sendMessage(new TextComponent("\u00A7e" + player.getName().getString() +
                        " \u00A76wants to teleport to you. Type \u00A7a/tpaccept \u00A76or \u00A7c/tpdeny"),
                        player.getUUID());
                return 1;
            } else {
                ctx.getSource()
                        .sendFailure(new TextComponent("\u00A7c[VC] That player already has a pending request."));
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("\u00A7cPlayer not found."));
            return 0;
        }
    }

    private static int tpaHereCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

            if (player.getUUID().equals(target.getUUID())) {
                ctx.getSource().sendFailure(new TextComponent("\u00A7c[VC] You cannot teleport yourself to yourself."));
                return 0;
            }

            if (TeleportManager.getInstance().sendTpaRequest(player, target, true)) {
                player.sendMessage(
                        new TextComponent(
                                "\u00A7a[VC] TPA here request sent to \u00A7e" + target.getName().getString()),
                        player.getUUID());
                target.sendMessage(new TextComponent("\u00A7e" + player.getName().getString() +
                        " \u00A76wants you to teleport to them. Type \u00A7a/tpaccept \u00A76or \u00A7c/tpdeny"),
                        player.getUUID());
                return 1;
            } else {
                ctx.getSource()
                        .sendFailure(new TextComponent("\u00A7c[VC] That player already has a pending request."));
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(new TextComponent("\u00A7cPlayer not found."));
            return 0;
        }
    }

    private static int tpAcceptCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            if (TeleportManager.getInstance().acceptTpaRequest(player, ctx.getSource().getServer())) {
                player.sendMessage(new TextComponent("\u00A7a[VC] Teleport request accepted."), player.getUUID());
                return 1;
            }
            player.sendMessage(new TextComponent("\u00A7c[VC] No pending teleport request."), player.getUUID());
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int tpDenyCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            if (TeleportManager.getInstance().denyTpaRequest(player)) {
                player.sendMessage(new TextComponent("\u00A7c[VC] Teleport request denied."), player.getUUID());
                return 1;
            }
            player.sendMessage(new TextComponent("\u00A7c[VC] No pending teleport request."), player.getUUID());
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int backCommand(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            var loc = TeleportManager.getInstance().getLastLocation(player.getUUID());

            if (loc == null) {
                player.sendMessage(new TextComponent("\u00A7c[VC] No location to return to."), player.getUUID());
                return 0;
            }

            // Check timeout for regular /back (not death locations)
            int timeoutSeconds = 0;
            if (!loc.isDeath()) {
                timeoutSeconds = EssentialsConfig.getInstance().getBackTimeout();
            }

            // Check death back delay (cooldown ONLY applies to deaths)
            if (loc.isDeath()) {
                int delaySeconds = EssentialsConfig.getInstance().getDeathBackDelay();
                if (delaySeconds > 0) {
                    long elapsed = (System.currentTimeMillis() - loc.timestamp()) / 1000;
                    if (elapsed < delaySeconds) {
                        ctx.getSource().sendFailure(new TextComponent("\u00A7c[VC] You must wait " +
                                formatTime((int) (delaySeconds - elapsed))
                                + " before returning to your death location."));
                        return 0;
                    }
                }
            }

            // Check if regular back location has expired (not death locations)
            if (timeoutSeconds > 0 && !loc.isDeath()) {
                long elapsed = (System.currentTimeMillis() - loc.timestamp()) / 1000;
                if (elapsed > timeoutSeconds) {
                    ctx.getSource().sendFailure(new TextComponent(
                            "\u00A7c[VC] Your previous location has expired (" + timeoutSeconds + "s timeout)."));
                    return 0;
                }
            }

            // Find the target world and teleport
            var server = ctx.getSource().getServer();
            for (var level : server.getAllLevels()) {
                if (level.dimension().location().toString().equals(loc.world())) {
                    TeleportManager.getInstance().teleportPlayer(player, level, loc.x(), loc.y(), loc.z(),
                            loc.yaw(), loc.pitch());
                    player.sendMessage(new TextComponent("\u00A7a[VC] Returned to previous location."),
                            player.getUUID());
                    return 1;
                }
            }

            ctx.getSource().sendFailure(new TextComponent("\u00A7c[VC] World no longer exists."));
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }
}
