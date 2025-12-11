package network.vonix.vonixcore.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import network.vonix.vonixcore.VonixCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Event handler for graves - handles death and grave interaction.
 */
@EventBusSubscriber(modid = VonixCore.MODID)
public class GravesListener {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!GravesManager.enabled)
            return;

        // Collect items
        Inventory inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }

        // Calculate XP
        int totalXp = player.experienceLevel * 7; // Simplified XP calc

        if (items.isEmpty() && totalXp == 0)
            return;

        // Create grave
        Grave grave = GravesManager.getInstance().createGrave(player, items, totalXp);

        if (grave != null) {
            // Clear inventory so items don't drop
            inv.clearContent();
            player.experienceLevel = 0;
            player.experienceProgress = 0;
            player.totalExperience = 0;

            player.sendSystemMessage(Component.literal(
                    "§6[Graves] §7Your items have been stored at §e" +
                            grave.getX() + ", " + grave.getY() + ", " + grave.getZ()));
        }
    }

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        if (!GravesManager.enabled)
            return;

        BlockPos pos = event.getPos();
        String world = player.level().dimension().location().toString();

        GravesManager manager = GravesManager.getInstance();
        if (manager == null)
            return;

        Grave grave = manager.getGraveAt(world, pos);
        if (grave == null)
            return;

        // Check if can loot
        if (!manager.canLoot(player, grave)) {
            event.setCanceled(true);
            long timeLeft = (grave.getCreatedAt() + (GravesManager.protectionTime * 1000L) - System.currentTimeMillis())
                    / 1000;
            player.sendSystemMessage(Component.literal(
                    "§c[Graves] §7This grave belongs to §e" + grave.getOwnerName() +
                            "§7. Protected for §e" + timeLeft + "s"));
            return;
        }

        // Loot the grave
        manager.lootGrave(player, grave, player.serverLevel());
        player.sendSystemMessage(Component.literal("§a[Graves] §7You recovered your items!"));
    }
}
