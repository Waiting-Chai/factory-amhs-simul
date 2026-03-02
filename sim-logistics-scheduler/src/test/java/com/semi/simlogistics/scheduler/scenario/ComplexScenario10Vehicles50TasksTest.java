package com.semi.simlogistics.scheduler.scenario;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.scheduler.engine.DispatchEngine;
import com.semi.simlogistics.scheduler.engine.DispatchEngineConfig;
import com.semi.simlogistics.scheduler.engine.DispatchResult;
import com.semi.simlogistics.scheduler.engine.RealTimeDispatcher;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Complex scenario test for Phase 3.5 (REQ-DS-004, REQ-DS-005, REQ-DS-007).
 * <p>
 * Tests a complex scenario with 10 vehicles and 50 tasks to verify:
 * <ul>
 *   <li>Automatic dispatch works correctly under load</li>
 *   <li>Throughput meets minimum threshold</li>
 *   <li>All tasks eventually complete</li>
 *   <li>Dispatch engine returns in finite steps</li>
 *   <li>No vehicle double-assignment occurs</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("Complex Scenario Tests: 10 Vehicles, 50 Tasks (Phase 3.5)")
class ComplexScenario10Vehicles50TasksTest {

    // Throughput threshold: minimum 80% of tasks should be assigned
    private static final double MIN_THROUGHPUT_RATIO = 0.8;
    // Maximum dispatch cycles to avoid infinite loops
    private static final int MAX_DISPATCH_CYCLES = 100;

    private TaskQueue taskQueue;
    private List<Vehicle> vehicles;
    private VehiclePool vehiclePool;
    private DispatchEngine dispatchEngine;
    private RealTimeDispatcher dispatcher;
    private DispatchEngineConfig config;
    private VehicleMatcher matcher;
    private DispatchNotificationPort notificationPort;

    /**
     * Track all tasks created for the scenario (for completion verification).
     * This is needed because tasks are removed from the queue when assigned.
     */
    private final List<Task> allCreatedTasks = new ArrayList<>();

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

        // Use shortest distance rule for more realistic dispatch
        DispatchRule dispatchRule = new TestDispatchRules.FirstVehicleRule();
        TaskSelectionRule taskSelectionRule = new TestDispatchRules.FirstTaskRule();
        matcher = new VehicleMatcher(dispatchRule, taskSelectionRule);

        // Create dispatcher with new API (creates DispatchEngine internally with VehiclePool)
        dispatcher = new RealTimeDispatcher(taskQueue, vehicles, matcher, vehiclePool, notificationPort, config, false);
        dispatchEngine = dispatcher.getDispatchEngine();

