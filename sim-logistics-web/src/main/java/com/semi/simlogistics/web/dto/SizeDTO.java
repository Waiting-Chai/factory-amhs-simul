package com.semi.simlogistics.web.dto;

/**
 * Size dimensions for 3D models.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class SizeDTO {

    private double width;
    private double height;
    private double depth;

    public SizeDTO() {
    }

    public SizeDTO(double width, double height, double depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }
}
