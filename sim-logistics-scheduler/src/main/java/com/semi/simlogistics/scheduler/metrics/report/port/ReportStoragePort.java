package com.semi.simlogistics.scheduler.metrics.report.port;

import com.semi.simlogistics.scheduler.metrics.report.model.ExportResult;

import java.util.Optional;

/**
 * Report storage port interface (Phase 8 - Reserved).
 * <p>
 * This port defines the contract for storing generated reports.
 * Implementations will handle MinIO upload in Phase 8.
 * <p>
 * Design for Phase 8:
 * <ul>
 *   <li>Store report file to MinIO object storage</li>
 *   <li>Return URL for accessing the stored file</li>
 *   <li>Support file metadata (size, checksum, content type)</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public interface ReportStoragePort {

    /**
     * Store a report export result.
     * <p>
     * In Phase 8, this will upload to MinIO and return the access URL.
     * For Phase 4, the default implementation may be no-op or local file storage.
     *
     * @param simulationId the simulation ID
     * @param exportResult  the export result containing file content
     * @return the storage result with access URL (empty if not implemented)
     */
    Optional<StorageResult> storeReport(String simulationId, ExportResult exportResult);

    /**
     * Retrieve a stored report by its ID.
     *
     * @param reportId the report ID
     * @return the export result if found
     */
    Optional<ExportResult> retrieveReport(String reportId);

    /**
     * Delete a stored report.
     *
     * @param reportId the report ID
     * @return true if deleted successfully
     */
    boolean deleteReport(String reportId);

    /**
     * Storage result data class.
     * <p>
     * Contains metadata about the stored report.
     */
    class StorageResult {

        private final String reportId;
        private final String accessUrl;
        private final String objectKey;
        private final String bucketName;

        public StorageResult(
                String reportId,
                String accessUrl,
                String objectKey,
                String bucketName
        ) {
            this.reportId = reportId;
            this.accessUrl = accessUrl;
            this.objectKey = objectKey;
            this.bucketName = bucketName;
        }

        public String getReportId() {
            return reportId;
        }

        public String getAccessUrl() {
            return accessUrl;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getBucketName() {
            return bucketName;
        }
    }
}
