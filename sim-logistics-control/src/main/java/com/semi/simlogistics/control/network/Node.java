package com.semi.simlogistics.control.network;

/**
 * Node in the network graph.
 * <p>
 * Represents a point in the traffic network where vehicles can pass through.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Node {

    private final String id;
    private final double x; // meters
    private final double y; // meters
    private final double z; // meters (optional, default 0)

    /**
     * Create a node.
     *
     * @param id unique node identifier
     * @param x x coordinate in meters
     * @param y y coordinate in meters
     */
    public Node(String id, double x, double y) {
        this(id, x, y, 0.0);
    }

    /**
     * Create a node with 3D coordinates.
     *
     * @param id unique node identifier
     * @param x x coordinate in meters
     * @param y y coordinate in meters
     * @param z z coordinate in meters
     */
    public Node(String id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    /**
     * Calculate distance to another node.
     *
     * @param other target node
     * @return Euclidean distance in meters
     */
    public double distanceTo(Node other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Node{id='" + id + "', x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
