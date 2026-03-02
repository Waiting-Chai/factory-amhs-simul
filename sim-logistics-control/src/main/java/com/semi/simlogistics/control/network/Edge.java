package com.semi.simlogistics.control.network;

/**
 * Edge in the network graph.
 * <p>
 * Represents a directed connection between two nodes.
 * Bidirectional paths must be stored as two separate edges with opposite directions.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Edge {

    private final String id;
    private final String fromNodeId;
    private final String toNodeId;
    private final double length; // meters
    private final int capacity; // number of vehicles allowed simultaneously

    /**
     * Create an edge.
     *
     * @param id unique edge identifier
     * @param fromNodeId source node ID
     * @param toNodeId target node ID
     * @param length length in meters
     * @param capacity maximum number of vehicles allowed (default 1)
     */
    public Edge(String id, String fromNodeId, String toNodeId, double length, int capacity) {
        this.id = id;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.length = length;
        this.capacity = capacity;
    }

    /**
     * Create an edge with default capacity of 1.
     *
     * @param id unique edge identifier
     * @param fromNodeId source node ID
     * @param toNodeId target node ID
     * @param length length in meters
     */
    public Edge(String id, String fromNodeId, String toNodeId, double length) {
        this(id, fromNodeId, toNodeId, length, 1);
    }

    public String getId() {
        return id;
    }

    public String getFromNodeId() {
        return fromNodeId;
    }

    public String getToNodeId() {
        return toNodeId;
    }

    public double getLength() {
        return length;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return id.equals(edge.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Edge{id='" + id + "', from='" + fromNodeId + "', to='" + toNodeId +
               "', length=" + length + ", capacity=" + capacity + "}";
    }
}
