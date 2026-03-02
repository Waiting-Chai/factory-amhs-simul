package com.semi.simlogistics.web.dto;

/**
 * Transform configuration for model positioning.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class TransformConfigDTO {

    private ScaleDTO scale;
    private RotationDTO rotation;
    private PositionDTO pivot;

    public TransformConfigDTO() {
    }

    public ScaleDTO getScale() {
        return scale;
    }

    public void setScale(ScaleDTO scale) {
        this.scale = scale;
    }

    public RotationDTO getRotation() {
        return rotation;
    }

    public void setRotation(RotationDTO rotation) {
        this.rotation = rotation;
    }

    public PositionDTO getPivot() {
        return pivot;
    }

    public void setPivot(PositionDTO pivot) {
        this.pivot = pivot;
    }
}
