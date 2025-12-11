package network.vonix.vonixcore.claims;

import network.vonix.vonixcore.VonixCore;
import org.bukkit.Location;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages land claims - creation, deletion, and permission checks.
 */
public class ClaimsManager {

    private static ClaimsManager instance;
    private final VonixCore plugin;

    private final Map<Integer, Claim> claims = new ConcurrentHashMap<>();
    private final Map<UUID, Location> corner1Selections = new ConcurrentHashMap<>();
    private final Map<UUID, Location> corner2Selections = new ConcurrentHashMap<>();

    // Config values
    private int defaultClaimRadius = 10;
    private int maxClaimSize = 100;
    private int maxClaimsPerPlayer = 5;
    private boolean requirePermissionToCreate = false;
    private boolean protectBuilding = true;
    private boolean protectContainers = true;
    private boolean preventExplosions = true;
    private boolean allowVonixShopsBypass = true;

    public ClaimsManager(VonixCore plugin) {
        this.plugin = plugin;
        instance = this;
        loadConfig();
    }

    public static ClaimsManager getInstance() {
        return instance;
    }

    private void loadConfig() {
        var config = plugin.getConfig();
        defaultClaimRadius = config.getInt("claims.defaultClaimRadius", 10);
        maxClaimSize = config.getInt("claims.maxClaimSize", 100);
        maxClaimsPerPlayer = config.getInt("claims.maxClaimsPerPlayer", 5);
        requirePermissionToCreate = config.getBoolean("claims.requirePermissionToCreate", false);
        protectBuilding = config.getBoolean("claims.protectBuilding", true);
        protectContainers = config.getBoolean("claims.protectContainers", true);
        preventExplosions = config.getBoolean("claims.preventExplosions", true);
        allowVonixShopsBypass = config.getBoolean("claims.allowVonixShopsBypass", true);
    }

