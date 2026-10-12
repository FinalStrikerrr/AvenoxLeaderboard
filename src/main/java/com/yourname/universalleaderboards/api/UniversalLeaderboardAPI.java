package com.yourname.universalleaderboards.api;

import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;
import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import com.yourname.universalleaderboards.leaderboard.LeaderboardSorter;
import com.yourname.universalleaderboards.leaderboard.TimedLeaderboard;
import com.yourname.universalleaderboards.placeholder.PlaceholderManager;
import com.yourname.universalleaderboards.storage.StorageProvider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API entry point. Other plugins should fetch this via:
 *
 *   UniversalLeaderboardAPI api = UniversalLeaderboardAPI.get();
 *
 * once AvenoxLeaderboard has finished enabling (soft-depend on it in plugin.yml).
 */
public final class UniversalLeaderboardAPI {

    private static UniversalLeaderboardAPI instance;

    private final LeaderboardManager leaderboardManager;
    private final StorageProvider storage;
    private final PlaceholderManager placeholderManager;

    public UniversalLeaderboardAPI(LeaderboardManager leaderboardManager, StorageProvider storage, PlaceholderManager placeholderManager) {
        this.leaderboardManager = leaderboardManager;
        this.storage = storage;
        this.placeholderManager = placeholderManager;
        instance = this;
    }

    public static UniversalLeaderboardAPI get() {
        if (instance == null) {
            throw new IllegalStateException("AvenoxLeaderboard has not finished enabling yet.");
        }
        return instance;
    }

    public Optional<Leaderboard> getLeaderboard(String id) {
        return leaderboardManager.getBoard(id);
    }

    public List<LeaderboardEntry> getTopEntries(String id, int amount) {
        return getLeaderboard(id)
                .map(board -> {
                    List<LeaderboardEntry> snapshot = board.getSnapshot();
                    return snapshot.subList(0, Math.min(amount, snapshot.size()));
                })
                .orElse(List.of());
    }

    public CompletableFuture<LeaderboardEntry> getPlayerEntry(String id, UUID uuid) {
        return getLeaderboard(id)
                .map(board -> storage.getEntry(
                        board.getId(),
                        TimedLeaderboard.periodTypeKey(board.getTimedPeriod()),
                        board.getPeriodStart(),
                        uuid))
                .orElse(CompletableFuture.completedFuture(null));
    }

    public CompletableFuture<Integer> getPlayerPosition(String id, UUID uuid) {
        return getLeaderboard(id)
                .map(board -> storage.getRank(
                        board.getId(),
                        TimedLeaderboard.periodTypeKey(board.getTimedPeriod()),
                        board.getPeriodStart(),
                        uuid,
                        board.getOrder().name().equals("DESC")))
                .orElse(CompletableFuture.completedFuture(-1));
    }

    public CompletableFuture<Double> getPlayerValue(String id, UUID uuid) {
        return getPlayerEntry(id, uuid).thenApply(entry -> entry == null ? null : entry.getValue());
    }

    public void refreshLeaderboard(String id) {
        getLeaderboard(id).ifPresent(leaderboardManager::forceRefresh);
    }

    public void resetLeaderboard(String id) {
        getLeaderboard(id).ifPresent(board -> leaderboardManager.resetBoard(board, null));
    }

    /** Register a provider for boards configured with `provider: <id>` instead of a placeholder. */
    public void registerProvider(String id, CustomLeaderboardProvider provider) {
        placeholderManager.registerProvider(id, provider);
    }

    public void unregisterProvider(String id) {
        placeholderManager.unregisterProvider(id);
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}
