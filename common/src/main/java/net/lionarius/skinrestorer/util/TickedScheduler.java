package net.lionarius.skinrestorer.util;

import com.google.common.collect.Queues;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class TickedScheduler implements Runnable {
    private final MinecraftServer server;
    private final Queue<TickTask> queue = Queues.newPriorityBlockingQueue();
    private final Map<Integer, Integer> idMap = new ConcurrentHashMap<>();
    
    public TickedScheduler(MinecraftServer server) {
        this.server = server;
    }
    
    public void schedule(Runnable task, int delay) {
        this.schedule(task, delay, task);
    }
    
    public void schedule(Runnable task, int delay, Object id) {
        var taskId = id.hashCode();
        var serverTick = this.server.getTickCount();
        this.idMap.merge(taskId, serverTick, Integer::max);
        this.queue.add(new TickTask(serverTick, serverTick + delay, taskId, task));
    }
    
    public void cancel(Object id) {
        var taskId = id.hashCode();
        this.idMap.remove(taskId);
    }
    
    @Override
    public void run() {
        TickTask nextTask;
        while ((nextTask = this.queue.peek()) != null) {
            if (nextTask.runOnTick() > this.server.getTickCount())
                break;
            
            var tickTask = this.queue.remove();
            var lastTaskScheduledOnTick = this.idMap.get(tickTask.id());
            
            if (lastTaskScheduledOnTick != null && lastTaskScheduledOnTick <= tickTask.scheduledOnTick()) {
                this.idMap.remove(tickTask.id());
                if (tickTask.task() != null)
                    tickTask.task().run();
            }
        }
    }
    
    private record TickTask(int scheduledOnTick, int runOnTick, int id, Runnable task) implements Comparable<TickTask> {
        @Override
        public int compareTo(@NotNull TickedScheduler.TickTask other) {
            return Integer.compare(this.runOnTick, other.runOnTick);
        }
    }
}
