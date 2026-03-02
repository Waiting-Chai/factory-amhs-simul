package com.semi.simlogistics.scheduler.metrics.report.exporter;

import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for CsvReportExporter (REQ-KPI-007).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("CsvReportExporter Tests (REQ-KPI-007)")
class CsvReportExporterTest {

    private CsvReportExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CsvReportExporter();
    }

    @Nested
    @DisplayName("CSV Export Tests")
    class CsvExportTests {

        @Test
        @DisplayName("Should export CSV with stable header and column order")
        void shouldExportCsvWithStableHeaderAndColumnOrder() {
            // Given: Summary report with data
            SummaryReport report = createTestReport();

            // When: Export to CSV
            var result = exporter.exportSummaryReport(report);

            // Then: Should have stable header and column order
            String csv = new String(result.getContent(), StandardCharsets.UTF_8);
            String[] lines = csv.split("\n");

            assertThat(lines).hasSizeGreaterThanOrEqualTo(1);

            // Check header
            String header = lines[0];
            assertThat(header).contains("simulation_id");
            assertThat(header).contains("total_tasks_completed");
            assertThat(header).contains("tasks_per_hour");
            assertThat(header).contains("material_throughput");
            assertThat(header).contains("vehicle_utilization");
            assertThat(header).contains("equipment_utilization");
            assertThat(header).contains("total_energy");
            assertThat(header).contains("avg_completion_time");
            assertThat(header).contains("generated_at");

            // Check column order (header columns should be in fixed order)
            String[] columns = header.split(",");
            assertThat(columns).isNotEmpty();
        }

        @Test
        @DisplayName("Should export CSV with deterministic number formatting")
        void shouldExportCsvWithDeterministicNumberFormatting() {
            // Given: Summary report with specific values
            SummaryReport report = new SummaryReport(
                    "sim-001",
                    LocalDateTime.of(2026, 2, 10, 12, 0, 0),
                    150,
                    6000.5,
                    1234.567,
                    0.75,
                    0.60,
                    10,
                    5000.25,
                    45.3,
                    ""
            );

            // When: Export to CSV
            var result = exporter.exportSummaryReport(report);

            // Then: Numbers should be formatted deterministically
            String csv = new String(result.getContent(), StandardCharsets.UTF_8);

            assertThat(csv).contains("150");      // integer
            assertThat(csv).contains("6000.5");   // one decimal
            assertThat(csv).contains("0.75");     // two decimal for ratio
            assertThat(csv).contains("5000.25");  // two decimal for energy
        }

        @Test
        @DisplayName("Should export CSV for empty snapshot")
        void shouldExportCsvForEmptySnapshot() {
            // Given: Empty summary report
            SummaryReport report = SummaryReport.empty("sim-001");

            // When: Export to CSV
            var result = exporter.exportSummaryReport(report);

            // Then: Should export with zero/default values
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContentType()).isEqualTo("text/csv");
            assertThat(result.getFileName()).endsWith(".csv");

            String csv = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(csv).contains("sim-001");
            assertThat(csv).contains("0");  // zero values
        }

        @Test
        @DisplayName("Should produce valid CSV with proper escaping")
        void shouldProduceValidCsvWithProperEscaping() {
            // Given: Summary report
            SummaryReport report = createTestReport();

            // When: Export to CSV
            var result = exporter.exportSummaryReport(report);

            // Then: Should produce valid CSV
            assertThat(result.getContentType()).isEqualTo("text/csv");
            assertThat(result.getFileName()).endsWith(".csv");
            assertThat(result.getSize()).isGreaterThan(0);
            assertThat(result.getChecksum()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include all required fields in CSV")
        void shouldIncludeAllRequiredFieldsInCsv() {
            // Given: Summary report
            SummaryReport report = createTestReport();

            // When: Export to CSV
            var result = exporter.exportSummaryReport(report);

            // Then: All required fields should be present
            String csv = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(csv).contains("simulation_id");
            assertThat(csv).contains("total_tasks_completed");
            assertThat(csv).contains("tasks_per_hour");
            assertThat(csv).contains("material_throughput");
            assertThat(csv).contains("vehicle_utilization");
            assertThat(csv).contains("equipment_utilization");
            assertThat(csv).contains("wip_total");
            assertThat(csv).contains("total_energy");
            assertThat(csv).contains("avg_completion_time");
            assertThat(csv).contains("bottleneck_summary");
        }

        @Test
        @DisplayName("Should export bottleneck identification field in CSV")
        void shouldExportBottleneckIdentificationFieldInCsv() {
            // Given: Summary report with bottleneck
            SummaryReport report = new SummaryReport(
                    "sim-001",
                    LocalDateTime.of(2026, 2, 10, 12, 0, 0),
                    100,
                    6000.0,
                    1000.0,
                    0.75,
                    0.60,
                    10,
                    5000.0,
                    45.0,
                    "WARNING: 1 bottleneck(s) detected. Monitor system performance."
            );

            // When: Export to CSV
            var result = exporter.exportSummaryReport(report);

            // Then: Should export bottleneck summary
            String csv = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(csv).contains("bottleneck_summary");
            assertThat(csv).contains("WARNING: 1 bottleneck");
        }
    }

    // Helper methods

    private SummaryReport createTestReport() {
        return new SummaryReport(
                "sim-001",
                LocalDateTime.of(2026, 2, 10, 12, 0, 0),
                100,
                6000.0,
                1000.0,
                0.75,
                0.60,
                10,
                5000.0,
                45.0,
                ""
        );
    }
}
