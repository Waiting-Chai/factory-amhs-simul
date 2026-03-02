package com.semi.simlogistics.scheduler.metrics.calculator;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.model.MetricData;
import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryMetricsRepository;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for ThroughputCalculator (REQ-KPI-002).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("ThroughputCalculator Tests (REQ-KPI-002)")
class ThroughputCalculatorTest {

    private static final String SIMULATION_ID = "test-simulation-001";
    private MetricsRepositoryPort repository;
    private ThroughputCalculator calculator;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        calculator = new ThroughputCalculator(repository);
    }

    @Nested
    @DisplayName("Total Throughput Tests")
    class TotalThroughputTests {

        @Test
        @DisplayName("Should calculate total throughput from aggregates")
        void shouldCalculateTotalThroughput() {
            // Given: Multiple aggregates
            repository.saveAggregate(createAggregate(100, 0.0));
            repository.saveAggregate(createAggregate(100, 60.0));
            repository.saveAggregate(createAggregate(100, 120.0));

            // When: Calculate throughput
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughput(SIMULATION_ID);

            // Then: Should sum up all aggregates
            assertThat(summary.getTotalTasksCompleted()).isEqualTo(300);
        }

        @Test
        @DisplayName("Should calculate tasks per hour correctly")
        void shouldCalculateTasksPerHourCorrectly() {
            // Given: Aggregate with known tasks per hour
            MetricAggregate aggregate = new MetricAggregate.Builder(SIMULATION_ID, 60.0)
                    .tasksCompleted(100)
                    .tasksPerHour(6000.0)  // 100 tasks / 60s * 3600
                    .build();
            repository.saveAggregate(aggregate);

            // When: Calculate throughput
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughput(SIMULATION_ID);

            // Then: Should match aggregate value
            assertThat(summary.getTasksPerHour()).isEqualTo(6000.0);
        }

        @Test
        @DisplayName("Should return zero summary for empty repository")
        void shouldReturnZeroSummaryForEmptyRepository() {
            // When: Calculate throughput with no data
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughput(SIMULATION_ID);

            // Then: Should return zero values
            assertThat(summary.getTotalTasksCompleted()).isEqualTo(0);
            assertThat(summary.getTasksPerHour()).isEqualTo(0.0);
            assertThat(summary.getMaterialThroughput()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Time Range Throughput Tests")
    class TimeRangeThroughputTests {

        @Test
        @DisplayName("Should calculate throughput within time range")
        void shouldCalculateThroughputInRange() {
            // Given: Aggregates at different times
            repository.saveAggregate(createAggregate(50, 0.0));
            repository.saveAggregate(createAggregate(100, 60.0));
            repository.saveAggregate(createAggregate(75, 120.0));
            repository.saveAggregate(createAggregate(80, 180.0));

            // When: Calculate throughput for range [60, 180]
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughputInRange(
                    SIMULATION_ID,
                    60.0,
                    180.0
            );

            // Then: Should only include aggregates in range
            assertThat(summary.getTotalTasksCompleted()).isEqualTo(255); // 100 + 75 + 80
        }

        @Test
        @DisplayName("Should average rate metrics across aggregates in range")
        void shouldAverageRateMetricsAcrossAggregates() {
            // Given: Multiple aggregates with different rates
            repository.saveAggregate(createAggregateWithRate(100, 0.0, 100.0));
            repository.saveAggregate(createAggregateWithRate(200, 60.0, 200.0));
            repository.saveAggregate(createAggregateWithRate(300, 120.0, 300.0));

            // When: Calculate across all
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughputInRange(
                    SIMULATION_ID,
                    0.0,
                    180.0
            );

            // Then: Should average the rates
            assertThat(summary.getTasksPerHour()).isEqualTo(200.0); // (100+200+300)/3
        }

        @Test
        @DisplayName("Should return zero for empty time range")
        void shouldReturnZeroForEmptyTimeRange() {
            // When: Calculate throughput with no aggregates
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughputInRange(
                    SIMULATION_ID,
                    0.0,
                    100.0
            );

            // Then: Should return zero values
            assertThat(summary.getTotalTasksCompleted()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Target Meeting Tests")
    class TargetMeetingTests {

        @Test
        @DisplayName("Should detect when throughput meets target")
        void shouldDetectWhenThroughputMeetsTarget() {
            // Given: Throughput of 100 tasks/hour
            repository.saveAggregate(createAggregateWithRate(100, 0.0, 100.0));

            // When: Check against target of 100 ± 5%
            boolean meets = calculator.meetsTarget(SIMULATION_ID, 100.0, 0.05);

            // Then: Should meet target
            assertThat(meets).isTrue();
        }

        @Test
        @DisplayName("Should detect when throughput is within tolerance")
        void shouldDetectWhenThroughputIsWithinTolerance() {
            // Given: Throughput of 105 tasks/hour
            repository.saveAggregate(createAggregateWithRate(105, 0.0, 105.0));

            // When: Check against target of 100 ± 10%
            boolean meets = calculator.meetsTarget(SIMULATION_ID, 100.0, 0.10);

            // Then: Should meet target (105 is within 90-110)
            assertThat(meets).isTrue();
        }

        @Test
        @DisplayName("Should detect when throughput below target")
        void shouldDetectWhenThroughputBelowTarget() {
            // Given: Throughput of 90 tasks/hour
            repository.saveAggregate(createAggregateWithRate(90, 0.0, 90.0));

            // When: Check against target of 100 ± 5%
            boolean meets = calculator.meetsTarget(SIMULATION_ID, 100.0, 0.05);

            // Then: Should not meet target (90 < 95)
            assertThat(meets).isFalse();
        }

        @Test
        @DisplayName("Should detect when throughput above target")
        void shouldDetectWhenThroughputAboveTarget() {
            // Given: Throughput of 110 tasks/hour
            repository.saveAggregate(createAggregateWithRate(110, 0.0, 110.0));

            // When: Check against target of 100 ± 5%
            boolean meets = calculator.meetsTarget(SIMULATION_ID, 100.0, 0.05);

            // Then: Should not meet target (110 > 105)
            assertThat(meets).isFalse();
        }
    }

    @Nested
    @DisplayName("Throughput By Type Tests")
    class ThroughputByTypeTests {

        @Test
        @DisplayName("Should group and count tasks by type")
        void shouldGroupAndCountTasksByType() {
            // Given: Task completion events with different types
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.TASK_COMPLETED,
                            10.0)
                    .entityId("TASK-001")
                    .entityType("TRANSPORT")
                    .data(new MetricData().putString("taskType", "TRANSPORT"))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.TASK_COMPLETED,
                            20.0)
                    .entityId("TASK-002")
                    .entityType("LOADING")
                    .data(new MetricData().putString("taskType", "LOADING"))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.TASK_COMPLETED,
                            30.0)
                    .entityId("TASK-003")
                    .entityType("TRANSPORT")
                    .data(new MetricData().putString("taskType", "TRANSPORT"))
                    .build());

            // When: Calculate throughput by type
            var throughputByType = calculator.calculateThroughputByType(SIMULATION_ID);

            // Then: Should group tasks by type
            assertThat(throughputByType).hasSize(2);
            assertThat(throughputByType.get("TRANSPORT")).isEqualTo(2);
            assertThat(throughputByType.get("LOADING")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should use UNKNOWN type when taskType is missing")
        void shouldUseUnknownTypeWhenTaskTypeMissing() {
            // Given: Task completion event without taskType
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.TASK_COMPLETED,
                            10.0)
                    .entityId("TASK-001")
                    .entityType("TRANSPORT")
                    .data(new MetricData())  // No taskType
                    .build());

            // When: Calculate throughput by type
            var throughputByType = calculator.calculateThroughputByType(SIMULATION_ID);

            // Then: Should categorize as UNKNOWN
            assertThat(throughputByType).hasSize(1);
            assertThat(throughputByType.get("UNKNOWN")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter events by simulation ID")
        void shouldFilterEventsBySimulationId() {
            // Given: Events from different simulations
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.TASK_COMPLETED,
                            10.0)
                    .entityId("TASK-001")
                    .entityType("TRANSPORT")
                    .data(new MetricData().putString("taskType", "TRANSPORT"))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            "other-simulation",
                            MetricEvent.EventTypes.TASK_COMPLETED,
                            10.0)
                    .entityId("TASK-002")
                    .entityType("LOADING")
                    .data(new MetricData().putString("taskType", "LOADING"))
                    .build());

            // When: Calculate throughput by type for SIMULATION_ID
            var throughputByType = calculator.calculateThroughputByType(SIMULATION_ID);

            // Then: Should only include events from SIMULATION_ID
            assertThat(throughputByType).hasSize(1);
            assertThat(throughputByType.get("TRANSPORT")).isEqualTo(1);
            assertThat(throughputByType.containsKey("LOADING")).isFalse();
        }

        @Test
        @DisplayName("Should return empty map when no task events exist")
        void shouldReturnEmptyMapWhenNoTaskEvents() {
            // When: Calculate throughput by type with no events
            var throughputByType = calculator.calculateThroughputByType(SIMULATION_ID);

            // Then: Should return empty map
            assertThat(throughputByType).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle non-existent simulation")
        void shouldHandleNonExistentSimulation() {
            // When: Calculate throughput for non-existent simulation
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughput("non-existent");

            // Then: Should return zero values
            assertThat(summary.getTotalTasksCompleted()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle division by zero in range calculation")
        void shouldHandleDivisionByZeroInRangeCalculation() {
            // Given: Aggregates with zero total tasks
            repository.saveAggregate(createAggregate(0, 0.0));
            repository.saveAggregate(createAggregate(0, 60.0));

            // When: Calculate throughput
            ThroughputCalculator.ThroughputSummary summary = calculator.calculateThroughputInRange(
                    SIMULATION_ID,
                    0.0,
                    60.0
            );

            // Then: Should not divide by zero
            assertThat(summary).isNotNull();
            assertThat(summary.getTotalTasksCompleted()).isEqualTo(0);
        }
    }

    // Helper methods

    private MetricAggregate createAggregate(int tasksCompleted, double recordedAt) {
        return new MetricAggregate.Builder(SIMULATION_ID, recordedAt)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(tasksCompleted)
                .tasksPerHour(tasksCompleted / 60.0 * 3600.0)  // Simplified
                .materialThroughput(tasksCompleted * 5.0)
                .vehicleUtilization(0.7)
                .equipmentUtilization(0.6)
                .wipTotal(10)
                .energyTotal(1000.0)
                .build();
    }

    private MetricAggregate createAggregateWithRate(int tasksCompleted, double recordedAt, double tasksPerHour) {
        return new MetricAggregate.Builder(SIMULATION_ID, recordedAt)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(tasksCompleted)
                .tasksPerHour(tasksPerHour)
                .materialThroughput(tasksPerHour * 5.0)
                .vehicleUtilization(0.7)
                .equipmentUtilization(0.6)
                .wipTotal(10)
                .energyTotal(1000.0)
                .build();
    }
}
