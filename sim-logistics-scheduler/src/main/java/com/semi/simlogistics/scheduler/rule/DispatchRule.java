package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for task-to-vehicle dispatch rules (REQ-DS-003).
 * <p>
 * Dispatch rules implement different selection strategies for matching
 * tasks to available vehicles. Each rule evaluates candidates and returns
 * the most appropriate vehicle based on its criteria.
 * <p>
 * Implementations must be:
 * <ul>
 *   <li>Null-safe: Return Optional.empty() for null/empty candidate lists</li>
 *   <li>Deterministic: Same inputs should produce same output</li>
 *   <li>Stateless: No mutable shared state (thread-safe)</li>
 *   <li>Fast: Selection should complete in O(n) time where n = candidates.size()</li>
 * </ul>
 * <p>
 * All rules must break ties deterministically to ensure stable selection.
 * Recommended tie-breaker: vehicleId in ascending (alphabetical) order.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface DispatchRule {

    /**
     * Select the best vehicle for a task from available candidates.
     * <p>
     * Implementations should:
     * <ul>
     *   <li>Return Optional.empty() if candidates is null or empty</li>
     *   <li>Use context for calculations (distance, utilization, time)</li>
     *   <li>Break ties deterministically (e.g., by vehicleId)</li>
     * </ul>
     *
     * @param task       the task to be assigned (must not be null)
     * @param candidates list of available vehicles (may be null or empty)
     * @param context    dispatch context with distance/utilization/time providers (must not be null)
     * @return the selected vehicle, or empty if no suitable vehicle found
     * @throws NullPointerException if task or context is null
     */
    Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context);
}
