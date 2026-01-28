package network.vonix.vonixcore.economy;

import com.google.gson.*;
import network.vonix.vonixcore.VonixCore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles loading and saving economy plan configurations from JSON files.
 * Supports buy-only, sell-only, or both prices per item.
 */
public class EconomyPlanLoader {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /**
     * JSON structure for economy plan
     */
    public static class EconomyPlan {
        public int version = 1;
        public List<ItemPrice> items = new ArrayList<>();
    }

    public static class ItemPrice {
        public String id;
        public Double buy; // null = cannot buy from admin shop
        public Double sell; // null = cannot sell to admin shop

        public ItemPrice() {
        }

        public ItemPrice(String id, Double buy, Double sell) {
            this.id = id;
            this.buy = buy;
            this.sell = sell;
        }
    }

    /**
     * Load economy plan from a JSON file.
     * 
     * @param jsonPath Path to the JSON file
     * @return EconomyPlan or null if failed
     */
    public static EconomyPlan loadFromFile(Path jsonPath) {
        if (!Files.exists(jsonPath)) {
            VonixCore.LOGGER.warn("[Economy] File not found: {}", jsonPath);
            return null;
        }

        try (Reader reader = Files.newBufferedReader(jsonPath)) {
            EconomyPlan plan = GSON.fromJson(reader, EconomyPlan.class);
            if (plan == null || plan.items == null) {
                VonixCore.LOGGER.error("[Economy] Invalid JSON format in: {}", jsonPath);
                return null;
            }
            VonixCore.LOGGER.info("[Economy] Loaded {} items from {}", plan.items.size(), jsonPath.getFileName());
            return plan;
        } catch (JsonSyntaxException e) {
            VonixCore.LOGGER.error("[Economy] JSON syntax error in {}: {}", jsonPath, e.getMessage());
            return null;
        } catch (IOException e) {
            VonixCore.LOGGER.error("[Economy] Failed to read {}: {}", jsonPath, e.getMessage());
            return null;
        }
    }

    /**
     * Import an economy plan into the database.
     * 
     * @param plan The economy plan to import
     * @return Future completing with number of items imported successfully
     */
    public static CompletableFuture<Integer> importToDatabase(EconomyPlan plan) {
        if (plan == null || plan.items == null || plan.items.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        ShopManager shopManager = ShopManager.getInstance();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (ItemPrice item : plan.items) {
            if (item.id == null || item.id.isEmpty()) {
                VonixCore.LOGGER.warn("[Economy] Skipping item with no ID");
                continue;
            }

            // Normalize item ID (add minecraft: if no namespace)
            String itemId = item.id;
            if (!itemId.contains(":")) {
                itemId = "minecraft:" + itemId;
            }

            futures.add(shopManager.setAdminPrice(itemId, item.buy, item.sell));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int imported = 0;
                    for (var f : futures) {
                        if (f.join())
                            imported++;
                    }
                    VonixCore.LOGGER.info("[Economy] Imported {} of {} items to admin shop", imported,
                            plan.items.size());
                    return imported;
                });
    }

    /**
     * Export current admin shop prices to a JSON file.
     * 
     * @param jsonPath Path to save the JSON file
     * @return Future completing with true if successful
     */
    public static CompletableFuture<Boolean> exportToFile(Path jsonPath) {
        return ShopManager.getInstance().getAllAdminItems().thenApply(items -> {
            try {
                EconomyPlan plan = new EconomyPlan();
                plan.version = 1;
                for (ShopManager.AdminShopItem item : items) {
                    plan.items.add(new ItemPrice(item.itemId(), item.buyPrice(), item.sellPrice()));
                }

                // Create parent directories if needed
                Files.createDirectories(jsonPath.getParent());

                try (Writer writer = Files.newBufferedWriter(jsonPath)) {
                    GSON.toJson(plan, writer);
                }

                VonixCore.LOGGER.info("[Economy] Exported {} items to {}", plan.items.size(), jsonPath.getFileName());
                return true;
            } catch (IOException e) {
                VonixCore.LOGGER.error("[Economy] Failed to export to {}: {}", jsonPath, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get the default economy plan file path.
     */
    public static Path getDefaultPath() {
        return VonixCore.getInstance().getConfigPath().resolve("economy_plan.json");
    }

    /**
     * Create a sample economy plan file if it doesn't exist.
     */
    public static void createSampleFile(Path jsonPath) {
        if (Files.exists(jsonPath)) {
            return;
        }

        EconomyPlan sample = new EconomyPlan();
        sample.items.add(new ItemPrice("minecraft:diamond", 40.0, 8.0));
        sample.items.add(new ItemPrice("minecraft:iron_ingot", 6.0, 1.0));
        sample.items.add(new ItemPrice("minecraft:gold_ingot", 8.0, 1.5));
        sample.items.add(new ItemPrice("minecraft:emerald", 35.0, 7.0));
        sample.items.add(new ItemPrice("minecraft:netherite_ingot", 200.0, 40.0));
        sample.items.add(new ItemPrice("minecraft:diamond_pickaxe", 100.0, null)); // Buy only
        sample.items.add(new ItemPrice("minecraft:rotten_flesh", null, 0.05)); // Sell only

        try {
            Files.createDirectories(jsonPath.getParent());
            try (Writer writer = Files.newBufferedWriter(jsonPath)) {
                GSON.toJson(sample, writer);
            }
            VonixCore.LOGGER.info("[Economy] Created sample economy plan: {}", jsonPath.getFileName());
        } catch (IOException e) {
            VonixCore.LOGGER.warn("[Economy] Failed to create sample file: {}", e.getMessage());
        }
    }
}
