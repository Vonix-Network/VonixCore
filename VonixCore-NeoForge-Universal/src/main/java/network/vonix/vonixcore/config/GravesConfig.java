package network.vonix.vonixcore.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import network.vonix.vonixcore.VonixCore;

/**
 * Configuration for the Graves system.
 */
@EventBusSubscriber(modid = VonixCore.MODID, bus = EventBusSubscriber.Bus.MOD)
public class GravesConfig {

        public static final ModConfigSpec SPEC;
        public static final GravesConfig CONFIG;

        // General settings
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.BooleanValue autoDisableIfAlternativeDetected;

        // Timing settings
        public final ModConfigSpec.IntValue expirationTime;
        public final ModConfigSpec.IntValue protectionTime;

        // XP settings
        public final ModConfigSpec.DoubleValue xpRetention;

        // Limits
        public final ModConfigSpec.IntValue maxGravesPerPlayer;

        static {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
                CONFIG = new GravesConfig(builder);
                SPEC = builder.build();
        }

        private GravesConfig(ModConfigSpec.Builder builder) {
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
