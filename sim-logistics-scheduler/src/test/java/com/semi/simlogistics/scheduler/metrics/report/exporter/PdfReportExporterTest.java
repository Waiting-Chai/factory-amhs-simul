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
 * TDD tests for PdfReportExporter (REQ-KPI-007).
 * <p>
 * Note: Phase 4 uses simplified text-based PDF format.
 * Full PDF generation with proper formatting will be implemented in Phase 8.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("PdfReportExporter Tests (REQ-KPI-007)")
class PdfReportExporterTest {

    private PdfReportExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new PdfReportExporter();
    }

    @Nested
    @DisplayName("PDF Export Tests")
    class PdfExportTests {

        @Test
        @DisplayName("Should export PDF with non-empty content")
        void shouldExportPdfWithNonEmptyContent() {
            // Given: Summary report with data
            SummaryReport report = createTestReport();

            // When: Export to PDF
            var result = exporter.exportSummaryReport(report);

            // Then: Should have non-empty content
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getSize()).isGreaterThan(0);
            assertThat(result.getContentType()).isEqualTo("text/plain");
            assertThat(result.getFileName()).endsWith(".txt");
        }

        @Test
        @DisplayName("Should contain key sections in PDF text")
        void shouldContainKeySectionsInPdfText() {
            // Given: Summary report with data
            SummaryReport report = createTestReport();

            // When: Export to PDF
            var result = exporter.exportSummaryReport(report);

            // Then: Should contain key sections (text-based PDF for Phase 4)
            String content = new String(result.getContent(), StandardCharsets.UTF_8);

            // For Phase 4, we use text-based format
            // Key sections should be present
            assertThat(content).contains("KPI SUMMARY REPORT");  // Upper case
            assertThat(content).contains("Simulation ID");  // Mixed case as in output
            assertThat(content).contains("THROUGHPUT METRICS");  // Upper case section header
            assertThat(content).contains("RESOURCE UTILIZATION");  // Upper case section header
            assertThat(content).contains("ENERGY CONSUMPTION");  // Upper case section header
        }

        @Test
        @DisplayName("Should export PDF for empty snapshot")
        void shouldExportPdfForEmptySnapshot() {
            // Given: Empty summary report
            SummaryReport report = SummaryReport.empty("sim-001");

            // When: Export to PDF
            var result = exporter.exportSummaryReport(report);

            // Then: Should export with zero/default values
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContentType()).isEqualTo("text/plain");  // Phase 4: text/plain
            assertThat(result.getFileName()).endsWith(".txt");  // Phase 4: .txt

            String content = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(content).contains("sim-001");
        }

        @Test
        @DisplayName("Should produce valid file metadata")
        void shouldProduceValidFileMetadata() {
            // Given: Summary report
            SummaryReport report = createTestReport();

            // When: Export to PDF
            var result = exporter.exportSummaryReport(report);

            // Then: Should have valid metadata (Phase 4 uses text/plain)
            assertThat(result.getFileName()).endsWith(".txt");  // Phase 4: .txt, Phase 8: .pdf
            assertThat(result.getContentType()).isEqualTo("text/plain");  // Phase 4: text/plain
            assertThat(result.getSize()).isGreaterThan(0);
            assertThat(result.getChecksum()).isNotEmpty();
            assertThat(result.getFormat()).isEqualTo(com.semi.simlogistics.scheduler.metrics.report.model.ReportFormat.PDF);
        }

        @Test
        @DisplayName("Should include all KPI metrics in PDF")
        void shouldIncludeAllKpiMetricsInPdf() {
            // Given: Summary report with all fields
            SummaryReport report = createTestReport();

            // When: Export to PDF
            var result = exporter.exportSummaryReport(report);

            // Then: All KPI metrics should be included
            String content = new String(result.getContent(), StandardCharsets.UTF_8);

            assertThat(content).contains("sim-001");
            assertThat(content).contains("100");        // totalTasksCompleted
            assertThat(content).contains("6000");       // tasksPerHour (without .0)
            assertThat(content).contains("1000");       // materialThroughput (without .0)
            assertThat(content).contains("75.00%");    // vehicleUtilization (as percentage)
            assertThat(content).contains("60.00%");    // equipmentUtilization (as percentage)
            assertThat(content).contains("5000");       // totalEnergy (without .0)
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
