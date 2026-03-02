package com.semi.simlogistics.vehicle;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Vehicle path and transport type methods (REQ-TC-007, REQ-TC-007.1).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
class VehiclePathTest {

    @Test
    void testGetPath_InitialValueIsNull() {
        // Given: New OHT vehicle
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test Vehicle", position, 10.0);

        // When: Get path
        // Then: Should be null
        assertThat(vehicle.getPath()).isNull();
    }

    @Test
    void testSetPathAndGetPath() {
        // Given: OHT vehicle
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test Vehicle", position, 10.0);

        // When: Set path
        vehicle.setPath("A->B->C");

        // Then: Path should be retrievable
        assertThat(vehicle.getPath()).isEqualTo("A->B->C");
    }

    @Test
    void testSetPath_UpdatesExistingPath() {
        // Given: Vehicle with existing path
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test Vehicle", position, 10.0);
        vehicle.setPath("OLD_PATH");

        // When: Update path
        vehicle.setPath("NEW_PATH");

        // Then: Path should be updated
        assertThat(vehicle.getPath()).isEqualTo("NEW_PATH");
    }

    @Test
    void testGetTransportType_OHTVehicle() {
        // Given: OHT vehicle
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test OHT", position, 10.0);

        // When: Get transport type
        TransportType transportType = vehicle.getTransportType();

        // Then: Should return OHT
        assertThat(transportType).isEqualTo(TransportType.OHT);
    }

    @Test
    void testGetTransportType_AGVVehicle() {
        // Given: AGV vehicle (simulated with OHTVehicle but different type)
        // Note: OHTVehicle constructor always sets EntityType.OHT_VEHICLE
        // In production, AGVVehicle class would set EntityType.AGV_VEHICLE
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test AGV", position, 10.0);

        // When: Get transport type
        TransportType transportType = vehicle.getTransportType();

        // Then: Should return OHT (since OHTVehicle has OHT_VEHICLE type)
        // TODO: When AGVVehicle is implemented, this test should use AGVVehicle
        // and expect TransportType.AGV
        assertThat(transportType).isEqualTo(TransportType.OHT);
    }

    @Test
    void testPathToString_Conversion() {
        // Given: Vehicle with path containing multiple nodes
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test Vehicle", position, 10.0);
        vehicle.setPath("NODE_A->NODE_B->NODE_C");

        // When: Get path
        String path = vehicle.getPath();

        // Then: Should be the expected string representation
        assertThat(path).isEqualTo("NODE_A->NODE_B->NODE_C");
    }

    @Test
    void testSetPath_WithEmptyString() {
        // Given: Vehicle with existing path
        Position position = new Position(0.0, 0.0, 0.0);
        OHTVehicle vehicle = new OHTVehicle("V1", "Test Vehicle", position, 10.0);
        vehicle.setPath("SOME_PATH");

        // When: Set empty path
        vehicle.setPath("");

        // Then: Path should be empty
        assertThat(vehicle.getPath()).isEqualTo("");
    }
}
