package com.semi.simlogistics.control.movement;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.control.path.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for NetworkMovement.
 * <p>
 * Tests AGV vehicle movement along network graph paths.
 * Follows TDD approach: tests written before implementation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class NetworkMovementTest {

    private NetworkGraph graph;
    private NetworkMovement movement;

    @BeforeEach
    void setUp() {
        graph = createSimpleGraph();
        movement = new NetworkMovement(graph);
    }

    /**
     * Create a simple linear graph for testing:
     * N1 --(10m)--> N2 --(15m)--> N3
     */
    private NetworkGraph createSimpleGraph() {
        NetworkGraph g = new NetworkGraph();

        // Add nodes
        Node n1 = new Node("N1", 0.0, 0.0);
        Node n2 = new Node("N2", 10.0, 0.0);
        Node n3 = new Node("N3", 25.0, 0.0);

        g.addNode(n1);
        g.addNode(n2);
        g.addNode(n3);

        // Add edges
        Edge e1 = new Edge("E1", "N1", "N2", 10.0);
        Edge e2 = new Edge("E2", "N2", "N3", 15.0);

        g.addEdge(e1);
        g.addEdge(e2);

        return g;
    }

    /**
     * Create a graph with branching for testing:
     * N1 --(10m)--> N2 --(15m)--> N3
     *               |
     *              (12m)
     *               |
     *               v
     *               N4
     */
    private NetworkGraph createBranchedGraph() {
        NetworkGraph g = new NetworkGraph();

        // Add nodes
        Node n1 = new Node("N1", 0.0, 0.0);
        Node n2 = new Node("N2", 10.0, 0.0);
        Node n3 = new Node("N3", 25.0, 0.0);
        Node n4 = new Node("N4", 10.0, 12.0);

        g.addNode(n1);
        g.addNode(n2);
        g.addNode(n3);
        g.addNode(n4);

        // Add edges
        Edge e1 = new Edge("E1", "N1", "N2", 10.0);
        Edge e2 = new Edge("E2", "N2", "N3", 15.0);
        Edge e3 = new Edge("E3", "N2", "N4", 12.0);

        g.addEdge(e1);
        g.addEdge(e2);
        g.addEdge(e3);

        return g;
    }

    @Test
    void testCalculateMovementTime_SimplePath() {
        // Given: A path from N1 to N3
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);
        double speed = 2.0; // 2 m/s

        // When: Calculate movement time
        double time = movement.calculateMovementTime(path, speed);

        // Then: Time should be distance / speed = (10 + 15) / 2 = 12.5 seconds
        assertThat(time).isEqualTo(12.5);
    }

    @Test
    void testCalculateMovementTime_BranchedPath() {
        // Given: A branched graph
        NetworkGraph branchedGraph = createBranchedGraph();
        NetworkMovement branchedMovement = new NetworkMovement(branchedGraph);

        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N4"), 0.0);
        double speed = 3.0; // 3 m/s

        // When: Calculate movement time
        double time = branchedMovement.calculateMovementTime(path, speed);

        // Then: Time should be distance / speed = (10 + 12) / 3 = 7.33 seconds
        assertThat(time).isEqualTo(22.0 / 3.0, org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    void testCalculateMovementTime_SingleNode() {
        // Given: A path with single node (no movement)
        Path path = new Path(java.util.Collections.singletonList("N1"), 0.0);
        double speed = 2.0;

        // When: Calculate movement time
        double time = movement.calculateMovementTime(path, speed);

        // Then: Time should be 0 (no movement)
        assertThat(time).isEqualTo(0.0);
    }

    @Test
    void testCalculateMovementTime_ZeroSpeed() {
        // Given: A path with zero speed
        Path path = new Path(java.util.Arrays.asList("N1", "N2"), 0.0);
        double speed = 0.0;

        // When/Then: Should throw exception
        assertThatThrownBy(() -> movement.calculateMovementTime(path, speed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("speed must be positive");
    }

    @Test
    void testCalculateMovementTime_InvalidNode() {
        // Given: A path with non-existent node
        Path path = new Path(java.util.Arrays.asList("N1", "INVALID", "N3"), 0.0);
        double speed = 2.0;

        // When/Then: Should throw exception
        assertThatThrownBy(() -> movement.calculateMovementTime(path, speed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Node not found");
    }

    @Test
    void testCalculateMovementTime_NonAdjacentNodes() {
        // Given: A path with non-adjacent nodes
        Path path = new Path(java.util.Arrays.asList("N1", "N3"), 0.0);
        double speed = 2.0;

        // When/Then: Should throw exception (no direct edge from N1 to N3)
        assertThatThrownBy(() -> movement.calculateMovementTime(path, speed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No edge found");
    }

    @Test
    void testCalculateDistance_SimplePath() {
        // Given: A path from N1 to N3
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Calculate distance
        double distance = movement.calculateDistance(path);

        // Then: Distance should be 10 + 15 = 25 meters
        assertThat(distance).isEqualTo(25.0);
    }

    @Test
    void testCalculateDistance_SingleNode() {
        // Given: A path with single node
        Path path = new Path(java.util.Collections.singletonList("N1"), 0.0);

        // When: Calculate distance
        double distance = movement.calculateDistance(path);

        // Then: Distance should be 0
        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    void testCalculateProgressAtTime_Halfway() {
        // Given: A path and movement
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);
        double totalTime = 12.5; // (10+15) / 2.0
        double halfTime = totalTime / 2.0;

        // When: Calculate progress at half time
        double progress = movement.calculateProgressAtTime(path, 2.0, halfTime);

        // Then: Should be approximately 0.5 (halfway)
        assertThat(progress).isEqualTo(0.5, org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    void testCalculateProgressAtTime_Start() {
        // Given: A path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Calculate progress at time 0
        double progress = movement.calculateProgressAtTime(path, 2.0, 0.0);

        // Then: Should be 0.0 (at start)
        assertThat(progress).isEqualTo(0.0);
    }

    @Test
    void testCalculateProgressAtTime_End() {
        // Given: A path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);
        double totalTime = 12.5; // (10+15) / 2.0

        // When: Calculate progress at total time
        double progress = movement.calculateProgressAtTime(path, 2.0, totalTime);

        // Then: Should be 1.0 (at end)
        assertThat(progress).isEqualTo(1.0);
    }

    @Test
    void testGetCurrentNodeId_AtStart() {
        // Given: A path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Get current node at progress 0.0
        String nodeId = movement.getCurrentNodeId(path, 0.0);

        // Then: Should be N1
        assertThat(nodeId).isEqualTo("N1");
    }

    @Test
    void testGetCurrentNodeId_AtEnd() {
        // Given: A path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Get current node at progress 1.0
        String nodeId = movement.getCurrentNodeId(path, 1.0);

        // Then: Should be N3
        assertThat(nodeId).isEqualTo("N3");
    }

    @Test
    void testGetCurrentNodeId_InFirstSegment() {
        // Given: A path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Get current node at progress 0.2 (still in first segment: 10/25 = 0.4)
        String nodeId = movement.getCurrentNodeId(path, 0.2);

        // Then: Should be N1 (still in first segment)
        assertThat(nodeId).isEqualTo("N1");
    }

    @Test
    void testGetCurrentNodeId_InSecondSegment() {
        // Given: A path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Get current node at progress 0.6 (in second segment: 10/25 = 0.4, so 0.6 > 0.4)
        String nodeId = movement.getCurrentNodeId(path, 0.6);

        // Then: Should be N2
        assertThat(nodeId).isEqualTo("N2");
    }

    @Test
    void testValidatePath_ValidPath() {
        // Given: A valid path
        Path path = new Path(java.util.Arrays.asList("N1", "N2", "N3"), 0.0);

        // When: Validate path
        boolean isValid = movement.validatePath(path);

        // Then: Should be valid
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidatePath_InvalidNode() {
        // Given: A path with invalid node
        Path path = new Path(java.util.Arrays.asList("N1", "INVALID", "N3"), 0.0);

        // When: Validate path
        boolean isValid = movement.validatePath(path);

        // Then: Should be invalid
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidatePath_NonAdjacentNodes() {
        // Given: A path with non-adjacent nodes
        Path path = new Path(java.util.Arrays.asList("N1", "N3"), 0.0);

        // When: Validate path
        boolean isValid = movement.validatePath(path);

        // Then: Should be invalid
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidatePath_EmptyPath() {
        // Given: An empty path
        Path path = new Path();

        // When: Validate path
        boolean isValid = movement.validatePath(path);

        // Then: Should be valid (empty path is valid for no movement)
        assertThat(isValid).isTrue();
    }
}
