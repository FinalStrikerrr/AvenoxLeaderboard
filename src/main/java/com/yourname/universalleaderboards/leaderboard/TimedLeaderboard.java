package com.yourname.universalleaderboards.leaderboard;

import com.yourname.universalleaderboards.leaderboard.LeaderboardType.TimedPeriod;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Computes the "period_start" bucket used to key timed leaderboard rows in storage.
 * Historical periods are never deleted automatically; only /leaderboard reset removes rows.
 */
public final class TimedLeaderboard {

    private TimedLeaderboard() {}

    public static long currentPeriodStart(TimedPeriod period, int customIntervalSeconds) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return switch (period) {
            case ALL_TIME -> 0L;
            case HOURLY -> now.truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
            case DAILY -> now.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
            case WEEKLY -> now.truncatedTo(ChronoUnit.DAYS)
                    .minusDays(now.getDayOfWeek().getValue() - 1)
                    .toInstant().toEpochMilli();
            case MONTHLY -> now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
            case CUSTOM -> {
                long intervalMs = Math.max(60, customIntervalSeconds) * 1000L;
                long nowMs = Instant.now().toEpochMilli();
                yield nowMs - (nowMs % intervalMs);
            }
        };
    }

    public static String periodTypeKey(TimedPeriod period) {
        return period.name();
    }
}
