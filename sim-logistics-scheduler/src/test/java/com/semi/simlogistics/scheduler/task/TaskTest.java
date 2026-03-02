package com.semi.simlogistics.scheduler.task;

import com.semi.simlogistics.core.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for Task model (REQ-DS-001).
 * These tests define the expected behavior of the Task class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("Task Model Tests (REQ-DS-001)")
class TaskTest {

    @Nested
    @DisplayName("Task Creation Tests")
    class TaskCreationTests {

        @Test
        @DisplayName("Should create task with default PENDING status")
        void shouldCreateTaskWithPendingStatus() {
            // Given: Task parameters
            String id = "TASK-001";
            String simulationId = "SIM-001";
            TaskType type = TaskType.TRANSPORT;
            Position source = new Position(0.0, 0.0, 0.0);
            Position destination = new Position(10.0, 10.0, 0.0);
            TaskPriority priority = TaskPriority.NORMAL;

            // When: Create task
            Task task = new Task(id, simulationId, type, source, destination, priority);

            // Then: Task should have PENDING status
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(task.getId()).isEqualTo(id);
            assertThat(task.getSimulationId()).isEqualTo(simulationId);
            assertThat(task.getTaskType()).isEqualTo(type);
            assertThat(task.getPriority()).isEqualTo(priority);
        }

        @Test
        @DisplayName("Should record creation time")
        void shouldRecordCreationTime() {
            // Given
            long beforeCreation = System.currentTimeMillis();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            long afterCreation = System.currentTimeMillis();

            // Then: Creation time should be between before and after
            assertThat(task.getCreatedTime()).isGreaterThanOrEqualTo(beforeCreation);
            assertThat(task.getCreatedTime()).isLessThanOrEqualTo(afterCreation);
        }

        @Test
        @DisplayName("Should support optional cargo info")
        void shouldSupportOptionalCargoInfo() {
            // Given
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            // When: Set cargo info
            Map<String, Object> cargoInfo = Map.of(
                    "cargoId", "CARGO-001",
                    "type", "WAFER",
                    "weight", 5.0
            );
            task.setCargoInfo(cargoInfo);

            // Then: Cargo info should be retrievable
            assertThat(task.getCargoInfo()).isNotNull();
            assertThat(task.getCargoInfo()).containsEntry("cargoId", "CARGO-001");
        }
    }

    @Nested
    @DisplayName("Task State Machine Tests")
    class StateMachineTests {

        @Test
        @DisplayName("Should transition from PENDING to ASSIGNED")
        void shouldTransitionPendingToAssigned() {
            // Given: A PENDING task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            // When: Assign to vehicle
            boolean success = task.assignTo("VEHICLE-001");

            // Then: State should transition to ASSIGNED
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
            assertThat(task.getAssignedVehicleId()).isEqualTo("VEHICLE-001");
        }

        @Test
        @DisplayName("Should transition from ASSIGNED to IN_PROGRESS")
        void shouldTransitionAssignedToInProgress() {
            // Given: An ASSIGNED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");

            // When: Start execution
            boolean success = task.start();

            // Then: State should transition to IN_PROGRESS
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.getStartedTime()).isNotNull();
        }

        @Test
        @DisplayName("Should transition from IN_PROGRESS to COMPLETED")
        void shouldTransitionInProgressToCompleted() {
            // Given: An IN_PROGRESS task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");
            task.start();

            // When: Complete task
            boolean success = task.complete();

            // Then: State should transition to COMPLETED
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(task.getCompletedTime()).isNotNull();
        }

        @Test
        @DisplayName("Should reject invalid state transition: PENDING to IN_PROGRESS")
        void shouldRejectInvalidTransitionPendingToInProgress() {
            // Given: A PENDING task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            // When: Try to start without assigning
            boolean success = task.start();

            // Then: Should fail
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        }

