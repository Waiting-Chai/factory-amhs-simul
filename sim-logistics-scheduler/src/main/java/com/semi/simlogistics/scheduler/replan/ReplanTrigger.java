package com.semi.simlogistics.scheduler.replan;

/**
 * Types of replan triggers for dynamic replanning (REQ-DS-006).
 * <p>
 * Defines the different scenarios that can trigger a path replanning
 * operation for a vehicle.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public enum ReplanTrigger {

    /**
     * Path blocked - vehicle has been waiting too long at a control point.
     * Triggered by TrafficManager when blocking timeout threshold is exceeded.
     */
    PATH_BLOCKED,

    /**
     * Task cancelled - the currently assigned task was cancelled.
     * Vehicle needs to stop and either wait for new task or return to standby.
     */
    TASK_CANCELLED,

    /**
     * Traffic conflict - TrafficManager requests replanning to avoid deadlock.
     * Multiple vehicles in same area need alternative paths.
     */
    TRAFFIC_CONFLICT
}
