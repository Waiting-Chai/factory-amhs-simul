package com.semi.simlogistics.web.service;

import com.semi.simlogistics.web.dto.*;
import com.semi.simlogistics.web.domain.scene.Scene;
import com.semi.simlogistics.web.domain.scene.SceneDraft;
import com.semi.simlogistics.web.domain.scene.SceneDraftRepository;
import com.semi.simlogistics.web.domain.scene.SceneRepository;
import com.semi.simlogistics.web.exception.ResourceConflictException;
import com.semi.simlogistics.web.exception.ResourceNotFoundException;
import com.semi.simlogistics.web.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for scene management operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@Service
public class SceneService {

    private static final Logger logger = LoggerFactory.getLogger(SceneService.class);

    private final SceneRepository sceneRepository;
    private final SceneDraftRepository sceneDraftRepository;

    public SceneService(SceneRepository sceneRepository, SceneDraftRepository sceneDraftRepository) {
        this.sceneRepository = sceneRepository;
        this.sceneDraftRepository = sceneDraftRepository;
    }

    /**
     * Get paginated list of scenes.
     *
     * @param page the page number (0-based)
     * @param pageSize the page size
     * @param search the search keyword (optional, also referred as keyword in repository layer)
     * @return paged result of scene summaries
     */
    public PagedResultDTO<SceneSummaryDTO> getScenes(int page, int pageSize, String search) {
        logger.info("Getting scenes: page={}, pageSize={}, search={}", page, pageSize, search);

        List<SceneSummaryDTO> items;
        long total;

        if (search != null && !search.trim().isEmpty()) {
            items = sceneRepository.search(search, page, pageSize);
            total = sceneRepository.countSearch(search);
        } else {
            items = sceneRepository.findAll(page, pageSize);
            total = sceneRepository.count();
        }

        return new PagedResultDTO<>(items, total, page, pageSize);
    }

    /**
     * Get scene detail by ID.
     *
     * @param sceneId the scene ID
     * @return scene detail
     * @throws ResourceNotFoundException if scene not found
     */
    public SceneDetailDTO getSceneById(String sceneId) {
        logger.info("Getting scene by ID: {}", sceneId);

        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("Scene", sceneId));

