package network.vonix.vonixcore.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * World-related commands like /time, /weather, etc.
 */
public class WorldCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // These commands require OP permissions

        // /day
        dispatcher.register(Commands.literal("day")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setDayTime(1000);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aTime set to day"), false);
                    return 1;
                }));

        // /night
        dispatcher.register(Commands.literal("night")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setDayTime(13000);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aTime set to night"), false);
                    return 1;
                }));

        // /sun
        dispatcher.register(Commands.literal("sun")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setWeatherParameters(24000, 0, false, false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aWeather set to clear"), false);
                    return 1;
                }));

        // /rain
        dispatcher.register(Commands.literal("rain")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setWeatherParameters(0, 24000, true, false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aWeather set to rain"), false);
                    return 1;
                }));
    }
}
