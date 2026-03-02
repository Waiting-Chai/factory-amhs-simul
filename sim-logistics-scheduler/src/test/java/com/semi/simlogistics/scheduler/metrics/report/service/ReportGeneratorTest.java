package com.semi.simlogistics.scheduler.metrics.report.service;

import com.semi.simlogistics.scheduler.metrics.calculator.EnergyCalculator;
import com.semi.simlogistics.scheduler.metrics.calculator.ThroughputCalculator;
import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryMetricsRepository;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import com.semi.simlogistics.scheduler.metrics.report.model.DashboardSnapshot;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for ReportGenerator (REQ-KPI-007).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("ReportGenerator Tests (REQ-KPI-007)")
class ReportGeneratorTest {

    private static final String SIMULATION_ID = "test-simulation-001";
    private MetricsRepositoryPort repository;
    private ReportGenerator generator;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        ThroughputCalculator throughputCalculator = new ThroughputCalculator(repository);
        EnergyCalculator energyCalculator = new EnergyCalculator(repository);
        generator = new ReportGenerator(repository, throughputCalculator, energyCalculator);
    }

    @Nested
    @DisplayName("Summary Report Tests")
    class SummaryReportTests {

        @Test
        @DisplayName("Should build summary report with required fields")
        void shouldBuildSummaryReportWithRequiredFields() {
            // Given: Aggregates with all required metrics
            repository.saveAggregate(createAggregate(
                    100,     // tasksCompleted
                    6000.0,  // tasksPerHour
                    1000.0,  // materialThroughput
                    0.75,    // vehicleUtilization
                    0.60,    // equipmentUtilization
                    5000.0,  // energyTotal
                    0.0
            ));

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should contain all required fields
            assertThat(report.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(report.getTotalTasksCompleted()).isEqualTo(100);
            assertThat(report.getTasksPerHour()).isEqualTo(6000.0);
            assertThat(report.getMaterialThroughput()).isEqualTo(1000.0);
            assertThat(report.getVehicleUtilization()).isEqualTo(0.75);
            assertThat(report.getEquipmentUtilization()).isEqualTo(0.60);
            assertThat(report.getTotalEnergy()).isEqualTo(5000.0);
        }

        @Test
        @DisplayName("Should calculate average completion time from aggregates")
        void shouldCalculateAverageCompletionTime() {
            // Given: Multiple aggregates
            repository.saveAggregate(createAggregate(50, 3000.0, 500.0, 0.7, 0.6, 2500.0, 0.0));
            repository.saveAggregate(createAggregate(75, 4500.0, 750.0, 0.8, 0.65, 3000.0, 60.0));

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should calculate average completion time (from task events)
            assertThat(report.getAverageCompletionTime()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle empty metrics gracefully")
        void shouldHandleEmptyMetricsGracefully() {
            // Given: No metrics data

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should return report with zero/default values
            assertThat(report.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(report.getTotalTasksCompleted()).isEqualTo(0);
            assertThat(report.getTasksPerHour()).isEqualTo(0.0);
            assertThat(report.getMaterialThroughput()).isEqualTo(0.0);
            assertThat(report.getVehicleUtilization()).isEqualTo(0.0);
            assertThat(report.getEquipmentUtilization()).isEqualTo(0.0);
            assertThat(report.getTotalEnergy()).isEqualTo(0.0);
            assertThat(report.getAverageCompletionTime()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should aggregate multiple time periods correctly")
        void shouldAggregateMultipleTimePeriodsCorrectly() {
            // Given: Multiple aggregates at different times
            repository.saveAggregate(createAggregate(100, 6000.0, 1000.0, 0.75, 0.60, 5000.0, 0.0));
            repository.saveAggregate(createAggregate(150, 7000.0, 1200.0, 0.80, 0.65, 5500.0, 60.0));
            repository.saveAggregate(createAggregate(120, 6500.0, 1100.0, 0.78, 0.62, 5200.0, 120.0));

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should sum up totals and average rates
            assertThat(report.getTotalTasksCompleted()).isEqualTo(370); // 100 + 150 + 120
        }

        @Test
        @DisplayName("Should include bottleneck identification in summary report")
        void shouldIncludeBottleneckIdentificationInSummaryReport() {
            // Given: Aggregates with high utilization (potential bottleneck)
            repository.saveAggregate(createAggregate(100, 6000.0, 1000.0, 0.95, 0.60, 5000.0, 0.0));

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should include bottleneck identification
            assertThat(report.getBottleneckSummary()).isNotNull();
            // High vehicle utilization should be detected
            assertThat(report.getBottleneckSummary()).isNotEmpty();
        }

        @Test
        @DisplayName("Should calculate average completion time from completionTime field")
        void shouldCalculateAverageCompletionTimeFromCompletionTimeField() {
            // Given: Aggregates and task events with completionTime
            repository.saveAggregate(createAggregate(100, 6000.0, 1000.0, 0.75, 0.60, 5000.0, 0.0));

            // Add task completion events with completionTime
            com.semi.simlogistics.scheduler.metrics.model.MetricData data1 =
                    new com.semi.simlogistics.scheduler.metrics.model.MetricData()
                            .putString("taskType", "TRANSPORT")
                            .putString("assignedVehicle", "V-001")
                            .putDouble("completionTime", 30.0);
            repository.saveEvent(new com.semi.simlogistics.scheduler.metrics.model.MetricEvent.Builder(
                            SIMULATION_ID,
                            com.semi.simlogistics.scheduler.metrics.model.MetricEvent.EventTypes.TASK_COMPLETED,
                            10.0)
                    .entityId("TASK-001")
                    .entityType("TRANSPORT")
                    .data(data1)
                    .build());

            com.semi.simlogistics.scheduler.metrics.model.MetricData data2 =
                    new com.semi.simlogistics.scheduler.metrics.model.MetricData()
                            .putString("taskType", "LOADING")
                            .putString("assignedVehicle", "V-002")
                            .putDouble("completionTime", 60.0);
            repository.saveEvent(new com.semi.simlogistics.scheduler.metrics.model.MetricEvent.Builder(
                            SIMULATION_ID,
                            com.semi.simlogistics.scheduler.metrics.model.MetricEvent.EventTypes.TASK_COMPLETED,
                            20.0)
                    .entityId("TASK-002")
                    .entityType("LOADING")
                    .data(data2)
                    .build());

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should calculate average completion time correctly
            assertThat(report.getAverageCompletionTime()).isEqualTo(45.0); // (30 + 60) / 2
        }

        @Test
        @DisplayName("Should return zero average completion time when no completionTime")
        void shouldReturnZeroAverageCompletionTimeWhenNoCompletionTime() {
            // Given: Aggregates without completionTime in events
            repository.saveAggregate(createAggregate(100, 6000.0, 1000.0, 0.75, 0.60, 5000.0, 0.0));

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should return zero when no completionTime data
            assertThat(report.getAverageCompletionTime()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Dashboard Snapshot Tests")
    class DashboardSnapshotTests {

        @Test
        @DisplayName("Should build dashboard snapshot with current and trend")
        void shouldBuildDashboardSnapshotWithCurrentAndTrend() {
            // Given: Multiple aggregates for trend data
            repository.saveAggregate(createAggregate(50, 3000.0, 500.0, 0.6, 0.5, 2000.0, 0.0));
            repository.saveAggregate(createAggregate(75, 4500.0, 750.0, 0.7, 0.6, 3000.0, 60.0));
            repository.saveAggregate(createAggregate(100, 6000.0, 1000.0, 0.8, 0.7, 4000.0, 120.0));

            // When: Generate dashboard snapshot
            DashboardSnapshot snapshot = generator.generateDashboardSnapshot(SIMULATION_ID);

            // Then: Should contain current KPI and trend data
            assertThat(snapshot.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(snapshot.getCurrentKpi()).isNotNull();
            assertThat(snapshot.getCurrentKpi().getTasksPerHour()).isEqualTo(6000.0);
            assertThat(snapshot.getTrendData()).hasSize(3);
        }

        @Test
        @DisplayName("Should provide stable field names for frontend consumption")
        void shouldProvideStableFieldNamesForFrontendConsumption() {
            // Given: Single aggregate
            repository.saveAggregate(createAggregate(100, 6000.0, 1000.0, 0.75, 0.60, 5000.0, 0.0));

            // When: Generate dashboard snapshot
            DashboardSnapshot snapshot = generator.generateDashboardSnapshot(SIMULATION_ID);

            // Then: Should have fixed, predictable field names
            assertThat(snapshot.getSimulationId()).isNotNull();
            assertThat(snapshot.getCurrentKpi()).isNotNull();
            assertThat(snapshot.getTrendData()).isNotNull();
            assertThat(snapshot.getGeneratedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty data for dashboard snapshot")
        void shouldHandleEmptyDataForDashboardSnapshot() {
            // Given: No metrics data

            // When: Generate dashboard snapshot
            DashboardSnapshot snapshot = generator.generateDashboardSnapshot(SIMULATION_ID);

            // Then: Should return snapshot with empty trend data
            assertThat(snapshot.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(snapshot.getTrendData()).isEmpty();
            assertThat(snapshot.getCurrentKpi().getTasksPerHour()).isEqualTo(0.0);
        }
    }

    // Helper methods

    private MetricAggregate createAggregate(
            int tasksCompleted,
            double tasksPerHour,
            double materialThroughput,
            double vehicleUtilization,
            double equipmentUtilization,
            double energyTotal,
            double recordedAt
    ) {
        return new MetricAggregate.Builder(SIMULATION_ID, recordedAt)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(tasksCompleted)
                .tasksPerHour(tasksPerHour)
                .materialThroughput(materialThroughput)
                .vehicleUtilization(vehicleUtilization)
                .equipmentUtilization(equipmentUtilization)
                .wipTotal(10)
                .energyTotal(energyTotal)
                .build();
    }
}
