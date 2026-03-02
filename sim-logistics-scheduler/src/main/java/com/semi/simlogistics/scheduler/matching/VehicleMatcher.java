package com.semi.simlogistics.scheduler.matching;

import com.semi.simlogistics.scheduler.rule.DispatchContext;
import com.semi.simlogistics.scheduler.rule.DispatchRule;
import com.semi.simlogistics.scheduler.rule.TaskSelectionRule;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Task-vehicle matching service (REQ-DS-004).
 * <p>
 * Encapsulates the matching algorithm that selects which vehicle to assign
 * to a task. This service uses:
 * <ul>
 *   <li>TaskSelectionRule: Selects the best task from a list (if needed)</li>
 *   <li>DispatchRule: Selects the best vehicle for a task</li>
 *   <li>DispatchContext: Provides distance/utilization/time data</li>
 * </ul>
 * <p>
 * The matching process:
 * <ol>
 *   <li>Filter available vehicles (IDLE state only)</li>
 *   <li>Use dispatch rule to select best vehicle for the task</li>
 *   <li>Return matching result with task-vehicle pair</li>
 * </ol>
 * <p>
 * This class is stateless and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class VehicleMatcher {

    private final DispatchRule dispatchRule;
    private final TaskSelectionRule taskSelectionRule;

    /**
     * Create a new vehicle matcher.
     *
     * @param dispatchRule        rule for selecting vehicles (must not be null)
     * @param taskSelectionRule   rule for selecting tasks (must not be null)
     * @throws NullPointerException if any rule is null
     */
    public VehicleMatcher(DispatchRule dispatchRule, TaskSelectionRule taskSelectionRule) {
        this.dispatchRule = Objects.requireNonNull(dispatchRule, "DispatchRule cannot be null");
        this.taskSelectionRule = Objects.requireNonNull(taskSelectionRule, "TaskSelectionRule cannot be null");
    }

    /**
     * Match a task to an available vehicle.
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Get available vehicles from context (already filtered to IDLE state)</li>
     *   <li>Use dispatch rule to select best vehicle for the task</li>
     *   <li>If vehicle found, return success result with task-vehicle pair</li>
     *   <li>If no vehicle available, return empty result</li>
     * </ol>
     *
     * @param task    the task to match (must not be null)
     * @param context matching context with vehicles and dispatch data (must not be null)
     * @return optional matching result, empty if no vehicle available
     * @throws NullPointerException if task or context is null
     */
    public Optional<MatchingResult> match(Task task, MatchingContext context) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(context, "MatchingContext cannot be null");

        List<Vehicle> availableVehicles = context.getAvailableVehicles();
        DispatchContext dispatchContext = context.getDispatchContext();

        // Use dispatch rule to select best vehicle
        Optional<Vehicle> selectedVehicle = dispatchRule.selectVehicle(
                task,
                availableVehicles,
                dispatchContext
        );

        // Return result based on selection
        return selectedVehicle.map(vehicle -> MatchingResult.success(task.getId(), vehicle.id()));
    }
}
