package com.semi.simlogistics.scheduler.metrics.report.exporter;

import com.semi.simlogistics.scheduler.metrics.report.model.ExportResult;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;

/**
 * Report exporter interface (REQ-KPI-007).
 * <p>
 * Defines the contract for exporting reports to different formats.
 * Implementations should handle empty data gracefully.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public interface ReportExporter {

    /**
     * Export a summary report to the specific format.
     *
     * @param report the summary report to export
     * @return the export result containing file metadata and content
     */
    ExportResult exportSummaryReport(SummaryReport report);
}
