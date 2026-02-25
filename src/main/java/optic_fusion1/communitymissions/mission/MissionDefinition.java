package optic_fusion1.communitymissions.mission;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public record MissionDefinition(String id,
        String displayName,
        String description,
        MissionObjectiveType objectiveType,
        Material requiredMaterial,
        EntityType requiredEntityType,
        long targetAmount,
        List<MissionMilestone> milestones,
        int pointsPerContribution,
        boolean enabledByDefault) {

}
