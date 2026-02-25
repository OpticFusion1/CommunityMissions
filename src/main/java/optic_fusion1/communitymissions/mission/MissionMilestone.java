package optic_fusion1.communitymissions.mission;

import java.util.List;

public record MissionMilestone(long target,
        MissionPerkType perk,
        int perkDurationSeconds,
        List<String> rewardCommands,
        String broadcastMessage) {

}
