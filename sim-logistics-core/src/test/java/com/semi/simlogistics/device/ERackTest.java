package com.semi.simlogistics.device;

import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ERack class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class ERackTest {

    @Test
    void testERackCreation() {
        Position pos = new Position(10.0, 20.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT, TransportType.AGV);

        ERack rack = new ERack("ERACK-001", "Test Rack", pos, 10, types);

        assertThat(rack.id()).isEqualTo("ERACK-001");
        assertThat(rack.name()).isEqualTo("Test Rack");
        assertThat(rack.getCapacity()).isEqualTo(10);
        assertThat(rack.getCurrentLoad()).isEqualTo(0);
        assertThat(rack.isEmpty()).isTrue();
        assertThat(rack.isFull()).isFalse();
    }

    @Test
    void testLoadCargo() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        ERack rack = new ERack("ERACK-001", "Test", pos, 10, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        boolean result = rack.load(cargo);

        assertThat(result).isTrue();
        assertThat(rack.getCurrentLoad()).isEqualTo(1);
        assertThat(rack.isEmpty()).isFalse();
        assertThat(rack.peek()).isEqualTo(cargo);
    }

    @Test
    void testLoadCargoWhenFull() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        ERack rack = new ERack("ERACK-001", "Test", pos, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        rack.load(cargo1);
        boolean result = rack.load(cargo2);

        assertThat(result).isFalse();
        assertThat(rack.getCurrentLoad()).isEqualTo(1);
        assertThat(rack.isFull()).isTrue();
    }

    @Test
    void testUnloadCargo() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        ERack rack = new ERack("ERACK-001", "Test", pos, 10, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        rack.load(cargo);
        Cargo unloaded = rack.unload();

        assertThat(unloaded).isEqualTo(cargo);
        assertThat(rack.getCurrentLoad()).isEqualTo(0);
        assertThat(rack.isEmpty()).isTrue();
    }

    @Test
    void testUnloadCargoWhenEmpty() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        ERack rack = new ERack("ERACK-001", "Test", pos, 10, types);

        Cargo unloaded = rack.unload();

        assertThat(unloaded).isNull();
    }

    @Test
    void testFIFOBehavior() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        ERack rack = new ERack("ERACK-001", "Test", pos, 10, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo3 = new Cargo("CARGO-003", Cargo.CargoType.FOUP, 250.0);

        rack.load(cargo1);
        rack.load(cargo2);
        rack.load(cargo3);

        assertThat(rack.unload()).isEqualTo(cargo1); // First in, first out
        assertThat(rack.unload()).isEqualTo(cargo2);
        assertThat(rack.unload()).isEqualTo(cargo3);
    }

    @Test
    void testSupportedTransportTypes() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT, TransportType.AGV);
        ERack rack = new ERack("ERACK-001", "Test", pos, 10, types);

        assertThat(rack.supportsTransportType(TransportType.OHT)).isTrue();
        assertThat(rack.supportsTransportType(TransportType.AGV)).isTrue();
        assertThat(rack.supportsTransportType(TransportType.HUMAN)).isFalse();
    }

    @Test
    void testSupportsTransportTypes() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT, TransportType.AGV);
        ERack rack = new ERack("ERACK-001", "Test", pos, 10, types);

        List<TransportType> required1 = Arrays.asList(TransportType.OHT);
        List<TransportType> required2 = Arrays.asList(TransportType.AGV, TransportType.HUMAN);
        List<TransportType> required3 = Arrays.asList(TransportType.HUMAN);

        assertThat(rack.supportsTransportTypes(required1)).isTrue();
        assertThat(rack.supportsTransportTypes(required2)).isTrue(); // AGV in intersection
        assertThat(rack.supportsTransportTypes(required3)).isFalse();
    }
}
