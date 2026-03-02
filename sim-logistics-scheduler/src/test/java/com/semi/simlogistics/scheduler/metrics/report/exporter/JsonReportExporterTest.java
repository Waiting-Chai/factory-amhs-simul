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
 * TDD tests for JsonReportExporter (REQ-KPI-007).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("JsonReportExporter Tests (REQ-KPI-007)")
class JsonReportExporterTest {

    private JsonReportExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new JsonReportExporter();
    }

    @Nested
    @DisplayName("JSON Export Tests")
    class JsonExportTests {

        @Test
        @DisplayName("Should export JSON with expected schema")
        void shouldExportJsonWithExpectedSchema() {
            // Given: Summary report with data
            SummaryReport report = createTestReport();

            // When: Export to JSON
            var result = exporter.exportSummaryReport(report);

            // Then: Should have valid JSON structure
            String json = new String(result.getContent(), StandardCharsets.UTF_8);

            assertThat(json).contains("{");
            assertThat(json).contains("}");
            assertThat(json).contains("\"simulationId\"");
            assertThat(json).contains("\"totalTasksCompleted\"");
            assertThat(json).contains("\"tasksPerHour\"");
            assertThat(json).contains("\"materialThroughput\"");
            assertThat(json).contains("\"vehicleUtilization\"");
            assertThat(json).contains("\"equipmentUtilization\"");
            assertThat(json).contains("\"totalEnergy\"");
            assertThat(json).contains("\"averageCompletionTime\"");
            assertThat(json).contains("\"generatedAt\"");
        }

        @Test
        @DisplayName("Should contain required fields in JSON")
        void shouldContainRequiredFieldsInJson() {
            // Given: Summary report
            SummaryReport report = createTestReport();

            // When: Export to JSON
            var result = exporter.exportSummaryReport(report);

            // Then: All required fields should be present
            String json = new String(result.getContent(), StandardCharsets.UTF_8);

            // Check that key-value pairs are present (may have whitespace/line breaks)
            assertThat(json).contains("\"simulationId\"");
            assertThat(json).contains("sim-001");
            assertThat(json).contains("\"totalTasksCompleted\"");
            assertThat(json).contains("100");
            assertThat(json).contains("\"tasksPerHour\"");
            assertThat(json).contains("6000");
            assertThat(json).contains("\"materialThroughput\"");
            assertThat(json).contains("1000");
            assertThat(json).contains("\"vehicleUtilization\"");
            assertThat(json).contains("0.75");
            assertThat(json).contains("\"equipmentUtilization\"");
            assertThat(json).contains("0.6");
            assertThat(json).contains("\"wipTotal\"");
            assertThat(json).contains("10");
            assertThat(json).contains("\"totalEnergy\"");
            assertThat(json).contains("5000");
            assertThat(json).contains("\"averageCompletionTime\"");
            assertThat(json).contains("45");
        }

        @Test
        @DisplayName("Should export bottleneck identification field in JSON")
        void shouldExportBottleneckIdentificationFieldInJson() {
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

            // When: Export to JSON
            var result = exporter.exportSummaryReport(report);

            // Then: Should export bottleneck summary
            String json = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(json).contains("\"bottleneckSummary\"");
            assertThat(json).contains("WARNING: 1 bottleneck");
        }

        @Test
        @DisplayName("Should export JSON for empty snapshot")
        void shouldExportJsonForEmptySnapshot() {
            // Given: Empty summary report
            SummaryReport report = SummaryReport.empty("sim-001");

            // When: Export to JSON
            var result = exporter.exportSummaryReport(report);

            // Then: Should export with zero/default values
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContentType()).isEqualTo("application/json");
            assertThat(result.getFileName()).endsWith(".json");

            String json = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(json).contains("\"simulationId\"");
            assertThat(json).contains("sim-001");
            assertThat(json).contains("\"totalTasksCompleted\"");
            assertThat(json).contains("0");
            assertThat(json).contains("\"tasksPerHour\"");
        }

        @Test
        @DisplayName("Should produce readable JSON structure")
        void shouldProduceReadableJsonStructure() {
            // Given: Summary report
            SummaryReport report = createTestReport();

            // When: Export to JSON
            var result = exporter.exportSummaryReport(report);

            // Then: Should produce readable JSON for secondary processing
            assertThat(result.getContentType()).isEqualTo("application/json");
            assertThat(result.getFileName()).endsWith(".json");
            assertThat(result.getSize()).isGreaterThan(0);
            assertThat(result.getChecksum()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle special characters in simulation ID")
        void shouldHandleSpecialCharactersInSimulationId() {
            // Given: Report with special characters in ID
            SummaryReport report = new SummaryReport(
                    "sim-with-special-chars_2026",
                    LocalDateTime.now(),
                    50,
                    3000.0,
                    500.0,
                    0.5,
                    0.45,
                    5,
                    2500.0,
                    30.0,
                    ""
            );

            // When: Export to JSON
            var result = exporter.exportSummaryReport(report);

            // Then: Should handle special characters correctly
            String json = new String(result.getContent(), StandardCharsets.UTF_8);
            assertThat(json).contains("sim-with-special-chars_2026");
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
