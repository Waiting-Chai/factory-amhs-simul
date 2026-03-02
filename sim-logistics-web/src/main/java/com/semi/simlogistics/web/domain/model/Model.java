package com.semi.simlogistics.web.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.semi.simlogistics.web.dto.ModelMetadataDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain model for Model (GLB 3D model).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class Model {

    /**
     * Persistent database ID (CHAR(36)).
     * Used to distinguish insert vs update operations.
     */
    private String id;

    /**
     * Business model ID (e.g., "MODEL-12345678").
     */
    private String modelId;
    private String name;
    private String description;
    private String modelType;
    private String status;
    private String defaultVersionId;
    private ModelMetadataDTO metadata;
    private List<ModelVersion> versions;
    private int versionCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public Model() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public ModelMetadataDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(ModelMetadataDTO metadata) {
        this.metadata = metadata;
    }

    public List<ModelVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<ModelVersion> versions) {
        this.versions = versions;
        this.versionCount = versions != null ? versions.size() : 0;
    }

    public int getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(int versionCount) {
        this.versionCount = versionCount;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Model model = (Model) o;
        return Objects.equals(modelId, model.modelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId);
    }

    @Override
    public String toString() {
        return "Model{" +
                "modelId='" + modelId + '\'' +
                ", name='" + name + '\'' +
                ", modelType='" + modelType + '\'' +
                ", status='" + status + '\'' +
                ", versionCount=" + versionCount +
                '}';
    }
}
