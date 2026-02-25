package optic_fusion1.communitymissions.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import optic_fusion1.communitymissions.CommunityMissions;
import optic_fusion1.communitymissions.mission.MissionDefinition;
import optic_fusion1.communitymissions.mission.MissionMilestone;
import optic_fusion1.communitymissions.mission.MissionObjectiveType;
import optic_fusion1.communitymissions.mission.MissionPerkType;

public class ConfigManager {

    private final CommunityMissions plugin;

    public ConfigManager(CommunityMissions plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public int getRotationMinutes() {
        return Math.max(15, plugin.getConfig().getInt("rotation.interval-minutes", 180));
    }

    public int getAutosaveSeconds() {
        return Math.max(30, plugin.getConfig().getInt("storage.autosave-seconds", 60));
    }

    public int getConcurrentMissionCount() {
        return Math.max(1, plugin.getConfig().getInt("rotation.concurrent-missions", 2));
    }

    public ZoneId getZoneId() {
        String id = plugin.getConfig().getString("rotation.timezone", "UTC");
        try {
            return ZoneId.of(id);
        } catch (Exception ignored) {
            return ZoneId.of("UTC");
        }
    }

    public String prefix() {
        return color(plugin.getConfig().getString("messages.prefix", "&8[&dCommunityMissions&8]&r "));
    }

    public String color(String value) {
        return value == null ? "" : value.replace('&', '§');
    }

    public List<String> getStringList(String path) {
        List<String> source = plugin.getConfig().getStringList(path);
        List<String> colored = new ArrayList<>(source.size());
        for (String line : source) {
            colored.add(color(line));
        }
        return colored;
    }

    public String message(String key, String def) {
        return color(plugin.getConfig().getString("messages." + key, def));
    }

    public List<MissionDefinition> readMissionDefinitions() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("missions");
        if (section == null) {
            return Collections.emptyList();
        }
        List<MissionDefinition> out = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection m = section.getConfigurationSection(id);
            if (m == null) {
                continue;
            }

            MissionObjectiveType type;
            try {
                type = MissionObjectiveType.valueOf(m.getString("type", "BLOCK_BREAK").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().log(Level.WARNING, "Unknown mission type for {0}. Skipping.", id);
                continue;
            }

            Material material = null;
            String matRaw = m.getString("material", "");
            if (!matRaw.isBlank()) {
                material = Material.matchMaterial(matRaw.toUpperCase(Locale.ROOT));
            }

            EntityType entityType = null;
            String entityRaw = m.getString("entity", "");
            if (!entityRaw.isBlank()) {
                try {
                    entityType = EntityType.valueOf(entityRaw.toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {
                    plugin.getLogger().log(Level.WARNING, "Invalid entity type for mission {0}.", id);
                }
            }

            long target = Math.max(1, m.getLong("target", 100));
            int points = Math.max(1, m.getInt("points-per-contribution", 1));
            String displayName = color(m.getString("display-name", id));
            String description = color(m.getString("description", ""));
            boolean enabled = m.getBoolean("enabled", true);

            List<MissionMilestone> milestones = new ArrayList<>();
            for (String line : m.getStringList("milestones")) {
                // format: target|perk|durationSeconds|broadcast|command1;;command2
                String[] split = line.split("\\|", 5);
                if (split.length < 4) {
                    continue;
                }
                long milestoneTarget;
                int duration;
                MissionPerkType perk;
                try {
                    milestoneTarget = Long.parseLong(split[0]);
                    perk = MissionPerkType.valueOf(split[1].toUpperCase(Locale.ROOT));
                    duration = Integer.parseInt(split[2]);
                } catch (Exception ignored) {
                    plugin.getLogger().log(Level.WARNING, "Invalid milestone in mission {0}: {1}", new Object[]{id, line});
                    continue;
                }
                String broadcast = color(split[3]);
                List<String> commands = new ArrayList<>();
                if (split.length == 5 && !split[4].isBlank()) {
                    for (String cmd : split[4].split(";;")) {
                        if (!cmd.isBlank()) {
                            commands.add(cmd.trim());
                        }
                    }
                }
                milestones.add(new MissionMilestone(milestoneTarget, perk, duration, commands, broadcast));
            }

            out.add(new MissionDefinition(id, displayName, description, type, material, entityType, target, milestones, points, enabled));
        }
        return out;
    }
}
