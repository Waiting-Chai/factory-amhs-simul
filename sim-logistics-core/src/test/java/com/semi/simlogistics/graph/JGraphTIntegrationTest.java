package com.semi.simlogistics.graph;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for JGraphT library.
 *
 * <p>This test verifies that JGraphT is properly integrated and provides
 * basic graph functionality including:
 * <ul>
 *   <li>Graph creation and manipulation</li>
 *   <li>Vertex and edge management</li>
 *   <li>Shortest path calculation using Dijkstra's algorithm</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
@DisplayName("JGraphT Integration Tests")
class JGraphTIntegrationTest {

    @Test
    @DisplayName("Should create a simple directed weighted graph")
    void shouldCreateDirectedWeightedGraph() {
        // Create a directed weighted graph
        Graph<String, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        // Add vertices (representing logistics locations)
        graph.addVertex("Station_A");
        graph.addVertex("Station_B");
        graph.addVertex("Station_C");
        graph.addVertex("Warehouse");

        // Verify vertices were added
        assertThat(graph.vertexSet()).hasSize(4);
        assertThat(graph.containsVertex("Station_A")).isTrue();
        assertThat(graph.containsVertex("Warehouse")).isTrue();
    }

    @Test
    @DisplayName("Should add weighted edges between vertices")
    void shouldAddWeightedEdges() {
        // Create graph
        Graph<String, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        // Add vertices
        String source = "Station_A";
        String target = "Station_B";
        graph.addVertex(source);
        graph.addVertex(target);

        // Add edge with weight (representing distance or travel time)
        DefaultWeightedEdge edge = graph.addEdge(source, target);
        graph.setEdgeWeight(edge, 10.5);

        // Verify edge was added
        assertThat(graph.containsEdge(source, target)).isTrue();
        assertThat(graph.getEdgeWeight(edge)).isEqualTo(10.5);
    }

    @Test
    @DisplayName("Should calculate shortest path using Dijkstra's algorithm")
    void shouldCalculateShortestPath() {
        // Create a graph representing a logistics network
        Graph<String, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        // Add vertices (locations in the factory)
        graph.addVertex("Entrance");
        graph.addVertex("Storage");
        graph.addVertex("Assembly_Line_1");
        graph.addVertex("Assembly_Line_2");
        graph.addVertex("Shipping");

        // Add edges with weights (representing travel distances/times)
        addWeightedEdge(graph, "Entrance", "Storage", 5.0);
        addWeightedEdge(graph, "Entrance", "Assembly_Line_1", 10.0);
        addWeightedEdge(graph, "Storage", "Assembly_Line_1", 3.0);
        addWeightedEdge(graph, "Storage", "Assembly_Line_2", 8.0);
        addWeightedEdge(graph, "Assembly_Line_1", "Assembly_Line_2", 2.0);
        addWeightedEdge(graph, "Assembly_Line_1", "Shipping", 7.0);
        addWeightedEdge(graph, "Assembly_Line_2", "Shipping", 4.0);

        // Calculate shortest path from Entrance to Shipping
        DijkstraShortestPath<String, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        GraphPath<String, DefaultWeightedEdge> shortestPath = dijkstraAlg.getPath("Entrance", "Shipping");

        // Verify shortest path
        assertThat(shortestPath).isNotNull();
        // The actual shortest path is: Entrance -> Storage -> Assembly_Line_1 -> Assembly_Line_2 -> Shipping
        // Weight: 5 + 3 + 2 + 4 = 14.0
        assertThat(shortestPath.getVertexList())
                .containsExactly("Entrance", "Storage", "Assembly_Line_1", "Assembly_Line_2", "Shipping");
        assertThat(shortestPath.getWeight()).isEqualTo(14.0); // 5 + 3 + 2 + 4

        // Alternative paths:
        // - Entrance -> Storage -> Assembly_Line_2 -> Shipping = 5 + 8 + 4 = 17.0
        // - Entrance -> Assembly_Line_1 -> Shipping = 10 + 7 = 17.0
        // - Entrance -> Storage -> Assembly_Line_1 -> Shipping = 5 + 3 + 7 = 15.0
        // - Entrance -> Assembly_Line_1 -> Assembly_Line_2 -> Shipping = 10 + 2 + 4 = 16.0
        // Shortest is: Entrance -> Storage -> Assembly_Line_1 -> Assembly_Line_2 -> Shipping = 14.0
    }

    @Test
    @DisplayName("Should handle graph with multiple possible paths")
    void shouldHandleMultiplePaths() {
        // Create graph with multiple paths
        Graph<String, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        // Add vertices
        graph.addVertex("Start");
        graph.addVertex("Middle_1");
        graph.addVertex("Middle_2");
        graph.addVertex("End");

        // Add edges creating multiple paths
        addWeightedEdge(graph, "Start", "Middle_1", 5.0);
        addWeightedEdge(graph, "Start", "Middle_2", 10.0);
        addWeightedEdge(graph, "Middle_1", "End", 15.0);
        addWeightedEdge(graph, "Middle_2", "End", 5.0);

        // Calculate shortest path
        ShortestPathAlgorithm<String, DefaultWeightedEdge> alg = new DijkstraShortestPath<>(graph);
        double path1Weight = alg.getPathWeight("Start", "End");

        // Path 1: Start -> Middle_1 -> End = 20.0
        // Path 2: Start -> Middle_2 -> End = 15.0
        assertThat(path1Weight).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should return null when no path exists between vertices")
    void shouldReturnNullWhenNoPathExists() {
        // Create graph with disconnected components
        Graph<String, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        // Add vertices
        graph.addVertex("Component_A_1");
        graph.addVertex("Component_A_2");
        graph.addVertex("Component_B_1");
        graph.addVertex("Component_B_2");

        // Add edges only within component A
        addWeightedEdge(graph, "Component_A_1", "Component_A_2", 5.0);

        // Add edges only within component B
        addWeightedEdge(graph, "Component_B_1", "Component_B_2", 5.0);

        // Try to find path between disconnected components
        DijkstraShortestPath<String, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(graph);
        GraphPath<String, DefaultWeightedEdge> path = dijkstraAlg.getPath("Component_A_1", "Component_B_1");

        // No path should exist
        assertThat(path).isNull();
    }

    /**
     * Helper method to add a weighted edge to the graph.
     *
     * @param graph   the graph to add the edge to
     * @param source  the source vertex
     * @param target  the target vertex
     * @param weight  the weight of the edge
     */
    private void addWeightedEdge(Graph<String, DefaultWeightedEdge> graph,
                                  String source,
                                  String target,
                                  double weight) {
        DefaultWeightedEdge edge = graph.addEdge(source, target);
        graph.setEdgeWeight(edge, weight);
    }
}
