package optic_fusion1.communitymissions.placeholder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import optic_fusion1.communitymissions.CommunityMissions;
import optic_fusion1.communitymissions.mission.ActiveMission;
import optic_fusion1.communitymissions.mission.MissionService;
import optic_fusion1.communitymissions.util.FormatUtil;
import org.bukkit.OfflinePlayer;

public class CommunityMissionsPlaceholderExpansion extends PlaceholderExpansion {

    private final CommunityMissions plugin;
    private final MissionService missionService;

    public CommunityMissionsPlaceholderExpansion(CommunityMissions plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
    }

    @Override
    public String getIdentifier() {
        return "communitymissions";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        String key = identifier.toLowerCase(Locale.ROOT);

        if (key.equals("active_count")) {
            return Integer.toString(getOrderedActiveMissions().size());
        }

        if (key.equals("rotation_remaining")) {
            long remaining = missionService.getRotationDueEpochSeconds() - Instant.now().getEpochSecond();
            return FormatUtil.compactDuration(remaining);
        }

        if (key.equals("player_points")) {
            if (player == null) {
                return "0";
            }
            return Long.toString(missionService.getPlayerPoints(player.getUniqueId()));
        }

        if (key.equals("player_active_total")) {
            if (player == null) {
                return "0";
            }
            return Long.toString(getPlayerActiveTotal(player.getUniqueId()));
        }

        if (key.startsWith("active_")) {
            return handleActiveMissionPlaceholder(player, key);
        }

        if (key.startsWith("top_active_")) {
            return handleTopPlaceholder(key, missionService.getTopPlayers(10));
        }

        if (key.startsWith("top_points_")) {
            return handleTopPlaceholder(key, missionService.getTopPlayersByPoints(10));
        }

        return null;
    }

    private String handleActiveMissionPlaceholder(OfflinePlayer player, String key) {
        String[] parts = key.split("_");
        if (parts.length < 3) {
            return null;
        }

        int index;
        try {
            index = Integer.parseInt(parts[1]) - 1;
        } catch (NumberFormatException exception) {
            return null;
        }

        List<ActiveMission> active = getOrderedActiveMissions();
        if (index < 0 || index >= active.size()) {
            return "";
        }

        ActiveMission mission = active.get(index);
        return switch (parts[2]) {
            case "id" ->
                mission.definition().id();
            case "name" ->
                mission.definition().displayName();
            case "progress" ->
                Long.toString(mission.progress());
            case "target" ->
                Long.toString(mission.definition().targetAmount());
            case "percent" ->
                Integer.toString((int) Math.min(100,
                (mission.progress() * 100) / Math.max(1, mission.definition().targetAmount())));
            case "progressbar" ->
                FormatUtil.progressBar(mission.progress(), mission.definition().targetAmount(), 18);
            case "player", "you" -> {
                if (player == null) {
                    yield "0";
                }
                yield Long.toString(missionService.getContributionForMission(mission.definition().id(), player.getUniqueId()));
            }
            default ->
                null;
        };
    }

    private String handleTopPlaceholder(String key, List<Map.Entry<OfflinePlayer, Long>> topList) {
        String[] parts = key.split("_");
        if (parts.length < 4) {
            return null;
        }

        int rank;
        try {
            rank = Integer.parseInt(parts[2]) - 1;
        } catch (NumberFormatException exception) {
            return null;
        }

        if (rank < 0 || rank >= topList.size()) {
            return "";
        }

        Map.Entry<OfflinePlayer, Long> entry = topList.get(rank);
        return switch (parts[3]) {
            case "name" ->
                entry.getKey().getName() == null ? "Unknown" : entry.getKey().getName();
            case "value", "amount" ->
                Long.toString(entry.getValue());
            default ->
                null;
        };
    }

    private long getPlayerActiveTotal(UUID playerId) {
        long total = 0;
        for (ActiveMission mission : missionService.getActiveMissions()) {
            total += missionService.getContributionForMission(mission.definition().id(), playerId);
        }
        return total;
    }

    private List<ActiveMission> getOrderedActiveMissions() {
        List<ActiveMission> sorted = new ArrayList<>(missionService.getActiveMissions());
        sorted.sort(Comparator.comparing(mission -> mission.definition().id()));
        return sorted;
    }
}
