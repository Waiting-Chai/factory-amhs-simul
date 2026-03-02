package com.semi.simlogistics.movement;

import com.semi.simlogistics.core.Position;

/**
 * Track movement logic for OHT vehicles along Bezier curve paths.
 * <p>
 * Supports both linear and cubic Bezier curve tracks.
 * Linear tracks have no control points, while Bezier tracks use two control points
 * to define a smooth curve between start and end positions.
 * <p>
 * Coordinate units: meters (m)
 * Angle units: radians [-π, π]
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class TrackMovement {

    private final Position start;
    private final Position end;
    private final Position control1;
    private final Position control2;
    private final boolean linear;
    private Double cachedTotalDistance;

    /**
     * Create a track movement.
     * <p>
     * If control points are null, the track is linear (straight line).
     * If control points are provided, the track is a cubic Bezier curve.
     * <p>
     * Control points must be either both null (linear track) or both non-null (Bezier track).
     * Mixed cases (one null, one non-null) will throw IllegalArgumentException.
     *
     * @param start starting position
     * @param end ending position
     * @param control1 first control point (null for linear track)
     * @param control2 second control point (null for linear track)
     * @throws IllegalArgumentException if control points are mixed (one null, one non-null)
     */
    public TrackMovement(Position start, Position end, Position control1, Position control2) {
        this.start = start;
        this.end = end;

        // Validate control points: must be both null (linear) or both non-null (Bezier)
        if ((control1 == null) != (control2 == null)) {
            throw new IllegalArgumentException(
                "Control points must be both null (linear track) or both non-null (Bezier track). " +
                "Got: control1=" + (control1 == null ? "null" : "non-null") +
                ", control2=" + (control2 == null ? "null" : "non-null"));
        }

        this.control1 = control1;
        this.control2 = control2;
        this.linear = (control1 == null) && (control2 == null);
    }

    /**
     * Get position at normalized progress t.
     * <p>
     * t must be in range [0, 1], where 0 is start and 1 is end.
     *
     * @param t normalized progress [0, 1]
     * @return position at progress t
     * @throws IllegalArgumentException if t is not in [0, 1]
     */
    public Position getPositionAt(double t) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Progress t must be in [0, 1], got: " + t);
        }

        if (linear) {
            return getLinearPositionAt(t);
        } else {
            return getBezierPositionAt(t);
        }
    }

    /**
     * Get position at distance along track.
     * <p>
     * Distance must be in range [0, totalDistance].
     *
     * @param distance distance from start (meters)
     * @return position at distance
     * @throws IllegalArgumentException if distance is not valid
     */
    public Position getPositionAtDistance(double distance) {
        double totalDistance = getTotalDistance();
        if (distance < 0.0 || distance > totalDistance) {
            throw new IllegalArgumentException(
                    "Distance must be in [0, " + totalDistance + "], got: " + distance);
        }

        double t = distance / totalDistance;
        return getPositionAt(t);
    }

    /**
     * Get total track length (arc length).
     * <p>
     * For linear tracks, this is the Euclidean distance.
     * For Bezier tracks, this is approximated using numerical integration.
     *
     * @return total distance in meters
     */
    public double getTotalDistance() {
        if (cachedTotalDistance == null) {
            if (linear) {
                cachedTotalDistance = start.distanceTo(end);
            } else {
                cachedTotalDistance = calculateBezierLength();
            }
        }
        return cachedTotalDistance;
    }

    /**
     * Get direction (heading) at normalized progress t.
     * <p>
     * Direction is the tangent angle in radians, measured from +X axis,
     * in range [-π, π]. Positive is counter-clockwise.
     *
     * @param t normalized progress [0, 1]
     * @return direction angle in radians [-π, π]
     * @throws IllegalArgumentException if t is not in [0, 1]
     */
    public double getDirectionAt(double t) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Progress t must be in [0, 1], got: " + t);
        }

        if (linear) {
            return getLinearDirection();
        } else {
            return getBezierDirectionAt(t);
        }
    }

    /**
     * Check if this is a linear (straight line) track.
     *
     * @return true if linear, false if Bezier curve
     */
    public boolean isLinear() {
        return linear;
    }

    /**
     * Get start position.
     *
     * @return start position
     */
    public Position getStart() {
        return start;
    }

    /**
     * Get end position.
     *
     * @return end position
     */
    public Position getEnd() {
        return end;
    }

    /**
     * Get first control point (for Bezier tracks).
     *
     * @return first control point, or null if linear track
     */
    public Position getControl1() {
        return control1;
    }

    /**
     * Get second control point (for Bezier tracks).
     *
     * @return second control point, or null if linear track
     */
    public Position getControl2() {
        return control2;
    }

    /**
     * Calculate position on linear track at progress t.
     */
    private Position getLinearPositionAt(double t) {
        double x = start.x() + t * (end.x() - start.x());
        double y = start.y() + t * (end.y() - start.y());
        double z = start.z() + t * (end.z() - start.z());
        return new Position(x, y, z);
    }

    /**
     * Calculate direction of linear track.
     */
    private double getLinearDirection() {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        return Math.atan2(dy, dx);
    }

    /**
     * Calculate position on cubic Bezier curve at progress t.
     * <p>
     * Cubic Bezier formula:
     * B(t) = (1-t)³*P0 + 3*(1-t)²*t*P1 + 3*(1-t)*t²*P2 + t³*P3
     */
    private Position getBezierPositionAt(double t) {
        double mt = 1.0 - t;
        double mt2 = mt * mt;
        double mt3 = mt2 * mt;
        double t2 = t * t;
        double t3 = t2 * t;

        // Calculate each coordinate using cubic Bezier formula
        double x = mt3 * start.x() +
                   3.0 * mt2 * t * control1.x() +
                   3.0 * mt * t2 * control2.x() +
                   t3 * end.x();

        double y = mt3 * start.y() +
                   3.0 * mt2 * t * control1.y() +
                   3.0 * mt * t2 * control2.y() +
                   t3 * end.y();

        double z = mt3 * start.z() +
                   3.0 * mt2 * t * control1.z() +
                   3.0 * mt * t2 * control2.z() +
                   t3 * end.z();

        return new Position(x, y, z);
    }

    /**
     * Calculate Bezier curve arc length using numerical integration.
     * <p>
     * Approximates the integral of sqrt((dx/dt)² + (dy/dt)² + (dz/dt)²) from t=0 to t=1.
     * Uses Gaussian quadrature with 20 segments for accuracy.
     */
    private double calculateBezierLength() {
        final int segments = 20;
        double length = 0.0;
        Position prev = getBezierPositionAt(0.0);

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Position curr = getBezierPositionAt(t);
            length += prev.distanceTo(curr);
            prev = curr;
        }

        return length;
    }

    /**
     * Calculate direction (tangent angle) on Bezier curve at progress t.
     * <p>
     * Direction is calculated by differentiating the Bezier curve:
     * B'(t) = 3*(1-t)²*(P1-P0) + 6*(1-t)*t*(P2-P1) + 3*t²*(P3-P2)
     */
    private double getBezierDirectionAt(double t) {
        double mt = 1.0 - t;

        // Derivative of cubic Bezier (tangent vector)
        double dx = 3.0 * mt * mt * (control1.x() - start.x()) +
                    6.0 * mt * t * (control2.x() - control1.x()) +
                    3.0 * t * t * (end.x() - control2.x());

        double dy = 3.0 * mt * mt * (control1.y() - start.y()) +
                    6.0 * mt * t * (control2.y() - control1.y()) +
                    3.0 * t * t * (end.y() - control2.y());

        // Calculate direction from tangent vector
        return Math.atan2(dy, dx);
    }

    @Override
    public String toString() {
        if (linear) {
            return "TrackMovement{linear from " + start + " to " + end + "}";
        } else {
            return "TrackMovement{bezier from " + start + " to " + end +
                   ", c1=" + control1 + ", c2=" + control2 + "}";
        }
    }
}
