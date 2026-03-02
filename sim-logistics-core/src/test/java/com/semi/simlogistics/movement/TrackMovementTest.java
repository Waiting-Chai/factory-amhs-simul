package com.semi.simlogistics.movement;

import com.semi.simlogistics.core.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for TrackMovement class.
 * <p>
 * Tests OHT vehicle movement along Bezier curve tracks.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class TrackMovementTest {

    private static final double EPSILON = 1e-6;

    @Test
    void testLinearTrackMovement() {
        // Given: A linear track from (0, 0, 0) to (100, 0, 0)
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Moving to 50% progress
        Position pos = movement.getPositionAt(0.5);

        // Then: Should be at midpoint
        assertThat(pos.x()).isCloseTo(50.0, within(EPSILON));
        assertThat(pos.y()).isCloseTo(0.0, within(EPSILON));
        assertThat(pos.z()).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    void testLinearTrackStartPoint() {
        // Given: A linear track
        Position start = new Position(10.0, 20.0, 30.0);
        Position end = new Position(100.0, 200.0, 300.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: At t=0
        Position pos = movement.getPositionAt(0.0);

        // Then: Should be at start
        assertThat(pos).isEqualTo(start);
    }

    @Test
    void testLinearTrackEndPoint() {
        // Given: A linear track
        Position start = new Position(10.0, 20.0, 30.0);
        Position end = new Position(100.0, 200.0, 300.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: At t=1
        Position pos = movement.getPositionAt(1.0);

        // Then: Should be at end
        assertThat(pos).isEqualTo(end);
    }

    @Test
    void testBezierTrackMovement() {
        // Given: A cubic Bezier track
        // Start: (0, 0, 0)
        // Control 1: (25, 0, 10)
        // Control 2: (75, 0, 10)
        // End: (100, 0, 0)
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: At t=0.5
        Position pos = movement.getPositionAt(0.5);

        // Then: Should follow Bezier curve
        // For cubic Bezier at t=0.5:
        // B(0.5) = (1-0.5)^3 * P0 + 3*(1-0.5)^2*0.5 * P1 + 3*(1-0.5)*0.5^2 * P2 + 0.5^3 * P3
        // = 0.125 * P0 + 0.375 * P1 + 0.375 * P2 + 0.125 * P3
        // x = 0.125*0 + 0.375*25 + 0.375*75 + 0.125*100 = 50
        // y = 0.125*0 + 0.375*0 + 0.375*0 + 0.125*0 = 0
        // z = 0.125*0 + 0.375*10 + 0.375*10 + 0.125*0 = 7.5
        assertThat(pos.x()).isCloseTo(50.0, within(EPSILON));
        assertThat(pos.y()).isCloseTo(0.0, within(EPSILON));
        assertThat(pos.z()).isCloseTo(7.5, within(EPSILON));
    }

    @Test
    void testBezierTrackStartPoint() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 10.0, 10.0);
        Position c2 = new Position(75.0, 20.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: At t=0
        Position pos = movement.getPositionAt(0.0);

        // Then: Should be at start
        assertThat(pos).isEqualTo(start);
    }

    @Test
    void testBezierTrackEndPoint() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 10.0, 10.0);
        Position c2 = new Position(75.0, 20.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: At t=1
        Position pos = movement.getPositionAt(1.0);

        // Then: Should be at end
        assertThat(pos).isEqualTo(end);
    }

    @Test
    void testBezierTrackQuarterPoint() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: At t=0.25
        Position pos = movement.getPositionAt(0.25);

        // Then: Should follow Bezier curve
        // B(0.25) = 0.421875*P0 + 0.421875*P1 + 0.140625*P2 + 0.015625*P3
        // x = 0.421875*0 + 0.421875*25 + 0.140625*75 + 0.015625*100 = 22.65625
        // z = 0.421875*0 + 0.421875*10 + 0.140625*10 + 0.015625*0 = 5.625
        assertThat(pos.x()).isCloseTo(22.65625, within(EPSILON));
        assertThat(pos.z()).isCloseTo(5.625, within(EPSILON));
    }

    @Test
    void testGetTotalDistanceLinear() {
        // Given: A linear track of 100m
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Getting total distance
        double distance = movement.getTotalDistance();

        // Then: Should be 100m
        assertThat(distance).isCloseTo(100.0, within(EPSILON));
    }

    @Test
    void testGetTotalDistanceLinearDiagonal() {
        // Given: A diagonal linear track
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(3.0, 4.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Getting total distance
        double distance = movement.getTotalDistance();

        // Then: Should be 5m (3-4-5 triangle)
        assertThat(distance).isCloseTo(5.0, within(EPSILON));
    }

    @Test
    void testGetTotalDistanceBezier() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: Getting total distance
        double distance = movement.getTotalDistance();

        // Then: Should be greater than straight line distance
        // Straight line is 100m, Bezier curve is longer
        assertThat(distance).isGreaterThan(100.0);
        assertThat(distance).isLessThan(150.0); // Reasonable upper bound
    }

    @Test
    void testGetPositionAtDistanceLinear() {
        // Given: A linear track of 100m
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Getting position at 50m
        Position pos = movement.getPositionAtDistance(50.0);

        // Then: Should be at midpoint
        assertThat(pos.x()).isCloseTo(50.0, within(EPSILON));
        assertThat(pos.y()).isCloseTo(0.0, within(EPSILON));
        assertThat(pos.z()).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    void testGetPositionAtDistanceBezier() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: Getting position at half distance
        double totalDistance = movement.getTotalDistance();
        Position pos = movement.getPositionAtDistance(totalDistance / 2.0);

        // Then: Should be approximately at midpoint (t=0.5)
        assertThat(pos.x()).isCloseTo(50.0, within(0.1));
        assertThat(pos.z()).isCloseTo(7.5, within(0.1));
    }

    @Test
    void testGetDirectionAtLinear() {
        // Given: A linear track along X axis
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Getting direction at any point
        double direction = movement.getDirectionAt(0.5);

        // Then: Should be 0 (pointing along +X axis)
        assertThat(direction).isCloseTo(0.0, within(EPSILON));
    }

    @Test
    void testGetDirectionAtYAxis() {
        // Given: A linear track along Y axis
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(0.0, 100.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Getting direction
        double direction = movement.getDirectionAt(0.5);

        // Then: Should be π/2 (pointing along +Y axis)
        assertThat(direction).isCloseTo(Math.PI / 2.0, within(EPSILON));
    }

    @Test
    void testGetDirectionAtNegativeX() {
        // Given: A linear track along negative X axis
        Position start = new Position(100.0, 0.0, 0.0);
        Position end = new Position(0.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Getting direction
        double direction = movement.getDirectionAt(0.5);

        // Then: Should be π (or -π)
        assertThat(Math.abs(direction)).isCloseTo(Math.PI, within(EPSILON));
    }

    @Test
    void testGetDirectionAtBezierStart() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 25.0, 0.0);
        Position c2 = new Position(75.0, 25.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: Getting direction at start
        double direction = movement.getDirectionAt(0.0);

        // Then: Should point towards control point 1
        // Direction from (0,0) to (25,25) is π/4
        assertThat(direction).isCloseTo(Math.PI / 4.0, within(0.01));
    }

    @Test
    void testGetDirectionAtBezierEnd() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 25.0, 0.0);
        Position c2 = new Position(75.0, 25.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: Getting direction at end
        double direction = movement.getDirectionAt(1.0);

        // Then: Should point from control point 2 to end
        // Direction from (75,25) to (100,0) is -π/4
        assertThat(direction).isCloseTo(-Math.PI / 4.0, within(0.01));
    }

    @Test
    void testIsLinear() {
        // Given: A linear track
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When: Checking if linear
        // Then: Should be linear
        assertThat(movement.isLinear()).isTrue();
    }

    @Test
    void testIsNotLinear() {
        // Given: A Bezier track
        Position start = new Position(0.0, 0.0, 0.0);
        Position c1 = new Position(25.0, 0.0, 10.0);
        Position c2 = new Position(75.0, 0.0, 10.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // When: Checking if linear
        // Then: Should not be linear
        assertThat(movement.isLinear()).isFalse();
    }

    @Test
    void testInvalidProgress() {
        // Given: A track
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // When/Then: Negative progress should throw exception
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> movement.getPositionAt(-0.1)
        );

        // When/Then: Progress > 1 should throw exception
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> movement.getPositionAt(1.1)
        );
    }

    @Test
    void testControlPointValidation_BothNull_IsValid() {
        // Given: Valid positions with both control points null (linear track)
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);

        // When: Creating TrackMovement with both null control points
        TrackMovement movement = new TrackMovement(start, end, null, null);

        // Then: Should be valid and linear
        assertThat(movement.isLinear()).isTrue();
    }

    @Test
    void testControlPointValidation_BothNonNull_IsValid() {
        // Given: Valid positions with both control points non-null (Bezier track)
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        Position c1 = new Position(30.0, 50.0, 0.0);
        Position c2 = new Position(70.0, 50.0, 0.0);

        // When: Creating TrackMovement with both non-null control points
        TrackMovement movement = new TrackMovement(start, end, c1, c2);

        // Then: Should be valid and not linear
        assertThat(movement.isLinear()).isFalse();
    }

    @Test
    void testControlPointValidation_OnlyControl1Null_ThrowsException() {
        // Given: Valid positions
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        Position c1 = null;
        Position c2 = new Position(70.0, 50.0, 0.0);

        // When/Then: Creating TrackMovement with only control1 null should throw exception
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new TrackMovement(start, end, c1, c2)
        );
    }

    @Test
    void testControlPointValidation_OnlyControl2Null_ThrowsException() {
        // Given: Valid positions
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(100.0, 0.0, 0.0);
        Position c1 = new Position(30.0, 50.0, 0.0);
        Position c2 = null;

        // When/Then: Creating TrackMovement with only control2 null should throw exception
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new TrackMovement(start, end, c1, c2)
        );
    }
}
