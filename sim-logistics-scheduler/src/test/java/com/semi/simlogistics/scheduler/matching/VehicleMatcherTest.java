package com.semi.simlogistics.scheduler.matching;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.scheduler.rule.DispatchContext;
import com.semi.simlogistics.scheduler.rule.DispatchRule;
import com.semi.simlogistics.scheduler.rule.TaskSelectionRule;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskPriority;
import com.semi.simlogistics.scheduler.task.TaskType;
import com.semi.simlogistics.scheduler.test.TestDispatchRules;
import com.semi.simlogistics.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for VehicleMatcher.
 * Tests the task-vehicle matching algorithm.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("VehicleMatcher Tests")
class VehicleMatcherTest {

    private VehicleMatcher matcher;
    private DispatchContext dispatchContext;

    @BeforeEach
    void setUp() {
        DispatchRule dispatchRule = new TestDispatchRules.FirstVehicleRule();
        TaskSelectionRule taskSelectionRule = new TestDispatchRules.FirstTaskRule();
        dispatchContext = new TestDispatchRules.SimpleDispatchContext();
        matcher = new VehicleMatcher(dispatchRule, taskSelectionRule);
    }

    // ========== Test Helpers ==========

    private Task createTask(String id, TaskPriority priority) {
        return new Task(id, "SIM-001", TaskType.TRANSPORT,
                new Position(0.0, 0.0, 0.0),
                new Position(100.0, 100.0, 0.0),
                priority);
    }

    private TestVehicle createVehicle(String id, Position position, VehicleState state) {
        TestVehicle vehicle = new TestVehicle(id, "Vehicle " + id, position);
        vehicle.setState(state);
        return vehicle;
    }

    /**
     * Test implementation of Vehicle.
     */
    private static class TestVehicle extends Vehicle {
        protected TestVehicle(String id, String name, Position position) {
            super(id, name, com.semi.simlogistics.core.EntityType.AGV_VEHICLE, position, null);
        }

        @Override
        protected Object run(com.semi.jSimul.core.Process.ProcessContext ctx) throws Exception {
            return null;
        }
    }

    @Nested
    @DisplayName("Task-Vehicle Matching Tests")
    class MatchingTests {

        @Test
        @DisplayName("Should match when vehicle available")
        void shouldMatchWhenVehicleAvailable() {
            // Given: A task and available vehicle
            Task task = createTask("TASK-001", TaskPriority.NORMAL);
            List<Task> tasks = List.of(task);

            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0), VehicleState.IDLE);
            List<Vehicle> vehicles = List.of(vehicle);

            // When: Match
            MatchingContext context = new MatchingContext(tasks, vehicles, dispatchContext);
            Optional<MatchingResult> result = matcher.match(task, context);

