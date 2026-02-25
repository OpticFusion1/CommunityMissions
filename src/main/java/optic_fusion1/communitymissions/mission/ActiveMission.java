package optic_fusion1.communitymissions.mission;

import java.util.HashSet;
import java.util.Set;

public class ActiveMission {

    private final MissionDefinition definition;
    private long progress;
    private final Set<Long> triggeredMilestones;

    public ActiveMission(MissionDefinition definition) {
        this(definition, 0, new HashSet<>());
    }

    public ActiveMission(MissionDefinition definition, long progress, Set<Long> triggeredMilestones) {
        this.definition = definition;
        this.progress = progress;
        this.triggeredMilestones = new HashSet<>(triggeredMilestones);
    }

    public MissionDefinition definition() {
        return definition;
    }

    public long progress() {
        return progress;
    }

    public void addProgress(long amount) {
        this.progress = Math.max(0, this.progress + amount);
    }

    public void setProgress(long progress) {
        this.progress = Math.max(0, progress);
    }

    public Set<Long> triggeredMilestones() {
        return triggeredMilestones;
    }

    public boolean isComplete() {
        return progress >= definition.targetAmount();
    }
}
