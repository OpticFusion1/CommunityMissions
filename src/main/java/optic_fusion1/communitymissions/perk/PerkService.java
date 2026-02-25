package optic_fusion1.communitymissions.perk;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import optic_fusion1.communitymissions.CommunityMissions;
import optic_fusion1.communitymissions.config.ConfigManager;
import optic_fusion1.communitymissions.data.DataStore;
import optic_fusion1.communitymissions.mission.MissionPerkType;
import static optic_fusion1.communitymissions.mission.MissionPerkType.HASTE;
import static optic_fusion1.communitymissions.mission.MissionPerkType.HERO_OF_THE_VILLAGE;
import static optic_fusion1.communitymissions.mission.MissionPerkType.NIGHT_VISION;
import static optic_fusion1.communitymissions.mission.MissionPerkType.SPEED;

public class PerkService {

    private CommunityMissions plugin;
    private DataStore dataStore;
    private ConfigManager config;
    private Map<MissionPerkType, Long> runtimeExpirations = new EnumMap<>(MissionPerkType.class);
    private Map<MissionPerkType, BukkitTask> expirationTasks = new EnumMap<>(MissionPerkType.class);

    public PerkService(CommunityMissions plugin, DataStore dataStore, ConfigManager config) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.config = config;
    }

    public void activatePerk(MissionPerkType type, int durationSeconds) {
        long expiresAt = Instant.now().getEpochSecond() + Math.max(5, durationSeconds);
        runtimeExpirations.put(type, expiresAt);
        dataStore.setPerkExpiration(type.name(), expiresAt);

        applyToOnlinePlayers(type);
        scheduleExpiration(type, expiresAt);
        Bukkit.broadcastMessage(config.prefix() + config.message("perk-unlocked", "&aGlobal perk activated: &f%perk% &afor &f%duration%s&a!")
                .replace("%perk%", prettify(type.name()))
                .replace("%duration%", String.valueOf(durationSeconds)));
    }

    public void resumePersistedPerks() {
        long now = Instant.now().getEpochSecond();
        for (Map.Entry<String, Long> entry : dataStore.perkExpirations().entrySet()) {
            MissionPerkType type;
            try {
                type = MissionPerkType.valueOf(entry.getKey());
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            long expiresAt = entry.getValue();
            if (expiresAt <= now) {
                dataStore.removePerkExpiration(entry.getKey());
                continue;
            }
            runtimeExpirations.put(type, expiresAt);
            applyToOnlinePlayers(type);
            scheduleExpiration(type, expiresAt);
        }
    }

    public void handleJoin(Player player) {
        long now = Instant.now().getEpochSecond();
        for (Map.Entry<MissionPerkType, Long> entry : runtimeExpirations.entrySet()) {
            if (entry.getValue() > now) {
                apply(player, entry.getKey(), (int) (entry.getValue() - now));
            }
        }
    }

    private void applyToOnlinePlayers(MissionPerkType type) {
        long seconds = Math.max(1, runtimeExpirations.getOrDefault(type, Instant.now().getEpochSecond()) - Instant.now().getEpochSecond());
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player, type, (int) seconds);
        }
    }

    private void apply(Player player, MissionPerkType type, int durationSeconds) {
        int ticks = Math.max(20, durationSeconds * 20);
        PotionEffect effect = switch (type) {
            case SPEED ->
                new PotionEffect(PotionEffectType.SPEED, ticks, 0, true, false, true);
            case NIGHT_VISION ->
                new PotionEffect(PotionEffectType.NIGHT_VISION, ticks, 0, true, false, true);
            case HASTE ->
                new PotionEffect(PotionEffectType.HASTE, ticks, 0, true, false, true);
            case HERO_OF_THE_VILLAGE ->
                new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, ticks, 0, true, false, true);
        };
        player.addPotionEffect(effect, true);
    }

    private void scheduleExpiration(MissionPerkType type, long epochSeconds) {
        BukkitTask existing = expirationTasks.get(type);
        if (existing != null) {
            existing.cancel();
        }
        long delayTicks = Math.max(1, (epochSeconds - Instant.now().getEpochSecond()) * 20L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> expirePerk(type), delayTicks);
        expirationTasks.put(type, task);
    }

    private void expirePerk(MissionPerkType type) {
        runtimeExpirations.remove(type);
        dataStore.removePerkExpiration(type.name());
        expirationTasks.remove(type);
        Bukkit.broadcastMessage(config.prefix() + config.message("perk-expired", "&cGlobal perk expired: &f%perk%")
                .replace("%perk%", prettify(type.name())));
    }

    public void clearRuntimeTasks() {
        for (BukkitTask task : expirationTasks.values()) {
            task.cancel();
        }
        expirationTasks.clear();
    }

    private String prettify(String input) {
        String lower = input.toLowerCase().replace('_', ' ');
        StringBuilder out = new StringBuilder();
        for (String part : lower.split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return out.toString().trim();
    }
}
