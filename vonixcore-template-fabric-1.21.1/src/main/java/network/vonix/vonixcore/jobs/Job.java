package network.vonix.vonixcore.jobs;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a job with configurable rewards and actions.
 */
public class Job {

    private final String id;
    private String name;
    private String description;
    private String chatColor;
    private String icon; // Material name
    private int maxLevel;
    
    // ActionType -> Target (block/mob) -> Reward
    private final Map<ActionType, Map<String, JobReward>> rewards = new HashMap<>();

    public Job(String id) {
        this.id = id;
        this.name = id;
        this.description = "";
        this.chatColor = "WHITE";
        this.icon = "DIRT";
        this.maxLevel = 100;
    }

    public void addReward(ActionType type, String target, double income, double exp) {
        rewards.computeIfAbsent(type, k -> new HashMap<>())
               .put(target.toLowerCase(), new JobReward(income, exp));
    }

    public JobReward getReward(ActionType type, String target) {
        Map<String, JobReward> typeRewards = rewards.get(type);
        if (typeRewards == null) return null;
        
        // Check specific target first
        JobReward reward = typeRewards.get(target.toLowerCase());
        if (reward != null) return reward;
        
        // Check wildcard
        return typeRewards.get("*");
    }

    public double getRequiredExp(int level) {
        // Simple exponential curve: 100 * (1.5 ^ (level - 1))
        return 100 * Math.pow(1.5, level - 1);
    }

    public double getIncomeMultiplier(int level) {
        // 1% increase per level
        return 1.0 + ((level - 1) * 0.01);
    }

    public double getExpMultiplier(int level) {
        // 1% increase per level
        return 1.0 + ((level - 1) * 0.01);
    }

    // Enums and Inner Classes
    public enum ActionType {
        BREAK,
        PLACE,
        KILL,
        FISH,
        BREED,
        CRAFT,
        EAT
    }

    public record JobReward(double income, double experience) {}

    // Getters and Setters
    public String getId() { return id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getChatColor() { return chatColor; }
    public void setChatColor(String chatColor) { this.chatColor = chatColor; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }
}
