package network.vonix.vonixcore.economy;

import com.google.gson.*;
import network.vonix.vonixcore.VonixCore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
            VonixCore.getInstance().getLogger().warning("[Economy] File not found: " + jsonPath);
            return null;
        }

        try (Reader reader = Files.newBufferedReader(jsonPath)) {
            EconomyPlan plan = GSON.fromJson(reader, EconomyPlan.class);
            if (plan == null || plan.items == null) {
                VonixCore.getInstance().getLogger().severe("[Economy] Invalid JSON format in: " + jsonPath);
                return null;
            }
            VonixCore.getInstance().getLogger()
                    .info("[Economy] Loaded " + plan.items.size() + " items from " + jsonPath.getFileName());
            return plan;
        } catch (JsonSyntaxException e) {
            VonixCore.getInstance().getLogger()
                    .severe("[Economy] JSON syntax error in " + jsonPath + ": " + e.getMessage());
            return null;
        } catch (IOException e) {
            VonixCore.getInstance().getLogger().severe("[Economy] Failed to read " + jsonPath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Import an economy plan into the database.
     * 
     * @param plan The economy plan to import
     * @return Number of items imported successfully
     */
    public static int importToDatabase(EconomyPlan plan) {
        if (plan == null || plan.items == null || plan.items.isEmpty()) {
            return 0;
        }

        int imported = 0;

        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            for (ItemPrice item : plan.items) {
                if (item.id == null || item.id.isEmpty()) {
                    continue;
                }

                // Normalize item ID (add minecraft: if no namespace)
                String itemId = item.id;
                if (!itemId.contains(":")) {
                    itemId = "minecraft:" + itemId;
                }

                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT OR REPLACE INTO vc_admin_shop (item_id, buy_price, sell_price) VALUES (?, ?, ?)");
                stmt.setString(1, itemId);
                stmt.setObject(2, item.buy);
                stmt.setObject(3, item.sell);
                stmt.executeUpdate();
                imported++;
            }
        } catch (SQLException e) {
            VonixCore.getInstance().getLogger().severe("[Economy] Failed to import prices: " + e.getMessage());
        }

        VonixCore.getInstance().getLogger()
                .info("[Economy] Imported " + imported + " of " + plan.items.size() + " items to admin shop");
        return imported;
    }

    /**
     * Export current admin shop prices to a JSON file.
     * 
     * @param jsonPath Path to save the JSON file
     * @return true if successful
     */
    public static boolean exportToFile(Path jsonPath) {
        try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
            EconomyPlan plan = new EconomyPlan();
            plan.version = 1;

            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT item_id, buy_price, sell_price FROM vc_admin_shop ORDER BY item_id");
            while (rs.next()) {
                plan.items.add(new ItemPrice(
                        rs.getString("item_id"),
                        rs.getObject("buy_price") != null ? rs.getDouble("buy_price") : null,
                        rs.getObject("sell_price") != null ? rs.getDouble("sell_price") : null));
            }

            // Create parent directories if needed
            Files.createDirectories(jsonPath.getParent());

            try (Writer writer = Files.newBufferedWriter(jsonPath)) {
                GSON.toJson(plan, writer);
            }

            VonixCore.getInstance().getLogger()
                    .info("[Economy] Exported " + plan.items.size() + " items to " + jsonPath.getFileName());
            return true;
        } catch (Exception e) {
            VonixCore.getInstance().getLogger()
                    .severe("[Economy] Failed to export to " + jsonPath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the default economy plan file path.
     */
    public static Path getDefaultPath() {
        return VonixCore.getInstance().getDataFolder().toPath().resolve("economy_plan.json");
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
            VonixCore.getInstance().getLogger()
                    .info("[Economy] Created sample economy plan: " + jsonPath.getFileName());
        } catch (IOException e) {
            VonixCore.getInstance().getLogger().warning("[Economy] Failed to create sample file: " + e.getMessage());
        }
    }
}
