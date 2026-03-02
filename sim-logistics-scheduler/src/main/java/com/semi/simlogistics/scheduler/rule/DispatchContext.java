package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.core.Position;

/**
 * Context interface for dispatch rule calculations (REQ-DS-003).
 * <p>
 * Provides dispatch rules with the necessary information to make decisions
 * without directly depending on external services or databases.
 * <p>
 * This interface decouples the rule layer from infrastructure concerns,
 * making rules testable and reusable in different contexts.
 * <p>
 * Implementations can provide:
 * <ul>
 *   <li>Distance calculations (using path planner or Euclidean distance)</li>
 *   <li>Vehicle utilization metrics (from metrics collector)</li>
 *   <li>Current simulation time (from simulation clock)</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface DispatchContext {

    /**
     * Calculate the distance between two positions.
     * <p>
     * Implementations may use:
     * <ul>
     *   <li>Path planner for accurate route distance</li>
     *   <li>Euclidean distance for fast estimation</li>
     *   <li>Cache for repeated queries</li>
     * </ul>
     *
     * @param from starting position (must not be null)
     * @param to   destination position (must not be null)
     * @return distance in meters (must be non-negative)
     * @throws NullPointerException if from or to is null
     */
    double getDistance(Position from, Position to);

    /**
     * Get the current utilization of a vehicle.
     * <p>
     * Utilization is defined as the fraction of time the vehicle has been busy
     * over a recent time window. Value ranges from 0.0 (idle) to 1.0 (fully utilized).
     * <p>
     * Implementations should:
     * <ul>
     *   <li>Calculate utilization from metrics collector</li>
     *   <li>Use a sliding window (e.g., last 1 hour of simulation time)</li>
     *   <li>Return 0.0 if no data is available</li>
     * </ul>
     *
     * @param vehicleId vehicle identifier (must not be null)
     * @return utilization value from 0.0 to 1.0
     * @throws NullPointerException if vehicleId is null
     */
    double getUtilization(String vehicleId);

    /**
     * Get the current simulation time.
     * <p>
     * This returns simulation time, not wall-clock time. The value is in
     * milliseconds since simulation start, or could be seconds depending
     * on the simulation clock implementation.
     * <p>
     * Implementations should delegate to the simulation clock.
     *
     * @return current simulation time in milliseconds
     */
    long getCurrentTime();
}
