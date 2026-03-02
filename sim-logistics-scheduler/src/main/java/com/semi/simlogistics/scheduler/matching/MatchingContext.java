package com.semi.simlogistics.scheduler.matching;

import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.scheduler.rule.DispatchContext;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Context for task-vehicle matching operations (REQ-DS-004).
 * <p>
 * Provides the matching algorithm with:
 * <ul>
 *   <li>Available tasks (pending tasks in queue)</li>
 *   <li>Available vehicles (vehicles in IDLE state)</li>
 *   <li>Dispatch context (for distance/utilization/time calculations)</li>
 * </ul>
 * <p>
 * This class filters vehicles to only include those available for assignment
 * (currently in IDLE state). This ensures that matching only considers vehicles
 * that can actually accept new tasks.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class MatchingContext {

    private final List<Task> availableTasks;
    private final List<Vehicle> availableVehicles;
    private final DispatchContext dispatchContext;

    /**
     * Create a new matching context.
     * <p>
     * Vehicles are automatically filtered to only include those in IDLE state.
     *
     * @param availableTasks   all tasks available for matching (must not be null)
     * @param allVehicles      all vehicles in the system (must not be null)
     * @param dispatchContext  dispatch context for calculations (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    public MatchingContext(List<Task> availableTasks, List<Vehicle> allVehicles,
                           DispatchContext dispatchContext) {
        this.availableTasks = new ArrayList<>(Objects.requireNonNull(availableTasks, "Available tasks cannot be null"));
        this.dispatchContext = Objects.requireNonNull(dispatchContext, "Dispatch context cannot be null");

        // Filter vehicles to only include those available for assignment
        this.availableVehicles = Objects.requireNonNull(allVehicles, "Vehicles cannot be null").stream()
                .filter(v -> v.getState() == VehicleState.IDLE)
                .collect(Collectors.toList());
    }

    /**
     * Get tasks available for matching.
     * <p>
     * Returns a defensive copy to prevent external modification.
     *
     * @return list of available tasks
     */
    public List<Task> getAvailableTasks() {
        return new ArrayList<>(availableTasks);
    }

    /**
     * Get vehicles available for assignment.
     * <p>
     * This only includes vehicles in IDLE state. Vehicles in other states
     * (MOVING, LOADING, CHARGING, etc.) are excluded.
     * <p>
     * Returns a defensive copy to prevent external modification.
     *
     * @return list of available vehicles (IDLE state only)
     */
    public List<Vehicle> getAvailableVehicles() {
        return new ArrayList<>(availableVehicles);
    }

    /**
     * Get the dispatch context for calculations.
     *
     * @return dispatch context
     */
    public DispatchContext getDispatchContext() {
        return dispatchContext;
    }
}
