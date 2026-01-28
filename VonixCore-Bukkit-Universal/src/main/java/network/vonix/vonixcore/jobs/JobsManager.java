package network.vonix.vonixcore.jobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages jobs, player job data, and job rewards.
 * Inspired by Jobs Reborn plugin.
 */
public class JobsManager {

    private static JobsManager instance;

    private final VonixCore plugin;

    // All available jobs
    private final Map<String, Job> jobs = new LinkedHashMap<>();

    // Player job data (UUID -> list of their jobs)
    private final Map<UUID, List<PlayerJob>> playerJobs = new ConcurrentHashMap<>();

    // Configuration
    private int maxJobs = 3;
    private boolean enabled = true;
    private double globalIncomeMultiplier = 1.0;
    private double globalExpMultiplier = 1.0;

    public JobsManager(VonixCore plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static JobsManager getInstance() {
        return instance;
    }

    public void initialize(Connection conn) throws SQLException {
        createTables(conn);
        loadJobsConfig();
        loadPlayerData();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(new JobsListener(this), plugin);
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
        File configFile = new File(plugin.getDataFolder(), "jobs.yml");

        if (!configFile.exists()) {
            createDefaultJobsConfig(configFile);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        jobs.clear();

        // Load settings
        enabled = config.getBoolean("enabled", true);
        maxJobs = config.getInt("max-jobs", 3);
        globalIncomeMultiplier = config.getDouble("global-income-multiplier", 1.0);
        globalExpMultiplier = config.getDouble("global-exp-multiplier", 1.0);

        // Load jobs
        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");
        if (jobsSection == null)
            return;

        for (String jobId : jobsSection.getKeys(false)) {
            ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
            if (jobSection == null)
                continue;

            Job job = new Job(jobId);
            job.setName(jobSection.getString("name", jobId));
            job.setDescription(jobSection.getString("description", ""));
            job.setChatColor(jobSection.getString("color", "WHITE"));
            job.setMaxLevel(jobSection.getInt("max-level", 100));

            String iconName = jobSection.getString("icon", "DIAMOND_PICKAXE");
            Material icon = Material.getMaterial(iconName);
            if (icon != null)
                job.setIcon(icon);

            // Load actions
            ConfigurationSection actionsSection = jobSection.getConfigurationSection("actions");
            if (actionsSection != null) {
                for (String actionName : actionsSection.getKeys(false)) {
                    try {
                        Job.ActionType actionType = Job.ActionType.valueOf(actionName.toUpperCase());
                        ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionName);
                        if (actionSection != null) {
                            for (String target : actionSection.getKeys(false)) {
                                ConfigurationSection rewardSection = actionSection.getConfigurationSection(target);
                                if (rewardSection != null) {
                                    double income = rewardSection.getDouble("income", 0);
                                    double exp = rewardSection.getDouble("exp", 0);
                                    job.addReward(actionType, target, income, exp);
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Unknown action type: " + actionName);
                    }
                }
            }

            jobs.put(jobId.toLowerCase(), job);
        }

        plugin.getLogger().info("[Jobs] Loaded " + jobs.size() + " jobs");
    }

    /**
     * Create default jobs configuration
     */
    private void createDefaultJobsConfig(File file) {
        try {
            file.getParentFile().mkdirs();
            FileConfiguration config = new YamlConfiguration();

            config.set("enabled", true);
            config.set("max-jobs", 3);
            config.set("global-income-multiplier", 1.0);
            config.set("global-exp-multiplier", 1.0);

            // Miner job
            ConfigurationSection miner = config.createSection("jobs.miner");
            miner.set("name", "&7Miner");
            miner.set("description", "Earn money by mining ores and stone");
            miner.set("color", "GRAY");
            miner.set("icon", "DIAMOND_PICKAXE");
            miner.set("max-level", 100);

            ConfigurationSection minerBreak = miner.createSection("actions.break");
            minerBreak.set("stone.income", 0.5);
            minerBreak.set("stone.exp", 1.0);
            minerBreak.set("coal_ore.income", 2.0);
            minerBreak.set("coal_ore.exp", 3.0);
            minerBreak.set("iron_ore.income", 3.0);
            minerBreak.set("iron_ore.exp", 5.0);
            minerBreak.set("gold_ore.income", 5.0);
            minerBreak.set("gold_ore.exp", 8.0);
            minerBreak.set("diamond_ore.income", 10.0);
            minerBreak.set("diamond_ore.exp", 15.0);
            minerBreak.set("emerald_ore.income", 12.0);
            minerBreak.set("emerald_ore.exp", 20.0);
            minerBreak.set("deepslate_diamond_ore.income", 12.0);
            minerBreak.set("deepslate_diamond_ore.exp", 18.0);

            // Woodcutter job
            ConfigurationSection woodcutter = config.createSection("jobs.woodcutter");
            woodcutter.set("name", "&2Woodcutter");
            woodcutter.set("description", "Earn money by chopping trees");
            woodcutter.set("color", "DARK_GREEN");
            woodcutter.set("icon", "DIAMOND_AXE");
            woodcutter.set("max-level", 100);

            ConfigurationSection woodcutterBreak = woodcutter.createSection("actions.break");
            woodcutterBreak.set("oak_log.income", 1.0);
            woodcutterBreak.set("oak_log.exp", 2.0);
            woodcutterBreak.set("spruce_log.income", 1.0);
            woodcutterBreak.set("spruce_log.exp", 2.0);
            woodcutterBreak.set("birch_log.income", 1.0);
            woodcutterBreak.set("birch_log.exp", 2.0);
            woodcutterBreak.set("jungle_log.income", 1.5);
            woodcutterBreak.set("jungle_log.exp", 2.5);
            woodcutterBreak.set("acacia_log.income", 1.5);
            woodcutterBreak.set("acacia_log.exp", 2.5);
            woodcutterBreak.set("dark_oak_log.income", 1.5);
            woodcutterBreak.set("dark_oak_log.exp", 2.5);
            woodcutterBreak.set("cherry_log.income", 2.0);
            woodcutterBreak.set("cherry_log.exp", 3.0);

            // Farmer job
            ConfigurationSection farmer = config.createSection("jobs.farmer");
            farmer.set("name", "&aFarmer");
            farmer.set("description", "Earn money by farming crops");
            farmer.set("color", "GREEN");
            farmer.set("icon", "DIAMOND_HOE");
            farmer.set("max-level", 100);

            ConfigurationSection farmerBreak = farmer.createSection("actions.break");
            farmerBreak.set("wheat.income", 1.0);
            farmerBreak.set("wheat.exp", 1.5);
            farmerBreak.set("carrots.income", 1.0);
            farmerBreak.set("carrots.exp", 1.5);
            farmerBreak.set("potatoes.income", 1.0);
            farmerBreak.set("potatoes.exp", 1.5);
            farmerBreak.set("beetroots.income", 1.5);
            farmerBreak.set("beetroots.exp", 2.0);
            farmerBreak.set("nether_wart.income", 2.0);
            farmerBreak.set("nether_wart.exp", 3.0);

            ConfigurationSection farmerBreed = farmer.createSection("actions.breed");
            farmerBreed.set("cow.income", 5.0);
            farmerBreed.set("cow.exp", 8.0);
            farmerBreed.set("pig.income", 4.0);
            farmerBreed.set("pig.exp", 6.0);
            farmerBreed.set("sheep.income", 3.0);
            farmerBreed.set("sheep.exp", 5.0);
            farmerBreed.set("chicken.income", 2.0);
            farmerBreed.set("chicken.exp", 3.0);

            // Hunter job
            ConfigurationSection hunter = config.createSection("jobs.hunter");
            hunter.set("name", "&cHunter");
            hunter.set("description", "Earn money by killing mobs");
            hunter.set("color", "RED");
            hunter.set("icon", "DIAMOND_SWORD");
            hunter.set("max-level", 100);

            ConfigurationSection hunterKill = hunter.createSection("actions.kill");
            hunterKill.set("zombie.income", 2.0);
            hunterKill.set("zombie.exp", 3.0);
            hunterKill.set("skeleton.income", 2.5);
            hunterKill.set("skeleton.exp", 4.0);
            hunterKill.set("spider.income", 2.0);
            hunterKill.set("spider.exp", 3.0);
            hunterKill.set("creeper.income", 3.0);
            hunterKill.set("creeper.exp", 5.0);
            hunterKill.set("enderman.income", 5.0);
            hunterKill.set("enderman.exp", 8.0);
            hunterKill.set("blaze.income", 6.0);
            hunterKill.set("blaze.exp", 10.0);
            hunterKill.set("wither_skeleton.income", 8.0);
            hunterKill.set("wither_skeleton.exp", 12.0);
            hunterKill.set("warden.income", 350.0);
            hunterKill.set("warden.exp", 100.0);

            // Fisherman job
            ConfigurationSection fisherman = config.createSection("jobs.fisherman");
            fisherman.set("name", "&bFisherman");
            fisherman.set("description", "Earn money by fishing");
            fisherman.set("color", "AQUA");
            fisherman.set("icon", "FISHING_ROD");
            fisherman.set("max-level", 100);

            ConfigurationSection fishermanFish = fisherman.createSection("actions.fish");
            fishermanFish.set("cod.income", 3.0);
            fishermanFish.set("cod.exp", 4.0);
            fishermanFish.set("salmon.income", 4.0);
            fishermanFish.set("salmon.exp", 5.0);
            fishermanFish.set("tropical_fish.income", 5.0);
            fishermanFish.set("tropical_fish.exp", 7.0);
            fishermanFish.set("pufferfish.income", 6.0);
            fishermanFish.set("pufferfish.exp", 8.0);
            fishermanFish.set("*.income", 2.0); // Default for any catch
            fishermanFish.set("*.exp", 3.0);

            // Builder job
            ConfigurationSection builder = config.createSection("jobs.builder");
            builder.set("name", "&6Builder");
            builder.set("description", "Earn money by placing blocks");
            builder.set("color", "GOLD");
            builder.set("icon", "BRICKS");
            builder.set("max-level", 100);

            ConfigurationSection builderPlace = builder.createSection("actions.place");
            builderPlace.set("*.income", 0.25); // Any block
            builderPlace.set("*.exp", 0.5);
            builderPlace.set("bricks.income", 1.0);
            builderPlace.set("bricks.exp", 1.5);
            builderPlace.set("stone_bricks.income", 0.75);
            builderPlace.set("stone_bricks.exp", 1.0);

            // Crafter job
            ConfigurationSection crafter = config.createSection("jobs.crafter");
            crafter.set("name", "&eCrafter");
            crafter.set("description", "Earn money by crafting items");
            crafter.set("color", "YELLOW");
            crafter.set("icon", "CRAFTING_TABLE");
            crafter.set("max-level", 100);

            ConfigurationSection crafterCraft = crafter.createSection("actions.craft");
            crafterCraft.set("iron_pickaxe.income", 5.0);
            crafterCraft.set("iron_pickaxe.exp", 8.0);
            crafterCraft.set("diamond_pickaxe.income", 15.0);
            crafterCraft.set("diamond_pickaxe.exp", 20.0);
            crafterCraft.set("iron_sword.income", 4.0);
            crafterCraft.set("iron_sword.exp", 6.0);
            crafterCraft.set("diamond_sword.income", 12.0);
            crafterCraft.set("diamond_sword.exp", 18.0);

            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create jobs config", e);
        }
    }

    /**
     * Load player job data from database
     */
    private void loadPlayerData() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
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
                plugin.getLogger().log(Level.WARNING, "Failed to load player jobs", e);
            }
        });
    }

