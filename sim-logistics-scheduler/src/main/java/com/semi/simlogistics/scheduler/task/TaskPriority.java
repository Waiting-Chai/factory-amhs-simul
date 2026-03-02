package com.semi.simlogistics.scheduler.task;

/**
 * Task priority enumeration (REQ-DS-001).
 * <p>
 * Defines priority levels for task scheduling.
 * Higher priority tasks are scheduled first.
 * For same priority, FIFO ordering applies.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public enum TaskPriority {
    /**
     * Lowest priority (background tasks).
     */
    LOWEST(1),

    /**
     * Low priority.
     */
    LOW(2),

    /**
     * Normal/default priority.
     */
    NORMAL(3),

    /**
     * High priority.
     */
    HIGH(4),

    /**
     * Urgent priority (time-sensitive).
     */
    URGENT(5),

    /**
     * Critical priority (highest).
     */
    CRITICAL(6);

    private final int level;

    TaskPriority(int level) {
        this.level = level;
    }

    /**
     * Get the numeric priority level for comparison.
     *
     * @return priority level (higher = more important)
     */
    public int getLevel() {
        return level;
    }
}
