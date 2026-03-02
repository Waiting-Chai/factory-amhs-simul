package com.semi.simlogistics.scheduler.metrics.report;

import com.semi.simlogistics.scheduler.metrics.calculator.EnergyCalculator;
import com.semi.simlogistics.scheduler.metrics.calculator.ThroughputCalculator;
import com.semi.simlogistics.scheduler.metrics.collector.MetricsCollector;
import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.model.MetricData;
import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryConfig;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryMetricsRepository;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import com.semi.simlogistics.scheduler.metrics.report.exporter.CsvReportExporter;
import com.semi.simlogistics.scheduler.metrics.report.exporter.JsonReportExporter;
import com.semi.simlogistics.scheduler.metrics.report.exporter.PdfReportExporter;
import com.semi.simlogistics.scheduler.metrics.report.model.DashboardSnapshot;
import com.semi.simlogistics.scheduler.metrics.report.model.ExportResult;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;
import com.semi.simlogistics.scheduler.metrics.report.service.ReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 Report Export Integration Test (REQ-KPI-007).
 * <p>
 * End-to-end test for report generation and export.
 * Verifies that CSV/JSON/PDF formats can be generated with consistent field metrics.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("Phase 4 Report Export Integration Test (REQ-KPI-007)")
class Phase4ReportExportIntegrationTest {

    private static final String SIMULATION_ID = "integration-test-sim-001";
    private MetricsRepositoryPort repository;
    private MetricsCollector collector;
    private ReportGenerator generator;
    private CsvReportExporter csvExporter;
    private JsonReportExporter jsonExporter;
    private PdfReportExporter pdfExporter;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        collector = new MetricsCollector(SIMULATION_ID, repository, new InMemoryConfig());

        ThroughputCalculator throughputCalculator = new ThroughputCalculator(repository);
        EnergyCalculator energyCalculator = new EnergyCalculator(repository);
        generator = new ReportGenerator(repository, throughputCalculator, energyCalculator);

