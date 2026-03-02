package com.semi.simlogistics.control.network;

import java.util.*;

/**
 * Network graph for traffic navigation.
 * <p>
 * Represents a directed graph of nodes and edges for vehicle routing.
 * Bidirectional paths must be stored as two separate edges with opposite directions.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class NetworkGraph {

    private final Map<String, Node> nodes;
    private final Map<String, Edge> edges;
    private final Map<String, Set<String>> adjacencyList; // nodeId -> Set of edgeIds

    /**
     * Create an empty network graph.
     */
    public NetworkGraph() {
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Add a node to the graph.
     *
     * @param node node to add
     * @throws IllegalArgumentException if node ID already exists
     */
    public void addNode(Node node) {
        if (nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Node already exists: " + node.getId());
        }
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new HashSet<>());
    }

    /**
     * Add an edge to the graph.
     *
     * @param edge edge to add
     * @throws IllegalArgumentException if edge ID already exists or nodes don't exist
     */
    public void addEdge(Edge edge) {
        if (edges.containsKey(edge.getId())) {
            throw new IllegalArgumentException("Edge already exists: " + edge.getId());
        }
        if (!nodes.containsKey(edge.getFromNodeId())) {
            throw new IllegalArgumentException("From node not found: " + edge.getFromNodeId());
        }
        if (!nodes.containsKey(edge.getToNodeId())) {
            throw new IllegalArgumentException("To node not found: " + edge.getToNodeId());
        }

        edges.put(edge.getId(), edge);
        adjacencyList.get(edge.getFromNodeId()).add(edge.getId());
    }

    /**
     * Get a node by ID.
     *
     * @param nodeId node ID
     * @return node, or null if not found
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Get an edge by ID.
     *
     * @param edgeId edge ID
     * @return edge, or null if not found
     */
    public Edge getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    /**
     * Get all outgoing edges from a node.
     *
     * @param nodeId source node ID
     * @return unmodifiable set of edge IDs
     */
    public Set<String> getOutgoingEdges(String nodeId) {
        Set<String> edgeIds = adjacencyList.get(nodeId);
        return edgeIds != null ? Collections.unmodifiableSet(edgeIds) : Collections.emptySet();
    }

    /**
     * Check if two nodes are connected.
     *
     * @param fromNodeId source node ID
     * @param toNodeId target node ID
     * @return true if a path exists
     */
    public boolean isConnected(String fromNodeId, String toNodeId) {
        if (!nodes.containsKey(fromNodeId) || !nodes.containsKey(toNodeId)) {
            return false;
        }
        if (fromNodeId.equals(toNodeId)) {
            return true;
        }

        // BFS to find path
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(fromNodeId);
        visited.add(fromNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String edgeId : adjacencyList.getOrDefault(current, Collections.emptySet())) {
                Edge edge = edges.get(edgeId);
                if (edge != null && toNodeId.equals(edge.getToNodeId())) {
                    return true;
                }
                if (!visited.contains(edge.getToNodeId())) {
                    visited.add(edge.getToNodeId());
                    queue.add(edge.getToNodeId());
                }
            }
        }
        return false;
    }

    /**
     * Get all nodes in the graph.
     *
     * @return unmodifiable collection of nodes
     */
    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Get all edges in the graph.
     *
     * @return unmodifiable collection of edges
     */
    public Collection<Edge> getAllEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    /**
     * Get the number of nodes in the graph.
     *
     * @return node count
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Get the number of edges in the graph.
     *
     * @return edge count
     */
    public int getEdgeCount() {
        return edges.size();
    }
}
