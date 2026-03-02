package com.semi.simlogistics.web.domain.model;

import com.semi.simlogistics.web.dto.ModelSummaryDTO;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Model persistence operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public interface ModelRepository {

    /**
     * Find model by ID.
     *
     * @param modelId the model ID
     * @return optional model
     */
    Optional<Model> findById(String modelId);

    /**
     * Find model by name.
     *
     * @param name the model name
     * @return optional model
     */
    Optional<Model> findByName(String name);

    /**
     * Find models with optional filters and pagination.
     *
     * @param page the page number (0-based)
     * @param pageSize the page size
     * @param modelType the model type filter
     * @param status the model status filter
     * @param keyword the search keyword
     * @return list of model summaries
     */
    List<ModelSummaryDTO> findByFilters(int page, int pageSize, String modelType, String status, String keyword);

    /**
     * Count models with optional filters.
     *
     * @param modelType the model type filter
     * @param status the model status filter
     * @param keyword the search keyword
     * @return total count
     */
    long countByFilters(String modelType, String status, String keyword);

    /**
     * Save model (insert or update).
     *
     * @param model the model to save
     * @return saved model
     */
    Model save(Model model);

    /**
     * Delete model by ID.
     *
     * @param modelId the model ID
     */
    void deleteById(String modelId);

    /**
     * Clear default version reference for a model.
     *
     * @param modelId the model ID
     */
    void clearDefaultVersionId(String modelId);

    /**
     * Clear default version references by version ID.
     *
     * @param versionId the version ID
     */
    void clearDefaultVersionReferences(String versionId);

    /**
     * Check if model name exists.
     *
     * @param name the model name
     * @return true if exists
     */
    boolean existsByName(String name);

    /**
     * Check if model name exists excluding specific model.
     *
     * @param name the model name
     * @param excludeModelId the model ID to exclude
     * @return true if exists
     */
    boolean existsByNameExcluding(String name, String excludeModelId);
}
