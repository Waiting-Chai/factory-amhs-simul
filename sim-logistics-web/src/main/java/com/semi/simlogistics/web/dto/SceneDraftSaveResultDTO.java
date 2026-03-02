package com.semi.simlogistics.web.dto;

import java.time.LocalDateTime;

/**
 * Result DTO for scene draft save operation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SceneDraftSaveResultDTO {

    private boolean success;
    private LocalDateTime savedAt;

    public SceneDraftSaveResultDTO() {
    }

    public SceneDraftSaveResultDTO(boolean success, LocalDateTime savedAt) {
        this.success = success;
        this.savedAt = savedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    @Override
    public String toString() {
        return "SceneDraftSaveResultDTO{" +
                "success=" + success +
                ", savedAt=" + savedAt +
                '}';
    }
}
