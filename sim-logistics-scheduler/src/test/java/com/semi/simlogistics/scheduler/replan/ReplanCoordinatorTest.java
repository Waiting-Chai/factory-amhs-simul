package com.semi.simlogistics.scheduler.replan;

import com.semi.simlogistics.control.path.Path;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskPriority;
import com.semi.simlogistics.scheduler.task.TaskStatus;
import com.semi.simlogistics.scheduler.task.TaskType;
import com.semi.simlogistics.vehicle.OHTVehicle;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ReplanCoordinator (REQ-DS-006).
 * <p>
 * TDD Test scenarios (no Mockito, uses FakePathPlanner):
 * 1) Path blocked triggers replan and switches to new path
 * 2) Max replan attempts reached stops replanning and enters waiting
 * 3) Task cancellation stops vehicle and replans to new task start (FIXED - no longer placeholder)
 * 4) Task cancellation with no new task returns to standby point
 * 5) Traffic manager replan produces alternative path without deadlock/spin (ENHANCED)
 * 6) Regression: No infinite loops or hangs in replanning logic
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-09
 */
class ReplanCoordinatorTest {

    private ReplanCoordinator coordinator;
    private FakePathPlanner fakePathPlanner;
    private OHTVehicle vehicle;
    private Task task;
    private TaskNodeResolver.TestStubTaskNodeResolver taskNodeResolver;

    // ==================== Setup Helpers ====================

    private void setUpWithFakePlanner() {
        fakePathPlanner = FakePathPlanner.withLinearPaths();
        taskNodeResolver = new TaskNodeResolver.TestStubTaskNodeResolver();
        coordinator = new ReplanCoordinator(fakePathPlanner, taskNodeResolver);

        // Create test vehicle
        vehicle = new OHTVehicle("V001", "Test Vehicle",
                new Position(0, 0, 0), 2.5);
        vehicle.setState(VehicleState.MOVING);
        vehicle.setCurrentNodeId("P1");
        vehicle.setDestinationNodeId("P5");

        // Create test task
        task = new Task("T001", "SIM001", TaskType.TRANSPORT,
                new Position(0, 0, 0), new Position(100, 100, 0), TaskPriority.NORMAL);
        task.assignTo("V001");
        task.start();
    }

    // ==================== Scenario 1: Path Blocked Replan ====================

    @org.junit.jupiter.api.Test
    void testPathBlockedTriggersReplanAndSwitchesToNewPath() {
        setUpWithFakePlanner();

        // When: Request replan due to path blockage
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 100.0);

        // Then: Replan should succeed with new path
        assertTrue(result.isSuccess(), "Replan should succeed");
        assertTrue(result.getNewPath().isPresent(), "New path should be present");
        assertEquals("P1->P2->P3->P5", result.getNewPath().get(),
                "New path should match alternative route");
        assertEquals(ReplanTrigger.PATH_BLOCKED, result.getTrigger());

