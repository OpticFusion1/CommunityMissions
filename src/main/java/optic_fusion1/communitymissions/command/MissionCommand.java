package optic_fusion1.communitymissions.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import optic_fusion1.communitymissions.CommunityMissions;
import optic_fusion1.communitymissions.config.ConfigManager;
import optic_fusion1.communitymissions.gui.MissionsMenu;
import optic_fusion1.communitymissions.mission.ActiveMission;
import optic_fusion1.communitymissions.mission.MissionService;

public class MissionCommand implements CommandExecutor, TabCompleter {

    private final CommunityMissions plugin;
    private final MissionService missionService;
    private final MissionsMenu menu;
    private final ConfigManager config;

    public MissionCommand(CommunityMissions plugin, MissionService missionService, MissionsMenu menu, ConfigManager config) {
        this.plugin = plugin;
        this.missionService = missionService;
        this.menu = menu;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "missions", "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(config.prefix() + "§cOnly players can open the mission menu.");
                    return true;
                }
                menu.open(player);
            }
            case "top" ->
                sendTop(sender, args);
            case "stats" ->
                sendStats(sender, args);
            case "active" ->
                sendActive(sender);
            case "next" ->
                sendRotationTimer(sender);
            case "reload" -> {
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(config.prefix() + "§cYou do not have permission.");
                    return true;
                }
                missionService.reloadAll();
                sender.sendMessage(config.prefix() + "§aCommunityMissions reloaded.");
            }
            case "rotate" -> {
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(config.prefix() + "§cYou do not have permission.");
                    return true;
                }
                missionService.rotateMissions(true);
                sender.sendMessage(config.prefix() + "§aRotation completed.");
            }
            case "contribute" -> {
                if (!hasAdminPermission(sender)) {
                    sender.sendMessage(config.prefix() + "§cYou do not have permission.");
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(config.prefix() + "§cUsage: /" + label + " contribute <missionId> <player> <amount>");
                    return true;
                }
                ActiveMission mission = missionService.getActiveMissionById(args[1]);
                if (mission == null) {
                    sender.sendMessage(config.prefix() + "§cMission is not active or doesn't exist.");
                    return true;
                }
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
                if (player.getUniqueId() == null) {
                    sender.sendMessage(config.prefix() + "§cPlayer not found.");
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(config.prefix() + "§cAmount must be a number.");
                    return true;
                }
                UUID uuid = player.getUniqueId();
                missionService.addManualContribution(mission.definition().id(), uuid, amount);
                sender.sendMessage(config.prefix() + "§aAdded " + amount + " progress to " + mission.definition().displayName() + " for " + player.getName());
            }
            default ->
                sendHelp(sender, label);
        }
        return true;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.hasPermission("communitymissions.admin");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(config.prefix() + "§bCommunityMissions commands:");
        sender.sendMessage("§7/" + label + " missions §8- §fOpen mission menu");
        sender.sendMessage("§7/" + label + " active §8- §fList active mission progress");
        sender.sendMessage("§7/" + label + " next §8- §fShow next mission rotation timer");
        sender.sendMessage("§7/" + label + " stats [player] §8- §fShow mission contribution stats");
        sender.sendMessage("§7/" + label + " top [active|points] §8- §fLeaderboard views");
        if (hasAdminPermission(sender)) {
            sender.sendMessage("§7/" + label + " rotate §8- §fForce mission rotation");
            sender.sendMessage("§7/" + label + " reload §8- §fReload config");
            sender.sendMessage("§7/" + label + " contribute <mission> <player> <amount> §8- §fAdd contribution");
        }
    }

    private void sendTop(CommandSender sender, String[] args) {
        String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "active";
        if (mode.equals("points")) {
            sender.sendMessage(config.prefix() + "§bTop Contributors (lifetime points):");
            int i = 1;
            for (Map.Entry<OfflinePlayer, Long> entry : missionService.getTopPlayersByPoints(10)) {
                sender.sendMessage("§7" + i++ + ". §f" + entry.getKey().getName() + " §8- §e" + entry.getValue() + " pts");
            }
            return;
        }

        sender.sendMessage(config.prefix() + "§bTop Contributors (active missions):");
        int i = 1;
        for (Map.Entry<OfflinePlayer, Long> entry : missionService.getTopPlayers(10)) {
            sender.sendMessage("§7" + i++ + ". §f" + entry.getKey().getName() + " §8- §b" + entry.getValue());
        }
    }

    private void sendStats(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(config.prefix() + "§cUsage: /missions stats <player>");
            return;
        }

        UUID uuid = target.getUniqueId();
        if (uuid == null) {
            sender.sendMessage(config.prefix() + "§cPlayer not found.");
            return;
        }

        sender.sendMessage(config.prefix() + "§bStats for §f" + target.getName() + "§b:");
        sender.sendMessage("§7Lifetime points: §e" + missionService.getPlayerPoints(uuid));

        long totalActiveProgress = 0;
        for (ActiveMission mission : missionService.getActiveMissions()) {
            long contribution = missionService.getContributionForMission(mission.definition().id(), uuid);
            if (contribution > 0) {
                totalActiveProgress += contribution;
                sender.sendMessage("§8- §f" + mission.definition().displayName() + "§8: §b" + contribution + " progress");
            }
        }
        if (totalActiveProgress == 0) {
            sender.sendMessage("§8- §7No active mission contributions yet.");
        } else {
            sender.sendMessage("§7Total active mission contribution: §b" + totalActiveProgress);
        }
    }

    private void sendActive(CommandSender sender) {
        sender.sendMessage(config.prefix() + "§bActive mission progress:");
        for (ActiveMission mission : missionService.getActiveMissions()) {
            long progress = mission.progress();
            long target = mission.definition().targetAmount();
            int percent = (int) Math.min(100, (progress * 100) / Math.max(1, target));
            sender.sendMessage("§8- §f" + mission.definition().displayName() + " §7(" + progress + "/" + target + ", " + percent + "%)");
        }
    }

    private void sendRotationTimer(CommandSender sender) {
        long due = missionService.getRotationDueEpochSeconds();
        long remainingSeconds = due - Instant.now().getEpochSecond();
        if (remainingSeconds <= 0) {
            sender.sendMessage(config.prefix() + "§aA new rotation is due now.");
            return;
        }

        Duration duration = Duration.ofSeconds(remainingSeconds);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
        sender.sendMessage(config.prefix() + "§bNext rotation in §f" + hours + "h " + minutes + "m " + seconds + "s§b.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], Arrays.asList("missions", "menu", "top", "help", "reload", "rotate", "contribute", "stats", "active", "next"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return partial(args[1], Arrays.asList("active", "points"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("contribute")) {
            List<String> ids = new ArrayList<>();
            for (ActiveMission mission : missionService.getActiveMissions()) {
                ids.add(mission.definition().id());
            }
            return partial(args[1], ids);
        }
        return List.of();
    }

    private List<String> partial(String input, List<String> source) {
        String lower = input.toLowerCase(Locale.ROOT);
        return source.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
