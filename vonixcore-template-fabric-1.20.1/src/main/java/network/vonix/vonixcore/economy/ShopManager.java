package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages server shops for buying/selling items.
 */
public class ShopManager {

    private static ShopManager instance;

    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }

    /**
     * Initialize shop tables in database.
     */
    public void initializeTable(Connection conn) throws SQLException {
        // Player shops table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_shops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        owner_uuid TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        item TEXT NOT NULL,
                        buy_price REAL,
                        sell_price REAL,
                        stock INTEGER DEFAULT 0,
                        created_at INTEGER NOT NULL
                    )
                """);

        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_shops_owner ON vc_shops (owner_uuid)");
        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_shops_location ON vc_shops (world, x, y, z)");
    }
}
