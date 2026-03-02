package com.semi.simlogistics.scheduler.engine;

/**
 * Configuration for DispatchEngine (REQ-DS-004).
 * <p>
 * Contains configurable parameters for the dispatch engine behavior.
 * This is a pure in-memory configuration object (no persistence).
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class DispatchEngineConfig {

    private final long dispatchIntervalMs;
    private final int maxRetryCount;
    private final boolean autoStartTasks;

    /**
     * Create a new dispatch engine configuration.
     *
     * @param dispatchIntervalMs interval between dispatch cycles in milliseconds
     * @param maxRetryCount      maximum number of retry attempts for failed assignments
     * @param autoStartTasks     whether to automatically start tasks after assignment
     * @throws IllegalArgumentException if dispatchIntervalMs is negative or maxRetryCount is negative
     */
    public DispatchEngineConfig(long dispatchIntervalMs, int maxRetryCount, boolean autoStartTasks) {
        if (dispatchIntervalMs < 0) {
            throw new IllegalArgumentException("Dispatch interval must be non-negative, got: " + dispatchIntervalMs);
        }
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("Max retry count must be non-negative, got: " + maxRetryCount);
        }
        this.dispatchIntervalMs = dispatchIntervalMs;
        this.maxRetryCount = maxRetryCount;
        this.autoStartTasks = autoStartTasks;
    }

    /**
     * Get the interval between dispatch cycles in milliseconds.
     *
     * @return dispatch interval in milliseconds
     */
    public long getDispatchIntervalMs() {
        return dispatchIntervalMs;
    }

    /**
     * Get the maximum number of retry attempts for failed assignments.
     *
     * @return maximum retry count
     */
    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    /**
     * Check if tasks should be automatically started after assignment.
     * <p>
     * When true, tasks transition from ASSIGNED to IN_PROGRESS immediately.
     * When false, tasks remain in ASSIGNED state until explicitly started.
     *
     * @return true if auto-start is enabled
     */
    public boolean isAutoStartTasks() {
        return autoStartTasks;
    }
}