            // Then: Should return matching result
            assertThat(result).isPresent();
            assertThat(result.get().getTaskId()).hasValue(task.getId());
            assertThat(result.get().getVehicleId()).hasValue(vehicle.id());
            assertThat(result.get().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should return empty when no candidates")
        void shouldReturnEmptyWhenNoCandidates() {
            // Given: A task but no vehicles
            Task task = createTask("TASK-001", TaskPriority.NORMAL);
            List<Task> tasks = List.of(task);
            List<Vehicle> vehicles = new ArrayList<>(); // Empty

            // When: Match
            MatchingContext context = new MatchingContext(tasks, vehicles, dispatchContext);
            Optional<MatchingResult> result = matcher.match(task, context);

            // Then: Should return empty
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should break ties deterministically")
        void shouldBreakTiesDeterministically() {
            // Given: Multiple vehicles
            Task task = createTask("TASK-001", TaskPriority.NORMAL);
            List<Task> tasks = List.of(task);

            // Vehicles at same distance
            TestVehicle vehicle1 = createVehicle("V-001", new Position(0.0, 0.0, 0.0), VehicleState.IDLE);
            TestVehicle vehicle2 = createVehicle("V-002", new Position(0.0, 0.0, 0.0), VehicleState.IDLE);
            List<Vehicle> vehicles = List.of(vehicle2, vehicle1); // Non-alphabetical order

            // When: Match multiple times
            MatchingContext context = new MatchingContext(tasks, vehicles, dispatchContext);
            Optional<MatchingResult> result1 = matcher.match(task, context);
            Optional<MatchingResult> result2 = matcher.match(task, context);
            Optional<MatchingResult> result3 = matcher.match(task, context);

            // Then: Should always return same result (first vehicle rule selects V-002)
            assertThat(result1).isPresent();
            assertThat(result2).isPresent();
            assertThat(result3).isPresent();
            assertThat(result1.get().getVehicleId()).hasValue("V-002");
            assertThat(result2.get().getVehicleId()).hasValue("V-002");
            assertThat(result3.get().getVehicleId()).hasValue("V-002");
        }
    }

    @Nested
    @DisplayName("MatchingResult Tests")
    class MatchingResultTests {

        @Test
        @DisplayName("Should create success result")
        void shouldCreateSuccessResult() {
            // When: Create success result
            MatchingResult result = MatchingResult.success("TASK-001", "V-001");

            // Then: Should have correct properties
            assertThat(result.getTaskId()).hasValue("TASK-001");
            assertThat(result.getVehicleId()).hasValue("V-001");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFailureReason()).isEmpty();
        }

        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            // When: Create failure result
            MatchingResult result = MatchingResult.failure("TASK-001", "No available vehicle");

            // Then: Should have correct properties
            assertThat(result.getTaskId()).hasValue("TASK-001");
            assertThat(result.getVehicleId()).isEmpty();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureReason()).hasValue("No available vehicle");
        }

        @Test
        @DisplayName("Should create empty result")
        void shouldCreateEmptyResult() {
            // When: Create empty result
            MatchingResult result = MatchingResult.empty();

            // Then: Should have empty properties
            assertThat(result.getTaskId()).isEmpty();
            assertThat(result.getVehicleId()).isEmpty();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailureReason()).isEmpty();
        }
    }

    @Nested
    @DisplayName("MatchingContext Tests")
    class MatchingContextTests {

        @Test
        @DisplayName("Should provide access to tasks and vehicles")
        void shouldProvideAccessToTasksAndVehicles() {
            // Given: Tasks and vehicles
            Task task = createTask("TASK-001", TaskPriority.NORMAL);
            List<Task> tasks = List.of(task);

            TestVehicle vehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0), VehicleState.IDLE);
            List<Vehicle> vehicles = List.of(vehicle);

            // When: Create context
            MatchingContext context = new MatchingContext(tasks, vehicles, dispatchContext);

            // Then: Should provide access
            assertThat(context.getAvailableTasks()).containsExactly(task);
            assertThat(context.getAvailableVehicles()).containsExactly(vehicle);
            assertThat(context.getDispatchContext()).isEqualTo(dispatchContext);
        }

        @Test
        @DisplayName("Should filter available vehicles")
        void shouldFilterAvailableVehicles() {
            // Given: Mixed state vehicles
            TestVehicle idleVehicle = createVehicle("V-001", new Position(0.0, 0.0, 0.0), VehicleState.IDLE);
            TestVehicle busyVehicle = createVehicle("V-002", new Position(10.0, 10.0, 0.0), VehicleState.MOVING);
            TestVehicle chargingVehicle = createVehicle("V-003", new Position(20.0, 20.0, 0.0), VehicleState.CHARGING);

            List<Task> tasks = List.of(createTask("TASK-001", TaskPriority.NORMAL));
            List<Vehicle> vehicles = List.of(idleVehicle, busyVehicle, chargingVehicle);

            // When: Create context
            MatchingContext context = new MatchingContext(tasks, vehicles, dispatchContext);

            // Then: Should only include IDLE vehicles
            assertThat(context.getAvailableVehicles())
                    .hasSize(1)
                    .containsExactly(idleVehicle);
        }

        @Test
        @DisplayName("Should throw NPE for null tasks")
        void shouldThrowNPEForNullTasks() {
            assertThatThrownBy(() ->
                    new MatchingContext(null, List.of(), dispatchContext)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NPE for null vehicles")
        void shouldThrowNPEForNullVehicles() {
            assertThatThrownBy(() ->
                    new MatchingContext(List.of(), null, dispatchContext)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NPE for null dispatch context")
        void shouldThrowNPEForNullDispatchContext() {
            assertThatThrownBy(() ->
                    new MatchingContext(List.of(), List.of(), null)
            ).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NPE for null dispatch rule in constructor")
        void shouldThrowNPEForNullDispatchRule() {
            assertThatThrownBy(() ->
                    new VehicleMatcher(null, new TestDispatchRules.FirstTaskRule())
            ).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("DispatchRule");
        }

        @Test
        @DisplayName("Should throw NPE for null task selection rule in constructor")
        void shouldThrowNPEForNullTaskSelectionRule() {
            assertThatThrownBy(() ->
                    new VehicleMatcher(new TestDispatchRules.FirstVehicleRule(), null)
            ).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("TaskSelectionRule");
        }

        @Test
        @DisplayName("Should throw NPE for null task in match")
        void shouldThrowNPEForNullTask() {
            MatchingContext context = new MatchingContext(List.of(), List.of(), dispatchContext);

            assertThatThrownBy(() ->
                    matcher.match(null, context)
            ).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Task");
        }

        @Test
        @DisplayName("Should throw NPE for null context in match")
        void shouldThrowNPEForNullContext() {
            Task task = createTask("TASK-001", TaskPriority.NORMAL);

            assertThatThrownBy(() ->
                    matcher.match(task, null)
            ).isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("MatchingContext");
        }
    }
}
