package com.semi.simlogistics.web.infrastructure.persistence.model;

import java.util.Optional;

/**
 * Repository interface for File persistence operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public interface FileRepository {

    /**
     * Find file by ID.
     *
     * @param fileId the file ID
     * @return optional file
     */
    Optional<File> findById(String fileId);

    /**
     * Save file record.
     *
     * @param file the file to save
     * @return saved file
     */
    File save(File file);

    /**
     * Delete file by ID.
     *
     * @param fileId the file ID
     */
    void deleteById(String fileId);
}
