package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Task priority-based selection rule (REQ-DS-003).
 * <p>
 * Selects tasks based on priority order. Higher priority tasks are
 * selected before lower priority tasks.
 * <p>
 * Tie-breaking for same priority:
 * <ol>
 *   <li>Earlier created time (FIFO)</li>
 *   <li>Lower taskId (alphabetical) if created time is same</li>
 * </ol>
 * <p>
 * Priority order (highest to lowest):
 * CRITICAL > URGENT > HIGH > NORMAL > LOW > LOWEST
 * <p>
 * This rule does NOT modify TaskQueue ordering. It provides a separate
 * selection method that can be used independently of the queue.
 * <p>
 * This rule implements {@link TaskSelectionRule} for task-to-vehicle
 * assignment scenarios where the highest priority task should be
 * selected first.
 * <p>
 * This rule is stateless and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class HighestPriorityRule implements TaskSelectionRule {

    /**
     * Comparator for task priority ordering.
     * <p>
     * Order: priority (descending) -> createdTime (ascending) -> taskId (ascending)
     */
    private static final Comparator<Task> TASK_COMPARATOR = (a, b) -> {
        // Higher priority first (descending by level)
        int priorityCompare = Integer.compare(
                b.getPriority().getLevel(),
                a.getPriority().getLevel()
        );
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // Earlier created time first (ascending)
        int timeCompare = Long.compare(a.getCreatedTime(), b.getCreatedTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        // Alphabetical taskId for stable ordering
        return a.getId().compareTo(b.getId());
    };

    /**
     * Select the next highest priority task from a list.
     * <p>
     * This method selects the task with the highest priority, breaking ties
     * by creation time and then by taskId for stability.
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Validate vehicle and context (throw NPE if null)</li>
     *   <li>Return empty if tasks is null or empty</li>
     *   <li>Find task with the highest priority using TASK_COMPARATOR</li>
     *   <li>Return the selected task</li>
     * </ol>
     *
     * @param tasks   list of tasks to select from (maybe null or empty)
     * @param vehicle the vehicle requesting a task (must not be null)
     * @param context dispatch context (must not be null)
     * @return the highest priority task, or empty if no tasks available
     * @throws NullPointerException if vehicle or context is null
     */
    @Override
    public Optional<Task> selectTask(List<Task> tasks, Vehicle vehicle, DispatchContext context) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        Objects.requireNonNull(context, "DispatchContext cannot be null");

        if (tasks == null || tasks.isEmpty()) {
            return Optional.empty();
        }

        // Find task with minimum according to comparator (highest priority, earliest, first id)
        return tasks.stream()
                .min(TASK_COMPARATOR);
    }
}
