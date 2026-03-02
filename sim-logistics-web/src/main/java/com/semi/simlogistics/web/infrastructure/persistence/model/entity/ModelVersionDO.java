package com.semi.simlogistics.web.infrastructure.persistence.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;

/**
 * Data object for model_versions table.
 *
 * Table: model_versions
 * - id: CHAR(36) PRIMARY KEY
 * - tenant_id: CHAR(36) NOT NULL
 * - model_id: CHAR(36) NOT NULL
 * - version: VARCHAR(50) NOT NULL
 * - file_id: CHAR(36) NOT NULL
 * - transform_config: JSON
 * - is_default: BOOLEAN NOT NULL DEFAULT FALSE
 * - status: VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
 * - created_at: DATETIME(3)
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@TableName(value = "model_versions", autoResultMap = true)
public class ModelVersionDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("model_id")
    // References model_library.id (DB primary key), not business modelId.
    private String modelId;

    @TableField("version")
    private String version;

    @TableField("file_id")
    private String fileId;

    @TableField(value = "transform_config", typeHandler = JacksonTypeHandler.class)
    private String transformConfig;

    @TableField("is_default")
    private Boolean isDefault;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getTransformConfig() {
        return transformConfig;
    }

    public void setTransformConfig(String transformConfig) {
        this.transformConfig = transformConfig;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
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
    public String toString() {
        return "ModelVersionDO{" +
                "id='" + id + '\'' +
                ", modelId='" + modelId + '\'' +
                ", version='" + version + '\'' +
                ", isDefault=" + isDefault +
                ", status='" + status + '\'' +
                '}';
    }
}
