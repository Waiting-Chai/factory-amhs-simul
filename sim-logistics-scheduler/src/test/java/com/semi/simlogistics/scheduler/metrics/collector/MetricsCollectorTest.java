package com.semi.simlogistics.scheduler.metrics.collector;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryConfig;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryMetricsRepository;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for MetricsCollector (REQ-KPI-001).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("MetricsCollector Tests (REQ-KPI-001)")
class MetricsCollectorTest {

    private static final String SIMULATION_ID = "test-simulation-001";
    private InMemoryMetricsRepository repository;
    private InMemoryConfig config;
    private MetricsCollector collector;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        config = new InMemoryConfig();
        collector = new MetricsCollector(SIMULATION_ID, repository, config);
    }

    @Nested
    @DisplayName("Event Sampling Tests")
    class EventSamplingTests {

        @Test
        @DisplayName("Should record task completion event with dual time基准")
        void shouldRecordTaskCompletionWithDualTime() {
            // When: Record task completion
            double simulatedTime = 123.456;
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", simulatedTime);

            // Then: Event should be saved
            assertThat(repository.getEventCount()).isEqualTo(1);

            MetricEvent event = repository.queryByEntityId("TASK-001").get(0);
            assertThat(event.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(event.getEventType()).isEqualTo(MetricEvent.EventTypes.TASK_COMPLETED);
            assertThat(event.getSimulatedTime()).isEqualTo(123.456);
            assertThat(event.getWallClockTime()).isNotNull(); // 墙钟时间存在
            assertThat(event.getEntityId()).isEqualTo("TASK-001");
        }

        @Test
        @DisplayName("Should increment task completion counter in period")
        void shouldIncrementTaskCompletionCounter() {
            // When: Record multiple task completions
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
            collector.recordTaskCompletion("TASK-002", "TRANSPORT", "V-002", 20.0);
            collector.recordTaskCompletion("TASK-003", "TRANSPORT", "V-001", 30.0);

            // Then: Counter should be incremented
            assertThat(collector.getTasksCompletedInPeriod()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should record task failure event")
        void shouldRecordTaskFailureEvent() {
            // When: Record task failure
            collector.recordTaskFailure("TASK-001", "TRANSPORT", "Vehicle unavailable", 50.0);

            // Then: Event should be saved
            assertThat(repository.getEventCount()).isEqualTo(1);

            MetricEvent event = repository.queryByEventType(MetricEvent.EventTypes.TASK_FAILED).get(0);
            assertThat(event.getEntityId()).isEqualTo("TASK-001");
            assertThat(event.getData().getString("failureReason", null)).isEqualTo("Vehicle unavailable");
        }

        @Test
        @DisplayName("Should record vehicle state change and track for utilization")
        void shouldRecordVehicleStateChange() {
            // When: Record state changes
            collector.recordVehicleStateChange("V-001", "IDLE", "MOVING", 10.0);
            collector.recordVehicleStateChange("V-001", "MOVING", "LOADING", 30.0);
            collector.recordVehicleStateChange("V-001", "LOADING", "MOVING", 35.0);

            // Then: Events should be saved
            assertThat(repository.queryByEntityId("V-001")).hasSize(3);
        }

        @Test
        @DisplayName("Should record vehicle movement event")
        void shouldRecordVehicleMovement() {
            // When: Record vehicle movement
            collector.recordVehicleMovement("V-001", 150.5, 25.0);

            // Then: Event should be saved
            MetricEvent event = repository.queryByEventType(MetricEvent.EventTypes.VEHICLE_MOVED).get(0);
            assertThat(event.getData().getDouble("distance", 0)).isEqualTo(150.5);
        }

        @Test
        @DisplayName("Should record energy consumption event")
        void shouldRecordEnergyConsumption() {
            // When: Record energy consumption
            collector.recordEnergyConsumption("V-001", "VEHICLE", 5000.0, 40.0);

            // Then: Event should be saved
            MetricEvent event = repository.queryByEventType(MetricEvent.EventTypes.ENERGY_CONSUMED).get(0);
            assertThat(event.getData().getDouble("energyWattSeconds", 0)).isEqualTo(5000.0);
        }

        @Test
        @DisplayName("Should update WIP count")
        void shouldUpdateWIP() {
            // When: Update WIP
            collector.updateWIP(25);

            // Then: WIP should be updated
            assertThat(collector.getWipTotal()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should not record events when sampling disabled")
        void shouldNotRecordEventsWhenSamplingDisabled() {
            // Given: Sampling disabled
            config.setBoolean("metrics.sampling.enabled", false);

            // When: Record event
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);

            // Then: No event should be saved
            assertThat(repository.getEventCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Aggregation Tests")
    class AggregationTests {

        @Test
        @DisplayName("Should trigger aggregation at interval (60 simulated seconds)")
        void shouldTriggerAggregationAtInterval() {
            // Given: Tasks completed
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
            collector.recordTaskCompletion("TASK-002", "TRANSPORT", "V-002", 20.0);

            // When: Trigger aggregation before interval
            MetricAggregate result1 = collector.triggerAggregation(30.0);
            assertThat(result1).isNull(); // No aggregation yet

            // When: Trigger aggregation at interval
            MetricAggregate result2 = collector.triggerAggregation(60.0);
            assertThat(result2).isNotNull();
            assertThat(result2.getTasksCompleted()).isEqualTo(2);

            // And: Counters should be reset
            assertThat(collector.getTasksCompletedInPeriod()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should create aggregate with correct metrics")
        void shouldCreateAggregateWithCorrectMetrics() {
            // Given: Some activity
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
            collector.recordTaskCompletion("TASK-002", "TRANSPORT", "V-002", 20.0);
            collector.recordTaskCompletion("TASK-003", "TRANSPORT", "V-001", 30.0);
            collector.updateWIP(15);
            collector.recordEnergyConsumption("V-001", "VEHICLE", 1000.0, 25.0);

            // When: Force aggregation
            MetricAggregate aggregate = collector.forceAggregation(60.0);

            // Then: Aggregate should contain correct values
            assertThat(aggregate.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(aggregate.getRecordedAt()).isEqualTo(60.0);
            assertThat(aggregate.getWallClockTime()).isNotNull();
            assertThat(aggregate.getTasksCompleted()).isEqualTo(3);
            assertThat(aggregate.getTasksPerHour()).isEqualTo(180.0); // 3 tasks / 60s * 3600
            assertThat(aggregate.getWipTotal()).isEqualTo(15);
            assertThat(aggregate.getEnergyTotal()).isEqualTo(1000.0);
        }

        @Test
        @DisplayName("Should calculate tasks per hour correctly")
        void shouldCalculateTasksPerHourCorrectly() {
            // Given: 5 tasks completed
            for (int i = 0; i < 5; i++) {
                collector.recordTaskCompletion("TASK-" + i, "TRANSPORT", "V-001", i * 5.0);
            }

            // When: Force aggregation at 60 seconds (default interval)
            collector.forceAggregation(60.0);

            // Then: Tasks per hour = 5 / 60 * 3600 = 300
            MetricAggregate aggregate = repository.getLatestAggregate(SIMULATION_ID).orElse(null);
            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getTasksPerHour()).isEqualTo(300.0);
        }

        @Test
        @DisplayName("Should reset accumulators after aggregation")
        void shouldResetAccumulatorsAfterAggregation() {
            // Given: Activity and aggregation
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
            collector.forceAggregation(60.0);

            // Then: Counters should be reset
            assertThat(collector.getTasksCompletedInPeriod()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should save aggregate to repository")
        void shouldSaveAggregateToRepository() {
            // When: Force aggregation
            collector.forceAggregation(60.0);

            // Then: Aggregate should be saved
            assertThat(repository.getAggregateCount()).isEqualTo(1);

            MetricAggregate aggregate = repository.getLatestAggregate(SIMULATION_ID).orElse(null);
            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getSimulationId()).isEqualTo(SIMULATION_ID);
        }
    }

    @Nested
    @DisplayName("Utilization Calculation Tests")
    class UtilizationCalculationTests {

        @Test
        @DisplayName("Should calculate time-weighted average utilization")
        void shouldCalculateTimeWeightedAverageUtilization() {
            // Given: Vehicle state transitions
            // Tracking starts at first state change (10.0s)
            // MOVING: 10-30s (20s, working)
            // LOADING: 30-35s (5s, working)
            // MOVING: 35-55s (20s, working)
            // IDLE: 55-60s (5s, not working)
            collector.recordVehicleStateChange("V-001", "IDLE", "MOVING", 10.0);
            collector.recordVehicleStateChange("V-001", "MOVING", "LOADING", 30.0);
            collector.recordVehicleStateChange("V-001", "LOADING", "MOVING", 35.0);
            collector.recordVehicleStateChange("V-001", "MOVING", "IDLE", 55.0);

            // When: Calculate utilization at 60s
            double utilization = collector.calculateAverageUtilization(60.0);

            // Then: Working time = 20 + 5 + 20 = 45s, Total tracking = 60-10 = 50s
            // Utilization = 45/50 = 0.9
            assertThat(utilization).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("Should return zero utilization when no states tracked")
        void shouldReturnZeroUtilizationWhenNoStatesTracked() {
            // When: Calculate utilization with no state tracking
            double utilization = collector.calculateAverageUtilization(100.0);

            // Then: Should return zero
            assertThat(utilization).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty repository gracefully")
        void shouldHandleEmptyRepository() {
            // When: Query summary from empty repository
            assertThat(repository.getSummary(SIMULATION_ID)).isNotPresent();
        }

        @Test
        @DisplayName("Should handle reset correctly")
        void shouldHandleReset() {
            // Given: Some activity
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
            collector.updateWIP(10);

            // When: Reset
            collector.reset();

            // Then: All state should be cleared
            assertThat(collector.getTasksCompletedInPeriod()).isEqualTo(0);
            assertThat(collector.getWipTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle zero elapsed time in aggregation")
        void shouldHandleZeroElapsedTimeInAggregation() {
            // When: Force aggregation at time 0
            MetricAggregate aggregate = collector.forceAggregation(0.0);

            // Then: Should not divide by zero
            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getTasksPerHour()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle null vehicle ID in task completion")
        void shouldHandleNullVehicleIdInTaskCompletion() {
            // When: Record task completion with null vehicle
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", null, 10.0);

            // Then: Event should be saved
            assertThat(repository.getEventCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use configured aggregation interval")
        void shouldUseConfiguredAggregationInterval() {
            // Given: Custom interval
            config.setDouble("metrics.aggregation.interval", 30.0);
            collector = new MetricsCollector(SIMULATION_ID, repository, config);

            // When: Record task and trigger at 25 seconds
            collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
            MetricAggregate result1 = collector.triggerAggregation(25.0);
            assertThat(result1).isNull();

            // When: Trigger at 30 seconds
            MetricAggregate result2 = collector.triggerAggregation(30.0);
            assertThat(result2).isNotNull();
        }
    }
}
