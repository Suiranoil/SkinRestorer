package net.lionarius.skinrestorer.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SkinExecutor {
    private static final int MAX_THREADS = 8;

    // dedicated pool for blocking skin fetches so we never tie up the common ForkJoinPool
    public static final ExecutorService FETCH_EXECUTOR = SkinExecutor.createExecutor();

    private SkinExecutor() {}

    private static ExecutorService createExecutor() {
        var counter = new AtomicInteger();
        var executor = new ThreadPoolExecutor(
                SkinExecutor.MAX_THREADS,
                SkinExecutor.MAX_THREADS,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    var thread = new Thread(runnable, "SkinRestorer-Fetch-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
