package network.vonix.vonixcore.config;

import java.nio.file.Path;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Essentials configuration (Economy, Homes, Warps).
 * Stored in config/vonixcore-essentials.toml
 */
public class EssentialsConfig {
        
        // Forge-specific static fields
        public static final ForgeConfigSpec SPEC;
        public static final EssentialsConfig CONFIG;

        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue maxHomesDefault;
        public final ForgeConfigSpec.IntValue maxHomesVip;
        public final ForgeConfigSpec.IntValue maxHomesMvp;
        public final ForgeConfigSpec.IntValue teleportDelay;
        public final ForgeConfigSpec.IntValue tpaTimeout;
        public final ForgeConfigSpec.IntValue cooldown;
        public final ForgeConfigSpec.IntValue backTimeout;
        public final ForgeConfigSpec.IntValue deathBackDelay;
        public final ForgeConfigSpec.BooleanValue warpGuiEnabled;
        public final ForgeConfigSpec.ConfigValue<String> currencySymbol;
        public final ForgeConfigSpec.DoubleValue startingBalance;
        public final ForgeConfigSpec.DoubleValue taxRate;

        public final ForgeConfigSpec.BooleanValue shopsEnabled;
        public final ForgeConfigSpec.BooleanValue jobsEnabled;
        public final ForgeConfigSpec.BooleanValue kitsEnabled;
        
        // RTP
        public final ForgeConfigSpec.BooleanValue rtpEnabled;
        public final ForgeConfigSpec.IntValue rtpCooldown;
        public final ForgeConfigSpec.IntValue rtpMinRange;
        public final ForgeConfigSpec.IntValue rtpMaxRange;
        public final ForgeConfigSpec.IntValue rtpMaxAttempts;
        public final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> rtpBlockedBiomes;

        static {
                Pair<EssentialsConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                        .configure(EssentialsConfig::new);
                CONFIG = pair.getLeft();
                SPEC = pair.getRight();
        }

        private EssentialsConfig(ForgeConfigSpec.Builder builder) {
                // General
                enabled = builder
                        .comment("Enable essentials features")
                        .define("enabled", true);
                shopsEnabled = builder
                        .comment("Enable shop features")
                        .define("shops.enabled", true);
                jobsEnabled = builder
                        .comment("Enable jobs features")
                        .define("jobs.enabled", true);
                kitsEnabled = builder
                        .comment("Enable kits features")
                        .define("kits.enabled", true);
                
                // Economy
                currencySymbol = builder
                        .comment("Currency symbol for economy")
                        .define("economy.currency_symbol", "$");
                startingBalance = builder
                        .comment("Starting balance for new players")
                        .defineInRange("economy.starting_balance", 100.0, 0.0, Double.MAX_VALUE);
                taxRate = builder
                        .comment("Tax rate for shops (0.05 = 5%)")
                        .defineInRange("economy.tax_rate", 0.05, 0.0, 1.0);

                // Homes
                maxHomesDefault = builder
                        .comment("Default maximum homes for regular players")
                        .defineInRange("homes.max_homes_default", 3, 0, 100);
                maxHomesVip = builder
                        .comment("Maximum homes for VIP players")
                        .defineInRange("homes.max_homes_vip", 5, 0, 100);
                maxHomesMvp = builder
                        .comment("Maximum homes for MVP players")
                        .defineInRange("homes.max_homes_mvp", 10, 0, 100);
                teleportDelay = builder
                        .comment("Teleport delay in seconds for homes")
                        .defineInRange("homes.teleport_delay", 3, 0, 60);

                // Warps
                warpGuiEnabled = builder
                        .comment("Enable GUI for warps")
                        .define("warps.enable_gui", true);

                // Teleport
                tpaTimeout = builder
                        .comment("TPA request timeout in seconds")
                        .defineInRange("teleport.tpa_timeout", 60, 10, 300);
                cooldown = builder
                        .comment("Teleport cooldown in seconds")
                        .defineInRange("teleport.cooldown", 0, 0, 300);
                backTimeout = builder
                        .comment("Back command timeout in seconds")
                        .defineInRange("teleport.back_timeout", 0, 0, 300);
                deathBackDelay = builder
                        .comment("Minimum time to wait before using /back after death (seconds)")
                        .comment("Prevents immediate return to boss fights etc.")
                        .defineInRange("teleport.death_back_delay", 60, 0, 3600);

                // RTP
                rtpEnabled = builder
                        .comment("Enable RTP command")
                        .define("teleport.rtp_enabled", true);
                rtpCooldown = builder
                        .comment("RTP cooldown in seconds")
                        .defineInRange("teleport.rtp_cooldown", 30, 0, 3600);
                rtpMinRange = builder
                        .comment("Minimum RTP radius")
                        .defineInRange("teleport.rtp_min_range", 1000, 0, 1000000);
                rtpMaxRange = builder
                        .comment("Maximum RTP radius")
                        .defineInRange("teleport.rtp_max_range", 10000, 0, 1000000);
                rtpMaxAttempts = builder
                        .comment("Maximum search attempts before giving up")
                        .defineInRange("teleport.rtp_max_attempts", 50, 1, 1000);
                rtpBlockedBiomes = builder
                        .comment("List of biome IDs to avoid (e.g. minecraft:ocean)")
                        .define("teleport.rtp_blocked_biomes", java.util.List.of("minecraft:ocean", "minecraft:deep_ocean"));
        }

