package com.semi.simlogistics.control.path;

import com.semi.simlogistics.control.network.Edge;
import com.semi.simlogistics.control.network.NetworkGraph;
import com.semi.simlogistics.control.network.Node;
import com.semi.simlogistics.core.TransportType;

import java.util.*;
import java.util.logging.Logger;

/**
 * A* path planner with LRU cache support.
 * <p>
 * Implements the A* algorithm for finding optimal paths in a network graph.
 * Uses Euclidean distance as the heuristic function. Supports LRU caching with
 * event-based invalidation when the graph structure changes.
 * <p>
 * Path planning is transport-type specific, allowing different transport types
 * (OHT, AGV) to have different cached paths.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class AStarPathPlanner implements PathPlanner {

    private static final int DEFAULT_CACHE_CAPACITY = 10000;
    private static final Logger LOGGER = Logger.getLogger(AStarPathPlanner.class.getName());

    private final NetworkGraph graph;
    private final int cacheCapacity;
    private final LinkedHashMap<PathCacheKey, Path> pathCache;

    /**
     * Create a path planner with default cache capacity (10,000).
     *
     * @param graph the network graph
     */
    public AStarPathPlanner(NetworkGraph graph) {
        this(graph, DEFAULT_CACHE_CAPACITY);
    }

    /**
     * Create a path planner with specified cache capacity.
     *
     * @param graph the network graph
     * @param cacheCapacity maximum number of cached paths
     */
    public AStarPathPlanner(NetworkGraph graph, int cacheCapacity) {
        this.graph = Objects.requireNonNull(graph, "NetworkGraph cannot be null");
        this.cacheCapacity = cacheCapacity;
        this.pathCache = new LinkedHashMap<PathCacheKey, Path>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<PathCacheKey, Path> eldest) {
                return size() > AStarPathPlanner.this.cacheCapacity;
            }
        };
    }

    /**
     * Plan a path from start node to end node using A* algorithm.
     * <p>
     * Returns a cached path if available, otherwise computes and caches the result.
     * Path planning is transport-type specific, allowing different transport types
     * (OHT, AGV) to have different cached paths.
     *
     * @param startNodeId start node ID
     * @param endNodeId end node ID
     * @param transportType transport type (OHT, AGV, HUMAN, CONVEYOR)
     * @return the planned path, or empty path if no path exists
     */
    @Override
    public Path planPath(String startNodeId, String endNodeId, TransportType transportType) {
        // Check cache first
        PathCacheKey key = new PathCacheKey(startNodeId, endNodeId, transportType);
        Path cachedPath = pathCache.get(key);
        if (cachedPath != null) {
            return cachedPath;
        }

        // Compute path using A* algorithm
        Path path = computeAStarPath(startNodeId, endNodeId, transportType);

        // Cache the result (even if empty, to avoid recomputation)
        pathCache.put(key, path);

        return path;
    }

    /**
     * Notify the planner that the graph structure has changed.
     * <p>
     * This clears the entire cache to ensure paths are recomputed with the updated graph.
     */
    @Override
    public void onGraphChanged() {
        pathCache.clear();
    }

    /**
     * Get the current cache size.
     *
     * @return number of cached paths
     */
    @Override
    public int getCacheSize() {
        return pathCache.size();
    }

    /**
     * Calculate heuristic distance between two nodes (Euclidean distance).
     * <p>
     * This is the A* admissible heuristic: h(n) = sqrt((x2-x1)^2 + (y2-y1)^2 + (z2-z1)^2)
     *
     * @param from start node
     * @param to target node
     * @return Euclidean distance in meters
     */
    public double calculateHeuristic(Node from, Node to) {
        return from.distanceTo(to);
    }

    /**
     * Compute path using A* algorithm.
     *
     * @param startNodeId start node ID
     * @param endNodeId target node ID
     * @param transportType transport type for this path
     * @return the computed path
     */
    private Path computeAStarPath(String startNodeId, String endNodeId, TransportType transportType) {
        // Validate nodes exist
        Node startNode = graph.getNode(startNodeId);
        Node endNode = graph.getNode(endNodeId);

        if (startNode == null || endNode == null) {
            LOGGER.warning("Cannot find path for " + transportType + ": " +
                    (startNode == null ? "start node '" + startNodeId + "' not found" : "") +
                    (endNode == null ? "end node '" + endNodeId + "' not found" : ""));
            return new Path(); // Empty path
        }

        // Special case: start equals end
        if (startNodeId.equals(endNodeId)) {
            return new Path(List.of(startNodeId), 0.0);
        }

        // A* algorithm
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        Map<String, String> cameFrom = new HashMap<>();
        Map<String, Double> gScore = new HashMap<>();
        Set<String> closedSet = new HashSet<>();

        // Initialize
        gScore.put(startNodeId, 0.0);
        double hStart = calculateHeuristic(startNode, endNode);
        openSet.offer(new AStarNode(startNodeId, 0.0, hStart));

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            String currentId = current.nodeId;

            // Check if we reached the goal
            if (currentId.equals(endNodeId)) {
                return reconstructPath(cameFrom, startNodeId, endNodeId, gScore.get(endNodeId));
            }

            // Skip if already processed
            if (closedSet.contains(currentId)) {
                continue;
            }
            closedSet.add(currentId);

            // Explore neighbors
            for (String edgeId : graph.getOutgoingEdges(currentId)) {
                Edge edge = graph.getEdge(edgeId);
                if (edge == null) {
                    continue;
                }

                String neighborId = edge.getToNodeId();

                // Skip if already processed
                if (closedSet.contains(neighborId)) {
                    continue;
                }

                // Calculate tentative g-score
                double tentativeGScore = gScore.get(currentId) + edge.getLength();

                // Check if this is a better path
                double currentGScore = gScore.getOrDefault(neighborId, Double.POSITIVE_INFINITY);
                if (tentativeGScore < currentGScore) {
                    // Update path
                    cameFrom.put(neighborId, currentId);
                    gScore.put(neighborId, tentativeGScore);

                    // Calculate f-score: f(n) = g(n) + h(n)
                    Node neighborNode = graph.getNode(neighborId);
                    double hScore = calculateHeuristic(neighborNode, endNode);
                    double fScore = tentativeGScore + hScore;

                    // Add to open set (or update existing)
                    openSet.offer(new AStarNode(neighborId, tentativeGScore, fScore));
                }
            }
        }

        // No path found
        LOGGER.warning("No path found for " + transportType + " from '" + startNodeId + "' to '" + endNodeId + "'");
        return new Path();
    }

    /**
     * Reconstruct path from the cameFrom map.
     *
     * @param cameFrom map recording the optimal path to each node
     * @param startNodeId start node ID
     * @param endNodeId end node ID
     * @param estimatedTime total path cost
     * @return reconstructed path
     */
    private Path reconstructPath(Map<String, String> cameFrom, String startNodeId, String endNodeId, double estimatedTime) {
        LinkedList<String> path = new LinkedList<>();
        String current = endNodeId;

        while (current != null) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }

        return new Path(path, estimatedTime);
    }

    /**
     * Cache key for path planning results.
     * <p>
     * Includes transport type to allow different transport types to have different cached paths.
     */
    private record PathCacheKey(String fromNodeId, String toNodeId, TransportType transportType) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathCacheKey that = (PathCacheKey) o;
            return fromNodeId.equals(that.fromNodeId) &&
                    toNodeId.equals(that.toNodeId) &&
                    transportType == that.transportType;
        }

    }

    /**
     * Node in the A* search space.
     *
     * @param gScore Cost from start
     * @param fScore gScore + heuristic
     */
    private record AStarNode(String nodeId, double gScore, double fScore) implements Comparable<AStarNode> {
        @Override
        public int compareTo(AStarNode other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }
}
