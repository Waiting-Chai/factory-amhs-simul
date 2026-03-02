package com.semi.simlogistics.web.infrastructure.persistence.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;

/**
 * Data object for model_library table.
 *
 * Table: model_library
 * - id: CHAR(36) PRIMARY KEY
 * - tenant_id: CHAR(36) NOT NULL
 * - model_type: VARCHAR(50) NOT NULL
 * - name: VARCHAR(255) NOT NULL
 * - description: TEXT
 * - status: VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
 * - default_version_id: CHAR(36)
 * - metadata: JSON
 * - created_at: DATETIME(3)
 * - updated_at: DATETIME(3)
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@TableName(value = "model_library", autoResultMap = true)
public class ModelLibraryDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("model_type")
    private String modelType;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("status")
    private String status;

    @TableField("default_version_id")
    private String defaultVersionId;

    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private String metadata;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

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

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
    public String toString() {
        return "ModelLibraryDO{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", modelType='" + modelType + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
