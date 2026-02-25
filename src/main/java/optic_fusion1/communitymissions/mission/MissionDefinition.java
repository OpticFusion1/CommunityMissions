package optic_fusion1.communitymissions.mission;

import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public record MissionDefinition(String id,
        String displayName,
        String description,
        MissionObjectiveType objectiveType,
        Material requiredMaterial,
        EntityType requiredEntityType,
        Set<String> allowedWorlds,
        long targetAmount,
        List<MissionMilestone> milestones,
        int pointsPerContribution,
        boolean enabledByDefault) {

}
