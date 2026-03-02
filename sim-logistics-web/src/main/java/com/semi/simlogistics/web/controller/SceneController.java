package com.semi.simlogistics.web.controller;

import com.semi.simlogistics.web.common.ApiEnvelope;
import com.semi.simlogistics.web.dto.*;
import com.semi.simlogistics.web.service.SceneService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for scene management.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@RestController
@RequestMapping("/api/v1/scenes")
public class SceneController {

    private static final Logger logger = LoggerFactory.getLogger(SceneController.class);

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    /**
     * GET /api/v1/scenes
     * Get paginated list of scenes with optional search.
     *
     * Contract alignment:
     * - page: 0-based (frontend sends 1-based, converts before calling)
     * - search: search keyword (unified param name)
     * - type: filter by scene type (not yet implemented, ignored)
     *
     * @param page the page number (default 0, 0-based)
     * @param pageSize the page size (default 20)
     * @param search the search keyword (optional, was 'keyword')
     * @return paged result of scene summaries
     */
    @GetMapping
    public ResponseEntity<ApiEnvelope<PagedResultDTO<SceneSummaryDTO>>> getScenes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search) {

        logger.debug("GET /api/v1/scenes: page={}, pageSize={}, search={}", page, pageSize, search);

        PagedResultDTO<SceneSummaryDTO> result = sceneService.getScenes(page, pageSize, search);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * GET /api/v1/scenes/{id}
     * Get scene detail by ID.
     *
     * @param id the scene ID
     * @return scene detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiEnvelope<SceneDetailDTO>> getSceneById(@PathVariable("id") String id) {
        logger.debug("GET /api/v1/scenes/{}", id);

        SceneDetailDTO result = sceneService.getSceneById(id);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * POST /api/v1/scenes
     * Create a new scene.
     *
     * @param request the create request
     * @return created scene detail
     */
    @PostMapping
    public ResponseEntity<ApiEnvelope<SceneDetailDTO>> createScene(
            @Valid @RequestBody CreateSceneRequestDTO request) {

        logger.debug("POST /api/v1/scenes: name={}", request.getName());

        SceneDetailDTO result = sceneService.createScene(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(result));
    }

    /**
     * PUT /api/v1/scenes/{id}
     * Update an existing scene.
     *
     * @param id the scene ID
     * @param request the update request
     * @return updated scene detail
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiEnvelope<SceneDetailDTO>> updateScene(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateSceneRequestDTO request) {

        logger.debug("PUT /api/v1/scenes/{}", id);

        SceneDetailDTO result = sceneService.updateScene(id, request);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * DELETE /api/v1/scenes/{id}
     * Delete a scene by ID.
     *
     * @param id the scene ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScene(@PathVariable("id") String id) {
        logger.debug("DELETE /api/v1/scenes/{}", id);

        sceneService.deleteScene(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/scenes/{id}/copy
     * Copy a scene.
     *
     * @param id the scene ID to copy
     * @return copy result
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiEnvelope<SceneCopyResultDTO>> copyScene(@PathVariable("id") String id) {
        logger.debug("POST /api/v1/scenes/{}/copy", id);

        SceneCopyResultDTO result = sceneService.copyScene(id);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * POST /api/v1/scenes/import
     * Import a scene from file.
     *
     * @param file the uploaded file
     * @return import result
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<SceneImportResultDTO>> importScene(
            @RequestParam("file") MultipartFile file) {

        logger.debug("POST /api/v1/scenes/import: file={}", file.getOriginalFilename());

        SceneImportResultDTO result = sceneService.importScene(file);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * GET /api/v1/scenes/{id}/export
     * Export a scene to file.
     *
     * @param id the scene ID
     * @return scene file as attachment
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<SceneDetailDTO> exportScene(@PathVariable("id") String id) {
        logger.debug("GET /api/v1/scenes/{}/export", id);

        SceneDetailDTO result = sceneService.exportScene(id);

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment",
                result.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".json");

        return ResponseEntity.ok()
                .headers(headers)
                .body(result);
    }

    /**
     * GET /api/v1/scenes/{id}/draft
     * Get scene draft.
     * Returns 404 if draft does not exist.
     *
     * @param id the scene ID
     * @return draft payload or 404
     */
    @GetMapping("/{id}/draft")
    public ResponseEntity<ApiEnvelope<SceneDraftPayloadDTO>> getSceneDraft(@PathVariable("id") String id) {
        logger.debug("GET /api/v1/scenes/{}/draft", id);

        SceneDraftPayloadDTO result = sceneService.getSceneDraft(id);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * POST /api/v1/scenes/{id}/draft
     * Save scene draft.
     *
     * Contract: Accepts SceneDraftPayloadDTO with structure:
     * {
     *   "sceneId": string,
     *   "content": SceneDetailDTO,
     *   "savedAt": string (ISO-8601),
     *   "version": number
     * }
     *
     * @param id the scene ID (path variable)
     * @param payload the draft payload (matches frontend SceneDraftPayload)
     * @return save result
     */
    @PostMapping("/{id}/draft")
    public ResponseEntity<ApiEnvelope<SceneDraftSaveResultDTO>> saveSceneDraft(
            @PathVariable("id") String id,
            @Valid @RequestBody SceneDraftPayloadDTO payload) {

        logger.debug("POST /api/v1/scenes/{}/draft: payload.sceneId={}, payload.version={}",
                id, payload.getSceneId(), payload.getVersion());

        // Validate sceneId in path matches payload
        if (!id.equals(payload.getSceneId())) {
            logger.warn("Scene ID mismatch: path={}, payload.sceneId={}", id, payload.getSceneId());
        }

        SceneDraftSaveResultDTO result = sceneService.saveSceneDraft(payload);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * DELETE /api/v1/scenes/{id}/draft
     * Delete scene draft.
     *
     * @param id the scene ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}/draft")
    public ResponseEntity<Void> deleteSceneDraft(@PathVariable("id") String id) {
        logger.debug("DELETE /api/v1/scenes/{}/draft", id);

        sceneService.deleteSceneDraft(id);
        return ResponseEntity.noContent().build();
    }
}