        return toDetailDTO(scene);
    }

    /**
     * Create a new scene.
     *
     * @param request the create request
     * @return created scene detail
     * @throws ResourceConflictException if scene name already exists
     * @throws ValidationException if validation fails
     */
    @Transactional
    public SceneDetailDTO createScene(CreateSceneRequestDTO request) {
        logger.info("Creating scene: name={}", request.getName());

        validateSceneRequest(request);

        if (sceneRepository.existsByName(request.getName())) {
            throw new ResourceConflictException("Scene name already exists: " + request.getName());
        }

        Scene scene = new Scene();
        scene.setSceneId(generateSceneId());
        scene.setName(request.getName());
        scene.setDescription(request.getDescription());
        scene.setEntities(request.getEntities() != null ? request.getEntities() : new ArrayList<>());
        scene.setPaths(request.getPaths() != null ? request.getPaths() : new ArrayList<>());
        scene.setProcessFlows(request.getProcessFlows() != null ? request.getProcessFlows() : new ArrayList<>());
        scene.setVersion(1);
        scene.setCreatedAt(LocalDateTime.now());
        scene.setUpdatedAt(LocalDateTime.now());

        Scene savedScene = sceneRepository.save(scene);

        logger.info("Scene created successfully: sceneId={}", savedScene.getSceneId());

        return toDetailDTO(savedScene);
    }

    /**
     * Update an existing scene.
     *
     * @param sceneId the scene ID
     * @param request the update request
     * @return updated scene detail
     * @throws ResourceNotFoundException if scene not found
     * @throws ResourceConflictException if scene name already exists
     * @throws ValidationException if validation fails
     */
    @Transactional
    public SceneDetailDTO updateScene(String sceneId, UpdateSceneRequestDTO request) {
        logger.info("Updating scene: sceneId={}", sceneId);

        validateSceneRequest(request);

        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("Scene", sceneId));

        if (request.getName() != null && !request.getName().equals(scene.getName())) {
            if (sceneRepository.existsByNameExcluding(request.getName(), sceneId)) {
                throw new ResourceConflictException("Scene name already exists: " + request.getName());
            }
            scene.setName(request.getName());
        }

        if (request.getDescription() != null) {
            scene.setDescription(request.getDescription());
        }

        if (request.getEntities() != null) {
            scene.setEntities(request.getEntities());
        }

        if (request.getPaths() != null) {
            scene.setPaths(request.getPaths());
        }

        if (request.getProcessFlows() != null) {
            scene.setProcessFlows(request.getProcessFlows());
        }

        scene.setVersion(scene.getVersion() + 1);
        scene.setUpdatedAt(LocalDateTime.now());

        Scene savedScene = sceneRepository.save(scene);

        logger.info("Scene updated successfully: sceneId={}, version={}", savedScene.getSceneId(), savedScene.getVersion());

        return toDetailDTO(savedScene);
    }

    /**
     * Delete a scene by ID.
     *
     * @param sceneId the scene ID
     * @throws ResourceNotFoundException if scene not found
     */
    @Transactional
    public void deleteScene(String sceneId) {
        logger.info("Deleting scene: sceneId={}", sceneId);

        if (sceneRepository.findById(sceneId).isEmpty()) {
            throw new ResourceNotFoundException("Scene", sceneId);
        }

        sceneRepository.deleteById(sceneId);
        sceneDraftRepository.deleteBySceneId(sceneId);

        logger.info("Scene deleted successfully: sceneId={}", sceneId);
    }

    /**
     * Copy a scene.
     *
     * @param sceneId the scene ID to copy
     * @return copy result
     * @throws ResourceNotFoundException if scene not found
     */
    @Transactional
    public SceneCopyResultDTO copyScene(String sceneId) {
        logger.info("Copying scene: sceneId={}", sceneId);

        Scene sourceScene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("Scene", sceneId));

        String newName = sourceScene.getName() + " (Copy)";
        int counter = 1;
        while (sceneRepository.existsByName(newName)) {
            counter++;
            newName = sourceScene.getName() + " (Copy " + counter + ")";
        }

        Scene newScene = new Scene();
        newScene.setSceneId(generateSceneId());
        newScene.setName(newName);
        newScene.setDescription(sourceScene.getDescription());
        newScene.setEntities(new ArrayList<>(sourceScene.getEntities()));
        newScene.setPaths(new ArrayList<>(sourceScene.getPaths()));
        newScene.setProcessFlows(new ArrayList<>(sourceScene.getProcessFlows()));
        newScene.setVersion(1);
        newScene.setCreatedAt(LocalDateTime.now());
        newScene.setUpdatedAt(LocalDateTime.now());

        Scene savedScene = sceneRepository.save(newScene);

        logger.info("Scene copied successfully: originalSceneId={}, newSceneId={}", sceneId, savedScene.getSceneId());

        return new SceneCopyResultDTO(savedScene.getSceneId(), savedScene.getName(), savedScene.getVersion());
    }

    /**
     * Import a scene from file.
     *
     * @param file the uploaded file
     * @return import result
     * @throws ValidationException if validation fails
     */
    @Transactional
    public SceneImportResultDTO importScene(MultipartFile file) {
        logger.info("Importing scene from file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new ValidationException("file", "File is empty");
        }

        try {
            byte[] bytes = file.getBytes();
            String content = new String(bytes);

            // Parse JSON content
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            SceneDetailDTO sceneDetail = mapper.readValue(content, SceneDetailDTO.class);

            // Validate scene data
            if (sceneDetail.getName() == null || sceneDetail.getName().trim().isEmpty()) {
                throw new ValidationException("name", "Scene name is required");
            }

            // Check for name conflicts
            String finalName = sceneDetail.getName();
            int counter = 1;
            while (sceneRepository.existsByName(finalName)) {
                counter++;
                finalName = sceneDetail.getName() + " (" + counter + ")";
            }

            // Create scene
            Scene scene = new Scene();
            scene.setSceneId(generateSceneId());
            scene.setName(finalName);
            scene.setDescription(sceneDetail.getDescription());
            scene.setEntities(sceneDetail.getEntities() != null ? sceneDetail.getEntities() : new ArrayList<>());
            scene.setPaths(sceneDetail.getPaths() != null ? sceneDetail.getPaths() : new ArrayList<>());
            scene.setProcessFlows(sceneDetail.getProcessFlows() != null ? sceneDetail.getProcessFlows() : new ArrayList<>());
            scene.setVersion(1);
            scene.setCreatedAt(LocalDateTime.now());
            scene.setUpdatedAt(LocalDateTime.now());

            Scene savedScene = sceneRepository.save(scene);

            SceneImportResultDTO result = new SceneImportResultDTO(
                    savedScene.getSceneId(),
                    savedScene.getName(),
                    savedScene.getVersion()
            );

            if (counter > 1) {
                result.setWarnings(List.of("Scene name was changed due to conflict: " + finalName));
            }

            logger.info("Scene imported successfully: sceneId={}", savedScene.getSceneId());

            return result;

        } catch (IOException e) {
            logger.error("Failed to import scene: {}", e.getMessage(), e);
            throw new ValidationException("Failed to parse import file: " + e.getMessage());
        }
    }

    /**
     * Export a scene to file.
     *
     * @param sceneId the scene ID
     * @return scene detail for export
     * @throws ResourceNotFoundException if scene not found
     */
    public SceneDetailDTO exportScene(String sceneId) {
        logger.info("Exporting scene: sceneId={}", sceneId);

        return getSceneById(sceneId);
    }

    /**
     * Get scene draft.
     *
     * @param sceneId the scene ID
     * @return draft payload or null if not exists
     */
    public SceneDraftPayloadDTO getSceneDraft(String sceneId) {
        logger.info("Getting scene draft: sceneId={}", sceneId);

        Optional<SceneDraft> draft = sceneDraftRepository.findBySceneId(sceneId);

        if (draft.isEmpty()) {
            return null;
        }

        SceneDraftPayloadDTO payload = new SceneDraftPayloadDTO();
        payload.setSceneId(draft.get().getSceneId());
        payload.setContent(toDetailDTO(draft.get().getContent()));
        payload.setSavedAt(draft.get().getSavedAt());
        payload.setVersion(draft.get().getVersion());

        return payload;
    }

    /**
     * Save scene draft.
     *
     * Contract: Accepts SceneDraftPayloadDTO matching frontend structure.
     * Payload contains: sceneId, content (SceneDetailDTO), savedAt, version
     *
     * @param payload the draft payload
     * @return save result
     * @throws ResourceNotFoundException if scene not found
     */
    @Transactional
    public SceneDraftSaveResultDTO saveSceneDraft(SceneDraftPayloadDTO payload) {
        String sceneId = payload.getSceneId();
        logger.info("Saving scene draft: sceneId={}, version={}", sceneId, payload.getVersion());

        // Verify scene exists
        Scene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("Scene", sceneId));

        SceneDraft draft = new SceneDraft();
        draft.setSceneId(sceneId);
        draft.setContent(payload.getContent());
        draft.setVersion(payload.getVersion());

        // Use savedAt from payload if provided, otherwise repository will set current time
        if (payload.getSavedAt() != null) {
            draft.setSavedAt(payload.getSavedAt());
        }

        // Save and use returned value (contains actual persisted savedAt)
        SceneDraft savedDraft = sceneDraftRepository.save(draft);

        logger.info("Scene draft saved successfully: sceneId={}", sceneId);

        // Return savedAt from the persisted draft (repository return value)
        return new SceneDraftSaveResultDTO(true, savedDraft.getSavedAt());
    }

    /**
     * Delete scene draft.
     *
     * @param sceneId the scene ID
     */
    @Transactional
    public void deleteSceneDraft(String sceneId) {
        logger.info("Deleting scene draft: sceneId={}", sceneId);

        sceneDraftRepository.deleteBySceneId(sceneId);

        logger.info("Scene draft deleted successfully: sceneId={}", sceneId);
    }

    private void validateSceneRequest(Object request) {
        if (request instanceof CreateSceneRequestDTO req) {
            if (req.getName() == null || req.getName().trim().isEmpty()) {
                throw new ValidationException("name", "Scene name is required");
            }
        } else if (request instanceof UpdateSceneRequestDTO req) {
            if (req.getName() != null && req.getName().trim().isEmpty()) {
                throw new ValidationException("name", "Scene name cannot be empty");
            }
        }
    }

    private String generateSceneId() {
        return "SCENE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private SceneDetailDTO toDetailDTO(Scene scene) {
        SceneDetailDTO dto = new SceneDetailDTO();
        dto.setSceneId(scene.getSceneId());
        dto.setName(scene.getName());
        dto.setDescription(scene.getDescription());
        dto.setVersion(scene.getVersion());
        dto.setCreatedAt(scene.getCreatedAt());
        dto.setUpdatedAt(scene.getUpdatedAt());
        dto.setEntities(scene.getEntities());
        dto.setPaths(scene.getPaths());
        dto.setProcessFlows(scene.getProcessFlows());
        return dto;
    }

    private SceneDetailDTO toDetailDTO(SceneDetailDTO dto) {
        return dto;
    }
}
