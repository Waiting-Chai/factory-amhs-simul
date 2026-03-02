package com.semi.simlogistics.web.dto;

import java.util.Objects;

/**
 * 3D position in logistics simulation space.
 * Unit: meters
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class PositionDTO {

    private double x;
    private double y;
    private double z;

    public PositionDTO() {
    }

    public PositionDTO(double x, double y, double z) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionDTO that = (PositionDTO) o;
        return Double.compare(that.x, x) == 0
                && Double.compare(that.y, y) == 0
                && Double.compare(that.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "PositionDTO{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
