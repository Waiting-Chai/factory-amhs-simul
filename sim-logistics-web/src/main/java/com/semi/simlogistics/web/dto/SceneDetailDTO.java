package com.semi.simlogistics.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scene detail with full information.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneDetailDTO {

    private String sceneId;
    private String name;
    private String description;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EntityDTO> entities;
    private List<PathDTO> paths;
    private List<ProcessFlowBindingDTO> processFlows;

    public SceneDetailDTO() {
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

    public List<EntityDTO> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityDTO> entities) {
        this.entities = entities;
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

    @Override
    public String toString() {
        return "SceneDetailDTO{" +
                "sceneId='" + sceneId + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                '}';
    }
}
