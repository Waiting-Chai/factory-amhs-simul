package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Distance-based dispatch rule (REQ-DS-003).
 * <p>
 * Selects the vehicle closest to the task's source position.
 * Distance is calculated using the DispatchContext.getDistance() method,
 * which may use path planning or Euclidean distance depending on implementation.
 * <p>
 * Tie-breaking: When multiple vehicles have the same distance,
 * selects the vehicle with the lowest vehicleId (alphabetical order).
 * <p>
 * This rule is stateless and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class ShortestDistanceRule implements DispatchRule {

    /**
     * Select the vehicle with minimum distance to task source.
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Validate task and context (throw NPE if null)</li>
     *   <li>Return empty if candidates is null or empty</li>
     *   <li>For each candidate, calculate distance to task source</li>
     *   <li>Select vehicle with minimum distance</li>
     *   <li>Break ties by vehicleId (alphabetical ascending)</li>
     * </ol>
     *
     * @param task       the task to be assigned (must not be null)
     * @param candidates list of available vehicles (may be null or empty)
     * @param context    dispatch context for distance calculation (must not be null)
     * @return the closest vehicle, or empty if no candidates
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

        // Find vehicle with minimum distance
        Vehicle closest = null;
        double minDistance = Double.MAX_VALUE;
        String closestId = null;

        for (Vehicle vehicle : candidates) {
            double distance = context.getDistance(task.getSource(), vehicle.position());
            String vehicleId = vehicle.id();

            // Update closest if:
            // 1. This vehicle is closer, OR
            // 2. Same distance (using stable comparison) but vehicleId is alphabetically first (tie-breaker)
            if (closest == null ||
                Double.compare(distance, minDistance) < 0 ||
                (Double.compare(distance, minDistance) == 0 && vehicleId.compareTo(closestId) < 0)) {
                closest = vehicle;
                minDistance = distance;
                closestId = vehicleId;
            }
        }

        return Optional.ofNullable(closest);
    }
}
