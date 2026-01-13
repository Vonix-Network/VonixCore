package network.vonix.vonixcore.economy;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ShopsConfig;

import java.util.*;

/**
 * Loads economy plan data (balanced shop prices, etc.)
 */
public class EconomyPlanLoader {

    private static EconomyPlanLoader instance;
    private final Map<String, Double> buyPrices = new HashMap<>();
    private final Map<String, Double> sellPrices = new HashMap<>();

    public static EconomyPlanLoader getInstance() {
        if (instance == null) {
            instance = new EconomyPlanLoader();
        }
        return instance;
    }

    /**
     * Load default prices.
     */
    public void loadDefaultPrices() {
        // Basic items
        buyPrices.put("minecraft:cobblestone", 0.10);
        sellPrices.put("minecraft:cobblestone", 0.05);
        
        buyPrices.put("minecraft:stone", 0.20);
        sellPrices.put("minecraft:stone", 0.10);
        
        buyPrices.put("minecraft:iron_ingot", 10.0);
        sellPrices.put("minecraft:iron_ingot", 5.0);
        
        buyPrices.put("minecraft:gold_ingot", 25.0);
        sellPrices.put("minecraft:gold_ingot", 12.5);
        
        buyPrices.put("minecraft:diamond", 100.0);
        sellPrices.put("minecraft:diamond", 50.0);
        
        buyPrices.put("minecraft:wheat", 1.0);
        sellPrices.put("minecraft:wheat", 0.5);
        
        buyPrices.put("minecraft:bread", 5.0);
        sellPrices.put("minecraft:bread", 2.5);
        
        VonixCore.LOGGER.info("[Economy] Loaded {} item prices", buyPrices.size());
    }

    public double getBuyPrice(String itemId) {
        return buyPrices.getOrDefault(itemId, -1.0);
    }

    public double getSellPrice(String itemId) {
        return sellPrices.getOrDefault(itemId, -1.0);
    }

    public Set<String> getItemsWithPrices() {
        Set<String> items = new HashSet<>();
        items.addAll(buyPrices.keySet());
        items.addAll(sellPrices.keySet());
        return items;
    }
}
