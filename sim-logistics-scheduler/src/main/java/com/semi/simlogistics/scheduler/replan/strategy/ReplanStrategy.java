package com.semi.simlogistics.scheduler.replan.strategy;

import com.semi.simlogistics.scheduler.replan.ReplanResult;
import com.semi.simlogistics.scheduler.replan.ReplanTrigger;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

/**
 * Strategy interface for vehicle path replanning (REQ-DS-006).
 * <p>
 * Different replanning scenarios require different strategies:
 * - Task cancellation: stop vehicle, return to standby or plan to new task
 * - Path blocked: calculate alternative route to same destination
 * - Traffic conflict: find alternate path to avoid congestion
 * <p>
 * Implementations must be thread-safe as they may be called from
 * multiple vehicle processes concurrently.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface ReplanStrategy {

    /**
     * Execute replanning for a vehicle.
     * <p>
     * Called when a replan trigger occurs. The strategy should:
     * 1. Determine the new destination (if applicable)
     * 2. Calculate a new path using PathPlanner
     * 3. Update vehicle state and path if successful
     * 4. Return appropriate ReplanResult
     * <p>
     * Constraints:
     * - Must use simulation time (not wall clock) for any time calculations
     * - Must respect max replan attempts configured in system_config
     * - Must not cause dispatch loop to spin or block indefinitely
     * - Must handle task state machine transitions correctly
     *
     * @param vehicle vehicle to replan for
     * @param currentTask current task (may be null if vehicle has no task)
     * @param newTask new task if available (null for task cancellation scenarios)
     * @param currentTime current simulation time from env.now()
     * @return replan result indicating success or failure
     */
    ReplanResult execute(Vehicle vehicle, Task currentTask, Task newTask, double currentTime);

    /**
     * Get the trigger type this strategy handles.
     *
     * @return the replan trigger type
     */
    ReplanTrigger getTriggerType();
}
