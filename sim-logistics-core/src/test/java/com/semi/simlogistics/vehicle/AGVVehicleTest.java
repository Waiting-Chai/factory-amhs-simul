package com.semi.simlogistics.vehicle;

import com.semi.simlogistics.capability.BatteryCapability;
import com.semi.simlogistics.capability.DefaultBatteryCapability;
import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AGVVehicle class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class AGVVehicleTest {

    @Test
    void testAGVVehicleCreation() {
        Position pos = new Position(0.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 1.0, 0.2);

        assertThat(agv.id()).isEqualTo("AGV-001");
        assertThat(agv.name()).isEqualTo("Test AGV");
        assertThat(agv.getMaxSpeed()).isEqualTo(2.0);
        assertThat(agv.getBatteryConsumptionRate()).isEqualTo(0.01);
        assertThat(agv.getChargingRate()).isEqualTo(0.1);
        assertThat(agv.getState()).isEqualTo(VehicleState.IDLE);
        assertThat(agv.hasBattery()).isTrue();
        assertThat(agv.getBattery().getBatteryLevel()).isEqualTo(1.0);
    }

    @Test
    void testMoveTo() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(100.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 1.0, 0.2);

        agv.moveTo(dest);

        assertThat(agv.position()).isEqualTo(dest);
        assertThat(agv.getState()).isEqualTo(VehicleState.IDLE);
    }

    @Test
    void testMoveToConsumesBattery() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(100.0, 0.0); // 100 meters

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.001, 0.1, 1.0, 0.2); // 0.001 per meter

        double initialLevel = agv.getBattery().getBatteryLevel();
        agv.moveTo(dest);
        double finalLevel = agv.getBattery().getBatteryLevel();

        // 100 meters * 0.001 = 0.1 consumption
        assertThat(finalLevel).isEqualTo(initialLevel - 0.1, within(0.001));
    }

    @Test
    void testMoveToWhenBatteryLow() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(10.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 0.15, 0.2); // Below low battery threshold

        assertThatThrownBy(() -> agv.moveTo(dest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("battery is too low");
    }

    @Test
    void testLoadAndUnloadCargo() {
        Position pos = new Position(0.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 1.0, 0.2);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        agv.load(cargo);

        assertThat(agv.getTransport().isLoaded()).isTrue();
        assertThat(agv.getTransport().getCurrentCargo()).isEqualTo(cargo);

        agv.unload();

        assertThat(agv.getTransport().isLoaded()).isFalse();
        assertThat(agv.getTransport().getCurrentCargo()).isNull();
    }

    @Test
    void testLoadWhenNotIdle() {
        Position pos = new Position(0.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 1.0, 0.2);

        agv.setState(VehicleState.MOVING);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        assertThatThrownBy(() -> agv.load(cargo))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testUnloadWhenNotIdle() {
        Position pos = new Position(0.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 1.0, 0.2);

        agv.setState(VehicleState.MOVING);

        assertThatThrownBy(() -> agv.unload())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testIsAvailable() {
        Position pos = new Position(0.0, 0.0);

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.01, 0.1, 1.0, 0.2);

        assertThat(agv.isAvailable()).isTrue();

        agv.setState(VehicleState.MOVING);
        assertThat(agv.isAvailable()).isFalse();

        agv.setState(VehicleState.IDLE);
        assertThat(agv.isAvailable()).isTrue();
    }

    @Test
    void testBatteryDoesNotGoBelowZero() {
        Position pos = new Position(0.0, 0.0);
        Position dest = new Position(1000.0, 0.0); // Very far

        AGVVehicle agv = new AGVVehicle("AGV-001", "Test AGV", pos, 2.0,
                0.001, 0.1, 0.5, 0.2); // 0.001 per meter

        agv.moveTo(dest);

        // Battery should not go below 0
        assertThat(agv.getBattery().getBatteryLevel()).isGreaterThanOrEqualTo(0.0);
        assertThat(agv.getBattery().getBatteryLevel()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void testBatteryConsumeDirectly() {
        BatteryCapability battery = new DefaultBatteryCapability(1.0, 0.2);

        assertThat(battery.getBatteryLevel()).isEqualTo(1.0);

        battery.consume(0.3);

        assertThat(battery.getBatteryLevel()).isEqualTo(0.7);
    }

    @Test
    void testBatteryConsumeNegativeAmount() {
        BatteryCapability battery = new DefaultBatteryCapability(1.0, 0.2);

        assertThatThrownBy(() -> battery.consume(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBatteryConsumeClampsToZero() {
        BatteryCapability battery = new DefaultBatteryCapability(0.5, 0.2);

        battery.consume(1.0); // More than current level

        assertThat(battery.getBatteryLevel()).isEqualTo(0.0);
    }
}
