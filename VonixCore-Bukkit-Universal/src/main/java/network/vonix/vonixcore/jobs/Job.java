package network.vonix.vonixcore.jobs;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a job definition with actions and rewards.
 */
public class Job {

    private String id;
    private String name;
    private String description;
    private String chatColor;
    private Material icon;
    private int maxLevel;
    private double baseExpMultiplier;
    private double baseIncomeMultiplier;

    // Action type -> (Material/Entity -> Reward)
    private Map<ActionType, Map<String, JobReward>> actions = new HashMap<>();

    public Job(String id) {
        this.id = id;
        this.name = id;
        this.maxLevel = 100;
        this.baseExpMultiplier = 1.0;
        this.baseIncomeMultiplier = 1.0;
        this.chatColor = "WHITE";
        this.icon = Material.DIAMOND_PICKAXE;
    }

    /**
     * Get the required experience for a level
     */
    public double getRequiredExp(int level) {
        // Formula: 100 * level + level^2 * 10
        return 100 * level + (level * level * 10);
    }

    /**
     * Get reward for an action
     */
    public JobReward getReward(ActionType actionType, String target) {
        Map<String, JobReward> actionMap = actions.get(actionType);
        if (actionMap == null)
            return null;

        // Try exact match first
        JobReward reward = actionMap.get(target.toLowerCase());
        if (reward != null)
            return reward;

        // Try wildcard
        return actionMap.get("*");
    }

    /**
     * Add a reward for an action
     */
    public void addReward(ActionType actionType, String target, double income, double exp) {
        actions.computeIfAbsent(actionType, k -> new HashMap<>())
                .put(target.toLowerCase(), new JobReward(income, exp));
    }

    /**
     * Get income multiplier for a level
     */
    public double getIncomeMultiplier(int level) {
        return baseIncomeMultiplier + (level - 1) * 0.02; // 2% per level
    }

    /**
     * Get experience multiplier for a level
     */
    public double getExpMultiplier(int level) {
        return baseExpMultiplier + (level - 1) * 0.01; // 1% per level
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChatColor() {
        return chatColor;
    }

    public void setChatColor(String chatColor) {
        this.chatColor = chatColor;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public double getBaseExpMultiplier() {
        return baseExpMultiplier;
    }

    public void setBaseExpMultiplier(double baseExpMultiplier) {
        this.baseExpMultiplier = baseExpMultiplier;
    }

    public double getBaseIncomeMultiplier() {
        return baseIncomeMultiplier;
    }

    public void setBaseIncomeMultiplier(double baseIncomeMultiplier) {
        this.baseIncomeMultiplier = baseIncomeMultiplier;
    }

    public Map<ActionType, Map<String, JobReward>> getActions() {
        return actions;
    }

    /**
     * Action types that can earn job rewards
     */
    public enum ActionType {
        BREAK, // Breaking blocks
        PLACE, // Placing blocks
        KILL, // Killing entities
        FISH, // Fishing
        CRAFT, // Crafting items
        SMELT, // Smelting in furnace
        BREW, // Brewing potions
        ENCHANT, // Enchanting items
        REPAIR, // Repairing with anvil
        BREED, // Breeding animals
        TAME, // Taming animals
        MILK, // Milking cows
        SHEAR, // Shearing sheep
        EXPLORE, // Exploring new chunks
        TRADE // Trading with villagers
    }

    /**
     * Reward data for an action
     */
    public static class JobReward {
        public final double income;
        public final double experience;

        public JobReward(double income, double experience) {
            this.income = income;
            this.experience = experience;
        }
    }
}
