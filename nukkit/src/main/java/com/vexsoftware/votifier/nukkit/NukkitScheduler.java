package com.vexsoftware.votifier.nukkit;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;

import cn.nukkit.scheduler.Task;

import java.util.concurrent.TimeUnit;

/**
 * @author good777LUCKY
 */
class NukkitScheduler implements VotifierScheduler {

    private final NuVotifier plugin;

    public NukkitScheduler(NuVotifier plugin) {
        this.plugin = plugin;
    }

    private int toTicks(int time, TimeUnit unit) {
        return (int) (unit.toMillis(time) / 50);
    }

    @Override
    public ScheduledVotifierTask sync(Runnable runnable) {
        return new NukkitTaskWrapper(plugin.getServer().getScheduler().scheduleTask(plugin, runnable));
    }

    @Override
    public ScheduledVotifierTask onPool(Runnable runnable) {
        return new NukkitTaskWrapper(plugin.getServer().getScheduler().scheduleTask(plugin, runnable, true));
    }

    @Override
    public ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit) {
        return new NukkitTaskWrapper(plugin.getServer().getScheduler().scheduleDelayedTask(plugin, runnable, toTicks(delay, unit)));
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new NukkitTaskWrapper(plugin.getServer().getScheduler().scheduleDelayedTask(plugin, runnable, toTicks(delay, unit), true));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new NukkitTaskWrapper(plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, runnable, toTicks(delay, unit), toTicks(repeat, unit), true));
    }

    private static class NukkitTaskWrapper implements ScheduledVotifierTask {
        private final Task task;

        private NukkitTaskWrapper(Task task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
