package com.semi.simlogistics.scheduler.task;

import com.semi.simlogistics.core.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for TaskQueue (REQ-DS-002).
 * These tests define the expected behavior of the priority-based task queue.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("TaskQueue Tests (REQ-DS-002)")
class TaskQueueTest {

    /**
     * Helper to create a Task with explicit createdTime for testing.
     * Uses reflection to access package-visible test constructor.
     */
    private Task createTaskWithCreatedTime(String id, TaskPriority priority, long createdTime) {
        try {
            Constructor<Task> ctor = Task.class.getDeclaredConstructor(
                    String.class, String.class, TaskType.class,
                    Position.class, Position.class, TaskPriority.class, long.class);
            ctor.setAccessible(true);
            return ctor.newInstance(id, "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    priority, createdTime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test task with explicit createdTime", e);
        }
    }

    @Nested
    @DisplayName("Priority Ordering Tests")
    class PriorityOrderingTests {

        @Test
        @DisplayName("Should enqueue tasks in priority order (highest first)")
        void shouldEnqueueTasksInPriorityOrder() {
            // Given: Empty queue
            TaskQueue queue = new TaskQueue();

            // When: Add tasks with different priorities
            Task normalTask = createTask("TASK-001", TaskPriority.NORMAL);
            Task highTask = createTask("TASK-002", TaskPriority.HIGH);
            Task lowTask = createTask("TASK-003", TaskPriority.LOW);

            queue.enqueue(normalTask);
            queue.enqueue(highTask);
            queue.enqueue(lowTask);

            // Then: Peek should return highest priority task
            assertThat(queue.peekNext()).isSameAs(highTask);
            assertThat(queue.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should poll tasks in priority order")
        void shouldPollTasksInPriorityOrder() {
            // Given: Queue with mixed priorities
            TaskQueue queue = new TaskQueue();
            queue.enqueue(createTask("TASK-001", TaskPriority.NORMAL));
            queue.enqueue(createTask("TASK-002", TaskPriority.HIGH));
            queue.enqueue(createTask("TASK-003", TaskPriority.LOW));
            queue.enqueue(createTask("TASK-004", TaskPriority.URGENT));

            // When: Poll tasks
            Task first = queue.pollNext();
            Task second = queue.pollNext();
            Task third = queue.pollNext();
            Task fourth = queue.pollNext();

            // Then: Should return in priority order
            assertThat(first.getId()).isEqualTo("TASK-004"); // URGENT
            assertThat(second.getId()).isEqualTo("TASK-002"); // HIGH
            assertThat(third.getId()).isEqualTo("TASK-001"); // NORMAL
            assertThat(fourth.getId()).isEqualTo("TASK-003"); // LOW
        }
    }

    @Nested
    @DisplayName("FIFO Ordering Tests (Same Priority)")
    class FIFOOrderingTests {

        @Test
        @DisplayName("Should order same-priority tasks by creation time (FIFO)")
        void shouldOrderSamePriorityByCreationTime() {
            // Given: Queue
            TaskQueue queue = new TaskQueue();

            // When: Add tasks with same priority (different IDs ensure FIFO)
            Task task1 = createTask("TASK-001", TaskPriority.NORMAL);
            Task task2 = createTask("TASK-002", TaskPriority.NORMAL);
            Task task3 = createTask("TASK-003", TaskPriority.NORMAL);

            queue.enqueue(task1);
            queue.enqueue(task2);
            queue.enqueue(task3);

            // Then: Should respect FIFO order (id tie-breaker ensures this)
            assertThat(queue.pollNext()).isSameAs(task1);
            assertThat(queue.pollNext()).isSameAs(task2);
            assertThat(queue.pollNext()).isSameAs(task3);
        }
    }

    @Nested
    @DisplayName("Task Loss Prevention Tests")
    class TaskLossPreventionTests {

        @Test
        @DisplayName("Should not lose tasks with same priority and createdTime (id tie-breaker)")
        void shouldNotLoseTasksWithSamePriorityAndCreatedTime() {
            // Given: Queue
            TaskQueue queue = new TaskQueue();

            // When: Add multiple tasks with same priority
            // Even if createdTime is same (unlikely but possible), id ensures uniqueness
            Task task1 = createTask("TASK-A", TaskPriority.NORMAL);
            Task task2 = createTask("TASK-B", TaskPriority.NORMAL);
            Task task3 = createTask("TASK-C", TaskPriority.NORMAL);

            queue.enqueue(task1);
            queue.enqueue(task2);
            queue.enqueue(task3);

            // Then: All tasks should be enqueued (no loss due to Set deduplication)
            assertThat(queue.size()).isEqualTo(3);

            // And: All tasks should be retrievable in FIFO order
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-A");
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-B");
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-C");
            assertThat(queue.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Should handle identical priority, createdTime with stable id ordering (explicit test)")
        void shouldHandleIdenticalPriorityAndCreatedTimeWithStableIdOrdering() {
            // Given: Queue
            TaskQueue queue = new TaskQueue();

            // When: Create tasks with EXPLICIT same createdTime using test constructor
            // This is the critical regression test for task loss prevention
            long sameTimestamp = 1000000L;
            Task task1 = createTaskWithCreatedTime("TASK-Z", TaskPriority.NORMAL, sameTimestamp);
            Task task2 = createTaskWithCreatedTime("TASK-A", TaskPriority.NORMAL, sameTimestamp);
            Task task3 = createTaskWithCreatedTime("TASK-M", TaskPriority.NORMAL, sameTimestamp);

            queue.enqueue(task1);
            queue.enqueue(task2);
            queue.enqueue(task3);

            // Then: All tasks must be enqueued (no loss from Set deduplication)
            assertThat(queue.size()).isEqualTo(3);

            // And: Order must be stable by id (A, M, Z)
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-A");
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-M");
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-Z");
            assertThat(queue.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Empty Queue Behavior Tests")
    class EmptyQueueBehaviorTests {

        @Test
        @DisplayName("Should return null when polling empty queue")
        void shouldReturnNullWhenPollingEmptyQueue() {
            // Given: Empty queue
            TaskQueue queue = new TaskQueue();

            // When: Poll from empty queue
            Task task = queue.pollNext();

            // Then: Should return null
            assertThat(task).isNull();
        }

        @Test
        @DisplayName("Should return null when peeking empty queue")
        void shouldReturnNullWhenPeekingEmptyQueue() {
            // Given: Empty queue
            TaskQueue queue = new TaskQueue();

            // When: Peek empty queue
            Task task = queue.peekNext();

            // Then: Should return null
            assertThat(task).isNull();
        }

        @Test
        @DisplayName("Should report empty correctly")
        void shouldReportEmptyCorrectly() {
            // Given: Empty queue
            TaskQueue queue = new TaskQueue();

            // Then: Should be empty
            assertThat(queue.isEmpty()).isTrue();
            assertThat(queue.size()).isEqualTo(0);

            // When: Add task
            queue.enqueue(createTask("TASK-001", TaskPriority.NORMAL));

            // Then: Should not be empty
            assertThat(queue.isEmpty()).isFalse();
            assertThat(queue.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Snapshot/Restore Tests")
    class SnapshotRestoreTests {

        @Test
        @DisplayName("Should create snapshot of queue state")
        void shouldCreateSnapshotOfQueueState() {
            // Given: Queue with tasks
            TaskQueue queue = new TaskQueue();
            Task task1 = createTask("TASK-001", TaskPriority.NORMAL);
            Task task2 = createTask("TASK-002", TaskPriority.HIGH);
            queue.enqueue(task1);
            queue.enqueue(task2);

            // When: Create snapshot
            TaskQueue.Snapshot snapshot = queue.toSnapshot();

            // Then: Snapshot should contain all tasks
            assertThat(snapshot.getTasks()).hasSize(2);
            assertThat(snapshot.getTasks()).extracting("id").containsExactly("TASK-002", "TASK-001");
        }

        @Test
        @DisplayName("Should restore queue from snapshot")
        void shouldRestoreQueueFromSnapshot() {
            // Given: Original queue with tasks
            TaskQueue original = new TaskQueue();
            original.enqueue(createTask("TASK-001", TaskPriority.NORMAL));
            original.enqueue(createTask("TASK-002", TaskPriority.HIGH));
            original.enqueue(createTask("TASK-003", TaskPriority.LOW));

            TaskQueue.Snapshot snapshot = original.toSnapshot();

            // When: Create new queue and restore
            TaskQueue restored = new TaskQueue();
            restored.restoreFromSnapshot(snapshot);

            // Then: Restored queue should have same state
            assertThat(restored.size()).isEqualTo(original.size());
            assertThat(restored.peekNext().getId()).isEqualTo(original.peekNext().getId());

            // Verify full order
            assertThat(restored.pollNext().getId()).isEqualTo("TASK-002"); // HIGH
            assertThat(restored.pollNext().getId()).isEqualTo("TASK-001"); // NORMAL
            assertThat(restored.pollNext().getId()).isEqualTo("TASK-003"); // LOW
        }

        @Test
        @DisplayName("Should restore empty queue from empty snapshot")
        void shouldRestoreEmptyQueueFromEmptySnapshot() {
            // Given: Empty queue
            TaskQueue original = new TaskQueue();
            TaskQueue.Snapshot snapshot = original.toSnapshot();

            // When: Restore
            TaskQueue restored = new TaskQueue();
            restored.restoreFromSnapshot(snapshot);

            // Then: Should be empty
            assertThat(restored.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Should handle enqueue after poll")
        void shouldHandleEnqueueAfterPoll() {
            // Given: Queue with tasks
            TaskQueue queue = new TaskQueue();
            queue.enqueue(createTask("TASK-001", TaskPriority.HIGH));
            queue.enqueue(createTask("TASK-002", TaskPriority.NORMAL));

            // When: Poll one task
            Task polled = queue.pollNext();
            assertThat(polled.getId()).isEqualTo("TASK-001");

            // And: Add new task
            queue.enqueue(createTask("TASK-003", TaskPriority.URGENT));

            // Then: New task should be at front
            assertThat(queue.peekNext().getId()).isEqualTo("TASK-003");
        }

        @Test
        @DisplayName("Should handle all priority levels")
        void shouldHandleAllPriorityLevels() {
            // Given: Queue
            TaskQueue queue = new TaskQueue();

            // When: Add tasks of all priority levels
            queue.enqueue(createTask("TASK-1", TaskPriority.LOWEST));
            queue.enqueue(createTask("TASK-2", TaskPriority.LOW));
            queue.enqueue(createTask("TASK-3", TaskPriority.NORMAL));
            queue.enqueue(createTask("TASK-4", TaskPriority.HIGH));
            queue.enqueue(createTask("TASK-5", TaskPriority.URGENT));
            queue.enqueue(createTask("TASK-6", TaskPriority.CRITICAL));

            // Then: Should order correctly
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-6"); // CRITICAL
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-5"); // URGENT
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-4"); // HIGH
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-3"); // NORMAL
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-2"); // LOW
            assertThat(queue.pollNext().getId()).isEqualTo("TASK-1"); // LOWEST
        }
    }

    // Helper methods

    private Task createTask(String id, TaskPriority priority) {
        return new Task(
                id,
                "SIM-001",
                TaskType.TRANSPORT,
                new Position(0.0, 0.0, 0.0),
                new Position(10.0, 10.0, 0.0),
                priority
        );
    }
}
