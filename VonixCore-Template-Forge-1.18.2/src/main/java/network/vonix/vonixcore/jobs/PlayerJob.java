package network.vonix.vonixcore.jobs;

import java.util.UUID;

/**
 * Represents a player's job data.
 */
public class PlayerJob {

    private UUID playerUuid;
    private String jobId;
    private int level;
    private double experience;
    private double points; // Job-specific currency
    private long joinedAt;
    private long lastWorked;

    public PlayerJob() {
        this.level = 1;
        this.experience = 0;
        this.points = 0;
        this.joinedAt = System.currentTimeMillis();
    }

    public PlayerJob(UUID playerUuid, String jobId) {
        this();
        this.playerUuid = playerUuid;
        this.jobId = jobId;
    }

    /**
     * Add experience and check for level up
     */
    public boolean addExperience(double amount, Job job) {
        this.experience += amount;
        this.lastWorked = System.currentTimeMillis();

        // Check for level up
        double required = job.getRequiredExp(level);
        if (experience >= required && level < job.getMaxLevel()) {
            experience -= required;
            level++;
            return true; // Leveled up
        }
        return false;
    }

    /**
     * Get progress to next level (0.0 to 1.0)
     */
    public double getProgress(Job job) {
        double required = job.getRequiredExp(level);
        return Math.min(1.0, experience / required);
    }

    // Getters and Setters
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
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

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public long getLastWorked() {
        return lastWorked;
    }

    public void setLastWorked(long lastWorked) {
        this.lastWorked = lastWorked;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    public void addPoints(double amount) {
        this.points += amount;
    }
}
