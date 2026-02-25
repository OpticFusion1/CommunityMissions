package optic_fusion1.communitymissions;

import optic_fusion1.communitymissions.command.MissionCommand;
import optic_fusion1.communitymissions.config.ConfigManager;
import optic_fusion1.communitymissions.data.DataStore;
import optic_fusion1.communitymissions.gui.MissionsMenu;
import optic_fusion1.communitymissions.listener.MissionProgressListener;
import optic_fusion1.communitymissions.listener.PlayerListener;
import optic_fusion1.communitymissions.mission.MissionScheduler;
import optic_fusion1.communitymissions.mission.MissionService;
import optic_fusion1.communitymissions.perk.PerkService;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CommunityMissions extends JavaPlugin {

    private PluginManager pluginManager = Bukkit.getPluginManager();
    private ConfigManager configManager;
    private DataStore dataStore;
    private PerkService perkService;
    private MissionService missionService;
    private MissionScheduler missionScheduler;
    private MissionsMenu missionMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        dataStore = new DataStore(this);
        dataStore.load();

        perkService = new PerkService(this, dataStore, configManager);
        missionService = new MissionService(this, configManager, dataStore, perkService);
        missionService.loadMissions();

        missionMenu = new MissionsMenu(missionService, configManager);
        missionScheduler = new MissionScheduler(this, missionService, configManager);

        registerListeners();

        MissionCommand command = new MissionCommand(this, missionService, missionMenu, configManager);

        perkService.resumePersistedPerks();
        missionScheduler.start();

        long autosaveTicks = Math.max(20L * 30, configManager.getAutosaveSeconds() * 20L);
        Bukkit.getScheduler().runTaskTimer(this, dataStore::save, autosaveTicks, autosaveTicks);
    }

    @Override
    public void onDisable() {
        if (missionScheduler != null) {
            missionScheduler.stop();
        }
        if (perkService != null) {
            perkService.clearRuntimeTasks();
        }
        if (dataStore != null) {
            dataStore.save();
        }
    }

    private void registerListeners() {
        registerListener(new MissionProgressListener(missionService));
        registerListener(new PlayerListener(perkService));
        registerListener(missionMenu);
    }

    private void registerListener(Listener listener) {
        pluginManager.registerEvents(listener, this);
    }
}
