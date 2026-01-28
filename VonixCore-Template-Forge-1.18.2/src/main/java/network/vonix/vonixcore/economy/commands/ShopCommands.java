package network.vonix.vonixcore.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import network.vonix.vonixcore.config.EssentialsConfig;
import network.vonix.vonixcore.economy.EconomyManager;
import network.vonix.vonixcore.economy.rewards.DailyRewardResult;
import network.vonix.vonixcore.economy.ShopManager;
import network.vonix.vonixcore.util.TimeUtils;

public class ShopCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!EssentialsConfig.CONFIG.shopsEnabled.get()) {
            return;
        }

        dispatcher.register(Commands.literal("daily")
                .executes(ctx -> {
                    try {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ShopManager.getInstance().claimDailyReward(player.getUUID()).thenAccept(result -> {
                            player.getServer().execute(() -> {
                                if (result.getStatus() == DailyRewardResult.Status.SUCCESS) {
                                    double amount = result.getAmount();
                                    player.sendMessage(new TextComponent("§6§l✦ DAILY REWARD ✦"), player.getUUID());
                                    player.sendMessage(new TextComponent("§aYou received " + EconomyManager.getInstance().format(amount) + "!"), player.getUUID());
                                    player.sendMessage(new TextComponent("§7Current Streak: §e" + result.getStreak() + " days"), player.getUUID());
                                    player.sendMessage(new TextComponent("§7Come back tomorrow for another reward!"), player.getUUID());
                                } else if (result.getStatus() == DailyRewardResult.Status.COOLDOWN) {
                                    player.sendMessage(new TextComponent("§c[Daily] You have already claimed your daily reward."), player.getUUID());
                                } else {
                                    player.sendMessage(new TextComponent("§c[Daily] An error occurred while claiming your reward."), player.getUUID());
                                }
                            });
                        });
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(new TextComponent("An error occurred: " + e.getMessage()));
                    }
                    return 1;
                }));
    }
}
