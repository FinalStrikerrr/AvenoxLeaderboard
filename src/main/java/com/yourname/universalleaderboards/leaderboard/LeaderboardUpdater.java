package com.yourname.universalleaderboards.leaderboard;

import com.yourname.universalleaderboards.placeholder.PlaceholderManager;
import com.yourname.universalleaderboards.storage.StorageProvider;

import java.util.List;
import java.util.logging.Logger;

/**
 * Drives a single leaderboard's refresh cycle:
 *   1. Resolve values for online tracked players (async, rate-limited) - skipped entirely
 *      for COUNTER-source boards, whose values are already being written directly by
 *      PersistentCounterService's atomic increments; refreshing them only means re-reading
 *      the current top-N.
 *   2. Batch-upsert those values into storage (async).
 *   3. Re-query top-N from storage (SQL ORDER BY + LIMIT, never a full table scan)
 *      and atomically swap the leaderboard's in-memory snapshot.
 *
 * This entire sequence must be invoked from an async context; it performs no scheduling
 * itself so callers (LeaderboardManager) control batching/spacing across many boards.
 */
public class LeaderboardUpdater {

    private final PlaceholderManager placeholderManager;
    private final StorageProvider storage;
    private final Logger logger;
    private final boolean debugLogging;

    public LeaderboardUpdater(PlaceholderManager placeholderManager, StorageProvider storage, Logger logger, boolean debugLogging) {
        this.placeholderManager = placeholderManager;
        this.storage = storage;
        this.logger = logger;
        this.debugLogging = debugLogging;
    }

    /** Synchronous body intended to run inside an async scheduler task. */
    public void refresh(Leaderboard board) {
        if (!board.isEnabled()) return;

        try {
            String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());
            long periodStart = TimedLeaderboard.currentPeriodStart(board.getTimedPeriod(), board.getCustomIntervalSeconds());
            board.setPeriodStart(periodStart);

            if (board.getSource() != LeaderboardType.SourceType.COUNTER) {
                List<LeaderboardEntry> resolved = placeholderManager.resolveOnlinePlayers(board);
                if (!resolved.isEmpty()) {
                    storage.upsertEntries(board.getId(), periodType, periodStart, resolved).join();
                }
            }

            List<LeaderboardEntry> top = storage.getTopEntries(
                    board.getId(), periodType, periodStart,
                    board.getOrder() == LeaderboardType.Order.DESC, board.getMaxEntries()
            ).join();

            board.updateSnapshot(top);

            if (debugLogging) {
                logger.info("[AvenoxLeaderboard] Refreshed board '" + board.getId() + "' (" + top.size() + " entries).");
            }
        } catch (Exception ex) {
            logger.warning("[AvenoxLeaderboard] Failed to refresh leaderboard '" + board.getId() + "': " + ex.getMessage());
        }
    }
}
