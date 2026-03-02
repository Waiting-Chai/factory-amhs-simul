package com.semi.simlogistics.web.dto;

/**
 * Metadata for 3D model files.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class ModelMetadataDTO {

    private SizeDTO size;
    private PositionDTO anchor;

    public ModelMetadataDTO() {
    }

    public SizeDTO getSize() {
        return size;
    }

    public void setSize(SizeDTO size) {
        this.size = size;
    }

    public PositionDTO getAnchor() {
        return anchor;
    }

    public void setAnchor(PositionDTO anchor) {
        this.anchor = anchor;
    }
}
