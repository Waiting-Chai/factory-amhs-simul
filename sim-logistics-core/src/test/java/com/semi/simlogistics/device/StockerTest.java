package com.semi.simlogistics.device;

import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Stocker class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class StockerTest {

    @Test
    void testStockerCreation() {
        Position pos = new Position(10.0, 20.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT, TransportType.AGV);

        Stocker stocker = new Stocker("STOCKER-001", "Main Stocker", pos, 100, 2, types);

        assertThat(stocker.id()).isEqualTo("STOCKER-001");
        assertThat(stocker.name()).isEqualTo("Main Stocker");
        assertThat(stocker.getCapacity()).isEqualTo(100);
        assertThat(stocker.getCraneCount()).isEqualTo(2);
        assertThat(stocker.getCurrentLoad()).isEqualTo(0);
        assertThat(stocker.isEmpty()).isTrue();
        assertThat(stocker.isFull()).isFalse();
    }

    @Test
    void testLoadCargo() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        boolean result = stocker.load(cargo);

        assertThat(result).isTrue();
        assertThat(stocker.getCurrentLoad()).isEqualTo(1);
        assertThat(stocker.isEmpty()).isFalse();
        assertThat(stocker.getSlotState(0)).isEqualTo(Stocker.SlotState.OCCUPIED);
        assertThat(stocker.getCargoAtSlot(0)).isEqualTo(cargo);
    }

    @Test
    void testLoadCargoAssignsToFirstEmptySlot() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo1);
        stocker.load(cargo2);

        assertThat(stocker.getCargoAtSlot(0)).isEqualTo(cargo1);
        assertThat(stocker.getCargoAtSlot(1)).isEqualTo(cargo2);
    }

    @Test
    void testLoadCargoWhenFull() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 1, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo1);
        boolean result = stocker.load(cargo2);

        assertThat(result).isFalse();
        assertThat(stocker.getCurrentLoad()).isEqualTo(1);
        assertThat(stocker.isFull()).isTrue();
    }

    @Test
    void testUnloadCargo() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo);
        Cargo unloaded = stocker.unload();

        assertThat(unloaded).isEqualTo(cargo);
        assertThat(stocker.getCurrentLoad()).isEqualTo(0);
        assertThat(stocker.isEmpty()).isTrue();
        assertThat(stocker.getSlotState(0)).isEqualTo(Stocker.SlotState.EMPTY);
    }

    @Test
    void testUnloadCargoWhenEmpty() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        Cargo unloaded = stocker.unload();

        assertThat(unloaded).isNull();
    }

    @Test
    void testUnloadByCargoId() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo3 = new Cargo("CARGO-003", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo1);
        stocker.load(cargo2);
        stocker.load(cargo3);

        Cargo unloaded = stocker.unloadByCargoId("CARGO-002");

        assertThat(unloaded).isEqualTo(cargo2);
        assertThat(stocker.getCurrentLoad()).isEqualTo(2);
        assertThat(stocker.getSlotState(1)).isEqualTo(Stocker.SlotState.EMPTY);
        assertThat(stocker.getCargoAtSlot(0)).isEqualTo(cargo1);
        assertThat(stocker.getCargoAtSlot(2)).isEqualTo(cargo3);
    }

    @Test
    void testUnloadByCargoIdNotFound() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo);
        Cargo unloaded = stocker.unloadByCargoId("CARGO-999");

        assertThat(unloaded).isNull();
        assertThat(stocker.getCurrentLoad()).isEqualTo(1);
    }

    @Test
    void testUnloadByCargoIdNullId() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        assertThatThrownBy(() -> stocker.unloadByCargoId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testUnloadFromSlot() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo1);
        stocker.load(cargo2);

        Cargo unloaded = stocker.unloadFromSlot(1);

        assertThat(unloaded).isEqualTo(cargo2);
        assertThat(stocker.getSlotState(1)).isEqualTo(Stocker.SlotState.EMPTY);
        assertThat(stocker.getSlotState(0)).isEqualTo(Stocker.SlotState.OCCUPIED);
    }

    @Test
    void testUnloadFromSlotWhenEmpty() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        Cargo unloaded = stocker.unloadFromSlot(0);

        assertThat(unloaded).isNull();
    }

    @Test
    void testGetSlotState() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        assertThat(stocker.getSlotState(0)).isEqualTo(Stocker.SlotState.EMPTY);

        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        stocker.load(cargo);

        assertThat(stocker.getSlotState(0)).isEqualTo(Stocker.SlotState.OCCUPIED);
    }

    @Test
    void testGetSlotStateInvalidIndex() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        assertThatThrownBy(() -> stocker.getSlotState(-1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stocker.getSlotState(10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetOccupiedSlots() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        assertThat(stocker.getOccupiedSlots()).isEmpty();

        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo3 = new Cargo("CARGO-003", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo1);
        stocker.load(cargo2);
        stocker.load(cargo3);

        // Remove middle cargo to create gap
        stocker.unloadFromSlot(1);

        List<Integer> occupied = stocker.getOccupiedSlots();

        assertThat(occupied).containsExactly(0, 2);
    }

    @Test
    void testGetEmptySlots() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 3, 1, types);

        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        stocker.load(cargo);

        List<Integer> empty = stocker.getEmptySlots();

        assertThat(empty).containsExactly(1, 2);
    }

    @Test
    void testFIFOBehavior() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo3 = new Cargo("CARGO-003", Cargo.CargoType.FOUP, 250.0);

        stocker.load(cargo1);
        stocker.load(cargo2);
        stocker.load(cargo3);

        // unload() returns from first occupied slot (FIFO by slot index)
        assertThat(stocker.unload()).isEqualTo(cargo1);
        assertThat(stocker.unload()).isEqualTo(cargo2);
        assertThat(stocker.unload()).isEqualTo(cargo3);
    }

    @Test
    void testSupportedTransportTypes() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT, TransportType.AGV);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        assertThat(stocker.supportsTransportType(TransportType.OHT)).isTrue();
        assertThat(stocker.supportsTransportType(TransportType.AGV)).isTrue();
        assertThat(stocker.supportsTransportType(TransportType.HUMAN)).isFalse();
    }

    @Test
    void testSupportsTransportTypes() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT, TransportType.AGV);
        Stocker stocker = new Stocker("STOCKER-001", "Test", pos, 10, 1, types);

        List<TransportType> required1 = Arrays.asList(TransportType.OHT);
        List<TransportType> required2 = Arrays.asList(TransportType.AGV, TransportType.HUMAN);
        List<TransportType> required3 = Arrays.asList(TransportType.HUMAN);

        assertThat(stocker.supportsTransportTypes(required1)).isTrue();
        assertThat(stocker.supportsTransportTypes(required2)).isTrue(); // AGV in intersection
        assertThat(stocker.supportsTransportTypes(required3)).isFalse();
    }

    @Test
    void testLoadWhenFullTriggersOnCapacityFull() {
        Position pos = new Position(0.0, 0.0);
        List<TransportType> types = Arrays.asList(TransportType.OHT);

        // Create a testable Stocker that exposes onCapacityFull() call
        TestableStocker stocker = new TestableStocker("STOCKER-001", "Test", pos, 1, 1, types);
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        // First load should succeed
        assertThat(stocker.load(cargo1)).isTrue();
        assertThat(stocker.onCapacityFullCalled).isFalse();

        // Second load when full should trigger onCapacityFull()
        assertThat(stocker.load(cargo2)).isFalse();
        assertThat(stocker.onCapacityFullCalled).isTrue();
    }

    /**
     * Testable Stocker subclass that exposes when onCapacityFull() is called.
     */
    private static class TestableStocker extends Stocker {
        boolean onCapacityFullCalled = false;

        public TestableStocker(String id, String name, Position position, int capacity,
                               int craneCount, List<TransportType> supportedTransportTypes) {
            super(id, name, position, capacity, craneCount, supportedTransportTypes);
        }

        @Override
        protected void onCapacityFull() {
            onCapacityFullCalled = true;
            super.onCapacityFull();
        }
    }
}
