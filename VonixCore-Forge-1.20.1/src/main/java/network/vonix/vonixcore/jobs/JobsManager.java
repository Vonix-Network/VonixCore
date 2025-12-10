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
 * Manages jobs, player job data, and job rewards for NeoForge.
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
                    joined_at INTEGER,
                    last_worked INTEGER,
                    UNIQUE(uuid, job_id)
                )
                """);

        conn.createStatement().execute(
                "CREATE INDEX IF NOT EXISTS idx_pj_uuid ON vonixcore_player_jobs(uuid)");
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
        // Miner
        Job miner = new Job("miner");
        miner.setName("§7Miner");
        miner.setDescription("Earn money by mining ores and stone");
        miner.setIcon(Items.DIAMOND_PICKAXE);
        miner.addReward(Job.ActionType.BREAK, "stone", 0.5, 1.0);
        miner.addReward(Job.ActionType.BREAK, "coal_ore", 2.0, 3.0);
        miner.addReward(Job.ActionType.BREAK, "iron_ore", 3.0, 5.0);
        miner.addReward(Job.ActionType.BREAK, "gold_ore", 5.0, 8.0);
        miner.addReward(Job.ActionType.BREAK, "diamond_ore", 10.0, 15.0);
        miner.addReward(Job.ActionType.BREAK, "emerald_ore", 12.0, 20.0);
        miner.addReward(Job.ActionType.BREAK, "deepslate_diamond_ore", 12.0, 18.0);
        jobs.put("miner", miner);

        // Woodcutter
        Job woodcutter = new Job("woodcutter");
        woodcutter.setName("§2Woodcutter");
        woodcutter.setDescription("Earn money by chopping trees");
        woodcutter.setIcon(Items.DIAMOND_AXE);
        woodcutter.addReward(Job.ActionType.BREAK, "oak_log", 1.0, 2.0);
        woodcutter.addReward(Job.ActionType.BREAK, "spruce_log", 1.0, 2.0);
        woodcutter.addReward(Job.ActionType.BREAK, "birch_log", 1.0, 2.0);
        woodcutter.addReward(Job.ActionType.BREAK, "jungle_log", 1.5, 2.5);
        woodcutter.addReward(Job.ActionType.BREAK, "acacia_log", 1.5, 2.5);
        woodcutter.addReward(Job.ActionType.BREAK, "dark_oak_log", 1.5, 2.5);
        woodcutter.addReward(Job.ActionType.BREAK, "cherry_log", 2.0, 3.0);
        jobs.put("woodcutter", woodcutter);

        // Farmer
        Job farmer = new Job("farmer");
        farmer.setName("§aFarmer");
        farmer.setDescription("Earn money by farming crops");
        farmer.setIcon(Items.DIAMOND_HOE);
        farmer.addReward(Job.ActionType.BREAK, "wheat", 1.0, 1.5);
        farmer.addReward(Job.ActionType.BREAK, "carrots", 1.0, 1.5);
        farmer.addReward(Job.ActionType.BREAK, "potatoes", 1.0, 1.5);
        farmer.addReward(Job.ActionType.BREAK, "beetroots", 1.5, 2.0);
        farmer.addReward(Job.ActionType.BREAK, "nether_wart", 2.0, 3.0);
        jobs.put("farmer", farmer);

        // Hunter
        Job hunter = new Job("hunter");
        hunter.setName("§cHunter");
        hunter.setDescription("Earn money by killing mobs");
        hunter.setIcon(Items.DIAMOND_SWORD);
        hunter.addReward(Job.ActionType.KILL, "zombie", 2.0, 3.0);
        hunter.addReward(Job.ActionType.KILL, "skeleton", 2.5, 4.0);
        hunter.addReward(Job.ActionType.KILL, "spider", 2.0, 3.0);
        hunter.addReward(Job.ActionType.KILL, "creeper", 3.0, 5.0);
        hunter.addReward(Job.ActionType.KILL, "enderman", 5.0, 8.0);
        hunter.addReward(Job.ActionType.KILL, "blaze", 6.0, 10.0);
        hunter.addReward(Job.ActionType.KILL, "wither_skeleton", 8.0, 12.0);
        jobs.put("hunter", hunter);

        // Builder
        Job builder = new Job("builder");
        builder.setName("§6Builder");
        builder.setDescription("Earn money by placing blocks");
        builder.setIcon(Items.BRICKS);
        builder.addReward(Job.ActionType.PLACE, "*", 0.25, 0.5);
        builder.addReward(Job.ActionType.PLACE, "bricks", 1.0, 1.5);
        builder.addReward(Job.ActionType.PLACE, "stone_bricks", 0.75, 1.0);
        jobs.put("builder", builder);
    }

    /**
     * Create default jobs configuration file
     */
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

    /**
     * Load player job data from database
     */
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
                    pj.setJoinedAt(rs.getLong("joined_at"));
                    pj.setLastWorked(rs.getLong("last_worked"));

                    playerJobs.computeIfAbsent(uuid, k -> new ArrayList<>()).add(pj);
                }
            } catch (SQLException e) {
                VonixCore.LOGGER.warn("Failed to load player jobs: {}", e.getMessage());
            }
        });
    }

    /**
     * Join a job
     */
    public boolean joinJob(ServerPlayer player, String jobId) {
        if (!enabled)
            return false;

        Job job = jobs.get(jobId.toLowerCase());
        if (job == null) {
            player.sendSystemMessage(Component.literal("Unknown job: " + jobId)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        List<PlayerJob> pJobs = playerJobs.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());

        // Check if already has job
        if (pJobs.stream().anyMatch(pj -> pj.getJobId().equalsIgnoreCase(jobId))) {
            player.sendSystemMessage(Component.literal("You already have this job!")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Check max jobs
        if (pJobs.size() >= maxJobs) {
            player.sendSystemMessage(Component.literal("You can only have " + maxJobs + " jobs!")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        PlayerJob newJob = new PlayerJob(player.getUUID(), jobId.toLowerCase());
        pJobs.add(newJob);

        // Save to database
        savePlayerJob(newJob);

        player.sendSystemMessage(Component.literal("You joined the " + job.getName() + " §rjob!")
                .withStyle(ChatFormatting.GREEN));
        return true;
    }

    /**
     * Leave a job
     */
    public boolean leaveJob(ServerPlayer player, String jobId) {
        List<PlayerJob> pJobs = playerJobs.get(player.getUUID());
        if (pJobs == null) {
            player.sendSystemMessage(Component.literal("You don't have any jobs!")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        PlayerJob toRemove = pJobs.stream()
                .filter(pj -> pj.getJobId().equalsIgnoreCase(jobId))
                .findFirst()
                .orElse(null);

        if (toRemove == null) {
            player.sendSystemMessage(Component.literal("You don't have that job!")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        pJobs.remove(toRemove);

        // Delete from database
        deletePlayerJob(player.getUUID(), jobId.toLowerCase());

        Job job = jobs.get(jobId.toLowerCase());
        String jobName = job != null ? job.getName() : jobId;
        player.sendSystemMessage(Component.literal("You left the " + jobName + " §rjob!")
                .withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /**
     * Process a job action
     */
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

            // Calculate rewards with multipliers
            double incomeMultiplier = job.getIncomeMultiplier(pJob.getLevel()) * globalIncomeMultiplier;
            double expMultiplier = job.getExpMultiplier(pJob.getLevel()) * globalExpMultiplier;

            double income = reward.income * incomeMultiplier;
            double exp = reward.experience * expMultiplier;

            // Give income
            if (income > 0) {
                eco.deposit(player.getUUID(), income);
            }

            // Add experience
            if (exp > 0) {
                boolean leveledUp = pJob.addExperience(exp, job);
                if (leveledUp) {
                    player.sendSystemMessage(Component.literal("Level up! " + job.getName() +
                            " §ris now level " + pJob.getLevel())
                            .withStyle(ChatFormatting.GOLD));
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

        String blockName = event.getState().getBlock().getDescriptionId();
        // Convert "block.minecraft.stone" to "stone"
        blockName = blockName.replace("block.minecraft.", "");

        processAction(player, Job.ActionType.BREAK, blockName);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!enabled)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        String blockName = event.getPlacedBlock().getBlock().getDescriptionId();
        blockName = blockName.replace("block.minecraft.", "");

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

        String entityName = event.getEntity().getType().getDescriptionId();
        // Convert "entity.minecraft.zombie" to "zombie"
        entityName = entityName.replace("entity.minecraft.", "");

        processAction(player, Job.ActionType.KILL, entityName);
    }

    /**
     * Get player's jobs
     */
    public List<PlayerJob> getPlayerJobs(UUID uuid) {
        return playerJobs.getOrDefault(uuid, new ArrayList<>());
    }

    /**
     * Get all available jobs
     */
    public Collection<Job> getAllJobs() {
        return jobs.values();
    }

    /**
     * Get a job by ID
     */
    public Job getJob(String jobId) {
        return jobs.get(jobId.toLowerCase());
    }

    // Database operations
    private void savePlayerJob(PlayerJob pj) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        """
                                INSERT OR REPLACE INTO vonixcore_player_jobs
                                (uuid, job_id, level, experience, joined_at, last_worked)
                                VALUES (?, ?, ?, ?, ?, ?)
                                """);
                stmt.setString(1, pj.getPlayerUuid().toString());
                stmt.setString(2, pj.getJobId());
                stmt.setInt(3, pj.getLevel());
                stmt.setDouble(4, pj.getExperience());
                stmt.setLong(5, pj.getJoinedAt());
                stmt.setLong(6, pj.getLastWorked());
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
