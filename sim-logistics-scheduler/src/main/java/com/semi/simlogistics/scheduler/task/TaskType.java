package com.semi.simlogistics.scheduler.task;

/**
 * Task type enumeration (REQ-DS-001, REQ-DS-008).
 * <p>
 * Defines the types of tasks in the logistics simulation system.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public enum TaskType {
    /**
     * Transport task: move cargo from source to destination.
     * Requires a vehicle with transport capability.
     */
    TRANSPORT,

    /**
     * Maintenance task: repair or service a vehicle.
     * Requires an operator/human resource.
     */
    MAINTENANCE
}
