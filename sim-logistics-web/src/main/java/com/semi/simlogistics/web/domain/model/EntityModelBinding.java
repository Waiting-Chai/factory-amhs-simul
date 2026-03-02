package com.semi.simlogistics.web.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.semi.simlogistics.web.dto.TransformConfigDTO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model for entity-model binding.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class EntityModelBinding {

    private String id;
    private String sceneId;
    private String entityId;
    private String modelVersionId;
    private TransformConfigDTO customTransform;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public EntityModelBinding() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public TransformConfigDTO getCustomTransform() {
        return customTransform;
    }

    public void setCustomTransform(TransformConfigDTO customTransform) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityModelBinding that = (EntityModelBinding) o;
        return Objects.equals(sceneId, that.sceneId) && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sceneId, entityId);
    }

    @Override
    public String toString() {
        return "EntityModelBinding{" +
                "sceneId='" + sceneId + '\'' +
                ", entityId='" + entityId + '\'' +
                ", modelVersionId='" + modelVersionId + '\'' +
                '}';
    }
}
