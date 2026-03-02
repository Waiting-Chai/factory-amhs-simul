package com.semi.simlogistics.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Model version information.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class ModelVersionDTO {

    private String versionId;
    private String version;
    private boolean isDefault;
    private String status;
    private TransformConfigDTO transformConfig;
    private String fileUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public ModelVersionDTO() {
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public TransformConfigDTO getTransformConfig() {
        return transformConfig;
    }

    public void setTransformConfig(TransformConfigDTO transformConfig) {
        this.transformConfig = transformConfig;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
