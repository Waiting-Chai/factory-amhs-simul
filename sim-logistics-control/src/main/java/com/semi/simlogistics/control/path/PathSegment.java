package com.semi.simlogistics.control.path;

import com.semi.simlogistics.core.Position;

import java.util.Objects;

/**
 * Path segment with type-specific structure (REQ-TC-003, REQ-TC-012).
 * <p>
 * LINEAR segment: from, to (straight line between two points)
 * BEZIER segment: from, to, c1, c2 (cubic Bezier curve)
 * <p>
 * from/to reference point IDs in the path's points collection.
 * c1/c2 are absolute coordinates in meters (not references).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class PathSegment {

    private final String id;
    private final SegmentType type;
    private final String from;
    private final String to;
    private final Position c1; // null for LINEAR
    private final Position c2; // null for LINEAR

    /**
     * Create a LINEAR segment.
     *
     * @param id segment ID
     * @param from start point ID (must exist in path's points)
     * @param to end point ID (must exist in path's points)
     * @throws IllegalArgumentException if any argument is null or empty
     */
    public PathSegment(String id, String from, String to) {
        this(id, SegmentType.LINEAR, from, to, null, null);
    }

    /**
     * Create a BEZIER segment.
     *
     * @param id segment ID
     * @param from start point ID (must exist in path's points)
     * @param to end point ID (must exist in path's points)
     * @param c1 control point 1 (absolute coordinates in meters)
     * @param c2 control point 2 (absolute coordinates in meters)
     * @throws IllegalArgumentException if any argument is null or empty
     */
    public PathSegment(String id, String from, String to, Position c1, Position c2) {
        this(id, SegmentType.BEZIER, from, to, c1, c2);
    }

    private PathSegment(String id, SegmentType type, String from, String to, Position c1, Position c2) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Segment ID cannot be null or empty");
        }
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("From point ID cannot be null or empty");
        }
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("To point ID cannot be null or empty");
        }

        this.id = id;
        this.type = Objects.requireNonNull(type, "Segment type cannot be null");
        this.from = from;
        this.to = to;

        // Validate control points match segment type
        if (type == SegmentType.BEZIER) {
            this.c1 = Objects.requireNonNull(c1, "Control point c1 cannot be null for BEZIER segment");
            this.c2 = Objects.requireNonNull(c2, "Control point c2 cannot be null for BEZIER segment");
        } else {
            this.c1 = null;
            this.c2 = null;
        }
    }

    /**
     * Get segment ID.
     *
     * @return segment ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get segment type.
     *
     * @return segment type (LINEAR or BEZIER)
     */
    public SegmentType getType() {
        return type;
    }

    /**
     * Get start point ID.
     *
     * @return point ID that must exist in path's points collection
     */
    public String getFrom() {
        return from;
    }

    /**
     * Get end point ID.
     *
     * @return point ID that must exist in path's points collection
     */
    public String getTo() {
        return to;
    }

    /**
     * Get control point 1 (BEZIER only).
     *
     * @return control point 1 position in meters, or null for LINEAR segments
     */
    public Position getC1() {
        return c1;
    }

    /**
     * Get control point 2 (BEZIER only).
     *
     * @return control point 2 position in meters, or null for LINEAR segments
     */
    public Position getC2() {
        return c2;
    }

    /**
     * Check if this segment has control points.
     *
     * @return true if BEZIER segment, false if LINEAR
     */
    public boolean hasControlPoints() {
        return type == SegmentType.BEZIER;
    }

    /**
     * Calculate segment length.
     * <p>
     * LINEAR: Euclidean distance between from and to points.
     * BEZIER: Arc length calculated via BezierMath.
     *
     * @param fromPoint start position (resolved from points by from ID)
     * @param toPoint end position (resolved from points by to ID)
     * @return length in meters
     */
    public double calculateLength(Position fromPoint, Position toPoint) {
        Objects.requireNonNull(fromPoint, "From point cannot be null");
        Objects.requireNonNull(toPoint, "To point cannot be null");

        if (type == SegmentType.LINEAR) {
            return BezierMath.distance(fromPoint, toPoint);
        } else {
            return BezierMath.calculateArcLength(fromPoint, c1, c2, toPoint);
        }
    }

    /**
     * Calculate tangent angle at the start of this segment.
     * <p>
     * LINEAR: angle from from to to.
     * BEZIER: tangent at t=0.
     * <p>
     * Returns angle in radians, range [-π, π] (REQ-TC-004).
     *
     * @param fromPoint start position (resolved from points by from ID)
     * @param toPoint end position (resolved from points by to ID)
     * @return angle in radians, range [-π, π]
     */
    public double calculateStartAngle(Position fromPoint, Position toPoint) {
        Objects.requireNonNull(fromPoint, "From point cannot be null");
        Objects.requireNonNull(toPoint, "To point cannot be null");

        if (type == SegmentType.LINEAR) {
            double dx = toPoint.x() - fromPoint.x();
            double dy = toPoint.y() - fromPoint.y();
            return Math.atan2(dy, dx);
        } else {
            return BezierMath.calculateTangentAngle(fromPoint, c1, c2, toPoint, 0.0);
        }
    }

    /**
     * Calculate tangent angle at the end of this segment.
     * <p>
     * LINEAR: angle from from to to (same as start angle).
     * BEZIER: tangent at t=1.
     * <p>
     * Returns angle in radians, range [-π, π] (REQ-TC-004).
     *
     * @param fromPoint start position (resolved from points by from ID)
     * @param toPoint end position (resolved from points by to ID)
     * @return angle in radians, range [-π, π]
     */
    public double calculateEndAngle(Position fromPoint, Position toPoint) {
        Objects.requireNonNull(fromPoint, "From point cannot be null");
        Objects.requireNonNull(toPoint, "To point cannot be null");

        if (type == SegmentType.LINEAR) {
            double dx = toPoint.x() - fromPoint.x();
            double dy = toPoint.y() - fromPoint.y();
            return Math.atan2(dy, dx);
        } else {
            return BezierMath.calculateTangentAngle(fromPoint, c1, c2, toPoint, 1.0);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathSegment that = (PathSegment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PathSegment{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                (hasControlPoints() ? ", c1=" + c1 + ", c2=" + c2 : "") +
                '}';
    }
}
