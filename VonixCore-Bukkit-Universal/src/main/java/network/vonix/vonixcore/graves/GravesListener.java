package network.vonix.vonixcore.graves;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles grave-related events
 */
public class GravesListener implements Listener {
    private final VonixCore plugin;
    private final GravesManager gravesManager;

    public GravesListener(VonixCore plugin, GravesManager gravesManager) {
        this.plugin = plugin;
        this.gravesManager = gravesManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gravesManager.isEnabled())
            return;

        Player player = event.getEntity();

        // Skip if keepInventory is on
        if (event.getKeepInventory())
            return;

        // Get drops
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        if (drops.isEmpty())
            return;

        // Get XP
        int droppedXp = event.getDroppedExp();

        // Create grave
        Grave grave = gravesManager.createGrave(player, drops, getTotalXp(player));

        if (grave != null) {
            // Clear drops since they're now in the grave
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Notify player
            player.sendMessage(ChatColor.GOLD + "☠ " + ChatColor.WHITE + "Your items have been stored in a grave!");
            player.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.WHITE +
                    formatLocation(grave.getLocation()));
            player.sendMessage(ChatColor.GRAY + "Expires in: " + ChatColor.WHITE +
                    grave.getTimeRemainingFormatted());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Grave grave = gravesManager.getGraveAt(block.getLocation());

        if (grave == null)
            return;

        Player player = event.getPlayer();

        // Check if player can break the grave
        if (!gravesManager.canLoot(player, grave)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This grave belongs to " + grave.getOwnerName() +
                    " and is still protected!");
            return;
        }

        // Drop items from chest first
        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getInventory();
            for (ItemStack item : inv.getContents()) {
                if (item != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            inv.clear();
        }

        // Give XP and remove grave
        gravesManager.lootGrave(player, grave);
        player.sendMessage(ChatColor.GREEN + "You looted " + grave.getOwnerName() + "'s grave!");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;
        if (!(event.getInventory().getHolder() instanceof Chest chest))
            return;

        Location location = chest.getLocation();
        Grave grave = gravesManager.getGraveAt(location);

        if (grave == null)
            return;

        // Check if player can loot
        if (!gravesManager.canLoot(player, grave)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "This grave belongs to " + grave.getOwnerName() +
                    " and is still protected!");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;
        if (!(event.getInventory().getHolder() instanceof Chest chest))
            return;

        Location location = chest.getLocation();
        Grave grave = gravesManager.getGraveAt(location);

        if (grave == null)
            return;

        // Check if grave is empty
        Inventory inv = chest.getInventory();
        boolean empty = Arrays.stream(inv.getContents())
                .allMatch(item -> item == null || item.getType().isAir());

        if (empty) {
            // Give XP and remove grave
            if (!grave.isLooted()) {
                gravesManager.lootGrave(player, grave);
                player.sendMessage(ChatColor.GREEN + "You've collected all items from the grave!");
            }
        }
    }

    private int getTotalXp(Player player) {
        int level = player.getLevel();
        float exp = player.getExp();

        // Calculate total XP based on level
        int totalXp;
        if (level <= 16) {
            totalXp = (int) (level * level + 6 * level);
        } else if (level <= 31) {
            totalXp = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalXp = (int) (4.5 * level * level - 162.5 * level + 2220);
        }

        // Add partial level XP
        totalXp += (int) (exp * getXpForLevel(level + 1));

        return totalXp;
    }

    private int getXpForLevel(int level) {
        if (level <= 16) {
            return 2 * level + 7;
        } else if (level <= 31) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + " " +
                location.getBlockX() + ", " +
                location.getBlockY() + ", " +
                location.getBlockZ();
    }
}
