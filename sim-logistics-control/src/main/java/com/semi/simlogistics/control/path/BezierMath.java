package com.semi.simlogistics.control.path;

import com.semi.simlogistics.core.Position;

/**
 * Bezier curve geometry and math utilities (REQ-TC-003).
 * <p>
 * Provides cubic Bezier curve calculations for:
 * - Point on curve at parameter t
 * - Tangent (direction) at parameter t
 * - Arc length calculation
 * <p>
 * All coordinates in meters. All angles in radians, range [-π, π] (REQ-TC-004).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public final class BezierMath {

    private BezierMath() {
        // Utility class
    }

    /**
     * Calculate point on cubic Bezier curve at parameter t.
     * <p>
     * Cubic Bezier formula:
     * B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3
     *
     * @param p0 start point (from)
     * @param p1 control point 1 (c1)
     * @param p2 control point 2 (c2)
     * @param p3 end point (to)
     * @param t parameter [0, 1]
     * @return position on curve at parameter t
     * @throws IllegalArgumentException if t is not in [0, 1]
     */
    public static Position calculatePoint(Position p0, Position p1, Position p2, Position p3, double t) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Parameter t must be in [0, 1], got: " + t);
        }

        double mt = 1.0 - t;
        double mt2 = mt * mt;
        double mt3 = mt2 * mt;
        double t2 = t * t;
        double t3 = t2 * t;

        double x = mt3 * p0.x() + 3 * mt2 * t * p1.x() + 3 * mt * t2 * p2.x() + t3 * p3.x();
        double y = mt3 * p0.y() + 3 * mt2 * t * p1.y() + 3 * mt * t2 * p2.y() + t3 * p3.y();
        double z = mt3 * p0.z() + 3 * mt2 * t * p1.z() + 3 * mt * t2 * p2.z() + t3 * p3.z();

        return new Position(x, y, z);
    }

    /**
     * Calculate tangent (first derivative) at parameter t.
     * <p>
     * Cubic Bezier derivative:
     * B'(t) = 3(1-t)²(P1-P0) + 6(1-t)t(P2-P1) + 3t²(P3-P2)
     * <p>
     * Returns angle in radians, range [-π, π] (REQ-TC-004).
     * <p>
     * Handles degenerate curves (where derivative is zero) by using neighborhood sampling.
     * If the curve is degenerate at t and in neighborhood, throws an exception.
     *
     * @param p0 start point (from)
     * @param p1 control point 1 (c1)
     * @param p2 control point 2 (c2)
     * @param p3 end point (to)
     * @param t parameter [0, 1]
     * @return angle in radians, range [-π, π]
     * @throws IllegalArgumentException if t is not in [0, 1]
     * @throws IllegalArgumentException if curve is degenerate (zero derivative) at t and neighborhood
     */
    public static double calculateTangentAngle(Position p0, Position p1, Position p2, Position p3, double t) {
        if (t < 0.0 || t > 1.0) {
            throw new IllegalArgumentException("Parameter t must be in [0, 1], got: " + t);
        }

        double mt = 1.0 - t;

        // First derivative (velocity vector)
        double dx = 3 * mt * mt * (p1.x() - p0.x())
                  + 6 * mt * t * (p2.x() - p1.x())
                  + 3 * t * t * (p3.x() - p2.x());

        double dy = 3 * mt * mt * (p1.y() - p0.y())
                  + 6 * mt * t * (p2.y() - p1.y())
                  + 3 * t * t * (p3.y() - p2.y());

        // Check for degenerate case (zero derivative)
        double derivativeMag = Math.sqrt(dx * dx + dy * dy);
        if (derivativeMag < 1e-10) {
            // Try neighborhood sampling to recover direction
            final double eps = 1e-6;
            double tSample = (t < 0.5) ? t + eps : t - eps;

            // Clamp to [0, 1]
            if (tSample < 0.0) tSample = 0.0;
            if (tSample > 1.0) tSample = 1.0;

            double mtSample = 1.0 - tSample;
            double dxSample = 3 * mtSample * mtSample * (p1.x() - p0.x())
                           + 6 * mtSample * tSample * (p2.x() - p1.x())
                           + 3 * tSample * tSample * (p3.x() - p2.x());

            double dySample = 3 * mtSample * mtSample * (p1.y() - p0.y())
                           + 6 * mtSample * tSample * (p2.y() - p1.y())
                           + 3 * tSample * tSample * (p3.y() - p2.y());

            double sampleMag = Math.sqrt(dxSample * dxSample + dySample * dySample);
            if (sampleMag < 1e-10) {
                throw new IllegalArgumentException(
                        "Degenerate Bezier curve: zero derivative at t=" + t + " and neighborhood. " +
                        "All control points may be coincident.");
            }

            // Use neighborhood direction
            return Math.atan2(dySample, dxSample);
        }

        // Use atan2 for angle in range [-π, π]
        return Math.atan2(dy, dx);
    }

    /**
     * Calculate arc length of cubic Bezier curve using numerical integration.
     * <p>
     * Uses subdivision method with fixed number of segments.
     * Arc length = ∫√((dx/dt)² + (dy/dt)² + (dz/dt)²) dt from 0 to 1
     *
     * @param p0 start point (from)
     * @param p1 control point 1 (c1)
     * @param p2 control point 2 (c2)
     * @param p3 end point (to)
     * @return arc length in meters
     */
    public static double calculateArcLength(Position p0, Position p1, Position p2, Position p3) {
        // Use 20 segments for good accuracy
        final int segments = 20;
        double length = 0.0;

        Position prev = p0;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Position curr = calculatePoint(p0, p1, p2, p3, t);
            length += distance(prev, curr);
            prev = curr;
        }

        return length;
    }

    /**
     * Calculate linear distance between two positions.
     *
     * @param a first position
     * @param b second position
     * @return distance in meters
     */
    public static double distance(Position a, Position b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double dz = b.z() - a.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
