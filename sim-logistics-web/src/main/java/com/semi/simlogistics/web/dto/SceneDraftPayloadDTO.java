package com.semi.simlogistics.web.dto;

import java.time.LocalDateTime;

/**
 * Scene draft payload.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneDraftPayloadDTO {

    private String sceneId;
    private SceneDetailDTO content;
    private LocalDateTime savedAt;
    private int version;

    public SceneDraftPayloadDTO() {
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }

    public SceneDetailDTO getContent() {
        return content;
    }

    public void setContent(SceneDetailDTO content) {
        this.content = content;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "SceneDraftPayloadDTO{" +
                "sceneId='" + sceneId + '\'' +
                ", savedAt=" + savedAt +
                ", version=" + version +
                '}';
    }
}
