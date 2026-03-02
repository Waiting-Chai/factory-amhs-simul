package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for task selection rules (REQ-DS-003).
 * <p>
 * Task selection rules implement different strategies for choosing
 * which task to process next from a list of available tasks.
 * This is distinct from {@link DispatchRule}, which selects a vehicle
 * for a specific task.
 * <p>
 * Use cases:
 * <ul>
 *   <li>HighestPriorityRule: Select highest priority task first</li>
 *   <li>FIFORule: Select earliest created task (time-based)</li>
 *   <li>CustomRule: Domain-specific selection logic</li>
 * </ul>
 * <p>
 * Implementations must be:
 * <ul>
 *   <li>Null-safe: Return Optional.empty() for null/empty task lists</li>
 *   <li>Deterministic: Same inputs should produce same output</li>
 *   <li>Stateless: No mutable shared state (thread-safe)</li>
 *   <li>Fast: Selection should complete in O(n) time where n = tasks.size()</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface TaskSelectionRule {

    /**
     * Select the best task for a vehicle from available tasks.
     * <p>
     * Implementations should:
     * <ul>
     *   <li>Return Optional.empty() if tasks is null or empty</li>
     *   <li>Use context for calculations (current time, vehicle state, etc.)</li>
     *   <li>Break ties deterministically (e.g., by taskId)</li>
     * </ul>
     *
     * @param tasks   list of available tasks (may be null or empty)
     * @param vehicle the vehicle requesting a task (must not be null)
     * @param context dispatch context with time/state providers (must not be null)
     * @return the selected task, or empty if no suitable task found
     * @throws NullPointerException if vehicle or context is null
     */
    Optional<Task> selectTask(List<Task> tasks, Vehicle vehicle, DispatchContext context);
}
