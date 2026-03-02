package com.semi.simlogistics.control.path;

import com.semi.simlogistics.core.TransportType;

/**
 * Path planner interface for vehicle route planning.
 * <p>
 * Defines the contract for path planning algorithms that calculate optimal routes
 * between nodes in a network graph. Implementations may use different algorithms
 * (A*, Dijkstra, etc.) and optimization strategies (caching, heuristics).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public interface PathPlanner {

    /**
     * Plan a path from start node to end node for the specified transport type.
     * <p>
     * Different transport types (OHT, AGV) may have different path constraints
     * and cached routes. The path planner should return the optimal path
     * based on the transport type's requirements.
     *
     * @param startNodeId start node ID
     * @param endNodeId end node ID
     * @param transportType transport type (OHT, AGV, HUMAN, CONVEYOR)
     * @return the planned path, or empty path if no path exists
     */
    Path planPath(String startNodeId, String endNodeId, TransportType transportType);

    /**
     * Notify the planner that the graph structure has changed.
     * <p>
     * Implementations should clear any cached or pre-computed paths
     * to ensure routes are recalculated with the updated graph.
     */
    void onGraphChanged();

    /**
     * Get the current cache size.
     * <p>
     * Returns the number of cached paths. Implementations without caching
     * should return 0.
     *
     * @return number of cached paths
     */
    int getCacheSize();
}
