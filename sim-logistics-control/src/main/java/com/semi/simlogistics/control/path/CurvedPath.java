package com.semi.simlogistics.control.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Curved path with points and segments (REQ-TC-003, REQ-TC-012).
 * <p>
 * Represents a path composed of:
 * - points: collection of PathPoint (id + position)
 * - segments: ordered list of PathSegment (LINEAR or BEZIER)
 * <p>
 * Validation rules:
 * - LINEAR segments must have only from/to (no c1/c2)
 * - BEZIER segments must have from/to/c1/c2
 * - from/to must reference existing point IDs in points
 * - segments must be continuous (previous segment's 'to' = current segment's 'from')
 * - segments are validated at construction time
 * <p>
 * All coordinates in meters (REQ-TC-004).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class CurvedPath {

    private final String id;
    private final Map<String, PathPoint> points;
    private final List<PathSegment> segments;
    private final double totalLength;

    /**
     * Create a curved path.
     *
     * @param id path ID
     * @param points collection of path points (will be copied)
     * @param segments ordered list of path segments (will be copied and validated)
     * @throws IllegalArgumentException if validation fails
     */
    public CurvedPath(String id, List<PathPoint> points, List<PathSegment> segments) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Path ID cannot be null or empty");
        }
        this.id = id;

        // Copy points to map for ID lookup
        this.points = new LinkedHashMap<>();
        if (points != null) {
            for (PathPoint point : points) {
                if (this.points.containsKey(point.getId())) {
                    throw new IllegalArgumentException("Duplicate point ID: " + point.getId());
                }
                this.points.put(point.getId(), point);
            }
        }

        // Copy and validate segments
        this.segments = new ArrayList<>();
        if (segments != null) {
            for (int i = 0; i < segments.size(); i++) {
                PathSegment segment = segments.get(i);
                validateSegment(segment);

                // Validate segment continuity: previous segment's 'to' must equal current segment's 'from'
                if (i > 0) {
                    PathSegment prevSegment = this.segments.get(i - 1);
                    if (!prevSegment.getTo().equals(segment.getFrom())) {
                        throw new IllegalArgumentException(
                                "Segment '" + segment.getId() + "' is not continuous. " +
                                "Previous segment '" + prevSegment.getId() + "' ends at '" + prevSegment.getTo() + "', " +
                                "but segment '" + segment.getId() + "' starts at '" + segment.getFrom() + "'");
                    }
                }

                this.segments.add(segment);
            }
        }

        // Calculate total length
        this.totalLength = calculateTotalLength();
    }

    /**
     * Validate a segment against this path's points.
     *
     * @param segment segment to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateSegment(PathSegment segment) {
        Objects.requireNonNull(segment, "Segment cannot be null");

        // Check from/to references exist
        if (!points.containsKey(segment.getFrom())) {
            throw new IllegalArgumentException(
                    "Segment '" + segment.getId() + "' references unknown from point: " + segment.getFrom());
        }
        if (!points.containsKey(segment.getTo())) {
            throw new IllegalArgumentException(
                    "Segment '" + segment.getId() + "' references unknown to point: " + segment.getTo());
        }

        // Validate segment structure matches type
        if (segment.getType() == SegmentType.LINEAR && segment.hasControlPoints()) {
            throw new IllegalArgumentException(
                    "LINEAR segment '" + segment.getId() + "' must not have control points");
        }
        if (segment.getType() == SegmentType.BEZIER && !segment.hasControlPoints()) {
            throw new IllegalArgumentException(
                    "BEZIER segment '" + segment.getId() + "' must have control points c1 and c2");
        }
    }

    /**
     * Calculate total path length by summing segment lengths.
     *
     * @return total length in meters
     */
    private double calculateTotalLength() {
        double length = 0.0;
        for (PathSegment segment : segments) {
            PathPoint fromPoint = points.get(segment.getFrom());
            PathPoint toPoint = points.get(segment.getTo());
            length += segment.calculateLength(fromPoint.getPosition(), toPoint.getPosition());
        }
        return length;
    }

    /**
     * Get path ID.
     *
     * @return path ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get path points (unmodifiable view).
     *
     * @return unmodifiable collection of points
     */
    public List<PathPoint> getPoints() {
        return Collections.unmodifiableList(new ArrayList<>(points.values()));
    }

    /**
     * Get a point by ID.
     *
     * @param pointId point ID
     * @return point, or null if not found
     */
    public PathPoint getPoint(String pointId) {
        return points.get(pointId);
    }

    /**
     * Get path segments (unmodifiable view).
     *
     * @return unmodifiable list of segments in order
     */
    public List<PathSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Get total path length.
     *
     * @return total length in meters
     */
    public double getTotalLength() {
        return totalLength;
    }

    /**
     * Get number of points.
     *
     * @return point count
     */
    public int getPointCount() {
        return points.size();
    }

    /**
     * Get number of segments.
     *
     * @return segment count
     */
    public int getSegmentCount() {
        return segments.size();
    }

    /**
     * Check if path is empty.
     *
     * @return true if path has no segments
     */
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    @Override
    public String toString() {
        return "CurvedPath{" +
                "id='" + id + '\'' +
                ", points=" + points.size() +
                ", segments=" + segments.size() +
                ", totalLength=" + String.format("%.2f", totalLength) + "m" +
                '}';
    }
}
