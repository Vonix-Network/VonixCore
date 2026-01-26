package network.vonix.vonixcore.config;

import java.nio.file.Path;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Essentials configuration (Economy, Homes, Warps).
 * Stored in config/vonixcore-essentials.yml
 */
public class EssentialsConfig extends BaseConfig {

        private static EssentialsConfig instance;
        
        // Forge-specific static fields
        public static EssentialsConfig CONFIG;
        public static ForgeConfigSpec SPEC;

        public static EssentialsConfig getInstance() {
                if (instance == null) {
                        instance = new EssentialsConfig();
                }
                return instance;
        }

        public static void init(Path configDir) {
                getInstance().loadConfig(configDir);
        }

        private EssentialsConfig() {
                super("vonixcore-essentials.yml");
        }

        private void loadConfig(Path configDir) {
                super.load(configDir);
        }

        @Override
        protected String getHeader() {
                return """
                                # VonixCore Essentials Configuration
                                # Economy, Homes, Warps, and Teleportation
                                """;
        }

        @Override
        protected void setDefaults() {
                // General
                setDefault("enabled", true);
                
                // Economy
                setDefault("economy.currency_symbol", "$");
                setDefault("economy.starting_balance", 100.0);
                setDefault("economy.tax_rate", 0.05); // 5% tax on shops

                // Homes
                setDefault("homes.max_homes_default", 3);
                setDefault("homes.max_homes_vip", 5);
                setDefault("homes.max_homes_mvp", 10);
                setDefault("homes.teleport_delay", 3);

                // Warps
                setDefault("warps.enable_gui", true);

                // Teleport
                setDefault("teleport.tpa_timeout", 60);
                setDefault("teleport.cooldown", 0);
                setDefault("teleport.back_timeout", 0);
                setDefault("teleport.death_back_timeout", 0);
                setDefault("teleport.death_back_delay", 3);
        }

        // Economy
        public String getCurrencySymbol() {
                return getString("economy.currency_symbol", "$");
        }

        public double getStartingBalance() {
                return getDouble("economy.starting_balance", 100.0);
        }

        public double getTaxRate() {
                return getDouble("economy.tax_rate", 0.05);
        }

        // Homes
        public int getDefaultMaxHomes() {
                return getInt("homes.max_homes_default", 3);
        }

        public int getVipMaxHomes() {
                return getInt("homes.max_homes_vip", 5);
        }

        public int getMvpMaxHomes() {
                return getInt("homes.max_homes_mvp", 10);
        }
        
        public int getMaxHomes() {
                return getDefaultMaxHomes();
        }

        public int getTeleportDelay() {
                return getInt("homes.teleport_delay", 3);
        }

        // Warps
        public boolean isWarpGuiEnabled() {
                return getBoolean("warps.enable_gui", true);
        }

        // Teleport
        public int getTpaTimeout() {
                return getInt("teleport.tpa_timeout", 60);
        }
        
        public int getCooldown() {
                return getInt("teleport.cooldown", 0);
        }
        
        public int getBackTimeout() {
                return getInt("teleport.back_timeout", 0);
        }
        
        public int getDeathBackTimeout() {
                return getInt("teleport.death_back_timeout", 0);
        }
        
        public int getDeathBackDelay() {
                return getInt("teleport.death_back_delay", 3);
        }
        
        public boolean isEnabled() {
                return getBoolean("enabled", true);
        }
}
