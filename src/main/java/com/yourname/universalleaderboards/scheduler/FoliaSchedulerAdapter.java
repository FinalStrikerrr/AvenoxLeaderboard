package com.yourname.universalleaderboards.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Folia-safe scheduler implementation. Only used when CompatibilityManager.isFolia() is true.
 * Never touches org.bukkit.scheduler.BukkitScheduler, which is unsafe under Folia.
 */
public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;
    private final Set<ScheduledTask> activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    @Override
    public void runAtEntity(Entity entity, Runnable task) {
        entity.getScheduler().run(plugin, t -> task.run(), null);
    }

    @Override
    public void runAtLocation(Location location, Runnable task) {
        Bukkit.getRegionScheduler().run(plugin, location, t -> task.run());
    }

    @Override
    public CancellableTask runLater(Runnable task, long delayTicks) {
        ScheduledTask scheduled = Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks));
        activeTasks.add(scheduled);
        return () -> {
            scheduled.cancel();
            activeTasks.remove(scheduled);
        };
    }

    @Override
    public CancellableTask runRepeating(Runnable task, long delayTicks, long periodTicks) {
        ScheduledTask scheduled = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, t -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
        activeTasks.add(scheduled);
        return () -> {
            scheduled.cancel();
            activeTasks.remove(scheduled);
        };
    }

    @Override
    public CancellableTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        long delayMs = delayTicks * 50L;
        long periodMs = Math.max(50L, periodTicks * 50L);
        ScheduledTask scheduled = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin, t -> task.run(), Math.max(1, delayMs), periodMs, TimeUnit.MILLISECONDS);
        activeTasks.add(scheduled);
        return () -> {
            scheduled.cancel();
            activeTasks.remove(scheduled);
        };
    }

    @Override
    public void shutdown() {
        for (ScheduledTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }
}
