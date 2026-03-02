package com.semi.simlogistics.vehicle;

import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OHTVehicle class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class OHTVehicleTest {

    @Test
    void testOHTVehicleCreation() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);

        assertThat(oht.id()).isEqualTo("OHT-001");
        assertThat(oht.name()).isEqualTo("Test OHT");
        assertThat(oht.type()).isEqualTo(EntityType.OHT_VEHICLE);
        assertThat(oht.position()).isEqualTo(pos);
        assertThat(oht.getMaxSpeed()).isEqualTo(5.0);
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
        assertThat(oht.hasBattery()).isFalse();
    }

    @Test
    void testMoveToUpdatesPosition() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(100.0, 50.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);

        oht.moveTo(dest);

        assertThat(oht.position()).isEqualTo(dest);
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
    }

    @Test
    void testMoveToWhenNotIdle() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(10.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        oht.setState(VehicleState.LOADING);

        assertThatThrownBy(() -> oht.moveTo(dest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available for movement");
    }

    @Test
    void testMoveToWhenBlocked() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(10.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        oht.setState(VehicleState.BLOCKED);

        // BLOCKED state should allow movement (waiting for traffic control to clear)
        oht.moveTo(dest);

        assertThat(oht.position()).isEqualTo(dest);
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
    }

    @Test
    void testLoadCargo() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        oht.load(cargo);

        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
        assertThat(oht.getTransport().isLoaded()).isTrue();
        assertThat(oht.getTransport().getCurrentCargo()).isEqualTo(cargo);
    }

    @Test
    void testUnloadCargo() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        oht.load(cargo);
        oht.unload();

        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
        assertThat(oht.getTransport().isLoaded()).isFalse();
        assertThat(oht.getTransport().getCurrentCargo()).isNull();
    }

    @Test
    void testLoadWhenNotIdle() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        oht.setState(VehicleState.MOVING);

        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        assertThatThrownBy(() -> oht.load(cargo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available for loading");
    }

    @Test
    void testUnloadWhenNotIdle() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        oht.setState(VehicleState.MOVING);

        assertThatThrownBy(() -> oht.unload())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available for unloading");
    }

    @Test
    void testLoadUnloadStateTransitions() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        // IDLE -> IDLE (load completes and returns to IDLE)
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
        oht.load(cargo);
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);

        // IDLE -> IDLE (unload completes and returns to IDLE)
        oht.unload();
        assertThat(oht.getState()).isEqualTo(VehicleState.IDLE);
    }

    @Test
    void testIsAvailable() {
        Position pos = new Position(0.0, 0.0);

        OHTVehicle oht = new OHTVehicle("OHT-001", "Test OHT", pos, 5.0);

        assertThat(oht.isAvailable()).isTrue();

        oht.setState(VehicleState.MOVING);
        assertThat(oht.isAvailable()).isFalse();

        oht.setState(VehicleState.BLOCKED);
        assertThat(oht.isAvailable()).isFalse(); // BLOCKED is not available

        oht.setState(VehicleState.IDLE);
        assertThat(oht.isAvailable()).isTrue();
    }
}
