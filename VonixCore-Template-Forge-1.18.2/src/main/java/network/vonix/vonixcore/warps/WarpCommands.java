package network.vonix.vonixcore.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.teleport.TeleportManager;

import java.util.List;

public class WarpCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /warp <name>
        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> teleportWarp(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        // /warps
        dispatcher.register(Commands.literal("warps")
                .executes(ctx -> listWarps(ctx.getSource())));

        // /setwarp <name> (op only)
        dispatcher.register(Commands.literal("setwarp")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setWarp(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        // /delwarp <name> (op only)
        dispatcher.register(Commands.literal("delwarp")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> deleteWarp(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
    }

    private static int teleportWarp(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(new TextComponent("§cThis command can only be used by players"));
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();

        WarpManager.Warp warp = WarpManager.getInstance().getWarp(name);
        if (warp == null) {
            source.sendFailure(new TextComponent("§cWarp '" + name + "' not found"));
            return 0;
        }

        ResourceLocation worldId = ResourceLocation.tryParse(warp.world());
        if (worldId == null) {
            source.sendFailure(new TextComponent("§cInvalid world"));
            return 0;
        }

        ServerLevel level = source.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY,
                        worldId));
        if (level == null) {
            source.sendFailure(new TextComponent("§cWorld not found"));
            return 0;
        }

        TeleportManager.getInstance().teleportPlayer(player, level, warp.x(), warp.y(), warp.z(), warp.yaw(),
                warp.pitch());
        source.sendSuccess(new TextComponent("§aTeleported to warp '" + name + "'"), false);
        return 1;
    }

    private static int setWarp(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(new TextComponent("§cThis command can only be used by players"));
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (WarpManager.getInstance().setWarp(name, player)) {
            source.sendSuccess(new TextComponent("§aWarp '" + name + "' created!"), false);
            return 1;
        } else {
            source.sendFailure(new TextComponent("§cFailed to create warp"));
            return 0;
        }
    }

    private static int deleteWarp(CommandSourceStack source, String name) {
        if (WarpManager.getInstance().deleteWarp(name)) {
            source.sendSuccess(new TextComponent("§aWarp '" + name + "' deleted"), false);
            return 1;
        } else {
            source.sendFailure(new TextComponent("§cWarp '" + name + "' not found"));
            return 0;
        }
    }

    private static int listWarps(CommandSourceStack source) {
        List<WarpManager.Warp> warps = WarpManager.getInstance().getWarps();
        if (warps.isEmpty()) {
            source.sendSuccess(new TextComponent("§7No warps available"), false);
        } else {
            source.sendSuccess(new TextComponent("§6Available Warps:"), false);
            for (WarpManager.Warp warp : warps) {
                source.sendSuccess(new TextComponent("§e- " + warp.name()), false);
            }
        }
        return 1;
    }
}