    /**
     * Join a job
     */
    public boolean joinJob(Player player, String jobId) {
        if (!enabled)
            return false;

        Job job = jobs.get(jobId.toLowerCase());
        if (job == null) {
            player.sendMessage(Component.text("Unknown job: " + jobId).color(NamedTextColor.RED));
            return false;
        }

        List<PlayerJob> pJobs = playerJobs.computeIfAbsent(player.getUniqueId(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());

        // Check if already has job
        if (pJobs.stream().anyMatch(pj -> pj.getJobId().equalsIgnoreCase(jobId))) {
            player.sendMessage(Component.text("You already have this job!").color(NamedTextColor.RED));
            return false;
        }

        // Check max jobs
        if (pJobs.size() >= maxJobs && !player.hasPermission("vonixcore.jobs.unlimited")) {
            player.sendMessage(Component.text("You can only have " + maxJobs + " jobs!")
                    .color(NamedTextColor.RED));
            return false;
        }

        PlayerJob newJob = new PlayerJob(player.getUniqueId(), jobId.toLowerCase());
        pJobs.add(newJob);

        // Save to database
        savePlayerJob(newJob);

        player.sendMessage(Component.text("You joined the " + job.getName() + " &rjob!")
                .color(NamedTextColor.GREEN));
        return true;
    }

    /**
     * Leave a job
     */
    public boolean leaveJob(Player player, String jobId) {
        List<PlayerJob> pJobs = playerJobs.get(player.getUniqueId());
        if (pJobs == null) {
            player.sendMessage(Component.text("You don't have any jobs!").color(NamedTextColor.RED));
            return false;
        }

        PlayerJob toRemove = pJobs.stream()
                .filter(pj -> pj.getJobId().equalsIgnoreCase(jobId))
                .findFirst()
                .orElse(null);

        if (toRemove == null) {
            player.sendMessage(Component.text("You don't have that job!").color(NamedTextColor.RED));
            return false;
        }

        pJobs.remove(toRemove);

        // Delete from database
        deletePlayerJob(player.getUniqueId(), jobId.toLowerCase());

        Job job = jobs.get(jobId.toLowerCase());
        String jobName = job != null ? job.getName() : jobId;
        player.sendMessage(Component.text("You left the " + jobName + " &rjob!")
                .color(NamedTextColor.YELLOW));
        return true;
    }

    /**
     * Process a job action - called by JobsListener
     */
    public void processAction(Player player, Job.ActionType actionType, String target) {
        if (!enabled)
            return;

        List<PlayerJob> pJobs = playerJobs.get(player.getUniqueId());
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
                eco.deposit(player.getUniqueId(), income);
            }

            // Add experience
            if (exp > 0) {
                boolean leveledUp = pJob.addExperience(exp, job);
                if (leveledUp) {
                    player.sendMessage(
                            Component.text("Level up! " + job.getName() + " &ris now level " + pJob.getLevel())
                                    .color(NamedTextColor.GOLD));
                }
            }
        }
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
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
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
                plugin.getLogger().log(Level.WARNING, "Failed to save player job", e);
            }
        });
    }

    private void deletePlayerJob(UUID uuid, String jobId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM vonixcore_player_jobs WHERE uuid = ? AND job_id = ?");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, jobId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete player job", e);
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

    public void loadPlayerJobs(UUID uuid) {
        // Already loaded in initialize()
    }

    public void unloadPlayerJobs(UUID uuid) {
        // Jobs are saved on shutdown
    }
}
