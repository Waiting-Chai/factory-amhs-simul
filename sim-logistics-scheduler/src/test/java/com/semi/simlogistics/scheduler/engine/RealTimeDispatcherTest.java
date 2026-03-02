package com.semi.simlogistics.scheduler.engine;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.scheduler.matching.VehicleMatcher;
import com.semi.simlogistics.scheduler.port.DispatchNotificationPort;
import com.semi.simlogistics.scheduler.pool.VehiclePool;
import com.semi.simlogistics.scheduler.rule.DispatchRule;
import com.semi.simlogistics.scheduler.rule.TaskSelectionRule;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskPriority;
import com.semi.simlogistics.scheduler.task.TaskQueue;
import com.semi.simlogistics.scheduler.task.TaskStatus;
import com.semi.simlogistics.scheduler.task.TaskType;
import com.semi.simlogistics.scheduler.test.TestDispatchRules;
import com.semi.simlogistics.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for RealTimeDispatcher (REQ-DS-005).
 * Tests runtime task management capabilities.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("RealTimeDispatcher Tests (REQ-DS-005)")
class RealTimeDispatcherTest {

    private TaskQueue taskQueue;
    private List<Vehicle> vehicles;
    private VehiclePool vehiclePool;
    private DispatchEngine dispatchEngine;
    private DispatchNotificationPort notificationPort;
    private RealTimeDispatcher dispatcher;
    private DispatchEngineConfig config;
    private VehicleMatcher matcher;

    @BeforeEach
    void setUp() {
        taskQueue = new TaskQueue();
        vehicles = new ArrayList<>();
        vehiclePool = new VehiclePool(vehicles);
        notificationPort = new TestDispatchRules.NoOpNotificationPort();

        config = new DispatchEngineConfig(
                1000L,  // dispatchIntervalMs
                3,      // maxRetryCount
                true    // autoStartTasks
        );

        // Use simple test rules
        DispatchRule dispatchRule = new TestDispatchRules.FirstVehicleRule();
        TaskSelectionRule taskSelectionRule = new TestDispatchRules.FirstTaskRule();
        matcher = new VehicleMatcher(dispatchRule, taskSelectionRule);

        // Create dispatcher with new API (creates DispatchEngine internally)
        dispatcher = new RealTimeDispatcher(taskQueue, vehicles, matcher, vehiclePool, notificationPort, config, false);
        dispatchEngine = dispatcher.getDispatchEngine();
    }

    // ========== Test Helpers ==========

    private Task createTask(String id, TaskPriority priority) {
        return new Task(id, "SIM-001", TaskType.TRANSPORT,
                new Position(0.0, 0.0, 0.0),
                new Position(100.0, 100.0, 0.0),
                priority);
    }

    private TestVehicle createVehicle(String id, Position position) {
        TestVehicle vehicle = new TestVehicle(id, "Vehicle " + id, position);
        vehicle.setState(VehicleState.IDLE);
        return vehicle;
    }

