package net.hardwarelounge.gallium;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GalliumScheduledTasks {

    private final DiscordBot parent;
    private final ScheduledExecutorService executorService;

    public GalliumScheduledTasks(DiscordBot parent) {
        this.parent = parent;
        executorService = Executors.newScheduledThreadPool(10);
    }

    public void scheduleTasks() {
        executorService.scheduleAtFixedRate(() -> {
            int pardonedPunishments = parent.getPunishmentManager().pardonExpiredPunishments();
            parent.getLogger().debug("Task pardonExpiredPunishments finished and cleaned up {} expired punishments", pardonedPunishments);
        }, 15, 120, TimeUnit.SECONDS);

        executorService.scheduleAtFixedRate(() -> {
            parent.getSecurityListener().clearHistoryMaps();
            parent.getLogger().debug("Cleared 5-minute history maps on spam and blacklist words");
        }, 5, 5, TimeUnit.MINUTES);
    }

}
