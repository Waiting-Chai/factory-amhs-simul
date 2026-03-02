package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.*;

/**
 * Control point for traffic capacity management.
 * <p>
 * Controls vehicle access to specific network points with capacity limits.
 * Priority-based arbitration supported via PriorityManager.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class ControlPoint {

    private final String id;
    private final String atNodeId; // Node ID where this control point is located
    private final int capacity;
    private int currentLoad;
    private final Map<String, Vehicle> occupyingVehicles; // vehicleId -> Vehicle
    private final Map<String, Double> waitStartTime; // vehicleId -> wait start time
    private final Map<String, Vehicle> waitingVehicles; // vehicleId -> Vehicle (for priority arbitration)
    private final PriorityManager priorityManager; // Priority manager for aging calculation
    private final Random random; // Random for tie-breaking (4th arbitration rule)

    /**
     * Create a control point.
     *
     * @param id unique control point identifier
     * @param atNodeId node ID where this control point is located
     * @param capacity maximum simultaneous vehicles (default 1)
     */
    public ControlPoint(String id, String atNodeId, int capacity) {
        this.id = id;
        this.atNodeId = atNodeId;
        this.capacity = capacity;
        this.currentLoad = 0;
        this.occupyingVehicles = new HashMap<>();
        this.waitStartTime = new HashMap<>();
        this.waitingVehicles = new HashMap<>();
        this.priorityManager = new PriorityManager(); // Use default parameters per REQ-TC-000
        this.random = new Random(); // For random tie-breaking per REQ-TC-005
    }

    /**
     * Create a control point with default capacity of 1.
     *
     * @param id unique control point identifier
     * @param atNodeId node ID where this control point is located
     */
    public ControlPoint(String id, String atNodeId) {
        this(id, atNodeId, 1);
    }

    /**
     * Request entry to this control point.
     * <p>
     * REQ-TC-005: No cutting in line - if waiting queue exists, new requests must wait.
     * Even if capacity is available, vehicles must wait their turn through arbitration.
     *
     * @param vehicle vehicle requesting entry
     * @param currentTime current simulation time (from env.now())
     * @return true if entry granted, false if must wait
     * @throws IllegalArgumentException if vehicle is an Operator (Human)
     */
    public boolean requestEntry(Vehicle vehicle, double currentTime) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }

        // REQ-TC-005 & spec: Human operators cannot occupy ControlPoint
        if (vehicle.type() == EntityType.OPERATOR) {
            throw new IllegalArgumentException(
                "Human operators cannot occupy ControlPoint (only SafetyZone). Vehicle: " + vehicle.id());
        }

        synchronized (this) {
            // REQ-TC-005: No cutting in line - if waiting queue exists, must wait for arbitration
            if (currentLoad < capacity && waitingVehicles.isEmpty()) {
                occupyingVehicles.put(vehicle.id(), vehicle);
                currentLoad++;
                // Remove from waiting list when granted (fix for testControlPointRelease)
                waitStartTime.remove(vehicle.id());
                waitingVehicles.remove(vehicle.id());
                return true;
            }

            // Record wait start time and store vehicle for priority arbitration
            waitStartTime.putIfAbsent(vehicle.id(), currentTime);
            waitingVehicles.putIfAbsent(vehicle.id(), vehicle);
            return false;
        }
    }

    /**
     * Release this control point.
     * <p>
     * Per spec (Scenario: 故障车辆资源占用): Failed vehicles retain current resources.
     * If the vehicle is in FAILED state, the release is rejected and resources are retained.
     *
     * @param vehicle vehicle leaving
     */
    public void release(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }

        synchronized (this) {
            if (occupyingVehicles.containsKey(vehicle.id())) {
                // Spec (Scenario: 故障车辆资源占用): Failed vehicles retain current resources
                // Hard constraint: Failed vehicles keep their resources
                if (vehicle.getState() == VehicleState.FAILED) {
                    // Reject release: failed vehicle keeps the control point occupied
                    return;
                }

                occupyingVehicles.remove(vehicle.id());
                currentLoad--;
                waitStartTime.remove(vehicle.id());
                waitingVehicles.remove(vehicle.id());
            }
        }
    }

    /**
     * Release this control point and return the next vehicle that should be granted access.
     * <p>
     * Safety checks:
     * - Only vehicles currently occupying can release and trigger grant
     * - Failed vehicles cannot release (per spec: 故障车辆资源占用)
     * - Capacity is validated before granting to prevent overflow
     * <p>
     * This method completes the full grant process per REQ-TC-005:
     * 1) Arbitrates and selects top priority vehicle
     * 2) Removes from waiting queue
     * 3) Adds to occupying vehicles
     * 4) Increments current load
     * <p>
     * Arbitration rules per REQ-TC-005:
     * 1) Effective priority (base + aging)
     * 2) Wait time (FIFO)
     * 3) Distance (optional, not yet implemented)
     * 4) Random tie-break
     *
     * @param vehicle vehicle leaving
     * @param currentTime current simulation time (from env.now())
     * @return ID of the next vehicle that should be granted access, or null if no vehicles waiting or safety check fails
     */
    public String releaseAndGetNext(Vehicle vehicle, double currentTime) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }

        synchronized (this) {
            // Safety: Only allow releasing vehicles that are actually occupying
            boolean wasOccupying = occupyingVehicles.containsKey(vehicle.id());
            if (!wasOccupying) {
                // Vehicle is not occupying - do not grant anyone (prevents capacity overflow)
                return null;
            }

            // Spec (Scenario: 故障车辆资源占用): Failed vehicles retain current resources
            if (vehicle.getState() == VehicleState.FAILED) {
                // Reject release: failed vehicle keeps the control point occupied
                return null;
            }

            // Release the occupying vehicle
            occupyingVehicles.remove(vehicle.id());
            currentLoad--;
            waitStartTime.remove(vehicle.id());
            waitingVehicles.remove(vehicle.id());

            // Implement priority arbitration logic per REQ-TC-005
            if (waitingVehicles.isEmpty()) {
                return null;
            }

            // Safety: Validate capacity before granting (should always be true after release, but defensive)
            if (currentLoad >= capacity) {
                // Capacity is full - safety check prevents overflow
                return null;
            }

            String topVehicleId = null;
            int topEffectivePriority = -1;
            double topWaitStartTime = Double.MAX_VALUE;
            List<String> candidates = new ArrayList<>(); // For tie-breaking

            // First pass: Find highest effective priority and wait time
            for (Map.Entry<String, Vehicle> entry : waitingVehicles.entrySet()) {
                String vehicleId = entry.getKey();
                Vehicle waitingVehicle = entry.getValue();
                Double waitStart = waitStartTime.get(vehicleId);

                if (waitStart == null) {
                    continue;
                }

                // Calculate effective priority using PriorityManager
                int effectivePriority = priorityManager.calculateEffectivePriority(
                    waitingVehicle, waitStart, currentTime);

                // Arbitration rules per REQ-TC-005:
                // 1) Higher effective priority wins
                // 2) If tie, earlier wait start time (FIFO) wins
                // 3) Distance (not yet implemented)
                // 4) Random tie-break (implemented below)

                if (effectivePriority > topEffectivePriority) {
                    // Higher priority found, clear previous candidates
                    topVehicleId = vehicleId;
                    topEffectivePriority = effectivePriority;
                    topWaitStartTime = waitStart;
                    candidates.clear();
                    candidates.add(vehicleId);
                } else if (effectivePriority == topEffectivePriority) {
                    // Same priority, check wait time
                    if (waitStart < topWaitStartTime) {
                        // Earlier wait time found, clear previous candidates
                        topVehicleId = vehicleId;
                        topWaitStartTime = waitStart;
                        candidates.clear();
                        candidates.add(vehicleId);
                    } else if (waitStart == topWaitStartTime) {
                        // Same priority AND same wait time - collect candidates for random selection
                        topVehicleId = vehicleId; // Will be overwritten by random selection
                        topWaitStartTime = waitStart;
                        candidates.add(vehicleId);
                    }
                }
            }

            // Random tie-break: If multiple candidates with same priority and wait time, pick randomly
            if (candidates.size() > 1) {
                topVehicleId = candidates.get(random.nextInt(candidates.size()));
            }

            // Complete the grant process: remove from waiting, add to occupying, increment load
            if (topVehicleId != null) {
                Vehicle grantedVehicle = waitingVehicles.remove(topVehicleId);
                waitStartTime.remove(topVehicleId);
                occupyingVehicles.put(topVehicleId, grantedVehicle);
                currentLoad++;
            }

            return topVehicleId;
        }
    }

    /**
     * Get current load (number of vehicles).
     *
     * @return current load
     */
    public int getCurrentLoad() {
        return currentLoad;
    }

    /**
     * Get capacity.
     *
     * @return maximum capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Check if at capacity.
     *
     * @return true if full
     */
    public boolean isFull() {
        return currentLoad >= capacity;
    }

    /**
     * Check if empty.
     *
     * @return true if no vehicles
     */
    public boolean isEmpty() {
        return currentLoad == 0;
    }

    /**
     * Get available capacity.
     *
     * @return available slots
     */
    public int getAvailableCapacity() {
        return capacity - currentLoad;
    }

    /**
     * Get node ID where this control point is located.
     *
     * @return node ID
     */
    public String getAtNodeId() {
        return atNodeId;
    }

    /**
     * Get control point ID.
     *
     * @return control point ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get vehicles currently waiting.
     *
     * @return set of waiting vehicle IDs
     */
    public Set<String> getWaitingVehicleIds() {
        return Collections.unmodifiableSet(waitStartTime.keySet());
    }

    /**
     * Get waiting vehicles as a collection (for conflict arbitration).
     * <p>
     * Returns an unmodifiable collection of vehicles currently waiting for access.
     * This is used by TrafficManager to perform conflict arbitration when multiple
     * vehicles are competing for the same control point (REQ-TC-008).
     *
     * @return unmodifiable collection of waiting vehicles
     */
    public Collection<Vehicle> getWaitingVehicles() {
        return Collections.unmodifiableCollection(waitingVehicles.values());
    }

    /**
     * Get waiting time for a specific vehicle.
     *
     * @param vehicleId vehicle ID
     * @param currentTime current simulation time (from env.now())
     * @return waiting time, or 0 if not waiting
     */
    public double getWaitingTime(String vehicleId, double currentTime) {
        Double startTime = waitStartTime.get(vehicleId);
        if (startTime == null) {
            return 0.0;
        }
        return currentTime - startTime;
    }

    /**
     * Get wait start time map for blocking timeout monitoring (REQ-TC-007).
     * <p>
     * Returns the map of vehicle ID to wait start time for use by
     * BlockTimeoutMonitor to check if vehicles have been blocked too long.
     *
     * @return unmodifiable map of wait start times
     */
    public Map<String, Double> getWaitStartTime() {
        return Collections.unmodifiableMap(waitStartTime);
    }
}
