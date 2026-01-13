package network.vonix.vonixcore.jobs;

/**
 * Represents a job type.
 */
public record Job(
    String id,
    String displayName,
    String description,
    double basePayRate
) {
    public static final Job MINER = new Job("miner", "Miner", "Mine ores for money", 1.0);
    public static final Job FARMER = new Job("farmer", "Farmer", "Harvest crops for money", 0.8);
    public static final Job HUNTER = new Job("hunter", "Hunter", "Kill mobs for money", 1.5);
    public static final Job WOODCUTTER = new Job("woodcutter", "Woodcutter", "Chop trees for money", 0.7);
    public static final Job FISHERMAN = new Job("fisherman", "Fisherman", "Catch fish for money", 0.9);
}