        @Test
        @DisplayName("Should reject invalid state transition: ASSIGNED to COMPLETED")
        void shouldRejectInvalidTransitionAssignedToCompleted() {
            // Given: An ASSIGNED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");

            // When: Try to complete without starting
            boolean success = task.complete();

            // Then: Should fail
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        }
    }

    @Nested
    @DisplayName("Task Cancellation Tests")
    class CancellationTests {

        @Test
        @DisplayName("Should cancel PENDING task")
        void shouldCancelPendingTask() {
            // Given: A PENDING task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            // When: Cancel task
            boolean success = task.cancel();

            // Then: State should be CANCELLED
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should cancel ASSIGNED task")
        void shouldCancelAssignedTask() {
            // Given: An ASSIGNED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");

            // When: Cancel task
            boolean success = task.cancel();

            // Then: State should be CANCELLED
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should mark IN_PROGRESS task for cancellation (with interruption flag)")
        void shouldMarkInProgressTaskForCancellation() {
            // Given: An IN_PROGRESS task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");
            task.start();

            // When: Cancel task
            boolean success = task.cancel();

            // Then: Should mark for interruption (not immediate cancellation)
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLING);
            assertThat(task.isInterruptionRequested()).isTrue();
        }

        @Test
        @DisplayName("Should not cancel COMPLETED task")
        void shouldNotCancelCompletedTask() {
            // Given: A COMPLETED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");
            task.start();
            task.complete();

            // When: Try to cancel
            boolean success = task.cancel();

            // Then: Should fail
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should not cancel already CANCELLED task")
        void shouldNotCancelAlreadyCancelledTask() {
            // Given: A CANCELLED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.cancel();

            // When: Try to cancel again
            boolean success = task.cancel();

            // Then: Should fail
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Task Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should record error message")
        void shouldRecordErrorMessage() {
            // Given: A task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            // When: Fail task with error
            boolean success = task.fail("Vehicle malfunction");

            // Then: Error should be recorded
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
            assertThat(task.getErrorMessage()).isEqualTo("Vehicle malfunction");
        }

        @Test
        @DisplayName("Should not fail COMPLETED task (terminal state)")
        void shouldNotFailCompletedTask() {
            // Given: A COMPLETED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");
            task.start();
            task.complete();

            // When: Try to fail completed task
            boolean success = task.fail("Post-completion error");

            // Then: Should fail, status unchanged
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            assertThat(task.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should not fail CANCELLED task (terminal state)")
        void shouldNotFailCancelledTask() {
            // Given: A CANCELLED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.cancel();

            // When: Try to fail cancelled task
            boolean success = task.fail("Cancellation error");

            // Then: Should fail, status unchanged
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
            assertThat(task.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should not fail already FAILED task (terminal state)")
        void shouldNotFailAlreadyFailedTask() {
            // Given: A FAILED task
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.fail("Original error");

            // When: Try to fail again
            boolean success = task.fail("Second error");

            // Then: Should fail, status unchanged
            assertThat(success).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
            assertThat(task.getErrorMessage()).isEqualTo("Original error");
        }

        @Test
        @DisplayName("Should fail from CANCELLING state")
        void shouldFailFromCancellingState() {
            // Given: A CANCELLING task (was IN_PROGRESS)
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            task.assignTo("VEHICLE-001");
            task.start();
            task.cancel(); // Transitions to CANCELLING

            // When: Fail from CANCELLING
            boolean success = task.fail("Failed during cancellation");

            // Then: Should succeed
            assertThat(success).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
            assertThat(task.getErrorMessage()).isEqualTo("Failed during cancellation");
        }
    }

    @Nested
    @DisplayName("Task Constructor Validation Tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Should throw NPE for null id")
        void shouldThrowNPEForNullId() {
            assertThatThrownBy(() -> new Task(null, "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Task id cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null simulationId")
        void shouldThrowNPEForNullSimulationId() {
            assertThatThrownBy(() -> new Task("TASK-001", null, TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Simulation id cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null taskType")
        void shouldThrowNPEForNullTaskType() {
            assertThatThrownBy(() -> new Task("TASK-001", "SIM-001", null,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Task type cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null source")
        void shouldThrowNPEForNullSource() {
            assertThatThrownBy(() -> new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    null,
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Source position cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null destination")
        void shouldThrowNPEForNullDestination() {
            assertThatThrownBy(() -> new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    null,
                    TaskPriority.NORMAL))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Destination position cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null priority")
        void shouldThrowNPEForNullPriority() {
            assertThatThrownBy(() -> new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Task priority cannot be null");
        }

        @Test
        @DisplayName("Should throw IAE for blank id (empty string)")
        void shouldThrowIAEForBlankIdEmpty() {
            assertThatThrownBy(() -> new Task("", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Task id cannot be blank");
        }

        @Test
        @DisplayName("Should throw IAE for blank id (whitespace)")
        void shouldThrowIAEForBlankIdWhitespace() {
            assertThatThrownBy(() -> new Task("  ", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Task id cannot be blank");
        }

        @Test
        @DisplayName("Should throw IAE for blank simulationId (empty string)")
        void shouldThrowIAEForBlankSimulationIdEmpty() {
            assertThatThrownBy(() -> new Task("TASK-001", "", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Simulation id cannot be blank");
        }

        @Test
        @DisplayName("Should throw IAE for blank simulationId (whitespace)")
        void shouldThrowIAEForBlankSimulationIdWhitespace() {
            assertThatThrownBy(() -> new Task("TASK-001", "  \t  ", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Simulation id cannot be blank");
        }

        @Test
        @DisplayName("Should throw NPE for null vehicleId in assignTo")
        void shouldThrowNPEForNullVehicleId() {
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            assertThatThrownBy(() -> task.assignTo(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Vehicle id cannot be null");
        }

        @Test
        @DisplayName("Should throw IAE for blank vehicleId in assignTo")
        void shouldThrowIAEForBlankVehicleId() {
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            assertThatThrownBy(() -> task.assignTo("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Vehicle id cannot be blank");
        }
    }
}
