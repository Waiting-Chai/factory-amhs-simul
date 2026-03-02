package com.semi.simlogistics.scheduler.pool;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.vehicle.Vehicle;
import com.semi.simlogistics.core.VehicleState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Vehicle pool for managing available vehicles (REQ-DS-007).
 * <p>
 * Provides live view of vehicle state and filtering capabilities
 * for transport type and entity type. The pool maintains a live
 * reference to the vehicle list, so state changes are immediately
 * visible to the dispatch engine.
 * <p>
 * Thread-safe: Uses ConcurrentHashMap for vehicle tracking and
 * returns unmodifiable snapshots for query results.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class VehiclePool {

    /**
     * Live reference to the vehicle list (not a copy).
     * <p>
     * Changes to vehicle states are immediately visible through
     * queries, supporting the "live view" requirement.
     */
    private final List<Vehicle> vehicles;

    /**
     * Set of manually assigned vehicle IDs (REQ-DS-005).
     * <p>
     * Vehicles with manual assignments are excluded from automatic
     * dispatch to prevent override of user decisions.
     */
    private final Set<String> manuallyAssignedVehicleIds;

    /**
     * Set of manually locked task IDs (REQ-DS-005).
     * <p>
     * Tasks with manual locks are excluded from automatic dispatch.
     */
    private final Set<String> manuallyLockedTaskIds;

    /**
     * Create a new vehicle pool.
     *
     * @param vehicles live list of vehicles (must not be null)
     * @throws NullPointerException if vehicles is null
     */
    public VehiclePool(List<Vehicle> vehicles) {
        this.vehicles = Objects.requireNonNull(vehicles, "Vehicle list cannot be null");
        this.manuallyAssignedVehicleIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.manuallyLockedTaskIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    /**
     * Get all available (IDLE) vehicles (REQ-DS-007).
     * <p>
     * Returns an unmodifiable snapshot of currently IDLE vehicles,
     * excluding those with manual assignments.
     *
     * @return unmodifiable list of available vehicles
     */
    public List<Vehicle> getAvailableVehicles() {
        return getVehiclesMatching(this::isAvailableForDispatch);
    }

    /**
     * Get available vehicles filtered by transport type (REQ-DS-007).
     *
     * @param transportType transport type to filter by
     * @return unmodifiable list of available vehicles with matching transport type
     * @throws NullPointerException if transportType is null
     */
    public List<Vehicle> getAvailableVehiclesByTransportType(TransportType transportType) {
        Objects.requireNonNull(transportType, "TransportType cannot be null");
        return getVehiclesMatching(v ->
                isAvailableForDispatch(v) && v.getTransportType() == transportType);
    }

    /**
     * Get available vehicles filtered by entity type (REQ-DS-007).
     *
     * @param entityType entity type to filter by
     * @return unmodifiable list of available vehicles with matching entity type
     * @throws NullPointerException if entityType is null
     */
    public List<Vehicle> getAvailableVehiclesByEntityType(EntityType entityType) {
        Objects.requireNonNull(entityType, "EntityType cannot be null");
        return getVehiclesMatching(v ->
                isAvailableForDispatch(v) && v.type() == entityType);
    }

    /**
     * Get all vehicles (live reference).
     * <p>
     * This returns the actual list reference, not a copy. Changes
     * to vehicle states are immediately visible.
     *
     * @return live list of all vehicles
     */
    public List<Vehicle> getAllVehicles() {
        return vehicles;
    }

    /**
     * Find a vehicle by ID.
     *
     * @param vehicleId vehicle ID to find
     * @return vehicle if found, null otherwise
     */
    public Vehicle getVehicleById(String vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        return vehicles.stream()
                .filter(v -> v.id().equals(vehicleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mark a vehicle as manually assigned (REQ-DS-005).
     * <p>
     * Manually assigned vehicles are excluded from automatic dispatch
     * to prevent override of user decisions.
     *
     * @param vehicleId vehicle ID to mark as manually assigned
     * @return true if marked, false if vehicle not found
     */
    public boolean markManuallyAssigned(String vehicleId) {
        if (getVehicleById(vehicleId) == null) {
            return false;
        }
        manuallyAssignedVehicleIds.add(vehicleId);
        return true;
    }

    /**
     * Unmark a vehicle as manually assigned (REQ-DS-005).
     * <p>
     * Allows the vehicle to be considered for automatic dispatch again.
     *
     * @param vehicleId vehicle ID to unmark
     * @return true if unmarked, false if was not marked
     */
    public boolean unmarkManuallyAssigned(String vehicleId) {
        return manuallyAssignedVehicleIds.remove(vehicleId);
    }

    /**
     * Check if a vehicle is manually assigned.
     *
     * @param vehicleId vehicle ID to check
     * @return true if vehicle is manually assigned
     */
    public boolean isManuallyAssigned(String vehicleId) {
        return manuallyAssignedVehicleIds.contains(vehicleId);
    }

    /**
     * Lock a task to prevent automatic dispatch (REQ-DS-005).
     * <p>
     * Manually locked tasks are excluded from automatic dispatch.
     *
     * @param taskId task ID to lock
     */
    public void lockTask(String taskId) {
        manuallyLockedTaskIds.add(taskId);
    }

    /**
     * Unlock a task to allow automatic dispatch (REQ-DS-005).
     *
     * @param taskId task ID to unlock
     * @return true if unlocked, false if was not locked
     */
    public boolean unlockTask(String taskId) {
        return manuallyLockedTaskIds.remove(taskId);
    }

    /**
     * Check if a task is manually locked.
     *
     * @param taskId task ID to check
     * @return true if task is locked
     */
    public boolean isTaskLocked(String taskId) {
        return manuallyLockedTaskIds.contains(taskId);
    }

    /**
     * Clear all manual assignments and locks.
     * <p>
     * Typically called between test scenarios or when resetting
     * the dispatch state.
     */
    public void clearManualAssignments() {
        manuallyAssignedVehicleIds.clear();
        manuallyLockedTaskIds.clear();
    }

    /**
     * Get the number of manually assigned vehicles.
     *
     * @return count of manually assigned vehicles
     */
    public int getManuallyAssignedCount() {
        return manuallyAssignedVehicleIds.size();
    }

    /**
     * Get the number of locked tasks.
     *
     * @return count of locked tasks
     */
    public int getLockedTaskCount() {
        return manuallyLockedTaskIds.size();
    }

    /**
     * Get vehicles matching a predicate.
     * <p>
     * Returns an unmodifiable snapshot of vehicles matching the criteria.
     *
     * @param predicate filter predicate
     * @return unmodifiable list of matching vehicles
     */
    private List<Vehicle> getVehiclesMatching(Predicate<Vehicle> predicate) {
        return vehicles.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Check if a vehicle is available for automatic dispatch.
     * <p>
     * A vehicle is available if:
     * <ul>
     *   <li>It is in IDLE state</li>
     *   <li>It is not manually assigned</li>
     * </ul>
     *
     * @param vehicle vehicle to check
     * @return true if available for automatic dispatch
     */
    private boolean isAvailableForDispatch(Vehicle vehicle) {
        return vehicle.getState() == VehicleState.IDLE
                && !manuallyAssignedVehicleIds.contains(vehicle.id());
    }
}
