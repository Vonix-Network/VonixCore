package network.vonix.vonixcore.command;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility commands: tp, tphere, nick, seen, msg, r, heal, feed, fly, etc.
 */
public class UtilityCommands implements CommandExecutor, TabCompleter {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> ignoreList = new ConcurrentHashMap<>();

    private final VonixCore plugin;

    public UtilityCommands(VonixCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        return switch (cmd) {
            // Teleport commands
            case "tp" -> handleTp(sender, args);
            case "tphere" -> handleTphere(sender, args);
            case "tppos" -> handleTppos(sender, args);
            case "tpall" -> handleTpall(sender);
            case "rtp" -> handleRtp(sender);

            // Player utilities
            case "nick" -> handleNick(sender, args);
            case "seen" -> handleSeen(sender, args);
            case "whois" -> handleWhois(sender, args);
            case "ping" -> handlePing(sender);
            case "near" -> handleNear(sender, args);
            case "getpos" -> handleGetpos(sender);
            case "playtime" -> handlePlaytime(sender);

            // Messaging
            case "msg", "tell", "whisper" -> handleMsg(sender, args);
            case "r", "reply" -> handleReply(sender, args);
            case "ignore" -> handleIgnore(sender, args);

            // Admin
            case "heal" -> handleHeal(sender, args);
            case "feed" -> handleFeed(sender, args);
            case "fly" -> handleFly(sender, args);
            case "god" -> handleGod(sender, args);
            case "speed" -> handleSpeed(sender, args);
            case "clear", "clearinventory" -> handleClear(sender, args);
            case "repair" -> handleRepair(sender);
            case "more" -> handleMore(sender);
            case "hat" -> handleHat(sender);
            case "broadcast", "bc" -> handleBroadcast(sender, args);
            case "invsee" -> handleInvsee(sender, args);
            case "enderchest", "ec" -> handleEnderchest(sender, args);
            case "workbench", "craft" -> handleWorkbench(sender);
            case "gc" -> handleGc(sender);
            case "lag" -> handleLag(sender);

            default -> false;
        };
    }

