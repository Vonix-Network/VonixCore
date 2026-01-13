package network.vonix.vonixcore.listener;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.ProtectionConfig;
import network.vonix.vonixcore.teleport.TeleportManager;

/**
 * Handles entity-related events.
 */
public class EntityEventListener {

    /**
     * Register all entity event listeners.
     */
    public static void register() {
        // Entity death event
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                // Save death location for /back command
                TeleportManager.getInstance().saveLastLocation(player);
                VonixCore.LOGGER.debug("[Essentials] Saved death location for {}", player.getName().getString());

                // Log entity kill if protection is enabled
                if (VonixCore.getInstance().isProtectionEnabled()
                        && ProtectionConfig.getInstance().isLogEntityKills()) {
                    logEntityKill(player, damageSource);
                }
            }
        });
    }

    private static void logEntityKill(ServerPlayer player, DamageSource source) {
        VonixCore.executeAsync(() -> {
            try {
                String killer = "unknown";
                if (source.getEntity() != null) {
                    killer = source.getEntity() instanceof ServerPlayer killerPlayer
                            ? killerPlayer.getName().getString()
                            : source.getEntity().getType().getDescriptionId();
                }
                VonixCore.LOGGER.debug("[Protection] Player death: {} killed by {}",
                        player.getName().getString(), killer);
            } catch (Exception e) {
                VonixCore.LOGGER.error("[Protection] Failed to log entity death: {}", e.getMessage());
            }
        });
    }
}
