package com.semi.simlogistics.web.infrastructure.persistence.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;

/**
 * Data object for entity_model_binding table.
 *
 * Table: entity_model_binding
 * - id: CHAR(36) PRIMARY KEY
 * - tenant_id: CHAR(36) NOT NULL
 * - scene_id: CHAR(36) NOT NULL
 * - entity_id: VARCHAR(100) NOT NULL
 * - model_version_id: CHAR(36) NOT NULL
 * - custom_transform: JSON
 * - created_at: DATETIME(3)
 * - updated_at: DATETIME(3)
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@TableName(value = "entity_model_binding", autoResultMap = true)
public class EntityModelBindingDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("scene_id")
    private String sceneId;

    @TableField("entity_id")
    private String entityId;

    @TableField("model_version_id")
    private String modelVersionId;

    @TableField(value = "custom_transform", typeHandler = JacksonTypeHandler.class)
    private String customTransform;

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

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getModelVersionId() {
        return modelVersionId;
    }

    public void setModelVersionId(String modelVersionId) {
        this.modelVersionId = modelVersionId;
    }

    public String getCustomTransform() {
        return customTransform;
    }

    public void setCustomTransform(String customTransform) {
        this.customTransform = customTransform;
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
        return "EntityModelBindingDO{" +
                "id='" + id + '\'' +
                ", sceneId='" + sceneId + '\'' +
                ", entityId='" + entityId + '\'' +
                ", modelVersionId='" + modelVersionId + '\'' +
                '}';
    }
}
