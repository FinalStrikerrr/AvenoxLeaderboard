package com.yourname.universalleaderboards.cache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two responsibilities:
 *  1. Caches a player's raw resolved placeholder value per board, so re-opening a GUI or
 *     re-running an update cycle within the TTL window doesn't re-invoke PlaceholderAPI.
 *  2. Tracks "dirty" players per board so the updater only recalculates who actually changed,
 *     avoiding a full-server PAPI sweep every cycle.
 *
 * Leaderboard.getSnapshot() (the actual top-N sorted view shown in GUIs) is stored on the
 * Leaderboard object itself as a copy-on-write list; this class only caches the raw
 * per-player placeholder resolution, which is the expensive/spammy part.
 */
public class LeaderboardCache {

    private final Map<String, Map<UUID, CacheEntry<String>>> placeholderCache = new ConcurrentHashMap<>();
    private final long placeholderTtlMillis;

    public LeaderboardCache(long placeholderTtlMillis) {
        this.placeholderTtlMillis = placeholderTtlMillis;
    }

    public String getCachedValue(String boardId, UUID uuid) {
        Map<UUID, CacheEntry<String>> boardMap = placeholderCache.get(boardId);
        if (boardMap == null) return null;
        CacheEntry<String> entry = boardMap.get(uuid);
        if (entry == null || entry.isExpired(placeholderTtlMillis)) return null;
        return entry.getValue();
    }

    public void cacheValue(String boardId, UUID uuid, String rawValue) {
        placeholderCache.computeIfAbsent(boardId, k -> new ConcurrentHashMap<>())
                .put(uuid, new CacheEntry<>(rawValue));
    }

    public void invalidate(String boardId, UUID uuid) {
        Map<UUID, CacheEntry<String>> boardMap = placeholderCache.get(boardId);
        if (boardMap != null) boardMap.remove(uuid);
    }

    public void invalidateBoard(String boardId) {
        placeholderCache.remove(boardId);
    }

    public void clear() {
        placeholderCache.clear();
    }
}
