package com.semi.simlogistics.scheduler.rule;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskPriority;
import com.semi.simlogistics.scheduler.task.TaskType;
import com.semi.simlogistics.vehicle.Vehicle;
import com.semi.simlogistics.core.VehicleState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for HighestPriorityRule (REQ-DS-003).
 * Tests the task priority-based selection strategy.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("HighestPriorityRule Tests (REQ-DS-003)")
class HighestPriorityRuleTest {

    @Test
    @DisplayName("Should implement TaskSelectionRule interface")
    void shouldImplementTaskSelectionRule() {
        // Given: A rule instance
        HighestPriorityRule rule = new HighestPriorityRule();

        // Then: Should implement TaskSelectionRule (not DispatchRule)
        assertThat(rule).isInstanceOf(TaskSelectionRule.class);
        assertThat(rule).isNotInstanceOf(DispatchRule.class);
    }

    /**
     * Test implementation of DispatchContext.
     */
    private static class TestDispatchContext implements DispatchContext {
        @Override
        public double getDistance(Position from, Position to) {
            return 0.0; // Not used for priority rule
        }

        @Override
        public double getUtilization(String vehicleId) {
            return 0.5; // Not used for priority rule
        }

        @Override
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Test implementation of Vehicle.
     */
    private static class TestVehicle extends Vehicle {
        protected TestVehicle(String id, String name, Position position) {
            super(id, name, com.semi.simlogistics.core.EntityType.AGV_VEHICLE, position, null);
            setState(VehicleState.IDLE);
        }

        @Override
        protected Object run(com.semi.jSimul.core.Process.ProcessContext ctx) throws Exception {
            return null;
        }
    }

    @Nested
    @DisplayName("Task Selection Tests")
    class TaskSelectionTests {

        @Test
        @DisplayName("Should select highest priority task from list")
        void shouldSelectHighestPriorityTask() {
            // Given: A rule and multiple tasks with different priorities
            HighestPriorityRule rule = new HighestPriorityRule();

            Task normalTask = createTask("TASK-001", TaskPriority.NORMAL);
            Task highTask = createTask("TASK-002", TaskPriority.HIGH);
            Task lowTask = createTask("TASK-003", TaskPriority.LOW);

            List<Task> tasks = List.of(normalTask, highTask, lowTask);
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();

            // When: Select next task
            Optional<Task> result = rule.selectTask(tasks, vehicle, context);

            // Then: Should select HIGH priority task
            assertThat(result).isPresent();
            assertThat(result.get().getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(result.get().getId()).isEqualTo("TASK-002");
        }

        @Test
        @DisplayName("Should handle all priority levels correctly")
        void shouldHandleAllPriorityLevels() {
            // Given: Tasks with all priority levels
            HighestPriorityRule rule = new HighestPriorityRule();

            Task lowestTask = createTask("TASK-LOWEST", TaskPriority.LOWEST);
            Task lowTask = createTask("TASK-LOW", TaskPriority.LOW);
            Task normalTask = createTask("TASK-NORMAL", TaskPriority.NORMAL);
            Task highTask = createTask("TASK-HIGH", TaskPriority.HIGH);
            Task urgentTask = createTask("TASK-URGENT", TaskPriority.URGENT);
            Task criticalTask = createTask("TASK-CRITICAL", TaskPriority.CRITICAL);

            List<Task> tasks = List.of(normalTask, criticalTask, lowTask, urgentTask, highTask, lowestTask);
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();

            // When: Select next task
            Optional<Task> result = rule.selectTask(tasks, vehicle, context);

            // Then: Should select CRITICAL (highest) priority
            assertThat(result).isPresent();
            assertThat(result.get().getPriority()).isEqualTo(TaskPriority.CRITICAL);
            assertThat(result.get().getId()).isEqualTo("TASK-CRITICAL");
        }
    }

    @Nested
    @DisplayName("Tie-Breaking Tests")
    class TieBreakingTests {

        @Test
        @DisplayName("Should break ties by created time (earlier first)")
        void shouldBreakTiesByCreatedTime() {
            // Given: Multiple tasks with same priority but different creation times
            HighestPriorityRule rule = new HighestPriorityRule();

            long baseTime = System.currentTimeMillis();
            Task task1 = createTaskWithTime("TASK-001", TaskPriority.NORMAL, baseTime);
            Task task2 = createTaskWithTime("TASK-002", TaskPriority.NORMAL, baseTime + 1000); // Created later
            Task task3 = createTaskWithTime("TASK-003", TaskPriority.NORMAL, baseTime + 2000); // Created latest

            List<Task> tasks = List.of(task3, task1, task2); // Non-time order
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();

            // When: Select next task
            Optional<Task> result = rule.selectTask(tasks, vehicle, context);

            // Then: Should select earliest created task
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("TASK-001");
        }