        private static EssentialsConfig instance;

        public static EssentialsConfig getInstance() {
                if (instance == null) {
                        instance = CONFIG; // Use the Forge config instance
                }
                return instance;
        }

        public static void init(Path configDir) {
                // Config is already loaded by Forge, just initialize our instance reference
                getInstance();
        }

        // Economy
        public String getCurrencySymbol() {
                return CONFIG.currencySymbol.get();
        }

        public double getStartingBalance() {
                return CONFIG.startingBalance.get();
        }

        public double getTaxRate() {
                return CONFIG.taxRate.get();
        }

        public boolean isShopsEnabled() {
                return CONFIG.shopsEnabled.get();
        }

        public boolean isJobsEnabled() {
                return CONFIG.jobsEnabled.get();
        }

        public boolean isKitsEnabled() {
                return CONFIG.kitsEnabled.get();
        }

        // Homes
        public int getDefaultMaxHomes() {
                return CONFIG.maxHomesDefault.get();
        }

        public int getVipMaxHomes() {
                return CONFIG.maxHomesVip.get();
        }

        public int getMvpMaxHomes() {
                return CONFIG.maxHomesMvp.get();
        }
        
        public int getMaxHomes() {
                return getDefaultMaxHomes();
        }

        public int getTeleportDelay() {
                return CONFIG.teleportDelay.get();
        }

        // Warps
        public boolean isWarpGuiEnabled() {
                return CONFIG.warpGuiEnabled.get();
        }

        // Teleport
        public int getTpaTimeout() {
                return CONFIG.tpaTimeout.get();
        }
        
        public int getCooldown() {
                return CONFIG.cooldown.get();
        }
        
        public int getBackTimeout() {
                return CONFIG.backTimeout.get();
        }
        
        public int getDeathBackDelay() {
                return CONFIG.deathBackDelay.get();
        }
        
        public boolean isEnabled() {
                return CONFIG.enabled.get();
        }

        // RTP
        public int getRtpMinRange() {
                return CONFIG.rtpMinRange.get();
        }

        public int getRtpMaxRange() {
                return CONFIG.rtpMaxRange.get();
        }

        public int getRtpMaxAttempts() {
                return CONFIG.rtpMaxAttempts.get();
        }

        public java.util.List<? extends String> getRtpBlockedBiomes() {
                return CONFIG.rtpBlockedBiomes.get();
        }
}
