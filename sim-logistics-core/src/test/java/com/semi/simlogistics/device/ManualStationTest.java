package com.semi.simlogistics.device;

import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.operator.Operator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ManualStation class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class ManualStationTest {

    @Test
    void testManualStationCreation() {
        Position pos = new Position(10.0, 20.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);

        ManualStation station = new ManualStation("STATION-001", "Manual Station 1", pos, 5, types);

        assertThat(station.id()).isEqualTo("STATION-001");
        assertThat(station.name()).isEqualTo("Manual Station 1");
        assertThat(station.getCapacity()).isEqualTo(5);
        assertThat(station.getCurrentLoad()).isEqualTo(0);
        assertThat(station.isOperatorPresent()).isFalse();
    }

    @Test
    void testLoadCargo() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        boolean result = station.load(cargo);

        assertThat(result).isTrue();
        assertThat(station.getCurrentLoad()).isEqualTo(1);
        assertThat(station.isEmpty()).isFalse();
    }

    @Test
    void testUnloadCargo() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        station.load(cargo);
        Cargo unloaded = station.unload();

        assertThat(unloaded).isEqualTo(cargo);
        assertThat(station.getCurrentLoad()).isEqualTo(0);
        assertThat(station.isEmpty()).isTrue();
    }

    @Test
    void testUnloadWhenEmpty() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);

        Cargo unloaded = station.unload();

        assertThat(unloaded).isNull();
    }

    @Test
    void testFIFOBehavior() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo3 = new Cargo("CARGO-003", Cargo.CargoType.FOUP, 250.0);

        station.load(cargo1);
        station.load(cargo2);
        station.load(cargo3);

        // FIFO: first in, first out
        assertThat(station.unload()).isEqualTo(cargo1);
        assertThat(station.unload()).isEqualTo(cargo2);
        assertThat(station.unload()).isEqualTo(cargo3);
    }

    @Test
    void testLoadWhenFull() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        station.load(cargo1);
        boolean result = station.load(cargo2);

        assertThat(result).isFalse();
        assertThat(station.getCurrentLoad()).isEqualTo(1);
        assertThat(station.isFull()).isTrue();
    }

    @Test
    void testOperatorPresence() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);

        assertThat(station.isOperatorPresent()).isFalse();

        station.setOperatorPresent(true);
        assertThat(station.isOperatorPresent()).isTrue();

        station.setOperatorPresent(false);
        assertThat(station.isOperatorPresent()).isFalse();
    }

    @Test
    void testSupportedTransportTypes() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN, TransportType.OHT);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);

        assertThat(station.supportsTransportType(TransportType.HUMAN)).isTrue();
        assertThat(station.supportsTransportType(TransportType.OHT)).isTrue();
        assertThat(station.supportsTransportType(TransportType.AGV)).isFalse();
    }

    @Test
    void testGetAvailableCapacity() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);

        assertThat(station.getAvailableCapacity()).isEqualTo(10);

        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        station.load(cargo);

        assertThat(station.getAvailableCapacity()).isEqualTo(9);
    }

    @Test
    void testLoadUnloadFlow() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);

        station.setOperatorPresent(true);

        // Load multiple items
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo3 = new Cargo("CARGO-003", Cargo.CargoType.FOUP, 250.0);

        assertThat(station.load(cargo1)).isTrue();
        assertThat(station.load(cargo2)).isTrue();
        assertThat(station.load(cargo3)).isTrue();

        assertThat(station.getCurrentLoad()).isEqualTo(3);

        // Unload in FIFO order
        assertThat(station.unload()).isEqualTo(cargo1);
        assertThat(station.getCurrentLoad()).isEqualTo(2);

        assertThat(station.unload()).isEqualTo(cargo2);
        assertThat(station.getCurrentLoad()).isEqualTo(1);

        assertThat(station.unload()).isEqualTo(cargo3);
        assertThat(station.getCurrentLoad()).isEqualTo(0);
    }

    @Test
    void testComputeProcessTimeWithNullOperator() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);

        assertThatThrownBy(() -> station.computeProcessTime(null, "load"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operator cannot be null");
    }

    @Test
    void testComputeProcessTimeWhenOperatorNotOnShift() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);
        Operator operator = new Operator("OP-001", "Test", pos);
        // operator is NOT on shift

        assertThatThrownBy(() -> station.computeProcessTime(operator, "load"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not on shift");
    }

    @Test
    void testComputeProcessTimeWithSkill() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);
        Operator operator = new Operator("OP-001", "Test", pos);
        operator.setOnShift(true);
        operator.addSkill("load");

        // With skill: 60.0 * 0.8 = 48.0 seconds
        double time = station.computeProcessTime(operator, "load");
        assertThat(time).isEqualTo(48.0);
    }

    @Test
    void testComputeProcessTimeWithoutSkill() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.HUMAN);
        ManualStation station = new ManualStation("STATION-001", "Test", pos, 10, types);
        Operator operator = new Operator("OP-001", "Test", pos);
        operator.setOnShift(true);
        // operator does NOT have "load" skill

        // Without skill: 60.0 * 1.0 = 60.0 seconds
        double time = station.computeProcessTime(operator, "load");
        assertThat(time).isEqualTo(60.0);
    }
}
