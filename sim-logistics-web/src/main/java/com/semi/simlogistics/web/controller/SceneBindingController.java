package com.semi.simlogistics.web.controller;

import com.semi.simlogistics.web.common.ApiEnvelope;
import com.semi.simlogistics.web.dto.EntityModelBindingDTO;
import com.semi.simlogistics.web.dto.SetEntityModelBindingRequestDTO;
import com.semi.simlogistics.web.service.ModelService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for scene entity-model bindings.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@RestController
@RequestMapping("/api/v1/scenes")
public class SceneBindingController {

    private static final Logger logger = LoggerFactory.getLogger(SceneBindingController.class);

    private final ModelService modelService;

    public SceneBindingController(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * GET /api/v1/scenes/{sceneId}/bindings
     * Get entity-model bindings for a scene.
     *
     * @param sceneId the scene ID
     * @return list of bindings
     */
    @GetMapping("/{sceneId}/bindings")
    public ResponseEntity<ApiEnvelope<List<EntityModelBindingDTO>>> getEntityBindings(
            @PathVariable("sceneId") String sceneId) {
        logger.debug("GET /api/v1/scenes/{}/bindings", sceneId);
        List<EntityModelBindingDTO> result = modelService.getEntityBindings(sceneId);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * PUT /api/v1/scenes/{sceneId}/bindings/{entityId}
     * Set entity-model binding.
     *
     * @param sceneId the scene ID
     * @param entityId the entity ID
     * @param request the binding request
     * @return 204 No Content
     */
    @PutMapping("/{sceneId}/bindings/{entityId}")
    public ResponseEntity<Void> setEntityBinding(
            @PathVariable("sceneId") String sceneId,
            @PathVariable("entityId") String entityId,
            @Valid @RequestBody SetEntityModelBindingRequestDTO request) {
        logger.debug("PUT /api/v1/scenes/{}/bindings/{}", sceneId, entityId);
        modelService.setEntityBinding(sceneId, entityId, request);
        return ResponseEntity.noContent().build();
    }
}
