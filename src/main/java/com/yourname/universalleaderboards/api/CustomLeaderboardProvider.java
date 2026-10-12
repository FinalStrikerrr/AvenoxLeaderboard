package com.yourname.universalleaderboards.api;

import java.util.UUID;

/**
 * Implement this and register it via UniversalLeaderboardAPI#registerProvider(id, provider)
 * to feed a leaderboard from a value PlaceholderAPI cannot expose directly.
 *
 * Reference the provider from leaderboards.yml with:
 *   leaderboards:
 *     myboard:
 *       provider: "myplugin"
 *       type: NUMBER
 *
 * Implementations should be fast; if a value is expensive to compute, cache it internally.
 * This method may be called from an async thread - do not touch Bukkit API here unless
 * it's documented as thread-safe.
 */
public interface CustomLeaderboardProvider {

    /**
     * Returns the current numeric value for the given player, or null if unavailable
     * (the player will simply be skipped for this update cycle).
     */
    Double getValue(UUID uuid);
}
