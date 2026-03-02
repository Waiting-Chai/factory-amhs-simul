package com.semi.simlogistics.web.domain.scene;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.semi.simlogistics.web.dto.EntityDTO;
import com.semi.simlogistics.web.dto.PathDTO;
import com.semi.simlogistics.web.dto.ProcessFlowBindingDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain model for Scene.
 *
 * Contains persistent ID for update detection.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-10
 */
public class Scene {

    /**
     * Persistent database ID (CHAR(36)).
     * Used to distinguish insert vs update operations.
     * Null for new scenes, set for existing scenes loaded from database.
     */
    private String id;

    /**
     * Business scene ID (e.g., "SCENE-12345678").
     * Unique business identifier used in API.
     */
    private String sceneId;
    private String name;
    private String description;
    private int version;
    private List<EntityDTO> entities;
    private List<PathDTO> paths;
    private List<ProcessFlowBindingDTO> processFlows;
    private int entityCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public Scene() {
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

    public List<EntityDTO> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityDTO> entities) {
        this.entities = entities;
        this.entityCount = entities != null ? entities.size() : 0;
    }

    public List<PathDTO> getPaths() {
        return paths;
    }

    public void setPaths(List<PathDTO> paths) {
        this.paths = paths;
    }

    public List<ProcessFlowBindingDTO> getProcessFlows() {
        return processFlows;
    }

    public void setProcessFlows(List<ProcessFlowBindingDTO> processFlows) {
        this.processFlows = processFlows;
    }

    public int getEntityCount() {
        return entityCount;
    }

    public void setEntityCount(int entityCount) {
        this.entityCount = entityCount;
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
        Scene scene = (Scene) o;
        return Objects.equals(sceneId, scene.sceneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sceneId);
    }

    @Override
    public String toString() {
        return "Scene{" +
                "sceneId='" + sceneId + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", entityCount=" + entityCount +
                '}';
    }
}
