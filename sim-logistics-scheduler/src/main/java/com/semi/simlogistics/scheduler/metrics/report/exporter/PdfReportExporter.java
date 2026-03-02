package com.semi.simlogistics.scheduler.metrics.report.exporter;

import com.semi.simlogistics.scheduler.metrics.report.model.ExportResult;
import com.semi.simlogistics.scheduler.metrics.report.model.ReportFormat;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * PDF report exporter (REQ-KPI-007).
 * <p>
 * Exports summary reports to PDF format.
 * <p>
 * Note: Phase 4 uses text-based placeholder format.
 * Content type is text/plain for Phase 4 (not a real PDF).
 * Full PDF generation will be implemented in Phase 8
 * using a PDF library (iText or Apache PDFBox).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class PdfReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export summary report to PDF format.
     * <p>
     * Phase 4: Returns text-based placeholder with content type text/plain.
     * Phase 8: Will return actual PDF with content type application/pdf.
     *
     * @param report the summary report to export
     * @return the export result containing text-based PDF placeholder
     */
    @Override
    public ExportResult exportSummaryReport(SummaryReport report) {
        if (report == null) {
            report = SummaryReport.empty("unknown");
        }

        // For Phase 4, use text-based format with text/plain content type
        // In Phase 8, this will be replaced with proper PDF generation
        String content = buildTextPdf(report);
        byte[] pdfContent = content.getBytes(StandardCharsets.UTF_8);

        String fileName = generateFileName(report.getSimulationId(), "txt");

        return new ExportResult(
                fileName,
                "text/plain",  // Phase 4: text/plain, Phase 8: application/pdf
                pdfContent,
                ReportFormat.PDF  // Format enum remains PDF for API consistency
        );
    }

    /**
     * Build text-based PDF content from summary report.
     * <p>
     * Phase 4: Simple text format
     * Phase 8: Will use PDF library for proper formatting
     *
     * @param report the summary report
     * @return PDF string content (text-based for Phase 4)
     */
    private String buildTextPdf(SummaryReport report) {
        StringBuilder pdf = new StringBuilder();

        // Header
        pdf.append("================================================================================\n");
        pdf.append("                           KPI SUMMARY REPORT\n");
        pdf.append("================================================================================\n\n");

        // Simulation Info
        pdf.append("SIMULATION INFORMATION\n");
        pdf.append("----------------------------------------\n");
        pdf.append("Simulation ID:        ").append(report.getSimulationId()).append("\n");
        pdf.append("Generated At:         ").append(formatDateTime(report.getGeneratedAt())).append("\n\n");

        // Throughput Section
        pdf.append("THROUGHPUT METRICS\n");
        pdf.append("----------------------------------------\n");
        pdf.append("Total Tasks:          ").append(report.getTotalTasksCompleted()).append("\n");
        pdf.append("Tasks Per Hour:       ").append(formatDouble(report.getTasksPerHour())).append("\n");
        pdf.append("Material Throughput:  ").append(formatDouble(report.getMaterialThroughput())).append("\n\n");

        // Utilization Section
        pdf.append("RESOURCE UTILIZATION\n");
        pdf.append("----------------------------------------\n");
        pdf.append("Vehicle Utilization:  ").append(formatPercent(report.getVehicleUtilization())).append("\n");
        pdf.append("Equipment Utilization: ").append(formatPercent(report.getEquipmentUtilization())).append("\n\n");

        // Energy Section
        pdf.append("ENERGY CONSUMPTION\n");
        pdf.append("----------------------------------------\n");
        pdf.append("Total Energy:         ").append(formatDouble(report.getTotalEnergy())).append(" watt-seconds\n\n");

        // Additional Metrics
        pdf.append("ADDITIONAL METRICS\n");
        pdf.append("----------------------------------------\n");
        pdf.append("WIP Total:            ").append(report.getWipTotal()).append("\n");
        pdf.append("Avg Completion Time:  ").append(formatDouble(report.getAverageCompletionTime())).append(" seconds\n\n");

        // Bottleneck Identification
        pdf.append("BOTTLENECK IDENTIFICATION\n");
        pdf.append("----------------------------------------\n");
        String bottleneckSummary = report.getBottleneckSummary();
        if (bottleneckSummary != null && !bottleneckSummary.isEmpty()) {
            pdf.append(bottleneckSummary).append("\n\n");
        } else {
            pdf.append("No significant bottlenecks detected.\n\n");
        }

        // Footer
        pdf.append("================================================================================\n");
        pdf.append("Generated by sim-logistics-scheduler | Phase 4 (Text-based PDF)\n");
        pdf.append("Note: Full PDF formatting will be implemented in Phase 8\n");
        pdf.append("================================================================================\n");

        return pdf.toString();
    }

    /**
     * Format double value for output.
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
     * Format ratio as percentage.
     *
     * @param value the ratio value (0.0 to 1.0)
     * @return formatted percentage string
     */
    private String formatPercent(double value) {
        return String.format("%.2f%%", value * 100);
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
