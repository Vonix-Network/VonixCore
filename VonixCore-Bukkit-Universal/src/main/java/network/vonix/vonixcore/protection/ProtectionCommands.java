package network.vonix.vonixcore.protection;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CoreProtect-style protection commands for Bukkit.
 * Provides lookup, rollback, restore, inspect, and purge functionality.
 */
public class ProtectionCommands implements CommandExecutor, TabCompleter {

    private final VonixCore plugin;

    // Players currently in inspector mode
    private static final Set<UUID> inspectorMode = ConcurrentHashMap.newKeySet();

    // History of rollbacks for undo
    private static final Map<UUID, Deque<RollbackData>> rollbackHistory = new ConcurrentHashMap<>();

    public ProtectionCommands(VonixCore plugin) {
        this.plugin = plugin;
    }

    public static boolean isInspecting(UUID uuid) {
        return inspectorMode.contains(uuid);
    }

    public static void inspectBlock(Player player, Block block) {
        String world = block.getWorld().getName();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        VonixCore.getInstance().getServer().getScheduler().runTaskAsynchronously(VonixCore.getInstance(), () -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                String sql = "SELECT time, user, type, old_type, new_type, action FROM vp_block " +
                        "WHERE world = ? AND x = ? AND y = ? AND z = ? " +
                        "ORDER BY time DESC LIMIT 10";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, world);
                    stmt.setInt(2, x);
                    stmt.setInt(3, y);
                    stmt.setInt(4, z);

                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> results = new ArrayList<>();
                        results.add(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Block History (" + x + ", " + y + ", "
                                + z + ") ===");

                        boolean hasResults = false;
                        while (rs.next()) {
                            hasResults = true;
                            long time = rs.getLong("time");
                            String user = rs.getString("user");
                            String oldType = rs.getString("old_type");
                            String newType = rs.getString("new_type");
                            int action = rs.getInt("action");

                            String actionStr = getActionString(action);
                            String timeStr = formatTimeAgo(time);
                            String blockInfo = action == 0 ? oldType : newType;

                            results.add(ChatColor.GRAY + timeStr + ChatColor.WHITE + " - " + ChatColor.AQUA + user
                                    + ChatColor.GRAY + " " + actionStr + ChatColor.YELLOW + " "
                                    + formatBlockName(blockInfo));
                        }

                        if (!hasResults) {
                            results.add(ChatColor.GRAY + "No changes recorded at this location.");
                        }

                        // Send results on main thread
                        Bukkit.getScheduler().runTask(VonixCore.getInstance(), () -> {
                            for (String msg : results) {
                                player.sendMessage(msg);
                            }
                        });
                    }
                }
            } catch (SQLException e) {
                VonixCore.getInstance().getLogger().severe("[Protection] Error inspecting block: " + e.getMessage());
                Bukkit.getScheduler().runTask(VonixCore.getInstance(),
                        () -> player.sendMessage(ChatColor.RED + "Error querying block history."));
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /co and /vp commands
        if (command.getName().equalsIgnoreCase("co") || command.getName().equalsIgnoreCase("vp")) {
            if (!sender.hasPermission("vonixcore.protection.use")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                return helpCommand(sender);
            }

            String subCmd = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            return switch (subCmd) {
                case "help", "?" -> helpCommand(sender);
                case "inspect", "i" -> inspectCommand(sender);
                case "lookup", "l" -> lookupCommand(sender, subArgs);
                case "rollback", "rb" -> rollbackCommand(sender, subArgs, true);
                case "restore", "rs" -> restoreCommand(sender, subArgs, false);
                case "undo" -> undoCommand(sender);
                case "purge" -> purgeCommand(sender, subArgs);
                case "status" -> statusCommand(sender);
                case "near" -> nearCommand(sender, subArgs);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /co help for help.");
                    yield true;
                }
            };
        }

        return false;
    }

    private boolean helpCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== VonixCore Protection (CoreProtect) ===");
        sender.sendMessage(ChatColor.AQUA + "/co inspect" + ChatColor.GRAY + " - Toggle inspector mode");
        sender.sendMessage(
                ChatColor.AQUA + "/co lookup u:<user> t:<time> r:<radius>" + ChatColor.GRAY + " - Search logs");
        sender.sendMessage(
                ChatColor.AQUA + "/co rollback u:<user> t:<time> r:<radius>" + ChatColor.GRAY + " - Rollback changes");
        sender.sendMessage(
                ChatColor.AQUA + "/co restore u:<user> t:<time> r:<radius>" + ChatColor.GRAY + " - Restore changes");
        sender.sendMessage(ChatColor.AQUA + "/co undo" + ChatColor.GRAY + " - Undo last rollback/restore");
        sender.sendMessage(ChatColor.AQUA + "/co purge t:<time>" + ChatColor.GRAY + " - Delete old data");
        sender.sendMessage(ChatColor.AQUA + "/co near [radius]" + ChatColor.GRAY + " - Lookup nearby changes");
        sender.sendMessage(ChatColor.AQUA + "/co status" + ChatColor.GRAY + " - Show database status");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Parameters:");
        sender.sendMessage(ChatColor.GRAY + "  u:<user> " + ChatColor.WHITE + "- Player name");
        sender.sendMessage(ChatColor.GRAY + "  t:<time> " + ChatColor.WHITE + "- Time (e.g., 1h, 3d, 1w)");
        sender.sendMessage(ChatColor.GRAY + "  r:<radius> " + ChatColor.WHITE + "- Radius (default: 10)");
        sender.sendMessage(ChatColor.GRAY + "  a:<action> " + ChatColor.WHITE + "- Action filter");
        sender.sendMessage(ChatColor.GRAY + "  b:<block> " + ChatColor.WHITE + "- Block type filter");
        return true;
    }

    private boolean inspectCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires a player.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (inspectorMode.contains(uuid)) {
            inspectorMode.remove(uuid);
            player.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE + "Inspector mode " + ChatColor.RED
                    + "disabled" + ChatColor.WHITE + ".");
        } else {
            inspectorMode.add(uuid);
            player.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE + "Inspector mode " + ChatColor.GREEN
                    + "enabled" + ChatColor.WHITE + ". Click on blocks to see history.");
        }
        return true;
    }

    private boolean lookupCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires a player.");
            return true;
        }

        String params = args.length > 0 ? String.join(" ", args) : "r:5 t:3d";
        lookupWithParams(player, params);
        return true;
    }

    private void lookupWithParams(Player player, String params) {
        LookupParams parsed = parseParams(params);
        if (parsed.radius <= 0)
            parsed.radius = ProtectionConfig.defaultRadius;
        if (parsed.time <= 0)
            parsed.time = ProtectionConfig.defaultTime;

        Location loc = player.getLocation();
        String world = player.getWorld().getName();
        int maxResults = ProtectionConfig.maxLookupResults;

        // Execute query async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                StringBuilder sql = new StringBuilder(
                        "SELECT time, user, x, y, z, type, old_type, new_type, action FROM vp_block WHERE world = ? ");

                List<Object> queryParams = new ArrayList<>();
                queryParams.add(world);

                // Time filter
                long minTime = (System.currentTimeMillis() / 1000L) - parsed.time;
                sql.append("AND time >= ? ");
                queryParams.add(minTime);

                // Radius filter
                int bx = loc.getBlockX();
                int by = loc.getBlockY();
                int bz = loc.getBlockZ();
                sql.append("AND x >= ? AND x <= ? AND y >= ? AND y <= ? AND z >= ? AND z <= ? ");
                queryParams.add(bx - parsed.radius);
                queryParams.add(bx + parsed.radius);
                queryParams.add(by - parsed.radius);
                queryParams.add(by + parsed.radius);
                queryParams.add(bz - parsed.radius);
                queryParams.add(bz + parsed.radius);

                // User filter
                if (parsed.user != null && !parsed.user.isEmpty()) {
                    sql.append("AND user = ? ");
                    queryParams.add(parsed.user);
                }

                // Block filter
                if (parsed.block != null && !parsed.block.isEmpty()) {
                    sql.append("AND (type LIKE ? OR old_type LIKE ? OR new_type LIKE ?) ");
                    String blockPattern = "%" + parsed.block + "%";
                    queryParams.add(blockPattern);
                    queryParams.add(blockPattern);
                    queryParams.add(blockPattern);
                }

                // Action filter
                if (parsed.action != null) {
                    sql.append("AND action = ? ");
                    queryParams.add(parsed.action);
                }

                sql.append("ORDER BY time DESC LIMIT ?");
                queryParams.add(maxResults);

                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < queryParams.size(); i++) {
                        stmt.setObject(i + 1, queryParams.get(i));
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        List<String> results = new ArrayList<>();
                        results.add(ChatColor.GOLD + "" + ChatColor.BOLD + "=== VonixCore Lookup Results ===");

                        int count = 0;
                        while (rs.next()) {
                            count++;
                            long time = rs.getLong("time");
                            String user = rs.getString("user");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            String oldType = rs.getString("old_type");
                            String newType = rs.getString("new_type");
                            int action = rs.getInt("action");

                            String actionStr = getActionString(action);
                            String timeStr = formatTimeAgo(time);
                            String blockInfo = action == 0 ? oldType : newType;

                            results.add(ChatColor.GRAY + timeStr + ChatColor.WHITE + " - " +
                                    ChatColor.AQUA + user + ChatColor.GRAY + " " + actionStr + " " +
                                    ChatColor.YELLOW + formatBlockName(blockInfo) + ChatColor.GRAY + " at " +
                                    ChatColor.GREEN + "(" + x + ", " + y + ", " + z + ")");
                        }

                        if (count == 0) {
                            results.add(ChatColor.GRAY + "No results found.");
                        } else {
                            results.add(
                                    ChatColor.GRAY + "Found " + ChatColor.WHITE + count + ChatColor.GRAY + " results.");
                        }

                        // Send results on main thread
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            for (String msg : results) {
                                player.sendMessage(msg);
                            }
                        });
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[Protection] Lookup error: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> player.sendMessage(ChatColor.RED + "Error executing lookup query."));
            }
        });
    }

    private boolean rollbackCommand(CommandSender sender, String[] args, boolean isRollback) {
        return rollbackRestoreExec(sender, args, isRollback);
    }

    private boolean restoreCommand(CommandSender sender, String[] args, boolean isRollback) {
        return rollbackRestoreExec(sender, args, isRollback);
    }

    private boolean rollbackRestoreExec(CommandSender sender, String[] args, boolean isRollback) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires a player.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please specify parameters (e.g., u:player t:1h r:10)");
            return true;
        }

        String params = String.join(" ", args);
        LookupParams parsed = parseParams(params);
        if (parsed.radius <= 0)
            parsed.radius = ProtectionConfig.defaultRadius;
        if (parsed.time <= 0)
            parsed.time = ProtectionConfig.defaultTime;

        // Require user for large operations
        if (parsed.user == null && parsed.radius > 20) {
            sender.sendMessage(ChatColor.RED + "Please specify a user (u:<name>) for large radius rollbacks.");
            return true;
        }

        Location loc = player.getLocation();
        String world = player.getWorld().getName();
        String operation = isRollback ? "rollback" : "restore";

        player.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE + "Starting " + operation + "... (r:"
                + parsed.radius + " t:" + formatDuration(parsed.time) + ")");

        // Execute async
        LookupParams finalParsed = parsed;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                long minTime = (System.currentTimeMillis() / 1000L) - finalParsed.time;

                // Build query (similar to lookup)
                StringBuilder sql = new StringBuilder(
                        "SELECT id, x, y, z, old_type, old_data, new_type, new_data, action FROM vp_block WHERE world = ? AND time >= ? ");

                List<Object> queryParams = new ArrayList<>();
                queryParams.add(world);
                queryParams.add(minTime);

                // Radius filter
                int bx = loc.getBlockX();
                int by = loc.getBlockY();
                int bz = loc.getBlockZ();
                sql.append("AND x >= ? AND x <= ? AND y >= ? AND y <= ? AND z >= ? AND z <= ? ");
                queryParams.add(bx - finalParsed.radius);
                queryParams.add(bx + finalParsed.radius);
                queryParams.add(by - finalParsed.radius);
                queryParams.add(by + finalParsed.radius);
                queryParams.add(bz - finalParsed.radius);
                queryParams.add(bz + finalParsed.radius);

                // User filter
                if (finalParsed.user != null && !finalParsed.user.isEmpty()) {
                    sql.append("AND user = ? ");
                    queryParams.add(finalParsed.user);
                }

                // Block filter
                if (finalParsed.block != null && !finalParsed.block.isEmpty()) {
                    sql.append("AND (type LIKE ? OR old_type LIKE ? OR new_type LIKE ?) ");
                    String blockPattern = "%" + finalParsed.block + "%";
                    queryParams.add(blockPattern);
                    queryParams.add(blockPattern);
                    queryParams.add(blockPattern);
                }

                sql.append("ORDER BY time ");
                sql.append(isRollback ? "DESC" : "ASC");

                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < queryParams.size(); i++) {
                        stmt.setObject(i + 1, queryParams.get(i));
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        List<BlockChange> changes = new ArrayList<>();

                        while (rs.next()) {
                            BlockChange change = new BlockChange();
                            change.id = rs.getLong("id");
                            change.x = rs.getInt("x");
                            change.y = rs.getInt("y");
                            change.z = rs.getInt("z");
                            change.oldType = rs.getString("old_type");
                            change.oldData = rs.getString("old_data");
                            change.newType = rs.getString("new_type");
                            change.newData = rs.getString("new_data");
                            change.action = rs.getInt("action");
                            changes.add(change);
                        }

                        if (changes.isEmpty()) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.GOLD
                                    + "[VonixCore] " + ChatColor.GRAY + "No changes to " + operation + "."));
                            return;
                        }

                        // Apply changes on main thread
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            int modified = 0;
                            for (BlockChange change : changes) {
                                Location blockLoc = new Location(player.getWorld(), change.x, change.y, change.z);
                                String targetBlock = isRollback ? change.oldType : change.newType;

                                if (targetBlock != null && !targetBlock.isEmpty()) {
                                    Material mat = getMaterial(targetBlock);
                                    if (mat != null) {
                                        blockLoc.getBlock().setType(mat);
                                        modified++;
                                    }
                                }
                            }

                            player.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE +
                                    (isRollback ? "Rollback" : "Restore") + " complete. " + ChatColor.GREEN + modified
                                    + ChatColor.WHITE + " blocks modified.");

                            // Store for undo
                            RollbackData undoData = new RollbackData();
                            undoData.changes = changes;
                            undoData.wasRollback = isRollback;
                            undoData.world = world;

                            rollbackHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>())
                                    .push(undoData);
                        });
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("[Protection] Rollback error: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> player.sendMessage(ChatColor.RED + "Error executing " + operation + "."));
            }
        });

        return true;
    }

    private boolean undoCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires a player.");
            return true;
        }

        Deque<RollbackData> history = rollbackHistory.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No rollback/restore to undo.");
            return true;
        }

        RollbackData lastOp = history.pop();

        int modified = 0;
        for (BlockChange change : lastOp.changes) {
            Location loc = new Location(player.getWorld(), change.x, change.y, change.z);
            String targetBlock = lastOp.wasRollback ? change.newType : change.oldType;

            if (targetBlock != null && !targetBlock.isEmpty()) {
                Material mat = getMaterial(targetBlock);
                if (mat != null) {
                    loc.getBlock().setType(mat);
                    modified++;
                }
            }
        }

        player.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE + "Undo complete. " + ChatColor.GREEN
                + modified + ChatColor.WHITE + " blocks restored.");
        return true;
    }

    private boolean nearCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires a player.");
            return true;
        }

        int radius = args.length > 0 ? parseInt(args[0], 5) : 5;
        lookupWithParams(player, "r:" + radius + " t:3d");
        return true;
    }

    private boolean purgeCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please specify time (e.g., t:30d)");
            return true;
        }

        String timeStr = args[0].startsWith("t:") ? args[0].substring(2) : args[0];
        long seconds = parseTime(timeStr);

        if (seconds < 86400) {
            sender.sendMessage(ChatColor.RED + "Minimum purge time is 1 day (1d).");
            return true;
        }

        long cutoff = (System.currentTimeMillis() / 1000L) - seconds;
        sender.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE + "Purging data older than "
                + formatDuration(seconds) + "...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                int deleted = 0;

                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM vp_block WHERE time < ?")) {
                    stmt.setLong(1, cutoff);
                    deleted += stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM vp_container WHERE time < ?")) {
                    stmt.setLong(1, cutoff);
                    deleted += stmt.executeUpdate();
                }

                int finalDeleted = deleted;
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(ChatColor.GOLD + "[VonixCore] " + ChatColor.WHITE + "Purge complete. "
                                + ChatColor.GREEN + finalDeleted + ChatColor.WHITE + " entries removed."));

            } catch (SQLException e) {
                plugin.getLogger().severe("[Protection] Purge error: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(ChatColor.RED + "Error executing purge."));
            }
        });

        return true;
    }

    private boolean statusCommand(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                long blockCount = 0;
                long containerCount = 0;

                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM vp_block")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            blockCount = rs.getLong(1);
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM vp_container")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            containerCount = rs.getLong(1);
                    }
                }

                long finalBlockCount = blockCount;
                long finalContainerCount = containerCount;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== VonixCore Protection Status ===");
                    sender.sendMessage(
                            ChatColor.GRAY + "Block logs: " + ChatColor.WHITE + String.format("%,d", finalBlockCount));
                    sender.sendMessage(ChatColor.GRAY + "Container logs: " + ChatColor.WHITE
                            + String.format("%,d", finalContainerCount));
                });

            } catch (SQLException e) {
                plugin.getLogger().severe("[Protection] Status error: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage(ChatColor.RED + "Error getting status."));
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "inspect", "lookup", "rollback", "restore", "undo", "purge",
                    "status", "near"));
        }

        return completions;
    }

    // Helper methods

    private LookupParams parseParams(String params) {
        LookupParams result = new LookupParams();
        String[] parts = params.split("\\s+");

        for (String part : parts) {
            if (part.startsWith("u:") || part.startsWith("user:")) {
                result.user = part.substring(part.indexOf(':') + 1);
            } else if (part.startsWith("t:") || part.startsWith("time:")) {
                result.time = parseTime(part.substring(part.indexOf(':') + 1));
            } else if (part.startsWith("r:") || part.startsWith("radius:")) {
                result.radius = parseInt(part.substring(part.indexOf(':') + 1), 10);
            } else if (part.startsWith("b:") || part.startsWith("block:")) {
                result.block = part.substring(part.indexOf(':') + 1);
            } else if (part.startsWith("a:") || part.startsWith("action:")) {
                String action = part.substring(part.indexOf(':') + 1).toLowerCase();
                if (action.equals("break") || action.equals("-block")) {
                    result.action = 0;
                } else if (action.equals("place") || action.equals("+block")) {
                    result.action = 1;
                } else if (action.equals("explode")) {
                    result.action = 2;
                }
            }
        }

        return result;
    }

    private long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty())
            return 0;

        long multiplier = 1;
        String numPart = timeStr;

        if (timeStr.endsWith("s")) {
            multiplier = 1;
            numPart = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("m")) {
            multiplier = 60;
            numPart = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("h")) {
            multiplier = 3600;
            numPart = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("d")) {
            multiplier = 86400;
            numPart = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("w")) {
            multiplier = 604800;
            numPart = timeStr.substring(0, timeStr.length() - 1);
        }

        try {
            return Long.parseLong(numPart) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseInt(String str, int defaultVal) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static String formatTimeAgo(long unixTime) {
        long now = System.currentTimeMillis() / 1000L;
        long diff = now - unixTime;

        if (diff < 60)
            return diff + "s ago";
        if (diff < 3600)
            return (diff / 60) + "m ago";
        if (diff < 86400)
            return (diff / 3600) + "h ago";
        return (diff / 86400) + "d ago";
    }

    private String formatDuration(long seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return (seconds / 60) + "m";
        if (seconds < 86400)
            return (seconds / 3600) + "h";
        return (seconds / 86400) + "d";
    }

    private static String getActionString(int action) {
        return switch (action) {
            case 0 -> "broke";
            case 1 -> "placed";
            case 2 -> "exploded";
            default -> "modified";
        };
    }

    private static String formatBlockName(String blockId) {
        if (blockId == null)
            return "unknown";
        String name = blockId.replace("minecraft:", "").replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private Material getMaterial(String blockId) {
        if (blockId == null || blockId.isEmpty())
            return Material.AIR;

        try {
            String materialName = blockId.replace("minecraft:", "").toUpperCase();
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    }

    // Data classes

    private static class LookupParams {
        String user;
        long time;
        int radius;
        String block;
        Integer action;
    }

    private static class BlockChange {
        long id;
        int x, y, z;
        String oldType, oldData, newType, newData;
        int action;
    }

    private static class RollbackData {
        List<BlockChange> changes;
        boolean wasRollback;
        String world;
    }
}
