package network.vonix.vonixcore.kits;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.util.TimeUtils;

/**
 * Commands for the Kit system.
 */
public class KitCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
                .then(Commands.argument("kit", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            KitManager.getInstance().getKitNames().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(KitCommands::giveKit))
                .executes(KitCommands::listKits));

        dispatcher.register(Commands.literal("kits")
                .executes(KitCommands::listKits));
    }

    private static int listKits(CommandContext<CommandSourceStack> ctx) {
        KitManager manager = KitManager.getInstance();
        ctx.getSource().sendSuccess(new TextComponent("=== Available Kits ===").withStyle(ChatFormatting.GOLD), false);
        
        for (String kitName : manager.getKitNames()) {
            KitManager.Kit kit = manager.getKit(kitName);
            String cooldown = kit.oneTime() ? "One-time" : TimeUtils.formatDuration(kit.cooldownSeconds());
            ctx.getSource().sendSuccess(new TextComponent("• " + kitName)
                    .withStyle(ChatFormatting.YELLOW)
                    .append(new TextComponent(" (" + cooldown + ")").withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    private static int giveKit(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(new TextComponent("This command is for players only!"));
            return 0;
        }

        String kitName = StringArgumentType.getString(ctx, "kit");
        KitManager manager = KitManager.getInstance();

        manager.giveKit(player, kitName).thenAcceptAsync(result -> {
            switch (result.getStatus()) {
                case SUCCESS ->
                        ctx.getSource().sendSuccess(new TextComponent("You received the " + kitName + " kit!").withStyle(ChatFormatting.GREEN), false);
                case NOT_FOUND ->
                        ctx.getSource().sendFailure(new TextComponent("Kit not found: " + kitName));
                case ON_COOLDOWN ->
                        ctx.getSource().sendFailure(new TextComponent("You must wait " + TimeUtils.formatDuration(result.getCooldown()) + " before using this kit again."));
                case ALREADY_CLAIMED ->
                        ctx.getSource().sendFailure(new TextComponent("You have already claimed this one-time kit!"));
            }
        }, player.getServer());

        return 1;
    }
}
