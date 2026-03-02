package com.semi.simlogistics.scheduler.port;

import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Task persistence (Phase 8).
 * <p>
 * This port defines the contract for task persistence operations.
 * In Phase 3, this is an interface only - no database implementation.
 * MySQL adapter will be implemented in Phase 8 (Data Persistence).
 * <p>
 * Architecture compliance:
 * - Core/scheduler modules depend only on this port interface
 * - Database adapters (sim-logistics-web) implement this port
 * - No direct database dependencies in domain layer
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface TaskRepositoryPort {

    /**
     * Save a task (create or update).
     *
     * @param task task to save
     * @return saved task with generated ID if new
     */
    Task save(Task task);

    /**
     * Find task by ID.
     *
     * @param id task ID
     * @return optional task, empty if not found
     */
    Optional<Task> findById(String id);

    /**
     * Find all tasks for a simulation.
     *
     * @param simulationId simulation ID
     * @return list of tasks for the simulation
     */
    List<Task> findBySimulationId(String simulationId);

    /**
     * Delete a task by ID.
     *
     * @param id task ID
     * @return true if task was deleted, false if not found
     */
    boolean deleteById(String id);

    /**
     * Find all tasks with a specific status.
     *
     * @param status task status
     * @return list of tasks with the status
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * Find tasks assigned to a specific vehicle.
     *
     * @param vehicleId vehicle ID
     * @return list of tasks assigned to the vehicle
     */
    List<Task> findByAssignedVehicleId(String vehicleId);
}