        // And: Vehicle path should be updated
        assertEquals("P1->P2->P3->P5", vehicle.getPath(),
                "Vehicle path should be updated to new path");
    }

    @org.junit.jupiter.api.Test
    void testPathBlockedFailsWhenNoPathAvailable() {
        // Given: Fake planner with no routes
        fakePathPlanner = FakePathPlanner.withNoRoutes();
        taskNodeResolver = new TaskNodeResolver.TestStubTaskNodeResolver();
        coordinator = new ReplanCoordinator(fakePathPlanner, taskNodeResolver);

        vehicle = new OHTVehicle("V001", "Test Vehicle",
                new Position(0, 0, 0), 2.5);
        vehicle.setState(VehicleState.MOVING);
        vehicle.setCurrentNodeId("P1");
        vehicle.setDestinationNodeId("P5");

        task = new Task("T001", "SIM001", TaskType.TRANSPORT,
                new Position(0, 0, 0), new Position(100, 100, 0), TaskPriority.NORMAL);
        task.assignTo("V001");
        task.start();

        // When: Request replan due to path blockage
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 100.0);

        // Then: Replan should fail
        assertFalse(result.isSuccess(), "Replan should fail when no path available");
        assertTrue(result.getErrorMessage().isPresent(), "Error message should be present");
        assertTrue(result.getErrorMessage().get().contains("No alternative path available"),
                "Error should indicate no path available");
    }

    // ==================== Scenario 2: Max Replan Attempts ====================

    @org.junit.jupiter.api.Test
    void testMaxReplanAttemptsStopsReplanning() {
        setUpWithFakePlanner();
        coordinator.setMaxReplanAttempts(3);

        // When: Replan requested 3 times
        for (int i = 0; i < 3; i++) {
            coordinator.requestReplan(vehicle, task, null,
                    ReplanTrigger.PATH_BLOCKED, 100.0 + i);
        }

        // Then: 4th attempt should be rejected
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 104.0);

        assertFalse(result.isSuccess(), "Replan should fail after max attempts");
        assertTrue(result.getErrorMessage().get().contains("Maximum replan attempts"),
                "Error should indicate max attempts reached");
    }

    @org.junit.jupiter.api.Test
    void testReplanCounterResetsAfterSuccessfulPassage() {
        setUpWithFakePlanner();
        coordinator.setMaxReplanAttempts(2);

        coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 100.0);
        coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 101.0);

        // When: Vehicle passes through successfully (resets counter)
        coordinator.onVehiclePassed(vehicle.id());

        // Then: Should be able to replan again
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 200.0);

        assertTrue(result.isSuccess(), "Replan should succeed after counter reset");
    }

    // ==================== Scenario 3: Task Cancel with New Task (FIXED) ====================

    @org.junit.jupiter.api.Test
    void testTaskCancelledWithNewTaskReplansToNewTaskStart() {
        setUpWithFakePlanner();

        // Given: Current task cancelled, new task available
        task.cancel();
        Task newTask = new Task("T002", "SIM001", TaskType.TRANSPORT,
                new Position(50, 50, 0), new Position(150, 150, 0), TaskPriority.NORMAL);

        // Set up node resolver to map new task to P7
        taskNodeResolver.mapSourceNode("T002", "P7");
        fakePathPlanner.setPath("P1", "P7", Path.of("P1", "P4", "P7"));

        // When: Request replan due to task cancellation
        ReplanResult result = coordinator.requestReplan(vehicle, task, newTask,
                ReplanTrigger.TASK_CANCELLED, 100.0);

        // Then: Should plan to new task start (FIXED - no longer placeholder)
        assertTrue(result.isSuccess(), "Replan should succeed");
        assertEquals("P1->P4->P7", result.getNewPath().get(),
                "Should plan to new task start point");
        assertEquals("P7", vehicle.getDestinationNodeId(),
                "Vehicle destination should be new task start");
        assertEquals(VehicleState.IDLE, vehicle.getState(),
                "Vehicle should be IDLE after task cancellation");
    }

    // ==================== Scenario 4: Task Cancel with No New Task ====================

    @org.junit.jupiter.api.Test
    void testTaskCancelledWithNoNewTaskReturnsToStandby() {
        setUpWithFakePlanner();

        // Given: Current task cancelled, no new task
        task.cancel();
        coordinator.setStandbyPoint("STANDBY");

        // When: Request replan due to task cancellation without new task
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.TASK_CANCELLED, 100.0);

        // Then: Should plan to standby point
        assertTrue(result.isSuccess(), "Replan should succeed");
        assertEquals("P1->P2->STANDBY", result.getNewPath().get(),
                "Should plan to standby point");
        assertEquals("STANDBY", vehicle.getDestinationNodeId(),
                "Vehicle destination should be standby");
        assertEquals(VehicleState.IDLE, vehicle.getState(),
                "Vehicle should return to IDLE state");
    }

    @org.junit.jupiter.api.Test
    void testTaskCancelledWithNoStandbyPointStopsVehicle() {
        setUpWithFakePlanner();

        // Given: Current task cancelled, no standby point configured
        task.cancel();

        // When: Request replan without standby point
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.TASK_CANCELLED, 100.0);

        // Then: Should stop vehicle at current location
        assertTrue(result.getErrorMessage().isPresent(),
                "Should return error when no standby point");
        assertEquals(VehicleState.IDLE, vehicle.getState(),
                "Vehicle should stop and enter IDLE");
    }

    // ==================== Scenario 5: Traffic Conflict Replan (ENHANCED) ====================

    @org.junit.jupiter.api.Test
    void testTrafficConflictReplanProducesAlternativePath() {
        setUpWithFakePlanner();

        // When: Request replan due to traffic conflict
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.TRAFFIC_CONFLICT, 100.0);

        // Then: Should return alternative path
        assertTrue(result.isSuccess(), "Replan should succeed");
        assertEquals("P1->P2->P3->P5", result.getNewPath().get(),
                "Should provide alternative path");
        assertEquals(ReplanTrigger.TRAFFIC_CONFLICT, result.getTrigger());
    }

    @org.junit.jupiter.api.Test
    void testTrafficConflictReplanDoesNotCauseDeadlockOrSpin() {
        setUpWithFakePlanner();
        coordinator.setMaxReplanAttempts(5);

        // Given: Multiple vehicles need replanning
        OHTVehicle vehicle2 = new OHTVehicle("V002", "Vehicle 2",
                new Position(10, 0, 0), 2.5);
        vehicle2.setCurrentNodeId("P2");
        vehicle2.setDestinationNodeId("P5");

        // Configure different paths for each vehicle
        fakePathPlanner.setPath("P2", "P5", Path.of("P2", "P7", "P9", "P5"));

        // When: Both vehicles replan multiple times (simulating potential deadlock scenario)
        int maxIterations = 10;
        int successCount = 0;
        int maxAttemptHitCount = 0;

        for (int i = 0; i < maxIterations; i++) {
            ReplanResult result1 = coordinator.requestReplan(vehicle, task, null,
                    ReplanTrigger.TRAFFIC_CONFLICT, 100.0 + i);
            ReplanResult result2 = coordinator.requestReplan(vehicle2, task, null,
                    ReplanTrigger.TRAFFIC_CONFLICT, 100.0 + i);

            // ENHANCED: Verify finite completion - should not hang
            assertNotNull(result1, "Result should not be null");
            assertNotNull(result2, "Result should not be null");

            if (result1.isSuccess()) successCount++;
            if (result2.isSuccess()) successCount++;

            // Track when max attempts are hit
            if (!result1.isSuccess() && result1.getErrorMessage().isPresent() &&
                result1.getErrorMessage().get().contains("Maximum")) {
                maxAttemptHitCount++;
            }
        }

        // ENHANCED: Verify no infinite spin - both should have hit max attempts
        assertTrue(coordinator.getReplanAttempts(vehicle.id()) >= 5,
                "Vehicle 1 should have hit max attempts");
        assertTrue(coordinator.getReplanAttempts(vehicle2.id()) >= 5,
                "Vehicle 2 should have hit max attempts");

        // Verify different paths were provided (avoid deadlock)
        assertNotEquals(
                fakePathPlanner.planPath("P1", "P5", vehicle.getTransportType()).getNodeIds(),
                fakePathPlanner.planPath("P2", "P5", vehicle2.getTransportType()).getNodeIds(),
                "Paths should be different to avoid deadlock");
    }

    // ==================== Scenario 6: Regression Test - No Infinite Loops ====================

    @org.junit.jupiter.api.Test
    void testReplanDoesNotCauseInfiniteLoop() {
        setUpWithFakePlanner();
        coordinator.setMaxReplanAttempts(10);

        // Given: Process many replan requests
        int requestCount = 0;
        for (int i = 0; i < 20; i++) {
            coordinator.requestReplan(vehicle, task, null,
                    ReplanTrigger.PATH_BLOCKED, 100.0 + i);
            requestCount++;
        }

        // Then: Should complete in finite steps without hanging
        assertEquals(20, requestCount, "All requests should be processed");

        // Verify we didn't get stuck in infinite loop
        assertTrue(coordinator.getReplanAttempts(vehicle.id()) <= 10,
                "Should respect max attempts limit");

        // Verify path planner call count is bounded
        assertTrue(fakePathPlanner.getCallCount() <= 30,
                "Path planner calls should be bounded (no infinite loop)");
    }

    // ==================== Integration Methods ====================

    @org.junit.jupiter.api.Test
    void testOnTaskCancelledIntegrationMethod() {
        setUpWithFakePlanner();

        // Given: Task in cancelled state
        task.cancel();
        coordinator.setStandbyPoint("STANDBY");

        // When: Call onTaskCancelled integration method
        ReplanResult result = coordinator.onTaskCancelled(vehicle, task, null, 100.0);

        // Then: Should work via integration method
        assertTrue(result.isSuccess(), "Integration method should work");
        assertEquals("P1->P2->STANDBY", result.getNewPath().get());
    }

    @org.junit.jupiter.api.Test
    void testOnPathBlockedIntegrationMethod() {
        setUpWithFakePlanner();

        // When: Call onPathBlocked integration method
        ReplanResult result = coordinator.onPathBlocked(vehicle, task, 100.0);

        // Then: Should work via integration method
        assertTrue(result.isSuccess(), "Integration method should work");
        assertEquals("P1->P2->P3->P5", result.getNewPath().get());
    }

    @org.junit.jupiter.api.Test
    void testOnTrafficConflictIntegrationMethod() {
        setUpWithFakePlanner();

        // When: Call onTrafficConflict integration method
        ReplanResult result = coordinator.onTrafficConflict(vehicle, task, 100.0);

        // Then: Should work via integration method
        assertTrue(result.isSuccess(), "Integration method should work");
        assertEquals("P1->P2->P3->P5", result.getNewPath().get());
    }

    @org.junit.jupiter.api.Test
    void testOnTaskCancelledValidatesTaskState() {
        setUpWithFakePlanner();

        // Given: Task not in cancelled state
        Task notCancelledTask = new Task("T002", "SIM001", TaskType.TRANSPORT,
                new Position(0, 0, 0), new Position(100, 100, 0), TaskPriority.NORMAL);

        // When: Call onTaskCancelled with non-cancelled task
        ReplanResult result = coordinator.onTaskCancelled(vehicle, notCancelledTask, null, 100.0);

        // Then: Should fail with state validation error
        assertFalse(result.isSuccess(), "Should fail for non-cancelled task");
        assertTrue(result.getErrorMessage().get().contains("not in cancelled state"),
                "Error should mention task state validation");
    }

    // ==================== Edge Cases ====================

    @org.junit.jupiter.api.Test
    void testReplanWithNullVehicle() {
        setUpWithFakePlanner();

        // When: Request replan with null vehicle
        ReplanResult result = coordinator.requestReplan(null, task, null,
                ReplanTrigger.PATH_BLOCKED, 100.0);

        // Then: Should fail gracefully
        assertFalse(result.isSuccess(), "Should fail with null vehicle");
        assertTrue(result.getErrorMessage().get().contains("Vehicle cannot be null"),
                "Error should mention null vehicle");
    }

    @org.junit.jupiter.api.Test
    void testReplanWithVehicleNoCurrentNode() {
        setUpWithFakePlanner();

        // Given: Vehicle without current node ID
        vehicle.setCurrentNodeId(null);

        // When: Request replan
        ReplanResult result = coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 100.0);

        // Then: Should fail gracefully
        assertFalse(result.isSuccess(), "Should fail without current node");
        assertTrue(result.getErrorMessage().get().contains("current node ID"),
                "Error should mention current node");
    }

    @org.junit.jupiter.api.Test
    void testResetReplanCounter() {
        setUpWithFakePlanner();
        coordinator.setMaxReplanAttempts(3);

        coordinator.requestReplan(vehicle, task, null,
                ReplanTrigger.PATH_BLOCKED, 100.0);

        assertEquals(1, coordinator.getReplanAttempts(vehicle.id()),
                "Should have 1 replan attempt");

        // When: Reset counter
        coordinator.resetReplanCounter(vehicle.id());

        // Then: Counter should be zero
        assertEquals(0, coordinator.getReplanAttempts(vehicle.id()),
                "Counter should be reset");
    }

    @org.junit.jupiter.api.Test
    void testGetTaskNodeResolver() {
        setUpWithFakePlanner();

        // When: Get task node resolver
        TaskNodeResolver resolver = coordinator.getTaskNodeResolver();

        // Then: Should return the resolver
        assertNotNull(resolver, "Resolver should not be null");
        assertEquals(taskNodeResolver, resolver, "Should return the configured resolver");
    }
}
