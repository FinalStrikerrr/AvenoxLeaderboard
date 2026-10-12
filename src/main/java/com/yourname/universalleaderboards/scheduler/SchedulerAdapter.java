package com.yourname.universalleaderboards.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Abstraction over Bukkit's scheduler and Folia's region/entity/global/async schedulers.
 * Every part of the plugin MUST go through this interface instead of calling
 * Bukkit.getScheduler() directly, so that Folia support is never accidentally broken.
 *
 * Rules enforced by design:
 *  - Database I/O, PlaceholderAPI batch resolution and file I/O -> runAsync().
 *  - Work that touches global plugin state but not a specific world region -> runGlobal().
 *  - Work that touches a specific entity (e.g. sending GUI packets) -> runAtEntity().
 *  - Work that touches a location/chunk -> runAtLocation().
 */
public interface SchedulerAdapter {

    void runAsync(Runnable task);

    void runGlobal(Runnable task);

    void runAtEntity(Entity entity, Runnable task);

    void runAtLocation(Location location, Runnable task);

    /** Delay in server ticks (20 ticks = 1 second). Runs on the global/main context. */
    CancellableTask runLater(Runnable task, long delayTicks);

    /** Repeating task on the global/main context. */
    CancellableTask runRepeating(Runnable task, long delayTicks, long periodTicks);

    /** Repeating task off the main thread(s) entirely - safe for DB/PAPI batch work. */
    CancellableTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);

    void shutdown();

    interface CancellableTask {
        void cancel();
    }
}
