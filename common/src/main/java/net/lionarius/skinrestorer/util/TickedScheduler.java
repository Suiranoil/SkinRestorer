package net.lionarius.skinrestorer.util;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class TickedScheduler implements Runnable {
    private final MinecraftServer server;
    private final Queue<TickTask> queue = new PriorityBlockingQueue<>();
    private final Map<Object, Integer> idMap = new ConcurrentHashMap<>();

    public TickedScheduler(MinecraftServer server) {
        this.server = server;
    }

    public void schedule(Runnable task, int delay) {
        this.schedule(task, delay, task);
    }

    public void schedule(Runnable task, int delay, Object id) {
        var serverTick = this.server.getTickCount();
        this.idMap.merge(id, serverTick, Integer::max);
        this.queue.add(new TickTask(serverTick, serverTick + delay, id, task));
    }

    public void cancel(Object id) {
        this.idMap.remove(id);
    }

    @Override
    public void run() {
        TickTask nextTask;
        while ((nextTask = this.queue.peek()) != null) {
            if (nextTask.runOnTick() > this.server.getTickCount()) break;

            var tickTask = this.queue.remove();
            var lastTaskScheduledOnTick = this.idMap.get(tickTask.id());

            if (lastTaskScheduledOnTick != null && lastTaskScheduledOnTick <= tickTask.scheduledOnTick()) {
                this.idMap.remove(tickTask.id());
                if (tickTask.task() != null) tickTask.task().run();
            }
        }
    }

    private record TickTask(int scheduledOnTick, int runOnTick, Object id, Runnable task)
            implements Comparable<TickTask> {
        @Override
        public int compareTo(@NotNull TickedScheduler.TickTask other) {
            return Integer.compare(this.runOnTick, other.runOnTick);
        }
    }
}
