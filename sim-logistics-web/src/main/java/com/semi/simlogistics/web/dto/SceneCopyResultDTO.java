package com.semi.simlogistics.web.dto;

/**
 * Result DTO for scene copy operation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneCopyResultDTO {

    private String newSceneId;
    private String name;
    private int version;

    public SceneCopyResultDTO() {
    }

    public SceneCopyResultDTO(String newSceneId, String name, int version) {
        this.newSceneId = newSceneId;
        this.name = name;
        this.version = version;
    }

    public String getNewSceneId() {
        return newSceneId;
    }

    public void setNewSceneId(String newSceneId) {
        this.newSceneId = newSceneId;
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

    @Override
    public String toString() {
        return "SceneCopyResultDTO{" +
                "newSceneId='" + newSceneId + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                '}';
    }
}
