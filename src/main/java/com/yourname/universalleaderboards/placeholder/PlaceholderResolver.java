package com.yourname.universalleaderboards.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Wraps PlaceholderAPI#setPlaceholders with error throttling so a broken expansion
 * cannot spam the console every cycle. PlaceholderAPI internally handles hopping to the
 * main thread for placeholders that require it; callers here are expected to invoke this
 * from an async context as part of a batch, matching PAPI's own recommended usage pattern.
 */
public class PlaceholderResolver {

    private final boolean placeholderApiEnabled;
    private final Logger logger;
    private final boolean debugLogging;
    private final Map<String, Long> lastErrorLogged = new ConcurrentHashMap<>();
    private static final long ERROR_LOG_THROTTLE_MS = 60_000L;

    public PlaceholderResolver(boolean placeholderApiEnabled, Logger logger, boolean debugLogging) {
        this.placeholderApiEnabled = placeholderApiEnabled;
        this.logger = logger;
        this.debugLogging = debugLogging;
    }

    public String resolve(OfflinePlayer player, String placeholder) {
        if (!placeholderApiEnabled || placeholder == null || placeholder.isEmpty()) return null;
        try {
            return PlaceholderAPI.setPlaceholders(player, placeholder);
        } catch (Exception ex) {
            throttledError("Failed to resolve placeholder '" + placeholder + "' for " +
                    (player.getName() != null ? player.getName() : player.getUniqueId()) + ": " + ex.getMessage());
            return null;
        }
    }

    private void throttledError(String message) {
        long now = System.currentTimeMillis();
        Long last = lastErrorLogged.get(message);
        if (last == null || now - last > ERROR_LOG_THROTTLE_MS || debugLogging) {
            logger.warning("[AvenoxLeaderboard] " + message);
            lastErrorLogged.put(message, now);
        }
    }
}
