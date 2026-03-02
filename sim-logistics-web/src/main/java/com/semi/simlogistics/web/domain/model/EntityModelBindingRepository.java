package com.semi.simlogistics.web.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EntityModelBinding persistence operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public interface EntityModelBindingRepository {

    /**
     * Find binding by ID.
     *
     * @param bindingId the binding ID
     * @return optional binding
     */
    Optional<EntityModelBinding> findById(String bindingId);

    /**
     * Find all bindings for a scene.
     *
     * @param sceneId the scene ID
     * @return list of bindings
     */
    List<EntityModelBinding> findBySceneId(String sceneId);

    /**
     * Find binding for a specific entity in a scene.
     *
     * @param sceneId the scene ID
     * @param entityId the entity ID
     * @return optional binding
     */
    Optional<EntityModelBinding> findBySceneIdAndEntityId(String sceneId, String entityId);

    /**
     * Save binding.
     *
     * @param binding the binding to save
     * @return saved binding
     */
    EntityModelBinding save(EntityModelBinding binding);

    /**
     * Delete binding by ID.
     *
     * @param bindingId the binding ID
     */
    void deleteById(String bindingId);

    /**
     * Delete all bindings for a scene.
     *
     * @param sceneId the scene ID
     */
    void deleteBySceneId(String sceneId);

    /**
     * Delete binding for a specific entity in a scene.
     *
     * @param sceneId the scene ID
     * @param entityId the entity ID
     */
    void deleteBySceneIdAndEntityId(String sceneId, String entityId);
}