    // === TELEPORT COMMANDS ===

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.tp")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /tp <player> [target]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        if (args.length >= 2) {
            Player dest = Bukkit.getPlayer(args[1]);
            if (dest == null) {
                sender.sendMessage(ChatColor.RED + "Destination player not found.");
                return true;
            }
            target.teleport(dest);
            sender.sendMessage(ChatColor.GREEN + "Teleported " + ChatColor.YELLOW + target.getName() +
                    ChatColor.GREEN + " to " + ChatColor.YELLOW + dest.getName());
        } else {
            player.teleport(target);
            player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.YELLOW + target.getName());
        }
        return true;
    }

    private boolean handleTphere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.tphere")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /tphere <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        target.teleport(player);
        player.sendMessage(
                ChatColor.GREEN + "Teleported " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " to you");
        target.sendMessage(ChatColor.GREEN + "You were teleported to " + ChatColor.YELLOW + player.getName());
        return true;
    }

    private boolean handleTppos(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.tppos")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /tppos <x> <y> <z>");
            return true;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            player.teleport(new Location(player.getWorld(), x, y, z));
            player.sendMessage(
                    String.format(ChatColor.GREEN + "Teleported to " + ChatColor.YELLOW + "%.1f, %.1f, %.1f", x, y, z));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid coordinates.");
        }
        return true;
    }

    private boolean handleTpall(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.tpall")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p != player) {
                p.teleport(player);
                p.sendMessage(ChatColor.GREEN + "You were teleported to " + ChatColor.YELLOW + player.getName());
                count++;
            }
        }
        player.sendMessage(
                ChatColor.GREEN + "Teleported " + ChatColor.YELLOW + count + ChatColor.GREEN + " players to you");
        return true;
    }

    private boolean handleRtp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "Searching for safe location...");

        Location safe = findSafeRtpLocation(player.getWorld(), player.getLocation());
        if (safe == null) {
            player.sendMessage(ChatColor.RED + "Could not find safe location after 50 attempts!");
            return true;
        }

        player.teleport(safe);
        player.sendMessage(String.format(ChatColor.GREEN + "Teleported to " + ChatColor.YELLOW + "X: %d, Y: %d, Z: %d",
                safe.getBlockX(), safe.getBlockY(), safe.getBlockZ()));
        return true;
    }

    private Location findSafeRtpLocation(World world, Location center) {
        int minDist = 500, maxDist = 5000;

        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            int dist = minDist + RANDOM.nextInt(maxDist - minDist);
            int x = center.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * dist);

            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);

            if (isSafeSpot(loc))
                return loc;
        }
        return null;
    }

    private boolean isSafeSpot(Location loc) {
        if (loc.getBlock().getType() != Material.AIR)
            return false;
        if (loc.add(0, 1, 0).getBlock().getType() != Material.AIR)
            return false;
        Material below = loc.subtract(0, 2, 0).getBlock().getType();
        return below.isSolid() && below != Material.LAVA && below != Material.MAGMA_BLOCK;
    }

    // === PLAYER UTILITIES ===

    private boolean handleNick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        if (args.length == 0) {
            nicknames.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Nickname cleared");
        } else {
            String nick = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
            nicknames.put(player.getUniqueId(), nick);
            player.setDisplayName(nick);
            player.sendMessage(ChatColor.GREEN + "Nickname set to: " + nick);
        }
        return true;
    }

    private boolean handleSeen(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /seen <player>");
            return true;
        }

        Player online = Bukkit.getPlayer(args[0]);
        if (online != null) {
            sender.sendMessage(
                    ChatColor.YELLOW + args[0] + ChatColor.GRAY + " is currently " + ChatColor.GREEN + "online");
        } else {
            sender.sendMessage(ChatColor.YELLOW + args[0] + ChatColor.GRAY + " is " + ChatColor.RED + "offline");
        }
        return true;
    }

    private boolean handleWhois(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /whois <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        sender.sendMessage(
                ChatColor.GOLD + "=== Player Info: " + ChatColor.YELLOW + target.getName() + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + target.getUniqueId());
        sender.sendMessage(ChatColor.GRAY + "Ping: " + ChatColor.WHITE + target.getPing() + "ms");
        Location loc = target.getLocation();
        sender.sendMessage(String.format(ChatColor.GRAY + "Location: " + ChatColor.WHITE + "%d, %d, %d in %s",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName()));
        sender.sendMessage(ChatColor.GRAY + "Health: " + ChatColor.RED + (int) target.getHealth() + "/20");
        sender.sendMessage(ChatColor.GRAY + "Food: " + ChatColor.YELLOW + target.getFoodLevel() + "/20");
        return true;
    }

    private boolean handlePing(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        int ping = player.getPing();
        ChatColor color = ping < 50 ? ChatColor.GREEN : ping < 150 ? ChatColor.YELLOW : ChatColor.RED;
        player.sendMessage(ChatColor.GRAY + "Your ping: " + color + ping + "ms");
        return true;
    }

    private boolean handleNear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        int radius = args.length > 0 ? parseInt(args[0], 100) : 100;
        List<String> nearby = new ArrayList<>();

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other != player && other.getWorld().equals(player.getWorld())) {
                double dist = player.getLocation().distance(other.getLocation());
                if (dist <= radius) {
                    nearby.add(String.format(ChatColor.YELLOW + "%s " + ChatColor.GRAY + "(%.0fm)", other.getName(),
                            dist));
                }
            }
        }

        if (nearby.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No players within " + radius + " blocks");
        } else {
            player.sendMessage(ChatColor.GOLD + "Nearby players: " + String.join(", ", nearby));
        }
        return true;
    }

    private boolean handleGetpos(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Location loc = player.getLocation();
        player.sendMessage(String.format(ChatColor.GRAY + "Position: " + ChatColor.YELLOW + "X: %d, Y: %d, Z: %d",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        return true;
    }

    private boolean handlePlaytime(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long seconds = ticks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        player.sendMessage(String.format(ChatColor.GRAY + "Playtime: " + ChatColor.YELLOW + "%dh %dm", hours, minutes));
        return true;
    }

    // === MESSAGING ===

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /msg <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        Set<UUID> ignored = ignoreList.getOrDefault(target.getUniqueId(), Set.of());
        if (ignored.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This player is ignoring you");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        player.sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "me " + ChatColor.GRAY + "-> " +
                ChatColor.YELLOW + target.getName() + ChatColor.GRAY + "] " + ChatColor.WHITE + message);
        target.sendMessage(ChatColor.GRAY + "[" + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " -> " +
                ChatColor.GOLD + "me" + ChatColor.GRAY + "] " + ChatColor.WHITE + message);

        lastMessaged.put(player.getUniqueId(), target.getUniqueId());
        lastMessaged.put(target.getUniqueId(), player.getUniqueId());
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /r <message>");
            return true;
        }

        UUID lastUuid = lastMessaged.get(player.getUniqueId());
        if (lastUuid == null) {
            player.sendMessage(ChatColor.RED + "No one to reply to");
            return true;
        }

        Player target = Bukkit.getPlayer(lastUuid);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player is offline");
            return true;
        }

        String[] newArgs = new String[args.length + 1];
        newArgs[0] = target.getName();
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return handleMsg(sender, newArgs);
    }

    private boolean handleIgnore(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /ignore <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        Set<UUID> ignored = ignoreList.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        if (ignored.contains(target.getUniqueId())) {
            ignored.remove(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "No longer ignoring " + ChatColor.YELLOW + target.getName());
        } else {
            ignored.add(target.getUniqueId());
            player.sendMessage(ChatColor.RED + "Now ignoring " + ChatColor.YELLOW + target.getName());
        }
        return true;
    }

    // === ADMIN COMMANDS ===

    private boolean handleHeal(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.heal")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(ChatColor.GREEN + "You have been healed!");
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Healed " + ChatColor.YELLOW + target.getName());
        }
        return true;
    }

    private boolean handleFeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.feed")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(ChatColor.GREEN + "You have been fed!");
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Fed " + ChatColor.YELLOW + target.getName());
        }
        return true;
    }

    private boolean handleFly(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.fly")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        target.setAllowFlight(!target.getAllowFlight());
        String status = target.getAllowFlight() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        target.sendMessage(ChatColor.GREEN + "Fly mode " + status);
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Fly mode " + status + ChatColor.GREEN + " for " + ChatColor.YELLOW
                    + target.getName());
        }
        return true;
    }

    private boolean handleGod(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.god")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        target.setInvulnerable(!target.isInvulnerable());
        String status = target.isInvulnerable() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        target.sendMessage(ChatColor.GREEN + "God mode " + status);
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "God mode " + status + ChatColor.GREEN + " for " + ChatColor.YELLOW
                    + target.getName());
        }
        return true;
    }

    private boolean handleSpeed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.speed")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /speed <0-10>");
            return true;
        }

        try {
            float speed = Float.parseFloat(args[0]) / 10;
            if (speed < 0 || speed > 1) {
                sender.sendMessage(ChatColor.RED + "Speed must be between 0 and 10.");
                return true;
            }
            if (player.isFlying()) {
                player.setFlySpeed(speed);
            } else {
                player.setWalkSpeed(speed);
            }
            player.sendMessage(ChatColor.GREEN + "Speed set to " + ChatColor.YELLOW + (speed * 10));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
        }
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.clear")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        target.getInventory().clear();
        target.sendMessage(ChatColor.GREEN + "Inventory cleared");
        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "Cleared inventory of " + ChatColor.YELLOW + target.getName());
        }
        return true;
    }

    private boolean handleRepair(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.repair")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || !(item.getItemMeta() instanceof Damageable)) {
            player.sendMessage(ChatColor.RED + "Hold a repairable item");
            return true;
        }

        Damageable meta = (Damageable) item.getItemMeta();
        meta.setDamage(0);
        item.setItemMeta(meta);
        player.sendMessage(ChatColor.GREEN + "Item repaired");
        return true;
    }

    private boolean handleMore(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.more")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item");
            return true;
        }

        item.setAmount(item.getMaxStackSize());
        player.sendMessage(ChatColor.GREEN + "Stack filled to " + item.getAmount());
        return true;
    }

    private boolean handleHat(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold an item to wear as a hat");
            return true;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(hand.clone());
        player.getInventory().setItemInMainHand(helmet);
        player.sendMessage(
                ChatColor.GREEN + "You are now wearing " + hand.getType().name().toLowerCase().replace("_", " "));
        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vonixcore.broadcast")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /broadcast <message>");
            return true;
        }

        String message = ChatColor.translateAlternateColorCodes('&', String.join(" ", args));
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[Broadcast] " + ChatColor.WHITE + message);
        return true;
    }

    private boolean handleInvsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.invsee")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /invsee <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        player.openInventory(target.getInventory());
        return true;
    }

    private boolean handleEnderchest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (!sender.hasPermission("vonixcore.enderchest")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : player;
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        player.openInventory(target.getEnderChest());
        return true;
    }

    private boolean handleWorkbench(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        player.openWorkbench(null, true);
        return true;
    }

    private boolean handleGc(CommandSender sender) {
        if (!sender.hasPermission("vonixcore.gc")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long max = rt.maxMemory() / 1024 / 1024;
        System.gc();

        sender.sendMessage(ChatColor.GOLD + "=== Server Stats ===");
        sender.sendMessage(ChatColor.GRAY + "Memory: " + ChatColor.YELLOW + used + "MB" + ChatColor.GRAY + "/"
                + ChatColor.YELLOW + max + "MB");
        sender.sendMessage(ChatColor.GRAY + "Online: " + ChatColor.YELLOW + Bukkit.getOnlinePlayers().size() + "/"
                + Bukkit.getMaxPlayers());
        sender.sendMessage(ChatColor.GRAY + "Worlds: " + ChatColor.YELLOW + Bukkit.getWorlds().size());
        return true;
    }

    private boolean handleLag(CommandSender sender) {
        double[] tps = Bukkit.getTPS();
        ChatColor color = tps[0] >= 18 ? ChatColor.GREEN : tps[0] >= 15 ? ChatColor.YELLOW : ChatColor.RED;
        sender.sendMessage(ChatColor.GRAY + "TPS: " + color + String.format("%.1f", tps[0]));
        return true;
    }

    // Helpers
    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String getNickname(UUID uuid) {
        return nicknames.get(uuid);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String cmd = command.getName().toLowerCase();
            if (cmd.equals("tp") || cmd.equals("tphere") || cmd.equals("msg") || cmd.equals("tell") ||
                    cmd.equals("whois") || cmd.equals("seen") || cmd.equals("invsee") || cmd.equals("heal") ||
                    cmd.equals("feed") || cmd.equals("fly") || cmd.equals("god") || cmd.equals("clear") ||
                    cmd.equals("ignore") || cmd.equals("enderchest") || cmd.equals("ec")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}
