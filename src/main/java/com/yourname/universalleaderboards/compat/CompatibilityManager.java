package com.yourname.universalleaderboards.compat;

/**
 * Detects the running server platform once at startup. Nothing else in the plugin
 * should re-implement this detection logic; every consumer asks this class.
 */
public final class CompatibilityManager {

    private static boolean folia;
    private static boolean detected = false;

    private CompatibilityManager() {}

    public static void detect() {
        if (detected) return;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        detected = true;
    }

    public static boolean isFolia() {
        if (!detected) detect();
        return folia;
    }
}
