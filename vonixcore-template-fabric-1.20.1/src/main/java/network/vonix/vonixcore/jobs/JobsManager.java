package network.vonix.vonixcore.jobs;

import network.vonix.vonixcore.VonixCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * Manages jobs system - players earn money by performing tasks.
 */
public class JobsManager {

    private static JobsManager instance;
    private final Map<UUID, String> playerJobs = new HashMap<>();

    public static JobsManager getInstance() {
        if (instance == null) {
            instance = new JobsManager();
        }
        return instance;
    }

    /**
     * Initialize jobs system.
     */
    public void initialize(Connection conn) throws SQLException {
        // Player jobs table
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_player_jobs (
                        uuid TEXT PRIMARY KEY,
                        job TEXT NOT NULL,
                        level INTEGER DEFAULT 1,
                        xp INTEGER DEFAULT 0,
                        joined_at INTEGER NOT NULL
                    )
                """);

        // Job earnings log
        conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS vc_job_earnings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        job TEXT NOT NULL,
                        action TEXT NOT NULL,
                        amount REAL NOT NULL,
                        earned_at INTEGER NOT NULL
                    )
                """);

        VonixCore.LOGGER.info("[VonixCore] Jobs system initialized");
    }

    /**
     * Shutdown the jobs system.
     */
    public void shutdown() {
        playerJobs.clear();
    }

    public String getPlayerJob(UUID uuid) {
        return playerJobs.get(uuid);
    }

    public void setPlayerJob(UUID uuid, String job) {
        playerJobs.put(uuid, job);
    }
}
