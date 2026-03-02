package com.semi.simlogistics.web.domain.scene;

import com.semi.simlogistics.web.dto.SceneSummaryDTO;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Scene operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public interface SceneRepository {

    /**
     * Find scene by ID.
     *
     * @param sceneId the scene ID
     * @return the scene if found
     */
    Optional<Scene> findById(String sceneId);

    /**
     * Find scene by name.
     *
     * @param name the scene name
     * @return the scene if found
     */
    Optional<Scene> findByName(String name);

    /**
     * Find all scenes with pagination.
     *
     * @param page the page number (0-based)
     * @param pageSize the page size
     * @return list of scene summaries
     */
    List<SceneSummaryDTO> findAll(int page, int pageSize);

    /**
     * Count total scenes.
     *
     * @return total count
     */
    long count();

    /**
     * Search scenes by keyword.
     *
     * @param keyword the search keyword
     * @param page the page number (0-based)
     * @param pageSize the page size
     * @return list of scene summaries
     */
    List<SceneSummaryDTO> search(String keyword, int page, int pageSize);

    /**
     * Count scenes matching search keyword.
     *
     * @param keyword the search keyword
     * @return count of matching scenes
     */
    long countSearch(String keyword);

    /**
     * Save or update scene.
     *
     * @param scene the scene to save
     * @return the saved scene
     */
    Scene save(Scene scene);

    /**
     * Delete scene by ID.
     *
     * @param sceneId the scene ID
     */
    void deleteById(String sceneId);

    /**
     * Check if scene exists by name.
     *
     * @param name the scene name
     * @return true if exists
     */
    boolean existsByName(String name);

    /**
     * Check if scene exists by name (excluding specific ID).
     *
     * @param name the scene name
     * @param excludeSceneId the scene ID to exclude
     * @return true if exists
     */
    boolean existsByNameExcluding(String name, String excludeSceneId);
}
