package com.semi.simlogistics.web.dto;

import java.time.LocalDateTime;

/**
 * Scene summary for list view.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneSummaryDTO {

    private String sceneId;
    private String name;
    private String description;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int entityCount;

    public SceneSummaryDTO() {
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public int getEntityCount() {
        return entityCount;
    }

    public void setEntityCount(int entityCount) {
        this.entityCount = entityCount;
    }

    @Override
    public String toString() {
        return "SceneSummaryDTO{" +
                "sceneId='" + sceneId + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", entityCount=" + entityCount +
                '}';
    }
}
