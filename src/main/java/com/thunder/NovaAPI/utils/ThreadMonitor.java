package com.thunder.NovaAPI.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import static com.thunder.NovaAPI.NovaAPI.LOGGER;

public class ThreadMonitor {

    private static final long CHECK_INTERVAL_MS = 10000; // 10 seconds
    private static Thread monitorThread;
    private static boolean running = false;

    public static void startMonitoring() {
        if (running) return; // Prevent multiple instances
        running = true;

        monitorThread = new Thread(() -> {
            while (running) {
                logAllThreads();
                checkForDeadlocks();
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);
                } catch (InterruptedException e) {
                    LOGGER.warn("Thread monitor interrupted", e);
                }
            }
        }, "Nova-ThreadMonitor");

        monitorThread.setDaemon(true); // Allow JVM to exit without waiting
        monitorThread.start();
        LOGGER.info("✅ Nova API Thread Monitor started.");
    }

    public static void stopMonitoring() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }

    private static void logAllThreads() {
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        LOGGER.debug("📌 Active Threads: {}", threads.size());

        for (Thread thread : threads.keySet()) {
            LOGGER.trace("🧵 Thread Name: {} | ID: {} | State: {} | Priority: {}",
                    thread.getName(), thread.getId(), thread.getState(), thread.getPriority());
        }
    }

    private static void checkForDeadlocks() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = threadBean.findDeadlockedThreads();

        if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
            LOGGER.warn("⚠️ WARNING: Deadlocked threads detected!");
            ThreadInfo[] deadlockedThreads = threadBean.getThreadInfo(deadlockedThreadIds);
            for (ThreadInfo info : deadlockedThreads) {
                LOGGER.error("🚨 Deadlocked Thread: {} | State: {} | Lock Owner: {}",
                        info.getThreadName(), info.getThreadState(), info.getLockOwnerName());
            }
        } else {
            LOGGER.debug("✅ No deadlocked threads detected.");
        }
    }
}