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
 * TDD tests for BottleneckAnalyzer (REQ-KPI-005).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("BottleneckAnalyzer Tests (REQ-KPI-005)")
class BottleneckAnalyzerTest {

    private static final String SIMULATION_ID = "test-simulation-001";
    private MetricsRepositoryPort repository;
    private InMemoryConfig config;
    private BottleneckAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        config = new InMemoryConfig();
        analyzer = new BottleneckAnalyzer(repository, config);
    }

    @Nested
    @DisplayName("Utilization Bottleneck Tests")
    class UtilizationBottleneckTests {

        @Test
        @DisplayName("Should detect critical vehicle utilization")
        void shouldDetectCriticalVehicleUtilization() {
            // Given: High vehicle utilization
            repository.saveAggregate(createAggregate(0.96, 0.5));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect critical bottleneck
            assertThat(report.hasCriticalBottlenecks()).isTrue();
            assertThat(report.getUtilizationBottlenecks()).anyMatch(item ->
                    item.getId().equals("VEHICLES") &&
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.CRITICAL
            );
        }

        @Test
        @DisplayName("Should detect warning level utilization")
        void shouldDetectWarningLevelUtilization() {
            // Given: Moderate high utilization (above warning threshold)
            repository.saveAggregate(createAggregate(0.85, 0.5));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect warning
            assertThat(report.getUtilizationBottlenecks()).anyMatch(item ->
                    item.getId().equals("VEHICLES") &&
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.WARNING
            );
        }

        @Test
        @DisplayName("Should not detect bottleneck when utilization normal")
        void shouldNotDetectBottleneckWhenUtilizationNormal() {
            // Given: Normal utilization
            repository.saveAggregate(createAggregate(0.6, 0.5));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should not have utilization bottlenecks
            assertThat(report.getUtilizationBottlenecks()).isEmpty();
        }

        @Test
        @DisplayName("Should detect equipment utilization bottleneck")
        void shouldDetectEquipmentUtilizationBottleneck() {
            // Given: High equipment utilization
            repository.saveAggregate(createAggregate(0.5, 0.97));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect equipment bottleneck
            assertThat(report.getUtilizationBottlenecks()).anyMatch(item ->
                    item.getId().equals("EQUIPMENT") &&
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.CRITICAL
            );
        }

        @Test
        @DisplayName("Should sort utilization bottlenecks by value descending")
        void shouldSortUtilizationBottlenecksByValueDescending() {
            // Given: Both vehicle and equipment at high utilization
            repository.saveAggregate(createAggregate(0.90, 0.85));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should be sorted descending
            var bottlenecks = report.getUtilizationBottlenecks();
            assertThat(bottlenecks).hasSize(2);
            assertThat(bottlenecks.get(0).getValue()).isGreaterThanOrEqualTo(bottlenecks.get(1).getValue());
        }
    }

    @Nested
    @DisplayName("WIP Bottleneck Tests")
    class WipBottleneckTests {

        @Test
        @DisplayName("Should detect critical WIP level")
        void shouldDetectCriticalWIPLevel() {
            // Given: WIP at 2x warning threshold
            int wipCount = config.getInt("wip.warning.threshold", 100) * 2;
            repository.saveAggregate(createAggregateWithWIP(wipCount));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect critical WIP bottleneck
            assertThat(report.getWipBottlenecks()).anyMatch(item ->
                    item.getId().equals("WIP_GLOBAL") &&
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.CRITICAL
            );
        }

        @Test
        @DisplayName("Should detect warning WIP level")
        void shouldDetectWarningWIPLevel() {
            // Given: WIP at warning threshold
            int wipCount = config.getInt("wip.warning.threshold", 100);
            repository.saveAggregate(createAggregateWithWIP(wipCount));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect warning
            assertThat(report.getWipBottlenecks()).anyMatch(item ->
                    item.getId().equals("WIP_GLOBAL") &&
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.WARNING
            );
        }

        @Test
        @DisplayName("Should not detect WIP bottleneck when below threshold")
        void shouldNotDetectWIPBottleneckWhenBelowThreshold() {
            // Given: Low WIP
            repository.saveAggregate(createAggregateWithWIP(50));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should not have WIP bottlenecks
            assertThat(report.getWipBottlenecks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Congestion Detection Tests")
    class CongestionDetectionTests {

        @Test
        @DisplayName("Should detect declining throughput")
        void shouldDetectDecliningThroughput() {
            // Given: Declining throughput over time (100 -> 90 -> 80 -> 70 -> 60 -> 55)
            repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 100.0, 0.0));
            repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 90.0, 60.0));
            repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 80.0, 120.0));
            repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 70.0, 180.0));
            repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 60.0, 240.0));
            repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 55.0, 300.0));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect declining throughput
            assertThat(report.getCongestionBottlenecks()).anyMatch(item ->
                    item.getId().equals("THROUGHPUT_DECLINE") &&
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.WARNING
            );
        }

        @Test
        @DisplayName("Should not detect congestion with stable throughput")
        void shouldNotDetectCongestionWithStableThroughput() {
            // Given: Stable throughput (all 100 tasks/hour)
            for (int i = 0; i < 10; i++) {
                repository.saveAggregate(createAggregateWithRate(0.5, 0.5, 100.0, i * 60.0));
            }

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should not detect congestion
            assertThat(report.getCongestionBottlenecks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Report Generation Tests")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should generate correct summary message for no bottlenecks")
        void shouldGenerateCorrectSummaryForNoBottlenecks() {
            // Given: Normal metrics
            repository.saveAggregate(createAggregate(0.5, 0.5));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Message should indicate no bottlenecks
            assertThat(report.getMessage()).contains("No significant bottlenecks");
        }

        @Test
        @DisplayName("Should generate summary with warning count")
        void shouldGenerateSummaryWithWarningCount() {
            // Given: Warning level utilization
            repository.saveAggregate(createAggregate(0.85, 0.5));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Message should mention warnings
            assertThat(report.getMessage()).contains("WARNING");
        }

        @Test
        @DisplayName("Should generate summary with critical count")
        void shouldGenerateSummaryWithCriticalCount() {
            // Given: Critical utilization
            repository.saveAggregate(createAggregate(0.96, 0.5));

            // When: Analyze bottlenecks
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Message should mention critical
            assertThat(report.getMessage()).contains("CRITICAL");
        }

        @Test
        @DisplayName("Should handle empty repository gracefully")
        void shouldHandleEmptyRepositoryGracefully() {
            // When: Analyze with no data
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should return report with empty lists
            assertThat(report.getAllBottlenecks()).isEmpty();
            assertThat(report.getMessage()).contains("No metrics data");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should use configured thresholds")
        void shouldUseConfiguredThresholds() {
            // Given: Custom thresholds
            config.setDouble("utilization.warning.threshold", 0.5);
            config.setDouble("utilization.critical.threshold", 0.7);
            analyzer = new BottleneckAnalyzer(repository, config);

            // Given: Utilization at 0.6 (above custom warning, below custom critical)
            repository.saveAggregate(createAggregate(0.6, 0.5));

            // When: Analyze
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should detect warning (not critical)
            assertThat(report.getUtilizationBottlenecks()).anyMatch(item ->
                    item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.WARNING
            );
        }

        @Test
        @DisplayName("Should return all bottlenecks regardless of type")
        void shouldReturnAllBottlenecksRegardlessOfType() {
            // Given: Single aggregate with multiple bottleneck types (high vehicle/util + high WIP)
            MetricAggregate aggregate = new MetricAggregate.Builder(SIMULATION_ID, 60.0)
                    .wallClockTime(LocalDateTime.now())
                    .tasksCompleted(50)
                    .tasksPerHour(3000.0)
                    .materialThroughput(15000.0)
                    .vehicleUtilization(0.96)  // Critical
                    .equipmentUtilization(0.97) // Critical
                    .wipTotal(250)  // Critical (2x warning threshold)
                    .energyTotal(5000.0)
                    .build();
            repository.saveAggregate(aggregate);

            // When: Analyze
            BottleneckAnalyzer.BottleneckReport report = analyzer.analyzeBottlenecks(SIMULATION_ID);

            // Then: Should include all bottlenecks (vehicles, equipment, WIP)
            assertThat(report.getAllBottlenecks()).hasSizeGreaterThan(2);
        }
    }

    // Helper methods

    private MetricAggregate createAggregate(double vehicleUtil, double equipmentUtil) {
        return new MetricAggregate.Builder(SIMULATION_ID, 60.0)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(50)
                .tasksPerHour(3000.0)
                .materialThroughput(15000.0)
                .vehicleUtilization(vehicleUtil)
                .equipmentUtilization(equipmentUtil)
                .wipTotal(50)
                .energyTotal(5000.0)
                .build();
    }

    private MetricAggregate createAggregateWithWIP(int wipTotal) {
        return new MetricAggregate.Builder(SIMULATION_ID, 60.0)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(50)
                .tasksPerHour(3000.0)
                .materialThroughput(15000.0)
                .vehicleUtilization(0.6)
                .equipmentUtilization(0.5)
                .wipTotal(wipTotal)
                .energyTotal(5000.0)
                .build();
    }

    private MetricAggregate createAggregateWithRate(double vehicleUtil, double equipmentUtil, double tasksPerHour, double recordedAt) {
        return new MetricAggregate.Builder(SIMULATION_ID, recordedAt)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(50)
                .tasksPerHour(tasksPerHour)
                .materialThroughput(tasksPerHour * 5.0)
                .vehicleUtilization(vehicleUtil)
                .equipmentUtilization(equipmentUtil)
                .wipTotal(50)
                .energyTotal(5000.0)
                .build();
    }
}
