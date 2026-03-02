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
 * TDD tests for ShortestDistanceRule (REQ-DS-003).
 * Tests the distance-based vehicle selection strategy.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("ShortestDistanceRule Tests (REQ-DS-003)")
class ShortestDistanceRuleTest {

    /**
     * Test implementation of DispatchContext that returns Euclidean distance.
     */
    private static class TestDispatchContext implements DispatchContext {
        @Override
        public double getDistance(Position from, Position to) {
            double dx = from.x() - to.x();
            double dy = from.y() - to.y();
            double dz = from.z() - to.z();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        @Override
        public double getUtilization(String vehicleId) {
            return 0.5; // Not used for distance rule
        }

        @Override
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
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
    @DisplayName("Distance Selection Tests")
    class DistanceSelectionTests {

        @Test
        @DisplayName("Should select vehicle closest to task source")
        void shouldSelectClosestVehicle() {
            // Given: A rule, task, and vehicles at different distances
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),  // Task source at origin
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // Vehicles at different distances: 5.0, 10.0, 15.0
            TestVehicle closeVehicle = new TestVehicle("V-CLOSE", "Close", new Position(3.0, 4.0, 0.0));    // distance 5.0
            TestVehicle mediumVehicle = new TestVehicle("V-MED", "Medium", new Position(6.0, 8.0, 0.0));     // distance 10.0
            TestVehicle farVehicle = new TestVehicle("V-FAR", "Far", new Position(12.0, 9.0, 0.0));       // distance 15.0

            List<Vehicle> candidates = List.of(mediumVehicle, farVehicle, closeVehicle); // Non-alphabetical order
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should select closest vehicle
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-CLOSE");
        }

        @Test
        @DisplayName("Should use context distance calculation")
        void shouldUseContextDistanceCalculation() {
            // Given: A rule and context
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(3.0, 4.0, 0.0));
            List<Vehicle> candidates = List.of(vehicle);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should use context distance (5.0)
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-001");
        }
    }

    @Nested
    @DisplayName("Tie-Breaking Tests")
    class TieBreakingTests {

        @Test
        @DisplayName("Should break ties by vehicleId (alphabetical order)")
        void shouldBreakTiesByVehicleId() {
            // Given: A rule and two vehicles at same distance
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // Both vehicles at distance 5.0 from task source (3-4-5 triangle)
            TestVehicle vehicleZ = new TestVehicle("V-Z", "Vehicle Z", new Position(3.0, 4.0, 0.0));
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(3.0, 4.0, 0.0));
            TestVehicle vehicleM = new TestVehicle("V-M", "Vehicle M", new Position(3.0, 4.0, 0.0));

            // Pass in non-alphabetical order
            List<Vehicle> candidates = List.of(vehicleZ, vehicleA, vehicleM);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should select alphabetically first (V-A)
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-A");
        }

        @Test
        @DisplayName("Should break ties deterministically (same input, same output)")
        void shouldBreakTiesDeterministically() {
            // Given: Same setup as previous test
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle vehicleZ = new TestVehicle("V-Z", "Vehicle Z", new Position(3.0, 4.0, 0.0));
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(3.0, 4.0, 0.0));
            TestVehicle vehicleM = new TestVehicle("V-M", "Vehicle M", new Position(3.0, 4.0, 0.0));

            List<Vehicle> candidates = List.of(vehicleZ, vehicleA, vehicleM);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle multiple times
            Optional<Vehicle> result1 = rule.selectVehicle(task, candidates, context);
            Optional<Vehicle> result2 = rule.selectVehicle(task, candidates, context);
            Optional<Vehicle> result3 = rule.selectVehicle(task, candidates, context);

