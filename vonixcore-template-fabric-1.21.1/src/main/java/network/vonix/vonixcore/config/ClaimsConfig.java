package network.vonix.vonixcore.config;

import java.nio.file.Path;

/**
 * Configuration for the Claims protection module.
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
                # VonixCore Claims Module Configuration
                # Land claiming and protection settings
                """;
    }

    @Override
    protected void setDefaults() {
        // Master toggle
        setDefault("claims.enabled", true);

        // Limits (matching Forge)
        setDefault("limits.default_claim_radius", 10);
        setDefault("limits.max_claim_size", 100);
        setDefault("limits.max_claims_per_player", 5);

        // Permissions
        setDefault("permissions.require_permission_to_create", false);

        // Protection (matching Forge)
        setDefault("protection.protect_building", true);
        setDefault("protection.protect_containers", true);
        setDefault("protection.protect_entities", true);
        setDefault("protection.prevent_explosions", true);
        setDefault("protection.prevent_fire_spread", true);

        // Integration
        setDefault("integration.allow_vonix_shops_bypass", true);
    }

    // ============ Getters ============

    public boolean isEnabled() {
        return getBoolean("claims.enabled", true);
    }

    // Limits
    public int getDefaultClaimRadius() {
        return getInt("limits.default_claim_radius", 10);
    }

    public int getMaxClaimSize() {
        return getInt("limits.max_claim_size", 100);
    }

    public int getMaxClaimsPerPlayer() {
        return getInt("limits.max_claims_per_player", 5);
    }

    // Permissions
    public boolean isRequirePermissionToCreate() {
        return getBoolean("permissions.require_permission_to_create", false);
    }

    // Protection
    public boolean isProtectBuilding() {
        return getBoolean("protection.protect_building", true);
    }

    public boolean isProtectContainers() {
        return getBoolean("protection.protect_containers", true);
    }

    public boolean isProtectEntities() {
        return getBoolean("protection.protect_entities", true);
    }

    public boolean isPreventExplosions() {
        return getBoolean("protection.prevent_explosions", true);
    }

    public boolean isPreventFireSpread() {
        return getBoolean("protection.prevent_fire_spread", true);
    }

    // Integration
    public boolean isAllowVonixShopsBypass() {
        return getBoolean("integration.allow_vonix_shops_bypass", true);
    }
}
