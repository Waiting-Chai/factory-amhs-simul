package com.semi.simlogistics.scheduler.port;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.vehicle.Vehicle;

/**
 * Port for dispatch notifications (REQ-DS-004).
 * <p>
 * This port defines the contract for sending task assignment notifications
 * to vehicles or other interested parties (e.g., event bus).
 * <p>
 * Implementations can:
 * <ul>
 *   <li>Send notification directly to vehicle process</li>
 *   <li>Publish event to simulation event bus</li>
 *   <li>Log assignment for debugging</li>
 * </ul>
 * <p>
 * This port is part of the scheduler module's interface with external systems.
 * Actual notification implementations are provided by infrastructure adapters.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface DispatchNotificationPort {

    /**
     * Notify that a task has been assigned to a vehicle.
     * <p>
     * This method is called after a task is successfully assigned.
     * Implementations should ensure the vehicle receives the assignment.
     *
     * @param task    the task that was assigned (must not be null)
     * @param vehicle the vehicle assigned to the task (must not be null)
     * @throws NullPointerException if task or vehicle is null
     * @throws RuntimeException      if notification fails (will be caught and logged)
     */
    void notifyTaskAssigned(Task task, Vehicle vehicle);
}
