package network.vonix.vonixcore.xpsync;

import net.minecraft.server.MinecraftServer;
import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.XPSyncConfig;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages XP synchronization to external API.
 */
public class XPSyncManager {

    private final MinecraftServer server;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public XPSyncManager(MinecraftServer server) {
        this.server = server;
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VonixCore-XPSync");
            t.setDaemon(true);
            return t;
        });

        int interval = XPSyncConfig.getInstance().getSyncInterval();
        scheduler.scheduleWithFixedDelay(this::syncAll, interval, interval, TimeUnit.SECONDS);

        VonixCore.LOGGER.info("[XPSync] Started with {}s interval", interval);
    }

    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        VonixCore.LOGGER.info("[XPSync] Stopped");
    }

    private void syncAll() {
        try {
            server.getPlayerList().getPlayers().forEach(player -> {
                int xp = player.experienceLevel;
                // TODO: Send to API
                VonixCore.LOGGER.debug("[XPSync] {} has {} XP levels", player.getName().getString(), xp);
            });
        } catch (Exception e) {
            VonixCore.LOGGER.error("[XPSync] Error syncing: {}", e.getMessage());
        }
    }
}
