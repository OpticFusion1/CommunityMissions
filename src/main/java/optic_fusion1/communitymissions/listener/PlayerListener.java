package optic_fusion1.communitymissions.listener;

import optic_fusion1.communitymissions.perk.PerkService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final PerkService perkService;

    public PlayerListener(PerkService perkService) {
        this.perkService = perkService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        perkService.handleJoin(event.getPlayer());
    }

}