        @Test
        @DisplayName("Should break ties by taskId when created time same")
        void shouldBreakTiesByTaskId() {
            // Given: Multiple tasks with same priority and created time
            HighestPriorityRule rule = new HighestPriorityRule();

            long sameTime = 1000000L;
            Task taskZ = createTaskWithTime("TASK-Z", TaskPriority.NORMAL, sameTime);
            Task taskA = createTaskWithTime("TASK-A", TaskPriority.NORMAL, sameTime);
            Task taskM = createTaskWithTime("TASK-M", TaskPriority.NORMAL, sameTime);

            List<Task> tasks = List.of(taskZ, taskA, taskM);
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();

            // When: Select next task
            Optional<Task> result = rule.selectTask(tasks, vehicle, context);

            // Then: Should select alphabetically first (TASK-A)
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("TASK-A");
        }
    }

    @Nested
    @DisplayName("Empty Input Tests")
    class EmptyInputTests {

        @Test
        @DisplayName("Should return empty for null task list")
        void shouldReturnEmptyForNullTasks() {
            // Given: A rule and null task list
            HighestPriorityRule rule = new HighestPriorityRule();
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();

            // When: Select from null tasks
            Optional<Task> result = rule.selectTask(null, vehicle, context);

            // Then: Should return empty
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for empty task list")
        void shouldReturnEmptyForEmptyTasks() {
            // Given: A rule and empty task list
            HighestPriorityRule rule = new HighestPriorityRule();
            List<Task> emptyTasks = List.of();
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();

            // When: Select from empty tasks
            Optional<Task> result = rule.selectTask(emptyTasks, vehicle, context);

            // Then: Should return empty
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NPE for null vehicle")
        void shouldThrowNPEForNullVehicle() {
            // Given: A rule
            HighestPriorityRule rule = new HighestPriorityRule();
            List<Task> tasks = List.of(createTask("TASK-001", TaskPriority.NORMAL));
            DispatchContext context = new TestDispatchContext();

            // When/Then: Should throw NPE
            assertThatThrownBy(() -> rule.selectTask(tasks, null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Vehicle cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null context")
        void shouldThrowNPEForNullContext() {
            // Given: A rule
            HighestPriorityRule rule = new HighestPriorityRule();
            List<Task> tasks = List.of(createTask("TASK-001", TaskPriority.NORMAL));
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));

            // When/Then: Should throw NPE
            assertThatThrownBy(() -> rule.selectTask(tasks, vehicle, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("DispatchContext cannot be null");
        }
    }

    @Nested
    @DisplayName("REQ-DS-003 Scenario Tests")
    class ScenarioTests {

        @Test
        @DisplayName("REQ-DS-003: Should prioritize high priority tasks")
        void shouldPrioritizeHighPriorityTasks() {
            // Scenario: 最高优先级规则
            // Given: 多个待分配任务
            Task urgentTask = createTask("TASK-URGENT", TaskPriority.URGENT);
            Task normalTask = createTask("TASK-NORMAL", TaskPriority.NORMAL);
            Task lowTask = createTask("TASK-LOW", TaskPriority.LOW);

            List<Task> tasks = List.of(normalTask, urgentTask, lowTask);
            // And: 有限车辆资源
            Vehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            DispatchContext context = new TestDispatchContext();
            HighestPriorityRule rule = new HighestPriorityRule();

            // When: 使用 HighestPriorityRule 分配
            Optional<Task> result = rule.selectTask(tasks, vehicle, context);

            // Then: 应优先分配高优先级任务
            assertThat(result).isPresent();
            assertThat(result.get().getPriority()).isEqualTo(TaskPriority.URGENT);
        }
    }

    // Helper methods

    private Task createTask(String id, TaskPriority priority) {
        return new Task(id, "SIM-001", TaskType.TRANSPORT,
                new Position(0.0, 0.0, 0.0),
                new Position(10.0, 10.0, 0.0),
                priority);
    }

    private Task createTaskWithTime(String id, TaskPriority priority, long createdTime) {
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
}
