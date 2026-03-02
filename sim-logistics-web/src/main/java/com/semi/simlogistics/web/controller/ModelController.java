package com.semi.simlogistics.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.semi.simlogistics.web.common.ApiEnvelope;
import com.semi.simlogistics.web.dto.*;
import com.semi.simlogistics.web.exception.ValidationException;
import com.semi.simlogistics.web.service.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * REST controller for model management.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private static final Logger logger = LoggerFactory.getLogger(ModelController.class);
    private static final String METADATA_TRANSFORM_FORMAT_ERROR = "metadata/transform格式错误";

    private final ModelService modelService;
    private final ObjectMapper objectMapper;

    public ModelController(ModelService modelService, ObjectMapper objectMapper) {
        this.modelService = modelService;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /api/v1/models
     * Get paginated list of models with optional search.
     *
     * @param page the page number (default 0, 0-based)
     * @param pageSize the page size (default 20)
     * @param type the model type filter (optional)
     * @param status the model status filter (optional)
     * @param search the search keyword (optional)
     * @return paged result of model summaries
     */
    @GetMapping
    public ResponseEntity<ApiEnvelope<PagedResultDTO<ModelSummaryDTO>>> getModels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        logger.debug("GET /api/v1/models: page={}, pageSize={}, type={}, status={}, search={}",
                page, pageSize, type, status, search);

        PagedResultDTO<ModelSummaryDTO> result = modelService.getModels(page, pageSize, type, status, search);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * GET /api/v1/models/{id}
     * Get model detail by ID.
     *
     * @param id the model ID
     * @return model detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiEnvelope<ModelDetailDTO>> getModelById(@PathVariable("id") String id) {
        logger.debug("GET /api/v1/models/{}", id);

        ModelDetailDTO result = modelService.getModelById(id);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * POST /api/v1/models/upload
     * Upload a new model file.
     *
     * @param file the uploaded GLB file
     * @param name the model name
     * @param modelType the model type
     * @param description the description
     * @return upload result
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<ModelUploadResultDTO>> uploadModel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "modelType", required = false) String modelType,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "metadata", required = false) String metadata) {

        logger.debug("POST /api/v1/models/upload: name={}, modelType={}, type={}, file={}",
                name, modelType, type, file.getOriginalFilename());

        JsonNode metadataNode = parseMetadata(metadata);
        String resolvedType = firstNonBlank(modelType, type, readText(metadataNode, "type"));
        String resolvedVersion = firstNonBlank(version, readText(metadataNode, "version"), "1.0.0");
        ModelMetadataDTO metadataDTO = readMetadata(metadataNode);
        TransformConfigDTO transformConfig = readTransform(metadataNode);

        if (resolvedType == null || resolvedType.isBlank()) {
            throw new ValidationException("type", "Model type is required");
        }

        ModelUploadResultDTO result = modelService.uploadModel(
                file,
                name,
                resolvedType,
                description,
                resolvedVersion,
                metadataDTO,
                transformConfig
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(result));
    }

    /**
     * PUT /api/v1/models/{id}
     * Update model metadata.
     *
     * @param id the model ID
     * @param name the new name
     * @param description the new description
     * @param modelType the new model type
     * @param transformConfig the transform config
     * @return updated model detail
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiEnvelope<ModelDetailDTO>> updateModel(
            @PathVariable("id") String id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "modelType", required = false) String modelType,
            @RequestBody(required = false) Map<String, Object> requestBody) {

        logger.debug("PUT /api/v1/models/{}", id);

        String resolvedName = firstNonBlank(name, readString(requestBody, "name"));
        String resolvedDescription = firstNonBlank(description, readString(requestBody, "description"));
        String resolvedModelType = firstNonBlank(
                modelType,
                readString(requestBody, "modelType"),
                readString(requestBody, "type")
        );

        TransformConfigDTO transformConfig = null;
        ModelMetadataDTO metadata = null;
        if (requestBody != null) {
            try {
                transformConfig = objectMapper.convertValue(requestBody.get("transformConfig"), TransformConfigDTO.class);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
            }
            JsonNode metadataNode = objectMapper.valueToTree(requestBody.get("metadata"));
            metadata = readMetadata(metadataNode);
            if (transformConfig == null) {
                transformConfig = readTransform(metadataNode);
            }
        }

        ModelDetailDTO result = modelService.updateModel(
                id,
                resolvedName,
                resolvedDescription,
                resolvedModelType,
                metadata,
                transformConfig
        );
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * POST /api/v1/models/{id}/versions
     * Upload a new version for existing model.
     *
     * @param id the model ID
     * @param file the uploaded GLB file
     * @param version the version string
     * @param transformConfig the transform config
     * @return model version detail
     */
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<ModelVersionDTO>> uploadModelVersion(
            @PathVariable("id") String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "metadata", required = false) String metadata) {

        logger.debug("POST /api/v1/models/{}/versions: version={}", id, version);

        JsonNode metadataNode = parseMetadata(metadata);
        String resolvedVersion = firstNonBlank(version, readText(metadataNode, "version"));
        if (resolvedVersion == null || resolvedVersion.isBlank()) {
            throw new ValidationException("version", "Version is required");
        }
        TransformConfigDTO transformConfig = readTransform(metadataNode);

        ModelVersionDTO result = modelService.uploadModelVersion(id, file, resolvedVersion, transformConfig);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(result));
    }

    /**
     * PUT /api/v1/models/{id}/versions/{versionId}
     * Set model version as default.
     *
     * @param id the model ID
     * @param versionId the version ID
     * @return 204 No Content or 200 OK
     */
    @PutMapping("/{id}/versions/{versionId}")
    public ResponseEntity<ApiEnvelope<ModelVersionDTO>> setDefaultVersion(
            @PathVariable("id") String id,
            @PathVariable("versionId") String versionId) {

        logger.debug("PUT /api/v1/models/{}/versions/{}", id, versionId);

        ModelVersionDTO result = modelService.setDefaultVersion(id, versionId);
        return ResponseEntity.ok(ApiEnvelope.success(result));
    }

    /**
     * PATCH /api/v1/models/{id}/enable
     * Enable model.
     *
     * @param id the model ID
     * @return 204 No Content or 200 OK
     */
    @PatchMapping("/{id}/enable")
    public ResponseEntity<Void> enableModel(@PathVariable("id") String id) {
        logger.debug("PATCH /api/v1/models/{}/enable", id);

        modelService.enableModel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/models/{id}/disable
     * Disable model.
     *
     * @param id the model ID
     * @return 204 No Content or 200 OK
     */
    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disableModel(@PathVariable("id") String id) {
        logger.debug("PATCH /api/v1/models/{}/disable", id);

        modelService.disableModel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/models/{id}
     * Delete model by ID.
     *
     * @param id the model ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModel(@PathVariable("id") String id) {
        logger.debug("DELETE /api/v1/models/{}", id);

        modelService.deleteModel(id);
        return ResponseEntity.noContent().build();
    }

    private JsonNode parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(metadata);
        } catch (IOException e) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
    }

    private String readText(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private TransformConfigDTO readTransform(JsonNode metadataNode) {
        if (metadataNode == null || metadataNode.isNull()) {
            return null;
        }
        JsonNode transformNode = metadataNode.get("transform");
        if (transformNode == null || transformNode.isNull()) {
            return null;
        }
        JsonNode normalizedTransformNode = normalizeTransformNode(transformNode);
        try {
            return objectMapper.treeToValue(normalizedTransformNode, TransformConfigDTO.class);
        } catch (MismatchedInputException | IllegalArgumentException e) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        } catch (IOException e) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
    }

    private ModelMetadataDTO readMetadata(JsonNode metadataNode) {
        if (metadataNode == null || metadataNode.isNull()) {
            return null;
        }
        ObjectNode metadataForModel = objectMapper.createObjectNode();
        JsonNode sizeNode = metadataNode.get("size");
        if (sizeNode != null && !sizeNode.isNull()) {
            metadataForModel.set("size", sizeNode);
        }
        JsonNode anchorNode = metadataNode.get("anchor");
        if (anchorNode != null && !anchorNode.isNull()) {
            metadataForModel.set("anchor", anchorNode);
        }
        if (metadataForModel.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(metadataForModel, ModelMetadataDTO.class);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        } catch (IOException ex) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
    }

    private JsonNode normalizeTransformNode(JsonNode transformNode) {
        if (!transformNode.isObject()) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
        ObjectNode normalizedTransform = ((ObjectNode) transformNode).deepCopy();
        normalizeScaleField(normalizedTransform);
        validateVectorField(normalizedTransform, "scale");
        validateVectorField(normalizedTransform, "rotation");
        validateVectorField(normalizedTransform, "pivot");
        return normalizedTransform;
    }

    private void normalizeScaleField(ObjectNode transformNode) {
        JsonNode scaleNode = transformNode.get("scale");
        if (scaleNode == null || scaleNode.isNull() || scaleNode.isObject()) {
            return;
        }
        if (!scaleNode.isArray() || scaleNode.size() != 3) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
        JsonNode x = scaleNode.get(0);
        JsonNode y = scaleNode.get(1);
        JsonNode z = scaleNode.get(2);
        if (!x.isNumber() || !y.isNumber() || !z.isNumber()) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
        ObjectNode scaleObject = objectMapper.createObjectNode();
        scaleObject.put("x", x.asDouble());
        scaleObject.put("y", y.asDouble());
        scaleObject.put("z", z.asDouble());
        transformNode.set("scale", scaleObject);
    }

    private void validateVectorField(ObjectNode transformNode, String fieldName) {
        JsonNode vectorNode = transformNode.get(fieldName);
        if (vectorNode == null || vectorNode.isNull()) {
            return;
        }
        if (!vectorNode.isObject()) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
        validateAxisNumber(vectorNode, fieldName, "x");
        validateAxisNumber(vectorNode, fieldName, "y");
        validateAxisNumber(vectorNode, fieldName, "z");
    }

    private void validateAxisNumber(JsonNode vectorNode, String fieldName, String axis) {
        JsonNode axisNode = vectorNode.get(axis);
        if (axisNode == null || !axisNode.isNumber()) {
            throw new IllegalArgumentException(METADATA_TRANSFORM_FORMAT_ERROR);
        }
    }

    private String readString(Map<String, Object> body, String field) {
        if (body == null || !body.containsKey(field) || body.get(field) == null) {
            return null;
        }
        return String.valueOf(body.get(field));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
