package com.semi.simlogistics.control.path;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.core.TransportType;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that validate our A* path planning against JGraphT's proven algorithms.
 * <p>
 * JGraphT is a well-tested graph library used here to validate the correctness of our
 * custom A* implementation. This test compares our AStarPathPlanner results against
 * JGraphT's Dijkstra implementation on identical graph structures.
 * <p>
 * Role of JGraphT in this project:
 * - Validation: Verify our path planning algorithms produce correct results
 * - Benchmarking: Compare performance against established implementations
 * - Not a replacement: Our custom implementation allows for vehicle-type-specific caching
 *   and integration with the simulation framework
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class JGraphTValidationTest {

    private NetworkGraph ourGraph;
    private AStarPathPlanner ourPlanner;
    private Graph<String, DefaultWeightedEdge> jgraphtGraph;
    private DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra;

    @BeforeEach
    void setUp() {
        ourGraph = new NetworkGraph();
        ourPlanner = new AStarPathPlanner(ourGraph);
        jgraphtGraph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        dijkstra = new DijkstraShortestPath<>(jgraphtGraph);
    }

    @Test
    void validateAStarAgainstJGraphTDijkstraOnLinearGraph() {
        // Given: A linear graph A -> B -> C -> D
        addLinearGraphToBoth();

        // When: Find path using our A* and JGraphT's Dijkstra
        var ourPath = ourPlanner.planPath("NODE-A", "NODE-D", TransportType.AGV);
        var jgraphtPath = dijkstra.getPath("NODE-A", "NODE-D");

        // Then: Paths should have same length and cost
        assertThat(ourPath.getNodeIds()).hasSize(4);
        assertThat(jgraphtPath.getLength()).isEqualTo(3); // 3 edges

        // Verify total distance matches
        double ourDistance = ourPath.getEstimatedTime();
        double jgraphtDistance = jgraphtPath.getWeight();
        assertThat(ourDistance).isCloseTo(jgraphtDistance, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    void validateAStarAgainstJGraphTDijkstraOnComplexGraph() {
        // Given: A complex graph with multiple paths
        addComplexGraphToBoth();

        // When: Find path using our A* and JGraphT's Dijkstra
        var ourPath = ourPlanner.planPath("START", "END", TransportType.AGV);
        var jgraphtPath = dijkstra.getPath("START", "END");

        // Then: Both should find the shortest path
        if (!ourPath.isEmpty() && jgraphtPath != null) {
            double ourDistance = ourPath.getEstimatedTime();
            double jgraphtDistance = jgraphtPath.getWeight();

            // Our A* should find the same shortest distance as Dijkstra
            assertThat(ourDistance).isCloseTo(jgraphtDistance, org.assertj.core.api.Assertions.within(0.001));
        }
    }

    @Test
    void validateAStarAgainstJGraphTDijkstraOnWeightedGraph() {
        // Given: A graph with varying edge weights
        addWeightedGraphToBoth();

        // When: Find path using our A* and JGraphT's Dijkstra
        var ourPath = ourPlanner.planPath("A", "F", TransportType.AGV);
        var jgraphtPath = dijkstra.getPath("A", "F");

        // Then: Both should choose the same optimal path
        assertThat(ourPath.isEmpty()).isFalse();
        assertThat(jgraphtPath).isNotNull();

        // Verify distances match
        double ourDistance = ourPath.getEstimatedTime();
        double jgraphtDistance = jgraphtPath.getWeight();
        assertThat(ourDistance).isCloseTo(jgraphtDistance, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    void validateNoPathDetection() {
        // Given: Two disconnected graph components
        addDisconnectedGraphToBoth();

        // When: Try to find path between disconnected nodes
        var ourPath = ourPlanner.planPath("NODE-A", "NODE-C", TransportType.AGV);
        var jgraphtPath = dijkstra.getPath("NODE-A", "NODE-C");

        // Then: Both should return empty/no path
        assertThat(ourPath.isEmpty()).isTrue();
        assertThat(jgraphtPath).isNull();
    }

    // ==================== Helper Methods ====================

    private void addLinearGraphToBoth() {
        // Add to our graph
        ourGraph.addNode(new Node("NODE-A", 0.0, 0.0));
        ourGraph.addNode(new Node("NODE-B", 100.0, 0.0));
        ourGraph.addNode(new Node("NODE-C", 200.0, 0.0));
        ourGraph.addNode(new Node("NODE-D", 300.0, 0.0));

        ourGraph.addEdge(new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0));
        ourGraph.addEdge(new Edge("EDGE-BC", "NODE-B", "NODE-C", 100.0));
        ourGraph.addEdge(new Edge("EDGE-CD", "NODE-C", "NODE-D", 100.0));

        // Add to JGraphT
        jgraphtGraph.addVertex("NODE-A");
        jgraphtGraph.addVertex("NODE-B");
        jgraphtGraph.addVertex("NODE-C");
        jgraphtGraph.addVertex("NODE-D");

        setEdgeWeight("NODE-A", "NODE-B", 100.0);
        setEdgeWeight("NODE-B", "NODE-C", 100.0);
        setEdgeWeight("NODE-C", "NODE-D", 100.0);
    }

    private void addComplexGraphToBoth() {
        // Create main path
        String[] nodes = {"START", "PATH-1", "PATH-2", "END"};
        double[] positions = {0.0, 100.0, 200.0, 300.0};

        for (int i = 0; i < nodes.length; i++) {
            ourGraph.addNode(new Node(nodes[i], positions[i], 0.0));
            jgraphtGraph.addVertex(nodes[i]);
        }

        // Add main path edges
        addEdge("START", "PATH-1", 100.0);
        addEdge("PATH-1", "PATH-2", 100.0);
        addEdge("PATH-2", "END", 100.0);

        // Add alternative path (longer)
        ourGraph.addNode(new Node("ALT-1", 100.0, -50.0));
        ourGraph.addNode(new Node("ALT-2", 200.0, -50.0));
        jgraphtGraph.addVertex("ALT-1");
        jgraphtGraph.addVertex("ALT-2");

        addEdge("START", "ALT-1", 111.8);
        addEdge("ALT-1", "ALT-2", 100.0);
        addEdge("ALT-2", "END", 111.8);
    }

    private void addWeightedGraphToBoth() {
        // A -1-> B -1-> C
        // |       |      |
        // 10      1      1
        // v       v      v
        // D -1-> E -1-> F

        String[] nodes = {"A", "B", "C", "D", "E", "F"};
        for (String node : nodes) {
            ourGraph.addNode(new Node(node, 0.0, 0.0));
            jgraphtGraph.addVertex(node);
        }

        addEdge("A", "B", 1.0);
        addEdge("B", "C", 1.0);
        addEdge("A", "D", 10.0);
        addEdge("D", "E", 1.0);
        addEdge("E", "F", 1.0);
        addEdge("B", "E", 1.0);
        addEdge("C", "F", 1.0);
    }

    private void addDisconnectedGraphToBoth() {
        ourGraph.addNode(new Node("NODE-A", 0.0, 0.0));
        ourGraph.addNode(new Node("NODE-B", 100.0, 0.0));
        ourGraph.addNode(new Node("NODE-C", 200.0, 0.0));

        ourGraph.addEdge(new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0));

        jgraphtGraph.addVertex("NODE-A");
        jgraphtGraph.addVertex("NODE-B");
        jgraphtGraph.addVertex("NODE-C");

        setEdgeWeight("NODE-A", "NODE-B", 100.0);
        // No edge connecting to NODE-C
    }

    private void addEdge(String from, String to, double weight) {
        // Add to our graph
        String edgeId = from + "-" + to;
        ourGraph.addEdge(new Edge(edgeId, from, to, weight));

        // Add to JGraphT
        setEdgeWeight(from, to, weight);
    }

    private void setEdgeWeight(String from, String to, double weight) {
        DefaultWeightedEdge edge = jgraphtGraph.addEdge(from, to);
        if (edge != null) {
            jgraphtGraph.setEdgeWeight(edge, weight);
        }
    }
}
