package com.semi.simlogistics.scheduler.matching;

import java.util.Optional;

/**
 * Result of a task-vehicle matching operation (REQ-DS-004).
 * <p>
 * Represents the outcome of attempting to match a task with a vehicle.
 * Can be a successful match, a failed match, or an empty result.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class MatchingResult {

    private final Optional<String> taskId;
    private final Optional<String> vehicleId;
    private final boolean success;
    private final Optional<String> failureReason;

    private MatchingResult(Optional<String> taskId, Optional<String> vehicleId,
                           boolean success, Optional<String> failureReason) {
        this.taskId = taskId;
        this.vehicleId = vehicleId;
        this.success = success;
        this.failureReason = failureReason;
    }

    /**
     * Create a successful matching result.
     *
     * @param taskId    the ID of the matched task (must not be null or blank)
     * @param vehicleId the ID of the matched vehicle (must not be null or blank)
     * @return a successful matching result
     * @throws NullPointerException     if taskId or vehicleId is null
     * @throws IllegalArgumentException if taskId or vehicleId is blank
     */
    public static MatchingResult success(String taskId, String vehicleId) {
        requireNonBlank(taskId, "Task ID cannot be null", "Task ID cannot be blank");
        requireNonBlank(vehicleId, "Vehicle ID cannot be null", "Vehicle ID cannot be blank");
        return new MatchingResult(Optional.of(taskId), Optional.of(vehicleId), true, Optional.empty());
    }

    /**
     * Create a failed matching result with a reason.
     *
     * @param taskId        the ID of the task that failed to match (must not be null or blank)
     * @param failureReason the reason why matching failed (must not be null or blank)
     * @return a failed matching result
     * @throws NullPointerException     if taskId or failureReason is null
     * @throws IllegalArgumentException if taskId or failureReason is blank
     */
    public static MatchingResult failure(String taskId, String failureReason) {
        requireNonBlank(taskId, "Task ID cannot be null", "Task ID cannot be blank");
        requireNonBlank(failureReason, "Failure reason cannot be null", "Failure reason cannot be blank");
        return new MatchingResult(Optional.of(taskId), Optional.empty(), false, Optional.of(failureReason));
    }

    /**
     * Create an empty matching result (no match found).
     *
     * @return an empty matching result
     */
    public static MatchingResult empty() {
        return new MatchingResult(Optional.empty(), Optional.empty(), false, Optional.empty());
    }

    /**
     * Validate that a string is not null or blank.
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

    /**
     * Get the ID of the matched task.
     *
     * @return optional task ID, empty if no match
     */
    public Optional<String> getTaskId() {
        return taskId;
    }

    /**
     * Get the ID of the matched vehicle.
     *
     * @return optional vehicle ID, empty if no match or match failed
     */
    public Optional<String> getVehicleId() {
        return vehicleId;
    }

    /**
     * Check if this result represents a successful match.
     *
     * @return true if matching succeeded, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the reason why matching failed.
     *
     * @return optional failure reason, empty if match succeeded or no attempt was made
     */
    public Optional<String> getFailureReason() {
        return failureReason;
    }
}
