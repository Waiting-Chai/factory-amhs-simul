package com.semi.simlogistics.control.path;

import com.semi.simlogistics.core.Position;

import java.util.Objects;

/**
 * Path point with ID and position (REQ-TC-003, REQ-TC-012).
 * <p>
 * Represents a point in a curved path that can be referenced by path segments.
 * Points are stored in the path's points collection and referenced by ID
 * from segment definitions.
 * <p>
 * Coordinate unit: meters (REQ-TC-004)
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class PathPoint {

    private final String id;
    private final Position position;

    /**
     * Create a path point.
     *
     * @param id unique point ID
     * @param position position in meters (x, y, z)
     * @throws IllegalArgumentException if id is null or empty
     * @throws IllegalArgumentException if position is null
     */
    public PathPoint(String id, Position position) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Point ID cannot be null or empty");
        }
        this.id = id;
        this.position = Objects.requireNonNull(position, "Position cannot be null");
    }

    /**
     * Get point ID.
     *
     * @return point ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get point position.
     *
     * @return position in meters (x, y, z)
     */
    public Position getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathPoint pathPoint = (PathPoint) o;
        return Objects.equals(id, pathPoint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PathPoint{" +
                "id='" + id + '\'' +
                ", position=" + position +
                '}';
    }
}
