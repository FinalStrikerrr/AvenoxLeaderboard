package com.yourname.universalleaderboards.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PaperSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;
    private final Set<BukkitTask> activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PaperSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runGlobal(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAtEntity(Entity entity, Runnable task) {
        // On non-Folia servers there is only one main thread; still route through it.
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAtLocation(Location location, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public CancellableTask runLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
        activeTasks.add(bukkitTask);
        return () -> {
            bukkitTask.cancel();
            activeTasks.remove(bukkitTask);
        };
    }

    @Override
    public CancellableTask runRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        activeTasks.add(bukkitTask);
        return () -> {
            bukkitTask.cancel();
            activeTasks.remove(bukkitTask);
        };
    }

    @Override
    public CancellableTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        activeTasks.add(bukkitTask);
        return () -> {
            bukkitTask.cancel();
            activeTasks.remove(bukkitTask);
        };
    }

    @Override
    public void shutdown() {
        for (BukkitTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }
}
