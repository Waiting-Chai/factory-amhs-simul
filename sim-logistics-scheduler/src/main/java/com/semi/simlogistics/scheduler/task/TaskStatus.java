package com.semi.simlogistics.scheduler.task;

/**
 * Task status enumeration (REQ-DS-001).
 * <p>
 * Defines the complete lifecycle states of a task with valid transitions:
 * <pre>
 * PENDING -> ASSIGNED -> IN_PROGRESS -> COMPLETED
 *    |          |            |
 *    v          v            v
 * CANCELLED  CANCELLED   CANCELLING -> CANCELLED
 *    |          |            |
 *    v          v            v
 * FAILED     FAILED       FAILED
 * </pre>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public enum TaskStatus {
    /**
     * Task is created and waiting to be assigned.
     */
    PENDING,

    /**
     * Task has been assigned to a vehicle but not yet started.
     */
    ASSIGNED,

    /**
     * Task is currently being executed by a vehicle.
     */
    IN_PROGRESS,

    /**
     * Task has been completed successfully.
     */
    COMPLETED,

    /**
     * Task was cancelled before execution.
     */
    CANCELLED,

    /**
     * Task is being cancelled (in-progress task waiting for interruption).
     */
    CANCELLING,

    /**
     * Task failed due to an error.
     */
    FAILED
}
