package network.vonix.vonixcore.jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.economy.EconomyManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages jobs, player job data, and job rewards for Forge 1.20.1.
 */
public class JobsManager {

    private static JobsManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // All available jobs
    private final Map<String, Job> jobs = new LinkedHashMap<>();

    // Player job data (UUID -> list of their jobs)
    private final Map<UUID, List<PlayerJob>> playerJobs = new ConcurrentHashMap<>();

    // Configuration
    private int maxJobs = 3;
    private boolean enabled = true;
    private double globalIncomeMultiplier = 1.0;
    private double globalExpMultiplier = 1.0;

    public JobsManager() {
        instance = this;
    }

    public static JobsManager getInstance() {
        if (instance == null) {
            instance = new JobsManager();
        }
        return instance;
    }

    public void initialize(Connection conn) throws SQLException {
        createTables(conn);
        loadJobsConfig();
        loadPlayerData();

        // Register event handler
        MinecraftForge.EVENT_BUS.register(this);

        VonixCore.LOGGER.info("[Jobs] Jobs system initialized with {} jobs", jobs.size());
    }

    private void createTables(Connection conn) throws SQLException {
        conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS vonixcore_player_jobs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    job_id TEXT NOT NULL,
                    level INTEGER DEFAULT 1,
                    experience REAL DEFAULT 0,
                    points REAL DEFAULT 0,
                    joined_at INTEGER,
                    last_worked INTEGER,
                    UNIQUE(uuid, job_id)
                )
                """);

        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_pj_uuid ON vonixcore_player_jobs(uuid)");

        // Add points column if not exists (for upgrades)
        try {
            conn.createStatement().execute("ALTER TABLE vonixcore_player_jobs ADD COLUMN points REAL DEFAULT 0");
        } catch (SQLException ignored) {
            // Column already exists
        }
    }

    /**
     * Load jobs from configuration
     */
    private void loadJobsConfig() {
        Path configPath = VonixCore.getInstance().getConfigPath().resolve("jobs.json");

        if (!Files.exists(configPath)) {
            createDefaultJobsConfig(configPath);
        }

        try {
            String json = Files.readString(configPath);
            Map<String, Object> config = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
            }.getType());

            enabled = (Boolean) config.getOrDefault("enabled", true);
            maxJobs = ((Number) config.getOrDefault("maxJobs", 3)).intValue();
            globalIncomeMultiplier = ((Number) config.getOrDefault("globalIncomeMultiplier", 1.0)).doubleValue();
            globalExpMultiplier = ((Number) config.getOrDefault("globalExpMultiplier", 1.0)).doubleValue();

            // Load built-in jobs
            createBuiltInJobs();

        } catch (Exception e) {
            VonixCore.LOGGER.warn("Failed to load jobs config: {}", e.getMessage());
            createBuiltInJobs();
        }
    }

    /**
     * Create built-in default jobs
     */
    private void createBuiltInJobs() {
        // Miner - Mining ores and stone
        Job miner = new Job("miner");
        miner.setName("§7Miner");
        miner.setDescription("Earn money by mining ores and stone");
        miner.setIcon(Items.DIAMOND_PICKAXE);
        miner.addReward(Job.ActionType.BREAK, "stone", 0.5, 1.0, 0.5);
        miner.addReward(Job.ActionType.BREAK, "deepslate", 0.75, 1.25, 0.75);
        miner.addReward(Job.ActionType.BREAK, "coal_ore", 2.0, 3.0, 2.0);
        miner.addReward(Job.ActionType.BREAK, "deepslate_coal_ore", 2.5, 4.0, 2.5);
        miner.addReward(Job.ActionType.BREAK, "iron_ore", 3.0, 5.0, 3.0);
        miner.addReward(Job.ActionType.BREAK, "deepslate_iron_ore", 4.0, 6.0, 4.0);
        miner.addReward(Job.ActionType.BREAK, "copper_ore", 2.5, 4.0, 2.5);
        miner.addReward(Job.ActionType.BREAK, "gold_ore", 5.0, 8.0, 5.0);
        miner.addReward(Job.ActionType.BREAK, "deepslate_gold_ore", 6.0, 10.0, 6.0);
        miner.addReward(Job.ActionType.BREAK, "redstone_ore", 3.5, 5.0, 3.5);
        miner.addReward(Job.ActionType.BREAK, "lapis_ore", 6.0, 8.0, 6.0);
        miner.addReward(Job.ActionType.BREAK, "diamond_ore", 10.0, 15.0, 10.0);
        miner.addReward(Job.ActionType.BREAK, "deepslate_diamond_ore", 12.0, 18.0, 12.0);
        miner.addReward(Job.ActionType.BREAK, "emerald_ore", 12.0, 20.0, 12.0);
        miner.addReward(Job.ActionType.BREAK, "deepslate_emerald_ore", 15.0, 25.0, 15.0);
        miner.addReward(Job.ActionType.BREAK, "ancient_debris", 20.0, 30.0, 20.0);
        miner.addReward(Job.ActionType.BREAK, "nether_quartz_ore", 2.0, 3.0, 2.0);
        // Anti-exploit: Placing ores gives NEGATIVE rewards
        miner.addReward(Job.ActionType.PLACE, "diamond_ore", -10.0, 0, -10.0);
        miner.addReward(Job.ActionType.PLACE, "deepslate_diamond_ore", -12.0, 0, -12.0);
        miner.addReward(Job.ActionType.PLACE, "emerald_ore", -12.0, 0, -12.0);
        miner.addReward(Job.ActionType.PLACE, "gold_ore", -5.0, 0, -5.0);
        miner.addReward(Job.ActionType.PLACE, "iron_ore", -3.0, 0, -3.0);
        jobs.put("miner", miner);

        // Woodcutter
        Job woodcutter = new Job("woodcutter");
        woodcutter.setName("§2Woodcutter");
        woodcutter.setDescription("Earn money by chopping trees");
        woodcutter.setIcon(Items.DIAMOND_AXE);
        woodcutter.addReward(Job.ActionType.BREAK, "oak_log", 1.0, 2.0, 1.0);
        woodcutter.addReward(Job.ActionType.BREAK, "spruce_log", 1.0, 2.0, 1.0);
        woodcutter.addReward(Job.ActionType.BREAK, "birch_log", 1.0, 2.0, 1.0);
        woodcutter.addReward(Job.ActionType.BREAK, "jungle_log", 1.5, 2.5, 1.5);
        woodcutter.addReward(Job.ActionType.BREAK, "acacia_log", 1.5, 2.5, 1.5);
        woodcutter.addReward(Job.ActionType.BREAK, "dark_oak_log", 1.5, 2.5, 1.5);
        woodcutter.addReward(Job.ActionType.BREAK, "cherry_log", 2.0, 3.0, 2.0);
        woodcutter.addReward(Job.ActionType.BREAK, "mangrove_log", 2.0, 3.0, 2.0);
        woodcutter.addReward(Job.ActionType.BREAK, "crimson_stem", 2.5, 4.0, 2.5);
        woodcutter.addReward(Job.ActionType.BREAK, "warped_stem", 2.5, 4.0, 2.5);
        jobs.put("woodcutter", woodcutter);

        // Farmer
        Job farmer = new Job("farmer");
        farmer.setName("§aFarmer");
        farmer.setDescription("Earn money by farming crops and breeding animals");
        farmer.setIcon(Items.DIAMOND_HOE);
        farmer.addReward(Job.ActionType.BREAK, "wheat", 1.0, 1.5, 1.0);
        farmer.addReward(Job.ActionType.BREAK, "carrots", 1.0, 1.5, 1.0);
        farmer.addReward(Job.ActionType.BREAK, "potatoes", 1.0, 1.5, 1.0);
        farmer.addReward(Job.ActionType.BREAK, "beetroots", 1.5, 2.0, 1.5);
        farmer.addReward(Job.ActionType.BREAK, "nether_wart", 2.0, 3.0, 2.0);
        farmer.addReward(Job.ActionType.BREAK, "melon", 1.5, 2.0, 1.5);
        farmer.addReward(Job.ActionType.BREAK, "pumpkin", 1.5, 2.0, 1.5);
        farmer.addReward(Job.ActionType.BREAK, "sugar_cane", 0.75, 1.0, 0.75);
        farmer.addReward(Job.ActionType.BREAK, "cocoa", 2.0, 3.0, 2.0);
        farmer.addReward(Job.ActionType.BREED, "cow", 3.0, 5.0, 3.0);
        farmer.addReward(Job.ActionType.BREED, "pig", 3.0, 5.0, 3.0);
        farmer.addReward(Job.ActionType.BREED, "sheep", 3.0, 5.0, 3.0);
        farmer.addReward(Job.ActionType.BREED, "chicken", 2.0, 3.0, 2.0);
        farmer.addReward(Job.ActionType.BREED, "rabbit", 2.5, 4.0, 2.5);
        jobs.put("farmer", farmer);

        // Hunter
        Job hunter = new Job("hunter");
        hunter.setName("§cHunter");
        hunter.setDescription("Earn money by killing mobs");
        hunter.setIcon(Items.DIAMOND_SWORD);
        hunter.addReward(Job.ActionType.KILL, "zombie", 2.0, 3.0, 2.0);
        hunter.addReward(Job.ActionType.KILL, "skeleton", 2.5, 4.0, 2.5);
        hunter.addReward(Job.ActionType.KILL, "spider", 2.0, 3.0, 2.0);
        hunter.addReward(Job.ActionType.KILL, "creeper", 3.0, 5.0, 3.0);
        hunter.addReward(Job.ActionType.KILL, "enderman", 5.0, 8.0, 5.0);
        hunter.addReward(Job.ActionType.KILL, "blaze", 6.0, 10.0, 6.0);
        hunter.addReward(Job.ActionType.KILL, "wither_skeleton", 8.0, 12.0, 8.0);
        hunter.addReward(Job.ActionType.KILL, "phantom", 4.0, 6.0, 4.0);
        hunter.addReward(Job.ActionType.KILL, "witch", 5.0, 8.0, 5.0);
        hunter.addReward(Job.ActionType.KILL, "pillager", 4.0, 7.0, 4.0);
        hunter.addReward(Job.ActionType.KILL, "ravager", 15.0, 25.0, 15.0);
        hunter.addReward(Job.ActionType.KILL, "warden", 50.0, 100.0, 50.0);
        jobs.put("hunter", hunter);

        // Builder
        Job builder = new Job("builder");
        builder.setName("§6Builder");
        builder.setDescription("Earn money by placing blocks");
        builder.setIcon(Items.BRICKS);
        builder.addReward(Job.ActionType.PLACE, "*", 0.25, 0.5, 0.25);
        builder.addReward(Job.ActionType.PLACE, "bricks", 1.0, 1.5, 1.0);
        builder.addReward(Job.ActionType.PLACE, "stone_bricks", 0.75, 1.0, 0.75);
        builder.addReward(Job.ActionType.PLACE, "deepslate_bricks", 1.0, 1.5, 1.0);
        builder.addReward(Job.ActionType.PLACE, "polished_blackstone_bricks", 1.0, 1.5, 1.0);
        builder.addReward(Job.ActionType.PLACE, "quartz_block", 1.5, 2.0, 1.5);
        jobs.put("builder", builder);

        // Fisherman
        Job fisherman = new Job("fisherman");
        fisherman.setName("§3Fisherman");
        fisherman.setDescription("Earn money by fishing");
        fisherman.setIcon(Items.FISHING_ROD);
        fisherman.addReward(Job.ActionType.FISH, "cod", 2.0, 3.0, 2.0);
        fisherman.addReward(Job.ActionType.FISH, "salmon", 2.5, 4.0, 2.5);
        fisherman.addReward(Job.ActionType.FISH, "tropical_fish", 5.0, 8.0, 5.0);
        fisherman.addReward(Job.ActionType.FISH, "pufferfish", 4.0, 6.0, 4.0);
        fisherman.addReward(Job.ActionType.FISH, "bow", 10.0, 15.0, 10.0);
        fisherman.addReward(Job.ActionType.FISH, "enchanted_book", 20.0, 30.0, 20.0);
        fisherman.addReward(Job.ActionType.FISH, "name_tag", 15.0, 20.0, 15.0);
        fisherman.addReward(Job.ActionType.FISH, "saddle", 15.0, 20.0, 15.0);
        fisherman.addReward(Job.ActionType.FISH, "nautilus_shell", 25.0, 40.0, 25.0);
        jobs.put("fisherman", fisherman);

        // Digger
        Job digger = new Job("digger");
        digger.setName("§eDigger");
        digger.setDescription("Earn money by digging soil and sand");
        digger.setIcon(Items.IRON_SHOVEL);
        digger.addReward(Job.ActionType.BREAK, "dirt", 0.25, 0.5, 0.25);
        digger.addReward(Job.ActionType.BREAK, "grass_block", 0.5, 1.0, 0.5);
        digger.addReward(Job.ActionType.BREAK, "sand", 0.5, 1.0, 0.5);
        digger.addReward(Job.ActionType.BREAK, "red_sand", 0.75, 1.0, 0.75);
        digger.addReward(Job.ActionType.BREAK, "gravel", 0.5, 1.0, 0.5);
        digger.addReward(Job.ActionType.BREAK, "clay", 1.5, 2.0, 1.5);
        digger.addReward(Job.ActionType.BREAK, "soul_sand", 1.0, 1.5, 1.0);
        digger.addReward(Job.ActionType.BREAK, "soul_soil", 1.0, 1.5, 1.0);
        digger.addReward(Job.ActionType.BREAK, "mycelium", 2.0, 3.0, 2.0);
        jobs.put("digger", digger);

        // Crafter
        Job crafter = new Job("crafter");
        crafter.setName("§dCrafter");
        crafter.setDescription("Earn money by crafting items");
        crafter.setIcon(Items.CRAFTING_TABLE);
        crafter.addReward(Job.ActionType.CRAFT, "*", 0.1, 0.25, 0.1);
        crafter.addReward(Job.ActionType.CRAFT, "iron_sword", 2.0, 3.0, 2.0);
        crafter.addReward(Job.ActionType.CRAFT, "iron_pickaxe", 3.0, 4.0, 3.0);
        crafter.addReward(Job.ActionType.CRAFT, "diamond_sword", 5.0, 8.0, 5.0);
        crafter.addReward(Job.ActionType.CRAFT, "diamond_pickaxe", 8.0, 12.0, 8.0);
        crafter.addReward(Job.ActionType.CRAFT, "iron_block", 2.0, 3.0, 2.0);
        crafter.addReward(Job.ActionType.CRAFT, "gold_block", 3.0, 5.0, 3.0);
        crafter.addReward(Job.ActionType.CRAFT, "diamond_block", 10.0, 15.0, 10.0);
        crafter.addReward(Job.ActionType.SMELT, "iron_ingot", 1.0, 1.5, 1.0);
        crafter.addReward(Job.ActionType.SMELT, "gold_ingot", 1.5, 2.0, 1.5);
        crafter.addReward(Job.ActionType.SMELT, "copper_ingot", 1.0, 1.5, 1.0);
        crafter.addReward(Job.ActionType.SMELT, "netherite_scrap", 5.0, 8.0, 5.0);
        crafter.addReward(Job.ActionType.SMELT, "glass", 0.5, 1.0, 0.5);
        jobs.put("crafter", crafter);
    }

    private void createDefaultJobsConfig(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("enabled", true);
            config.put("maxJobs", 3);
            config.put("globalIncomeMultiplier", 1.0);
            config.put("globalExpMultiplier", 1.0);
            Files.writeString(path, GSON.toJson(config));
        } catch (IOException e) {
            VonixCore.LOGGER.warn("Failed to create jobs config: {}", e.getMessage());
        }
    }

    private void loadPlayerData() {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT * FROM vonixcore_player_jobs");

                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerJob pj = new PlayerJob();
                    pj.setPlayerUuid(uuid);
                    pj.setJobId(rs.getString("job_id"));
                    pj.setLevel(rs.getInt("level"));
                    pj.setExperience(rs.getDouble("experience"));
                    pj.setPoints(rs.getDouble("points"));
                    pj.setJoinedAt(rs.getLong("joined_at"));
                    pj.setLastWorked(rs.getLong("last_worked"));

                    playerJobs.computeIfAbsent(uuid, k -> new ArrayList<>()).add(pj);
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.warn("Failed to load player jobs: {}", e.getMessage());
            }
        });
    }

    public boolean joinJob(ServerPlayer player, String jobId) {
        if (!enabled)
            return false;

        Job job = jobs.get(jobId.toLowerCase());
        if (job == null) {
            player.sendSystemMessage(Component.literal("Unknown job: " + jobId).withStyle(ChatFormatting.RED));
            return false;
        }

        List<PlayerJob> pJobs = playerJobs.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        if (pJobs.stream().anyMatch(pj -> pj.getJobId().equalsIgnoreCase(jobId))) {
            player.sendSystemMessage(Component.literal("You already have this job!").withStyle(ChatFormatting.RED));
            return false;
        }
        if (pJobs.size() >= maxJobs) {
            player.sendSystemMessage(
                    Component.literal("You can only have " + maxJobs + " jobs!").withStyle(ChatFormatting.RED));
            return false;
        }

        PlayerJob newJob = new PlayerJob(player.getUUID(), jobId.toLowerCase());
        pJobs.add(newJob);
        savePlayerJob(newJob);
        player.sendSystemMessage(
                Component.literal("You joined the " + job.getName() + " §rjob!").withStyle(ChatFormatting.GREEN));
        return true;
    }

    public boolean leaveJob(ServerPlayer player, String jobId) {
        List<PlayerJob> pJobs = playerJobs.get(player.getUUID());
        if (pJobs == null) {
            player.sendSystemMessage(Component.literal("You don't have any jobs!").withStyle(ChatFormatting.RED));
            return false;
        }

        PlayerJob toRemove = pJobs.stream().filter(pj -> pj.getJobId().equalsIgnoreCase(jobId)).findFirst()
                .orElse(null);
        if (toRemove == null) {
            player.sendSystemMessage(Component.literal("You don't have that job!").withStyle(ChatFormatting.RED));
            return false;
        }

        pJobs.remove(toRemove);
        deletePlayerJob(player.getUUID(), jobId.toLowerCase());
        Job job = jobs.get(jobId.toLowerCase());
        String jobName = job != null ? job.getName() : jobId;
        player.sendSystemMessage(
                Component.literal("You left the " + jobName + " §rjob!").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public void processAction(ServerPlayer player, Job.ActionType actionType, String target) {
        if (!enabled)
            return;
        List<PlayerJob> pJobs = playerJobs.get(player.getUUID());
        if (pJobs == null || pJobs.isEmpty())
            return;

        EconomyManager eco = EconomyManager.getInstance();

        for (PlayerJob pJob : pJobs) {
            Job job = jobs.get(pJob.getJobId());
            if (job == null)
                continue;

            Job.JobReward reward = job.getReward(actionType, target);
            if (reward == null)
                continue;

            double incomeMultiplier = job.getIncomeMultiplier(pJob.getLevel()) * globalIncomeMultiplier;
            double expMultiplier = job.getExpMultiplier(pJob.getLevel()) * globalExpMultiplier;

            double income = reward.income * incomeMultiplier;
            double exp = reward.experience * expMultiplier;
            double pts = reward.points * incomeMultiplier;

            if (income != 0) {
                if (income > 0) {
                    eco.deposit(player.getUUID(), income);
                } else {
                    eco.withdraw(player.getUUID(), -income);
                }
            }
            if (pts != 0) {
                pJob.addPoints(pts);
            }
            if (exp > 0) {
                boolean leveledUp = pJob.addExperience(exp, job);
                if (leveledUp) {
                    player.sendSystemMessage(Component.literal("Level up! " + job.getName() +
                            " §ris now level " + pJob.getLevel()).withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    // Event handlers
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!enabled)
            return;
        if (!(event.getPlayer() instanceof ServerPlayer player))
            return;
        String blockName = event.getState().getBlock().getDescriptionId().replace("block.minecraft.", "");
        processAction(player, Job.ActionType.BREAK, blockName);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!enabled)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        String blockName = event.getPlacedBlock().getBlock().getDescriptionId().replace("block.minecraft.", "");
        processAction(player, Job.ActionType.PLACE, blockName);
    }

    @SubscribeEvent
    public void onEntityKill(LivingDeathEvent event) {
        if (!enabled)
            return;
        if (event.getSource().getEntity() == null)
            return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;
        String entityName = event.getEntity().getType().getDescriptionId().replace("entity.minecraft.", "");
        processAction(player, Job.ActionType.KILL, entityName);
    }

    @SubscribeEvent
    public void onItemFished(net.minecraftforge.event.entity.player.ItemFishedEvent event) {
        if (!enabled)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        for (net.minecraft.world.item.ItemStack drop : event.getDrops()) {
            String itemName = drop.getItem().getDescriptionId().replace("item.minecraft.", "")
                    .replace("block.minecraft.", "");
            processAction(player, Job.ActionType.FISH, itemName);
        }
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!enabled)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        String itemName = event.getCrafting().getItem().getDescriptionId().replace("item.minecraft.", "")
                .replace("block.minecraft.", "");
        processAction(player, Job.ActionType.CRAFT, itemName);
    }

    @SubscribeEvent
    public void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!enabled)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        String itemName = event.getSmelting().getItem().getDescriptionId().replace("item.minecraft.", "")
                .replace("block.minecraft.", "");
        processAction(player, Job.ActionType.SMELT, itemName);
    }

    @SubscribeEvent
    public void onAnimalTame(net.minecraftforge.event.entity.living.AnimalTameEvent event) {
        if (!enabled)
            return;
        if (!(event.getTamer() instanceof ServerPlayer player))
            return;
        String entityName = event.getAnimal().getType().getDescriptionId().replace("entity.minecraft.", "");
        processAction(player, Job.ActionType.TAME, entityName);
    }

    @SubscribeEvent
    public void onBabySpawn(net.minecraftforge.event.entity.living.BabyEntitySpawnEvent event) {
        if (!enabled)
            return;
        if (event.getCausedByPlayer() == null)
            return;
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player))
            return;
        String entityName = event.getChild().getType().getDescriptionId().replace("entity.minecraft.", "");
        processAction(player, Job.ActionType.BREED, entityName);
    }

    public List<PlayerJob> getPlayerJobs(UUID uuid) {
        return playerJobs.getOrDefault(uuid, new ArrayList<>());
    }

    public Collection<Job> getAllJobs() {
        return jobs.values();
    }

    public Job getJob(String jobId) {
        return jobs.get(jobId.toLowerCase());
    }

    private void savePlayerJob(PlayerJob pj) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("""
                        INSERT OR REPLACE INTO vonixcore_player_jobs
                        (uuid, job_id, level, experience, points, joined_at, last_worked)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """);
                stmt.setString(1, pj.getPlayerUuid().toString());
                stmt.setString(2, pj.getJobId());
                stmt.setInt(3, pj.getLevel());
                stmt.setDouble(4, pj.getExperience());
                stmt.setDouble(5, pj.getPoints());
                stmt.setLong(6, pj.getJoinedAt());
                stmt.setLong(7, pj.getLastWorked());
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.warn("Failed to save player job: {}", e.getMessage());
            }
        });
    }

    private void deletePlayerJob(UUID uuid, String jobId) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM vonixcore_player_jobs WHERE uuid = ? AND job_id = ?");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                VonixCore.LOGGER.warn("Failed to delete player job: {}", e.getMessage());
            }
        });
    }

    public void saveAllJobs() {
        for (List<PlayerJob> pJobs : playerJobs.values()) {
            for (PlayerJob pj : pJobs) {
                savePlayerJob(pj);
            }
        }
    }

    public void shutdown() {
        saveAllJobs();
    }

    public void reload() {
        loadJobsConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxJobs() {
        return maxJobs;
    }
}
