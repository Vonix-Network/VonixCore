package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * Claims configuration for VonixCore.
 * Stored in config/vonixcore-claims.yml
 */
public class ClaimsConfig extends BaseConfig {

    private static ClaimsConfig instance;

    public static ClaimsConfig getInstance() {
        if (instance == null) {
            instance = new ClaimsConfig();
        }
        return instance;
    }

    public static void init(Path configDir) {
        getInstance().loadConfig(configDir);
    }

    private ClaimsConfig() {
        super("vonixcore-claims.yml");
    }

    private void loadConfig(Path configDir) {
        super.load(configDir);
    }

    @Override
    protected String getHeader() {
        return """
                # VonixCore Claims Configuration
                # Land claiming and protection settings
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("claims.enabled", true);

        // Claim limits
        setDefault("limits.max_claims_per_player", 5);
        setDefault("limits.min_claim_size", 100);
        setDefault("limits.max_claim_size", 10000);
        setDefault("limits.claims_cost", 0.0);

        // Claim blocks
        setDefault("blocks.starting_blocks", 500);
        setDefault("blocks.blocks_per_hour", 100);
        setDefault("blocks.max_accrued_blocks", 50000);

        // Protection
        setDefault("protection.prevent_block_break", true);
        setDefault("protection.prevent_block_place", true);
        setDefault("protection.prevent_container_access", true);
        setDefault("protection.prevent_entity_damage", true);
        setDefault("protection.prevent_explosions", true);
        setDefault("protection.prevent_fire_spread", true);

        // Trust levels
        setDefault("trust.allow_container_trust", true);
        setDefault("trust.allow_build_trust", true);
        setDefault("trust.allow_access_trust", true);
        setDefault("trust.allow_manager_trust", true);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("claims.enabled", true);
    }

    // Limits
    public int getMaxClaimsPerPlayer() {
        return getInt("limits.max_claims_per_player", 5);
    }

    public int getMinClaimSize() {
        return getInt("limits.min_claim_size", 100);
    }

    public int getMaxClaimSize() {
        return getInt("limits.max_claim_size", 10000);
    }

    public double getClaimsCost() {
        return getDouble("limits.claims_cost", 0.0);
    }

    // Blocks
    public int getStartingBlocks() {
        return getInt("blocks.starting_blocks", 500);
    }

    public int getBlocksPerHour() {
        return getInt("blocks.blocks_per_hour", 100);
    }

    public int getMaxAccruedBlocks() {
        return getInt("blocks.max_accrued_blocks", 50000);
    }

    // Protection
    public boolean isPreventBlockBreak() {
        return getBoolean("protection.prevent_block_break", true);
    }

    public boolean isPreventBlockPlace() {
        return getBoolean("protection.prevent_block_place", true);
    }

    public boolean isPreventContainerAccess() {
        return getBoolean("protection.prevent_container_access", true);
    }

    public boolean isPreventEntityDamage() {
        return getBoolean("protection.prevent_entity_damage", true);
    }

    public boolean isPreventExplosions() {
        return getBoolean("protection.prevent_explosions", true);
    }

    public boolean isPreventFireSpread() {
        return getBoolean("protection.prevent_fire_spread", true);
    }

    // Trust
    public boolean isAllowContainerTrust() {
        return getBoolean("trust.allow_container_trust", true);
    }

    public boolean isAllowBuildTrust() {
        return getBoolean("trust.allow_build_trust", true);
    }

    public boolean isAllowAccessTrust() {
        return getBoolean("trust.allow_access_trust", true);
    }

    public boolean isAllowManagerTrust() {
        return getBoolean("trust.allow_manager_trust", true);
    }
}