        csvExporter = new CsvReportExporter();
        jsonExporter = new JsonReportExporter();
        pdfExporter = new PdfReportExporter();
    }

    @Nested
    @DisplayName("End-to-End Report Generation")
    class EndToEndReportGenerationTests {

        @Test
        @DisplayName("Should assemble snapshot from metrics data stream")
        void shouldAssembleSnapshotFromMetricsDataStream() {
            // Given: Metrics data stream (events and aggregates)
            simulateMetricsCollection();

            // When: Generate dashboard snapshot
            DashboardSnapshot snapshot = generator.generateDashboardSnapshot(SIMULATION_ID);

            // Then: Should have current KPI and trend data
            assertThat(snapshot.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(snapshot.getCurrentKpi()).isNotNull();
            assertThat(snapshot.getTrendData()).isNotEmpty();
            assertThat(snapshot.getTrendData()).hasSizeGreaterThan(0);
        }

        @Test
        @DisplayName("Should generate summary report with all required fields")
        void shouldGenerateSummaryReportWithAllRequiredFields() {
            // Given: Metrics data stream
            simulateMetricsCollection();

            // When: Generate summary report
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // Then: Should have all required fields (REQ-KPI-007)
            assertThat(report.getSimulationId()).isEqualTo(SIMULATION_ID);
            assertThat(report.getTotalTasksCompleted()).isGreaterThan(0);
            assertThat(report.getTasksPerHour()).isGreaterThan(0);
            assertThat(report.getMaterialThroughput()).isGreaterThan(0);
            assertThat(report.getVehicleUtilization()).isGreaterThanOrEqualTo(0);
            assertThat(report.getEquipmentUtilization()).isGreaterThanOrEqualTo(0);
            assertThat(report.getTotalEnergy()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Multi-Format Export Tests")
    class MultiFormatExportTests {

        @Test
        @DisplayName("Should export all three formats successfully")
        void shouldExportAllThreeFormatsSuccessfully() {
            // Given: Summary report from metrics data
            simulateMetricsCollection();
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // When: Export to CSV, JSON, and PDF
            ExportResult csvResult = csvExporter.exportSummaryReport(report);
            ExportResult jsonResult = jsonExporter.exportSummaryReport(report);
            ExportResult pdfResult = pdfExporter.exportSummaryReport(report);

            // Then: All exports should succeed
            assertThat(csvResult).isNotNull();
            assertThat(csvResult.getContent()).isNotEmpty();
            assertThat(csvResult.getContentType()).isEqualTo("text/csv");

            assertThat(jsonResult).isNotNull();
            assertThat(jsonResult.getContent()).isNotEmpty();
            assertThat(jsonResult.getContentType()).isEqualTo("application/json");

            assertThat(pdfResult).isNotNull();
            assertThat(pdfResult.getContent()).isNotEmpty();
            assertThat(pdfResult.getContentType()).isEqualTo("text/plain");  // Phase 4: text/plain
        }

        @Test
        @DisplayName("Should maintain consistent metrics across formats")
        void shouldMaintainConsistentMetricsAcrossFormats() {
            // Given: Summary report with known values
            simulateMetricsCollection();
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // When: Export to all formats
            ExportResult csvResult = csvExporter.exportSummaryReport(report);
            ExportResult jsonResult = jsonExporter.exportSummaryReport(report);
            ExportResult pdfResult = pdfExporter.exportSummaryReport(report);

            // Then: Core metrics should be consistent across formats
            String csv = new String(csvResult.getContent(), StandardCharsets.UTF_8);
            String json = new String(jsonResult.getContent(), StandardCharsets.UTF_8);
            String pdf = new String(pdfResult.getContent(), StandardCharsets.UTF_8);

            // All formats should contain the simulation ID
            assertThat(csv).contains(SIMULATION_ID);
            assertThat(json).contains(SIMULATION_ID);
            assertThat(pdf).contains(SIMULATION_ID);

            // All formats should contain tasks completed count
            String tasksCompleted = String.valueOf(report.getTotalTasksCompleted());
            assertThat(csv).contains(tasksCompleted);
            assertThat(json).contains(tasksCompleted);
            assertThat(pdf).contains(tasksCompleted);
        }

        @Test
        @DisplayName("Should generate valid file names with timestamps")
        void shouldGenerateValidFileNamesWithTimestamps() {
            // Given: Summary report
            simulateMetricsCollection();
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // When: Export to all formats
            ExportResult csvResult = csvExporter.exportSummaryReport(report);
            ExportResult jsonResult = jsonExporter.exportSummaryReport(report);
            ExportResult pdfResult = pdfExporter.exportSummaryReport(report);

            // Then: File names should have proper extensions and timestamps
            assertThat(csvResult.getFileName()).endsWith(".csv");
            assertThat(csvResult.getFileName()).contains(SIMULATION_ID.replaceAll("[^a-zA-Z0-9_-]", "_"));

            assertThat(jsonResult.getFileName()).endsWith(".json");
            assertThat(jsonResult.getFileName()).contains(SIMULATION_ID.replaceAll("[^a-zA-Z0-9_-]", "_"));

            assertThat(pdfResult.getFileName()).endsWith(".txt");  // Phase 4: .txt, Phase 8: .pdf
            assertThat(pdfResult.getFileName()).contains(SIMULATION_ID.replaceAll("[^a-zA-Z0-9_-]", "_"));
        }

        @Test
        @DisplayName("Should include checksums for integrity verification")
        void shouldIncludeChecksumsForIntegrityVerification() {
            // Given: Summary report
            simulateMetricsCollection();
            SummaryReport report = generator.generateSummaryReport(SIMULATION_ID);

            // When: Export to all formats
            ExportResult csvResult = csvExporter.exportSummaryReport(report);
            ExportResult jsonResult = jsonExporter.exportSummaryReport(report);
            ExportResult pdfResult = pdfExporter.exportSummaryReport(report);

            // Then: All results should have SHA-256 checksums
            assertThat(csvResult.getChecksum()).isNotEmpty().hasSize(64); // SHA-256 hex
            assertThat(jsonResult.getChecksum()).isNotEmpty().hasSize(64);
            assertThat(pdfResult.getChecksum()).isNotEmpty().hasSize(64);
        }
    }

    @Nested
    @DisplayName("Empty Data Handling")
    class EmptyDataHandlingTests {

        @Test
        @DisplayName("Should export empty reports gracefully")
        void shouldExportEmptyReportsGracefully() {
            // Given: No metrics data
            SummaryReport emptyReport = SummaryReport.empty("empty-sim");

            // When: Export to all formats
            ExportResult csvResult = csvExporter.exportSummaryReport(emptyReport);
            ExportResult jsonResult = jsonExporter.exportSummaryReport(emptyReport);
            ExportResult pdfResult = pdfExporter.exportSummaryReport(emptyReport);

            // Then: Should not throw exceptions and should have content
            assertThat(csvResult.getContent()).isNotEmpty();
            assertThat(jsonResult.getContent()).isNotEmpty();
            assertThat(pdfResult.getContent()).isNotEmpty();
        }
    }

    /**
     * Simulate metrics collection for testing.
     */
    private void simulateMetricsCollection() {
        double currentTime = 0.0;

        // Simulate task completion events
        collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", 10.0);
        collector.recordTaskCompletion("TASK-002", "LOADING", "V-002", 20.0);
        collector.recordTaskCompletion("TASK-003", "TRANSPORT", "V-001", 30.0);

        // Simulate vehicle movement
        collector.recordVehicleMovement("V-001", 50.0, 15.0);
        collector.recordVehicleMovement("V-002", 75.0, 25.0);

        // Simulate energy consumption
        collector.recordEnergyConsumption("V-001", "VEHICLE", 1000.0, 10.0);
        collector.recordEnergyConsumption("V-002", "VEHICLE", 1500.0, 20.0);
        collector.recordEnergyConsumption("E-001", "EQUIPMENT", 500.0, 30.0);

        // Simulate WIP
        collector.updateWIP(10);

        // Trigger aggregation at 60 seconds
        collector.forceAggregation(60.0);

        // More events
        collector.recordTaskCompletion("TASK-004", "TRANSPORT", "V-002", 70.0);
        collector.recordTaskCompletion("TASK-005", "UNLOADING", "V-001", 80.0);

        // Trigger aggregation at 120 seconds
        collector.forceAggregation(120.0);
    }
}