            // Then: Should always return same vehicle
            assertThat(result1).isPresent();
            assertThat(result2).isPresent();
            assertThat(result3).isPresent();
            assertThat(result1.get().id()).isEqualTo("V-A");
            assertThat(result2.get().id()).isEqualTo("V-A");
            assertThat(result3.get().id()).isEqualTo("V-A");
        }

        @Test
        @DisplayName("Should break ties by vehicleId when distances are identical")
        void shouldBreakTiesByVehicleIdWhenDistancesIdentical() {
            // Given: A rule and vehicles with identical distances
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // Create vehicles at the same position (distance = 5.0 exactly)
            // The tiny offset (1e-16) is below double precision at this scale,
            // so all positions map to the same distance value at runtime
            TestVehicle vehicle1 = new TestVehicle("V-Z", "Vehicle Z", new Position(3.0, 4.0, 0.0));
            TestVehicle vehicle2 = new TestVehicle("V-A", "Vehicle A", new Position(3.0 + 1e-16, 4.0, 0.0));
            TestVehicle vehicle3 = new TestVehicle("V-M", "Vehicle M", new Position(3.0, 4.0 + 1e-16, 0.0));

            List<Vehicle> candidates = List.of(vehicle1, vehicle2, vehicle3);
            DispatchContext context = new TestDispatchContext();

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should break tie by vehicleId (alphabetical)
            assertThat(result).isPresent();
            // Double.compare performs exact comparison; all distances are equal, so tie-breaker applies
            assertThat(result.get().id()).isEqualTo("V-A");
        }
    }

    @Nested
    @DisplayName("Empty Input Tests")
    class EmptyInputTests {

        @Test
        @DisplayName("Should return empty for null candidates")
        void shouldReturnEmptyForNullCandidates() {
            // Given: A rule and null candidates
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);
            DispatchContext context = new TestDispatchContext();

            // When: Select from null candidates
            Optional<Vehicle> result = rule.selectVehicle(task, null, context);

            // Then: Should return empty
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for empty candidates")
        void shouldReturnEmptyForEmptyCandidates() {
            // Given: A rule and empty candidates
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);
            List<Vehicle> emptyCandidates = List.of();
            DispatchContext context = new TestDispatchContext();

            // When: Select from empty candidates
            Optional<Vehicle> result = rule.selectVehicle(task, emptyCandidates, context);

            // Then: Should return empty
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NPE for null task")
        void shouldThrowNPEForNullTask() {
            // Given: A rule
            DispatchRule rule = new ShortestDistanceRule();
            List<Vehicle> candidates = List.of(new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0)));
            DispatchContext context = new TestDispatchContext();

            // When/Then: Should throw NPE
            assertThatThrownBy(() -> rule.selectVehicle(null, candidates, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Task cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null context")
        void shouldThrowNPEForNullContext() {
            // Given: A rule
            DispatchRule rule = new ShortestDistanceRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);
            List<Vehicle> candidates = List.of(new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0)));

            // When/Then: Should throw NPE
            assertThatThrownBy(() -> rule.selectVehicle(task, candidates, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("DispatchContext cannot be null");
        }
    }

    @Nested
    @DisplayName("REQ-DS-003 Scenario Tests")
    class ScenarioTests {

        @Test
        @DisplayName("REQ-DS-003: Should select vehicle with shortest distance to task source")
        void shouldSelectVehicleWithShortestDistance() {
            // Scenario: 最短距离规则
            // Given: 一个运输任务
            // And: 多辆可用车辆
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(10.0, 10.0, 0.0),  // Task source
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // Vehicle A at (10, 10) - distance 0 (same as source)
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(10.0, 10.0, 0.0));
            // Vehicle B at (20, 10) - distance 10
            TestVehicle vehicleB = new TestVehicle("V-B", "Vehicle B", new Position(20.0, 10.0, 0.0));
            // Vehicle C at (10, 30) - distance 20
            TestVehicle vehicleC = new TestVehicle("V-C", "Vehicle C", new Position(10.0, 30.0, 0.0));

            List<Vehicle> candidates = List.of(vehicleC, vehicleB, vehicleA); // Reverse distance order
            DispatchContext context = new TestDispatchContext();
            DispatchRule rule = new ShortestDistanceRule();

            // When: 使用 ShortestDistanceRule 分配
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: 应选择距离任务起点最近的车辆
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-A");
        }
    }
}
