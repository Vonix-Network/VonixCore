package network.vonix.vonixcore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import network.vonix.vonixcore.VonixCore;

/**
 * Configuration for the Graves system.
 */
@Mod.EventBusSubscriber(modid = VonixCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class GravesConfig {

        public static final ForgeConfigSpec SPEC;
        public static final GravesConfig CONFIG;

        // General settings
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.BooleanValue autoDisableIfAlternativeDetected;

        // Timing settings
        public final ForgeConfigSpec.IntValue expirationTime;
        public final ForgeConfigSpec.IntValue protectionTime;

        // XP settings
        public final ForgeConfigSpec.DoubleValue xpRetention;

        // Limits
        public final ForgeConfigSpec.IntValue maxGravesPerPlayer;

        static {
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
                CONFIG = new GravesConfig(builder);
                SPEC = builder.build();
        }

        private GravesConfig(ForgeConfigSpec.Builder builder) {
                builder.comment("VonixCore Graves Configuration")
                                .push("graves");

                enabled = builder
                                .comment("Enable the graves system. When a player dies, their items are stored in a grave.")
                                .define("enabled", true);

                autoDisableIfAlternativeDetected = builder
                                .comment("Automatically disable VonixCore graves if another grave mod is detected.",
                                                "Detected mods: YIGD, Corpse, GraveStone, Universal Graves, Death Chest, etc.")
                                .define("autoDisableIfAlternativeDetected", true);

                expirationTime = builder
                                .comment("Time in seconds before a grave expires and is removed.",
                                                "Default: 3600 (1 hour)")
                                .defineInRange("expirationTime", 3600, 60, 86400);

                protectionTime = builder
                                .comment("Time in seconds that a grave is protected (only owner can loot).",
                                                "Default: 300 (5 minutes)")
                                .defineInRange("protectionTime", 300, 0, 3600);

                xpRetention = builder
                                .comment("Percentage of XP stored in the grave (0.0 to 1.0).",
                                                "Default: 0.8 (80%)")
                                .defineInRange("xpRetention", 0.8, 0.0, 1.0);

                maxGravesPerPlayer = builder
                                .comment("Maximum number of active graves per player.",
                                                "Oldest graves are removed when limit is exceeded.")
                                .defineInRange("maxGravesPerPlayer", 5, 1, 20);

                builder.pop();
        }

        @SubscribeEvent
        static void onLoad(final ModConfigEvent event) {
                if (event.getConfig().getSpec() == SPEC) {
                        VonixCore.LOGGER.info("[VonixCore] Graves config loaded");
                }
        }

        /**
         * Check if the graves system should be enabled.
         * Takes into account both the config setting and alternative mod detection.
         */
        public boolean shouldBeEnabled() {
                return enabled.get();
        }
}
