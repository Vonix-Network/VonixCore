package network.vonix.vonixcore.graves;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;

import java.util.List;

/**
 * Commands for the graves system.
 */
public class GravesCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("graves")
                .executes(GravesCommands::listGraves)
                .then(Commands.literal("list")
                        .executes(GravesCommands::listGraves))
                .then(Commands.literal("status")
                        .requires(s -> s.hasPermission(2))
                        .executes(GravesCommands::status)));

        VonixCore.LOGGER.info("[VonixCore] Graves commands registered");
    }

    private static int listGraves(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.literal("§cPlayers only"));
            return 0;
        }

        GravesManager manager = GravesManager.getInstance();
        if (manager == null) {
            player.sendSystemMessage(Component.literal("§cGraves system not initialized"));
            return 0;
        }

        List<Grave> graves = manager.getPlayerGraves(player.getUUID());
        if (graves.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7You have no active graves."));
            return 1;
        }

        player.sendSystemMessage(Component.literal("§6§l=== Your Graves ==="));
        for (Grave grave : graves) {
            String status = grave.isLooted() ? "§c(Looted)" : grave.isExpired() ? "§e(Expired)" : "§a(Active)";
            long timeLeft = (grave.getExpiresAt() - System.currentTimeMillis()) / 1000;
            String time = timeLeft > 0 ? formatTime(timeLeft) : "Expired";

            player.sendSystemMessage(Component.literal(String.format(
                    "§7- §e%d, %d, %d §7in §f%s %s §7(%s)",
                    grave.getX(), grave.getY(), grave.getZ(),
                    grave.getWorld().replace("minecraft:", ""),
                    status, time)));
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        GravesManager manager = GravesManager.getInstance();
        if (manager == null) {
            ctx.getSource().sendFailure(Component.literal("§cGraves system not initialized"));
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§6§l=== Graves Status ==="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7Enabled: §e" + GravesManager.enabled), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7Active graves: §e" + manager.getGraveCount()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§7Expiration: §e" + GravesManager.expirationTime + "s"),
                false);
        ctx.getSource().sendSuccess(
                () -> Component.literal("§7XP retention: §e" + (GravesManager.xpRetention * 100) + "%"), false);
        return 1;
    }

    private static String formatTime(long seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return (seconds / 60) + "m";
        return (seconds / 3600) + "h";
    }
}