    /**
     * Test implementation of Vehicle for testing.
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
    @DisplayName("Runtime Task Addition Tests (REQ-DS-005)")
    class RuntimeTaskAdditionTests {

        @Test
        @DisplayName("Should process runtime added task in next cycle")
        void shouldProcessRuntimeAddedTaskInNextCycle() {
            // Given: An available vehicle and initial dispatch cycle with no tasks
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            // When: Add a task at runtime
            Task runtimeTask = createTask("TASK-RUNTIME", TaskPriority.NORMAL);
            dispatcher.addTask(runtimeTask, false);

            // Then: Task should be in queue
            assertThat(taskQueue.size()).isGreaterThan(0);

            // When: Execute next dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Runtime task should be assigned
            assertThat(result.getAssignedCount()).isEqualTo(1);
            assertThat(runtimeTask.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);
            assertThat(runtimeTask.getAssignedVehicleId()).isEqualTo(vehicle.id());
        }

        @Test
        @DisplayName("Should process multiple runtime added tasks in sequence")
        void shouldProcessMultipleRuntimeAddedTasksInSequence() {
            // Given: Two available vehicles
            TestVehicle vehicle1 = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicle2 = createVehicle("V-002", new Position(10.0, 10.0, 0.0));
            vehicles.add(vehicle1);
            vehicles.add(vehicle2);

            // When: Add multiple tasks at runtime
            Task task1 = createTask("TASK-001", TaskPriority.NORMAL);
            Task task2 = createTask("TASK-002", TaskPriority.NORMAL);
            dispatcher.addTask(task1, false);
            dispatcher.addTask(task2, false);

            // Then: Both tasks should be in queue
            assertThat(taskQueue.size()).isGreaterThanOrEqualTo(2);

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Both tasks should be assigned
            assertThat(result.getAssignedCount()).isEqualTo(2);
            assertThat(task1.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);
            assertThat(task2.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Urgent Task Priority Tests (REQ-DS-005)")
    class UrgentTaskPriorityTests {

        @Test
        @DisplayName("Should prioritize urgent task over normal tasks")
        void shouldPrioritizeUrgentTaskOverNormalTasks() {
            // Given: One vehicle and multiple tasks with different priorities
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            // Add normal tasks first
            Task normalTask1 = createTask("TASK-NORMAL-1", TaskPriority.NORMAL);
            Task normalTask2 = createTask("TASK-NORMAL-2", TaskPriority.NORMAL);
            taskQueue.enqueue(normalTask1);
            taskQueue.enqueue(normalTask2);

            // When: Add urgent task at runtime
            Task urgentTask = createTask("TASK-URGENT", TaskPriority.URGENT);
            dispatcher.addTask(urgentTask, true);

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Only one task should be assigned (one vehicle)
            assertThat(result.getAssignedCount()).isEqualTo(1);

            // And: Urgent task should be assigned (highest priority in queue)
            assertThat(urgentTask.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);

            // And: Normal tasks should remain in queue
            assertThat(taskQueue.size()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should prioritize critical task over urgent and normal tasks")
        void shouldPrioritizeCriticalTaskOverUrgentAndNormalTasks() {
            // Given: One vehicle and multiple tasks with different priorities
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            // Add urgent and normal tasks first
            Task urgentTask = createTask("TASK-URGENT", TaskPriority.URGENT);
            Task normalTask = createTask("TASK-NORMAL", TaskPriority.NORMAL);
            taskQueue.enqueue(urgentTask);
            taskQueue.enqueue(normalTask);

            // When: Add critical task at runtime
            Task criticalTask = createTask("TASK-CRITICAL", TaskPriority.CRITICAL);
            dispatcher.addTask(criticalTask, true);

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Critical task should be assigned (highest priority)
            assertThat(result.getAssignedCount()).isEqualTo(1);
            assertThat(criticalTask.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Manual Assignment Tests (REQ-DS-005)")
    class ManualAssignmentTests {

        @Test
        @DisplayName("Should not override manual assignment")
        void shouldNotOverrideManualAssignment() {
            // Given: Two vehicles and one task
            TestVehicle vehicle1 = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicle2 = createVehicle("V-002", new Position(10.0, 10.0, 0.0));
            vehicles.add(vehicle1);
            vehicles.add(vehicle2);

            Task task = createTask("TASK-MANUAL", TaskPriority.NORMAL);

            // When: Manually assign task to vehicle1
            boolean assigned = dispatcher.manualAssign(task, "V-001");

            // Then: Assignment should succeed
            assertThat(assigned).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
            assertThat(task.getAssignedVehicleId()).isEqualTo("V-001");

            // And: Vehicle1 should be marked as manually assigned
            assertThat(vehiclePool.isManuallyAssigned("V-001")).isTrue();

            // And: Task should be locked
            assertThat(vehiclePool.isTaskLocked("TASK-MANUAL")).isTrue();

            // When: Execute dispatch cycle (automatic dispatch)
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: No additional assignments should be made
            // (task already assigned and locked)
            assertThat(result.getAssignedCount()).isEqualTo(0);

            // And: Task assignment should not be overridden
            assertThat(task.getAssignedVehicleId()).isEqualTo("V-001");
        }

        @Test
        @DisplayName("Should exclude manually assigned vehicle from automatic dispatch")
        void shouldExcludeManuallyAssignedVehicleFromAutomaticDispatch() {
            // Given: Two vehicles and one task
            TestVehicle vehicle1 = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicle2 = createVehicle("V-002", new Position(10.0, 10.0, 0.0));
            vehicles.add(vehicle1);
            vehicles.add(vehicle2);

            // When: Mark vehicle1 as manually assigned
            vehiclePool.markManuallyAssigned("V-001");

            // And: Add a task
            Task task = createTask("TASK-AUTO", TaskPriority.NORMAL);
            taskQueue.enqueue(task);

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Task should be assigned to vehicle2 (not the manually assigned vehicle1)
            assertThat(result.getAssignedCount()).isEqualTo(1);
            assertThat(task.getAssignedVehicleId()).isEqualTo("V-002");
        }

        @Test
        @DisplayName("Should fail manual assignment when vehicle not found")
        void shouldFailManualAssignmentWhenVehicleNotFound() {
            // Given: A task but no vehicles
            Task task = createTask("TASK-INVALID", TaskPriority.NORMAL);

            // When: Try to manually assign to non-existent vehicle
            boolean assigned = dispatcher.manualAssign(task, "V-NONEXISTENT");

            // Then: Assignment should fail
            assertThat(assigned).isFalse();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        }

        @Test
        @DisplayName("Should fail manual assignment when task not in PENDING state")
        void shouldFailManualAssignmentWhenTaskNotPending() {
            // Given: A vehicle and a task already in ASSIGNED state
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            Task task = createTask("TASK-ASSIGNED", TaskPriority.NORMAL);
            task.assignTo("V-OTHER"); // Already assigned

            // When: Try to manually assign an already assigned task
            boolean assigned = dispatcher.manualAssign(task, "V-001");

            // Then: Assignment should fail
            assertThat(assigned).isFalse();
            assertThat(task.getAssignedVehicleId()).isEqualTo("V-OTHER"); // Unchanged
        }
    }

    @Nested
    @DisplayName("Preemption Tests")
    class PreemptionTests {

        @Test
        @DisplayName("Should not preempt when preemption is disabled")
        void shouldNotPreemptWhenPreemptionIsDisabled() {
            // Given: Dispatcher with preemption disabled (default)
            assertThat(dispatcher.isPreemptionEnabled()).isFalse();

            // This test documents the current behavior
            // Preemption logic would be implemented in DispatchEngine
            // For now, we verify the flag is correctly set
        }

        @Test
        @DisplayName("Should support preemption when enabled")
        void shouldSupportPreemptionWhenEnabled() {
            // Given: Dispatcher with preemption enabled
            RealTimeDispatcher preemptDispatcher = new RealTimeDispatcher(
                    taskQueue, vehicles, matcher, vehiclePool, notificationPort, true);

            // Then: Preemption should be enabled
            assertThat(preemptDispatcher.isPreemptionEnabled()).isTrue();

            // Actual preemption logic would be in DispatchEngine
            // This test verifies the flag is correctly set
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null task gracefully")
        void shouldHandleNullTaskGracefully() {
            // When/Then: Adding null task should throw NPE
            org.junit.jupiter.api.Assertions.assertThrows(
                    NullPointerException.class,
                    () -> dispatcher.addTask(null, false)
            );
        }

        @Test
        @DisplayName("Should handle empty queue dispatch")
        void shouldHandleEmptyQueueDispatch() {
            // Given: No tasks in queue
            assertThat(taskQueue.isEmpty()).isTrue();

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Should complete with no assignments
            assertThat(result.getAssignedCount()).isEqualTo(0);
            assertThat(result.getUnassignedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not process locked tasks in automatic dispatch")
        void shouldNotProcessLockedTasksInAutomaticDispatch() {
            // Given: A vehicle and a locked task
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            Task lockedTask = createTask("TASK-LOCKED", TaskPriority.NORMAL);
            vehiclePool.lockTask("TASK-LOCKED");

            // When: Add locked task (should be skipped)
            dispatcher.addTask(lockedTask, false);

            // And: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Task should not be assigned (locked)
            assertThat(result.getAssignedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should manual assign by task ID successfully")
        void shouldManualAssignByTaskIdSuccessfully() {
            // Given: A vehicle and a task in queue
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            Task task = createTask("TASK-MANUAL-ID", TaskPriority.NORMAL);
            dispatcher.addTask(task, false);

            // When: Manual assign by task ID
            boolean assigned = dispatcher.manualAssign("TASK-MANUAL-ID", "V-001");

            // Then: Assignment should succeed
            assertThat(assigned).isTrue();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
            assertThat(task.getAssignedVehicleId()).isEqualTo("V-001");

            // And: Vehicle should be marked as manually assigned
            assertThat(vehiclePool.isManuallyAssigned("V-001")).isTrue();

            // And: Task should be locked
            assertThat(vehiclePool.isTaskLocked("TASK-MANUAL-ID")).isTrue();
        }
    }

    @Nested
    @DisplayName("Urgent Task Behavior Tests (REQ-DS-005)")
    class UrgentTaskBehaviorTests {

        @Test
        @DisplayName("Should prioritize urgent when added as normal task with urgent flag")
        void shouldPrioritizeUrgentWhenAddedAsNormalTaskWithUrgentFlag() {
            // Given: One vehicle and two tasks with different priorities
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            // Add a normal task first
            Task normalTask = createTask("TASK-NORMAL", TaskPriority.NORMAL);
            dispatcher.addTask(normalTask, false);

            // Add another normal task with urgent flag
            Task urgentFlagTask = createTask("TASK-URGENT-FLAG", TaskPriority.NORMAL);
            dispatcher.addTask(urgentFlagTask, true);

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Only one task should be assigned (one vehicle)
            assertThat(result.getAssignedCount()).isEqualTo(1);

            // And: The urgent flag task should be assigned (it has URGENT priority via enqueueUrgent)
            assertThat(urgentFlagTask.getStatus()).isIn(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);
            assertThat(urgentFlagTask.getAssignedVehicleId()).isEqualTo("V-001");

            // And: Normal task should remain in queue
            assertThat(normalTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Simulation Time Tests")
    class SimulationTimeTests {

        @Test
        @DisplayName("Should use provided simulation time in onDispatchCycle overload")
        void shouldUseProvidedSimulationTimeInOnDispatchCycleOverload() {
            // Given: Vehicles and tasks
            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);

            dispatcher.addTask(createTask("TASK-001", TaskPriority.NORMAL), false);

            // When: Execute dispatch cycle with explicit simulation time
            double providedSimTime = 123.456;
            DispatchResult result = dispatcher.onDispatchCycle(providedSimTime);

            // Then: Dispatch should complete successfully
            assertThat(result.getAssignedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should pass override simulation time to DispatchContext.getCurrentTime()")
        void shouldPassOverrideSimulationTimeToDispatchContext() {
            // Given: Time-capturing rule and dispatcher with custom matcher
            TestDispatchRules.TimeCapturingRule timeCapturingRule = new TestDispatchRules.TimeCapturingRule();
            VehicleMatcher timeCapturingMatcher = new VehicleMatcher(
                    timeCapturingRule,
                    new TestDispatchRules.FirstTaskRule()
            );

            RealTimeDispatcher timeCapturingDispatcher = new RealTimeDispatcher(
                    taskQueue, vehicles, timeCapturingMatcher, vehiclePool,
                    notificationPort, config, false
            );

            // Create vehicle and task
            TestVehicle vehicle = createVehicle("V-TIME-TEST", new Position(0.0, 0.0, 0.0));
            vehicles.add(vehicle);
            timeCapturingDispatcher.addTask(createTask("TASK-TIME-TEST", TaskPriority.NORMAL), false);

            // When: Execute dispatch cycle with explicit simulation time
            double providedSimTime = 123.456;
            timeCapturingDispatcher.onDispatchCycle(providedSimTime);

            // Then: The captured time from DispatchContext should match the provided time
            // Conversion: 123.456 seconds * 1000 = 123456 milliseconds (truncated)
            Long capturedTime = timeCapturingRule.getCapturedTimeMillis();
            assertThat(capturedTime).isNotNull();
            assertThat(capturedTime.longValue()).isEqualTo(123456L); // 123.456 * 1000, truncated
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should integrate with DispatchEngine for dispatch cycles")
        void shouldIntegrateWithDispatchEngineForDispatchCycles() {
            // Given: Vehicles and tasks
            TestVehicle vehicle1 = createVehicle("V-001", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicle2 = createVehicle("V-002", new Position(10.0, 10.0, 0.0));
            vehicles.add(vehicle1);
            vehicles.add(vehicle2);

            // Add tasks via dispatcher
            dispatcher.addTask(createTask("TASK-001", TaskPriority.NORMAL), false);
            dispatcher.addTask(createTask("TASK-002", TaskPriority.HIGH), false);

            // When: Execute dispatch cycle via dispatcher
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Should delegate to DispatchEngine successfully
            assertThat(result.getAssignedCount()).isEqualTo(2);
        }
    }
}
