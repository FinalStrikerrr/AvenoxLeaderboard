package com.yourname.universalleaderboards.storage;

import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * All methods are asynchronous and must never be called from a main/region thread.
 * Implementations use HikariCP-pooled JDBC connections.
 */
public interface StorageProvider {

    void initialize() throws Exception;

    void close();

    /** Insert or update a single player's value on a board/period. */
    CompletableFuture<Void> upsertEntry(String boardId, String periodType, long periodStart, LeaderboardEntry entry);

    /** Batched upsert for performance; used by the scheduled update cycle. */
    CompletableFuture<Void> upsertEntries(String boardId, String periodType, long periodStart, List<LeaderboardEntry> entries);

    /**
     * Atomically adds `delta` to each named player's stored value, inserting a fresh row at
     * `delta` if none exists yet. Unlike upsertEntries (which overwrites the value outright),
     * this is a genuine SQL-level increment (SQLite: `value = table.value + excluded.value`;
     * MySQL: `value = value + VALUES(value)`) - safe to call concurrently, including from
     * multiple servers sharing one MySQL database, with no read-modify-write race. Used by
     * event-driven counter boards (blocks mined/placed, mob kills) where the caller only
     * knows "how many more" happened, not the running total.
     */
    CompletableFuture<Void> incrementEntries(String boardId, String periodType, long periodStart, List<IncrementDelta> deltas);

    /** One player's pending delta plus the display info needed if this creates a new row. */
    record IncrementDelta(UUID uuid, String username, String displayName, double delta) {}

    /** Top N entries ordered by value, using SQL ORDER BY + LIMIT (never loads full table). */
    CompletableFuture<List<LeaderboardEntry>> getTopEntries(String boardId, String periodType, long periodStart, boolean descending, int limit);

    CompletableFuture<LeaderboardEntry> getEntry(String boardId, String periodType, long periodStart, UUID uuid);

    /** Player's 1-indexed rank using SQL COUNT, without pulling the whole table into memory. */
    CompletableFuture<Integer> getRank(String boardId, String periodType, long periodStart, UUID uuid, boolean descending);

    CompletableFuture<Long> getEntryCount(String boardId, String periodType, long periodStart);

    CompletableFuture<Void> removeEntry(String boardId, String periodType, long periodStart, UUID uuid);

    CompletableFuture<Void> resetBoard(String boardId, String periodType);

    CompletableFuture<Void> resetAll();
}
