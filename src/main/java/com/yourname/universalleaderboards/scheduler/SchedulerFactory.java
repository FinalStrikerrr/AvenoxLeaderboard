package com.yourname.universalleaderboards.scheduler;

import com.yourname.universalleaderboards.compat.CompatibilityManager;
import org.bukkit.plugin.Plugin;

public final class SchedulerFactory {

    private SchedulerFactory() {}

    public static SchedulerAdapter create(Plugin plugin) {
        if (CompatibilityManager.isFolia()) {
            return new FoliaSchedulerAdapter(plugin);
        }
        return new PaperSchedulerAdapter(plugin);
    }
}
