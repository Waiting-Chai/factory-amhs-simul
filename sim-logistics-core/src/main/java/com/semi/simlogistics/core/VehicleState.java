package com.semi.simlogistics.core;

/**
 * Vehicle state enumeration for logistics simulation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public enum VehicleState {
    /**
     * Vehicle is idle and available for tasks.
     */
    IDLE,

    /**
     * Vehicle is moving to a destination.
     */
    MOVING,

    /**
     * Vehicle is loading cargo.
     */
    LOADING,

    /**
     * Vehicle is unloading cargo.
     */
    UNLOADING,

    /**
     * Vehicle is charging (for AGV with battery).
     */
    CHARGING,

    /**
     * Vehicle is under maintenance.
     */
    MAINTENANCE,

    /**
     * Vehicle is blocked waiting for traffic control.
     */
    BLOCKED,

    /**
     * Vehicle has failed and is out of service.
     */
    FAILED
}
