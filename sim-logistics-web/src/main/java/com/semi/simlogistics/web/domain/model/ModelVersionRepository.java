package com.semi.simlogistics.web.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ModelVersion persistence operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public interface ModelVersionRepository {

    /**
     * Find model version by ID.
     *
     * @param versionId the version ID
     * @return optional model version
     */
    Optional<ModelVersion> findById(String versionId);

    /**
     * Find all versions for a model.
     *
     * @param modelId the model ID
     * @return list of versions
     */
    List<ModelVersion> findByModelId(String modelId);

    /**
     * Find default version for a model.
     *
     * @param modelId the model ID
     * @return optional default version
     */
    Optional<ModelVersion> findDefaultByModelId(String modelId);

    /**
     * Save model version.
     *
     * @param version the version to save
     * @return saved version
     */
    ModelVersion save(ModelVersion version);

    /**
     * Delete model version by ID.
     *
     * @param versionId the version ID
     */
    void deleteById(String versionId);

    /**
     * Set default version for a model.
     * Unsets default flag for all other versions.
     *
     * @param modelId the model ID
     * @param versionId the version ID to set as default
     */
    void setDefaultVersion(String modelId, String versionId);

    /**
     * Count versions for a model.
     *
     * @param modelId the model ID
     * @return count
     */
    long countByModelId(String modelId);

    /**
     * Count versions referencing a file.
     *
     * @param fileId the file ID
     * @return count
     */
    long countByFileId(String fileId);
}
