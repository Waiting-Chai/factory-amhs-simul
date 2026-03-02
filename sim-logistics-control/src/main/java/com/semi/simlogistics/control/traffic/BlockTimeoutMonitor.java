package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.control.path.Path;
import com.semi.simlogistics.control.path.PathPlanner;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Monitor for blocking timeout replan mechanism (REQ-TC-007).
 * <p>
 * Tracks vehicle waiting times and triggers replanning when:
 * 1. Vehicle waits longer than timeout threshold (default 60s)
 * 2. Replan attempts have not exceeded max attempts (default 3)
 * <p>
 * Uses simulation time (env.now()) not wall clock time.
 * After max attempts reached, vehicle continues waiting without further replanning.
 * <p>
 * Configuration from system_config (REQ-TC-000):
 * - traffic.replan.timeoutSeconds (default: 60)
 * - traffic.replan.maxAttempts (default: 3)
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-09
 */
public class BlockTimeoutMonitor {

    private final PathPlanner pathPlanner;
    private final double timeoutSeconds;
    private final int maxAttempts;
    private final Map<String, Integer> replanAttempts;

    /**
     * Create a BlockTimeoutMonitor with default configuration from system_config.
     *
     * @param pathPlanner path planner for replanning (can be null, replan will fail gracefully)
     */
    public BlockTimeoutMonitor(PathPlanner pathPlanner) {
        this(pathPlanner,
             SystemConfigProvider.getInstance().getReplanTimeoutSeconds(),
             SystemConfigProvider.getInstance().getReplanMaxAttempts());
    }

    /**
     * Create a BlockTimeoutMonitor with custom configuration.
     * <p>
     * Invalid values (<= 0) will fallback to system_config defaults.
     *
     * @param pathPlanner path planner for replanning (can be null, replan will fail gracefully)
     * @param timeoutSeconds timeout threshold in seconds (<= 0 uses system_config default)
     * @param maxAttempts maximum replan attempts (<= 0 uses system_config default)
     */
    public BlockTimeoutMonitor(PathPlanner pathPlanner, double timeoutSeconds, int maxAttempts) {
        this.pathPlanner = pathPlanner;

        // Use provided value or fallback to system_config default
        if (timeoutSeconds > 0) {
            this.timeoutSeconds = timeoutSeconds;
        } else {
            this.timeoutSeconds = SystemConfigProvider.getInstance().getReplanTimeoutSeconds();
        }

        if (maxAttempts > 0) {
            this.maxAttempts = maxAttempts;
        } else {
            this.maxAttempts = SystemConfigProvider.getInstance().getReplanMaxAttempts();
        }

        this.replanAttempts = new HashMap<>();
    }

    /**
     * Check if vehicle should trigger replan due to blocking timeout (REQ-TC-007).
     * <p>
     * Conditions for triggering replan:
     * 1. Vehicle is in wait queue (has wait start time)
     * 2. Wait time exceeds threshold
     * 3. Replan attempts < maxAttempts
     * <p>
     * If triggered, increments replan attempt counter.
     *
     * @param vehicle vehicle to check
     * @param waitStartTime map of vehicle ID to wait start time
     * @param currentTime current simulation time (from env.now())
     * @return true if replan should be triggered
     * @throws IllegalArgumentException if vehicle or waitStartTime is null
     */
    public boolean shouldTriggerReplan(Vehicle vehicle, Map<String, Double> waitStartTime, double currentTime) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        Objects.requireNonNull(waitStartTime, "Wait start time map cannot be null");

        String vehicleId = vehicle.id();

        // Check if vehicle is in wait queue
        Double waitStart = waitStartTime.get(vehicleId);
        if (waitStart == null) {
            return false; // Vehicle not waiting
        }

        // Check if wait time exceeds threshold
        double waited = currentTime - waitStart;
        if (waited <= timeoutSeconds) {
            return false; // Not exceeded threshold yet
        }

        // Check if max attempts reached
        int attempts = replanAttempts.getOrDefault(vehicleId, 0);
        if (attempts >= maxAttempts) {
            return false; // Max attempts reached, stop replanning
        }

        // Trigger replan: increment counter
        replanAttempts.put(vehicleId, attempts + 1);
        return true;
    }

    /**
     * Execute replan for a vehicle (REQ-TC-007).
     * <p>
     * Calls PathPlanner.planPath() with the vehicle's transport type to generate a new path.
     * Updates the vehicle's path if successful.
     *
     * @param vehicle vehicle to replan for
     * @param startNodeId start node ID
     * @param endNodeId end node ID
     * @return true if replan succeeded and path was updated, false otherwise
     * @throws IllegalArgumentException if vehicle, startNodeId, or endNodeId is null
     */
    public boolean executeReplan(Vehicle vehicle, String startNodeId, String endNodeId) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        Objects.requireNonNull(startNodeId, "Start node ID cannot be null");
        Objects.requireNonNull(endNodeId, "End node ID cannot be null");

        if (pathPlanner == null) {
            return false; // No PathPlanner available
        }

        // Get vehicle's transport type (OHT, AGV, HUMAN, CONVEYOR)
        var transportType = vehicle.getTransportType();

        // Call PathPlanner with correct transport type
        Path newPath = pathPlanner.planPath(startNodeId, endNodeId, transportType);

        if (newPath == null || newPath.isEmpty()) {
            return false; // No path available
        }

        // Update vehicle's path (convert Path to string representation)
        String pathString = pathToString(newPath);
        vehicle.setPath(pathString);

        return true;
    }

    /**
     * Convert Path to string representation for vehicle storage.
     *
     * @param path the path to convert
     * @return string of node IDs separated by "->"
     */
    private String pathToString(Path path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return String.join("->", path.getNodeIds());
    }

    /**
     * Reset replan counter when vehicle successfully passes (REQ-TC-007).
     *
     * @param vehicleId vehicle ID
     */
    public void onVehiclePassed(String vehicleId) {
        replanAttempts.remove(vehicleId);
    }

    /**
     * Get current replan attempt count for a vehicle.
     *
     * @param vehicleId vehicle ID
     * @return number of replan attempts
     */
    public int getReplanAttempts(String vehicleId) {
        return replanAttempts.getOrDefault(vehicleId, 0);
    }

    /**
     * Get configured timeout threshold.
     *
     * @return timeout in seconds
     */
    public double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Get configured max replan attempts.
     *
     * @return max attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Get the path planner used by this monitor.
     *
     * @return the PathPlanner, or null if none was set
     */
    public PathPlanner getPathPlanner() {
        return pathPlanner;
    }
}
