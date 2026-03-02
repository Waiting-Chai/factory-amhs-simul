package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.vehicle.AGVVehicle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for dual-layer application order (REQ-TC-008).
 * <p>
 * Tests that vehicles request ControlPoint before Edge, and failures
 * don't occupy any resources (avoiding deadlock).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class DualLayerApplicationTest {

    @Test
    void testControlPointRequestedBeforeEdge() {
        // Given: A network with ControlPoint and Edge
        NetworkGraph graph = new NetworkGraph();
        Node nodeA = new Node("NODE-A", 0.0, 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 10.0, 0.0, 0.0);

        graph.addNode(nodeA);
        graph.addNode(nodeB);

        Edge edge = new Edge("EDGE-AB", "NODE-A", "NODE-B", 10.0, 1);
        graph.addEdge(edge);

        ControlPoint cp = new ControlPoint("CP-A", "NODE-A", 1);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double currentTime = 0.0;

        // When: Vehicle requests to traverse (requires both ControlPoint and Edge)
        // TODO: Implement dual-layer application logic
        // This test documents expected behavior:
        // 1. ControlPoint should be requested FIRST
        // 2. Edge should be requested SECOND
        // 3. If ControlPoint fails, Edge should NOT be requested
        // 4. If Edge fails, ControlPoint should be released

        // For now, manually verify the order
        boolean cpGranted = cp.requestEntry(vehicle, currentTime);
        assertThat(cpGranted).isTrue();

        // Then: Both should be acquired
        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        // Edge reservation logic not yet implemented
    }

    @Test
    void testFailureWhenControlPointFull() {
        // Given: A full ControlPoint and available Edge
        ControlPoint cp = new ControlPoint("CP-A", "NODE-A", 1);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle1 = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        AGVVehicle vehicle2 = new AGVVehicle("V-2", "Vehicle 2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double currentTime = 0.0;

        // Occupy the ControlPoint
        cp.requestEntry(vehicle1, currentTime);
        assertThat(cp.isFull()).isTrue();

        // When: Second vehicle requests both ControlPoint and Edge
        // TODO: Implement dual-layer application logic
        boolean cpGranted = cp.requestEntry(vehicle2, currentTime);

        // Then: ControlPoint request should fail
        assertThat(cpGranted).isFalse();
        // And: Edge should NOT be occupied (since ControlPoint failed)
        // This prevents deadlock where one resource is held while waiting for another
    }

    @Test
    void testAtomicResourceAcquisition() {
        // Given: Available ControlPoint and Edge
        ControlPoint cp = new ControlPoint("CP-A", "NODE-A", 1);
        NetworkGraph graph = new NetworkGraph();

        Node nodeA = new Node("NODE-A", 0.0, 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 10.0, 0.0, 0.0);
        graph.addNode(nodeA);
        graph.addNode(nodeB);

        Edge edge = new Edge("EDGE-AB", "NODE-A", "NODE-B", 10.0, 1);
        graph.addEdge(edge);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double currentTime = 0.0;

        // When: Vehicle requests both resources atomically
        // TODO: Implement atomic acquisition logic
        boolean cpGranted = cp.requestEntry(vehicle, currentTime);
        // Edge reservation not yet implemented

        // Then: Both should be acquired
        assertThat(cpGranted).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);

        // If acquisition fails, NO resources should be held
        // This is critical for deadlock prevention
    }

    @Test
    void testReleaseOrderShouldBeReverse() {
        // Given: A vehicle occupying both ControlPoint and Edge
        ControlPoint cp = new ControlPoint("CP-A", "NODE-A", 1);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double currentTime = 0.0;
        cp.requestEntry(vehicle, currentTime);
        // Edge reservation not yet implemented

        // When: Vehicle releases resources
        // TODO: Implement dual-layer release logic
        // Expected order: Edge FIRST, then ControlPoint (reverse of acquisition)
        cp.release(vehicle);

        // Then: All resources should be released
        assertThat(cp.getCurrentLoad()).isEqualTo(0);
        assertThat(cp.isEmpty()).isTrue();
    }

    @Test
    void testResourceAcquisitionWithPartialFailure() {
        // Given: ControlPoint available, Edge at capacity
        ControlPoint cp = new ControlPoint("CP-A", "NODE-A", 1);

        NetworkGraph graph = new NetworkGraph();
        Node nodeA = new Node("NODE-A", 0.0, 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 10.0, 0.0, 0.0);
        graph.addNode(nodeA);
        graph.addNode(nodeB);

        Edge edge = new Edge("EDGE-AB", "NODE-A", "NODE-B", 10.0, 1);
        graph.addEdge(edge);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle1 = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        AGVVehicle vehicle2 = new AGVVehicle("V-2", "Vehicle 2", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double currentTime = 0.0;

        // Vehicle 1 occupies both
        cp.requestEntry(vehicle1, currentTime);
        // Vehicle 1 also occupies Edge (not yet implemented)

        // When: Vehicle 2 requests both resources
        boolean cpGranted = cp.requestEntry(vehicle2, currentTime + 1.0);

        // Then: Both requests should fail
        assertThat(cpGranted).isFalse();
        // And: No resources should be partially occupied
        // This is the "all or nothing" principle for deadlock prevention
    }
}
