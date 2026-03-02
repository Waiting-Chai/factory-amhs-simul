package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.vehicle.AGVVehicle;
import com.semi.simlogistics.vehicle.Vehicle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ControlPoint.
 * <p>
 * Tests capacity control and priority-based arbitration (REQ-TC-005).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class ControlPointTest {

    @Test
    void testControlPointCapacityLimit() {
        // Given: A ControlPoint with capacity 1
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "Vehicle 2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // When: First vehicle requests entry
        boolean granted1 = cp.requestEntry(vehicle1, currentTime);

        // Then: Request should be granted
        assertThat(granted1).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(cp.isFull()).isTrue();

        // When: Second vehicle requests entry
        boolean granted2 = cp.requestEntry(vehicle2, currentTime);

        // Then: Request should be denied
        assertThat(granted2).isFalse();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(cp.getWaitingVehicleIds()).contains("V-2");
    }

    @Test
    void testControlPointRelease() {
        // Given: A ControlPoint with one vehicle
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "Vehicle 2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime);

        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(cp.getWaitingVehicleIds()).contains("V-2");

        // When: First vehicle releases and grants to next waiting vehicle
        String grantedVehicleId = cp.releaseAndGetNext(vehicle1, currentTime + 1.0);

        // Then: V2 should be automatically granted (no need to request again)
        assertThat(grantedVehicleId).isEqualTo("V-2");
        assertThat(cp.getCurrentLoad()).isEqualTo(1); // V2 now occupying
        assertThat(cp.getWaitingVehicleIds()).doesNotContain("V-2"); // V2 removed from waiting
        assertThat(cp.isEmpty()).isFalse(); // Not empty - V2 is occupying
    }

    @Test
    void testControlPointDefaultCapacity() {
        // Given: A ControlPoint created without specifying capacity
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A");

        // Then: Default capacity should be 1
        assertThat(cp.getCapacity()).isEqualTo(1);
        assertThat(cp.getAvailableCapacity()).isEqualTo(1);
    }

    @Test
    void testControlPointCapacityGreaterThanOne() {
        // Given: A ControlPoint with capacity 3
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 3);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "Vehicle 2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "Vehicle 3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle4 = new AGVVehicle("V-4", "Vehicle 4", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // When: Three vehicles request entry
        boolean granted1 = cp.requestEntry(vehicle1, currentTime);
        boolean granted2 = cp.requestEntry(vehicle2, currentTime);
        boolean granted3 = cp.requestEntry(vehicle3, currentTime);

        // Then: All should be granted
        assertThat(granted1).isTrue();
        assertThat(granted2).isTrue();
        assertThat(granted3).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(3);
        assertThat(cp.isFull()).isTrue();

        // When: Fourth vehicle requests entry
        boolean granted4 = cp.requestEntry(vehicle4, currentTime);

        // Then: Should be denied
        assertThat(granted4).isFalse();
        assertThat(cp.getWaitingVehicleIds()).contains("V-4");
    }

    @Test
    void testControlPointPriorityArbitration() {
        // Given: A ControlPoint with capacity 1
        // And: Two vehicles with different priorities
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        PriorityManager priorityManager = new PriorityManager();
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle lowPriorityVehicle = new AGVVehicle("V-LOW", "Low Priority", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle highPriorityVehicle = new AGVVehicle("V-HIGH", "High Priority", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // Set priorities directly
        lowPriorityVehicle.setPriority(1);  // Low priority
        highPriorityVehicle.setPriority(10); // High priority

        // When: Low priority vehicle occupies the control point
        boolean lowGranted = cp.requestEntry(lowPriorityVehicle, currentTime);
        assertThat(lowGranted).isTrue();

        // And: High priority vehicle requests entry (gets queued)
        boolean highGranted = cp.requestEntry(highPriorityVehicle, currentTime + 1.0);
        assertThat(highGranted).isFalse();
        assertThat(cp.getWaitingVehicleIds()).contains("V-HIGH");

        // Then: When control point releases, it should return the top priority waiting vehicle
        String topVehicleId = cp.releaseAndGetNext(lowPriorityVehicle, currentTime + 2.0);
        // TDD: This assertion will FAIL initially because ControlPoint doesn't implement priority arbitration
        assertThat(topVehicleId).isEqualTo("V-HIGH");
        // After releaseAndGetNext(), the grant process is complete, so currentLoad should be 1
        assertThat(cp.getCurrentLoad()).isEqualTo(1); // V-HIGH is now occupying
    }

    @Test
    void testControlPointWaitingTimeTracking() {
        // Given: A ControlPoint with capacity 1
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double startTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "Vehicle 2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // When: Vehicle 1 occupies, vehicle 2 waits
        cp.requestEntry(vehicle1, startTime);
        cp.requestEntry(vehicle2, startTime);

        // Then: Waiting time should be tracked correctly
        double waitTimeAtStart = cp.getWaitingTime("V-2", startTime);
        assertThat(waitTimeAtStart).isEqualTo(0.0);

        // When: 30 seconds pass
        double currentTime = startTime + 30.0;
        double waitTimeAfter30s = cp.getWaitingTime("V-2", currentTime);
        assertThat(waitTimeAfter30s).isEqualTo(30.0);
    }

    @Test
    void testControlPointNodeIdBinding() {
        // Given: A ControlPoint
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);

        // Then: Should be bound to specific node
        assertThat(cp.getAtNodeId()).isEqualTo("NODE-A");
        assertThat(cp.getId()).isEqualTo("CP-1");
    }

    @Test
    void testRequestEntryWithNullVehicle() {
        // Given: A ControlPoint
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);

        // When: Request entry with null vehicle
        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> cp.requestEntry(null, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vehicle cannot be null");
    }

    @Test
    void testReleaseWithNullVehicle() {
        // Given: A ControlPoint
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);

        // When: Release with null vehicle
        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> cp.release(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vehicle cannot be null");
    }

    @Test
    void testPriorityAgingWithWaitingTime() {
        // Given: PriorityManager with default aging parameters
        PriorityManager priorityManager = new PriorityManager();
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle blockingVehicle = new AGVVehicle("V-BLOCK", "Blocking", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle waitingVehicle = new AGVVehicle("V-WAIT", "Waiting", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // Set base priorities
        blockingVehicle.setPriority(5);
        waitingVehicle.setPriority(5);

        // When: Blocking vehicle occupies, waiting vehicle waits
        cp.requestEntry(blockingVehicle, currentTime);
        cp.requestEntry(waitingVehicle, currentTime);

        // Then: Initial effective priority should be base priority
        int initialEffectivePriority = priorityManager.calculateEffectivePriority(
            waitingVehicle, currentTime, currentTime);
        assertThat(initialEffectivePriority).isEqualTo(5);

        // When: 30 seconds pass (one aging step)
        double after30s = currentTime + 30.0;
        int effectiveAfter30s = priorityManager.calculateEffectivePriority(
            waitingVehicle, currentTime, after30s);
        // effectivePriority = 5 + floor(30/30) * 1 = 5 + 1 = 6
        assertThat(effectiveAfter30s).isEqualTo(6);

        // When: 60 seconds pass (two aging steps)
        double after60s = currentTime + 60.0;
        int effectiveAfter60s = priorityManager.calculateEffectivePriority(
            waitingVehicle, currentTime, after60s);
        // effectivePriority = 5 + floor(60/30) * 1 = 5 + 2 = 7
        assertThat(effectiveAfter60s).isEqualTo(7);

        // When: 150 seconds pass (max boost reached)
        double after150s = currentTime + 150.0;
        int effectiveAfter150s = priorityManager.calculateEffectivePriority(
            waitingVehicle, currentTime, after150s);
        // effectivePriority = 5 + floor(150/30) * 1 = 5 + 5 = 10 (capped at max priority)
        assertThat(effectiveAfter150s).isEqualTo(10);
    }

    @Test
    void testPriorityAgingWithMaxBoost() {
        // Given: PriorityManager with custom parameters
        PriorityManager priorityManager = new PriorityManager(30.0, 2, 5);
        Vehicle vehicle = new AGVVehicle("V-1", "Vehicle", new Position(0, 0, 0), 1.0, 0.01, 0.1, 1.0, 0.2);
        vehicle.setPriority(1);

        // When: Waiting for very long time
        double waitTime = 300.0; // 10 aging steps
        int effectivePriority = priorityManager.calculateEffectivePriority(vehicle, waitTime);

        // Then: Should be capped at max boost
        // Base: 1, Boost: floor(300/30)*2 = 10*2 = 20, Max: 5, Result: 1+5=6
        assertThat(effectivePriority).isEqualTo(6);
    }

    @Test
    void testPriorityArbitrationOrder() {
        // Given: ControlPoint with capacity 1 and PriorityManager
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        PriorityManager priorityManager = new PriorityManager();
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // Set base priorities
        vehicle1.setPriority(5); // Medium
        vehicle2.setPriority(10); // High
        vehicle3.setPriority(1); // Low

        // When: Vehicle1 occupies, V2 and V3 wait
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle3, currentTime + 10.0); // Low priority, waits longer
        cp.requestEntry(vehicle2, currentTime + 20.0); // High priority, waits shorter

        // Then: Calculate effective priorities after 30 seconds
        double evalTime = currentTime + 30.0;
        int v1Base = priorityManager.calculateEffectivePriority(vehicle1, 0);
        int v2Effective = priorityManager.calculateEffectivePriority(vehicle2, currentTime + 20.0, evalTime);
        int v3Effective = priorityManager.calculateEffectivePriority(vehicle3, currentTime + 10.0, evalTime);

        // Vehicle2 (base 10, short wait) should have highest effective priority
        assertThat(v2Effective).isEqualTo(10);
        // Vehicle3 (base 1, long wait) gets aging boost
        // Wait time = 30 - 10 = 20s, aging not yet triggered (needs 30s)
        assertThat(v3Effective).isEqualTo(1); // 1 + floor(20/30)*1 = 1 + 0 = 1

        // Priority order: V2 (10) > V1 (5) > V3 (1-2)
    }

    @Test
    void testFIFOOrderWithSamePriority() {
        // Given: ControlPoint with capacity 1
        // And: Multiple vehicles with SAME priority
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // All same priority
        vehicle1.setPriority(5);
        vehicle2.setPriority(5);
        vehicle3.setPriority(5);

        // When: Vehicle1 occupies, V2 and V3 wait (in that order)
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime + 1.0);
        cp.requestEntry(vehicle3, currentTime + 2.0);

        // Then: When Vehicle1 releases, V2 should be selected (FIFO)
        String nextVehicleId = cp.releaseAndGetNext(vehicle1, currentTime + 3.0);
        assertThat(nextVehicleId).isEqualTo("V-2"); // V2 waited first

        // When: V2 is granted and then releases, V3 should be next
        cp.requestEntry(vehicle2, currentTime + 3.0);
        String nextAfterV2 = cp.releaseAndGetNext(vehicle2, currentTime + 4.0);
        assertThat(nextAfterV2).isEqualTo("V-3");
    }

    @Test
    void testPriorityAgingArbitration() {
        // Given: ControlPoint with capacity 1
        // And: Two vehicles with different base priorities
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle lowPriorityVehicle = new AGVVehicle("V-LOW", "Low", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle highPriorityVehicle = new AGVVehicle("V-HIGH", "High", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // Set base priorities: low=3, high=8
        lowPriorityVehicle.setPriority(3);
        highPriorityVehicle.setPriority(8);

        // When: Low priority vehicle occupies
        cp.requestEntry(lowPriorityVehicle, currentTime);

        // And: High priority vehicle waits
        cp.requestEntry(highPriorityVehicle, currentTime + 1.0);

        // And: Low priority vehicle releases after 60 seconds
        // At this point, high priority vehicle's effective priority is still 8 (no aging needed)
        String nextVehicleId = cp.releaseAndGetNext(lowPriorityVehicle, currentTime + 60.0);

        // Then: High priority vehicle should be selected
        assertThat(nextVehicleId).isEqualTo("V-HIGH");
    }

    @Test
    void testMultipleVehiclesArbitration() {
        // Given: ControlPoint with capacity 1
        // And: Multiple vehicles with different priorities
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle4 = new AGVVehicle("V-4", "V4", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // Set different priorities
        vehicle1.setPriority(1);  // Lowest
        vehicle2.setPriority(10); // Highest
        vehicle3.setPriority(5);  // Medium
        vehicle4.setPriority(8);  // High

        // When: Vehicle1 occupies, others wait
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime + 1.0); // Priority 10
        cp.requestEntry(vehicle3, currentTime + 2.0); // Priority 5
        cp.requestEntry(vehicle4, currentTime + 3.0); // Priority 8

        // Then: When Vehicle1 releases, V2 (highest priority) should be selected
        String nextVehicleId = cp.releaseAndGetNext(vehicle1, currentTime + 4.0);
        assertThat(nextVehicleId).isEqualTo("V-2");
    }

    @Test
    void testReleaseAndGetNextWithNoWaitingVehicles() {
        // Given: ControlPoint with one vehicle
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // When: Vehicle occupies and then releases with no waiting vehicles
        cp.requestEntry(vehicle, currentTime);
        String nextVehicleId = cp.releaseAndGetNext(vehicle, currentTime + 1.0);

        // Then: Should return null
        assertThat(nextVehicleId).isNull();
    }

    @Test
    void testGrantedVehicleRemovedFromWaitingQueue() {
        // Given: ControlPoint with capacity 1
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // Set priorities
        vehicle1.setPriority(1);
        vehicle2.setPriority(10); // Highest
        vehicle3.setPriority(5);

        // When: V1 occupies, V2 and V3 wait
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle3, currentTime + 1.0);
        cp.requestEntry(vehicle2, currentTime + 2.0);

        // Then: Waiting queue should contain V2 and V3
        assertThat(cp.getWaitingVehicleIds()).contains("V-2", "V-3");

        // When: V1 releases and top vehicle is selected
        String topVehicleId = cp.releaseAndGetNext(vehicle1, currentTime + 3.0);
        assertThat(topVehicleId).isEqualTo("V-2");

        // TDD: This assertion will FAIL initially because selected vehicle is not removed from waiting queue
        // Then: V2 must be removed from waiting queue (granted)
        assertThat(cp.getWaitingVehicleIds()).doesNotContain("V-2");
        // And: V3 should still be waiting
        assertThat(cp.getWaitingVehicleIds()).contains("V-3");
    }

    @Test
    void testRandomTieBreakWithSamePriorityAndWaitTime() {
        // Given: ControlPoint with capacity 1
        // And: Multiple vehicles with SAME priority and SAME wait start time
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // All same priority
        vehicle1.setPriority(5);
        vehicle2.setPriority(5);
        vehicle3.setPriority(5);

        // When: Vehicle1 occupies, V2 and V3 wait at EXACTLY the same time
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime + 1.0);
        cp.requestEntry(vehicle3, currentTime + 1.0); // Same wait start time as V2

        // Then: When V1 releases, either V2 or V3 should be selected (non-deterministic due to random tie-break)
        String selected = cp.releaseAndGetNext(vehicle1, currentTime + 2.0);
        assertThat(selected).isIn("V-2", "V-3");
        // And: The selected vehicle must be removed from waiting queue
        assertThat(cp.getWaitingVehicleIds()).doesNotContain(selected);
    }

    @Test
    void testRandomTieBreakDeterministicWithDifferentSeeds() {
        // This test verifies that random tie-breaking works by using different random seeds
        // Note: Current implementation uses default Random(), so this test demonstrates the behavior
        // In production, we might want to inject Random with configurable seed for reproducibility

        // Given: ControlPoint with capacity 1
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle4 = new AGVVehicle("V-4", "V4", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // All same priority
        vehicle1.setPriority(5);
        vehicle2.setPriority(5);
        vehicle3.setPriority(5);
        vehicle4.setPriority(5);

        // When: V1 occupies, V2, V3, V4 wait at the same time
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime + 1.0);
        cp.requestEntry(vehicle3, currentTime + 1.0);
        cp.requestEntry(vehicle4, currentTime + 1.0);

        // Then: Release should select one of them (random)
        String selected = cp.releaseAndGetNext(vehicle1, currentTime + 2.0);
        assertThat(selected).isIn("V-2", "V-3", "V-4");
        // And: Only two vehicles should remain in waiting queue
        assertThat(cp.getWaitingVehicleIds()).hasSize(2);
    }

    @Test
    void testNoCuttingInLineWhenWaitingQueueExists() {
        // Given: ControlPoint with capacity 1
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        // When: V1 occupies, V2 waits (capacity full)
        cp.requestEntry(vehicle1, currentTime);
        boolean v2Granted = cp.requestEntry(vehicle2, currentTime + 1.0);
        assertThat(v2Granted).isFalse();
        assertThat(cp.getWaitingVehicleIds()).contains("V-2");

        // When: V1 releases (capacity available, but V2 is still waiting)
        cp.release(vehicle1);
        assertThat(cp.getCurrentLoad()).isEqualTo(0);
        assertThat(cp.getWaitingVehicleIds()).contains("V-2"); // V2 still waiting

        // TDD: This assertion will FAIL initially - V3 cuts in line
        // When: V3 tries to enter while V2 is waiting
        boolean v3Granted = cp.requestEntry(vehicle3, currentTime + 2.0);
        // Expected: V3 should wait (not cut in line) even though capacity is available
        assertThat(v3Granted).isFalse();
        assertThat(cp.getWaitingVehicleIds()).contains("V-3");
    }

    @Test
    void testReleaseAndGetNextCompletesGrantProcess() {
        // Given: ControlPoint with capacity 1
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        vehicle1.setPriority(1);
        vehicle2.setPriority(10);

        // When: V1 occupies, V2 waits
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime + 1.0);

        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(cp.getWaitingVehicleIds()).contains("V-2");

        // When: V1 releases and V2 is granted
        String grantedVehicleId = cp.releaseAndGetNext(vehicle1, currentTime + 2.0);
        assertThat(grantedVehicleId).isEqualTo("V-2");

        // Then: V2 should be in occupying vehicles
        assertThat(cp.getCurrentLoad()).isEqualTo(1); // Load should be 1 (V2 occupying)
        assertThat(cp.getWaitingVehicleIds()).doesNotContain("V-2"); // V2 removed from waiting
        // V2 should be in occupyingVehicles (we can't directly check, but load=1 indicates this)
    }

    @Test
    void testReleaseAndGetNextWithNonOccupyingVehicleDoesNotGrant() {
        // Given: ControlPoint with waiting vehicles
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);
        double currentTime = 0.0;

        Position pos = new Position(0.0, 0.0, 0.0);
        Vehicle vehicle1 = new AGVVehicle("V-1", "V1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle2 = new AGVVehicle("V-2", "V2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        Vehicle vehicle3 = new AGVVehicle("V-3", "V3", pos, 1.0, 0.01, 0.1, 1.0, 0.2); // Not occupying

        // When: V1 occupies, V2 waits
        cp.requestEntry(vehicle1, currentTime);
        cp.requestEntry(vehicle2, currentTime + 1.0);

        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(cp.getWaitingVehicleIds()).contains("V-2");

        // TDD: This assertion will FAIL initially - V3 is not occupying but grant still happens
        // When: Try to release with V3 (which is NOT occupying)
        String grantedVehicleId = cp.releaseAndGetNext(vehicle3, currentTime + 2.0);

        // Then: Should NOT grant anyone (V3 was not occupying, so no capacity freed)
        assertThat(grantedVehicleId).isNull();
        assertThat(cp.getCurrentLoad()).isEqualTo(1); // Load unchanged (V1 still occupying)
        assertThat(cp.getWaitingVehicleIds()).contains("V-2"); // V2 still waiting
    }

    // NOTE: Capacity boundary validation (currentLoad >= capacity check) is implemented
    // defensively in releaseAndGetNext(), but testing the actual overflow scenario would require:
    // - Reflection to manipulate private state, or
    // - Package-private test helpers, or
    // - Integration tests with concurrent access
    // The normal release path is already covered by testReleaseAndGetNextWithNoWaitingVehicles.
    // In production, the safety check prevents capacity overflow in edge cases (concurrent calls,
    // state corruption, etc.). This is a defense-in-depth measure that's difficult to unit test
    // without introducing test-only code paths.
}
