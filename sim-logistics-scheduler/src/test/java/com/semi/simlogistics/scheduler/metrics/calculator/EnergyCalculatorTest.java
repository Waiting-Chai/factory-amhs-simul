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
 * TDD tests for EnergyCalculator (REQ-KPI-004).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("EnergyCalculator Tests (REQ-KPI-004)")
class EnergyCalculatorTest {

    private static final String SIMULATION_ID = "test-simulation-001";
    private MetricsRepositoryPort repository;
    private EnergyCalculator calculator;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        calculator = new EnergyCalculator(repository);
    }

    @Nested
    @DisplayName("Total Energy Tests")
    class TotalEnergyTests {

        @Test
        @DisplayName("Should calculate total energy from aggregates")
        void shouldCalculateTotalEnergyFromAggregates() {
            // Given: Multiple aggregates with energy consumption
            repository.saveAggregate(createAggregate(1000.0, 0.0));
            repository.saveAggregate(createAggregate(1500.0, 60.0));
            repository.saveAggregate(createAggregate(2000.0, 120.0));

            // When: Calculate energy consumption
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption(SIMULATION_ID);

            // Then: Should sum up all energy
            assertThat(summary.getTotalEnergy()).isEqualTo(4500.0);
        }

        @Test
        @DisplayName("Should calculate energy per task correctly")
        void shouldCalculateEnergyPerTaskCorrectly() {
            // Given: Aggregate with known energy and tasks
            MetricAggregate aggregate = new MetricAggregate.Builder(SIMULATION_ID, 60.0)
                    .tasksCompleted(100)
                    .energyTotal(5000.0)
                    .build();
            repository.saveAggregate(aggregate);

            // When: Calculate energy consumption
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption(SIMULATION_ID);

            // Then: Should calculate energy per task
            assertThat(summary.getEnergyPerTask()).isEqualTo(50.0); // 5000 / 100
        }

        @Test
        @DisplayName("Should return zero summary for empty repository")
        void shouldReturnZeroSummaryForEmptyRepository() {
            // When: Calculate energy with no data
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption(SIMULATION_ID);

            // Then: Should return zero values
            assertThat(summary.getTotalEnergy()).isEqualTo(0.0);
            assertThat(summary.getVehicleEnergy()).isEqualTo(0.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Energy By Entity Tests")
    class EnergyByEntityTests {

        @Test
        @DisplayName("Should group energy consumption by entity")
        void shouldGroupEnergyConsumptionByEntity() {
            // Given: Energy events from different entities
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 1000.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            20.0)
                    .entityId("V-002")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 1500.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            30.0)
                    .entityId("E-001")
                    .entityType("EQUIPMENT")
                    .data(new MetricData().putDouble("energyWattSeconds", 500.0))
                    .build());

            // When: Calculate energy by entity
            var energyByEntity = calculator.calculateEnergyByEntity(SIMULATION_ID);

            // Then: Should group by entityType:entityId
            assertThat(energyByEntity).hasSize(3);
            assertThat(energyByEntity.get("VEHICLE:V-001")).isEqualTo(1000.0);
            assertThat(energyByEntity.get("VEHICLE:V-002")).isEqualTo(1500.0);
            assertThat(energyByEntity.get("EQUIPMENT:E-001")).isEqualTo(500.0);
        }

        @Test
        @DisplayName("Should sum multiple energy events for same entity")
        void shouldSumMultipleEnergyEventsForSameEntity() {
            // Given: Multiple energy events for same vehicle
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 1000.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            20.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 500.0))
                    .build());

            // When: Calculate energy by entity
            var energyByEntity = calculator.calculateEnergyByEntity(SIMULATION_ID);

            // Then: Should sum events for same entity
            assertThat(energyByEntity.get("VEHICLE:V-001")).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("Should filter energy events by simulation ID")
        void shouldFilterEnergyEventsBySimulationId() {
            // Given: Energy events from different simulations
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 1000.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            "other-simulation",
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-002")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 500.0))
                    .build());

            // When: Calculate energy by entity for SIMULATION_ID
            var energyByEntity = calculator.calculateEnergyByEntity(SIMULATION_ID);

            // Then: Should only include events from SIMULATION_ID
            assertThat(energyByEntity).hasSize(1);
            assertThat(energyByEntity.get("VEHICLE:V-001")).isEqualTo(1000.0);
            assertThat(energyByEntity.containsKey("VEHICLE:V-002")).isFalse();
        }
    }

    @Nested
    @DisplayName("Vehicle vs Equipment Energy Tests")
    class VehicleVsEquipmentEnergyTests {

        @Test
        @DisplayName("Should separate vehicle and equipment energy from events")
        void shouldSeparateVehicleAndEquipmentEnergyFromEvents() {
            // Given: Aggregate with total energy and energy events
            repository.saveAggregate(createAggregate(5000.0, 60.0));

            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 3000.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            20.0)
                    .entityId("E-001")
                    .entityType("EQUIPMENT")
                    .data(new MetricData().putDouble("energyWattSeconds", 2000.0))
                    .build());

            // When: Calculate energy consumption
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption(SIMULATION_ID);

            // Then: Should separate vehicle and equipment energy
            assertThat(summary.getVehicleEnergy()).isEqualTo(3000.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(2000.0);
        }

        @Test
        @DisplayName("Should use total energy when no events recorded")
        void shouldUseTotalEnergyWhenNoEventsRecorded() {
            // Given: Aggregate with total energy but no events
            repository.saveAggregate(createAggregate(5000.0, 60.0));

            // When: Calculate energy consumption
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption(SIMULATION_ID);

            // Then: Should use total energy as vehicle energy (default)
            assertThat(summary.getVehicleEnergy()).isEqualTo(5000.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should ensure totalEnergy matches vehicleEnergy + equipmentEnergy when events exist")
        void shouldEnsureTotalEnergyMatchesSumWhenEventsExist() {
            // Given: Energy events from vehicles and equipment
            repository.saveAggregate(createAggregate(6000.0, 60.0));  // Aggregate total (will be overridden)

            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 3500.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            20.0)
                    .entityId("E-001")
                    .entityType("EQUIPMENT")
                    .data(new MetricData().putDouble("energyWattSeconds", 1500.0))
                    .build());

            // When: Calculate energy consumption
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption(SIMULATION_ID);

            // Then: totalEnergy should match vehicleEnergy + equipmentEnergy
            assertThat(summary.getTotalEnergy()).isEqualTo(5000.0);  // 3500 + 1500
            assertThat(summary.getVehicleEnergy()).isEqualTo(3500.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(1500.0);
        }
    }

    @Nested
    @DisplayName("Time Range Energy Tests")
    class TimeRangeEnergyTests {

        @Test
        @DisplayName("Should only count events within time range")
        void shouldOnlyCountEventsWithinTimeRange() {
            // Given: Events inside and outside the time range [30, 90]
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            20.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 1000.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            50.0)
                    .entityId("V-002")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 2000.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            100.0)
                    .entityId("E-001")
                    .entityType("EQUIPMENT")
                    .data(new MetricData().putDouble("energyWattSeconds", 1500.0))
                    .build());

            repository.saveAggregate(createAggregate(6000.0, 60.0));  // Aggregate in range

            // When: Calculate energy in range [30, 90]
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyInRange(
                    SIMULATION_ID,
                    30.0,
                    90.0
            );

            // Then: Should only include event at time 50.0
            assertThat(summary.getVehicleEnergy()).isEqualTo(2000.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(0.0);
            assertThat(summary.getEnergyByEntity()).hasSize(1);
            assertThat(summary.getEnergyByEntity().get("VEHICLE:V-002")).isEqualTo(2000.0);
        }

        @Test
        @DisplayName("Should ensure totalEnergy matches sum in time range")
        void shouldEnsureTotalEnergyMatchesSumInRange() {
            // Given: Events in time range [0, 100]
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            30.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 2500.0))
                    .build());
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            60.0)
                    .entityId("E-001")
                    .entityType("EQUIPMENT")
                    .data(new MetricData().putDouble("energyWattSeconds", 1500.0))
                    .build());

            repository.saveAggregate(createAggregate(5000.0, 60.0));  // Different total

            // When: Calculate energy in range [0, 100]
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyInRange(
                    SIMULATION_ID,
                    0.0,
                    100.0
            );

            // Then: totalEnergy should match vehicleEnergy + equipmentEnergy
            assertThat(summary.getTotalEnergy()).isEqualTo(4000.0);  // 2500 + 1500
            assertThat(summary.getVehicleEnergy()).isEqualTo(2500.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("Should fallback to aggregate total when no events in range")
        void shouldFallbackToAggregateTotalWhenNoEventsInRange() {
            // Given: Aggregates in range but no events in range
            repository.saveAggregate(createAggregate(5000.0, 60.0));  // In range [0, 100]

            // Event outside range
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            150.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData().putDouble("energyWattSeconds", 3000.0))
                    .build());

            // When: Calculate energy in range [0, 100]
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyInRange(
                    SIMULATION_ID,
                    0.0,
                    100.0
            );

            // Then: Should use aggregate total as fallback
            assertThat(summary.getTotalEnergy()).isEqualTo(5000.0);
            assertThat(summary.getVehicleEnergy()).isEqualTo(5000.0);
            assertThat(summary.getEquipmentEnergy()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Efficiency Score Tests")
    class EfficiencyScoreTests {

        @Test
        @DisplayName("Should calculate efficiency score correctly")
        void shouldCalculateEfficiencyScoreCorrectly() {
            // Given: Baseline of 100 energy per task, actual of 50
            repository.saveAggregate(createAggregateWithTasks(5000.0, 100));

            // When: Calculate efficiency score
            double score = calculator.calculateEfficiencyScore(SIMULATION_ID, 100.0);

            // Then: Score should be 2.0 (twice as efficient)
            assertThat(score).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should return zero score when no energy consumed")
        void shouldReturnZeroScoreWhenNoEnergyConsumed() {
            // Given: Aggregate with zero energy
            repository.saveAggregate(createAggregate(0.0, 60.0));

            // When: Calculate efficiency score
            double score = calculator.calculateEfficiencyScore(SIMULATION_ID, 100.0);

            // Then: Should return zero
            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle non-existent simulation")
        void shouldHandleNonExistentSimulation() {
            // When: Calculate energy for non-existent simulation
            EnergyCalculator.EnergySummary summary = calculator.calculateEnergyConsumption("non-existent");

            // Then: Should return zero values
            assertThat(summary.getTotalEnergy()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle missing energy data gracefully")
        void shouldHandleMissingEnergyDataGracefully() {
            // Given: Event without energy data
            repository.saveEvent(new MetricEvent.Builder(
                            SIMULATION_ID,
                            MetricEvent.EventTypes.ENERGY_CONSUMED,
                            10.0)
                    .entityId("V-001")
                    .entityType("VEHICLE")
                    .data(new MetricData())  // No energyWattSeconds
                    .build());

            // When: Calculate energy by entity
            var energyByEntity = calculator.calculateEnergyByEntity(SIMULATION_ID);

            // Then: Should use default value of 0
            assertThat(energyByEntity.get("VEHICLE:V-001")).isEqualTo(0.0);
        }
    }

    // Helper methods

    private MetricAggregate createAggregate(double energyTotal, double recordedAt) {
        return new MetricAggregate.Builder(SIMULATION_ID, recordedAt)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(50)
                .tasksPerHour(3000.0)
                .materialThroughput(15000.0)
                .vehicleUtilization(0.7)
                .equipmentUtilization(0.6)
                .wipTotal(10)
                .energyTotal(energyTotal)
                .build();
    }

    private MetricAggregate createAggregateWithTasks(double energyTotal, int tasksCompleted) {
        return new MetricAggregate.Builder(SIMULATION_ID, 60.0)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(tasksCompleted)
                .tasksPerHour(3000.0)
                .materialThroughput(15000.0)
                .vehicleUtilization(0.7)
                .equipmentUtilization(0.6)
                .wipTotal(10)
                .energyTotal(energyTotal)
                .build();
    }
}
