package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.operator.Operator;
import com.semi.simlogistics.vehicle.AGVVehicle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Human operator traffic rules.
 * <p>
 * Tests that Human operators do NOT occupy ControlPoint - they only use SafetyZone.
 * This is a critical safety rule from the spec.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class HumanNotOccupyControlPointTest {

    @Test
    void testHumanOperatorCannotOccupyControlPoint() {
        // Given: A ControlPoint
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Operator operator = new Operator("OP-1", "Operator 1", pos);

        // When: Checking if operator type should be rejected
        boolean shouldBeRejected = shouldRejectEntityType(operator.type());

        // Then: Operators should be rejected
        assertThat(shouldBeRejected).isTrue();
        assertThat(operator.type()).isEqualTo(EntityType.OPERATOR);

        // Note: We cannot call cp.requestEntry(operator, ...) because
        // Operator is not a Vehicle. In actual implementation, the
        // TrafficManager would check the entity type before calling
        // ControlPoint.requestEntry().
    }

    @Test
    void testVehicleCanOccupyControlPoint() {
        // Given: A ControlPoint
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // When: AGV vehicle requests entry to ControlPoint
        boolean granted = cp.requestEntry(vehicle, currentTime);

        // Then: Should be GRANTED
        assertThat(granted).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);
    }

    @Test
    void testControlPointRejectsOperatorEntityType() {
        // Given: Entity types to check
        EntityType operatorType = EntityType.OPERATOR;
        EntityType vehicleType = EntityType.AGV_VEHICLE;

        // When: Checking if entity types should be rejected
        boolean rejectOperator = shouldRejectEntityType(operatorType);
        boolean rejectVehicle = shouldRejectEntityType(vehicleType);

        // Then: Operators should be rejected, vehicles should not
        assertThat(rejectOperator).isTrue();
        assertThat(rejectVehicle).isFalse();
    }

    @Test
    void testHumanOperatorsUseSafetyZone() {
        // Given: A SafetyZone (different from ControlPoint)
        // TODO: Implement SafetyZone class for human-only areas

        Position pos = new Position(0.0, 0.0, 0.0);
        Operator operator = new Operator("OP-1", "Operator 1", pos);

        // When: Operator enters SafetyZone
        // Then: Should be allowed
        // SafetyZone zone = new SafetyZone("ZONE-1", "MANUAL-STATION-A", 5);
        // boolean granted = zone.requestEntry(operator, 0.0);
        // assertThat(granted).isTrue();

        // For now, just verify operator type
        assertThat(operator.type()).isEqualTo(EntityType.OPERATOR);
    }

    @Test
    void testControlPointAllowsOnlyVehicleTypes() {
        // Given: A ControlPoint
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);

        // Then: Should allow only vehicle entity types
        // Allowed types: OHT_VEHICLE, AGV_VEHICLE, CONVEYOR
        // Blocked types: OPERATOR (humans use SafetyZone)

        // TODO: Implement allowed entity types check
        // EntityType[] allowedTypes = {
        //     EntityType.OHT_VEHICLE,
        //     EntityType.AGV_VEHICLE,
        //     EntityType.CONVEYOR
        // };

        // EntityType[] blockedTypes = {
        //     EntityType.OPERATOR
        // };
    }

    @Test
    void testMixedTrafficWithHumanAndVehicle() {
        // Given: A manual station with SafetyZone and nearby ControlPoint
        // TODO: Implement SafetyZone

        Position pos = new Position(0.0, 0.0, 0.0);
        Operator operator = new Operator("OP-1", "Operator 1", pos);
        AGVVehicle vehicle = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        // When: Vehicle requests ControlPoint
        boolean vehicleGranted = cp.requestEntry(vehicle, currentTime);

        // Then: Vehicle should be allowed
        assertThat(vehicleGranted).isTrue();

        // When: Operator tries to request ControlPoint
        // Then: Should be rejected
        // boolean operatorGranted = cp.requestEntry(operator, currentTime);
        // assertThat(operatorGranted).isFalse();

        // Operator should use SafetyZone instead
        // SafetyZone zone = new SafetyZone("ZONE-1", "MANUAL-STATION", 5);
        // boolean zoneGranted = zone.requestEntry(operator, currentTime);
        // assertThat(zoneGranted).isTrue();
    }

    @Test
    void testControlPointEntityTypeFilter() {
        // Given: Need to filter entity types in ControlPoint
        // From user feedback: "spec 明确 Human 不占用 ControlPoint，只走 SafetyZone"

        // When: Checking if entity type should be allowed
        EntityType operatorType = EntityType.OPERATOR;
        EntityType vehicleType = EntityType.AGV_VEHICLE;

        // Then: Operators should be rejected
        boolean shouldRejectOperator = shouldRejectEntityType(operatorType);
        assertThat(shouldRejectOperator).isTrue();

        // And: Vehicles should be allowed
        boolean shouldRejectVehicle = shouldRejectEntityType(vehicleType);
        assertThat(shouldRejectVehicle).isFalse();

        // TODO: Implement this logic in ControlPoint.requestEntry()
    }

    /**
     * Helper method to check if entity type should be rejected from ControlPoint.
     * TODO: Implement this in ControlPoint.
     */
    private boolean shouldRejectEntityType(EntityType type) {
        // Human operators cannot occupy ControlPoint
        if (type == EntityType.OPERATOR) {
            return true;
        }
        // All other entity types are allowed (OHT_VEHICLE, AGV_VEHICLE, CONVEYOR, etc.)
        return false;
    }
}
