package com.semi.simlogistics.control.movement;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.path.Path;

import java.util.List;

/**
 * Network movement logic for AGV vehicles.
 * <p>
 * Implements movement along a network graph (node-edge structure).
 * Provides methods for calculating movement time, distance, progress,
 * and validating paths for network-based navigation.
 * <p>
 * Units:
 * <ul>
 *   <li>Distance: meters (m)</li>
 *   <li>Time: seconds (s)</li>
 *   <li>Speed: meters per second (m/s)</li>
 *   <li>Angle: radians [-π, π] (for future use with heading calculations)</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class NetworkMovement {

    private final NetworkGraph graph;

    /**
     * Create network movement with a graph.
     *
     * @param graph network graph for navigation
     * @throws IllegalArgumentException if graph is null
     */
    public NetworkMovement(NetworkGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph cannot be null");
        }
        this.graph = graph;
    }

    /**
     * Calculate movement time for a path at given speed.
     * <p>
     * Time = total distance / speed.
     *
     * @param path path to travel
     * @param speed vehicle speed in m/s
     * @return travel time in seconds
     * @throws IllegalArgumentException if path is null, speed <= 0, or path is invalid
     */
    public double calculateMovementTime(Path path, double speed) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (speed <= 0.0) {
            throw new IllegalArgumentException("speed must be positive, got: " + speed);
        }

        double distance = calculateDistance(path);
        return distance / speed;
    }

    /**
     * Calculate total distance of a path.
     * <p>
     * Sums the length of all edges in the path.
     *
     * @param path path to measure
     * @return total distance in meters
     * @throws IllegalArgumentException if path is null or contains invalid nodes
     */
    public double calculateDistance(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }

        List<String> nodeIds = path.getNodeIds();
        if (nodeIds.isEmpty() || nodeIds.size() == 1) {
            return 0.0;
        }

        double totalDistance = 0.0;
        for (int i = 0; i < nodeIds.size() - 1; i++) {
            String fromNodeId = nodeIds.get(i);
            String toNodeId = nodeIds.get(i + 1);

            Edge edge = findEdge(fromNodeId, toNodeId);
            totalDistance += edge.getLength();
        }

        return totalDistance;
    }

    /**
     * Calculate progress along path at given time.
     * <p>
     * Returns a value between 0.0 (start) and 1.0 (end).
     * Progress is based on distance traveled, not time.
     *
     * @param path path to travel
     * @param speed vehicle speed in m/s
     * @param elapsedTime elapsed time in seconds
     * @return progress ratio [0.0, 1.0]
     * @throws IllegalArgumentException if parameters are invalid
     */
    public double calculateProgressAtTime(Path path, double speed, double elapsedTime) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (speed <= 0.0) {
            throw new IllegalArgumentException("speed must be positive, got: " + speed);
        }
        if (elapsedTime < 0.0) {
            throw new IllegalArgumentException("elapsedTime cannot be negative, got: " + elapsedTime);
        }

        double totalDistance = calculateDistance(path);
        if (totalDistance == 0.0) {
            return 0.0;
        }

        double distanceTraveled = speed * elapsedTime;

        // Clamp to [0, totalDistance]
        if (distanceTraveled < 0.0) {
            distanceTraveled = 0.0;
        } else if (distanceTraveled > totalDistance) {
            distanceTraveled = totalDistance;
        }

        return distanceTraveled / totalDistance;
    }

    /**
     * Get current node ID at given progress along path.
     * <p>
     * Returns the node where the vehicle is currently located.
     * For progress values between nodes, returns the last passed node.
     *
     * @param path path to travel
     * @param progress progress ratio [0.0, 1.0]
     * @return current node ID
     * @throws IllegalArgumentException if path is null or progress is out of range
     */
    public String getCurrentNodeId(Path path, double progress) {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (progress < 0.0 || progress > 1.0) {
            throw new IllegalArgumentException("progress must be in [0.0, 1.0], got: " + progress);
        }

        List<String> nodeIds = path.getNodeIds();
        if (nodeIds.isEmpty()) {
            return null;
        }
        if (nodeIds.size() == 1) {
            return nodeIds.get(0);
        }

        // Handle end of path explicitly
        if (progress >= 1.0) {
            return nodeIds.get(nodeIds.size() - 1);
        }

        // Clamp progress to [0, 1)
        double clampedProgress = Math.max(0.0, Math.min(1.0, progress));

        // Calculate cumulative distances
        double totalDistance = calculateDistance(path);
        double targetDistance = totalDistance * clampedProgress;

        double cumulativeDistance = 0.0;
        for (int i = 0; i < nodeIds.size() - 1; i++) {
            String fromNodeId = nodeIds.get(i);
            String toNodeId = nodeIds.get(i + 1);

            Edge edge = findEdge(fromNodeId, toNodeId);
            double segmentLength = edge.getLength();

            if (cumulativeDistance + segmentLength >= targetDistance) {
                // Current position is in this segment
                return fromNodeId;
            }

            cumulativeDistance += segmentLength;
        }

        // Should not reach here, but return last node as fallback
        return nodeIds.get(nodeIds.size() - 1);
    }

    /**
     * Validate that a path is valid and can be traversed.
     * <p>
     * Checks that:
     * <ul>
     *   <li>All nodes exist in the graph</li>
     *   <li>Adjacent nodes are connected by edges</li>
     *   <li>Path is not null</li>
     * </ul>
     *
     * @param path path to validate
     * @return true if path is valid, false otherwise
     */
    public boolean validatePath(Path path) {
        if (path == null) {
            return false;
        }

        List<String> nodeIds = path.getNodeIds();
        if (nodeIds.isEmpty()) {
            return true; // Empty path is valid (no movement)
        }
        if (nodeIds.size() == 1) {
            return graph.getNode(nodeIds.get(0)) != null;
        }

        // Check all nodes exist and edges exist between adjacent nodes
        for (int i = 0; i < nodeIds.size() - 1; i++) {
            String fromNodeId = nodeIds.get(i);
            String toNodeId = nodeIds.get(i + 1);

            if (graph.getNode(fromNodeId) == null) {
                return false;
            }
            if (graph.getNode(toNodeId) == null) {
                return false;
            }

            try {
                findEdge(fromNodeId, toNodeId);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find edge between two adjacent nodes.
     *
     * @param fromNodeId source node ID
     * @param toNodeId target node ID
     * @return edge connecting the nodes
     * @throws IllegalArgumentException if nodes don't exist or no edge found
     */
    private Edge findEdge(String fromNodeId, String toNodeId) {
        if (graph.getNode(fromNodeId) == null) {
            throw new IllegalArgumentException("Node not found: " + fromNodeId);
        }
        if (graph.getNode(toNodeId) == null) {
            throw new IllegalArgumentException("Node not found: " + toNodeId);
        }

        for (String edgeId : graph.getOutgoingEdges(fromNodeId)) {
            Edge edge = graph.getEdge(edgeId);
            if (edge != null && edge.getToNodeId().equals(toNodeId)) {
                return edge;
            }
        }

        throw new IllegalArgumentException(
                "No edge found from " + fromNodeId + " to " + toNodeId);
    }

    /**
     * Get the network graph used by this movement.
     *
     * @return network graph
     */
    public NetworkGraph getGraph() {
        return graph;
    }
}
