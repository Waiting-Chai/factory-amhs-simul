package com.semi.simlogistics.scheduler.pool;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TDD tests for VehiclePool (REQ-DS-007).
 * Tests vehicle pool management and filtering capabilities.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("VehiclePool Tests (REQ-DS-007)")
class VehiclePoolTest {

    private List<Vehicle> vehicles;
    private VehiclePool vehiclePool;

    @BeforeEach
    void setUp() {
        vehicles = new ArrayList<>();
        vehiclePool = new VehiclePool(vehicles);
    }

    // ========== Test Helpers ==========

    private TestVehicle createVehicle(String id, EntityType type, VehicleState state, Position position) {
        TestVehicle vehicle = new TestVehicle(id, "Vehicle " + id, type, position);
        vehicle.setState(state);
        return vehicle;
    }

    /**
     * Test implementation of Vehicle for testing.
     */
    private static class TestVehicle extends Vehicle {
        protected TestVehicle(String id, String name, EntityType type, Position position) {
            super(id, name, type, position, null);
        }

        @Override
        protected Object run(com.semi.jSimul.core.Process.ProcessContext ctx) throws Exception {
            return null;
        }

        @Override
        public TransportType getTransportType() {
            // Map EntityType to TransportType for testing
            return switch (type()) {
                case OHT_VEHICLE -> TransportType.OHT;
                case AGV_VEHICLE -> TransportType.AGV;
                case OPERATOR -> TransportType.HUMAN;
                case CONVEYOR -> TransportType.CONVEYOR;
                default -> throw new IllegalArgumentException("No TransportType for: " + type());
            };
        }
    }

    @Nested
    @DisplayName("Available Vehicle Query Tests (REQ-DS-007)")
    class AvailableVehicleQueryTests {

        @Test
        @DisplayName("Should return all IDLE vehicles")
        void shouldReturnAllIdleVehicles() {
            // Given: Mixed state vehicles
            TestVehicle idleVehicle1 = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            TestVehicle idleVehicle2 = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            TestVehicle busyVehicle = createVehicle("V-003", EntityType.AGV_VEHICLE, VehicleState.MOVING, new Position(20, 20, 0));
            TestVehicle chargingVehicle = createVehicle("V-004", EntityType.AGV_VEHICLE, VehicleState.CHARGING, new Position(30, 30, 0));

            vehicles.add(idleVehicle1);
            vehicles.add(busyVehicle);
            vehicles.add(idleVehicle2);
            vehicles.add(chargingVehicle);

            // When: Query available vehicles
            List<Vehicle> available = vehiclePool.getAvailableVehicles();

            // Then: Should return only IDLE vehicles
            assertThat(available).hasSize(2);
            assertThat(available).containsExactly(idleVehicle1, idleVehicle2);
        }

        @Test
        @DisplayName("Should return empty list when no vehicles are IDLE")
        void shouldReturnEmptyListWhenNoVehiclesAreIdle() {
            // Given: All vehicles in non-IDLE states
            vehicles.add(createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.MOVING, new Position(0, 0, 0)));
            vehicles.add(createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.CHARGING, new Position(10, 10, 0)));

            // When: Query available vehicles
            List<Vehicle> available = vehiclePool.getAvailableVehicles();

