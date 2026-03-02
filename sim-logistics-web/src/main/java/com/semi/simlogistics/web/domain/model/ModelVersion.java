package com.semi.simlogistics.web.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.semi.simlogistics.web.dto.TransformConfigDTO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model for Model version.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class ModelVersion {

    private String id;
    private String versionId;
    private String modelId;
    private String version;
    private String fileId;
    private TransformConfigDTO transformConfig;
    private boolean isDefault;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public ModelVersion() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public TransformConfigDTO getTransformConfig() {
        return transformConfig;
    }

    public void setTransformConfig(TransformConfigDTO transformConfig) {
        this.transformConfig = transformConfig;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelVersion that = (ModelVersion) o;
        return Objects.equals(versionId, that.versionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId);
    }

    @Override
    public String toString() {
        return "ModelVersion{" +
                "versionId='" + versionId + '\'' +
                ", version='" + version + '\'' +
                ", isDefault=" + isDefault +
                ", status='" + status + '\'' +
                '}';
    }
}
