package com.yourname.universalleaderboards.compat;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.OfflinePlayer;

/**
 * Single place to isolate behavior that differs between 1.19.x - 1.21.x.
 * If a new Paper API breaks compatibility on a future version, patch it here only.
 */
public final class VersionAdapter {

    private static final String SERVER_VERSION = Bukkit.getBukkitVersion();

    private VersionAdapter() {}

    public static String serverVersion() {
        return SERVER_VERSION;
    }

    /**
     * Statistic.PLAY_ONE_MINUTE was the playtime statistic historically; some
     * PlaceholderAPI expansions and future Paper versions may rename this.
     * Centralizing the lookup means only this method needs updating.
     */
    public static long getPlaytimeTicks(OfflinePlayer player) {
        try {
            return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        } catch (IllegalArgumentException ex) {
            // Fallback for potential future renames - avoid hard crash.
            return 0L;
        }
    }

    /** Whether the running core exposes Folia's threaded-regions API. */
    public static boolean supportsRegionizedThreading() {
        return CompatibilityManager.isFolia();
    }
}
