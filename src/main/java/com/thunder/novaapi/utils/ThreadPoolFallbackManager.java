package com.thunder.novaapi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Manages a thread pool with automatic fallback to main thread execution if insufficient CPU threads are available.
 */
public class ThreadPoolFallbackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolFallbackManager.class);
    private static final int MIN_RECOMMENDED_THREADS = 4;

    private final ExecutorService executorService;
    private final boolean isFallback;

    public ThreadPoolFallbackManager(String threadPoolName) {
        int availableCores = Runtime.getRuntime().availableProcessors();
        if (availableCores >= MIN_RECOMMENDED_THREADS) {
            this.executorService = Executors.newFixedThreadPool(availableCores - 1, runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName(threadPoolName + "-Worker");
                thread.setDaemon(true);
                return thread;
            });
            this.isFallback = false;
            LOGGER.info("Initialized '{}' thread pool with {} threads.", threadPoolName, availableCores - 1);
        } else {
            this.executorService = null; // fallback to main thread
            this.isFallback = true;
            LOGGER.warn("Insufficient CPU cores detected ({} cores). '{}' thread pool falling back to main-thread execution.", availableCores, threadPoolName);
        }
    }

    /**
     * Submit a task to be executed asynchronously, or synchronously if fallback occurred.
     *
     * @param runnable the task to execute.
     * @return a Future representing task execution status; null if executed on main thread.
     */
    public Future<?> submit(Runnable runnable) {
        if (executorService != null) {
            return executorService.submit(runnable);
        } else {
            runnable.run();
            return null;
        }
    }

    /**
     * Check if the manager is operating in fallback (main-thread) mode.
     *
     * @return true if fallback occurred, false otherwise.
     */
    public boolean isFallback() {
        return isFallback;
    }

    /**
     * Shutdown the executor service gracefully, if initialized.
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            LOGGER.info("Thread pool shutdown initiated.");
        }
    }
}
