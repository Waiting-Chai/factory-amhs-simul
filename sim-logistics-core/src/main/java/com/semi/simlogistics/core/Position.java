package com.semi.simlogistics.core;

import java.util.Objects;

/**
 * 3D position in logistics simulation space.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Position {

    private final double x;
    private final double y;
    private final double z;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position(double x, double y) {
        this(x, y, 0.0);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    /**
     * Calculate Euclidean distance to another position.
     *
     * @param other the target position
     * @return distance in meters
     */
    public double distanceTo(Position other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return Double.compare(position.x, x) == 0
                && Double.compare(position.y, y) == 0
                && Double.compare(position.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("Position(%.2f, %.2f, %.2f)", x, y, z);
    }
}
