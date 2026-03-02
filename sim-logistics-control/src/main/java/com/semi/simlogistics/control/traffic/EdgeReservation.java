package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Edge reservation for dual-layer traffic control.
 * <p>
 * Implements REQ-TC-010:
 * - Integer capacity control (default 1 from system_config)
 * - Dual-layer coordination with ControlPoint
 * - Resource application order: ControlPoint first, then Edge
 * - Rollback on Edge failure after CP success
 * - Failed vehicle retains current resources
 * <p>
 * Key features:
 * - Capacity-based occupation control
 * - Vehicle tracking for proper release
 * - Statistics tracking (load, capacity, full status)
 * - Default capacity of 1 when 0 is specified
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class EdgeReservation {

    private final String edgeId;
    private final int capacity;
    private int currentLoad;
    private final Set<Vehicle> occupyingVehicles;

    /**
     * Create an edge reservation with specified capacity.
     * <p>
     * Per REQ-TC-010: If capacity is 0, default to 1 (from system_config).
     *
     * @param edgeId unique edge identifier
     * @param capacity maximum simultaneous vehicles (0 means use default of 1)
     */
    public EdgeReservation(String edgeId, int capacity) {
        this.edgeId = edgeId;
        // REQ-TC-010: Default capacity is 1 when 0 is specified
        this.capacity = capacity > 0 ? capacity : 1;
        this.currentLoad = 0;
        this.occupyingVehicles = new HashSet<>();
    }

    /**
     * Try to reserve this edge for a vehicle (REQ-TC-010: edge occupation application).
     * <p>
     * If the vehicle is already occupying, the request is ignored (no-op).
     * If capacity is available, the vehicle is added to occupying set.
     *
     * @param vehicle vehicle requesting occupation
     * @return true if reservation granted, false if capacity full
     * @throws IllegalArgumentException if vehicle is null
     */
    public boolean tryReserve(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }

        synchronized (this) {
            // If already occupying, ignore (no-op)
            if (occupyingVehicles.contains(vehicle)) {
                return true;
            }

            // Check capacity
            if (currentLoad >= capacity) {
                return false;
            }

            // Grant reservation
            occupyingVehicles.add(vehicle);
            currentLoad++;
            return true;
        }
    }

    /**
     * Release this edge (REQ-TC-010: edge occupation release).
     * <p>
     * Per spec (Scenario: 故障车辆资源占用): Failed vehicles retain current resources.
     * If the vehicle is in FAILED state, the release is rejected and resources are retained.
     * <p>
     * If the vehicle is not occupying, this is a no-op (graceful handling).
     *
     * @param vehicle vehicle leaving the edge
     * @throws IllegalArgumentException if vehicle is null
     */
    public void release(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }

        synchronized (this) {
            if (occupyingVehicles.contains(vehicle)) {
                // REQ-TC-010: Failed vehicles retain current resources (hard constraint)
                if (vehicle.getState() == VehicleState.FAILED) {
                    // Reject release: failed vehicle keeps the edge occupied
                    return;
                }
                occupyingVehicles.remove(vehicle);
                currentLoad--;
            }
            // If vehicle not occupying, no-op (graceful handling)
        }
    }

    /**
     * Get current load (number of vehicles occupying).
     *
     * @return current load
     */
    public int getCurrentLoad() {
        return currentLoad;
    }

    /**
     * Get capacity (maximum simultaneous vehicles).
     *
     * @return capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Check if edge is at capacity.
     *
     * @return true if full
     */
    public boolean isFull() {
        return currentLoad >= capacity;
    }

    /**
     * Get edge information as formatted string.
     *
     * @return formatted edge info
     */
    public String getEdgeInfo() {
        return String.format("Edge[id=%s, load=%d, capacity=%d, full=%s]",
                edgeId, currentLoad, capacity, isFull());
    }

    /**
     * Get edge ID.
     *
     * @return edge ID
     */
    public String getEdgeId() {
        return edgeId;
    }

    /**
     * Get set of vehicles currently occupying this edge.
     *
     * @return unmodifiable set of occupying vehicles
     */
    public Set<Vehicle> getOccupyingVehicles() {
        return Collections.unmodifiableSet(occupyingVehicles);
    }

    @Override
    public String toString() {
        return getEdgeInfo();
    }
}
