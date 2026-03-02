package com.semi.simlogistics.scheduler.metrics.report.exporter;

import com.semi.simlogistics.scheduler.metrics.report.model.ExportResult;
import com.semi.simlogistics.scheduler.metrics.report.model.ReportFormat;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * CSV report exporter (REQ-KPI-007).
 * <p>
 * Exports summary reports to CSV format with:
 * <ul>
 *   <li>Fixed table headers</li>
 *   <li>Stable column order</li>
 *   <li>Deterministic number formatting</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export summary report to CSV format.
     *
     * @param report the summary report to export
     * @return the export result containing CSV content
     */
    @Override
    public ExportResult exportSummaryReport(SummaryReport report) {
        if (report == null) {
            report = SummaryReport.empty("unknown");
        }

        String csv = buildCsv(report);
        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        String fileName = generateFileName(report.getSimulationId(), "csv");

        return new ExportResult(
                fileName,
                ReportFormat.CSV.getMimeType(),
                content,
                ReportFormat.CSV
        );
    }

    /**
     * Build CSV content from summary report.
     *
     * @param report the summary report
     * @return CSV string content
     */
    private String buildCsv(SummaryReport report) {
        StringBuilder csv = new StringBuilder();

        // Header row (fixed column order)
        csv.append("simulation_id,");
        csv.append("generated_at,");
        csv.append("total_tasks_completed,");
        csv.append("tasks_per_hour,");
        csv.append("material_throughput,");
        csv.append("vehicle_utilization,");
        csv.append("equipment_utilization,");
        csv.append("wip_total,");
        csv.append("total_energy,");
        csv.append("avg_completion_time,");
        csv.append("bottleneck_summary");
        csv.append("\n");

        // Data row
        csv.append(escapeCsv(report.getSimulationId())).append(",");
        csv.append(escapeCsv(formatDateTime(report.getGeneratedAt()))).append(",");
        csv.append(report.getTotalTasksCompleted()).append(",");
        csv.append(formatDecimal(report.getTasksPerHour())).append(",");
        csv.append(formatDecimal(report.getMaterialThroughput())).append(",");
        csv.append(formatRatio(report.getVehicleUtilization())).append(",");
        csv.append(formatRatio(report.getEquipmentUtilization())).append(",");
        csv.append(report.getWipTotal()).append(",");
        csv.append(formatDecimal(report.getTotalEnergy())).append(",");
        csv.append(formatDecimal(report.getAverageCompletionTime())).append(",");
        csv.append(escapeCsv(report.getBottleneckSummary()));

        return csv.toString();
    }

    /**
     * Format decimal value deterministically.
     *
     * @param value the decimal value
     * @return formatted string
     */
    private String formatDecimal(double value) {
        if (value == (long) value) {
            return String.format("%.0f", value);
        } else {
            return String.format("%.2f", value);
        }
    }

    /**
     * Format ratio (utilization) as percentage with 2 decimal places.
     *
     * @param value the ratio value (0.0 to 1.0)
     * @return formatted percentage string
     */
    private String formatRatio(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Format date time to string.
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
     * Escape CSV value if necessary.
     *
     * @param value the value to escape
     * @return escaped value
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
