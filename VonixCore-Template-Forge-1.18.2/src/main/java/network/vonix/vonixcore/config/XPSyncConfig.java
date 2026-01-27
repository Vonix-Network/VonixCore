package network.vonix.vonixcore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class XPSyncConfig {
    public static final ForgeConfigSpec SPEC;
    public static final XPSyncConfig CONFIG;

    public final ForgeConfigSpec.BooleanValue enabled;
    public final ForgeConfigSpec.ConfigValue<String> apiEndpoint;
    public final ForgeConfigSpec.ConfigValue<String> apiKey;
    public final ForgeConfigSpec.ConfigValue<String> serverName;
    public final ForgeConfigSpec.IntValue syncInterval;

    public final ForgeConfigSpec.BooleanValue trackPlaytime;
    public final ForgeConfigSpec.BooleanValue trackHealth;
    public final ForgeConfigSpec.BooleanValue verboseLogging;

    public final ForgeConfigSpec.IntValue connectionTimeout;
    public final ForgeConfigSpec.IntValue maxRetries;

    static {
        Pair<XPSyncConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder()
                .configure(XPSyncConfig::new);
        CONFIG = pair.getLeft();
        SPEC = pair.getRight();
    }

    private XPSyncConfig(ForgeConfigSpec.Builder builder) {
        builder.push("xpsync");

        enabled = builder.comment("Enable the XP Sync module")
                .define("enabled", true);

        builder.push("api");
        apiEndpoint = builder.define("endpoint", "https://vonix.network/api/minecraft/sync/xp");
        apiKey = builder.define("api_key", "YOUR_API_KEY_HERE");
        serverName = builder.define("server_name", "Server-1");
        syncInterval = builder.defineInRange("sync_interval", 300, 60, 86400); // 5 mins default
        builder.pop();

        builder.push("data");
        trackPlaytime = builder.define("track_playtime", true);
        trackHealth = builder.define("track_health", true);
        builder.pop();

        builder.push("advanced");
        verboseLogging = builder.define("verbose_logging", false);
        connectionTimeout = builder.defineInRange("connection_timeout", 10000, 1000, 60000);
        maxRetries = builder.defineInRange("max_retries", 3, 1, 10);
        builder.pop();

        builder.pop();
    }
}
