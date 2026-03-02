package com.semi.simlogistics.control.path;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.core.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AStarPathPlanner.
 * <p>
 * Tests A* path planning with multiple topologies:
 * - Linear topology
 * - Grid topology
 * - Complex topology with obstacles
 * - Bidirectional edges (two separate directed edges)
 * - Cache eviction and invalidation
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class AStarPathPlannerTest {

    private AStarPathPlanner planner;
    private NetworkGraph graph;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        planner = new AStarPathPlanner(graph);
    }

    // ==================== Basic Functionality Tests ====================

    @Test
    void testLinearTopologyPath() {
        // Given: A linear topology A -> B -> C -> D
        createLinearTopology();

        // When: Plan path from A to D
        Path path = planner.planPath("NODE-A", "NODE-D", TransportType.AGV);

        // Then: Should find path with all nodes
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isFalse();
        assertThat(path.getNodeIds()).containsExactly("NODE-A", "NODE-B", "NODE-C", "NODE-D");
        assertThat(path.getEstimatedTime()).isGreaterThan(0);
    }

    @Test
    void testLinearTopologyPathReversed() {
        // Given: A linear topology (unidirectional edges A->B->C->D)
        createLinearTopology();

        // When: Try to plan from D to A (against edge direction)
        Path path = planner.planPath("NODE-D", "NODE-A", TransportType.AGV);

        // Then: Should return empty path (no path exists)
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isTrue();
    }

    @Test
    void testGridTopologyShortestPath() {
        // Given: A 3x3 grid with diagonal shortcuts
        createGridTopology();

        // When: Plan path from top-left (0,0) to bottom-right (2,2)
        Path path = planner.planPath("NODE-0-0", "NODE-2-2", TransportType.AGV);

        // Then: Should find shortest path (diagonal if available)
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isFalse();
        assertThat(path.getStartNodeId()).isEqualTo("NODE-0-0");
        assertThat(path.getEndNodeId()).isEqualTo("NODE-2-2");

        // Verify it's actually the shortest path
        List<String> nodeIds = path.getNodeIds();
        assertThat(nodeIds.size()).isLessThanOrEqualTo(5); // At most 5 nodes if diagonal exists
    }

    @Test
    void testComplexTopologyWithObstacles() {
        // Given: A complex topology with multiple paths and obstacles
        createComplexTopology();

        // When: Plan path avoiding obstacles
        Path path = planner.planPath("START", "END", TransportType.AGV);

        // Then: Should find a valid path
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isFalse();
        assertThat(path.getStartNodeId()).isEqualTo("START");
        assertThat(path.getEndNodeId()).isEqualTo("END");

        // Verify path doesn't contain blocked nodes
        assertThat(path.getNodeIds()).doesNotContain("BLOCKED-1", "BLOCKED-2");
    }

    // ==================== Bidirectional Edge Tests ====================

    @Test
    void testBidirectionalEdgesRequireTwoSeparateEdges() {
        // Given: Two nodes with bidirectional connection
        Node nodeA = new Node("NODE-A", 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 100.0, 0.0);
        graph.addNode(nodeA);
        graph.addNode(nodeB);

        // Add TWO separate edges for bidirectional path
        Edge edgeAB = new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0);
        Edge edgeBA = new Edge("EDGE-BA", "NODE-B", "NODE-A", 100.0);
        graph.addEdge(edgeAB);
        graph.addEdge(edgeBA);

        // When: Plan path in both directions
        Path pathAB = planner.planPath("NODE-A", "NODE-B", TransportType.AGV);
        Path pathBA = planner.planPath("NODE-B", "NODE-A", TransportType.AGV);

        // Then: Both paths should exist
        assertThat(pathAB).isNotNull();
        assertThat(pathAB.isEmpty()).isFalse();
        assertThat(pathAB.getNodeIds()).containsExactly("NODE-A", "NODE-B");

        assertThat(pathBA).isNotNull();
        assertThat(pathBA.isEmpty()).isFalse();
        assertThat(pathBA.getNodeIds()).containsExactly("NODE-B", "NODE-A");
    }

    @Test
    void testUnidirectionalEdgeOnlyOneDirection() {
        // Given: Two nodes with unidirectional connection
        Node nodeA = new Node("NODE-A", 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 100.0, 0.0);
        graph.addNode(nodeA);
        graph.addNode(nodeB);

        // Add only ONE edge (A -> B)
        Edge edgeAB = new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0);
        graph.addEdge(edgeAB);

        // When: Plan path in both directions
        Path pathAB = planner.planPath("NODE-A", "NODE-B", TransportType.AGV);
        Path pathBA = planner.planPath("NODE-B", "NODE-A", TransportType.AGV);

        // Then: Only A->B should work, B->A should be empty
        assertThat(pathAB).isNotNull();
        assertThat(pathAB.isEmpty()).isFalse();

        assertThat(pathBA).isNotNull();
        assertThat(pathBA.isEmpty()).isTrue();
    }

    // ==================== Cache Tests ====================

    @Test
    void testCacheHitReturnsSameResult() {
        // Given: A linear topology
        createLinearTopology();

        // When: Plan same path twice
        Path path1 = planner.planPath("NODE-A", "NODE-D", TransportType.AGV);
        Path path2 = planner.planPath("NODE-A", "NODE-D", TransportType.AGV);

        // Then: Should return same result (cached)
        assertThat(path1).isNotNull();
        assertThat(path2).isNotNull();
        assertThat(path1.getNodeIds()).isEqualTo(path2.getNodeIds());
        assertThat(path1.getEstimatedTime()).isEqualTo(path2.getEstimatedTime());
    }

    @Test
    void testCacheCapacity() {
        // Given: A planner with capacity 10000
        assertThat(planner.getCacheSize()).isEqualTo(0);

        // When: Add 10001 unique paths
        // Create a chain of nodes: NODE-0 -> NODE-1 -> ... -> NODE-10002
        // Each path is from NODE-i to NODE-(i+1)
        for (int i = 0; i <= 10002; i++) {
            String nodeId = "NODE-" + i;
            graph.addNode(new Node(nodeId, i * 10.0, 0.0));
        }

        // Add edges and plan paths
        for (int i = 0; i < 10001; i++) {
            String from = "NODE-" + i;
            String to = "NODE-" + (i + 1);
            graph.addEdge(new Edge("EDGE-" + i, from, to, 10.0));
            planner.planPath(from, to, TransportType.AGV);
        }

        // Then: Cache size should not exceed 10000
        assertThat(planner.getCacheSize()).isLessThanOrEqualTo(10000);
    }

    @Test
    void testCacheInvalidationOnGraphChange() {
        // Given: A linear topology and cached path
        createLinearTopology();
        Path pathBefore = planner.planPath("NODE-A", "NODE-D", TransportType.AGV);
        assertThat(pathBefore).isNotNull();
        assertThat(pathBefore.isEmpty()).isFalse();
        assertThat(planner.getCacheSize()).isGreaterThan(0);

        // When: Modify graph (add new edge)
        Node nodeE = new Node("NODE-E", 400.0, 0.0);
        graph.addNode(nodeE);
        Edge edgeDE = new Edge("EDGE-DE", "NODE-D", "NODE-E", 100.0);
        graph.addEdge(edgeDE);
        planner.onGraphChanged();

        // Then: Cache should be cleared
        assertThat(planner.getCacheSize()).isEqualTo(0);

        // And: Should be able to plan new path
        Path pathAfter = planner.planPath("NODE-A", "NODE-E", TransportType.AGV);
        assertThat(pathAfter).isNotNull();
        assertThat(pathAfter.isEmpty()).isFalse();
    }

    // ==================== Edge Case Tests ====================

    @Test
    void testPathFromNodeToItself() {
        // Given: A graph with a node
        graph.addNode(new Node("NODE-A", 0.0, 0.0));

        // When: Plan path from node to itself
        Path path = planner.planPath("NODE-A", "NODE-A", TransportType.AGV);

        // Then: Should return path with single node
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isFalse();
        assertThat(path.getNodeIds()).containsExactly("NODE-A");
        assertThat(path.getEstimatedTime()).isEqualTo(0.0);
    }

    @Test
    void testPathWithNonExistentNodes() {
        // Given: An empty graph
        // (no nodes added)

        // When: Plan path with non-existent nodes
        Path path = planner.planPath("NON-EXISTENT-A", "NON-EXISTENT-B", TransportType.AGV);

        // Then: Should return empty path
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isTrue();
    }

    @Test
    void testDisconnectedNodes() {
        // Given: Two disconnected components
        graph.addNode(new Node("NODE-A", 0.0, 0.0));
        graph.addNode(new Node("NODE-B", 100.0, 0.0));
        graph.addNode(new Node("NODE-C", 200.0, 0.0));
        graph.addEdge(new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0));

        // When: Plan path from A to C (no connection)
        Path path = planner.planPath("NODE-A", "NODE-C", TransportType.AGV);

        // Then: Should return empty path
        assertThat(path).isNotNull();
        assertThat(path.isEmpty()).isTrue();
    }

    // ==================== Optimality Tests ====================

    @Test
    void testPathOptimalityLinear() {
        // Given: A linear topology
        createLinearTopology();

        // When: Plan path from A to D
        Path path = planner.planPath("NODE-A", "NODE-D", TransportType.AGV);

        // Then: Path should be optimal (sum of edge lengths)
        assertThat(path.getEstimatedTime()).isEqualTo(300.0); // 100+100+100
    }

    @Test
    void testPathOptimalityWithAlternativeRoutes() {
        // Given: A graph with multiple paths
        Node nodeA = new Node("NODE-A", 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 100.0, 0.0);
        Node nodeC = new Node("NODE-C", 50.0, 50.0);
        Node nodeD = new Node("NODE-D", 150.0, 50.0);
        Node nodeE = new Node("NODE-E", 200.0, 0.0);

        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);
        graph.addNode(nodeD);
        graph.addNode(nodeE);

        // Direct path A->B->E: 100 + 100 = 200
        // Alternative path A->C->D->E: 70.7 + 100 + 70.7 = 241.4
        // Shortest should be A->B->E
        graph.addEdge(new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0));
        graph.addEdge(new Edge("EDGE-BE", "NODE-B", "NODE-E", 100.0));
        graph.addEdge(new Edge("EDGE-AC", "NODE-A", "NODE-C", 70.7));
        graph.addEdge(new Edge("EDGE-CD", "NODE-C", "NODE-D", 100.0));
        graph.addEdge(new Edge("EDGE-DE", "NODE-D", "NODE-E", 70.7));

        // When: Plan path from A to E
        Path path = planner.planPath("NODE-A", "NODE-E", TransportType.AGV);

        // Then: Should choose shortest path (A->B->E)
        assertThat(path.getNodeIds()).containsExactly("NODE-A", "NODE-B", "NODE-E");
        assertThat(path.getEstimatedTime()).isEqualTo(200.0);
    }

    // ==================== Heuristic Function Tests ====================

    @Test
    void testEuclideanHeuristic() {
        // Given: Two nodes at known positions
        Node nodeA = new Node("NODE-A", 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 3.0, 4.0); // Distance = 5

        // When: Calculate heuristic
        double heuristic = planner.calculateHeuristic(nodeA, nodeB);

        // Then: Should equal Euclidean distance
        assertThat(heuristic).isEqualTo(5.0, org.assertj.core.api.Assertions.within(0.001));
    }

    // ==================== Helper Methods ====================

    /**
     * Create a linear topology: A -> B -> C -> D
     */
    private void createLinearTopology() {
        Node nodeA = new Node("NODE-A", 0.0, 0.0);
        Node nodeB = new Node("NODE-B", 100.0, 0.0);
        Node nodeC = new Node("NODE-C", 200.0, 0.0);
        Node nodeD = new Node("NODE-D", 300.0, 0.0);

        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);
        graph.addNode(nodeD);

        graph.addEdge(new Edge("EDGE-AB", "NODE-A", "NODE-B", 100.0));
        graph.addEdge(new Edge("EDGE-BC", "NODE-B", "NODE-C", 100.0));
        graph.addEdge(new Edge("EDGE-CD", "NODE-C", "NODE-D", 100.0));
    }

    /**
     * Create a 3x3 grid topology with diagonal shortcuts.
     */
    private void createGridTopology() {
        // Create 3x3 grid
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                String nodeId = "NODE-" + x + "-" + y;
                graph.addNode(new Node(nodeId, x * 100.0, y * 100.0));
            }
        }

        // Add horizontal edges
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 3; y++) {
                String from = "NODE-" + x + "-" + y;
                String to = "NODE-" + (x + 1) + "-" + y;
                graph.addEdge(new Edge("EDGE-" + from + "-" + to, from, to, 100.0));
            }
        }

        // Add vertical edges
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 2; y++) {
                String from = "NODE-" + x + "-" + y;
                String to = "NODE-" + x + "-" + (y + 1);
                graph.addEdge(new Edge("EDGE-" + from + "-" + to, from, to, 100.0));
            }
        }

        // Add some diagonal shortcuts
        graph.addEdge(new Edge("EDGE-0-0-1-1", "NODE-0-0", "NODE-1-1", 141.4));
        graph.addEdge(new Edge("EDGE-1-1-2-2", "NODE-1-1", "NODE-2-2", 141.4));
    }

    /**
     * Create a complex topology with obstacles.
     */
    private void createComplexTopology() {
        // Create main path
        graph.addNode(new Node("START", 0.0, 0.0));
        graph.addNode(new Node("PATH-1", 100.0, 0.0));
        graph.addNode(new Node("PATH-2", 200.0, 0.0));
        graph.addNode(new Node("END", 300.0, 0.0));

        // Create blocked path
        graph.addNode(new Node("BLOCKED-1", 100.0, 50.0));
        graph.addNode(new Node("BLOCKED-2", 200.0, 50.0));

        // Create alternative path
        graph.addNode(new Node("ALT-1", 100.0, -50.0));
        graph.addNode(new Node("ALT-2", 200.0, -50.0));

        // Add edges (only connect non-blocked paths)
        graph.addEdge(new Edge("EDGE-START-1", "START", "PATH-1", 100.0));
        graph.addEdge(new Edge("EDGE-1-2", "PATH-1", "PATH-2", 100.0));
        graph.addEdge(new Edge("EDGE-2-END", "PATH-2", "END", 100.0));

        // Alternative path
        graph.addEdge(new Edge("EDGE-START-ALT1", "START", "ALT-1", 111.8));
        graph.addEdge(new Edge("EDGE-ALT1-ALT2", "ALT-1", "ALT-2", 100.0));
        graph.addEdge(new Edge("EDGE-ALT2-END", "ALT-2", "END", 111.8));

        // DO NOT add edges to blocked nodes (simulate obstacles)
    }
}
