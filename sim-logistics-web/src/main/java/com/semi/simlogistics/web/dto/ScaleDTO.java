package com.semi.simlogistics.web.dto;

/**
 * Scale configuration for 3D models.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class ScaleDTO {

    private double x;
    private double y;
    private double z;

    public ScaleDTO() {
    }

    public ScaleDTO(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
