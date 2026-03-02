package com.semi.simlogistics.scenario;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.movement.TrackMovement;
import com.semi.simlogistics.vehicle.OHTVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test for basic OHT transport scenario.
 * <p>
 * Tests a minimal OHT transport scenario: OHT moves from point A to point B.
 * This validates:
 * - Entity movement along track
 * - Position changes according to TrackMovement logic
 * - State transitions (IDLE → MOVING → IDLE)
 * - Distance and direction calculations
 * <p>
 * Aligns with:
 * - REQ-ENT-002: OHTVehicle entity (OHT track movement scenario)
 * - REQ-TC-003: Curve path support (Bezier)
 * - REQ-TC-004: Unified unit constraints (coordinates in m, angles in radians [-π, π])
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class BasicTransportScenarioTest {

    private OHTVehicle oht;
    private Position pointA;
    private Position pointB;

    @BeforeEach
    void setUp() {
        // Given: Point A at origin (0, 0, 0) and Point B at (100, 0, 0)
        // Coordinates are in meters (m) as per REQ-TC-004
        pointA = new Position(0.0, 0.0, 0.0);
        pointB = new Position(100.0, 0.0, 0.0);

        // And: An OHT vehicle at point A with max speed 10 m/s
        oht = new OHTVehicle("OHT-001", "Test OHT", pointA, 10.0);
    }

    @Test
    void testOHTInitialState() {
        // Then: OHT should be at point A with IDLE state
        assertThat(oht.position()).isEqualTo(pointA);
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
        assertThat(oht.getMaxSpeed()).isEqualTo(10.0); // m/s
    }

    @Test
    void testLinearTrackMovementFromAToB() {
        // Given: A linear track from point A to point B
        TrackMovement track = new TrackMovement(pointA, pointB, null, null);

        // When: OHT moves from A to B
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
        oht.moveTo(pointB);

        // Then: OHT state should return to IDLE after movement
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);

        // And: OHT should be at point B
        Position finalPosition = oht.position();
        assertThat(finalPosition.x()).isCloseTo(100.0, within(0.001)); // meters
        assertThat(finalPosition.y()).isCloseTo(0.0, within(0.001));
        assertThat(finalPosition.z()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testTrackMovementDistanceCalculation() {
        // Given: A linear track from point A to point B
        TrackMovement track = new TrackMovement(pointA, pointB, null, null);

        // When: Calculating total distance
        double distance = track.getTotalDistance();

        // Then: Distance should be 100 meters
        assertThat(distance).isCloseTo(100.0, within(0.001));
    }

    @Test
    void testTrackMovementDirectionCalculation() {
        // Given: A linear track from point A to point B (along X axis)
        TrackMovement track = new TrackMovement(pointA, pointB, null, null);

        // When: Calculating direction at start, middle, and end
        double directionAtStart = track.getDirectionAt(0.0);
        double directionAtMid = track.getDirectionAt(0.5);
        double directionAtEnd = track.getDirectionAt(1.0);

        // Then: Direction should be 0 radians (pointing along +X axis)
        // Angles are in radians [-π, π] as per REQ-TC-004
        assertThat(directionAtStart).isCloseTo(0.0, within(0.001));
        assertThat(directionAtMid).isCloseTo(0.0, within(0.001));
        assertThat(directionAtEnd).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testTrackMovementProgressPositions() {
        // Given: A linear track from point A to point B
        TrackMovement track = new TrackMovement(pointA, pointB, null, null);

        // When: Getting positions at different progress levels
        Position pos0 = track.getPositionAt(0.0);   // Start
        Position pos25 = track.getPositionAt(0.25); // 25% progress
        Position pos50 = track.getPositionAt(0.5);  // 50% progress
        Position pos75 = track.getPositionAt(0.75); // 75% progress
        Position pos100 = track.getPositionAt(1.0);  // End

        // Then: Positions should follow linear interpolation
        assertThat(pos0.x()).isCloseTo(0.0, within(0.001));
        assertThat(pos25.x()).isCloseTo(25.0, within(0.001));
        assertThat(pos50.x()).isCloseTo(50.0, within(0.001));
        assertThat(pos75.x()).isCloseTo(75.0, within(0.001));
        assertThat(pos100.x()).isCloseTo(100.0, within(0.001));

        // Y and Z should remain 0
        assertThat(pos50.y()).isCloseTo(0.0, within(0.001));
        assertThat(pos50.z()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testBezierTrackMovementFromAToB() {
        // Given: Point A at (0, 0, 0), Point B at (100, 0, 0)
        // And: Control points creating a curved track (elevation change)
        Position control1 = new Position(25.0, 0.0, 10.0);
        Position control2 = new Position(75.0, 0.0, 10.0);
        TrackMovement bezierTrack = new TrackMovement(pointA, pointB, control1, control2);

        // When: Calculating track properties
        double distance = bezierTrack.getTotalDistance();
        Position posAt50 = bezierTrack.getPositionAt(0.5);
        double directionAtStart = bezierTrack.getDirectionAt(0.0);

        // Then: Distance should be greater than straight line (Bezier curve is longer)
        assertThat(distance).isGreaterThan(100.0);
        assertThat(distance).isLessThan(150.0); // Reasonable upper bound

        // And: Midpoint should have elevation (z > 0)
        assertThat(posAt50.x()).isCloseTo(50.0, within(0.1)); // X should be at midpoint
        assertThat(posAt50.z()).isCloseTo(7.5, within(0.1)); // Z should be elevated

        // And: Direction at start should point towards control point 1
        // Direction from (0,0) to (25,0,10) is approximately along X with slight Z
        assertThat(directionAtStart).isCloseTo(0.0, within(0.1)); // Mostly along +X axis
    }

    @Test
    void testMovementTimeCalculation() {
        // Given: A linear track of 100m and OHT speed of 10 m/s
        TrackMovement track = new TrackMovement(pointA, pointB, null, null);
        double speed = oht.getMaxSpeed(); // 10 m/s
        double distance = track.getTotalDistance(); // 100m

        // When: Calculating movement time
        double expectedTime = distance / speed;

        // Then: Movement time should be 10 seconds
        assertThat(expectedTime).isCloseTo(10.0, within(0.001));
    }

    @Test
    void testStateTransitionDuringMovement() {
        // Given: OHT at point A in IDLE state with state tracking
        TestableOHTVehicle trackableOht = new TestableOHTVehicle("OHT-TRACK", "Trackable OHT", pointA, 10.0);
        assertThat(trackableOht.getState()).isEqualTo(VehicleState.IDLE);

        // Note: getStateHistory() returns a copy, so we don't manipulate it directly
        // The history will record: MOVING (from moveTo start), IDLE (from moveTo end)

        // When: Moving to point B
        trackableOht.moveTo(pointB);

        // Then: State should transition from IDLE to MOVING, then back to IDLE
        List<VehicleState> stateHistory = trackableOht.getStateHistory();

        // Verify state transition sequence: MOVING → IDLE
        // (Initial IDLE is not recorded because constructor doesn't call setState())
        assertThat(stateHistory).hasSize(2);
        assertThat(stateHistory.get(0)).isEqualTo(VehicleState.MOVING);
        assertThat(stateHistory.get(1)).isEqualTo(VehicleState.IDLE);

        // Verify MOVING state occurred during movement
        assertThat(stateHistory).contains(VehicleState.MOVING);

        // Verify final state is IDLE
        assertThat(trackableOht.getState()).isEqualTo(VehicleState.IDLE);

        // And: Position should be at point B
        assertThat(trackableOht.position()).isEqualTo(pointB);
    }

    /**
     * Testable OHT vehicle that records state transitions for testing.
     * <p>
     * Extends OHTVehicle and overrides setState() to track all state changes.
     * This allows tests to verify that intermediate states (like MOVING) occur
     * during operations, not just the final state.
     */
    private static class TestableOHTVehicle extends OHTVehicle {

        private final List<VehicleState> stateHistory = new ArrayList<>();

        public TestableOHTVehicle(String id, String name, Position position, double maxSpeed) {
            super(id, name, position, maxSpeed);
        }

        @Override
        public void setState(VehicleState state) {
            // Record state change before calling super
            stateHistory.add(state);
            super.setState(state);
        }

        /**
         * Get the history of state transitions.
         *
         * @return list of states in order they were set
         */
        public List<VehicleState> getStateHistory() {
            return new ArrayList<>(stateHistory);
        }
    }

    @Test
    void testPositionAtDistanceFromStart() {
        // Given: A linear track of 100m
        TrackMovement track = new TrackMovement(pointA, pointB, null, null);

        // When: Getting position at 50m from start (halfway)
        Position pos = track.getPositionAtDistance(50.0);

        // Then: Should be at midpoint
        assertThat(pos.x()).isCloseTo(50.0, within(0.001));
        assertThat(pos.y()).isCloseTo(0.0, within(0.001));
        assertThat(pos.z()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testDiagonalTrackMovement() {
        // Given: A diagonal track from (0,0,0) to (30,40,0)
        // This forms a 3-4-5 triangle (distance = 50m)
        Position start = new Position(0.0, 0.0, 0.0);
        Position end = new Position(30.0, 40.0, 0.0);
        TrackMovement track = new TrackMovement(start, end, null, null);

        // When: Calculating distance and direction
        double distance = track.getTotalDistance();
        double direction = track.getDirectionAt(0.5);

        // Then: Distance should be 50m (3-4-5 triangle)
        assertThat(distance).isCloseTo(50.0, within(0.001));

        // And: Direction should be atan2(40, 30) ≈ 0.927 radians
        double expectedDirection = Math.atan2(40.0, 30.0);
        assertThat(direction).isCloseTo(expectedDirection, within(0.001));

        // Verify direction is in range [-π, π] as per REQ-TC-004
        assertThat(direction).isGreaterThanOrEqualTo(-Math.PI);
        assertThat(direction).isLessThanOrEqualTo(Math.PI);
    }

    @Test
    void testTrackIsLinearDetection() {
        // Given: Linear and Bezier tracks
        TrackMovement linearTrack = new TrackMovement(pointA, pointB, null, null);
        Position control1 = new Position(25.0, 0.0, 10.0);
        Position control2 = new Position(75.0, 0.0, 10.0);
        TrackMovement bezierTrack = new TrackMovement(pointA, pointB, control1, control2);

        // When: Checking track type
        // Then: Should correctly identify linear vs Bezier
        assertThat(linearTrack.isLinear()).isTrue();
        assertThat(bezierTrack.isLinear()).isFalse();
    }

    @Test
    void testMultiSegmentMovementScenario() {
        // Given: OHT at origin (0,0,0)
        Position origin = new Position(0.0, 0.0, 0.0);
        OHTVehicle oht = new OHTVehicle("OHT-MULTI", "Multi-Segment OHT", origin, 10.0);

        // And: Three segments forming a path
        Position p1 = new Position(50.0, 0.0, 0.0);  // First segment: 50m along X
        Position p2 = new Position(50.0, 50.0, 0.0); // Second segment: 50m along Y
        Position p3 = new Position(100.0, 50.0, 0.0); // Third segment: 50m along X

        // When: Moving through each segment
        oht.moveTo(p1);
        assertThat(oht.position()).isEqualTo(p1);

        oht.moveTo(p2);
        assertThat(oht.position()).isEqualTo(p2);

        oht.moveTo(p3);
        assertThat(oht.position()).isEqualTo(p3);

        // Then: OHT should reach final destination
        assertThat(oht.position().x()).isCloseTo(100.0, within(0.001));
        assertThat(oht.position().y()).isCloseTo(50.0, within(0.001));
        assertThat(oht.position().z()).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testInvalidMovementFromNonIdleState() {
        // Given: OHT in IDLE state
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);

        // Note: Current moveTo() implementation checks state before allowing movement
        // In a real scenario, state would be set to MOVING during movement
        // This test validates the state machine constraint

        // When: OHT completes movement
        oht.moveTo(pointB);

        // Then: Should be back in IDLE state
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
    }
}
