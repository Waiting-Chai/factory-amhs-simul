package com.semi.simlogistics.scheduler.metrics.analysis;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryConfig;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryMetricsRepository;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for VehicleCountEvaluator (REQ-KPI-006).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("VehicleCountEvaluator Tests (REQ-KPI-006)")
class  VehicleCountEvaluatorTest {

    private static final String SIMULATION_ID = "test-simulation-001";
    private MetricsRepositoryPort repository;
    private InMemoryConfig config;
    private VehicleCountEvaluator evaluator;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        config = new InMemoryConfig();
        evaluator = new VehicleCountEvaluator(repository, config);
    }

    @Nested
    @DisplayName("Binary Search Tests")
    class BinarySearchTests {

        @Test
        @DisplayName("Should find minimum vehicles when current meets target")
        void shouldFindMinimumVehiclesWhenCurrentMeetsTarget() {
            // Given: Current simulation meets target (100 tasks/hour with 10 vehicles)
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Find minimum vehicles
            VehicleCountEvaluator.EvaluationReport report = evaluator.findMinimumVehicles(
                    SIMULATION_ID,
                    10,
                    target
            );

            // Then: Should search downward
            assertThat(report.getOptimalVehicleCount()).isLessThanOrEqualTo(10);
            assertThat(report.getOptimalVehicleCount()).isGreaterThanOrEqualTo(1);
            assertThat(report.getSummary()).contains("Optimal vehicle count");
        }

        @Test
        @DisplayName("Should search upward when current insufficient")
        void shouldSearchUpwardWhenCurrentInsufficient() {
            // Given: Current simulation below target (50 tasks/hour with 10 vehicles, target is 100)
            createSimulationData(50.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Find minimum vehicles
            VehicleCountEvaluator.EvaluationReport report = evaluator.findMinimumVehicles(
                    SIMULATION_ID,
                    10,
                    target
            );

            // Then: Should recommend more vehicles
            assertThat(report.getOptimalVehicleCount()).isGreaterThan(10);
        }

        @Test
        @DisplayName("Should include evaluation results for each tested count")
        void shouldIncludeEvaluationResultsForEachTestedCount() {
            // Given: Simulation data
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Find minimum vehicles
            VehicleCountEvaluator.EvaluationReport report = evaluator.findMinimumVehicles(
                    SIMULATION_ID,
                    10,
                    target
            );

            // Then: Should have evaluation results
            assertThat(report.getEvaluations()).isNotEmpty();
        }

        @Test
        @DisplayName("Should converge to minimum vehicle count")
        void shouldConvergeToMinimumVehicleCount() {
            // Given: Current simulation with known throughput
            // 100 tasks/hour with 10 vehicles means 10 tasks/hour per vehicle
            createSimulationData(100.0, 10);

            // To get 50 tasks/hour, need 5 vehicles (linear projection)
            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(50.0, 0.05);

            // When: Find minimum vehicles
            VehicleCountEvaluator.EvaluationReport report = evaluator.findMinimumVehicles(
                    SIMULATION_ID,
                    10,
                    target
            );

            // Then: Should find optimal around 5 vehicles
            assertThat(report.getOptimalVehicleCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Single Evaluation Tests")
    class SingleEvaluationTests {

        @Test
        @DisplayName("Should evaluate specific vehicle count")
        void shouldEvaluateSpecificVehicleCount() {
            // Given: Simulation data (100 tasks/hour with 10 vehicles)
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Evaluate with 5 vehicles
            VehicleCountEvaluator.EvaluationResult result = evaluator.evaluateVehicleCount(
                    SIMULATION_ID,
                    10,
                    5,
                    target
            );

            // Then: Should project 50 tasks/hour (linear scaling)
            assertThat(result.getVehicleCount()).isEqualTo(5);
            assertThat(result.getThroughput().getTasksPerHour()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should detect when target is met")
        void shouldDetectWhenTargetIsMet() {
            // Given: Simulation data
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Evaluate with current count
            VehicleCountEvaluator.EvaluationResult result = evaluator.evaluateVehicleCount(
                    SIMULATION_ID,
                    10,
                    10,
                    target
            );

            // Then: Should meet target
            assertThat(result.meetsTarget()).isTrue();
        }

        @Test
        @DisplayName("Should detect when target is not met")
        void shouldDetectWhenTargetIsNotMet() {
            // Given: Simulation data
            createSimulationData(50.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Evaluate with current count
            VehicleCountEvaluator.EvaluationResult result = evaluator.evaluateVehicleCount(
                    SIMULATION_ID,
                    10,
                    10,
                    target
            );

            // Then: Should not meet target
            assertThat(result.meetsTarget()).isFalse();
        }

        @Test
        @DisplayName("Should generate correct reason message")
        void shouldGenerateCorrectReasonMessage() {
            // Given: Simulation data
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Evaluate
            VehicleCountEvaluator.EvaluationResult result = evaluator.evaluateVehicleCount(
                    SIMULATION_ID,
                    10,
                    10,
                    target
            );

            // Then: Reason should describe the evaluation
            assertThat(result.getReason()).contains("Projected throughput");
        }
    }

    @Nested
    @DisplayName("Default Target Tests")
    class DefaultTargetTests {

        @Test
        @DisplayName("Should create default target from config")
        void shouldCreateDefaultTargetFromConfig() {
            // Given: Config with custom target
            config.setDouble("throughput.target.tasks_per_hour", 200.0);

            // When: Create default target
            VehicleCountEvaluator.ThroughputTarget target = evaluator.createDefaultTarget();

            // Then: Should use config value
            assertThat(target.getTargetTasksPerHour()).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should use config default when no override")
        void shouldUseConfigDefaultWhenNoOverride() {
            // When: Create default target
            VehicleCountEvaluator.ThroughputTarget target = evaluator.createDefaultTarget();

            // Then: Should use default value (100)
            assertThat(target.getTargetTasksPerHour()).isEqualTo(100.0);
            assertThat(target.getTolerance()).isEqualTo(0.05);
        }
    }

    @Nested
    @DisplayName("Report Tests")
    class ReportTests {

        @Test
        @DisplayName("Should generate summary with optimal count")
        void shouldGenerateSummaryWithOptimalCount() {
            // Given: Simulation data
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Find minimum vehicles
            VehicleCountEvaluator.EvaluationReport report = evaluator.findMinimumVehicles(
                    SIMULATION_ID,
                    10,
                    target
            );

            // Then: Summary should include optimal count
            assertThat(report.getSummary()).contains("Optimal vehicle count");
            assertThat(report.getSummary()).contains("Projected throughput");
        }

        @Test
        @DisplayName("Should get optimal throughput from report")
        void shouldGetOptimalThroughputFromReport() {
            // Given: Simulation data and evaluation
            createSimulationData(100.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            VehicleCountEvaluator.EvaluationReport report = evaluator.findMinimumVehicles(
                    SIMULATION_ID,
                    10,
                    target
            );

            // When: Get optimal throughput
            var optimalThroughput = report.getOptimalThroughput();

            // Then: Should not be null
            assertThat(optimalThroughput).isNotNull();
            assertThat(optimalThroughput.getSimulationId()).isEqualTo(SIMULATION_ID);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle non-existent simulation")
        void shouldHandleNonExistentSimulation() {
            // Given: Target
            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Evaluate non-existent simulation
            VehicleCountEvaluator.EvaluationResult result = evaluator.evaluateVehicleCount(
                    "non-existent",
                    10,
                    10,
                    target
            );

            // Then: Should handle gracefully
            assertThat(result).isNotNull();
            assertThat(result.getThroughput().getTasksPerHour()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle zero current throughput")
        void shouldHandleZeroCurrentThroughput() {
            // Given: Simulation with zero throughput
            createSimulationData(0.0, 10);

            VehicleCountEvaluator.ThroughputTarget target =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.05);

            // When: Evaluate
            VehicleCountEvaluator.EvaluationResult result = evaluator.evaluateVehicleCount(
                    SIMULATION_ID,
                    10,
                    10,
                    target
            );

            // Then: Should handle gracefully (projected throughput still 0)
            assertThat(result.getThroughput().getTasksPerHour()).isEqualTo(0.0);
            assertThat(result.meetsTarget()).isFalse();
        }

        @Test
        @DisplayName("Should handle tolerance correctly")
        void shouldHandleToleranceCorrectly() {
            // Given: Simulation with exactly target throughput
            createSimulationData(100.0, 10);

            // Tight tolerance (1%)
            VehicleCountEvaluator.ThroughputTarget tightTarget =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.01);

            // Loose tolerance (20%)
            VehicleCountEvaluator.ThroughputTarget looseTarget =
                    new VehicleCountEvaluator.ThroughputTarget(100.0, 0.20);

            // When: Evaluate with both tolerances
            VehicleCountEvaluator.EvaluationResult tightResult = evaluator.evaluateVehicleCount(
                    SIMULATION_ID, 10, 10, tightTarget
            );
            VehicleCountEvaluator.EvaluationResult looseResult = evaluator.evaluateVehicleCount(
                    SIMULATION_ID, 10, 10, looseTarget
            );

            // Then: Both should meet target (100.0 == 100.0)
            assertThat(tightResult.meetsTarget()).isTrue();
            assertThat(looseResult.meetsTarget()).isTrue();
        }
    }

    // Helper methods

    private void createSimulationData(double tasksPerHour, int numVehicles) {
        // Create aggregate that represents the simulation state
        MetricAggregate aggregate = new MetricAggregate.Builder(SIMULATION_ID, 3600.0)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted((int) (tasksPerHour * 1))  // 1 hour of data
                .tasksPerHour(tasksPerHour)
                .materialThroughput(tasksPerHour * 5.0)
                .vehicleUtilization(0.7)
                .equipmentUtilization(0.6)
                .wipTotal(50)
                .energyTotal(10000.0)
                .build();

        repository.saveAggregate(aggregate);
    }
}
