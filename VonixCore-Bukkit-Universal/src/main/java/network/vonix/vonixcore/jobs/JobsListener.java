package network.vonix.vonixcore.jobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for player actions and rewards jobs accordingly.
 */
public class JobsListener implements Listener {

    private final JobsManager manager;

    public JobsListener(JobsManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        manager.loadPlayerJobs(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        manager.unloadPlayerJobs(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!manager.isEnabled())
            return;

        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        manager.processAction(player, Job.ActionType.BREAK, material.name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!manager.isEnabled())
            return;

        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        manager.processAction(player, Job.ActionType.PLACE, material.name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!manager.isEnabled())
            return;

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null)
            return;
        if (entity instanceof Player)
            return; // Don't reward PvP kills

        EntityType type = entity.getType();
        manager.processAction(killer, Job.ActionType.KILL, type.name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (!manager.isEnabled())
            return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;

        Player player = event.getPlayer();

        if (event.getCaught() instanceof org.bukkit.entity.Item item) {
            Material caught = item.getItemStack().getType();
            manager.processAction(player, Job.ActionType.FISH, caught.name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!manager.isEnabled())
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack result = event.getRecipe().getResult();
        manager.processAction(player, Job.ActionType.CRAFT, result.getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!manager.isEnabled())
            return;

        Player player = event.getPlayer();
        Material material = event.getItemType();

        // Process for each item extracted
        for (int i = 0; i < event.getItemAmount(); i++) {
            manager.processAction(player, Job.ActionType.SMELT, material.name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!manager.isEnabled())
            return;
        if (!(event.getBreeder() instanceof Player player))
            return;

        EntityType type = event.getEntity().getType();
        manager.processAction(player, Job.ActionType.BREED, type.name());
    }
}
