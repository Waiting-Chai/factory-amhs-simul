package com.semi.simlogistics.web.domain.scene;

import java.util.Optional;

/**
 * Repository interface for Scene draft operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public interface SceneDraftRepository {

    /**
     * Find draft by scene ID.
     *
     * @param sceneId the scene ID
     * @return the draft if found
     */
    Optional<SceneDraft> findBySceneId(String sceneId);

    /**
     * Save or update draft.
     *
     * @param draft the draft to save
     * @return the saved draft
     */
    SceneDraft save(SceneDraft draft);

    /**
     * Delete draft by scene ID.
     *
     * @param sceneId the scene ID
     */
    void deleteBySceneId(String sceneId);

    /**
     * Check if draft exists for scene.
     *
     * @param sceneId the scene ID
     * @return true if draft exists
     */
    boolean existsBySceneId(String sceneId);
}
