package network.vonix.vonixcore.claims;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Event handlers for claim protection.
 * Prevents unauthorized players from interacting with protected areas.
 */
public class ClaimsListener implements Listener {

    private final VonixCore plugin;
    private final ClaimsManager manager;

    public ClaimsListener(VonixCore plugin, ClaimsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Handle block breaking
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!manager.isProtectBuilding())
            return;

        Player player = event.getPlayer();

        // Admins bypass
        if (player.hasPermission("vonixcore.claims.admin"))
            return;

        Location loc = event.getBlock().getLocation();

        if (!manager.canBuild(player.getUniqueId(), loc)) {
            event.setCancelled(true);
            player.sendMessage("§cYou can't break blocks in this claim!");
        }
    }

    /**
     * Handle block placing
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!manager.isProtectBuilding())
            return;

        Player player = event.getPlayer();

        // Admins bypass
        if (player.hasPermission("vonixcore.claims.admin"))
            return;

        Location loc = event.getBlock().getLocation();

        if (!manager.canBuild(player.getUniqueId(), loc)) {
            event.setCancelled(true);
            player.sendMessage("§cYou can't place blocks in this claim!");
        }
    }

    /**
     * Handle right-click interactions (containers, wand selection)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null)
            return;

        // Handle wand selection (golden shovel)
        if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_SHOVEL) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                Location loc = block.getLocation();
                manager.setCorner1(player.getUniqueId(), loc);
                player.sendMessage(String.format("§aCorner 1 set: §e%d, %d, %d",
                        loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                event.setCancelled(true);
                return;
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Don't interfere with container interactions
                if (!isContainer(block.getType()) && !isSign(block.getType())) {
                    Location loc = block.getLocation();
                    manager.setCorner2(player.getUniqueId(), loc);
                    player.sendMessage(String.format("§aCorner 2 set: §e%d, %d, %d",
                            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

                    // Show selection size
                    Location corner1 = manager.getCorner1(player.getUniqueId());
                    if (corner1 != null) {
                        int sizeX = Math.abs(loc.getBlockX() - corner1.getBlockX()) + 1;
                        int sizeZ = Math.abs(loc.getBlockZ() - corner1.getBlockZ()) + 1;
                        player.sendMessage(String.format("§7Selection: §f%dx%d §7(%d blocks)",
                                sizeX, sizeZ, sizeX * sizeZ));
                        player.sendMessage("§7Use §e/vcclaims create §7to create claim");
                    }
                    return;
                }
            }
        }

        // Check for container interactions
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        if (!manager.isProtectContainers())
            return;

        // Admins bypass
        if (player.hasPermission("vonixcore.claims.admin"))
            return;

        Location loc = block.getLocation();
        Claim claim = manager.getClaimAt(loc);

        if (claim == null)
            return; // Not in claim

        // Check if player can interact
        if (claim.canInteract(player.getUniqueId()))
            return; // Owner or trusted

        // Check for VonixCore shop bypass
        if (manager.isAllowVonixShopsBypass()) {
            // Check chest shops
            if (isContainer(block.getType())) {
                var shopsManager = network.vonix.vonixcore.shops.ShopsManager.getInstance();
                if (shopsManager != null && shopsManager.getChestShopManager() != null) {
                    if (shopsManager.getChestShopManager().getShopAt(loc) != null) {
                        return; // Allow shop interaction
                    }
                }
            }
            // Check sign shops
            if (isSign(block.getType())) {
                if (block.getState() instanceof Sign sign) {
                    String line1 = sign.getSide(org.bukkit.block.sign.Side.FRONT).getLine(0).toLowerCase();
                    if (line1.contains("[buy]") || line1.contains("[sell]")) {
                        return; // Allow sign shop interaction
                    }
                }
            }
        }

        // Block container access
        if (isContainer(block.getType())) {
            event.setCancelled(true);
            player.sendMessage("§cYou can't access containers in this claim!");
        }
    }

    /**
     * Handle explosions
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!manager.isPreventExplosions())
            return;

        // Remove blocks in claims from explosion
        event.blockList().removeIf(block -> {
            Claim claim = manager.getClaimAt(block.getLocation());
            return claim != null;
        });
    }

    private boolean isContainer(Material mat) {
        return mat == Material.CHEST || mat == Material.TRAPPED_CHEST ||
                mat == Material.BARREL || mat == Material.HOPPER ||
                mat == Material.DROPPER || mat == Material.DISPENSER ||
                mat == Material.FURNACE || mat == Material.BLAST_FURNACE ||
                mat == Material.SMOKER || mat == Material.BREWING_STAND ||
                mat.name().contains("SHULKER_BOX");
    }

    private boolean isSign(Material mat) {
        return mat.name().contains("SIGN");
    }
}
