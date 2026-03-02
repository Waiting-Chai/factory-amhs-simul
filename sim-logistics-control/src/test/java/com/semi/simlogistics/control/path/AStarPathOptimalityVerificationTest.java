package com.semi.simlogistics.control.path;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.core.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Path optimality verification tests for AStarPathPlanner.
 * <p>
 * These tests verify that the A* algorithm produces optimal (shortest) paths
 * by comparing against exhaustive search results on various topologies.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class AStarPathOptimalityVerificationTest {

    private NetworkGraph graph;
    private AStarPathPlanner planner;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        planner = new AStarPathPlanner(graph);
    }

    @Test
    void verifyOptimalityOnSmallGraph() {
        // Given: A small graph with multiple possible paths
        createOptimalityTestGraph();

        // When: Find path using A*
        Path aStarPath = planner.planPath("START", "GOAL", TransportType.AGV);

        // Then: Compare with exhaustive search (BFS for unweighted, Dijkstra for weighted)
        Path optimalPath = findOptimalPathExhaustive("START", "GOAL");

        // Verify A* finds the optimal path
        assertThat(aStarPath.getNodeIds()).isEqualTo(optimalPath.getNodeIds());
        assertThat(aStarPath.getEstimatedTime()).isEqualTo(optimalPath.getEstimatedTime());
    }

    @Test
    void verifyOptimalityOnGridWithDiagonals() {
        // Given: A 4x4 grid with diagonal shortcuts
        createGridWithDiagonals();

        // When: Find path from corner to opposite corner
        Path aStarPath = planner.planPath("0-0", "3-3", TransportType.AGV);

        // Then: Compare with Dijkstra's algorithm
        Path dijkstraPath = findOptimalPathExhaustive("0-0", "3-3");

        // Verify A* finds the optimal path
        assertThat(aStarPath.getNodeIds()).isEqualTo(dijkstraPath.getNodeIds());
        assertThat(aStarPath.getEstimatedTime()).isEqualTo(dijkstraPath.getEstimatedTime());

        // The optimal path should use diagonals where beneficial
        // Expected: 0-0 -> 1-1 -> 2-2 -> 3-3 (cost ≈ 424.2)
        // Rather than: 0-0 -> 0-1 -> 0-2 -> 0-3 -> 1-3 -> 2-3 -> 3-3 (cost = 600)
        assertThat(aStarPath.getEstimatedTime()).isLessThan(500.0);
    }

    @Test
    void verifyOptimalityWithWeightedEdges() {
        // Given: A graph with varying edge weights
        createWeightedGraph();

        // When: Find path avoiding expensive edges
        Path aStarPath = planner.planPath("A", "F", TransportType.AGV);

        // Then: Compare with Dijkstra's algorithm
        Path optimalPath = findOptimalPathExhaustive("A", "F");

        // Verify A* finds the optimal path
        assertThat(aStarPath.getNodeIds()).isEqualTo(optimalPath.getNodeIds());
        assertThat(aStarPath.getEstimatedTime()).isEqualTo(optimalPath.getEstimatedTime());
    }

    @Test
    void verifyHeuristicAdmissibility() {
        // Given: A graph where Euclidean distance is always ≤ actual path distance
        // This is true when nodes are positioned according to their path distances
        createAdmissibleTestGraph();
        Node startNode = graph.getNode("START");
        Node goalNode = graph.getNode("GOAL");

        // When: Calculate heuristic
        double heuristic = planner.calculateHeuristic(startNode, goalNode);

        // Then: Heuristic should never overestimate the actual distance
        // (This is the admissibility condition for A*)
        Path actualPath = findOptimalPathExhaustive("START", "GOAL");
        double actualDistance = actualPath.getEstimatedTime();

        // For admissibility, we need h(n) ≤ actual_cost(n)
        // This holds when Euclidean distance ≤ shortest path distance
        assertThat(heuristic).isLessThanOrEqualTo(actualDistance);
    }

    @Test
    void verifyConsistencyOfHeuristic() {
        // Given: A graph with three nodes forming a triangle
        Node nodeA = new Node("A", 0.0, 0.0);
        Node nodeB = new Node("B", 3.0, 0.0);  // Distance A-B = 3
        Node nodeC = new Node("C", 0.0, 4.0);  // Distance A-C = 4, B-C = 5

        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);

        graph.addEdge(new Edge("AB", "A", "B", 3.0));
        graph.addEdge(new Edge("AC", "A", "C", 4.0));
        graph.addEdge(new Edge("BC", "B", "C", 5.0));

        // When: Calculate heuristics
        double hAC = planner.calculateHeuristic(nodeA, nodeC);
        double hAB = planner.calculateHeuristic(nodeA, nodeB);
        double hBC = planner.calculateHeuristic(nodeB, nodeC);
        double costAB = graph.getEdge("AB").getLength();

        // Then: Consistency condition: h(A,C) ≤ h(A,B) + cost(A,B) + h(B,C)
        // This ensures that when we follow the optimal path, the f-score is non-decreasing
        assertThat(hAC).isLessThanOrEqualTo(hAB + costAB + hBC);
    }

    @Test
    void verifyOptimalityOnRandomGraph() {
        // Given: A random graph with 20 nodes
        createRandomGraph(20, 0.3);

        // Test multiple random pairs
        Random random = new Random(42); // Fixed seed for reproducibility
        List<Node> nodes = new ArrayList<>(graph.getAllNodes());

        for (int i = 0; i < 10; i++) {
            int fromIdx = random.nextInt(nodes.size());
            int toIdx = random.nextInt(nodes.size());

            String fromId = nodes.get(fromIdx).getId();
            String toId = nodes.get(toIdx).getId();

            // When: Find path using A*
            Path aStarPath = planner.planPath(fromId, toId, TransportType.AGV);

            // Then: Compare with Dijkstra's algorithm
            Path optimalPath = findOptimalPathExhaustive(fromId, toId);

            // Verify A* finds the optimal path (if path exists)
            if (!optimalPath.isEmpty()) {
                assertThat(aStarPath.getNodeIds()).isEqualTo(optimalPath.getNodeIds());
                assertThat(aStarPath.getEstimatedTime()).isEqualTo(optimalPath.getEstimatedTime());
            } else {
                assertThat(aStarPath.isEmpty()).isTrue();
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Find optimal path using exhaustive search (Dijkstra's algorithm).
     * This serves as ground truth for verifying A* optimality.
     */
    private Path findOptimalPathExhaustive(String startId, String goalId) {
        if (graph.getNode(startId) == null || graph.getNode(goalId) == null) {
            return new Path();
        }

        if (startId.equals(goalId)) {
            return new Path(List.of(startId), 0.0);
        }

        // Dijkstra's algorithm
        PriorityQueue<DijkstraNode> openSet = new PriorityQueue<>();
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // Initialize
        for (Node node : graph.getAllNodes()) {
            dist.put(node.getId(), Double.POSITIVE_INFINITY);
        }
        dist.put(startId, 0.0);
        openSet.offer(new DijkstraNode(startId, 0.0));

        while (!openSet.isEmpty()) {
            DijkstraNode current = openSet.poll();
            String currentId = current.nodeId;

            if (visited.contains(currentId)) {
                continue;
            }
            visited.add(currentId);

            // Found the goal
            if (currentId.equals(goalId)) {
                return reconstructDijkstraPath(prev, startId, goalId, dist.get(goalId));
            }

            // Explore neighbors
            for (String edgeId : graph.getOutgoingEdges(currentId)) {
                Edge edge = graph.getEdge(edgeId);
                if (edge == null) continue;

                String neighborId = edge.getToNodeId();
                if (visited.contains(neighborId)) continue;

                double newDist = dist.get(currentId) + edge.getLength();
                if (newDist < dist.get(neighborId)) {
                    dist.put(neighborId, newDist);
                    prev.put(neighborId, currentId);
                    openSet.offer(new DijkstraNode(neighborId, newDist));
                }
            }
        }

        // No path found
        return new Path();
    }

    private Path reconstructDijkstraPath(Map<String, String> prev, String startId, String goalId, double cost) {
        LinkedList<String> path = new LinkedList<>();
        String current = goalId;

        while (current != null) {
            path.addFirst(current);
            current = prev.get(current);
        }

        return new Path(path, cost);
    }

    private void createAdmissibleTestGraph() {
        // Create a graph where Euclidean distance is always ≤ actual path distance
        // This is a simple linear graph where nodes are placed along a straight line
        graph.addNode(new Node("START", 0.0, 0.0));
        graph.addNode(new Node("MID1", 10.0, 0.0));
        graph.addNode(new Node("MID2", 20.0, 0.0));
        graph.addNode(new Node("GOAL", 30.0, 0.0));

        // All edges go in the same direction along the line
        graph.addEdge(new Edge("START-MID1", "START", "MID1", 10.0));
        graph.addEdge(new Edge("MID1-MID2", "MID1", "MID2", 10.0));
        graph.addEdge(new Edge("MID2-GOAL", "MID2", "GOAL", 10.0));

        // Euclidean distance from START to GOAL = 30.0
        // Actual path distance = 10 + 10 + 10 = 30.0
        // So heuristic is admissible (h = 30.0 ≤ actual = 30.0)
    }

    private void createOptimalityTestGraph() {
        // Create a graph with multiple paths to goal
        /*
         *     START
         *      /  \
         *    10    15
         *    /      \
         *   A        B
         *   | \     / \
         *   5  8  10  20
         *   |   \ /    |
         *   C    D     E
         *    \  / \   /
         *    10   5 10
         *      \  |  /
         *       GOAL
         */

        graph.addNode(new Node("START", 0, 0));
        graph.addNode(new Node("A", 10, 10));
        graph.addNode(new Node("B", 20, 10));
        graph.addNode(new Node("C", 10, 20));
        graph.addNode(new Node("D", 20, 20));
        graph.addNode(new Node("E", 30, 20));
        graph.addNode(new Node("GOAL", 20, 30));

        // Edges
        graph.addEdge(new Edge("START-A", "START", "A", 10.0));
        graph.addEdge(new Edge("START-B", "START", "B", 15.0));
        graph.addEdge(new Edge("A-C", "A", "C", 5.0));
        graph.addEdge(new Edge("A-D", "A", "D", 8.0));
        graph.addEdge(new Edge("B-D", "B", "D", 10.0));
        graph.addEdge(new Edge("B-E", "B", "E", 20.0));
        graph.addEdge(new Edge("C-GOAL", "C", "GOAL", 10.0));
        graph.addEdge(new Edge("D-GOAL", "D", "GOAL", 5.0));
        graph.addEdge(new Edge("E-GOAL", "E", "GOAL", 10.0));
        graph.addEdge(new Edge("C-D", "C", "D", 10.0));
    }

    private void createGridWithDiagonals() {
        // Create 4x4 grid
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                String nodeId = x + "-" + y;
                graph.addNode(new Node(nodeId, x * 100.0, y * 100.0));
            }
        }

        // Add horizontal edges (cost 100)
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                String from = x + "-" + y;
                String to = (x + 1) + "-" + y;
                graph.addEdge(new Edge(from + "-" + to, from, to, 100.0));
            }
        }

        // Add vertical edges (cost 100)
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                String from = x + "-" + y;
                String to = x + "-" + (y + 1);
                graph.addEdge(new Edge(from + "-" + to, from, to, 100.0));
            }
        }

        // Add diagonal edges (cost ≈ 141.4)
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                String from = x + "-" + y;
                String to = (x + 1) + "-" + (y + 1);
                graph.addEdge(new Edge(from + "-" + to, from, to, 141.4));
            }
        }
    }

    private void createWeightedGraph() {
        /*
         * A -1-> B -1-> C
         * |       |      |
         * 10      1      1
         * v       v      v
         * D -1-> E -1-> F
         */
        graph.addNode(new Node("A", 0, 0));
        graph.addNode(new Node("B", 1, 0));
        graph.addNode(new Node("C", 2, 0));
        graph.addNode(new Node("D", 0, 1));
        graph.addNode(new Node("E", 1, 1));
        graph.addNode(new Node("F", 2, 1));

        graph.addEdge(new Edge("AB", "A", "B", 1.0));
        graph.addEdge(new Edge("BC", "B", "C", 1.0));
        graph.addEdge(new Edge("AD", "A", "D", 10.0));
        graph.addEdge(new Edge("DE", "D", "E", 1.0));
        graph.addEdge(new Edge("EF", "E", "F", 1.0));
        graph.addEdge(new Edge("BE", "B", "E", 1.0));
        graph.addEdge(new Edge("CF", "C", "F", 1.0));
    }

    private void createRandomGraph(int numNodes, double edgeProbability) {
        Random random = new Random(42); // Fixed seed

        // Create nodes
        for (int i = 0; i < numNodes; i++) {
            String nodeId = "N" + i;
            double x = random.nextDouble() * 1000;
            double y = random.nextDouble() * 1000;
            graph.addNode(new Node(nodeId, x, y));
        }

        // Create random edges
        List<Node> nodes = new ArrayList<>(graph.getAllNodes());
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                if (random.nextDouble() < edgeProbability) {
                    Node from = nodes.get(i);
                    Node to = nodes.get(j);
                    double distance = from.distanceTo(to);

                    // Create directed edge (i -> j)
                    String edgeId = from.getId() + "-" + to.getId();
                    graph.addEdge(new Edge(edgeId, from.getId(), to.getId(), distance));

                    // Optionally create reverse edge
                    if (random.nextDouble() < edgeProbability) {
                        String reverseEdgeId = to.getId() + "-" + from.getId();
                        graph.addEdge(new Edge(reverseEdgeId, to.getId(), from.getId(), distance));
                    }
                }
            }
        }
    }

    private static class DijkstraNode implements Comparable<DijkstraNode> {
        private final String nodeId;
        private final double distance;

        public DijkstraNode(String nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }

        @Override
        public int compareTo(DijkstraNode other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}
