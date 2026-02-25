package optic_fusion1.communitymissions.mission;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import optic_fusion1.communitymissions.CommunityMissions;
import optic_fusion1.communitymissions.config.ConfigManager;
import optic_fusion1.communitymissions.data.DataStore;
import optic_fusion1.communitymissions.perk.PerkService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class MissionService {

    private final CommunityMissions plugin;
    private final ConfigManager config;
    private final DataStore dataStore;
    private final PerkService perkService;

    private final Map<String, MissionDefinition> missionDefinitions = new LinkedHashMap<>();
    private final Map<String, ActiveMission> activeMissions = new ConcurrentHashMap<>();

    public MissionService(CommunityMissions plugin, ConfigManager config, DataStore dataStore, PerkService perkService) {
        this.plugin = plugin;
        this.config = config;
        this.dataStore = dataStore;
        this.perkService = perkService;
    }

    public void loadMissions() {
        missionDefinitions.clear();
        for (MissionDefinition definition : config.readMissionDefinitions()) {
            missionDefinitions.put(definition.id(), definition);
        }

        activeMissions.clear();
        Set<String> stored = new LinkedHashSet<>(dataStore.getActiveMissions());
        for (String missionId : stored) {
            MissionDefinition definition = missionDefinitions.get(missionId);
            if (definition == null || !definition.enabledByDefault()) {
                continue;
            }
            ActiveMission active = new ActiveMission(definition,
                    dataStore.missionProgress(missionId),
                    dataStore.triggeredMilestones(missionId));
            activeMissions.put(missionId, active);
        }

        if (activeMissions.isEmpty()) {
            rotateMissions(false);
        }
    }

    public void rotateMissions(boolean announce) {
        List<MissionDefinition> enabled = new ArrayList<>(missionDefinitions.values().stream()
                .filter(MissionDefinition::enabledByDefault)
                .toList());
        if (enabled.isEmpty()) {
            return;
        }

        Collections.shuffle(enabled);
        int targetSize = Math.min(config.getConcurrentMissionCount(), enabled.size());
        activeMissions.clear();
        dataStore.getActiveMissions().clear();

        for (int i = 0; i < targetSize; i++) {
            MissionDefinition def = enabled.get(i);
            ActiveMission active = new ActiveMission(def, dataStore.missionProgress(def.id()), dataStore.triggeredMilestones(def.id()));
            activeMissions.put(def.id(), active);
            dataStore.getActiveMissions().add(def.id());
        }

        dataStore.setLastRotationEpochSeconds(Instant.now().getEpochSecond());
        if (announce) {
            Bukkit.broadcastMessage(config.prefix() + config.message("rotation", "&bNew community missions are now active! Use &f/missions menu&b."));
        }
    }

    public void contribute(UUID playerId, MissionObjectiveType type, long amount, String materialOrEntity) {
        for (ActiveMission active : activeMissions.values()) {
            MissionDefinition def = active.definition();
            if (def.objectiveType() != type) {
                continue;
            }
            if (!matchesConstraint(def, materialOrEntity)) {
                continue;
            }
            addProgress(playerId, active, amount);
        }
    }

    private boolean matchesConstraint(MissionDefinition definition, String materialOrEntity) {
        if (definition.requiredMaterial() != null) {
            return definition.requiredMaterial().name().equalsIgnoreCase(materialOrEntity);
        }
        if (definition.requiredEntityType() != null) {
            return definition.requiredEntityType().name().equalsIgnoreCase(materialOrEntity);
        }
        return true;
    }

    public void addManualContribution(String missionId, UUID playerId, long amount) {
        ActiveMission active = activeMissions.get(missionId);
        if (active == null) {
            return;
        }
        addProgress(playerId, active, amount);
    }

    private void addProgress(UUID playerId, ActiveMission active, long amount) {
        MissionDefinition definition = active.definition();
        active.addProgress(amount);
        dataStore.setMissionProgress(definition.id(), active.progress());

        dataStore.missionContributions(definition.id()).merge(playerId, amount, Long::sum);
        dataStore.addPlayerPoints(playerId, amount * definition.pointsPerContribution());

        evaluateMilestones(active);
    }

    private void evaluateMilestones(ActiveMission active) {
        for (MissionMilestone milestone : active.definition().milestones()) {
            if (active.progress() < milestone.target()) {
                continue;
            }
            if (!active.triggeredMilestones().add(milestone.target())) {
                continue;
            }
            dataStore.triggeredMilestones(active.definition().id()).add(milestone.target());
            if (!milestone.broadcastMessage().isBlank()) {
                Bukkit.broadcastMessage(config.prefix() + milestone.broadcastMessage()
                        .replace("%mission%", active.definition().displayName())
                        .replace("%target%", String.valueOf(milestone.target())));
            }
            perkService.activatePerk(milestone.perk(), milestone.perkDurationSeconds());
            for (String command : milestone.rewardCommands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command
                        .replace("%mission%", active.definition().id())
                        .replace("%progress%", String.valueOf(active.progress())));
            }
        }
    }

    public List<Map.Entry<OfflinePlayer, Long>> getTopPlayers(int limit) {
        Map<UUID, Long> aggregated = new HashMap<>();
        for (String missionId : activeMissions.keySet()) {
            for (Map.Entry<UUID, Long> entry : dataStore.missionContributions(missionId).entrySet()) {
                aggregated.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }
        return aggregated.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> Map.entry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<Map.Entry<OfflinePlayer, Long>> getTopPlayersByPoints(int limit) {
        return dataStore.playerPointsSnapshot().entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> Map.entry(Bukkit.getOfflinePlayer(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    public long getContributionForMission(String missionId, UUID playerId) {
        return dataStore.missionContributions(missionId).getOrDefault(playerId, 0L);
    }

    public long getPlayerPoints(UUID playerId) {
        return dataStore.playerPoints(playerId);
    }

    public Collection<ActiveMission> getActiveMissions() {
        return Collections.unmodifiableCollection(activeMissions.values());
    }

    public Collection<MissionDefinition> getAllMissions() {
        return Collections.unmodifiableCollection(missionDefinitions.values());
    }

    public ActiveMission getActiveMissionById(String id) {
        return activeMissions.get(id);
    }

    public long getRotationDueEpochSeconds() {
        return dataStore.getLastRotationEpochSeconds() + (config.getRotationMinutes() * 60L);
    }

    public void reloadAll() {
        config.reload();
        loadMissions();
    }
}
