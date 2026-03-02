package com.semi.simlogistics.scheduler.exception;

/**
 * Exception thrown when task dispatch fails.
 * <p>
 * This exception indicates that a task could not be successfully assigned
 * to a vehicle or the assignment notification failed.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class TaskDispatchFailedException extends RuntimeException {

    private final String taskId;

    /**
     * Create a new task dispatch failed exception.
     *
     * @param message the detail message
     * @param taskId  the ID of the task that failed to dispatch
     */
    public TaskDispatchFailedException(String message, String taskId) {
        super(message);
        this.taskId = taskId;
    }

    /**
     * Create a new task dispatch failed exception with a cause.
     *
     * @param message the detail message
     * @param taskId  the ID of the task that failed to dispatch
     * @param cause   the cause of the failure
     */
    public TaskDispatchFailedException(String message, String taskId, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
    }

    /**
     * Get the ID of the task that failed to dispatch.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }
}
