package com.semi.simlogistics.scheduler.replan;

import java.util.Optional;

/**
 * Result of a replanning operation (REQ-DS-006).
 * <p>
 * Encapsulates the outcome of a replanning attempt, including
 * success/failure status, the new path if successful, and error information.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class ReplanResult {

    private final boolean success;
    private final Optional<String> newPath;
    private final Optional<String> errorMessage;
    private final ReplanTrigger trigger;

    private ReplanResult(boolean success, String newPath, String errorMessage, ReplanTrigger trigger) {
        this.success = success;
        this.newPath = Optional.ofNullable(newPath);
        this.errorMessage = Optional.ofNullable(errorMessage);
        this.trigger = trigger;
    }

    /**
     * Create a successful replan result.
     *
     * @param newPath the new planned path
     * @param trigger the trigger that caused this replan
     * @return successful result
     */
    public static ReplanResult success(String newPath, ReplanTrigger trigger) {
        return new ReplanResult(true, newPath, null, trigger);
    }

    /**
     * Create a failed replan result.
     *
     * @param errorMessage error description
     * @param trigger the trigger that caused this replan attempt
     * @return failed result
     */
    public static ReplanResult failure(String errorMessage, ReplanTrigger trigger) {
        return new ReplanResult(false, null, errorMessage, trigger);
    }

    /**
     * Check if replanning was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the new planned path.
     *
     * @return optional path, empty if replanning failed
     */
    public Optional<String> getNewPath() {
        return newPath;
    }

    /**
     * Get error message if replanning failed.
     *
     * @return optional error message, empty if successful
     */
    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get the trigger that caused this replan.
     *
     * @return the replan trigger
     */
    public ReplanTrigger getTrigger() {
        return trigger;
    }

    @Override
    public String toString() {
        if (success) {
            return "ReplanResult[success, path=" + newPath.orElse("") + ", trigger=" + trigger + "]";
        }
        return "ReplanResult[failed, error=" + errorMessage.orElse("") + ", trigger=" + trigger + "]";
    }
}
