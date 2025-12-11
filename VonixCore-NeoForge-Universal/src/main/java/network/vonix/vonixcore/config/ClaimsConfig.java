package network.vonix.vonixcore.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for the Claims protection module.
 */
public class ClaimsConfig {

    public static final ModConfigSpec SPEC;
    public static final ClaimsConfig CONFIG;

    // Master toggle
    public final ModConfigSpec.BooleanValue enabled;

    // Claim limits
    public final ModConfigSpec.IntValue defaultClaimRadius;
    public final ModConfigSpec.IntValue maxClaimSize;
    public final ModConfigSpec.IntValue maxClaimsPerPlayer;

    // Permission settings
    public final ModConfigSpec.BooleanValue requirePermissionToCreate;

    // Protection toggles
    public final ModConfigSpec.BooleanValue protectBuilding;
    public final ModConfigSpec.BooleanValue protectContainers;
    public final ModConfigSpec.BooleanValue protectEntities;
    public final ModConfigSpec.BooleanValue preventExplosions;
    public final ModConfigSpec.BooleanValue preventFireSpread;

    // Shop integration
    public final ModConfigSpec.BooleanValue allowVonixShopsBypass;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CONFIG = new ClaimsConfig(builder);
        SPEC = builder.build();
    }

    private ClaimsConfig(ModConfigSpec.Builder builder) {
        builder.comment("VonixCore Claims Module Configuration")
                .push("claims");

        enabled = builder
                .comment("Enable the claims protection module")
                .define("enabled", true);

        builder.push("limits");

        defaultClaimRadius = builder
                .comment("Default radius when creating a claim with /vcclaims create")
                .defineInRange("defaultClaimRadius", 10, 1, 100);

        maxClaimSize = builder
                .comment("Maximum claim size (blocks per side). 0 = unlimited")
                .defineInRange("maxClaimSize", 100, 0, 1000);

        maxClaimsPerPlayer = builder
                .comment("Maximum claims per player. 0 = unlimited")
                .defineInRange("maxClaimsPerPlayer", 5, 0, 100);

        builder.pop();

        builder.push("permissions");

        requirePermissionToCreate = builder
                .comment("If true, players need vonixcore.claims.create permission to create claims")
                .define("requirePermissionToCreate", false);

        builder.pop();

        builder.push("protection");

        protectBuilding = builder
                .comment("Prevent block breaking and placing in claims")
                .define("protectBuilding", true);

        protectContainers = builder
                .comment("Prevent opening chests, barrels, and other containers")
                .define("protectContainers", true);

        protectEntities = builder
                .comment("Prevent damaging or interacting with entities")
                .define("protectEntities", true);

        preventExplosions = builder
                .comment("Prevent explosions from damaging claims")
                .define("preventExplosions", true);

        preventFireSpread = builder
                .comment("Prevent fire from spreading in claims")
                .define("preventFireSpread", true);

        builder.pop();

        builder.push("integration");

        allowVonixShopsBypass = builder
                .comment("Allow VonixCore shop interactions even for non-trusted players",
                        "This lets shops work inside protected claims")
                .define("allowVonixShopsBypass", true);

        builder.pop();
        builder.pop();
    }
}
