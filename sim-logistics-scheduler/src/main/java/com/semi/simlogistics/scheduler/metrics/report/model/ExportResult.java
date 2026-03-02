package com.semi.simlogistics.scheduler.metrics.report.model;

import java.util.Objects;

/**
 * Export result data class (REQ-KPI-007).
 * <p>
 * Represents the result of a report export operation.
 * Contains the file metadata and content for Phase 8 upload to MinIO.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ExportResult {

    private final String fileName;
    private final String contentType;
    private final byte[] content;
    private final long size;
    private final String checksum;
    private final ReportFormat format;

    /**
     * Create a new export result.
     *
     * @param fileName    the file name
     * @param contentType the MIME content type
     * @param content     the file content as byte array
     * @param format      the report format
     */
    public ExportResult(
            String fileName,
            String contentType,
            byte[] content,
            ReportFormat format
    ) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
        this.size = content != null ? content.length : 0;
        this.checksum = calculateChecksum(content);
        this.format = format;
    }

    /**
     * Calculate SHA-256 checksum for content integrity.
     *
     * @param content the content to checksum
     * @return hex-encoded SHA-256 checksum, or empty string if content is null
     */
    private String calculateChecksum(byte[] content) {
        if (content == null) {
            return "";
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return "";
        }
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public ReportFormat getFormat() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportResult that = (ExportResult) o;
        return size == that.size
                && Objects.equals(fileName, that.fileName)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(checksum, that.checksum)
                && format == that.format;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, contentType, size, checksum, format);
    }

    @Override
    public String toString() {
        return "ExportResult{" +
                "fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", checksum='" + checksum + '\'' +
                ", format=" + format +
                '}';
    }
}
