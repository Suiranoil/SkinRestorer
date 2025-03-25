package net.lionarius.skinrestorer.util;

import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

public class ServerUtils {
    
    private ServerUtils() {}
    
    public static void scheduleServerTask(MinecraftServer server, Consumer<MinecraftServer> task) {
        ServerUtils.scheduleServerTask(server, task, 0);
    }
    
    public static void scheduleServerTask(MinecraftServer server, Runnable task) {
        ServerUtils.scheduleServerTask(server, task, 0);
    }
    
    public static void scheduleServerTask(MinecraftServer server, Consumer<MinecraftServer> task, int tickDelay) {
        server.execute(new ScheduledTickTask(server, task, server.getTickCount() + tickDelay));
    }
    
    public static void scheduleServerTask(MinecraftServer server, Runnable task, int tickDelay) {
        server.execute(new ScheduledTickTask(server, _server -> task.run(), server.getTickCount() + tickDelay));
    }
    
    public record ScheduledTickTask(MinecraftServer server, Consumer<MinecraftServer> task,
                                    int tickTarget) implements Runnable {
        @Override
        public void run() {
            if (this.server.getTickCount() >= this.tickTarget)
                this.task.accept(this.server);
            else
                this.server.execute(this);
        }
    }
}