    /**
     * Initialize claims table in database
     */
    public void initializeTable(Connection conn) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS vonixcore_claims (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner VARCHAR(36) NOT NULL,
                    owner_name VARCHAR(16),
                    world VARCHAR(128) NOT NULL,
                    x1 INT NOT NULL, y1 INT NOT NULL, z1 INT NOT NULL,
                    x2 INT NOT NULL, y2 INT NOT NULL, z2 INT NOT NULL,
                    trusted TEXT,
                    created_at BIGINT NOT NULL
                )
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        loadClaims(conn);
        plugin.getLogger().info("[VonixCore] Claims table initialized, loaded " + claims.size() + " claims");
    }

    /**
     * Load all claims from database
     */
    private void loadClaims(Connection conn) throws SQLException {
        claims.clear();
        String sql = "SELECT * FROM vonixcore_claims";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                UUID owner = UUID.fromString(rs.getString("owner"));
                String ownerName = rs.getString("owner_name");
                String world = rs.getString("world");
                int x1 = rs.getInt("x1"), y1 = rs.getInt("y1"), z1 = rs.getInt("z1");
                int x2 = rs.getInt("x2"), y2 = rs.getInt("y2"), z2 = rs.getInt("z2");
                String trustedJson = rs.getString("trusted");
                long createdAt = rs.getLong("created_at");

                Set<UUID> trusted = parseTrusted(trustedJson);
                Claim claim = new Claim(id, owner, ownerName, world, x1, y1, z1, x2, y2, z2, trusted, createdAt);
                claims.put(id, claim);
            }
        }
    }

    /**
     * Create a new claim
     */
    public Claim createClaim(UUID owner, String ownerName, String world,
            Location pos1, Location pos2) {
        // Validate size
        int sizeX = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int sizeZ = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
        if (maxClaimSize > 0 && (sizeX > maxClaimSize || sizeZ > maxClaimSize)) {
            return null; // Too large
        }

        // Check overlap
        for (Claim existing : claims.values()) {
            if (existing.getWorld().equals(world)) {
                if (claimsOverlap(pos1, pos2, existing)) {
                    return null; // Overlaps
                }
            }
        }

        // Check player claim limit
        if (maxClaimsPerPlayer > 0) {
            long playerClaims = claims.values().stream()
                    .filter(c -> c.getOwner().equals(owner))
                    .count();
            if (playerClaims >= maxClaimsPerPlayer) {
                return null; // Limit reached
            }
        }

        try (Connection conn = plugin.getDatabase().getConnection()) {
            String sql = """
                    INSERT INTO vonixcore_claims
                    (owner, owner_name, world, x1, y1, z1, x2, y2, z2, trusted, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, owner.toString());
                ps.setString(2, ownerName);
                ps.setString(3, world);
                ps.setInt(4, Math.min(pos1.getBlockX(), pos2.getBlockX()));
                ps.setInt(5, Math.min(pos1.getBlockY(), pos2.getBlockY()));
                ps.setInt(6, Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
                ps.setInt(7, Math.max(pos1.getBlockX(), pos2.getBlockX()));
                ps.setInt(8, Math.max(pos1.getBlockY(), pos2.getBlockY()));
                ps.setInt(9, Math.max(pos1.getBlockZ(), pos2.getBlockZ()));
                ps.setString(10, "[]");
                ps.setLong(11, System.currentTimeMillis());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        Claim claim = new Claim(id, owner, ownerName, world,
                                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ(),
                                new HashSet<>(), System.currentTimeMillis());
                        claims.put(id, claim);
                        return claim;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[VonixCore] Failed to create claim: " + e.getMessage());
        }
        return null;
    }

    /**
     * Delete a claim
     */
    public boolean deleteClaim(int claimId) {
        if (!claims.containsKey(claimId))
            return false;

        try (Connection conn = plugin.getDatabase().getConnection()) {
            String sql = "DELETE FROM vonixcore_claims WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, claimId);
                ps.executeUpdate();
            }
            claims.remove(claimId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("[VonixCore] Failed to delete claim: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get claim at a location
     */
    public Claim getClaimAt(Location loc) {
        for (Claim claim : claims.values()) {
            if (claim.contains(loc)) {
                return claim;
            }
        }
        return null;
    }

    /**
     * Get all claims owned by a player
     */
    public List<Claim> getPlayerClaims(UUID owner) {
        List<Claim> result = new ArrayList<>();
        for (Claim claim : claims.values()) {
            if (claim.getOwner().equals(owner)) {
                result.add(claim);
            }
        }
        return result;
    }

    /**
     * Add trusted player to claim
     */
    public boolean trustPlayer(int claimId, UUID player) {
        Claim claim = claims.get(claimId);
        if (claim == null)
            return false;

        claim.addTrusted(player);
        saveTrusted(claimId, claim.getTrusted());
        return true;
    }

    /**
     * Remove trusted player from claim
     */
    public boolean untrustPlayer(int claimId, UUID player) {
        Claim claim = claims.get(claimId);
        if (claim == null)
            return false;

        claim.removeTrusted(player);
        saveTrusted(claimId, claim.getTrusted());
        return true;
    }

    /**
     * Check if player can build at location
     */
    public boolean canBuild(UUID player, Location loc) {
        Claim claim = getClaimAt(loc);
        if (claim == null)
            return true; // No claim = can build
        return claim.canInteract(player);
    }

    /**
     * Check if player can interact at location
     */
    public boolean canInteract(UUID player, Location loc) {
        Claim claim = getClaimAt(loc);
        if (claim == null)
            return true; // No claim = can interact
        return claim.canInteract(player);
    }

    // Selection management
    public void setCorner1(UUID player, Location loc) {
        corner1Selections.put(player, loc);
    }

    public void setCorner2(UUID player, Location loc) {
        corner2Selections.put(player, loc);
    }

    public Location getCorner1(UUID player) {
        return corner1Selections.get(player);
    }

    public Location getCorner2(UUID player) {
        return corner2Selections.get(player);
    }

    public void clearSelection(UUID player) {
        corner1Selections.remove(player);
        corner2Selections.remove(player);
    }

    public boolean hasSelection(UUID player) {
        return corner1Selections.containsKey(player) && corner2Selections.containsKey(player);
    }

    // Config getters
    public int getDefaultClaimRadius() {
        return defaultClaimRadius;
    }

    public int getMaxClaimSize() {
        return maxClaimSize;
    }

    public boolean isRequirePermissionToCreate() {
        return requirePermissionToCreate;
    }

    public boolean isProtectBuilding() {
        return protectBuilding;
    }

    public boolean isProtectContainers() {
        return protectContainers;
    }

    public boolean isPreventExplosions() {
        return preventExplosions;
    }

    public boolean isAllowVonixShopsBypass() {
        return allowVonixShopsBypass;
    }

    // Helper methods
    private boolean claimsOverlap(Location pos1, Location pos2, Claim existing) {
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return !(maxX < existing.getX1() || minX > existing.getX2() ||
                maxZ < existing.getZ1() || minZ > existing.getZ2());
    }

    private Set<UUID> parseTrusted(String json) {
        Set<UUID> trusted = new HashSet<>();
        if (json == null || json.isEmpty() || json.equals("[]"))
            return trusted;
        json = json.replace("[", "").replace("]", "").replace("\"", "");
        for (String part : json.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try {
                    trusted.add(UUID.fromString(trimmed));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return trusted;
    }

    private void saveTrusted(int claimId, Set<UUID> trusted) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (UUID uuid : trusted) {
            if (!first)
                json.append(",");
            json.append("\"").append(uuid.toString()).append("\"");
            first = false;
        }
        json.append("]");

        try (Connection conn = plugin.getDatabase().getConnection()) {
            String sql = "UPDATE vonixcore_claims SET trusted = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, json.toString());
                ps.setInt(2, claimId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[VonixCore] Failed to save trusted: " + e.getMessage());
        }
    }

    public int getClaimCount() {
        return claims.size();
    }

    public Claim getClaim(int id) {
        return claims.get(id);
    }
}