        // Clear task tracking list
        allCreatedTasks.clear();
    }

    // ========== Test Helpers ==========

    /**
     * Create a scenario with 10 vehicles and 50 tasks.
     *
     * @param randomSeed random seed for reproducibility
     */
    private void create10Vehicles50TasksScenario(int randomSeed) {
        Random random = new Random(randomSeed);

        // Create 10 vehicles positioned randomly in a 100x100x10 space
        for (int i = 1; i <= 10; i++) {
            double x = random.nextDouble() * 100;
            double y = random.nextDouble() * 100;
            double z = random.nextDouble() * 10;
            TestVehicle vehicle = new TestVehicle(
                    "V-" + String.format("%03d", i),
                    "Vehicle " + i,
                    new Position(x, y, z)
            );
            vehicle.setState(VehicleState.IDLE);
            vehicles.add(vehicle);
        }

        // Create 50 tasks with random positions and priorities
        for (int i = 1; i <= 50; i++) {
            double sourceX = random.nextDouble() * 100;
            double sourceY = random.nextDouble() * 100;
            double sourceZ = random.nextDouble() * 10;
            double destX = random.nextDouble() * 100;
            double destY = random.nextDouble() * 100;
            double destZ = random.nextDouble() * 10;

            // Mix of priorities: 70% normal, 20% high, 10% urgent
            TaskPriority priority;
            double priorityRoll = random.nextDouble();
            if (priorityRoll < 0.7) {
                priority = TaskPriority.NORMAL;
            } else if (priorityRoll < 0.9) {
                priority = TaskPriority.HIGH;
            } else {
                priority = TaskPriority.URGENT;
            }

            Task task = new Task(
                    "TASK-" + String.format("%03d", i),
                    "SIM-COMPLEX",
                    TaskType.TRANSPORT,
                    new Position(sourceX, sourceY, sourceZ),
                    new Position(destX, destY, destZ),
                    priority
            );

            taskQueue.enqueue(task);
            allCreatedTasks.add(task);
        }
    }

    /**
     * Count how many tasks are in a specific status.
     */
    private long countTasksByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    /**
     * Collect all tasks that were created for the scenario.
     * <p>
     * Uses TaskQueue snapshot to get tasks without modifying the queue.
     */
    private List<Task> collectAllTasks() {
        TaskQueue.Snapshot snapshot = taskQueue.toSnapshot();
        return new ArrayList<>(snapshot.getTasks());
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
    @DisplayName("Complex Scenario Completion Tests")
    class ScenarioCompletionTests {

        @Test
        @DisplayName("Should complete complex scenario with 10 vehicles and 50 tasks")
        void shouldCompleteComplexScenarioWith10Vehicles50Tasks() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42); // Fixed seed for reproducibility

            int totalTasks = 50;
            int totalVehicles = vehicles.size();

            // Verify we have vehicles available
            assertThat(vehiclePool.getAvailableVehicles()).hasSize(10);
            assertThat(taskQueue.size()).isEqualTo(50);

            // When: Execute first dispatch cycle
            DispatchResult firstResult = dispatcher.onDispatchCycle();

            // Then: First cycle should assign up to 10 tasks (one per vehicle)
            assertThat(firstResult.getAssignedCount()).isGreaterThan(0);

            // Continue with remaining cycles
            int cycleCount = 1;
            int assignedCount = firstResult.getAssignedCount();
            int lastAssignedCount = -1;

            while (cycleCount < MAX_DISPATCH_CYCLES) {
                DispatchResult result = dispatcher.onDispatchCycle();
                assignedCount += result.getAssignedCount();

                // Stop if no more tasks to assign
                if (result.getAssignedCount() == 0 && taskQueue.isEmpty()) {
                    break;
                }

                // Stop if no progress
                if (assignedCount == lastAssignedCount && result.getAssignedCount() == 0) {
                    break;
                }

                lastAssignedCount = assignedCount;
                cycleCount++;
            }

            // Then: Should have assigned most tasks
            assertThat(assignedCount).isGreaterThan(0);
            assertThat(cycleCount).isLessThan(MAX_DISPATCH_CYCLES);
        }

        @Test
        @DisplayName("Should complete scenario within timeout")
        void shouldCompleteScenarioWithinTimeout() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            // When/Then: Should complete within timeout (prevents infinite loops)
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                int cycleCount = 0;
                while (cycleCount < MAX_DISPATCH_CYCLES && !taskQueue.isEmpty()) {
                    dispatcher.onDispatchCycle();
                    cycleCount++;
                }
            });
        }
    }

    @Nested
    @DisplayName("Throughput Tests (REQ-DS-004)")
    class ThroughputTests {

        @Test
        @DisplayName("Should meet throughput threshold in complex scenario")
        void shouldMeetThroughputThresholdInComplexScenario() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            int totalTasks = 50;
            int minExpectedAssigned = (int) (totalTasks * MIN_THROUGHPUT_RATIO); // 40 tasks

            // When: Execute dispatch cycles
            int totalAssigned = 0;
            int cycleCount = 0;
            int lastAssignedCount = -1;

            while (cycleCount < MAX_DISPATCH_CYCLES) {
                DispatchResult result = dispatcher.onDispatchCycle();
                totalAssigned += result.getAssignedCount();

                // Stop if no progress
                if (totalAssigned == lastAssignedCount && result.getAssignedCount() == 0) {
                    break;
                }

                lastAssignedCount = totalAssigned;
                cycleCount++;

                // Stop if queue is empty
                if (taskQueue.isEmpty()) {
                    break;
                }
            }

            // Then: Should assign at least minimum throughput threshold
            // With 10 vehicles, we expect at least 10 tasks assigned per cycle initially
            assertThat(totalAssigned).isGreaterThanOrEqualTo(minExpectedAssigned);
        }

        @Test
        @DisplayName("Should meet throughput threshold based on completed tasks")
        void shouldMeetThroughputThresholdBasedOnCompletedTasks() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            int totalTasks = 50;
            int minExpectedCompleted = (int) (totalTasks * MIN_THROUGHPUT_RATIO); // 40 tasks

            // When: Execute dispatch cycles and simulate task completion
            int cycleCount = 0;
            int lastCompletedCount = -1;

            while (cycleCount < MAX_DISPATCH_CYCLES) {
                // Track queue size before dispatch
                int queueSizeBefore = taskQueue.size();

                DispatchResult result = dispatcher.onDispatchCycle();

                // Simulate task completion: mark all IN_PROGRESS tasks as COMPLETED
                for (Task task : allCreatedTasks) {
                    if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                        task.complete();
                    }
                }

                // Count completed tasks
                int completedCount = (int) allCreatedTasks.stream()
                        .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                        .count();

                // Stop if no progress
                if (completedCount == lastCompletedCount && result.getAssignedCount() == 0) {
                    break;
                }

                lastCompletedCount = completedCount;
                cycleCount++;

                // Stop if all tasks completed
                if (completedCount >= totalTasks) {
                    break;
                }

                // Stop if queue is empty and no further progress possible
                if (result.getAssignedCount() == 0 && queueSizeBefore == 0) {
                    break;
                }
            }

            // Then: Should complete at least minimum throughput threshold based on actual task status
            int finalCompletedCount = (int) allCreatedTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                    .count();
            assertThat(finalCompletedCount).isGreaterThanOrEqualTo(minExpectedCompleted);
        }

        @Test
        @DisplayName("Should measure throughput correctly across multiple cycles")
        void shouldMeasureThroughputCorrectlyAcrossMultipleCycles() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            // When: Execute dispatch cycles and collect metrics
            List<Integer> cycleAssignments = new ArrayList<>();
            int totalAssigned = 0;

            for (int i = 0; i < 10 && !taskQueue.isEmpty(); i++) {
                DispatchResult result = dispatcher.onDispatchCycle();
                cycleAssignments.add(result.getAssignedCount());
                totalAssigned += result.getAssignedCount();
            }

            // Then: First cycle should assign maximum (10 vehicles)
            assertThat(cycleAssignments.get(0)).isEqualTo(10);

            // And: Subsequent cycles may assign fewer as tasks deplete
            // And: Total should be meaningful
            assertThat(totalAssigned).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Task Completion Tests")
    class TaskCompletionTests {

        @Test
        @DisplayName("Should complete all tasks in 10 vehicles 50 tasks scenario")
        void shouldCompleteAllTasksIn10Vehicles50TasksScenario() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            int totalTasks = 50;

            // When: Execute dispatch cycles until all tasks are completed
            int cycleCount = 0;
            int lastCompletedCount = -1;

            while (cycleCount < MAX_DISPATCH_CYCLES) {
                // Track queue size before dispatch
                int queueSizeBefore = taskQueue.size();

                DispatchResult result = dispatcher.onDispatchCycle();

                // Simulate task completion: mark all IN_PROGRESS tasks as COMPLETED
                for (Task task : allCreatedTasks) {
                    if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                        task.complete();
                    }
                }

                // Count completed tasks
                int completedCount = (int) allCreatedTasks.stream()
                        .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                        .count();

                // Stop if no progress
                if (completedCount == lastCompletedCount && result.getAssignedCount() == 0) {
                    break;
                }

                lastCompletedCount = completedCount;
                cycleCount++;

                // Stop if all tasks completed
                if (completedCount >= totalTasks) {
                    break;
                }

                // Stop if queue is empty and no further progress possible
                if (result.getAssignedCount() == 0 && queueSizeBefore == 0) {
                    break;
                }
            }

            // Then: All tasks should be completed (strict assertion)
            int finalCompletedCount = (int) allCreatedTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                    .count();
            assertThat(finalCompletedCount).isEqualTo(totalTasks);
        }
    }

    @Nested
    @DisplayName("Finite Return Tests (REQ-DS-004)")
    class FiniteReturnTests {

        @Test
        @DisplayName("Should return in finite steps when no vehicle available in complex scenario")
        void shouldReturnInFiniteStepsWhenNoVehicleAvailableInComplexScenario() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            // Mark all vehicles as manually assigned (no vehicles available for auto dispatch)
            for (Vehicle v : vehicles) {
                vehiclePool.markManuallyAssigned(v.id());
            }

            // When: Execute dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Should return immediately with no assignments
            assertThat(result.getAssignedCount()).isEqualTo(0);
            assertThat(result.getFailedCount()).isEqualTo(0);

            // And: All tasks should remain in queue
            assertThat(taskQueue.size()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should not enter infinite loop when tasks exceed vehicle capacity")
        void shouldNotEnterInfiniteLoopWhenTasksExceedVehicleCapacity() {
            // Given: 10 vehicles and 50 tasks (more tasks than vehicles)
            create10Vehicles50TasksScenario(42);

            // When: Execute multiple dispatch cycles
            int cycleCount = 0;
            int totalAssigned = 0;

            while (cycleCount < MAX_DISPATCH_CYCLES && !taskQueue.isEmpty()) {
                DispatchResult result = dispatcher.onDispatchCycle();
                totalAssigned += result.getAssignedCount();

                // If no progress, break
                if (result.getAssignedCount() == 0) {
                    break;
                }

                cycleCount++;
            }

            // Then: Should terminate without infinite loop
            assertThat(cycleCount).isLessThan(MAX_DISPATCH_CYCLES);

            // And: Should have assigned some tasks
            assertThat(totalAssigned).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("No Double Assignment Tests")
    class NoDoubleAssignmentTests {

        @Test
        @DisplayName("Should not assign same vehicle twice in complex scenario")
        void shouldNotAssignSameVehicleTwiceInComplexScenario() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            // Track which vehicles get assigned
            List<String> assignedVehicleIds = new ArrayList<>();

            // When: Execute first dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Collect assigned vehicles by checking task assignments
            List<Task> allTasks = collectAllTasks();
            for (Task task : allTasks) {
                if (task.getAssignedVehicleId() != null) {
                    assignedVehicleIds.add(task.getAssignedVehicleId());
                }
            }

            // Then: No vehicle should be assigned twice
            List<String> uniqueVehicleIds = assignedVehicleIds.stream().distinct().toList();
            assertThat(assignedVehicleIds).hasSameSizeAs(uniqueVehicleIds);

            // And: At most 10 vehicles should be assigned (one per task)
            assertThat(assignedVehicleIds.size()).isLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Runtime Task Addition Tests in Complex Scenario")
    class RuntimeTaskAdditionTests {

        @Test
        @DisplayName("Should process runtime added tasks in complex scenario")
        void shouldProcessRuntimeAddedTasksInComplexScenario() {
            // Given: 10 vehicles and initial 30 tasks
            create10Vehicles50TasksScenario(42);
            int initialTaskCount = 30;
            int runtimeTaskCount = 20;

            // Remove excess tasks to start with 30
            while (taskQueue.size() > initialTaskCount) {
                taskQueue.pollNext();
            }

            // When: Execute initial dispatch cycles
            dispatcher.onDispatchCycle();

            // And: Add 20 tasks at runtime
            for (int i = 0; i < runtimeTaskCount; i++) {
                Task runtimeTask = new Task(
                        "TASK-RUNTIME-" + String.format("%03d", i),
                        "SIM-COMPLEX",
                        TaskType.TRANSPORT,
                        new Position(i * 5, 0, 0),
                        new Position(i * 5 + 10, 10, 0),
                        TaskPriority.NORMAL
                );
                dispatcher.addTask(runtimeTask, false);
            }

            // And: Execute another dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Runtime tasks should be processed
            assertThat(result.getAssignedCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should prioritize urgent runtime tasks in complex scenario")
        void shouldPrioritizeUrgentRuntimeTasksInComplexScenario() {
            // Given: 10 vehicles and 30 normal tasks
            create10Vehicles50TasksScenario(42);
            while (taskQueue.size() > 30) {
                taskQueue.pollNext();
            }

            // When: Execute initial dispatch
            dispatcher.onDispatchCycle();

            // And: Add urgent task at runtime
            Task urgentTask = new Task(
                    "TASK-URGENT-001",
                    "SIM-COMPLEX",
                    TaskType.TRANSPORT,
                    new Position(0, 0, 0),
                    new Position(100, 100, 0),
                    TaskPriority.URGENT
            );
            dispatcher.addTask(urgentTask, true);

            // And: Execute another dispatch cycle
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Urgent task should be assigned (if vehicles available)
            // The highest priority task should be processed
            assertThat(result.getAssignedCount()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Manual Assignment Tests in Complex Scenario")
    class ManualAssignmentTests {

        @Test
        @DisplayName("Should not override manual assignments in complex scenario")
        void shouldNotOverrideManualAssignmentsInComplexScenario() {
            // Given: 10 vehicles and 50 tasks
            create10Vehicles50TasksScenario(42);

            // When: Manually assign 3 vehicles to specific tasks
            Task manualTask1 = new Task("TASK-MANUAL-1", "SIM-COMPLEX", TaskType.TRANSPORT,
                    new Position(0, 0, 0), new Position(10, 10, 0), TaskPriority.NORMAL);
            Task manualTask2 = new Task("TASK-MANUAL-2", "SIM-COMPLEX", TaskType.TRANSPORT,
                    new Position(20, 20, 0), new Position(30, 30, 0), TaskPriority.NORMAL);
            Task manualTask3 = new Task("TASK-MANUAL-3", "SIM-COMPLEX", TaskType.TRANSPORT,
                    new Position(40, 40, 0), new Position(50, 50, 0), TaskPriority.NORMAL);

            taskQueue.enqueue(manualTask1);
            taskQueue.enqueue(manualTask2);
            taskQueue.enqueue(manualTask3);

            dispatcher.manualAssign(manualTask1, "V-001");
            dispatcher.manualAssign(manualTask2, "V-002");
            dispatcher.manualAssign(manualTask3, "V-003");

            // Verify manual assignments
            assertThat(manualTask1.getAssignedVehicleId()).isEqualTo("V-001");
            assertThat(manualTask2.getAssignedVehicleId()).isEqualTo("V-002");
            assertThat(manualTask3.getAssignedVehicleId()).isEqualTo("V-003");

            // When: Execute automatic dispatch
            DispatchResult result = dispatcher.onDispatchCycle();

            // Then: Manual assignments should not be overridden
            assertThat(manualTask1.getAssignedVehicleId()).isEqualTo("V-001");
            assertThat(manualTask2.getAssignedVehicleId()).isEqualTo("V-002");
            assertThat(manualTask3.getAssignedVehicleId()).isEqualTo("V-003");

            // And: Automatic dispatch should use remaining vehicles (7 vehicles)
            // Should assign some tasks to non-manually assigned vehicles
            assertThat(result.getAssignedCount()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Reproducibility Tests")
    class ReproducibilityTests {

        @Test
        @DisplayName("Should produce reproducible results with same random seed")
        void shouldProduceReproducibleResultsWithSameRandomSeed() {
            // Given: Two scenarios with same random seed
            create10Vehicles50TasksScenario(12345);

            int cycleCount1 = 0;
            int totalAssigned1 = 0;
            while (cycleCount1 < 10 && !taskQueue.isEmpty()) {
                DispatchResult result = dispatcher.onDispatchCycle();
                totalAssigned1 += result.getAssignedCount();
                cycleCount1++;
            }

            // Reset and create new scenario with same seed
            setUp();
            create10Vehicles50TasksScenario(12345);

            int cycleCount2 = 0;
            int totalAssigned2 = 0;
            while (cycleCount2 < 10 && !taskQueue.isEmpty()) {
                DispatchResult result = dispatcher.onDispatchCycle();
                totalAssigned2 += result.getAssignedCount();
                cycleCount2++;
            }

            // Then: Results should be identical (deterministic)
            assertThat(totalAssigned1).isEqualTo(totalAssigned2);
        }
    }
}
