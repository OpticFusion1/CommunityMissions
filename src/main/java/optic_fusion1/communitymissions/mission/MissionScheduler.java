package optic_fusion1.communitymissions.mission;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import optic_fusion1.communitymissions.CommunityMissions;
import optic_fusion1.communitymissions.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class MissionScheduler {

    private final CommunityMissions plugin;
    private final MissionService missionService;
    private final ConfigManager config;

    private BukkitTask rotationTask;
    private BukkitTask playtimeTask;

    public MissionScheduler(CommunityMissions plugin, MissionService missionService, ConfigManager config) {
        this.plugin = plugin;
        this.missionService = missionService;
        this.config = config;
    }

    public void start() {
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRotation, 20L, 20L * 30L);
        playtimeTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private final Map<UUID, Long> activeSessionSeconds = new HashMap<>();

            @Override
            public void run() {
                long now = Instant.now().getEpochSecond();
                Bukkit.getOnlinePlayers().forEach(player -> {
                    long last = activeSessionSeconds.getOrDefault(player.getUniqueId(), now);
                    long elapsed = now - last;
                    if (elapsed >= 60) {
                        long minutes = elapsed / 60;
                        missionService.contribute(player.getUniqueId(), MissionObjectiveType.PLAYTIME_MINUTES, minutes, "ANY");
                        activeSessionSeconds.put(player.getUniqueId(), now);
                    }
                });
            }
        }, 20L * 60L, 20L * 60L);
    }

    private void tickRotation() {
        long now = Instant.now().getEpochSecond();
        if (now >= missionService.getRotationDueEpochSeconds()) {
            missionService.rotateMissions(true);
        }
    }

    public void stop() {
        if (rotationTask != null) {
            rotationTask.cancel();
        }
        if (playtimeTask != null) {
            playtimeTask.cancel();
        }
    }
}
