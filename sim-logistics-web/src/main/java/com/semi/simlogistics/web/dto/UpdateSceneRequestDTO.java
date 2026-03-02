package com.semi.simlogistics.web.dto;

import java.util.List;

/**
 * Request DTO for updating a scene.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class UpdateSceneRequestDTO {

    private String name;
    private String description;
    private List<EntityDTO> entities;
    private List<PathDTO> paths;
    private List<ProcessFlowBindingDTO> processFlows;

    public UpdateSceneRequestDTO() {
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
}
