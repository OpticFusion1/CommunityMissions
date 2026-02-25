package optic_fusion1.communitymissions.listener;

import optic_fusion1.communitymissions.mission.MissionObjectiveType;
import optic_fusion1.communitymissions.mission.MissionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MissionProgressListener implements Listener {

    private final MissionService missionService;

    public MissionProgressListener(MissionService missionService) {
        this.missionService = missionService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        missionService.contribute(event.getPlayer().getUniqueId(), MissionObjectiveType.BLOCK_BREAK, 1,
                event.getBlock().getType().name());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        missionService.contribute(killer.getUniqueId(), MissionObjectiveType.ENTITY_KILL, 1,
                event.getEntityType().name());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            missionService.contribute(event.getPlayer().getUniqueId(), MissionObjectiveType.FISH_CATCH, 1, "ANY");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        missionService.contribute(player.getUniqueId(), MissionObjectiveType.ITEM_CRAFT, 1,
                event.getRecipe().getResult().getType().name());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().distanceSquared(event.getTo()) < 9) {
            return;
        }
        missionService.contribute(event.getPlayer().getUniqueId(), MissionObjectiveType.DISTANCE_WALK, 3, "ANY");
    }
}
