package com.semi.simlogistics.scheduler.metrics.report.model;

/**
 * Report format enumeration (REQ-KPI-007).
 * <p>
 * Defines supported export formats for KPI reports.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public enum ReportFormat {

    /**
     * CSV format (Comma-Separated Values).
     * Suitable for spreadsheet applications.
     */
    CSV("text/csv", ".csv"),

    /**
     * JSON format (JavaScript Object Notation).
     * Suitable for programmatic processing.
     */
    JSON("application/json", ".json"),

    /**
     * PDF format (Portable Document Format).
     * Suitable for printing and archiving.
     */
    PDF("application/pdf", ".pdf");

    private final String mimeType;
    private final String fileExtension;

    ReportFormat(String mimeType, String fileExtension) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
