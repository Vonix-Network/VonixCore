package network.vonix.vonixcore.claims;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commands for the claims system.
 * Main command: /vonixcoreclaims (aliases: /vcclaims, /claims)
 */
public class ClaimsCommands implements CommandExecutor, TabCompleter {

    private final VonixCore plugin;
    private final ClaimsManager manager;

    public ClaimsCommands(VonixCore plugin, ClaimsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "wand" -> {
                return giveWand(sender);
            }
            case "create" -> {
                int radius = -1;
                if (args.length > 1) {
                    try {
                        radius = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid radius!");
                        return true;
                    }
                }
                return createClaim(sender, radius);
            }
            case "delete" -> {
                return deleteClaim(sender);
            }
            case "trust" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /vcclaims trust <player>");
                    return true;
                }
                return trustPlayer(sender, args[1]);
            }
            case "untrust" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /vcclaims untrust <player>");
                    return true;
                }
                return untrustPlayer(sender, args[1]);
            }
            case "list" -> {
                return listClaims(sender);
            }
            case "info" -> {
                return claimInfo(sender);
            }
            case "admin" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /vcclaims admin <delete|list> ...");
                    return true;
                }
                return handleAdmin(sender, args);
            }
            default -> {
                showHelp(sender);
                return true;
            }
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== VonixCore Claims ===");
        sender.sendMessage("§e/vcclaims wand §7- Get claim selection wand");
        sender.sendMessage("§e/vcclaims create [radius] §7- Create claim");
        sender.sendMessage("§e/vcclaims delete §7- Delete claim you're in");
        sender.sendMessage("§e/vcclaims trust <player> §7- Trust player");
        sender.sendMessage("§e/vcclaims untrust <player> §7- Untrust player");
        sender.sendMessage("§e/vcclaims list §7- List your claims");
        sender.sendMessage("§e/vcclaims info §7- Info about current claim");
    }

    private boolean giveWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        if (!hasCreatePermission(player)) {
            player.sendMessage("§cYou don't have permission to create claims!");
            return true;
        }

        ItemStack wand = new ItemStack(Material.GOLDEN_SHOVEL);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Claim Wand");
            wand.setItemMeta(meta);
        }

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(wand);
        } else {
            player.getWorld().dropItem(player.getLocation(), wand);
        }

        player.sendMessage("§aReceived claim wand!");
        player.sendMessage("§7Left-click: Set corner 1");
        player.sendMessage("§7Right-click: Set corner 2");
        player.sendMessage("§7Then use §e/vcclaims create");
        return true;
    }

    private boolean createClaim(CommandSender sender, int radius) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        if (!hasCreatePermission(player)) {
            player.sendMessage("§cYou don't have permission to create claims!");
            return true;
        }

        String world = player.getWorld().getName();
        Location pos1, pos2;

        if (radius > 0) {
            // Create claim around player with radius
            Location center = player.getLocation();
            pos1 = center.clone().add(-radius, -64 - center.getBlockY(), -radius);
            pos2 = center.clone().add(radius, 320 - center.getBlockY(), radius);
        } else if (manager.hasSelection(player.getUniqueId())) {
            // Use wand selection
            pos1 = manager.getCorner1(player.getUniqueId());
            pos2 = manager.getCorner2(player.getUniqueId());
            // Extend Y to full height
            pos1 = new Location(pos1.getWorld(), pos1.getX(), -64, pos1.getZ());
            pos2 = new Location(pos2.getWorld(), pos2.getX(), 320, pos2.getZ());
        } else {
            // Use default radius
            int defaultRadius = manager.getDefaultClaimRadius();
            Location center = player.getLocation();
            pos1 = center.clone().add(-defaultRadius, -64 - center.getBlockY(), -defaultRadius);
            pos2 = center.clone().add(defaultRadius, 320 - center.getBlockY(), defaultRadius);
        }

        Claim claim = manager.createClaim(player.getUniqueId(), player.getName(), world, pos1, pos2);

        if (claim == null) {
            player.sendMessage("§cFailed to create claim! Possible reasons:");
            player.sendMessage("§c- Claim overlaps existing claim");
            player.sendMessage("§c- Claim too large");
            player.sendMessage("§c- Claim limit reached");
            return true;
        }

        manager.clearSelection(player.getUniqueId());
        player.sendMessage("§aClaim created! ID: " + claim.getId());
        player.sendMessage(String.format("§7Area: %d blocks (%d x %d)",
                claim.getArea(),
                claim.getX2() - claim.getX1() + 1,
                claim.getZ2() - claim.getZ1() + 1));
        return true;
    }

    private boolean deleteClaim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        Claim claim = manager.getClaimAt(player.getLocation());

        if (claim == null) {
            player.sendMessage("§cYou're not standing in a claim!");
            return true;
        }

        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("vonixcore.claims.admin")) {
            player.sendMessage("§cYou don't own this claim!");
            return true;
        }

        if (manager.deleteClaim(claim.getId())) {
            player.sendMessage("§aClaim deleted!");
        } else {
            player.sendMessage("§cFailed to delete claim!");
        }
        return true;
    }

    private boolean trustPlayer(CommandSender sender, String targetName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        Claim claim = manager.getClaimAt(player.getLocation());

        if (claim == null) {
            player.sendMessage("§cYou're not standing in a claim!");
            return true;
        }

        if (!claim.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this claim!");
            return true;
        }

        if (manager.trustPlayer(claim.getId(), target.getUniqueId())) {
            player.sendMessage("§aTrusted §e" + target.getName() + "§a in this claim!");
        }
        return true;
    }

    private boolean untrustPlayer(CommandSender sender, String targetName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }

        Claim claim = manager.getClaimAt(player.getLocation());

        if (claim == null) {
            player.sendMessage("§cYou're not standing in a claim!");
            return true;
        }

        if (!claim.getOwner().equals(player.getUniqueId())) {
            player.sendMessage("§cYou don't own this claim!");
            return true;
        }

        if (manager.untrustPlayer(claim.getId(), target.getUniqueId())) {
            player.sendMessage("§cRemoved trust for §e" + target.getName());
        }
        return true;
    }

    private boolean listClaims(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        List<Claim> claims = manager.getPlayerClaims(player.getUniqueId());

        if (claims.isEmpty()) {
            player.sendMessage("§7You don't have any claims.");
            return true;
        }

        player.sendMessage("§6=== Your Claims (" + claims.size() + ") ===");
        for (Claim claim : claims) {
            player.sendMessage(String.format(
                    "§e#%d §7- %s §8(%d,%d) to (%d,%d) §7[%d blocks]",
                    claim.getId(), claim.getWorld(),
                    claim.getX1(), claim.getZ1(),
                    claim.getX2(), claim.getZ2(),
                    claim.getArea()));
        }
        return true;
    }

    private boolean claimInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }

        Claim claim = manager.getClaimAt(player.getLocation());

        if (claim == null) {
            player.sendMessage("§7You're not standing in a claim.");
            return true;
        }

        player.sendMessage("§6=== Claim Info ===");
        player.sendMessage("§7ID: §f" + claim.getId());
        player.sendMessage("§7Owner: §f" + claim.getOwnerName());
        player.sendMessage("§7World: §f" + claim.getWorld());
        player.sendMessage(String.format("§7Bounds: §f(%d,%d) to (%d,%d)",
                claim.getX1(), claim.getZ1(), claim.getX2(), claim.getZ2()));
        player.sendMessage("§7Area: §f" + claim.getArea() + " blocks");

        if (!claim.getTrusted().isEmpty()) {
            player.sendMessage("§7Trusted: §f" + claim.getTrusted().size() + " players");
        }

        if (claim.canInteract(player.getUniqueId())) {
            player.sendMessage("§aYou can build here.");
        } else {
            player.sendMessage("§cYou cannot build here.");
        }

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.claims.admin")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }

        String adminCmd = args[1].toLowerCase();

        if (adminCmd.equals("delete") && args.length >= 3) {
            try {
                int claimId = Integer.parseInt(args[2]);
                if (manager.deleteClaim(claimId)) {
                    sender.sendMessage("§aDeleted claim #" + claimId);
                } else {
                    sender.sendMessage("§cClaim not found!");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid claim ID!");
            }
        } else if (adminCmd.equals("list")) {
            sender.sendMessage("§7Use /vcclaims list as that player or check database.");
        } else {
            sender.sendMessage("§cUsage: /vcclaims admin <delete|list> ...");
        }

        return true;
    }

    private boolean hasCreatePermission(Player player) {
        if (!manager.isRequirePermissionToCreate()) {
            return true;
        }
        return player.hasPermission("vonixcore.claims.create");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(
                    Arrays.asList("wand", "create", "delete", "trust", "untrust", "list", "info", "admin"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) {
                return null; // Return online players
            }
            if (args[0].equalsIgnoreCase("admin")) {
                return filterStartsWith(Arrays.asList("delete", "list"), args[1]);
            }
        }
        return List.of();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
