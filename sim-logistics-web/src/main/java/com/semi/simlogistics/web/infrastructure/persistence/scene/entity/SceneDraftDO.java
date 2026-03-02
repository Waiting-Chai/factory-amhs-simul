package com.semi.simlogistics.web.infrastructure.persistence.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;

/**
 * Data object for scene_drafts table.
 * Aligned with database-schema.md spec.
 *
 * Table: scene_drafts
 * - id: CHAR(36) PRIMARY KEY
 * - tenant_id: CHAR(36) NOT NULL
 * - scene_id: CHAR(36) NOT NULL (FK to scenes.id)
 * - content: JSON NOT NULL (SceneDetail payload - version is stored inside)
 * - updated_at: DATETIME(3)
 * - created_at: DATETIME(3)
 *
 * Note: version is stored inside content JSON, not as a separate column.
 *
 * @author shentw
 * @version 1.4
 * @since 2026-02-10
 */
@TableName(value = "scene_drafts", autoResultMap = true)
public class SceneDraftDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("scene_id")
    private String sceneId;

    @TableField(value = "content", typeHandler = JacksonTypeHandler.class)
    private String content;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    // savedAt is an alias for updatedAt (domain model uses savedAt)
    private transient LocalDateTime savedAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSavedAt() {
        return savedAt != null ? savedAt : updatedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
        // Also update updatedAt to keep them in sync
        this.updatedAt = savedAt;
    }

    @Override
    public String toString() {
        return "SceneDraftDO{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", sceneId='" + sceneId + '\'' +
                ", updatedAt=" + updatedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
