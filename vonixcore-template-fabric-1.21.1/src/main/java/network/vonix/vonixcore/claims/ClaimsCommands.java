package network.vonix.vonixcore.claims;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Commands for the claims system.
 */
public class ClaimsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("claim")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§6Claims Help:"), false);
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("§e/claim create §7- Create a claim at your location"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/claim delete §7- Delete a claim"), false);
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("§e/claim trust <player> §7- Trust a player in your claim"), false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/claim untrust <player> §7- Remove trust"),
                            false);
                    ctx.getSource().sendSuccess(() -> Component.literal("§e/claim list §7- List your claims"), false);
                    return 1;
                })
                .then(Commands.literal("create")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aClaim creation coming soon!"),
                                    false);
                            return 1;
                        }))
                .then(Commands.literal("delete")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aClaim deletion coming soon!"),
                                    false);
                            return 1;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§aClaim listing coming soon!"), false);
                            return 1;
                        })));
    }
}
