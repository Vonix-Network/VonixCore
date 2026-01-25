package network.vonix.vonixcore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Essentials configuration for VonixCore.
 * Stored in config/vonixcore-essentials.toml
 */
public class EssentialsConfig {

    public static final ForgeConfigSpec SPEC;
    public static final EssentialsConfig CONFIG;

    // Master toggle
    public final ForgeConfigSpec.BooleanValue enabled;

    // TPA settings
    public final ForgeConfigSpec.IntValue tpaCooldown;
    public final ForgeConfigSpec.IntValue tpaTimeout;
    public final ForgeConfigSpec.IntValue backTimeout;
    public final ForgeConfigSpec.IntValue deathBackTimeout;
    public final ForgeConfigSpec.IntValue deathBackDelay;

    // RTP settings
    public final ForgeConfigSpec.IntValue rtpCooldown;
    public final ForgeConfigSpec.IntValue rtpMaxRange;
    public final ForgeConfigSpec.IntValue rtpMinRange;

    static {
        Pair<EssentialsConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                .configure(EssentialsConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private EssentialsConfig(ForgeConfigSpec.Builder builder) {
        builder.comment(
                "VonixCore Essentials Configuration",
                "Teleportation and utility settings")
                .push("essentials");

        enabled = builder.comment(
                "Enable the essentials module",
                "Set to false to completely disable all essentials features")
                .define("enabled", true);

        builder.pop().comment(
                "TPA Settings",
                "Configure teleport request feature")
                .push("tpa");

        tpaCooldown = builder.comment(
                "Cooldown between TPA requests in seconds")
                .defineInRange("cooldown", 30, 0, 3600);

        tpaTimeout = builder.comment(
                "How long TPA requests remain valid (seconds)",
                "After this time, the request expires")
                .defineInRange("timeout", 120, 30, 600);

        backTimeout = builder.comment(
                "How long /back locations remain valid (seconds)",
                "Set to 0 to disable timeout (infinite)")
                .defineInRange("back_timeout", 300, 0, 86400);

        deathBackTimeout = builder.comment(
                "How long /back locations remain valid after death (seconds)",
                "Set to 0 to disable timeout (infinite)")
                .defineInRange("death_back_timeout", 60, 0, 86400);

        deathBackDelay = builder.comment(
                "Minimum time to wait before using /back after death (seconds)",
                "This is the cooldown that only applies after death",
                "Prevents immediate return to boss fights etc.")
                .defineInRange("death_back_delay", 0, 0, 3600);

        builder.pop().comment(
                "RTP Settings",
                "Configure the random teleport feature")
                .push("rtp");

        rtpCooldown = builder.comment(
                "Cooldown between /rtp uses in seconds",
                "Set to 0 to disable cooldown")
                .defineInRange("cooldown", 600, 0, 86400);

        rtpMaxRange = builder.comment(
                "Maximum distance from spawn for random teleport")
                .defineInRange("max_range", 10000, 100, 100000);

        rtpMinRange = builder.comment(
                "Minimum distance from spawn for random teleport")
                .defineInRange("min_range", 500, 0, 50000);

        builder.pop();
    }
}

