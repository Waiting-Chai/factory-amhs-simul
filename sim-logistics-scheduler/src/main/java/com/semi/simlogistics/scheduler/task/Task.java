package com.semi.simlogistics.scheduler.task;

import com.semi.simlogistics.core.Position;

import java.util.Map;
import java.util.Objects;

/**
 * Task model for logistics simulation (REQ-DS-001).
 * <p>
 * Represents a dispatchable unit of work (transport or maintenance)
 * with state machine validation and lifecycle management.
 * <p>
 * Valid state transitions:
 * <pre>
 * PENDING -> ASSIGNED -> IN_PROGRESS -> COMPLETED
 *    |          |            |
 *    v          v            v
 * CANCELLED  CANCELLED   CANCELLING -> CANCELLED
 *    |          |            |
 *    v          v            v
 * FAILED     FAILED       FAILED
 * </pre>
 * <p>
 * <b>Time semantics:</b> Timestamps currently use wall-clock time
 * (System.currentTimeMillis()) as a placeholder. This will be replaced
 * with simulation time in Phase 8 when integrating with the simulation clock.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class Task {

    // Core identity
    private final String id;
    private final String simulationId;
    private final TaskType taskType;

    // Route information
    private final Position source;
    private final Position destination;

    // Priority and status
    private final TaskPriority priority;
    private TaskStatus status;

    // Cargo information (optional)
    private Map<String, Object> cargoInfo;

    // Assignment
    private String assignedVehicleId;

    // Timestamps (all in milliseconds since epoch)
    // TODO: Phase 8 - Replace with simulation time from SimulationClock
    private final long createdTime;
    private Long startedTime;
    private Long completedTime;

    // Error handling
    private String errorMessage;

    // Interruption flag for in-progress cancellation
    private boolean interruptionRequested;

    /**
     * Create a new task.
     *
     * @param id           unique task identifier (must not be blank)
     * @param simulationId simulation this task belongs to (must not be blank)
     * @param taskType     type of task (TRANSPORT or MAINTENANCE)
     * @param source       starting position
     * @param destination  target position
     * @param priority     task priority level
     * @throws NullPointerException     if any required parameter is null
     * @throws IllegalArgumentException if id or simulationId is blank
     */
    public Task(String id, String simulationId, TaskType taskType,
                Position source, Position destination, TaskPriority priority) {
        this.id = requireNonBlank(id, "Task id cannot be null", "Task id cannot be blank");
        this.simulationId = requireNonBlank(simulationId, "Simulation id cannot be null", "Simulation id cannot be blank");
        this.taskType = Objects.requireNonNull(taskType, "Task type cannot be null");
        this.source = Objects.requireNonNull(source, "Source position cannot be null");
        this.destination = Objects.requireNonNull(destination, "Destination position cannot be null");
        this.priority = Objects.requireNonNull(priority, "Task priority cannot be null");
        this.status = TaskStatus.PENDING;
        this.createdTime = System.currentTimeMillis();
    }

    /**
     * Create a new task with explicit created time for testing only.
     * <p>
     * <b>TEST-ONLY CONSTRUCTOR:</b> This constructor allows injection of a specific
     * createdTime to test scenarios where multiple tasks have identical timestamps.
     * Do not use in production code.
     *
     * @param id           unique task identifier (must not be blank)
     * @param simulationId simulation this task belongs to (must not be blank)
     * @param taskType     type of task (TRANSPORT or MAINTENANCE)
     * @param source       starting position
     * @param destination  target position
     * @param priority     task priority level
     * @param createdTime  explicit creation timestamp in milliseconds
     * @throws NullPointerException     if any required parameter is null
     * @throws IllegalArgumentException if id or simulationId is blank
     */
    Task(String id, String simulationId, TaskType taskType,
         Position source, Position destination, TaskPriority priority, long createdTime) {
        this.id = requireNonBlank(id, "Task id cannot be null", "Task id cannot be blank");
        this.simulationId = requireNonBlank(simulationId, "Simulation id cannot be null", "Simulation id cannot be blank");
        this.taskType = Objects.requireNonNull(taskType, "Task type cannot be null");
        this.source = Objects.requireNonNull(source, "Source position cannot be null");
        this.destination = Objects.requireNonNull(destination, "Destination position cannot be null");
        this.priority = Objects.requireNonNull(priority, "Task priority cannot be null");
        this.status = TaskStatus.PENDING;
        this.createdTime = createdTime;
    }

    /**
     * Validate that a string is not null or blank.
     * <p>
     * Throws NullPointerException for null and IllegalArgumentException for blank strings,
     * with distinct error messages for better debugging.
     *
     * @param value       the string to validate
     * @param nullMessage  error message when value is null
     * @param blankMessage error message when value is blank (empty or whitespace only)
     * @return the validated string
     * @throws IllegalArgumentException if string is blank (empty or whitespace only)
     * @throws NullPointerException     if string is null
     */
    private static String requireNonBlank(String value, String nullMessage, String blankMessage) {
        if (value == null) {
            throw new NullPointerException(nullMessage);
        }
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(blankMessage);
        }
        return value;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getSimulationId() {
        return simulationId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public Position getSource() {
        return source;
    }

    public Position getDestination() {
        return destination;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Map<String, Object> getCargoInfo() {
        return cargoInfo;
    }

    public void setCargoInfo(Map<String, Object> cargoInfo) {
        this.cargoInfo = cargoInfo;
    }

    public String getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public Long getStartedTime() {
        return startedTime;
    }

    public Long getCompletedTime() {
        return completedTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isInterruptionRequested() {
        return interruptionRequested;
    }

    // State machine operations

    /**
     * Assign this task to a vehicle.
     * <p>
     * Valid from: PENDING
     * Transitions to: ASSIGNED
     *
     * @param vehicleId ID of the vehicle to assign (must not be null or blank)
     * @return true if assignment succeeded, false otherwise
     * @throws NullPointerException     if vehicleId is null
     * @throws IllegalArgumentException if vehicleId is blank
     */
    public boolean assignTo(String vehicleId) {
        requireNonBlank(vehicleId, "Vehicle id cannot be null", "Vehicle id cannot be blank");
        if (status != TaskStatus.PENDING) {
            return false;
        }
        this.assignedVehicleId = vehicleId;
        this.status = TaskStatus.ASSIGNED;
        return true;
    }

    /**
     * Mark task as started (vehicle began execution).
     * <p>
     * Valid from: ASSIGNED
     * Transitions to: IN_PROGRESS
     *
     * @return true if start succeeded, false otherwise
     */
    public boolean start() {
        if (status != TaskStatus.ASSIGNED) {
            return false;
        }
        this.status = TaskStatus.IN_PROGRESS;
        this.startedTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Mark task as completed successfully.
     * <p>
     * Valid from: IN_PROGRESS
     * Transitions to: COMPLETED
     *
     * @return true if completion succeeded, false otherwise
     */
    public boolean complete() {
        if (status != TaskStatus.IN_PROGRESS) {
            return false;
        }
        this.status = TaskStatus.COMPLETED;
        this.completedTime = System.currentTimeMillis();
        return true;
    }

    /**
     * Cancel this task.
     * <p>
     * Valid from: PENDING, ASSIGNED -> CANCELLED
     * Valid from: IN_PROGRESS -> CANCELLING (marks for interruption)
     * Invalid from: COMPLETED, CANCELLED, FAILED
     *
     * @return true if cancellation was initiated, false otherwise
     */
    public boolean cancel() {
        switch (status) {
            case PENDING:
            case ASSIGNED:
                this.status = TaskStatus.CANCELLED;
                return true;
            case IN_PROGRESS:
                this.status = TaskStatus.CANCELLING;
                this.interruptionRequested = true;
                return true;
            default:
                return false;
        }
    }

    /**
     * Mark task as failed.
     * <p>
     * Valid from: PENDING, ASSIGNED, IN_PROGRESS, CANCELLING
     * Invalid from: COMPLETED, CANCELLED, FAILED (terminal states)
     *
     * @param errorMessage error description
     * @return true if failure was recorded, false if state transition was invalid
     */
    public boolean fail(String errorMessage) {
        // Cannot fail from terminal states
        if (status == TaskStatus.COMPLETED ||
            status == TaskStatus.CANCELLED ||
            status == TaskStatus.FAILED) {
            return false;
        }
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        return true;
    }

    /**
     * Confirm cancellation of a task that was in CANCELLING state.
     * <p>
     * This is called after the vehicle acknowledges interruption.
     * <p>
     * Valid from: CANCELLING
     * Transitions to: CANCELLED
     */
    public void confirmCancellation() {
        if (status == TaskStatus.CANCELLING) {
            this.status = TaskStatus.CANCELLED;
        }
    }
}
