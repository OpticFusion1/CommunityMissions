package optic_fusion1.communitymissions.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import optic_fusion1.communitymissions.CommunityMissions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class DataStore {

    private final CommunityMissions plugin;
    private final File file;

    private final Map<String, Long> missionProgress = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> triggeredMilestones = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, Long>> missionContributions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerPoints = new ConcurrentHashMap<>();
    private final Map<String, Long> perkExpirationEpochSeconds = new ConcurrentHashMap<>();
    private final Set<String> activeMissions = Collections.synchronizedSet(new LinkedHashSet<>());
    private long lastRotationEpochSeconds;

    public DataStore(CommunityMissions plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        lastRotationEpochSeconds = yaml.getLong("rotation.last-epoch", 0);

        ConfigurationSection active = yaml.getConfigurationSection("rotation.active-missions");
        if (active != null) {
            activeMissions.clear();
            activeMissions.addAll(active.getKeys(false));
        }

        ConfigurationSection missionSection = yaml.getConfigurationSection("missions");
        if (missionSection != null) {
            for (String id : missionSection.getKeys(false)) {
                ConfigurationSection section = missionSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                missionProgress.put(id, section.getLong("progress", 0));
                Set<Long> milestones = new HashSet<>();
                for (String raw : section.getStringList("milestones-triggered")) {
                    try {
                        milestones.add(Long.valueOf(raw));
                    } catch (NumberFormatException ignored) {
                    }
                }
                triggeredMilestones.put(id, milestones);

                ConfigurationSection contrib = section.getConfigurationSection("contributions");
                if (contrib != null) {
                    Map<UUID, Long> map = new HashMap<>();
                    for (String key : contrib.getKeys(false)) {
                        try {
                            map.put(UUID.fromString(key), contrib.getLong(key));
                        } catch (Exception ignored) {
                        }
                    }
                    missionContributions.put(id, new ConcurrentHashMap<>(map));
                }
            }
        }

        ConfigurationSection points = yaml.getConfigurationSection("players.points");
        if (points != null) {
            for (String key : points.getKeys(false)) {
                try {
                    playerPoints.put(UUID.fromString(key), points.getLong(key));
                } catch (Exception ignored) {
                }
            }
        }

        ConfigurationSection perks = yaml.getConfigurationSection("perks.expiration");
        if (perks != null) {
            for (String perk : perks.getKeys(false)) {
                perkExpirationEpochSeconds.put(perk, perks.getLong(perk));
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("rotation.last-epoch", lastRotationEpochSeconds);
        for (String active : activeMissions) {
            yaml.set("rotation.active-missions." + active, true);
        }

        for (Map.Entry<String, Long> entry : missionProgress.entrySet()) {
            String id = entry.getKey();
            yaml.set("missions." + id + ".progress", entry.getValue());
            Set<Long> milestones = triggeredMilestones.getOrDefault(id, Set.of());
            List<String> out = new ArrayList<>(milestones.size());
            for (Long value : milestones) {
                out.add(String.valueOf(value));
            }
            yaml.set("missions." + id + ".milestones-triggered", out);

            Map<UUID, Long> contributions = missionContributions.getOrDefault(id, Map.of());
            for (Map.Entry<UUID, Long> c : contributions.entrySet()) {
                yaml.set("missions." + id + ".contributions." + c.getKey(), c.getValue());
            }
        }

        for (Map.Entry<UUID, Long> entry : playerPoints.entrySet()) {
            yaml.set("players.points." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Long> entry : perkExpirationEpochSeconds.entrySet()) {
            yaml.set("perks.expiration." + entry.getKey(), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data.yml: {0}", e.getMessage());
        }
    }

    public long missionProgress(String missionId) {
        return missionProgress.getOrDefault(missionId, 0L);
    }

    public void setMissionProgress(String missionId, long value) {
        missionProgress.put(missionId, value);
    }

    public Set<Long> triggeredMilestones(String missionId) {
        return triggeredMilestones.computeIfAbsent(missionId, k -> Collections.synchronizedSet(new HashSet<>()));
    }

    public Map<UUID, Long> missionContributions(String missionId) {
        return missionContributions.computeIfAbsent(missionId, k -> new ConcurrentHashMap<>());
    }

    public long playerPoints(UUID uuid) {
        return playerPoints.getOrDefault(uuid, 0L);
    }

    public void addPlayerPoints(UUID uuid, long points) {
        playerPoints.merge(uuid, points, Long::sum);
    }

    public Map<UUID, Long> playerPointsSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(playerPoints));
    }

    public void setPerkExpiration(String perk, long epochSeconds) {
        perkExpirationEpochSeconds.put(perk, epochSeconds);
    }

    public void removePerkExpiration(String perk) {
        perkExpirationEpochSeconds.remove(perk);
    }

    public Map<String, Long> perkExpirations() {
        return Collections.unmodifiableMap(perkExpirationEpochSeconds);
    }

    public Set<String> getActiveMissions() {
        return activeMissions;
    }

    public long getLastRotationEpochSeconds() {
        return lastRotationEpochSeconds;
    }

    public void setLastRotationEpochSeconds(long epoch) {
        this.lastRotationEpochSeconds = epoch;
    }
}
