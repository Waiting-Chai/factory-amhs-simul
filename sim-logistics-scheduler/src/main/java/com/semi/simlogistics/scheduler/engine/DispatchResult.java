package com.semi.simlogistics.scheduler.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a dispatch cycle (REQ-DS-004).
 * <p>
 * Contains statistics and information about the outcome of a single
 * dispatch cycle, including counts of assigned/unassigned/failed tasks
 * and any warnings that occurred during dispatch.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class DispatchResult {

    private final int assignedCount;
    private final int unassignedCount;
    private final int failedCount;
    private final List<String> warnings;

    /**
     * Create a new dispatch result.
     *
     * @param assignedCount   number of tasks successfully assigned
     * @param unassignedCount number of tasks that could not be assigned
     * @param failedCount     number of tasks that failed during assignment
     * @param warnings        list of warning messages (may be empty)
     */
    public DispatchResult(int assignedCount, int unassignedCount, int failedCount, List<String> warnings) {
        this.assignedCount = assignedCount;
        this.unassignedCount = unassignedCount;
        this.failedCount = failedCount;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /**
     * Create an empty dispatch result (no tasks processed).
     *
     * @return empty dispatch result
     */
    public static DispatchResult empty() {
        return new DispatchResult(0, 0, 0, Collections.emptyList());
    }

    /**
     * Get the number of tasks successfully assigned.
     *
     * @return assigned task count
     */
    public int getAssignedCount() {
        return assignedCount;
    }

    /**
     * Get the number of tasks that could not be assigned.
     * <p>
     * These tasks remain in the queue for the next cycle.
     *
     * @return unassigned task count
     */
    public int getUnassignedCount() {
        return unassignedCount;
    }

    /**
     * Get the number of tasks that failed during assignment.
     * <p>
     * These tasks have been marked as FAILED and removed from the queue.
     *
     * @return failed task count
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * Get the list of warning messages.
     * <p>
     * Returns a defensive copy to prevent external modification.
     *
     * @return list of warning messages
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Check if any warnings occurred during dispatch.
     *
     * @return true if there are warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Get a human-readable summary of this result.
     *
     * @return summary string
     */
    public String getSummary() {
        return String.format("DispatchResult{assigned=%d, unassigned=%d, failed=%d, warnings=%d}",
                assignedCount, unassignedCount, failedCount, warnings.size());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
