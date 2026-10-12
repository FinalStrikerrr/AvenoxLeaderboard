package com.yourname.universalleaderboards.stats;

import com.yourname.universalleaderboards.storage.StorageProvider;
import com.yourname.universalleaderboards.storage.StorageProvider.IncrementDelta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * Backs the three COUNTER-source boards (blocks-mined, blocks-placed, mob-kills). Event
 * listeners call increment() on the main thread (cheap - just a LongAdder bump, no I/O), and
 * a scheduled async task periodically drains the buffer into StorageProvider#incrementEntries,
 * which does a genuine atomic SQL increment rather than a read-then-write, so this is safe
 * even if a flush is still in flight when new increments arrive.
 *
 * A board here means "ALL_TIME" period only - these three boards don't support timed resets
 * on the same schedule as placeholder boards; if that's ever wanted, extend flush() to also
 * track per-period buckets the way TimedLeaderboard does for the placeholder engine.
 */
public class PersistentCounterService {

    private static final String PERIOD_TYPE = "ALL_TIME";
    private static final long PERIOD_START = 0L;

    private final StorageProvider storage;
    private final Logger logger;
    private final Map<String, Map<UUID, PendingDelta>> pending = new ConcurrentHashMap<>();

    public PersistentCounterService(StorageProvider storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public void increment(String boardId, Player player, long amount) {
        if (amount == 0) return;
        Map<UUID, PendingDelta> board = pending.computeIfAbsent(boardId, k -> new ConcurrentHashMap<>());
        board.computeIfAbsent(player.getUniqueId(), u -> new PendingDelta(player.getName(), player.getDisplayName()))
                .adder.add(amount);
    }

    /** Drains all pending deltas across all counter boards into atomic DB increments.
     *  Returns a future that completes once every board's increment write has finished,
     *  so callers that need to block until the flush is durable (namely onDisable, which
     *  must not close the storage pool while a write is still in flight) can join() it. */
    public CompletableFuture<Void> flush() {
        List<CompletableFuture<Void>> writes = new ArrayList<>();
        for (Map.Entry<String, Map<UUID, PendingDelta>> boardEntry : pending.entrySet()) {
            String boardId = boardEntry.getKey();
            Map<UUID, PendingDelta> board = boardEntry.getValue();
            if (board.isEmpty()) continue;

            List<IncrementDelta> deltas = new ArrayList<>();
            List<PendingDelta> drained = new ArrayList<>();
            for (UUID uuid : List.copyOf(board.keySet())) {
                PendingDelta pendingDelta = board.get(uuid);
                if (pendingDelta == null) continue;
                long amount = pendingDelta.adder.sumThenReset();
                if (amount == 0) continue;
                drained.add(pendingDelta);
                deltas.add(new IncrementDelta(uuid, pendingDelta.username, pendingDelta.displayName, amount));
            }

            if (!deltas.isEmpty()) {
                CompletableFuture<Void> write = storage.incrementEntries(boardId, PERIOD_TYPE, PERIOD_START, deltas)
                        .exceptionally(ex -> {
                            // Put failed deltas back so a temporary database failure cannot
                            // permanently erase block/mob progress.
                            for (int i = 0; i < drained.size(); i++) {
                                long amount = (long) deltas.get(i).delta();
                                drained.get(i).adder.add(amount);
                            }
                            logger.warning("[AvenoxLeaderboard] Failed to flush counter deltas for board '" + boardId + "': " + ex.getMessage());
                            return null;
                        });
                writes.add(write);
            }
        }
        return CompletableFuture.allOf(writes.toArray(new CompletableFuture[0]));
    }

    private static final class PendingDelta {
        final String username;
        final String displayName;
        final LongAdder adder = new LongAdder();

        PendingDelta(String username, String displayName) {
            this.username = username;
            this.displayName = displayName;
        }
    }
}
