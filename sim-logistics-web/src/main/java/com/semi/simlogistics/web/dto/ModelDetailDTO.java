package com.semi.simlogistics.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed representation of a model.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class ModelDetailDTO {

    private String modelId;
    private String name;
    private String description;
    private String modelType;
    private String version;
    private String status;
    private String defaultVersionId;
    private TransformConfigDTO transformConfig;
    private ModelMetadataDTO metadata;
    private List<ModelVersionDTO> versions;
    private String thumbnailUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public ModelDetailDTO() {
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDefaultVersionId() {
        return defaultVersionId;
    }

    public void setDefaultVersionId(String defaultVersionId) {
        this.defaultVersionId = defaultVersionId;
    }

    public TransformConfigDTO getTransformConfig() {
        return transformConfig;
    }

    public void setTransformConfig(TransformConfigDTO transformConfig) {
        this.transformConfig = transformConfig;
    }

    public ModelMetadataDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(ModelMetadataDTO metadata) {
        this.metadata = metadata;
    }

    public List<ModelVersionDTO> getVersions() {
        return versions;
    }

    public void setVersions(List<ModelVersionDTO> versions) {
        this.versions = versions;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
