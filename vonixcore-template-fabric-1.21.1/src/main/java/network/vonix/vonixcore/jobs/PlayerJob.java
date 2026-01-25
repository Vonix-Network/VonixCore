package network.vonix.vonixcore.jobs;

import java.util.UUID;

/**
 * Represents a player's job data.
 */
public class PlayerJob {
    private final UUID playerUuid;
    private String jobId;
    private int level;
    private int xp;
    private long joinedAt;

    public PlayerJob(UUID playerUuid, String jobId, int level, int xp, long joinedAt) {
        this.playerUuid = playerUuid;
        this.jobId = jobId;
        this.level = level;
        this.xp = xp;
        this.joinedAt = joinedAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public void addXp(int amount) {
        this.xp += amount;
        // Check for level up
        int xpForNextLevel = getXpForNextLevel();
        while (this.xp >= xpForNextLevel) {
            this.xp -= xpForNextLevel;
            this.level++;
        }
    }

    public int getXpForNextLevel() {
        return 100 * level; // Basic formula
    }

    public long getJoinedAt() {
        return joinedAt;
    }
}
