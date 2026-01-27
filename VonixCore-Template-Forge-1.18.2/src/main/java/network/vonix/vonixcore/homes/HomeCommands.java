package network.vonix.vonixcore.homes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.teleport.TeleportManager;

import java.util.List;

public class HomeCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /sethome [name]
        dispatcher.register(Commands.literal("sethome")
                .executes(ctx -> setHome(ctx.getSource(), "home"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> setHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        // /home [name]
        dispatcher.register(Commands.literal("home")
                .executes(ctx -> teleportHome(ctx.getSource(), "home"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> teleportHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        // /delhome <name>
        dispatcher.register(Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> deleteHome(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));

        // /homes
        dispatcher.register(Commands.literal("homes")
                .executes(ctx -> listHomes(ctx.getSource())));
    }

    private static int setHome(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(new TextComponent("§cThis command can only be used by players"));
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (HomeManager.getInstance().setHome(player, name)) {
            source.sendSuccess(new TextComponent("§aHome '" + name + "' set!"), false);
            return 1;
        } else {
            source.sendFailure(new TextComponent("§cCouldn't set home. You may have reached your limit."));
            return 0;
        }
    }

    private static int teleportHome(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(new TextComponent("§cThis command can only be used by players"));
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();

        HomeManager.Home home = HomeManager.getInstance().getHome(player.getUUID(), name);
        if (home == null) {
            source.sendFailure(new TextComponent("§cHome '" + name + "' not found"));
            return 0;
        }

        // Get the world
        ResourceLocation worldId = ResourceLocation.tryParse(home.world());
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

        TeleportManager.getInstance().teleportPlayer(player, level, home.x(), home.y(), home.z(), home.yaw(),
                home.pitch());
        source.sendSuccess(new TextComponent("§aTeleported to home '" + name + "'"), false);
        return 1;
    }

    private static int deleteHome(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(new TextComponent("§cThis command can only be used by players"));
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (HomeManager.getInstance().deleteHome(player.getUUID(), name)) {
            source.sendSuccess(new TextComponent("§aHome '" + name + "' deleted"), false);
            return 1;
        } else {
            source.sendFailure(new TextComponent("§cHome '" + name + "' not found"));
            return 0;
        }
    }

    private static int listHomes(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(new TextComponent("§cThis command can only be used by players"));
            return 0;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();

        List<HomeManager.Home> homes = HomeManager.getInstance().getHomes(player.getUUID());
        if (homes.isEmpty()) {
            source.sendSuccess(new TextComponent("§7You have no homes set. Use /sethome to create one."), false);
        } else {
            source.sendSuccess(
                    new TextComponent(
                            "§6Your Homes (" + homes.size() + "/" + EssentialsConfig.getInstance().getMaxHomes()
                                    + "):"),
                    false);
            for (HomeManager.Home home : homes) {
                source.sendSuccess(new TextComponent("§e- " + home.name() + " §7(" + home.world() + ")"), false);
            }
        }
        return 1;
    }
}
