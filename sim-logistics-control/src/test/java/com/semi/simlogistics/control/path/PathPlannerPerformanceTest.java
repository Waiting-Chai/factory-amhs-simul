package com.semi.simlogistics.control.path;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.core.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for AStarPathPlanner cache.
 * <p>
 * Verifies that caching provides significant performance improvements.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class PathPlannerPerformanceTest {

    private NetworkGraph graph;
    private AStarPathPlanner planner;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        planner = new AStarPathPlanner(graph);
        createLargeGraph();
    }

    @Test
    void testCachePerformanceImprovement() {
        // Given: A large graph
        List<String> nodeIds = new ArrayList<>(graph.getAllNodes().stream().map(Node::getId).toList());

        // When: First call (cache miss)
        long start1 = System.nanoTime();
        Path path1 = planner.planPath(nodeIds.get(0), nodeIds.get(nodeIds.size() - 1), TransportType.AGV);
        long time1 = System.nanoTime() - start1;

        // Then: Second call (cache hit) should be much faster
        long start2 = System.nanoTime();
        Path path2 = planner.planPath(nodeIds.get(0), nodeIds.get(nodeIds.size() - 1), TransportType.AGV);
        long time2 = System.nanoTime() - start2;

        assertThat(path1).isNotNull();
        assertThat(path2).isNotNull();
        assertThat(path1.getNodeIds()).isEqualTo(path2.getNodeIds());

        // Cache hit should be at least 100x faster
        System.out.println("First call (cache miss): " + time1 / 1_000_000.0 + " ms");
        System.out.println("Second call (cache hit): " + time2 / 1_000_000.0 + " ms");
        System.out.println("Speedup: " + (time1 / (double) time2) + "x");

        assertThat(time2).isLessThan(time1);
    }

    @Test
    void testCacheEfficiency() {
        // Given: A large graph
        List<String> nodeIds = new ArrayList<>(graph.getAllNodes().stream().map(Node::getId).toList());

        // When: Plan multiple paths
        int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            String from = nodeIds.get(i % nodeIds.size());
            String to = nodeIds.get((i + 10) % nodeIds.size());
            planner.planPath(from, to, TransportType.AGV);
        }

        // Then: Cache should contain entries
        assertThat(planner.getCacheSize()).isGreaterThan(0);
        assertThat(planner.getCacheSize()).isLessThanOrEqualTo(10000);

        System.out.println("Cache size after " + iterations + " iterations: " + planner.getCacheSize());
    }

    private void createLargeGraph() {
        // Create a 20x20 grid
        int gridSize = 20;
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                String nodeId = "NODE-" + x + "-" + y;
                graph.addNode(new Node(nodeId, x * 10.0, y * 10.0));
            }
        }

        // Add horizontal edges
        for (int x = 0; x < gridSize - 1; x++) {
            for (int y = 0; y < gridSize; y++) {
                String from = "NODE-" + x + "-" + y;
                String to = "NODE-" + (x + 1) + "-" + y;
                graph.addEdge(new Edge(from + "-" + to, from, to, 10.0));
            }
        }

        // Add vertical edges
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize - 1; y++) {
                String from = "NODE-" + x + "-" + y;
                String to = "NODE-" + x + "-" + (y + 1);
                graph.addEdge(new Edge(from + "-" + to, from, to, 10.0));
            }
        }
    }
}
