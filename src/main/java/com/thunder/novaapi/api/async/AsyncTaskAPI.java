package com.thunder.novaapi.api.async;

import com.thunder.novaapi.async.AsyncTaskManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public API surface for NovaAPI async task execution.
 */
public final class AsyncTaskAPI {
    private static final List<AsyncTaskListener> LISTENERS = new CopyOnWriteArrayList<>();

    private AsyncTaskAPI() {
    }

    public static CompletableFuture<Boolean> submitCpuTask(String label, AsyncTaskManager.TaskPayload taskPayload) {
        return AsyncTaskManager.submitCpuTask(label, taskPayload);
    }

    public static CompletableFuture<Boolean> submitIoTask(String label, AsyncTaskManager.TaskPayload taskPayload) {
        return AsyncTaskManager.submitIoTask(label, taskPayload);
    }

    public static void registerListener(AsyncTaskListener listener) {
        LISTENERS.add(Objects.requireNonNull(listener, "listener"));
    }

    public static void unregisterListener(AsyncTaskListener listener) {
        LISTENERS.remove(listener);
    }

    public static void notifyTaskQueued(String label, AsyncTaskType type, int backlog, int queueSize) {
        for (AsyncTaskListener listener : LISTENERS) {
            listener.onTaskQueued(label, type, backlog, queueSize);
        }
    }

    public static void notifyTaskRejected(String label, AsyncTaskType type, int backlog, int queueSize) {
        for (AsyncTaskListener listener : LISTENERS) {
            listener.onTaskRejected(label, type, backlog, queueSize);
        }
    }

    public static void notifyQueueDrained(int processed, int backlog) {
        if (processed <= 0) {
            return;
        }
        for (AsyncTaskListener listener : LISTENERS) {
            listener.onMainThreadQueueDrained(processed, backlog);
        }
    }
}
