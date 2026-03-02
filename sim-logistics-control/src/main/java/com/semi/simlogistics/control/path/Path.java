package com.semi.simlogistics.control.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Path representing a route through the network.
 * <p>
 * Contains ordered nodes and estimated travel time.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Path {

    private final List<String> nodeIds;
    private final double estimatedTime; // seconds

    /**
     * Create a path.
     *
     * @param nodeIds ordered list of node IDs
     * @param estimatedTime estimated travel time in seconds
     */
    public Path(List<String> nodeIds, double estimatedTime) {
        this.nodeIds = new ArrayList<>(Objects.requireNonNull(nodeIds, "nodeIds cannot be null"));
        this.estimatedTime = estimatedTime;
    }

    /**
     * Create an empty path.
     */
    public Path() {
        this(Collections.emptyList(), 0.0);
    }

    /**
     * Create a path from variable number of node IDs.
     *
     * @param nodeIds ordered node IDs
     * @return new path with estimated time 0.0
     */
    public static Path of(String... nodeIds) {
        if (nodeIds == null || nodeIds.length == 0) {
            return new Path(Collections.emptyList(), 0.0);
        }
        List<String> nodes = new ArrayList<>();
        Collections.addAll(nodes, nodeIds);
        return new Path(nodes, 0.0);
    }

    /**
     * Create an empty path.
     *
     * @return empty path
     */
    public static Path empty() {
        return new Path(Collections.emptyList(), 0.0);
    }

    /**
     * Check if path is empty.
     *
     * @return true if path has no nodes
     */
    public boolean isEmpty() {
        return nodeIds.isEmpty();
    }

    /**
     * Get number of nodes in path.
     *
     * @return node count
     */
    public int getNodeCount() {
        return nodeIds.size();
    }

    /**
     * Get ordered list of node IDs.
     *
     * @return unmodifiable list of node IDs
     */
    public List<String> getNodeIds() {
        return Collections.unmodifiableList(nodeIds);
    }

    /**
     * Get estimated travel time.
     *
     * @return estimated time in seconds
     */
    public double getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * Get start node ID.
     *
     * @return first node ID, or null if empty
     */
    public String getStartNodeId() {
        return nodeIds.isEmpty() ? null : nodeIds.get(0);
    }

    /**
     * Get end node ID.
     *
     * @return last node ID, or null if empty
     */
    public String getEndNodeId() {
        return nodeIds.isEmpty() ? null : nodeIds.get(nodeIds.size() - 1);
    }

    @Override
    public String toString() {
        return "Path{" +
               "nodes=" + nodeIds +
               ", time=" + estimatedTime + "s" +
               '}';
    }
}
