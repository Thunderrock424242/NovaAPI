package com.thunder.NovaAPI.task;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Manages background tasks while keeping dimension references lightweight (ResourceKey only).
 */
public final class BackgroundTaskScheduler {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private BackgroundTaskScheduler() {
    }

    public static Future<?> submit(ResourceKey<Level> dimension, long chunkPos, Runnable task) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(task, "task");
        return EXECUTOR.submit(() -> task.run());
    }

    public static <T> Future<T> submit(ResourceKey<Level> dimension, long chunkPos, Supplier<T> supplier) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(supplier, "supplier");
        return EXECUTOR.submit(supplier::get);
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
