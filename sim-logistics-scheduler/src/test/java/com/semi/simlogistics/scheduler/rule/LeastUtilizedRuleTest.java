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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for LeastUtilizedRule (REQ-DS-003).
 * Tests the utilization-based vehicle selection strategy.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("LeastUtilizedRule Tests (REQ-DS-003)")
class LeastUtilizedRuleTest {

    /**
     * Test implementation of DispatchContext that provides utilization data.
     */
    private static class TestDispatchContext implements DispatchContext {
        private final Map<String, Double> utilizationMap = new HashMap<>();

        void setUtilization(String vehicleId, double utilization) {
            utilizationMap.put(vehicleId, utilization);
        }

        @Override
        public double getDistance(Position from, Position to) {
            return 0.0; // Not used for utilization rule
        }

        @Override
        public double getUtilization(String vehicleId) {
            return utilizationMap.getOrDefault(vehicleId, 0.0);
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
    @DisplayName("Utilization Selection Tests")
    class UtilizationSelectionTests {

        @Test
        @DisplayName("Should select vehicle with lowest utilization")
        void shouldSelectLowestUtilizationVehicle() {
            // Given: A rule and vehicles with different utilizations
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle lowVehicle = new TestVehicle("V-LOW", "Low", new Position(0.0, 0.0, 0.0));
            TestVehicle medVehicle = new TestVehicle("V-MED", "Medium", new Position(0.0, 0.0, 0.0));
            TestVehicle highVehicle = new TestVehicle("V-HIGH", "High", new Position(0.0, 0.0, 0.0));

            List<Vehicle> candidates = List.of(medVehicle, highVehicle, lowVehicle);
            TestDispatchContext context = new TestDispatchContext();
            context.setUtilization("V-LOW", 0.2);   // 20% utilization
            context.setUtilization("V-MED", 0.5);   // 50% utilization
            context.setUtilization("V-HIGH", 0.8);  // 80% utilization

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should select lowest utilization vehicle
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-LOW");
        }

        @Test
        @DisplayName("Should handle utilization from context")
        void shouldHandleUtilizationFromContext() {
            // Given: A rule and context with utilization data
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle vehicle = new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0));
            List<Vehicle> candidates = List.of(vehicle);
            TestDispatchContext context = new TestDispatchContext();
            context.setUtilization("V-001", 0.35);

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should use context utilization data
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
            // Given: A rule and vehicles with same utilization
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // All vehicles with 50% utilization
            TestVehicle vehicleZ = new TestVehicle("V-Z", "Vehicle Z", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleM = new TestVehicle("V-M", "Vehicle M", new Position(0.0, 0.0, 0.0));

            List<Vehicle> candidates = List.of(vehicleZ, vehicleA, vehicleM);
            TestDispatchContext context = new TestDispatchContext();
            context.setUtilization("V-Z", 0.5);
            context.setUtilization("V-A", 0.5);
            context.setUtilization("V-M", 0.5);

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should select alphabetically first (V-A)
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-A");
        }

        @Test
        @DisplayName("Should break ties deterministically")
        void shouldBreakTiesDeterministically() {
            // Given: Same setup as previous test
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleM = new TestVehicle("V-M", "Vehicle M", new Position(0.0, 0.0, 0.0));

            List<Vehicle> candidates = List.of(vehicleM, vehicleA);
            TestDispatchContext context = new TestDispatchContext();
            context.setUtilization("V-A", 0.5);
            context.setUtilization("V-M", 0.5);

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
        @DisplayName("Should break ties by vehicleId when utilizations are identical")
        void shouldBreakTiesByVehicleIdWhenUtilizationsIdentical() {
            // Given: A rule and vehicles with identical utilizations
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // All vehicles with 50% utilization
            // The tiny offset (1e-17) is below double precision at this scale,
            // so all values map to the same double at runtime
            TestVehicle vehicleZ = new TestVehicle("V-Z", "Vehicle Z", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleM = new TestVehicle("V-M", "Vehicle M", new Position(0.0, 0.0, 0.0));

            List<Vehicle> candidates = List.of(vehicleZ, vehicleA, vehicleM);
            TestDispatchContext context = new TestDispatchContext();
            context.setUtilization("V-Z", 0.5);
            context.setUtilization("V-A", 0.5 + 1e-17); // Maps to same double as 0.5
            context.setUtilization("V-M", 0.5);

            // When: Select vehicle
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: Should break tie by vehicleId (alphabetical)
            assertThat(result).isPresent();
            // Double.compare performs exact comparison; all utilizations are equal, so tie-breaker applies
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
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);
            TestDispatchContext context = new TestDispatchContext();

            // When: Select from null candidates
            Optional<Vehicle> result = rule.selectVehicle(task, null, context);

            // Then: Should return empty
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for empty candidates")
        void shouldReturnEmptyForEmptyCandidates() {
            // Given: A rule and empty candidates
            DispatchRule rule = new LeastUtilizedRule();
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(0.0, 0.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);
            List<Vehicle> emptyCandidates = List.of();
            TestDispatchContext context = new TestDispatchContext();

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
            DispatchRule rule = new LeastUtilizedRule();
            List<Vehicle> candidates = List.of(new TestVehicle("V-001", "Vehicle", new Position(0.0, 0.0, 0.0)));
            TestDispatchContext context = new TestDispatchContext();

            // When/Then: Should throw NPE
            assertThatThrownBy(() -> rule.selectVehicle(null, candidates, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Task cannot be null");
        }

        @Test
        @DisplayName("Should throw NPE for null context")
        void shouldThrowNPEForNullContext() {
            // Given: A rule
            DispatchRule rule = new LeastUtilizedRule();
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
        @DisplayName("REQ-DS-003: Should select vehicle with lowest utilization")
        void shouldSelectVehicleWithLowestUtilization() {
            // Scenario: 最低利用率规则
            // Given: 一个任务
            Task task = new Task("TASK-001", "SIM-001", TaskType.TRANSPORT,
                    new Position(10.0, 10.0, 0.0),
                    new Position(100.0, 100.0, 0.0),
                    TaskPriority.NORMAL);

            // And: 多辆可用车辆
            TestVehicle vehicleA = new TestVehicle("V-A", "Vehicle A", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleB = new TestVehicle("V-B", "Vehicle B", new Position(0.0, 0.0, 0.0));
            TestVehicle vehicleC = new TestVehicle("V-C", "Vehicle C", new Position(0.0, 0.0, 0.0));

            List<Vehicle> candidates = List.of(vehicleA, vehicleB, vehicleC);
            TestDispatchContext context = new TestDispatchContext();
            // Vehicle A: 80% utilization (busy)
            context.setUtilization("V-A", 0.8);
            // Vehicle B: 30% utilization (less busy)
            context.setUtilization("V-B", 0.3);
            // Vehicle C: 10% utilization (least busy)
            context.setUtilization("V-C", 0.1);

            DispatchRule rule = new LeastUtilizedRule();

            // When: 使用 LeastUtilizedRule 分配
            Optional<Vehicle> result = rule.selectVehicle(task, candidates, context);

            // Then: 应选择利用率最低的车辆
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("V-C");
        }
    }
}
