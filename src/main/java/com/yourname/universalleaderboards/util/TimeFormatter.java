package com.yourname.universalleaderboards.util;

public final class TimeFormatter {

    private TimeFormatter() {}

    /** Formats a duration given in seconds as "7d 3h 12m". */
    public static String formatSeconds(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        sb.append(hours).append("h ").append(minutes).append("m");
        return sb.toString();
    }

    /** Some PlaceholderAPI expansions report playtime in ticks (20 ticks/sec). */
    public static String formatTicks(long ticks) {
        return formatSeconds(ticks / 20);
    }
}
