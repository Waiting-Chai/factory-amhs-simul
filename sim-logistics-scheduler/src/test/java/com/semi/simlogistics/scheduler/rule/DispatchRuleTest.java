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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for DispatchRule interface contract (REQ-DS-003).
 * These tests define the expected behavior of all dispatch rule implementations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("DispatchRule Interface Contract Tests (REQ-DS-003)")
class DispatchRuleTest {

    /**
     * Test implementation of DispatchRule for contract validation.
     * Uses simple distance-based selection.
     */
    private static class TestDispatchRule implements DispatchRule {
        @Override
        public Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context) {
            // Validate required parameters
            if (task == null) {
                throw new NullPointerException("Task cannot be null");
            }
            if (context == null) {
                throw new NullPointerException("DispatchContext cannot be null");
            }
            if (candidates == null || candidates.isEmpty()) {
                return Optional.empty();
            }
            // Select vehicle closest to task source
            Position taskSource = task.getSource();
            Vehicle closest = candidates.get(0);
            double minDistance = distance(taskSource, closest.position());

            for (Vehicle v : candidates) {
                double d = distance(taskSource, v.position());
                if (d < minDistance || (d == minDistance && v.id().compareTo(closest.id()) < 0)) {
                    minDistance = d;
                    closest = v;
                }
            }
            return Optional.of(closest);
        }

        private double distance(Position p1, Position p2) {
            double dx = p1.x() - p2.x();
            double dy = p1.y() - p2.y();
            return Math.sqrt(dx * dx + dy * dy);
        }
    }

    /**
     * Test implementation of DispatchContext for contract validation.
     */
    private static class TestDispatchContext implements DispatchContext {
        @Override
        public double getDistance(Position from, Position to) {
            double dx = from.x() - to.x();
            double dy = from.y() - to.y();
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public double getUtilization(String vehicleId) {
            return 0.5; // Fixed 50% utilization for testing
        }

        @Override
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Test implementation of Vehicle for contract validation.
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
    @DisplayName("DispatchRule Contract Tests")
    class ContractTests {

        @Test
        @DisplayName("Should return empty when no candidates available")
        void shouldReturnEmptyWhenNoCandidates() {
            // Given: A rule and empty candidate list
            DispatchRule rule = new TestDispatchRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            List<Vehicle> emptyCandidates = List.of();
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle from empty list
            Optional<Vehicle> result = rule.selectVehicle(task, emptyCandidates, context);

            // Then: Should return empty optional
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when candidates is null")
        void shouldReturnEmptyWhenCandidatesIsNull() {
            // Given: A rule and null candidate list
            DispatchRule rule = new TestDispatchRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle from null list
            Optional<Vehicle> result = rule.selectVehicle(task, null, context);

            // Then: Should return empty optional
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should select vehicle when candidates available")
        void shouldSelectVehicleWhenCandidatesAvailable() {
            // Given: A rule, task, and available vehicles
            DispatchRule rule = new TestDispatchRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle vehicle1 = new TestVehicle("V-001", "Vehicle 1", new Position(5.0, 5.0, 0.0));
            TestVehicle vehicle2 = new TestVehicle("V-002", "Vehicle 2", new Position(20.0, 20.0, 0.0));
            List<Vehicle> candidates = List.of(vehicle1, vehicle2);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should select closest vehicle
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-001");
        }

        @Test
        @DisplayName("Should handle context parameter")
        void shouldHandleContextParameter() {
            // Given: A rule with context-dependent logic
            DispatchRule rule = new TestDispatchRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle vehicle = new TestVehicle("V-001", "Vehicle 1", new Position(5.0, 5.0, 0.0));
            List<Vehicle> candidates = List.of(vehicle);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle with context
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should successfully use context
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should throw NPE for null task")
        void shouldThrowNPEForNullTask() {
            // Given: A rule
            DispatchRule rule = new TestDispatchRule();
            List<Vehicle> candidates = List.of(new TestVehicle("V-001", "Vehicle 1", new Position(0.0, 0.0, 0.0)));
            DispatchContext context = new TestDispatchContext();

            // When/Then: Should throw NPE for null task
            assertThatThrownBy(() -> rule.selectVehicle(null, candidates, context))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw NPE for null context")
        void shouldThrowNPEForNullContext() {
            // Given: A rule
            DispatchRule rule = new TestDispatchRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);
            List<Vehicle> candidates = List.of(new TestVehicle("V-001", "Vehicle 1", new Position(0.0, 0.0, 0.0)));

            // When/Then: Should throw NPE for null context
            assertThatThrownBy(() -> rule.selectVehicle(task, candidates, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("DispatchContext Contract Tests")
    class ContextContractTests {

        @Test
        @DisplayName("Should provide distance calculation")
        void shouldProvideDistanceCalculation() {
            // Given: A context
            DispatchContext context = new TestDispatchContext();
            Position p1 = new Position(0.0, 0.0, 0.0);
            Position p2 = new Position(3.0, 4.0, 0.0);

            // When: Calculate distance
            double distance = context.getDistance(p1, p2);

            // Then: Should return Euclidean distance (5.0)
            assertThat(distance).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should provide vehicle utilization")
        void shouldProvideVehicleUtilization() {
            // Given: A context
            DispatchContext context = new TestDispatchContext();

            // When: Get utilization
            double utilization = context.getUtilization("V-001");

            // Then: Should return utilization value
            assertThat(utilization).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should provide current time")
        void shouldProvideCurrentTime() {
            // Given: A context
            DispatchContext context = new TestDispatchContext();
            long before = System.currentTimeMillis();

            // When: Get current time
            long currentTime = context.getCurrentTime();

            // Then: Should return reasonable time
            assertThat(currentTime).isGreaterThanOrEqualTo(before);
            assertThat(currentTime).isLessThanOrEqualTo(System.currentTimeMillis());
        }
    }

    @Nested
    @DisplayName("Tie-Breaking Tests")
    class TieBreakingTests {

        @Test
        @DisplayName("Should break ties deterministically by vehicleId")
        void shouldBreakTiesDeterministically() {
            // Given: A rule and two vehicles at same distance
            DispatchRule rule = new TestDispatchRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(10.0, 10.0, 0.0),
                    TaskPriority.NORMAL);

            // Both vehicles at distance 10.0 from task source
            TestVehicle vehicleZ = new TestVehicle("V-Z", "Vehicle Z", new Position(10.0, 0.0, 0.0));
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(10.0, 0.0, 0.0));
            TestVehicle vehicleM = new TestVehicle("V-M", "Vehicle M", new Position(10.0, 0.0, 0.0));

            // Pass in non-alphabetical order
            List<Vehicle> candidates = List.of(vehicleZ, vehicleA, vehicleM);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should select alphabetically first (V-A)
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-A");
        }
    }
}
