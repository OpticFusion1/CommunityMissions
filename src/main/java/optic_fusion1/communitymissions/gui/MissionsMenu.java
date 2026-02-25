package optic_fusion1.communitymissions.gui;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import optic_fusion1.communitymissions.config.ConfigManager;
import optic_fusion1.communitymissions.mission.ActiveMission;
import optic_fusion1.communitymissions.mission.MissionService;
import optic_fusion1.communitymissions.util.FormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MissionsMenu implements Listener {

    private static final String TITLE = "§0Unity Missions";

    private final MissionService missionService;
    private final ConfigManager config;

    public MissionsMenu(MissionService missionService, ConfigManager config) {
        this.missionService = missionService;
        this.config = config;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        AtomicInteger slot = new AtomicInteger(10);
        missionService.getActiveMissions().stream()
                .sorted(Comparator.comparing(active -> active.definition().displayName()))
                .forEach(activeMission -> inventory.setItem(slot.getAndIncrement(), createMissionItem(activeMission)));

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName("§dYour Unity Profile");
        List<String> lore = new ArrayList<>();
        lore.add("§7Lifetime points: §f" + missionService.getPlayerPoints(player.getUniqueId()));
        lore.add("§7Rotation in: §f" + FormatUtil.compactDuration(Math.max(0,
                missionService.getRotationDueEpochSeconds() - Instant.now().getEpochSecond())));
        lore.add("§8Contribute by playing naturally.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        info.setItemMeta(meta);
        inventory.setItem(22, info);

        player.openInventory(inventory);
    }

    private ItemStack createMissionItem(ActiveMission mission) {
        Material icon = mission.definition().requiredMaterial() != null ? mission.definition().requiredMaterial() : Material.WRITABLE_BOOK;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b" + mission.definition().displayName());

        List<String> lore = new ArrayList<>();
        lore.add("§7" + mission.definition().description());
        lore.add(" ");
        lore.add("§7Objective: §f" + mission.definition().objectiveType().name());
        lore.add("§7Progress: §f" + mission.progress() + "§7/§f" + mission.definition().targetAmount());
        lore.add(FormatUtil.progressBar(mission.progress(), mission.definition().targetAmount(), 20));
        lore.add(" ");
        lore.add("§7Milestones:");
        mission.definition().milestones().stream()
                .sorted(Comparator.comparingLong(m -> m.target()))
                .forEach(m -> lore.add((mission.triggeredMilestones().contains(m.target()) ? "§a✔ " : "§7• ") + "§f" + m.target() + " §8(" + m.perk() + ")"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }
}
