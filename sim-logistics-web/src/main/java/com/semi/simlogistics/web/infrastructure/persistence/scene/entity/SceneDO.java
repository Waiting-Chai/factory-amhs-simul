package com.semi.simlogistics.web.infrastructure.persistence.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;

/**
 * Data object for scenes table.
 * Aligned with database-schema.md spec.
 *
 * Table: scenes
 * - id: CHAR(36) PRIMARY KEY (also serves as scene_id business identifier)
 * - tenant_id: CHAR(36) NOT NULL
 * - name: VARCHAR(255) NOT NULL
 * - description: TEXT
 * - version: INT NOT NULL
 * - schema_version: INT NOT NULL DEFAULT 1
 * - definition: JSON NOT NULL (contains entities, paths, processFlows)
 * - metadata: JSON (contains entityCount, etc.)
 * - created_at: DATETIME(3)
 * - updated_at: DATETIME(3)
 * - created_by: VARCHAR(100)
 * - updated_by: VARCHAR(100)
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-10
 */
@TableName(value = "scenes", autoResultMap = true)
public class SceneDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    // id serves as both persistent ID and business sceneId
    // We keep sceneId as an alias for compatibility
    @TableField(exist = false)
    private String sceneId;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("version")
    private Integer version;

    @TableField("schema_version")
    private Integer schemaVersion;

    @TableField(value = "definition", typeHandler = JacksonTypeHandler.class)
    private String definition;

    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private String metadata;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        // sceneId is an alias for id (business identifier)
        this.sceneId = id;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
        // Keep id in sync with sceneId
        this.id = sceneId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "SceneDO{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", schemaVersion=" + schemaVersion +
                '}';
    }
}
