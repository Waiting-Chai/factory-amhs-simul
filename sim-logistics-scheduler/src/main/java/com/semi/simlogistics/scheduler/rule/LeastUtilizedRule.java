package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Utilization-based dispatch rule (REQ-DS-003).
 * <p>
 * Selects the vehicle with the lowest current utilization.
 * Utilization is obtained from DispatchContext.getUtilization(),
 * which typically represents the fraction of time the vehicle has been busy
 * over a recent time window (e.g., last 1 hour of simulation time).
 * <p>
 * Utilization values range from 0.0 (completely idle) to 1.0 (fully utilized).
 * Lower values indicate more available capacity.
 * <p>
 * Tie-breaking: When multiple vehicles have the same utilization,
 * selects the vehicle with the lowest vehicleId (alphabetical order).
 * <p>
 * This rule is useful for load balancing across vehicles to prevent
 * some vehicles from being over-utilized while others sit idle.
 * <p>
 * This rule is stateless and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class LeastUtilizedRule implements DispatchRule {

    /**
     * Select the vehicle with minimum utilization.
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Validate task and context (throw NPE if null)</li>
     *   <li>Return empty if candidates is null or empty</li>
     *   <li>For each candidate, get utilization from context</li>
     *   <li>Select vehicle with minimum utilization</li>
     *   <li>Break ties by vehicleId (alphabetical ascending)</li>
     * </ol>
     * <p>
     * Utilization definition:
     * The context provides utilization as a double value from 0.0 to 1.0,
     * where 0.0 means completely idle and 1.0 means fully utilized.
     * Implementation is typically: busyTime / totalTime over a sliding window.
     *
     * @param task       the task to be assigned (must not be null)
     * @param candidates list of available vehicles (may be null or empty)
     * @param context    dispatch context for utilization data (must not be null)
     * @return the least utilized vehicle, or empty if no candidates
     * @throws NullPointerException if task or context is null
     */
    @Override
    public Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context) {
        // Validate required parameters
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(context, "DispatchContext cannot be null");

        // Return empty for no candidates
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        // Find vehicle with minimum utilization
        Vehicle leastUtilized = null;
        double minUtilization = Double.MAX_VALUE;
        String leastUtilizedId = null;

        for (Vehicle vehicle : candidates) {
            double utilization = context.getUtilization(vehicle.id());
            String vehicleId = vehicle.id();

            // Update leastUtilized if:
            // 1. This vehicle has lower utilization, OR
            // 2. Same utilization (using stable comparison) but vehicleId is alphabetically first (tie-breaker)
            if (leastUtilized == null ||
                Double.compare(utilization, minUtilization) < 0 ||
                (Double.compare(utilization, minUtilization) == 0 && vehicleId.compareTo(leastUtilizedId) < 0)) {
                leastUtilized = vehicle;
                minUtilization = utilization;
                leastUtilizedId = vehicleId;
            }
        }

        return Optional.ofNullable(leastUtilized);
    }
}
