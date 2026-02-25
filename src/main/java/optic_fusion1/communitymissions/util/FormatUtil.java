package optic_fusion1.communitymissions.util;

import java.time.Duration;

public final class FormatUtil {

    private FormatUtil() {
    }

    public static String progressBar(long progress, long target, int width) {
        if (target <= 0) {
            target = 1;
        }
        double ratio = Math.max(0, Math.min(1, (double) progress / target));
        int filled = (int) Math.round(ratio * width);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? "§a|" : "§7|");
        }
        return sb.append("§8]").toString();
    }

    public static String compactDuration(long seconds) {
        Duration d = Duration.ofSeconds(Math.max(0, seconds));
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long secs = d.toSecondsPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
