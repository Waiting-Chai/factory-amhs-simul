package com.semi.simlogistics.control.path;

import com.semi.simlogistics.core.Position;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Bezier segment structure (REQ-TC-003).
 * <p>
 * Tests path segment types (LINEAR vs BEZIER) and structure.
 * BEZIER segments contain: from, to, c1, c2 (c1/c2 are absolute coordinates in meters).
 * LINEAR segments contain: from, to only.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class BezierSegmentTest {

    @Test
    void testPathSegmentTypeLinear() {
        // Given: A LINEAR segment type
        SegmentType type = SegmentType.LINEAR;

        // When: Checking segment type
        String segmentType = type.name();

        // Then: Should be recognized as LINEAR
        assertThat(segmentType).isEqualTo("LINEAR");
    }

    @Test
    void testPathSegmentTypeBezier() {
        // Given: A BEZIER segment type
        SegmentType type = SegmentType.BEZIER;

        // When: Checking segment type
        String segmentType = type.name();

        // Then: Should be recognized as BEZIER
        assertThat(segmentType).isEqualTo("BEZIER");
    }

    @Test
    void testBezierSegmentStructure() {
        // Given: A BEZIER segment
        String fromPointId = "P1";
        String toPointId = "P2";

        // Control points (absolute coordinates in meters)
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);

        // When: Creating BEZIER segment
        PathSegment segment = new PathSegment("SEG-1", fromPointId, toPointId, c1, c2);

        // Then: Should contain all required fields
        assertThat(segment.getId()).isEqualTo("SEG-1");
        assertThat(segment.getType()).isEqualTo(SegmentType.BEZIER);
        assertThat(segment.getFrom()).isEqualTo("P1");
        assertThat(segment.getTo()).isEqualTo("P2");
        assertThat(segment.getC1()).isEqualTo(new Position(25.0, 0.0, 10.0));
        assertThat(segment.getC2()).isEqualTo(new Position(75.0, 0.0, 10.0));
        assertThat(segment.hasControlPoints()).isTrue();
    }

    @Test
    void testLinearSegmentStructure() {
        // Given: A LINEAR segment
        String fromPointId = "P1";
        String toPointId = "P2";

        // When: Creating LINEAR segment
        PathSegment segment = new PathSegment("SEG-1", fromPointId, toPointId);

        // Then: Should contain only from and to fields
        assertThat(segment.getId()).isEqualTo("SEG-1");
        assertThat(segment.getType()).isEqualTo(SegmentType.LINEAR);
        assertThat(segment.getFrom()).isEqualTo("P1");
        assertThat(segment.getTo()).isEqualTo("P2");
        assertThat(segment.hasControlPoints()).isFalse();
        assertThat(segment.getC1()).isNull();
        assertThat(segment.getC2()).isNull();
    }

    @Test
    void testBezierControlPointsAreAbsoluteCoordinates() {
        // Given: BEZIER segment control points in meters (REQ-TC-003, REQ-TC-004)
        Position c1 = new Position(10.5, 20.3, 5.0);
        Position c2 = new Position(50.7, 60.2, 8.0);

        // When: Creating BEZIER segment
        PathSegment segment = new PathSegment("SEG-1", "P1", "P2", c1, c2);

        // Then: Control points should be stored as absolute coordinates in meters
        assertThat(segment.getC1().x()).isEqualTo(10.5);
        assertThat(segment.getC1().y()).isEqualTo(20.3);
        assertThat(segment.getC1().z()).isEqualTo(5.0);
        assertThat(segment.getC2().x()).isEqualTo(50.7);
        assertThat(segment.getC2().y()).isEqualTo(60.2);
        assertThat(segment.getC2().z()).isEqualTo(8.0);
    }

    @Test
    void testBezierSegmentLengthCalculation() {
        // Given: A BEZIER curve
        Position from = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);
        Position to = new Position(100.0, 0.0, 0.0);

        PathSegment segment = new PathSegment("SEG-1", "P1", "P2", c1, c2);

        // When: Calculating curve length
        double length = segment.calculateLength(from, to);

        // Then: Length should be greater than straight line distance
        double straightDistance = BezierMath.distance(from, to);
        assertThat(length).isGreaterThan(straightDistance);
        // For this curve, arc length should be around 105-110 meters
        assertThat(length).isGreaterThan(100.0);
        assertThat(length).isLessThan(120.0);
    }

    @Test
    void testLinearSegmentLengthCalculation() {
        // Given: A LINEAR segment
        Position from = new Position(0.0, 0.0, 0.0);
        Position to = new Position(100.0, 0.0, 0.0);

        PathSegment segment = new PathSegment("SEG-1", "P1", "P2");

        // When: Calculating length
        double length = segment.calculateLength(from, to);

        // Then: Should equal Euclidean distance
        assertThat(length).isEqualTo(100.0);
    }

    @Test
    void testPathWithMultipleSegments() {
        // Given: Points for a path with multiple segments
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P2", new Position(50.0, 0.0, 0.0)),
                new PathPoint("P3", new Position(100.0, 0.0, 0.0)),
                new PathPoint("P4", new Position(150.0, 0.0, 0.0))
        );

        // Segment 1: LINEAR from P1 to P2
        PathSegment seg1 = new PathSegment("SEG-1", "P1", "P2");

        // Segment 2: BEZIER from P2 to P3
        Position c1 = new Position(65.0, 10.0, 5.0);
        Position c2 = new Position(85.0, 10.0, 5.0);
        PathSegment seg2 = new PathSegment("SEG-2", "P2", "P3", c1, c2);

        // Segment 3: LINEAR from P3 to P4
        PathSegment seg3 = new PathSegment("SEG-3", "P3", "P4");

        List<PathSegment> segments = Arrays.asList(seg1, seg2, seg3);

        // When: Creating curved path
        CurvedPath path = new CurvedPath("PATH-1", points, segments);

        // Then: All segments should be stored correctly
        assertThat(path.getSegments()).hasSize(3);
        assertThat(path.getPoints()).hasSize(4);
        assertThat(path.getSegmentCount()).isEqualTo(3);
        assertThat(path.getPointCount()).isEqualTo(4);

        // Total length should be sum of all segments
        assertThat(path.getTotalLength()).isGreaterThan(0);
        // 50 (linear) + ~53 (bezier arc) + 50 (linear) ≈ 153
        assertThat(path.getTotalLength()).isGreaterThan(140.0);
        assertThat(path.getTotalLength()).isLessThan(170.0);
    }

    @Test
    void testBezierControlPointsNotSameAsTrafficControlPoints() {
        // Given: BEZIER control points are geometric only (REQ-TC-003)
        PathSegment segment = new PathSegment(
                "SEG-1",
                "P1",
                "P2",
                new Position(25.0, 0.0, 10.0),
                new Position(75.0, 0.0, 10.0)
        );

        // Then: Control points are geometric, not traffic control
        assertThat(segment.hasControlPoints()).isTrue();
        assertThat(segment.getC1()).isNotNull();
        assertThat(segment.getC2()).isNotNull();

        // Traffic control points are separate (ControlPoint class in traffic package)
        // This test documents the conceptual distinction
    }

    @Test
    void testBezierSegmentStartAndEndAngles() {
        // Given: A BEZIER curve (REQ-TC-004: angles in radians, range [-π, π])
        Position from = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 25.0, 0.0);
        Position c2 = new Position(75.0, 25.0, 0.0);
        Position to = new Position(100.0, 0.0, 0.0);

        PathSegment segment = new PathSegment("SEG-1", "P1", "P2", c1, c2);

        // When: Calculating start and end angles
        double startAngle = segment.calculateStartAngle(from, to);
        double endAngle = segment.calculateEndAngle(from, to);

        // Then: Angles should be in radians and range [-π, π]
        assertThat(startAngle).isGreaterThan(-Math.PI);
        assertThat(startAngle).isLessThan(Math.PI);
        assertThat(endAngle).isGreaterThan(-Math.PI);
        assertThat(endAngle).isLessThan(Math.PI);

        // Start angle should be positive (curve goes up initially)
        assertThat(startAngle).isGreaterThan(0.0);
        // End angle should be negative (curve comes down at end)
        assertThat(endAngle).isLessThan(0.0);
    }

    @Test
    void testLinearSegmentStartAndEndAngles() {
        // Given: A LINEAR segment
        Position from = new Position(0.0, 0.0, 0.0);
        Position to = new Position(100.0, 100.0, 0.0);

        PathSegment segment = new PathSegment("SEG-1", "P1", "P2");

        // When: Calculating start and end angles
        double startAngle = segment.calculateStartAngle(from, to);
        double endAngle = segment.calculateEndAngle(from, to);

        // Then: Both should be π/4 (45 degrees) for diagonal line
        assertThat(startAngle).isEqualTo(Math.PI / 4);
        assertThat(endAngle).isEqualTo(Math.PI / 4);
    }

    @Test
    void testCurvedPathValidationFromPointNotFound() {
        // Given: Points collection
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0))
        );

        // And: A segment referencing non-existent from point
        PathSegment segment = new PathSegment("SEG-1", "P_NOT_EXISTS", "P1");

        // When: Creating curved path
        // Then: Should throw validation exception
        List<PathSegment> segments = Arrays.asList(segment);
        assertThatThrownBy(() -> new CurvedPath("PATH-1", points, segments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown from point");
    }

    @Test
    void testCurvedPathValidationToPointNotFound() {
        // Given: Points collection
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0))
        );

        // And: A segment referencing non-existent to point
        PathSegment segment = new PathSegment("SEG-1", "P1", "P_NOT_EXISTS");

        // When: Creating curved path
        // Then: Should throw validation exception
        List<PathSegment> segments = Arrays.asList(segment);
        assertThatThrownBy(() -> new CurvedPath("PATH-1", points, segments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown to point");
    }

    @Test
    void testCurvedPathValidationDuplicatePointIds() {
        // Given: Points with duplicate IDs
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P1", new Position(10.0, 0.0, 0.0))  // Duplicate!
        );

        // When: Creating curved path
        // Then: Should throw validation exception
        assertThatThrownBy(() -> new CurvedPath("PATH-1", points, new ArrayList<>()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate point ID");
    }

    @Test
    void testBezierPointOnCurve() {
        // Given: A BEZIER curve
        Position p0 = new Position(0.0, 0.0, 0.0);
        Position p1 = new Position(25.0, 25.0, 0.0);
        Position p2 = new Position(75.0, 25.0, 0.0);
        Position p3 = new Position(100.0, 0.0, 0.0);

        // When: Calculating point at t=0
        Position atT0 = BezierMath.calculatePoint(p0, p1, p2, p3, 0.0);
        // Then: Should equal start point
        assertThat(atT0.x()).isEqualTo(0.0);
        assertThat(atT0.y()).isEqualTo(0.0);

        // When: Calculating point at t=1
        Position atT1 = BezierMath.calculatePoint(p0, p1, p2, p3, 1.0);
        // Then: Should equal end point
        assertThat(atT1.x()).isEqualTo(100.0);
        assertThat(atT1.y()).isEqualTo(0.0);

        // When: Calculating point at t=0.5 (midpoint)
        Position atT05 = BezierMath.calculatePoint(p0, p1, p2, p3, 0.5);
        // Then: Should be somewhere in between
        assertThat(atT05.x()).isGreaterThan(0.0);
        assertThat(atT05.x()).isLessThan(100.0);
        assertThat(atT05.y()).isGreaterThan(0.0);  // Curve goes up
    }

    @Test
    void testBezierMathParameterValidation() {
        // Given: Bezier curve points
        Position p0 = new Position(0.0, 0.0, 0.0);
        Position p1 = new Position(25.0, 0.0, 0.0);
        Position p2 = new Position(75.0, 0.0, 0.0);
        Position p3 = new Position(100.0, 0.0, 0.0);

        // When: Calculating with t < 0
        // Then: Should throw exception
        assertThatThrownBy(() -> BezierMath.calculatePoint(p0, p1, p2, p3, -0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be in [0, 1]");

        // When: Calculating with t > 1
        // Then: Should throw exception
        assertThatThrownBy(() -> BezierMath.calculatePoint(p0, p1, p2, p3, 1.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be in [0, 1]");
    }

    @Test
    void testCurvedPathGetPointById() {
        // Given: A curved path
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P2", new Position(50.0, 0.0, 0.0)),
                new PathPoint("P3", new Position(100.0, 0.0, 0.0))
        );
        List<PathSegment> segments = Arrays.asList(
                new PathSegment("SEG-1", "P1", "P2"),
                new PathSegment("SEG-2", "P2", "P3")
        );

        CurvedPath path = new CurvedPath("PATH-1", points, segments);

        // When: Getting point by ID
        PathPoint p1 = path.getPoint("P1");
        PathPoint p2 = path.getPoint("P2");

        // Then: Should return correct points
        assertThat(p1).isNotNull();
        assertThat(p1.getId()).isEqualTo("P1");
        assertThat(p1.getPosition()).isEqualTo(new Position(0.0, 0.0, 0.0));

        assertThat(p2).isNotNull();
        assertThat(p2.getId()).isEqualTo("P2");
        assertThat(p2.getPosition()).isEqualTo(new Position(50.0, 0.0, 0.0));

        // When: Getting non-existent point
        PathPoint notFound = path.getPoint("P_NOT_EXISTS");
        // Then: Should return null
        assertThat(notFound).isNull();
    }

    @Test
    void testMixedSegmentTypesTotalLength() {
        // Given: A path with mixed LINEAR and BEZIER segments
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P2", new Position(30.0, 0.0, 0.0)),
                new PathPoint("P3", new Position(60.0, 0.0, 0.0)),
                new PathPoint("P4", new Position(90.0, 0.0, 0.0))
        );

        List<PathSegment> segments = Arrays.asList(
                new PathSegment("SEG-1", "P1", "P2"),  // LINEAR: 30m
                new PathSegment("SEG-2", "P2", "P3", new Position(40.0, 10.0, 0.0), new Position(50.0, 10.0, 0.0)),  // BEZIER: ~32m
                new PathSegment("SEG-3", "P3", "P4")   // LINEAR: 30m
        );

        // When: Creating path
        CurvedPath path = new CurvedPath("PATH-1", points, segments);

        // Then: Total length should be sum of all segments
        assertThat(path.getTotalLength()).isGreaterThan(85.0);  // 30 + 32 + 30 = 92
        assertThat(path.getTotalLength()).isLessThan(100.0);
    }

    // ==================== Review Issue #2: Segment Continuity Validation ====================

    @Test
    void testCurvedPathSegmentContinuitySuccess() {
        // Given: Points for a continuous path
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P2", new Position(50.0, 0.0, 0.0)),
                new PathPoint("P3", new Position(100.0, 0.0, 0.0))
        );

        // And: Continuous segments (P1->P2, P2->P3)
        List<PathSegment> segments = Arrays.asList(
                new PathSegment("SEG-1", "P1", "P2"),
                new PathSegment("SEG-2", "P2", "P3")
        );

        // When: Creating path with continuous segments
        CurvedPath path = new CurvedPath("PATH-1", points, segments);

        // Then: Path should be created successfully
        assertThat(path.getSegmentCount()).isEqualTo(2);
    }

    @Test
    void testCurvedPathSegmentContinuityFailure() {
        // Given: Points
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P2", new Position(50.0, 0.0, 0.0)),
                new PathPoint("P3", new Position(100.0, 0.0, 0.0)),
                new PathPoint("P4", new Position(150.0, 0.0, 0.0))
        );

        // And: Discontinuous segments (P1->P2, then P3->P4, skipping P2->P3)
        List<PathSegment> segments = Arrays.asList(
                new PathSegment("SEG-1", "P1", "P2"),
                new PathSegment("SEG-2", "P3", "P4")  // Does NOT connect to SEG-1's end (P2)
        );

        // When: Creating path with discontinuous segments
        // Then: Should throw exception with clear error message
        assertThatThrownBy(() -> new CurvedPath("PATH-1", points, segments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not continuous")
                .hasMessageContaining("SEG-2")
                .hasMessageContaining("ends at 'P2'")
                .hasMessageContaining("starts at 'P3'");
    }

    @Test
    void testCurvedPathMixedSegmentTypeContinuity() {
        // Given: Points
        List<PathPoint> points = Arrays.asList(
                new PathPoint("P1", new Position(0.0, 0.0, 0.0)),
                new PathPoint("P2", new Position(50.0, 0.0, 0.0)),
                new PathPoint("P3", new Position(100.0, 0.0, 0.0))
        );

        // And: Mixed segment types but continuous
        List<PathSegment> segments = Arrays.asList(
                new PathSegment("SEG-1", "P1", "P2"),  // LINEAR
                new PathSegment("SEG-2", "P2", "P3", new Position(60.0, 10.0, 0.0), new Position(80.0, 10.0, 0.0))  // BEZIER
        );

        // When: Creating path
        CurvedPath path = new CurvedPath("PATH-1", points, segments);

        // Then: Should validate successfully (continuity holds across different segment types)
        assertThat(path.getSegmentCount()).isEqualTo(2);
    }

    // ==================== Review Issue #3: Bezier Degenerate Curve Protection ====================

    @Test
    void testBezierDegenerateCurveAllPointsCoincident() {
        // Given: A degenerate curve where all points are the same (zero derivative everywhere)
        Position p = new Position(10.0, 20.0, 30.0);
        // p0, p1, p2, p3 all at same position

        // When: Calculating tangent angle
        // Then: Should throw exception (degenerate curve)
        assertThatThrownBy(() -> BezierMath.calculateTangentAngle(p, p, p, p, 0.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Degenerate Bezier curve")
                .hasMessageContaining("zero derivative")
                .hasMessageContaining("All control points may be coincident");
    }

    @Test
    void testBezierDegenerateCurveAtStart() {
        // Given: Curve with zero derivative at t=0 (p0 = p1)
        Position p0 = new Position(0.0, 0.0, 0.0);
        Position p1 = new Position(0.0, 0.0, 0.0);  // Same as p0
        Position p2 = new Position(100.0, 0.0, 0.0);
        Position p3 = new Position(100.0, 0.0, 0.0);

        // When: Calculating tangent angle at t=0 (where derivative is zero)
        // Then: Should use neighborhood sampling and still return valid angle
        double angle = BezierMath.calculateTangentAngle(p0, p1, p2, p3, 0.0);
        // Angle should be close to 0 (horizontal line to the right)
        assertThat(Math.abs(angle)).isLessThan(0.01);
    }

    @Test
    void testBezierDegenerateCurveAtEnd() {
        // Given: Curve with zero derivative at t=1 (p2 = p3)
        Position p0 = new Position(0.0, 0.0, 0.0);
        Position p1 = new Position(0.0, 0.0, 0.0);
        Position p2 = new Position(100.0, 0.0, 0.0);
        Position p3 = new Position(100.0, 0.0, 0.0);  // Same as p2

        // When: Calculating tangent angle at t=1 (where derivative is zero)
        // Then: Should use neighborhood sampling and still return valid angle
        double angle = BezierMath.calculateTangentAngle(p0, p1, p2, p3, 1.0);
        // Angle should be close to 0 (horizontal line to the right)
        assertThat(Math.abs(angle)).isLessThan(0.01);
    }

    @Test
    void testBezierNonDegenerateCurve() {
        // Given: A normal non-degenerate Bezier curve
        Position p0 = new Position(0.0, 0.0, 0.0);
        Position p1 = new Position(25.0, 25.0, 0.0);
        Position p2 = new Position(75.0, 25.0, 0.0);
        Position p3 = new Position(100.0, 0.0, 0.0);

        // When: Calculating tangent angle at various points
        double angleStart = BezierMath.calculateTangentAngle(p0, p1, p2, p3, 0.0);
        double angleMid = BezierMath.calculateTangentAngle(p0, p1, p2, p3, 0.5);
        double angleEnd = BezierMath.calculateTangentAngle(p0, p1, p2, p3, 1.0);

        // Then: All angles should be in valid range [-π, π]
        assertThat(angleStart).isGreaterThan(-Math.PI);
        assertThat(angleStart).isLessThan(Math.PI);
        assertThat(angleMid).isGreaterThan(-Math.PI);
        assertThat(angleMid).isLessThan(Math.PI);
        assertThat(angleEnd).isGreaterThan(-Math.PI);
        assertThat(angleEnd).isLessThan(Math.PI);
    }
}
