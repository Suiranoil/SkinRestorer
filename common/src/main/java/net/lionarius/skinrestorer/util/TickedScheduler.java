package net.lionarius.skinrestorer.util;

import com.google.common.collect.Queues;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;

public class TickedScheduler implements Runnable {
    private final MinecraftServer server;
    private final Queue<TickTask> queue = Queues.newPriorityBlockingQueue();
    
    public TickedScheduler(MinecraftServer server) {
        this.server = server;
    }
    
    public void schedule(Runnable task, int delay) {
        this.queue.add(new TickTask(this.server.getTickCount() + delay, task));
    }
    
    @Override
    public void run() {
        TickTask nextTask;
        while ((nextTask = this.queue.peek()) != null) {
            if (nextTask.tick() > this.server.getTickCount())
                break;
            
            var tickTask = this.queue.remove();
            tickTask.task().run();
        }
    }
    
    private record TickTask(int tick, Runnable task) implements Comparable<TickTask> {
        @Override
        public int compareTo(@NotNull TickedScheduler.TickTask other) {
            return Integer.compare(this.tick, other.tick);
        }
    }
}
