package com.yourname.universalleaderboards.leaderboard;

import com.yourname.universalleaderboards.scheduler.SchedulerAdapter;
import com.yourname.universalleaderboards.storage.StorageProvider;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LeaderboardManager {

    private final Map<String, Leaderboard> boards = new ConcurrentHashMap<>();
    private final LeaderboardUpdater updater;
    private final SchedulerAdapter scheduler;
    private final StorageProvider storage;
    private final Logger logger;

    private SchedulerAdapter.CancellableTask refreshTask;

    public LeaderboardManager(LeaderboardUpdater updater, SchedulerAdapter scheduler, StorageProvider storage, Logger logger) {
        this.updater = updater;
        this.scheduler = scheduler;
        this.storage = storage;
        this.logger = logger;
    }

    public void registerBoard(Leaderboard board) {
        boards.put(board.getId().toLowerCase(Locale.ROOT), board);
    }

    public void unregisterBoard(String id) {
        boards.remove(id.toLowerCase(Locale.ROOT));
    }

    public Optional<Leaderboard> getBoard(String id) {
        return Optional.ofNullable(boards.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<Leaderboard> getBoards() {
        return boards.values();
    }

    public void clearAll() {
        boards.clear();
    }

    /**
     * Starts a single global async-repeating task (1 tick period check) that walks all
     * registered boards and refreshes any whose individual update-interval has elapsed.
     * This avoids spawning one scheduled task per board.
     */
    public void startScheduler() {
        stopScheduler();
        refreshTask = scheduler.runAsyncRepeating(this::tick, 100L, 100L); // every 5s check
    }

    private void tick() {
        for (Leaderboard board : boards.values()) {
            if (board.isEnabled() && board.isStale()) {
                updater.refresh(board);
            }
        }
    }

    /**
     * Ensures a joining player has a row on every enabled leaderboard, including when the
     * current value is zero. The normal refresh task immediately replaces the seed value with
     * the real statistic/money/placeholder value for sources that provide one.
     */
    public void trackPlayerOnJoin(org.bukkit.entity.Player player) {
        if (player.hasPermission("universalleaderboards.exclude")) return;

        List<java.util.concurrent.CompletableFuture<Void>> writes = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Leaderboard board : boards.values()) {
            if (!board.isEnabled()) continue;
            if (!worldAllowed(board, player)) continue;
            if (board.getPermissionFilter() != null && !board.getPermissionFilter().isEmpty()
                    && !player.hasPermission(board.getPermissionFilter())) continue;

            String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());
            long periodStart = TimedLeaderboard.currentPeriodStart(board.getTimedPeriod(), board.getCustomIntervalSeconds());
            board.setPeriodStart(periodStart);

            LeaderboardEntry entry = new LeaderboardEntry(
                    player.getUniqueId(),
                    player.getName(),
                    player.getDisplayName(),
                    0D,
                    "0",
                    now
            );
            writes.add(storage.upsertEntry(board.getId(), periodType, periodStart, entry));
        }

        if (writes.isEmpty()) return;
        java.util.concurrent.CompletableFuture.allOf(writes.toArray(new java.util.concurrent.CompletableFuture[0]))
                .thenRun(() -> {
                    for (Leaderboard board : boards.values()) {
                        if (board.isEnabled()) forceRefresh(board);
                    }
                });
    }

    private boolean worldAllowed(Leaderboard board, org.bukkit.entity.Player player) {
        List<String> worlds = board.getWorldFilter();
        return worlds == null || worlds.isEmpty() || worlds.contains(player.getWorld().getName());
    }

    public void forceRefresh(Leaderboard board) {
        scheduler.runAsync(() -> updater.refresh(board));
    }

    /** Refreshes a board asynchronously and invokes the callback after the snapshot is updated. */
    public void forceRefresh(Leaderboard board, Runnable onComplete) {
        scheduler.runAsync(() -> {
            updater.refresh(board);
            if (onComplete != null) onComplete.run();
        });
    }

    public void stopScheduler() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void resetBoard(Leaderboard board, Runnable onComplete) {
        String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());
        storage.resetBoard(board.getId(), periodType).thenRun(() -> {
            board.updateSnapshot(List.of());
            if (onComplete != null) onComplete.run();
        });
    }

    public void resetAll(Runnable onComplete) {
        storage.resetAll().thenRun(() -> {
            for (Leaderboard board : boards.values()) {
                board.updateSnapshot(List.of());
            }
            if (onComplete != null) onComplete.run();
        });
    }
}