            // Then: Should return empty list
            assertThat(available).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when pool is empty")
        void shouldReturnEmptyListWhenPoolIsEmpty() {
            // Given: Empty vehicle pool
            // (vehicles list is empty)

            // When: Query available vehicles
            List<Vehicle> available = vehiclePool.getAvailableVehicles();

            // Then: Should return empty list
            assertThat(available).isEmpty();
        }
    }

    @Nested
    @DisplayName("Transport Type Filtering Tests (REQ-DS-007)")
    class TransportTypeFilteringTests {

        @Test
        @DisplayName("Should filter available vehicles by transport type - AGV")
        void shouldFilterAvailableVehiclesByTransportTypeAGV() {
            // Given: Mixed vehicle types
            TestVehicle agv1 = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            TestVehicle agv2 = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            TestVehicle oht = createVehicle("V-003", EntityType.OHT_VEHICLE, VehicleState.IDLE, new Position(20, 20, 0));
            TestVehicle busyAgv = createVehicle("V-004", EntityType.AGV_VEHICLE, VehicleState.MOVING, new Position(30, 30, 0));

            vehicles.add(agv1);
            vehicles.add(oht);
            vehicles.add(agv2);
            vehicles.add(busyAgv);

            // When: Query available AGV vehicles
            List<Vehicle> availableAgv = vehiclePool.getAvailableVehiclesByTransportType(TransportType.AGV);

            // Then: Should return only IDLE AGV vehicles
            assertThat(availableAgv).hasSize(2);
            assertThat(availableAgv).containsExactly(agv1, agv2);
        }

        @Test
        @DisplayName("Should filter available vehicles by transport type - OHT")
        void shouldFilterAvailableVehiclesByTransportTypeOHT() {
            // Given: Mixed vehicle types
            TestVehicle oht1 = createVehicle("V-001", EntityType.OHT_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            TestVehicle agv = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            TestVehicle oht2 = createVehicle("V-003", EntityType.OHT_VEHICLE, VehicleState.IDLE, new Position(20, 20, 0));

            vehicles.add(oht1);
            vehicles.add(agv);
            vehicles.add(oht2);

            // When: Query available OHT vehicles
            List<Vehicle> availableOht = vehiclePool.getAvailableVehiclesByTransportType(TransportType.OHT);

            // Then: Should return only IDLE OHT vehicles
            assertThat(availableOht).hasSize(2);
            assertThat(availableOht).containsExactly(oht1, oht2);
        }

        @Test
        @DisplayName("Should return empty list when no vehicles match transport type")
        void shouldReturnEmptyListWhenNoVehiclesMatchTransportType() {
            // Given: Only AGV vehicles
            vehicles.add(createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0)));

            // When: Query OHT vehicles
            List<Vehicle> availableOht = vehiclePool.getAvailableVehiclesByTransportType(TransportType.OHT);

            // Then: Should return empty list
            assertThat(availableOht).isEmpty();
        }

        @Test
        @DisplayName("Should throw NPE for null transport type")
        void shouldThrowNPEForNullTransportType() {
            assertThrows(NullPointerException.class, () ->
                    vehiclePool.getAvailableVehiclesByTransportType(null));
        }
    }

    @Nested
    @DisplayName("Entity Type Filtering Tests (REQ-DS-007)")
    class EntityTypeFilteringTests {

        @Test
        @DisplayName("Should filter available vehicles by entity type")
        void shouldFilterAvailableVehiclesByEntityType() {
            // Given: Mixed entity types
            TestVehicle agv1 = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            TestVehicle agv2 = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            TestVehicle oht = createVehicle("V-003", EntityType.OHT_VEHICLE, VehicleState.IDLE, new Position(20, 20, 0));
            TestVehicle busyAgv = createVehicle("V-004", EntityType.AGV_VEHICLE, VehicleState.MOVING, new Position(30, 30, 0));

            vehicles.add(agv1);
            vehicles.add(oht);
            vehicles.add(agv2);
            vehicles.add(busyAgv);

            // When: Query available AGV vehicles by entity type
            List<Vehicle> availableAgv = vehiclePool.getAvailableVehiclesByEntityType(EntityType.AGV_VEHICLE);

            // Then: Should return only IDLE AGV vehicles
            assertThat(availableAgv).hasSize(2);
            assertThat(availableAgv).containsExactly(agv1, agv2);
        }

        @Test
        @DisplayName("Should return empty list when no vehicles match entity type")
        void shouldReturnEmptyListWhenNoVehiclesMatchEntityType() {
            // Given: Only AGV vehicles
            vehicles.add(createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0)));

            // When: Query OHT vehicles
            List<Vehicle> availableOht = vehiclePool.getAvailableVehiclesByEntityType(EntityType.OHT_VEHICLE);

            // Then: Should return empty list
            assertThat(availableOht).isEmpty();
        }

        @Test
        @DisplayName("Should throw NPE for null entity type")
        void shouldThrowNPEForNullEntityType() {
            assertThrows(NullPointerException.class, () ->
                    vehiclePool.getAvailableVehiclesByEntityType(null));
        }
    }

    @Nested
    @DisplayName("Live View Tests (REQ-DS-007)")
    class LiveViewTests {

        @Test
        @DisplayName("Should provide live view of vehicle state changes")
        void shouldProvideLiveViewOfVehicleStateChanges() {
            // Given: A vehicle in IDLE state
            TestVehicle vehicle = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            vehicles.add(vehicle);

            // When: Query available vehicles initially
            List<Vehicle> available1 = vehiclePool.getAvailableVehicles();
            assertThat(available1).hasSize(1);

            // When: Vehicle state changes to MOVING
            vehicle.setState(VehicleState.MOVING);

            // Then: Live view should reflect the change
            List<Vehicle> available2 = vehiclePool.getAvailableVehicles();
            assertThat(available2).isEmpty();
        }

        @Test
        @DisplayName("Should return live reference to vehicle list")
        void shouldReturnLiveReferenceToVehicleList() {
            // Given: Pool with one vehicle
            TestVehicle vehicle = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            vehicles.add(vehicle);

            // When: Get all vehicles
            List<Vehicle> allVehicles = vehiclePool.getAllVehicles();

            // Then: Should be the same list reference (live view)
            assertThat(allVehicles).isSameAs(vehicles);

            // When: Add new vehicle to original list
            TestVehicle newVehicle = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            vehicles.add(newVehicle);

            // Then: Live reference should see the change
            assertThat(allVehicles).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Manual Assignment Tests (REQ-DS-005)")
    class ManualAssignmentTests {

        @Test
        @DisplayName("Should mark vehicle as manually assigned")
        void shouldMarkVehicleAsManuallyAssigned() {
            // Given: A vehicle
            TestVehicle vehicle = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            vehicles.add(vehicle);

            // When: Mark vehicle as manually assigned
            boolean marked = vehiclePool.markManuallyAssigned("V-001");

            // Then: Vehicle should be marked
            assertThat(marked).isTrue();
            assertThat(vehiclePool.isManuallyAssigned("V-001")).isTrue();
        }

        @Test
        @DisplayName("Should return false when marking non-existent vehicle")
        void shouldReturnFalseWhenMarkingNonExistentVehicle() {
            // When: Mark non-existent vehicle
            boolean marked = vehiclePool.markManuallyAssigned("V-NONEXISTENT");

            // Then: Should return false
            assertThat(marked).isFalse();
        }

        @Test
        @DisplayName("Should unmark manually assigned vehicle")
        void shouldUnmarkManuallyAssignedVehicle() {
            // Given: A manually assigned vehicle
            TestVehicle vehicle = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            vehicles.add(vehicle);
            vehiclePool.markManuallyAssigned("V-001");

            // When: Unmark vehicle
            boolean unmarked = vehiclePool.unmarkManuallyAssigned("V-001");

            // Then: Vehicle should no longer be marked
            assertThat(unmarked).isTrue();
            assertThat(vehiclePool.isManuallyAssigned("V-001")).isFalse();
        }

        @Test
        @DisplayName("Should exclude manually assigned vehicles from available list")
        void shouldExcludeManuallyAssignedVehiclesFromAvailableList() {
            // Given: Two IDLE vehicles
            TestVehicle vehicle1 = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            TestVehicle vehicle2 = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            vehicles.add(vehicle1);
            vehicles.add(vehicle2);

            // When: Mark vehicle1 as manually assigned
            vehiclePool.markManuallyAssigned("V-001");

            // Then: Only vehicle2 should be available
            List<Vehicle> available = vehiclePool.getAvailableVehicles();
            assertThat(available).hasSize(1);
            assertThat(available).containsExactly(vehicle2);
        }
    }

    @Nested
    @DisplayName("Task Lock Tests (REQ-DS-005)")
    class TaskLockTests {

        @Test
        @DisplayName("Should lock task to prevent automatic dispatch")
        void shouldLockTaskToPreventAutomaticDispatch() {
            // When: Lock a task
            vehiclePool.lockTask("TASK-001");

            // Then: Task should be locked
            assertThat(vehiclePool.isTaskLocked("TASK-001")).isTrue();
        }

        @Test
        @DisplayName("Should unlock task")
        void shouldUnlockTask() {
            // Given: A locked task
            vehiclePool.lockTask("TASK-001");

            // When: Unlock task
            boolean unlocked = vehiclePool.unlockTask("TASK-001");

            // Then: Task should no longer be locked
            assertThat(unlocked).isTrue();
            assertThat(vehiclePool.isTaskLocked("TASK-001")).isFalse();
        }

        @Test
        @DisplayName("Should return false when unlocking non-locked task")
        void shouldReturnFalseWhenUnlockingNonLockedTask() {
            // When: Unlock non-locked task
            boolean unlocked = vehiclePool.unlockTask("TASK-001");

            // Then: Should return false
            assertThat(unlocked).isFalse();
        }
    }

    @Nested
    @DisplayName("Utility Tests")
    class UtilityTests {

        @Test
        @DisplayName("Should find vehicle by ID")
        void shouldFindVehicleById() {
            // Given: Vehicles in pool
            TestVehicle vehicle = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            vehicles.add(vehicle);

            // When: Find vehicle by ID
            Vehicle found = vehiclePool.getVehicleById("V-001");

            // Then: Should return the vehicle
            assertThat(found).isNotNull();
            assertThat(found.id()).isEqualTo("V-001");
        }

        @Test
        @DisplayName("Should return null when vehicle ID not found")
        void shouldReturnNullWhenVehicleIdNotFound() {
            // When: Find non-existent vehicle
            Vehicle found = vehiclePool.getVehicleById("V-NONEXISTENT");

            // Then: Should return null
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Should return null for null vehicle ID")
        void shouldReturnNullForNullVehicleId() {
            // When: Find with null ID
            Vehicle found = vehiclePool.getVehicleById(null);

            // Then: Should return null
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Should clear all manual assignments and locks")
        void shouldClearAllManualAssignmentsAndLocks() {
            // Given: Manual assignments and locks
            TestVehicle vehicle = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            vehicles.add(vehicle);
            vehiclePool.markManuallyAssigned("V-001");
            vehiclePool.lockTask("TASK-001");

            // When: Clear manual assignments
            vehiclePool.clearManualAssignments();

            // Then: All should be cleared
            assertThat(vehiclePool.getManuallyAssignedCount()).isEqualTo(0);
            assertThat(vehiclePool.getLockedTaskCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should track counts correctly")
        void shouldTrackCountsCorrectly() {
            // Given: Multiple manual assignments and locks
            TestVehicle v1 = createVehicle("V-001", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(0, 0, 0));
            TestVehicle v2 = createVehicle("V-002", EntityType.AGV_VEHICLE, VehicleState.IDLE, new Position(10, 10, 0));
            vehicles.add(v1);
            vehicles.add(v2);

            vehiclePool.markManuallyAssigned("V-001");
            vehiclePool.markManuallyAssigned("V-002");
            vehiclePool.lockTask("TASK-001");
            vehiclePool.lockTask("TASK-002");
            vehiclePool.lockTask("TASK-003");

            // Then: Counts should be correct
            assertThat(vehiclePool.getManuallyAssignedCount()).isEqualTo(2);
            assertThat(vehiclePool.getLockedTaskCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw NPE for null vehicle list")
        void shouldThrowNPEForNullVehicleList() {
            assertThrows(NullPointerException.class, () ->
                    new VehiclePool(null));
        }
    }
}
