package com.thunder.novaapi.api.async;

/**
 * Receives async task lifecycle callbacks from NovaAPI.
 */
public interface AsyncTaskListener {
    /**
     * Called when a task is accepted and queued for main-thread execution.
     */
    default void onTaskQueued(String label, AsyncTaskType type, int backlog, int queueSize) {
    }

    /**
     * Called when a task is rejected by the worker or main-thread queue.
     */
    default void onTaskRejected(String label, AsyncTaskType type, int backlog, int queueSize) {
    }

    /**
     * Called after the main-thread queue is drained for the current tick.
     */
    default void onMainThreadQueueDrained(int processed, int backlog) {
    }
}
