package network.vonix.vonixcore.jobs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.economy.EconomyManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages jobs, player job data, and job rewards.
 * Fabric 1.20.1 Implementation
 */
public class JobsManager {

    private static JobsManager instance;

    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private final Map<UUID, List<PlayerJob>> playerJobs = new ConcurrentHashMap<>();

    // Settings from config
    private boolean enabled = true;
    private int maxJobs = 3;
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
        // Listener registration will be handled in VonixCore or via a separate init method
    }

    private void createTables(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("""
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
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pj_uuid ON vonixcore_player_jobs(uuid)");
        }
    }

    public void loadJobsConfig() {
        JobsConfig config = JobsConfig.getInstance();
        jobs.clear();

        enabled = config.getBoolean("enabled", true);
        maxJobs = config.getInt("max-jobs", 3);
        globalIncomeMultiplier = config.getDouble("global-income-multiplier", 1.0);
        globalExpMultiplier = config.getDouble("global-exp-multiplier", 1.0);

        loadJobFromConfig("miner");
        loadJobFromConfig("woodcutter");
        loadJobFromConfig("farmer");
        loadJobFromConfig("hunter");
        loadJobFromConfig("fisherman");
        loadJobFromConfig("builder");
        loadJobFromConfig("crafter");

        VonixCore.LOGGER.info("[Jobs] Loaded {} jobs", jobs.size());
    }

    private void loadJobFromConfig(String jobId) {
        JobsConfig config = JobsConfig.getInstance();
        String path = "jobs." + jobId;
        
        String name = config.getString(path + ".name", null);
        if (name == null) return; 

        Job job = new Job(jobId);
        job.setName(name);
        job.setDescription(config.getString(path + ".description", ""));
        job.setChatColor(config.getString(path + ".color", "WHITE"));
        job.setIcon(config.getString(path + ".icon", "DIRT"));
        job.setMaxLevel(config.getInt(path + ".max-level", 100));

        loadStandardActions(job, config, path);

        jobs.put(jobId.toLowerCase(), job);
    }

    private void loadStandardActions(Job job, JobsConfig config, String rootPath) {
        // Miner
        if (job.getId().equals("miner")) {
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "stone");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "coal_ore");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "iron_ore");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "gold_ore");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "diamond_ore");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "deepslate_diamond_ore");
        }
        // Woodcutter
        else if (job.getId().equals("woodcutter")) {
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "oak_log");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "spruce_log");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "birch_log");
        }
        // Farmer
        else if (job.getId().equals("farmer")) {
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "wheat");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "carrots");
            loadAction(job, config, rootPath, Job.ActionType.BREAK, "potatoes");
        }
        // Hunter
        else if (job.getId().equals("hunter")) {
            loadAction(job, config, rootPath, Job.ActionType.KILL, "zombie");
            loadAction(job, config, rootPath, Job.ActionType.KILL, "skeleton");
            loadAction(job, config, rootPath, Job.ActionType.KILL, "creeper");
        }
        // Fisherman
        else if (job.getId().equals("fisherman")) {
            loadAction(job, config, rootPath, Job.ActionType.FISH, "cod");
            loadAction(job, config, rootPath, Job.ActionType.FISH, "salmon");
        }
    }

    private void loadAction(Job job, JobsConfig config, String rootPath, Job.ActionType type, String target) {
        String actionPath = rootPath + ".actions." + type.name().toLowerCase() + "." + target;
        double income = config.getDouble(actionPath + ".income", 0);
        double exp = config.getDouble(actionPath + ".exp", 0);
        
        if (income > 0 || exp > 0) {
            job.addReward(type, target, income, exp);
        }
    }

    public void loadPlayerJobs(UUID uuid) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM vonixcore_player_jobs WHERE uuid = ?");
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                List<PlayerJob> loaded = new ArrayList<>();
                while (rs.next()) {
                    PlayerJob pj = new PlayerJob();
                    pj.setPlayerUuid(uuid);
                    pj.setJobId(rs.getString("job_id"));
                    pj.setLevel(rs.getInt("level"));
                    pj.setExperience(rs.getDouble("experience"));
                    pj.setJoinedAt(rs.getLong("joined_at"));
                    pj.setLastWorked(rs.getLong("last_worked"));
                    loaded.add(pj);
                }
                
                playerJobs.put(uuid, loaded);
            } catch (SQLException e) {
                VonixCore.LOGGER.error("Failed to load jobs for {}: {}", uuid, e.getMessage());
            }
        });
    }

    public void unloadPlayerJobs(UUID uuid) {
        List<PlayerJob> jobs = playerJobs.get(uuid);
        if (jobs != null) {
            jobs.forEach(this::savePlayerJob);
            playerJobs.remove(uuid);
        }
    }

    public boolean joinJob(ServerPlayer player, String jobId) {
        if (!enabled) return false;

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
            player.sendSystemMessage(Component.literal("You can only have " + maxJobs + " jobs!").withStyle(ChatFormatting.RED));
            return false;
        }

        PlayerJob newJob = new PlayerJob(player.getUUID(), jobId.toLowerCase());
        pJobs.add(newJob);
        savePlayerJob(newJob);

        player.sendSystemMessage(Component.literal("You joined the " + job.getName() + " job!").withStyle(ChatFormatting.GREEN));
        return true;
    }

    public boolean leaveJob(ServerPlayer player, String jobId) {
        List<PlayerJob> pJobs = playerJobs.get(player.getUUID());
        if (pJobs == null) return false;

        Optional<PlayerJob> toRemove = pJobs.stream()
                .filter(pj -> pj.getJobId().equalsIgnoreCase(jobId))
                .findFirst();

        if (toRemove.isEmpty()) {
            player.sendSystemMessage(Component.literal("You don't have that job!").withStyle(ChatFormatting.RED));
            return false;
        }

        pJobs.remove(toRemove.get());
        deletePlayerJob(player.getUUID(), jobId.toLowerCase());

        player.sendSystemMessage(Component.literal("You left the job.").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public void processAction(ServerPlayer player, Job.ActionType actionType, String target) {
        if (!enabled) return;

        List<PlayerJob> pJobs = playerJobs.get(player.getUUID());
        if (pJobs == null || pJobs.isEmpty()) return;

        EconomyManager eco = EconomyManager.getInstance();

        for (PlayerJob pJob : pJobs) {
            Job job = jobs.get(pJob.getJobId());
            if (job == null) continue;

            Job.JobReward reward = job.getReward(actionType, target);
            if (reward == null) continue;

            double incomeMultiplier = job.getIncomeMultiplier(pJob.getLevel()) * globalIncomeMultiplier;
            double expMultiplier = job.getExpMultiplier(pJob.getLevel()) * globalExpMultiplier;

            double income = reward.income() * incomeMultiplier;
            double exp = reward.experience() * expMultiplier;

            if (income > 0) {
                eco.deposit(player.getUUID(), income);
                player.sendSystemMessage(Component.literal("+" + String.format("%.2f", income) + " (" + job.getName() + ")")
                        .withStyle(ChatFormatting.GREEN));
            }

            if (exp > 0) {
                boolean leveledUp = pJob.addExperience(exp, job);
                if (leveledUp) {
                    player.sendSystemMessage(Component.literal("Level Up! " + job.getName() + " is now level " + pJob.getLevel())
                            .withStyle(ChatFormatting.GOLD));
                    savePlayerJob(pJob); 
                }
            }
        }
    }
    
    public List<PlayerJob> getPlayerJobs(UUID uuid) {
        return playerJobs.getOrDefault(uuid, Collections.emptyList());
    }

    public Collection<Job> getAllJobs() {
        return jobs.values();
    }

    public Job getJob(String id) {
        return jobs.get(id.toLowerCase());
    }

    private void savePlayerJob(PlayerJob pj) {
        VonixCore.executeAsync(() -> {
            try (Connection conn = VonixCore.getInstance().getDatabase().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("""
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
                VonixCore.LOGGER.error("Failed to save player job", e);
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
                VonixCore.LOGGER.error("Failed to delete player job", e);
            }
        });
    }

    public void shutdown() {
        for (List<PlayerJob> list : playerJobs.values()) {
            for (PlayerJob pj : list) {
                savePlayerJob(pj);
            }
        }
    }
}
