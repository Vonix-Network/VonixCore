package network.vonix.vonixcore.claims;

import network.vonix.vonixcore.VonixCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Manages land claims for player protection.
 */
public class ClaimsManager {

    private static ClaimsManager instance;

    public static ClaimsManager getInstance() {
        if (instance == null) {
            instance = new ClaimsManager();
        }
        return instance;
    }

    /**
     * Initialize claims tables in database.
     */
    public void initializeTable(Connection conn) throws SQLException {
        // Claims table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_claims (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner_uuid TEXT NOT NULL,
                        world TEXT NOT NULL,
                        min_x INTEGER NOT NULL,
                        min_z INTEGER NOT NULL,
                        max_x INTEGER NOT NULL,
                        max_z INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """);

        // Claim trust table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_claim_trust (
                        claim_id INTEGER NOT NULL,
                        trusted_uuid TEXT NOT NULL,
                        trust_level TEXT NOT NULL,
                        PRIMARY KEY (claim_id, trusted_uuid),
                        FOREIGN KEY (claim_id) REFERENCES vc_claims(id) ON DELETE CASCADE
                    )
                """);

        // Player claim blocks
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_claim_blocks (
                        uuid TEXT PRIMARY KEY,
                        blocks INTEGER DEFAULT 500,
                        last_accrual INTEGER NOT NULL
                    )
                """);

        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_claims_owner ON vc_claims (owner_uuid)");
        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_claims_location ON vc_claims (world, min_x, min_z, max_x, max_z)");
    }

    public boolean isLocationClaimed(String world, int x, int z) {
        // TODO: Implement claim lookup
        return false;
    }

    public boolean canPlayerBuild(UUID playerUuid, String world, int x, int z) {
        // TODO: Implement permission check
        return true;
    }
}
