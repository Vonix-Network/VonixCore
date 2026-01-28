package network.vonix.vonixcore.util;

public class TimeUtils {

    private TimeUtils() {
    }

    /**
     * Formats a duration in seconds into a human-readable string.
     * e.g., "1d 2h 3m 4s"
     *
     * @param totalSeconds The total duration in seconds.
     * @return A formatted string.
     */
    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder formatted = new StringBuilder();
        if (days > 0) {
            formatted.append(days).append("d ");
        }
        if (hours > 0) {
            formatted.append(hours).append("h ");
        }
        if (minutes > 0) {
            formatted.append(minutes).append("m ");
        }
        if (seconds > 0 || formatted.length() == 0) {
            formatted.append(seconds).append("s");
        }

        return formatted.toString().trim();
    }
}
