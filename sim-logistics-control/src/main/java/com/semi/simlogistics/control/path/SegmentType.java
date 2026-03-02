package com.semi.simlogistics.control.path;

/**
 * Path segment type enum (REQ-TC-003).
 * <p>
 * Defines the type of path segment for routing and rendering.
 * LINEAR segments connect two points with a straight line.
 * BEZIER segments use cubic Bezier curves with two control points.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public enum SegmentType {

    /**
     * Linear segment (straight line between two points).
     * <p>
     * Structure: from, to (no control points)
     */
    LINEAR,

    /**
     * Bezier curve segment (cubic Bezier).
     * <p>
     * Structure: from, to, c1, c2 (two control points)
     * Control points are absolute coordinates in meters.
     */
    BEZIER
}
