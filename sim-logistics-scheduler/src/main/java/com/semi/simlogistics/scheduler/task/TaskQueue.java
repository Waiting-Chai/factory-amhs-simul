package com.semi.simlogistics.scheduler.task;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Priority-based task queue (REQ-DS-002).
 * <p>
 * Provides FIFO ordering for same-priority tasks and priority-based
 * ordering across different priorities (higher priority first).
 * <p>
 * Thread-safe implementation supporting concurrent access.
 * Supports in-memory snapshot/restore for simulation pause/resume.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class TaskQueue {

    /**
     * Internal comparator: priority (descending) -> creation time (ascending) -> id (ascending).
     * <p>
     * The id tie-breaker ensures total ordering and prevents task loss when
     * priority and createdTime are equal (ConcurrentSkipListSet would otherwise
     * treat such tasks as duplicates).
     */
    private static final Comparator<Task> TASK_COMPARATOR = (a, b) -> {
        int priorityCompare = Integer.compare(
                b.getPriority().getLevel(),
                a.getPriority().getLevel()
        );
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        int timeCompare = Long.compare(a.getCreatedTime(), b.getCreatedTime());
        if (timeCompare != 0) {
            return timeCompare;
        }
        return a.getId().compareTo(b.getId());
    };

    /**
     * Ordered set of tasks using the comparator.
     * Thread-safe for concurrent access.
     */
    private final ConcurrentSkipListSet<Task> tasks;

    /**
     * Create a new empty task queue.
     */
    public TaskQueue() {
        this.tasks = new ConcurrentSkipListSet<>(TASK_COMPARATOR);
    }

    /**
     * Add a task to the queue.
     *
     * @param task task to enqueue (must not be null)
     * @throws NullPointerException if task is null
     */
    public void enqueue(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        tasks.add(task);
    }

    /**
     * Remove and return the next highest-priority task.
     *
     * @return the next task, or null if queue is empty
     */
    public Task pollNext() {
        Iterator<Task> iterator = tasks.iterator();
        if (iterator.hasNext()) {
            Task task = iterator.next();
            iterator.remove();
            return task;
        }
        return null;
    }

    /**
     * Return (but do not remove) the next highest-priority task.
     *
     * @return the next task, or null if queue is empty
     */
    public Task peekNext() {
        Iterator<Task> iterator = tasks.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Get the current number of tasks in the queue.
     *
     * @return queue size
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Check if the queue is empty.
     *
     * @return true if queue has no tasks
     */
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Find a task by ID in the queue.
     * <p>
     * This is a utility method for manual assignment (REQ-DS-005).
     * Returns the task without removing it from the queue.
     *
     * @param taskId task ID to find
     * @return task if found, null otherwise
     */
    public Task getById(String taskId) {
        if (taskId == null) {
            return null;
        }
        return tasks.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add a task to the queue with URGENT priority (REQ-DS-005).
     * <p>
     * This method creates an urgent wrapper for the task that ensures
     * it is processed before NORMAL and lower priority tasks.
     * The task's original priority is preserved in the task object,
     * but the queue ordering treats it as urgent.
     *
     * @param task task to enqueue as urgent (must not be null)
     * @throws NullPointerException if task is null
     */
    public void enqueueUrgent(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        // Create an urgent wrapper that will be sorted by URGENT priority
        Task urgentTask = new UrgentTaskWrapper(task);
        tasks.add(urgentTask);
    }

    /**
     * Wrapper class for urgent tasks (REQ-DS-005).
     * <p>
     * This wrapper ensures that urgent tasks are processed before
     * non-urgent tasks, regardless of the original task's priority.
     * The wrapper delegates all state operations to the wrapped task.
     */
    private static class UrgentTaskWrapper extends Task {
        private final Task delegate;

        UrgentTaskWrapper(Task delegate) {
            super(delegate.getId(), delegate.getSimulationId(), delegate.getTaskType(),
                    delegate.getSource(), delegate.getDestination(),
                    TaskPriority.URGENT);
            this.delegate = delegate;
        }

        @Override
        public boolean assignTo(String vehicleId) {
            return delegate.assignTo(vehicleId);
        }

        @Override
        public boolean start() {
            return delegate.start();
        }

        @Override
        public boolean complete() {
            return delegate.complete();
        }

        @Override
        public boolean fail(String reason) {
            return delegate.fail(reason);
        }

        @Override
        public boolean cancel() {
            return delegate.cancel();
        }

        @Override
        public TaskStatus getStatus() {
            return delegate.getStatus();
        }

        @Override
        public String getAssignedVehicleId() {
            return delegate.getAssignedVehicleId();
        }

        @Override
        public long getCreatedTime() {
            return delegate.getCreatedTime();
        }
    }

    /**
     * Create an in-memory snapshot of the current queue state.
     * <p>
     * The snapshot contains a copy of all tasks in priority order.
     * Can be used for simulation pause/resume (no database persistence).
     *
     * @return snapshot of current queue state
     */
    public Snapshot toSnapshot() {
        return new Snapshot(new ArrayList<>(tasks));
    }

    /**
     * Restore queue state from a snapshot.
     * <p>
     * Clears current queue and replaces with tasks from snapshot.
     * Tasks are inserted in priority order to maintain queue semantics.
     *
     * @param snapshot snapshot to restore from (must not be null)
     * @throws NullPointerException if snapshot is null
     */
    public void restoreFromSnapshot(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        tasks.clear();
        tasks.addAll(snapshot.getTasks());
    }

    /**
     * Immutable snapshot of queue state.
     * <p>
     * Contains ordered list of tasks for serialization/restoration.
     */
    public static class Snapshot {
        private final List<Task> tasks;

        private Snapshot(List<Task> tasks) {
            this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        }

        /**
         * Get ordered list of tasks in this snapshot.
         *
         * @return unmodifiable list of tasks (priority-ordered)
         */
        public List<Task> getTasks() {
            return tasks;
        }
    }
}
