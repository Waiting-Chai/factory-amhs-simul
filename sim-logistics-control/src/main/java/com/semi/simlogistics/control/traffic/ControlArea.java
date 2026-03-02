package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Control area for traffic capacity management over large regions.
 * <p>
 * Implements capacity control, nesting (parent-child relationships),
 * and statistics tracking per REQ-TC-006.
 * <p>
 * Key features:
 * - Capacity limit (default from system_config, fallback to 3)
 * - Nested structure support (entering child area also occupies parent)
 * - Statistics: current count, peak count, average wait time
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class ControlArea {

    private final String id;
    private final ControlArea parent;
    private final List<ControlArea> children;
    private final int capacity;
    private final Set<Vehicle> currentVehicles;

    // Statistics
    private int peakVehicleCount;
    private long totalWaitTimeMs;
    private int totalWaitingVehicles;

    /**
     * Default capacity per REQ-TC-000 (controlarea.default_capacity).
     */
    private static final int DEFAULT_CAPACITY = 3;

    /**
     * Create a control area.
     *
     * @param id unique area identifier
     * @param parent parent area (null for top-level areas)
     * @param capacity maximum simultaneous vehicles (0 = use default)
     */
    public ControlArea(String id, ControlArea parent, int capacity) {
        this.id = id;
        this.parent = parent;
        this.capacity = capacity > 0 ? capacity : DEFAULT_CAPACITY;
        this.children = new ArrayList<>();
        this.currentVehicles = new HashSet<>();

        // Initialize statistics
        this.peakVehicleCount = 0;
        this.totalWaitTimeMs = 0;
        this.totalWaitingVehicles = 0;

        // Register with parent
        if (parent != null) {
            parent.addChild(this);
        }
    }

    /**
     * Create a top-level control area with default capacity.
     *
     * @param id unique area identifier
     */
    public ControlArea(String id) {
        this(id, null, DEFAULT_CAPACITY);
    }

    /**
     * Try to enter the control area.
     * <p>
     * For nested areas, first checks parent capacity before occupying child.
     * This ensures consistency: parent full prevents child entry per REQ-TC-006.
     *
     * @param vehicle vehicle attempting to enter
     * @return true if entry allowed, false if at capacity
     */
    public boolean tryEnter(Vehicle vehicle) {
        if (currentVehicles.contains(vehicle)) {
            return true; // Already inside
        }

        // Check parent capacity first (nested consistency per REQ-TC-006)
        if (parent != null) {
            // Recursively check if parent can accept this vehicle
            // Note: We check but don't occupy parent yet - we'll do that after this area succeeds
            if (parent.getCurrentVehicleCount() >= parent.getCapacity()) {
                // Parent is full, reject entry to child area
                return false;
            }
        }

        // Check this area's capacity
        if (currentVehicles.size() >= capacity) {
            return false;
        }

        // Add to this area
        currentVehicles.add(vehicle);
        updatePeak();

        // Occupy parent area after this area succeeds (nested capacity per REQ-TC-006)
        if (parent != null) {
            parent.tryEnter(vehicle);
        }

        return true;
    }

    /**
     * Leave the control area.
     * <p>
     * Note: When leaving a child area, parent capacity is NOT released.
     * The vehicle is still in the parent area until explicitly leaving it.
     *
     * @param vehicle vehicle leaving the area
     */
    public void leave(Vehicle vehicle) {
        if (currentVehicles.remove(vehicle)) {
            // Vehicle successfully removed from this area
            // Parent area occupancy is NOT affected per REQ-TC-006
        }
    }

    /**
     * Add a child area to this area.
     *
     * @param child child area
     */
    private void addChild(ControlArea child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }

    /**
     * Update peak vehicle count statistic.
     */
    private void updatePeak() {
        int currentCount = currentVehicles.size();
        if (currentCount > peakVehicleCount) {
            peakVehicleCount = currentCount;
        }
    }

    /**
     * Record wait time for a vehicle.
     * <p>
     * Should be called when a waiting vehicle finally enters the area.
     *
     * @param vehicle vehicle that waited
     * @param waitTimeMs wait time in simulation time units
     */
    public void recordWaitTime(Vehicle vehicle, long waitTimeMs) {
        totalWaitTimeMs += waitTimeMs;
        totalWaitingVehicles++;
    }

    /**
     * Reset all statistics but keep current vehicles.
     * <p>
     * Clears peak, wait time metrics, but does NOT remove vehicles.
     */
    public void resetStatistics() {
        peakVehicleCount = 0;
        totalWaitTimeMs = 0;
        totalWaitingVehicles = 0;
        // Note: currentVehicles is NOT cleared - vehicles remain inside
    }

    // ==================== Getters ====================

    /**
     * Get area ID.
     *
     * @return area identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Get parent area.
     *
     * @return parent area, or null if top-level
     */
    public ControlArea getParent() {
        return parent;
    }

    /**
     * Get child areas.
     *
     * @return list of child areas
     */
    public List<ControlArea> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * Get capacity limit.
     *
     * @return maximum simultaneous vehicles
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Get current vehicle count.
     *
     * @return number of vehicles currently in area
     */
    public int getCurrentVehicleCount() {
        return currentVehicles.size();
    }

    /**
     * Get peak vehicle count (historical maximum).
     *
     * @return peak concurrent vehicles
     */
    public int getPeakVehicleCount() {
        return peakVehicleCount;
    }

    /**
     * Get average wait time.
     *
     * @return average wait time in milliseconds
     */
    public long getAverageWaitTimeMs() {
        if (totalWaitingVehicles == 0) {
            return 0;
        }
        return totalWaitTimeMs / totalWaitingVehicles;
    }

    /**
     * Get total number of vehicles that have waited.
     *
     * @return total waiting vehicles
     */
    public int getTotalWaitingVehicles() {
        return totalWaitingVehicles;
    }

    /**
     * Get area information as a formatted string.
     *
     * @return area info string
     */
    public String getAreaInfo() {
        return String.format("ControlArea[id=%s, capacity=%d, current=%d, peak=%d, avgWaitMs=%d]",
                id, capacity, currentVehicles.size(), peakVehicleCount, getAverageWaitTimeMs());
    }

    @Override
    public String toString() {
        return getAreaInfo();
    }
}
