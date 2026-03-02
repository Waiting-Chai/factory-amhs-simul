package com.semi.simlogistics.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.domain.model.*;
import com.semi.simlogistics.web.dto.*;
import com.semi.simlogistics.web.exception.ResourceConflictException;
import com.semi.simlogistics.web.exception.ResourceNotFoundException;
import com.semi.simlogistics.web.exception.ValidationException;
import com.semi.simlogistics.web.infrastructure.storage.ObjectStorageCompensationPort;
import com.semi.simlogistics.web.infrastructure.persistence.model.File;
import com.semi.simlogistics.web.infrastructure.persistence.model.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for model management operations.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@Service
public class ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelService.class);

    private final ModelRepository modelRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final EntityModelBindingRepository entityModelBindingRepository;
    private final FileRepository fileRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ObjectMapper objectMapper;
    private final String storageBucket;

    public ModelService(ModelRepository modelRepository,
                        ModelVersionRepository modelVersionRepository,
                        EntityModelBindingRepository entityModelBindingRepository,
                        FileRepository fileRepository,
                        ObjectStoragePort objectStoragePort,
                        ObjectMapper objectMapper,
                        @Value("${sim.minio.default-bucket:sim-artifacts}") String storageBucket) {
        this.modelRepository = modelRepository;
        this.modelVersionRepository = modelVersionRepository;
        this.entityModelBindingRepository = entityModelBindingRepository;
        this.fileRepository = fileRepository;
        this.objectStoragePort = objectStoragePort;
        this.objectMapper = objectMapper;
        this.storageBucket = storageBucket;
    }

    /**
     * Get paginated list of models.
     *
     * @param page the page number (0-based)
     * @param pageSize the page size
     * @param modelType the model type filter (optional)
     * @param status the model status filter (optional)
     * @param search the search keyword (optional)
     * @return paged result of model summaries
     */
    public PagedResultDTO<ModelSummaryDTO> getModels(int page, int pageSize, String modelType, String status, String search) {
        logger.info("Getting models: page={}, pageSize={}, type={}, status={}, search={}",
                page, pageSize, modelType, status, search);

        List<ModelSummaryDTO> items = modelRepository.findByFilters(page, pageSize, modelType, status, search);
        long total = modelRepository.countByFilters(modelType, status, search);

        return new PagedResultDTO<>(items, total, page, pageSize);
    }

    /**
     * Get model detail by ID.
     *
     * @param modelId the model ID
     * @return model detail
     * @throws ResourceNotFoundException if model not found
     */
    public ModelDetailDTO getModelById(String modelId) {
        logger.info("Getting model by ID: {}", modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        List<ModelVersion> versions = modelVersionRepository.findByModelId(modelId);
        model.setVersions(versions);

        return toDetailDTO(model);
    }

    /**
     * Upload a new model file.
     *
     * @param file the uploaded GLB file
     * @param name the model name
     * @param modelType the model type
     * @param description the description
     * @return upload result
     * @throws ValidationException if validation fails
     */
    @Transactional
    public ModelUploadResultDTO uploadModel(
            MultipartFile file,
            String name,
            String modelType,
            String description,
            String initialVersion,
            ModelMetadataDTO metadata,
            TransformConfigDTO transformConfig) {
        logger.info("Uploading model: name={}, modelType={}, file={}", name, modelType, file.getOriginalFilename());

        validateModelFile(file);
        validateModelName(name);

        if (modelRepository.existsByName(name)) {
            throw new ResourceConflictException("Model name already exists: " + name);
        }

        String businessModelId = generateModelId();
        String fileId = UUID.randomUUID().toString();
        String storageKey = generateStorageKey(businessModelId, file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            objectStoragePort.putObject(
                    storageBucket,
                    storageKey,
                    inputStream,
                    file.getSize(),
                    "model/gltf-binary"
            );

            File fileRecord = new File();
            fileRecord.setId(fileId);
            fileRecord.setFileName(file.getOriginalFilename());
            fileRecord.setFileType("model/gltf-binary");
            fileRecord.setFileSize(file.getSize());
            fileRecord.setStorageBucket(storageBucket);
            fileRecord.setStorageKey(storageKey);
            fileRecord.setStorageUrl(buildFileUrl(fileId));
            fileRepository.save(fileRecord);

            Model model = new Model();
            // DB primary key used by model_versions.model_id FK
            model.setId(UUID.randomUUID().toString());
            // Keep business id for upload flow and object key tracing
            model.setModelId(businessModelId);
            model.setName(name);
            model.setDescription(description);
            model.setModelType(modelType);
            model.setStatus("ACTIVE");
            model.setDefaultVersionId(null);
            model.setMetadata(metadata);
            model.setVersions(new ArrayList<>());
            model.setCreatedAt(LocalDateTime.now());
            model.setUpdatedAt(LocalDateTime.now());
            Model savedModel = modelRepository.save(model);

            ModelVersion modelVersion = new ModelVersion();
            modelVersion.setVersionId(UUID.randomUUID().toString());
            // Must reference model_library.id, not business modelId
            modelVersion.setModelId(savedModel.getId());
            modelVersion.setVersion(initialVersion);
            modelVersion.setFileId(fileId);
            modelVersion.setTransformConfig(transformConfig);
            modelVersion.setDefault(true);
            modelVersion.setStatus("ACTIVE");
            modelVersion.setCreatedAt(LocalDateTime.now());
            ModelVersion savedVersion = modelVersionRepository.save(modelVersion);

            savedModel.setDefaultVersionId(savedVersion.getVersionId());
            savedModel.setUpdatedAt(LocalDateTime.now());
            modelRepository.save(savedModel);

            logger.info("Model uploaded successfully: modelId={}, modelDbId={}", savedModel.getModelId(), savedModel.getId());

            ModelUploadResultDTO result = new ModelUploadResultDTO();
            result.setModelId(savedModel.getModelId());
            result.setVersionId(savedVersion.getVersionId());
            result.setName(name);
            result.setModelType(modelType);
            result.setVersion(initialVersion);
            result.setFileUrl(fileRecord.getStorageUrl());
            result.setCreatedAt(LocalDateTime.now());

            return result;
        } catch (RuntimeException e) {
            compensateDeleteUploadedObject(storageBucket, storageKey);
            throw e;
        } catch (IOException e) {
            logger.error("Failed to upload model file: {}", e.getMessage(), e);
            throw new ValidationException("file", "Failed to read uploaded file");
        }
    }

    /**
     * Update model metadata.
     *
     * @param modelId the model ID
     * @param name the new name
     * @param description the new description
     * @param modelType the new model type
     * @param transformConfig the transform config
     * @return updated model detail
     * @throws ResourceNotFoundException if model not found
     */
    @Transactional
    public ModelDetailDTO updateModel(
            String modelId,
            String name,
            String description,
            String modelType,
            ModelMetadataDTO metadata,
            TransformConfigDTO transformConfig) {
        logger.info("Updating model: modelId={}", modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        if (name != null && !name.equals(model.getName())) {
            if (modelRepository.existsByNameExcluding(name, modelId)) {
                throw new ResourceConflictException("Model name already exists: " + name);
            }
            model.setName(name);
        }

        if (description != null) {
            model.setDescription(description);
        }

        if (modelType != null) {
            model.setModelType(modelType);
        }
        if (metadata != null) {
            model.setMetadata(metadata);
        }

        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);

        logger.info("Model updated successfully: modelId={}", modelId);

        return getModelById(modelId);
    }

    /**
     * Upload a new version for existing model.
     *
     * @param modelId the model ID
     * @param file the uploaded GLB file
     * @param version the version string
     * @param transformConfig the transform config
     * @return model version detail
     * @throws ResourceNotFoundException if model not found
     */
    @Transactional
    public ModelVersionDTO uploadModelVersion(String modelId, MultipartFile file, String version, TransformConfigDTO transformConfig) {
        logger.info("Uploading model version: modelId={}, version={}", modelId, version);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        validateModelFile(file);

        String fileId = UUID.randomUUID().toString();
        String storageKey = generateStorageKey(modelId, file.getOriginalFilename());

        try {
            InputStream inputStream = file.getInputStream();
            objectStoragePort.putObject(storageBucket, storageKey, inputStream, file.getSize(), "model/gltf-binary");

            File fileRecord = new File();
            fileRecord.setId(fileId);
            fileRecord.setFileName(file.getOriginalFilename());
            fileRecord.setFileType("model/gltf-binary");
            fileRecord.setFileSize(file.getSize());
            fileRecord.setStorageBucket(storageBucket);
            fileRecord.setStorageKey(storageKey);
            fileRecord.setStorageUrl(buildFileUrl(fileId));
            fileRepository.save(fileRecord);

            ModelVersion modelVersion = new ModelVersion();
            modelVersion.setVersionId(UUID.randomUUID().toString());
            modelVersion.setModelId(modelId);
            modelVersion.setVersion(version);
            modelVersion.setFileId(fileId);
            modelVersion.setTransformConfig(transformConfig);
            modelVersion.setDefault(false);
            modelVersion.setStatus("ACTIVE");
            modelVersion.setCreatedAt(LocalDateTime.now());
            modelVersionRepository.save(modelVersion);

            logger.info("Model version uploaded successfully: modelId={}, version={}", modelId, version);

            return toVersionDTO(modelVersion, fileRecord.getStorageUrl());

        } catch (RuntimeException e) {
            compensateDeleteUploadedObject(storageBucket, storageKey);
            throw e;
        } catch (IOException e) {
            logger.error("Failed to upload model version file: {}", e.getMessage(), e);
            throw new ValidationException("file", "Failed to read uploaded file");
        }
    }

    /**
     * Set model version as default.
     *
     * @param modelId the model ID
     * @param versionId the version ID
     * @throws ResourceNotFoundException if model or version not found
     */
    @Transactional
    public ModelVersionDTO setDefaultVersion(String modelId, String versionId) {
        logger.info("Setting default version: modelId={}, versionId={}", modelId, versionId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        ModelVersion version = modelVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("ModelVersion", versionId));

        if (!version.getModelId().equals(modelId)) {
            throw new ValidationException("versionId", "Version does not belong to this model");
        }

        modelVersionRepository.setDefaultVersion(modelId, versionId);

        model.setDefaultVersionId(versionId);
        modelRepository.save(model);

        logger.info("Default version set successfully: modelId={}, versionId={}", modelId, versionId);

        ModelVersion updatedVersion = modelVersionRepository.findById(versionId)
                .orElse(version);
        String fileUrl = null;
        if (updatedVersion.getFileId() != null) {
            fileUrl = fileRepository.findById(updatedVersion.getFileId())
                    .map(File::getStorageUrl)
                    .orElse(null);
        }
        return toVersionDTO(updatedVersion, fileUrl);
    }

    /**
     * Enable model.
     *
     * @param modelId the model ID
     * @throws ResourceNotFoundException if model not found
     */
    @Transactional
    public void enableModel(String modelId) {
        logger.info("Enabling model: modelId={}", modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        model.setStatus("ACTIVE");
        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);

        logger.info("Model enabled successfully: modelId={}", modelId);
    }

    /**
     * Disable model.
     *
     * @param modelId the model ID
     * @throws ResourceNotFoundException if model not found
     */
    @Transactional
    public void disableModel(String modelId) {
        logger.info("Disabling model: modelId={}", modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        model.setStatus("DISABLED");
        model.setUpdatedAt(LocalDateTime.now());
        modelRepository.save(model);

        logger.info("Model disabled successfully: modelId={}", modelId);
    }

    /**
     * Delete model.
     *
     * @param modelId the model ID
     * @throws ResourceNotFoundException if model not found
     */
    @Transactional
    public void deleteModel(String modelId) {
        logger.info("Deleting model: modelId={}", modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelId));

        List<ModelVersion> versions = modelVersionRepository.findByModelId(modelId);
        Map<String, File> filesById = new LinkedHashMap<>();
        for (ModelVersion version : versions) {
            if (version.getFileId() == null || version.getFileId().isBlank()) {
                continue;
            }
            fileRepository.findById(version.getFileId())
                    .ifPresent(file -> filesById.putIfAbsent(version.getFileId(), file));
        }

        // Break fk_model_library_default_version before deleting versions.
        if (model.getDefaultVersionId() != null && !model.getDefaultVersionId().isBlank()) {
            modelRepository.clearDefaultVersionId(modelId);
        }

        // Delete child records first to satisfy fk_model_versions_file.
        for (ModelVersion version : versions) {
            modelRepository.clearDefaultVersionReferences(version.getVersionId());
            modelVersionRepository.deleteById(version.getVersionId());
        }

        modelRepository.deleteById(modelId);

        List<File> filesToDeleteFromStorage = new ArrayList<>();
        for (Map.Entry<String, File> entry : filesById.entrySet()) {
            String fileId = entry.getKey();
            long remainingReferences = modelVersionRepository.countByFileId(fileId);
            if (remainingReferences > 0) {
                logger.info("Skipping file deletion because file is still referenced: modelId={}, fileId={}, remainingReferences={}",
                        modelId, fileId, remainingReferences);
                continue;
            }
            fileRepository.deleteById(fileId);
            filesToDeleteFromStorage.add(entry.getValue());
        }

        scheduleObjectDeletionAfterCommit(filesToDeleteFromStorage);

        logger.info("Model deleted successfully: modelId={}", modelId);
    }

    /**
     * Get entity-model bindings for a scene.
     *
     * @param sceneId the scene ID
     * @return list of bindings
     */
    public List<EntityModelBindingDTO> getEntityBindings(String sceneId) {
        logger.info("Getting entity bindings for scene: {}", sceneId);

        List<EntityModelBinding> bindings = entityModelBindingRepository.findBySceneId(sceneId);
        List<EntityModelBindingDTO> result = new ArrayList<>();

        for (EntityModelBinding binding : bindings) {
            EntityModelBindingDTO dto = new EntityModelBindingDTO();
            dto.setBindingId(binding.getId());
            dto.setEntityId(binding.getEntityId());
            dto.setVersionId(binding.getModelVersionId());
            modelVersionRepository.findById(binding.getModelVersionId())
                    .ifPresent(version -> dto.setModelId(version.getModelId()));
            dto.setCustomTransform(binding.getCustomTransform());
            result.add(dto);
        }

        return result;
    }

    /**
     * Set entity-model binding.
     *
     * @param sceneId the scene ID
     * @param entityId the entity ID
     * @param request the binding request
     */
    @Transactional
    public void setEntityBinding(String sceneId, String entityId, SetEntityModelBindingRequestDTO request) {
        String resolvedVersionId = request.getModelVersionId();
        if (resolvedVersionId == null || resolvedVersionId.isBlank()) {
            resolvedVersionId = request.getVersionId();
        }
        if (resolvedVersionId == null || resolvedVersionId.isBlank()) {
            throw new ValidationException("versionId", "versionId is required");
        }
        final String finalVersionId = resolvedVersionId;
        ModelVersion targetVersion = modelVersionRepository.findById(finalVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("ModelVersion", finalVersionId));

        if (request.getModelId() != null && !request.getModelId().isBlank()
                && !request.getModelId().equals(targetVersion.getModelId())) {
            throw new ValidationException("modelId", "modelId does not match versionId");
        }

        logger.info("Setting entity binding: sceneId={}, entityId={}, modelVersionId={}",
                sceneId, entityId, resolvedVersionId);

        Optional<EntityModelBinding> existing = entityModelBindingRepository.findBySceneIdAndEntityId(sceneId, entityId);

        if (existing.isPresent()) {
            EntityModelBinding binding = existing.get();
            binding.setModelVersionId(resolvedVersionId);
            binding.setCustomTransform(request.getCustomTransform());
            binding.setUpdatedAt(LocalDateTime.now());
            entityModelBindingRepository.save(binding);
        } else {
            EntityModelBinding binding = new EntityModelBinding();
            binding.setSceneId(sceneId);
            binding.setEntityId(entityId);
            binding.setModelVersionId(resolvedVersionId);
            binding.setCustomTransform(request.getCustomTransform());
            binding.setCreatedAt(LocalDateTime.now());
            binding.setUpdatedAt(LocalDateTime.now());
            entityModelBindingRepository.save(binding);
        }

        logger.info("Entity binding set successfully: sceneId={}, entityId={}", sceneId, entityId);
    }

    private void validateModelFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("file", "File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".glb")) {
            throw new ValidationException("file", "Only GLB files are supported");
        }
    }

    private void validateModelName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "Model name is required");
        }
    }

    private String generateModelId() {
        return "MODEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateStorageKey(String modelId, String fileName) {
        return "models/" + modelId + "/" + fileName;
    }

    private void compensateDeleteUploadedObject(String bucket, String storageKey) {
        if (!(objectStoragePort instanceof ObjectStorageCompensationPort compensationPort)) {
            logger.warn("Compensation skipped: object storage adapter does not support deleteObject, bucket={}, key={}",
                    bucket, storageKey);
            return;
        }
        try {
            compensationPort.deleteObject(bucket, storageKey);
            logger.warn("Compensation succeeded: deleted uploaded object bucket={}, key={}", bucket, storageKey);
        } catch (Exception compensationEx) {
            logger.error("Compensation failed: bucket={}, key={}, reason={}",
                    bucket, storageKey, compensationEx.getMessage(), compensationEx);
        }
    }

    private void scheduleObjectDeletionAfterCommit(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteObjectsQuietly(files);
                }
            });
            return;
        }
        deleteObjectsQuietly(files);
    }

    private void deleteObjectsQuietly(List<File> files) {
        if (!(objectStoragePort instanceof ObjectStorageCompensationPort compensationPort)) {
            logger.warn("Object delete skipped: adapter does not support deleteObject");
            return;
        }
        for (File file : files) {
            if (file.getStorageBucket() == null || file.getStorageKey() == null) {
                continue;
            }
            try {
                compensationPort.deleteObject(file.getStorageBucket(), file.getStorageKey());
            } catch (Exception ex) {
                logger.error("Failed to delete object after DB deletion: bucket={}, key={}, reason={}",
                        file.getStorageBucket(), file.getStorageKey(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * Build API URL for file download.
     * Returns /api/v1/files/{fileId}/content format.
     */
    private String buildFileUrl(String fileId) {
        return "/api/v1/files/" + fileId + "/content";
    }

    private ModelDetailDTO toDetailDTO(Model model) {
        ModelDetailDTO dto = new ModelDetailDTO();
        dto.setModelId(model.getModelId());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setModelType(model.getModelType());
        dto.setStatus(model.getStatus());
        dto.setDefaultVersionId(model.getDefaultVersionId());
        dto.setMetadata(model.getMetadata());
        dto.setCreatedAt(model.getCreatedAt());
        dto.setUpdatedAt(model.getUpdatedAt());

        List<ModelVersionDTO> versionDTOs = new ArrayList<>();
        for (ModelVersion version : model.getVersions()) {
            String fileUrl = null;
            if (version.getFileId() != null) {
                Optional<File> fileOpt = fileRepository.findById(version.getFileId());
                if (fileOpt.isPresent()) {
                    fileUrl = fileOpt.get().getStorageUrl();
                }
            }
            versionDTOs.add(toVersionDTO(version, fileUrl));
        }
        dto.setVersions(versionDTOs);

        return dto;
    }

    private ModelVersionDTO toVersionDTO(ModelVersion version, String fileUrl) {
        ModelVersionDTO dto = new ModelVersionDTO();
        dto.setVersionId(version.getVersionId());
        dto.setVersion(version.getVersion());
        dto.setDefault(version.isDefault());
        dto.setStatus(version.getStatus());
        dto.setTransformConfig(version.getTransformConfig());
        dto.setFileUrl(fileUrl);
        dto.setCreatedAt(version.getCreatedAt());
        return dto;
    }
}
