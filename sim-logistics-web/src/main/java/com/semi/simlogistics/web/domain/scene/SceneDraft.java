package com.semi.simlogistics.web.domain.scene;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.semi.simlogistics.web.dto.SceneDetailDTO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model for Scene draft.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneDraft {

    private String sceneId;
    private SceneDetailDTO content;
    private int version;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime savedAt;

    public SceneDraft() {
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SceneDraft that = (SceneDraft) o;
        return Objects.equals(sceneId, that.sceneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sceneId);
    }

    @Override
    public String toString() {
        return "SceneDraft{" +
                "sceneId='" + sceneId + '\'' +
                ", version=" + version +
                ", savedAt=" + savedAt +
                '}';
    }
}
