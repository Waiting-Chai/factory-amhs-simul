package com.semi.simlogistics.control.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Edge.
 * <p>
 * Tests bidirectional edge rules: bidirectional paths = two separate directed edges.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class EdgeTest {

    @Test
    void testBidirectionalPathRequiresTwoSeparateEdges() {
        // Given: Two nodes A and B
        String nodeIdA = "NODE-A";
        String nodeIdB = "NODE-B";
        double length = 100.0;

        // When: Create two edges with opposite directions
        Edge edgeAB = new Edge("EDGE-AB", nodeIdA, nodeIdB, length);
        Edge edgeBA = new Edge("EDGE-BA", nodeIdB, nodeIdA, length);

        // Then: Each edge should have unique ID
        assertThat(edgeAB.getId()).isNotEqualTo(edgeBA.getId());
        assertThat(edgeAB.getId()).isEqualTo("EDGE-AB");
        assertThat(edgeBA.getId()).isEqualTo("EDGE-BA");

        // And: Directions should be opposite
        assertThat(edgeAB.getFromNodeId()).isEqualTo(nodeIdA);
        assertThat(edgeAB.getToNodeId()).isEqualTo(nodeIdB);
        assertThat(edgeBA.getFromNodeId()).isEqualTo(nodeIdB);
        assertThat(edgeBA.getToNodeId()).isEqualTo(nodeIdA);

        // And: Both edges can coexist (different edgeId)
        assertThat(edgeAB).isNotEqualTo(edgeBA);
    }

    @Test
    void testBidirectionalEdgesInNetworkGraph() {
        // Given: A network graph
        NetworkGraph graph = new NetworkGraph();

        // When: Add bidirectional edges between two nodes
        Node nodeA = new Node("NODE-A", 0.0, 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 100.0, 0.0, 0.0);

        graph.addNode(nodeA);
        graph.addNode(nodeB);

        Edge edgeAB = new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0);
        Edge edgeBA = new Edge("EDGE-BA", "NODE-B", "NODE-A", 100.0);

        graph.addEdge(edgeAB);
        graph.addEdge(edgeBA);

        // Then: Both edges should exist in graph
        assertThat(graph.getEdge("EDGE-AB")).isNotNull();
        assertThat(graph.getEdge("EDGE-BA")).isNotNull();

        // And: Graph should be connected in both directions
        assertThat(graph.isConnected("NODE-A", "NODE-B")).isTrue();
        assertThat(graph.isConnected("NODE-B", "NODE-A")).isTrue();
    }

    @Test
    void testEdgeEqualityById() {
        // Given: Two edges with same ID but different properties
        Edge edge1 = new Edge("EDGE-1", "NODE-A", "NODE-B", 100.0, 1);
        Edge edge2 = new Edge("EDGE-1", "NODE-C", "NODE-D", 200.0, 2);

        // Then: They should be equal (ID-based equality)
        assertThat(edge1).isEqualTo(edge2);
        assertThat(edge1.hashCode()).isEqualTo(edge2.hashCode());
    }

    @Test
    void testEdgeWithCapacity() {
        // Given: An edge with capacity > 1
        Edge edge = new Edge("EDGE-1", "NODE-A", "NODE-B", 100.0, 3);

        // Then: Capacity should be set correctly
        assertThat(edge.getCapacity()).isEqualTo(3);
        assertThat(edge.getLength()).isEqualTo(100.0);
    }

    @Test
    void testEdgeDefaultCapacity() {
        // Given: An edge created without specifying capacity
        Edge edge = new Edge("EDGE-1", "NODE-A", "NODE-B", 100.0);

        // Then: Default capacity should be 1
        assertThat(edge.getCapacity()).isEqualTo(1);
    }
}
