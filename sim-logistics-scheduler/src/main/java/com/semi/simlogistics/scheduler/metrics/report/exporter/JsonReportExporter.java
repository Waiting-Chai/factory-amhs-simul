package com.semi.simlogistics.scheduler.metrics.report.exporter;

import com.semi.simlogistics.scheduler.metrics.report.model.ExportResult;
import com.semi.simlogistics.scheduler.metrics.report.model.ReportFormat;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * JSON report exporter (REQ-KPI-007).
 * <p>
 * Exports summary reports to JSON format.
 * Output is structured and readable for secondary processing.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class JsonReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Export summary report to JSON format.
     *
     * @param report the summary report to export
     * @return the export result containing JSON content
     */
    @Override
    public ExportResult exportSummaryReport(SummaryReport report) {
        if (report == null) {
            report = SummaryReport.empty("unknown");
        }

        String json = buildJson(report);
        byte[] content = json.getBytes(StandardCharsets.UTF_8);

        String fileName = generateFileName(report.getSimulationId(), "json");

        return new ExportResult(
                fileName,
                ReportFormat.JSON.getMimeType(),
                content,
                ReportFormat.JSON
        );
    }

    /**
     * Build JSON content from summary report.
     *
     * @param report the summary report
     * @return JSON string content
     */
    private String buildJson(SummaryReport report) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Required fields
        json.append("  \"simulationId\": \"").append(escapeJson(report.getSimulationId())).append("\",\n");
        json.append("  \"generatedAt\": \"").append(formatDateTime(report.getGeneratedAt())).append("\",\n");
        json.append("  \"totalTasksCompleted\": ").append(report.getTotalTasksCompleted()).append(",\n");
        json.append("  \"tasksPerHour\": ").append(formatDouble(report.getTasksPerHour())).append(",\n");
        json.append("  \"materialThroughput\": ").append(formatDouble(report.getMaterialThroughput())).append(",\n");
        json.append("  \"vehicleUtilization\": ").append(formatDouble(report.getVehicleUtilization())).append(",\n");
        json.append("  \"equipmentUtilization\": ").append(formatDouble(report.getEquipmentUtilization())).append(",\n");
        json.append("  \"wipTotal\": ").append(report.getWipTotal()).append(",\n");
        json.append("  \"totalEnergy\": ").append(formatDouble(report.getTotalEnergy())).append(",\n");
        json.append("  \"averageCompletionTime\": ").append(formatDouble(report.getAverageCompletionTime())).append(",\n");
        json.append("  \"bottleneckSummary\": \"").append(escapeJson(report.getBottleneckSummary())).append("\"\n");

        json.append("}");

        return json.toString();
    }

    /**
     * Format double value for JSON output.
     *
     * @param value the double value
     * @return formatted string
     */
    private String formatDouble(double value) {
        if (value == (long) value) {
            return String.format("%.0f", value);
        } else {
            return String.format("%.2f", value);
        }
    }

    /**
     * Format date time to ISO-8601 string.
     *
     * @param dateTime the date time
     * @return formatted string
     */
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Escape string for JSON output.
     *
     * @param value the value to escape
     * @return escaped value
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Generate file name for export.
     *
     * @param simulationId the simulation ID
     * @param extension   the file extension
     * @return generated file name
     */
    private String generateFileName(String simulationId, String extension) {
        String timestamp = java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        return String.format("kpi_report_%s_%s.%s",
                sanitizeForFileName(simulationId), timestamp, extension);
    }

    /**
     * Sanitize simulation ID for use in file name.
     *
     * @param simulationId the simulation ID
     * @return sanitized ID
     */
    private String sanitizeForFileName(String simulationId) {
        if (simulationId == null) {
            return "unknown";
        }
        return simulationId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
