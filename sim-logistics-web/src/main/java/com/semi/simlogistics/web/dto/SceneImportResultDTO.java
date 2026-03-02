package com.semi.simlogistics.web.dto;

import java.util.List;

/**
 * Result DTO for scene import operation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneImportResultDTO {

    private String sceneId;
    private String name;
    private int version;
    private List<String> warnings;

    public SceneImportResultDTO() {
    }

    public SceneImportResultDTO(String sceneId, String name, int version) {
        this.sceneId = sceneId;
        this.name = name;
        this.version = version;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    @Override
    public String toString() {
        return "SceneImportResultDTO{" +
                "sceneId='" + sceneId + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                '}';
    }
}
