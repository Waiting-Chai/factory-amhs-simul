package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.control.conflict.ConflictResolver;
import com.semi.simlogistics.control.path.PathPlanner;
import com.semi.simlogistics.movement.TrackMovement;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified traffic controller for managing all traffic control components.
 * <p>
 * Implements REQ-TC-008, REQ-TC-009, and REQ-TC-010:
 * - Single entry point for all traffic requests (OHT/AGV/HUMAN)
 * - Event-driven processing mechanism
 * - Domain-based strategy selection (pluggable strategies)
 * - Control point, area, and edge management
 * - Conflict resolution integration
 * - Dual-layer coordination (ControlPoint + EdgeReservation)
 * <p>
 * Key features:
 * - Unified request passage through requestPassage()
 * - Event-driven synchronous processing (no polling)
 * - Pluggable domain strategies per path type
 * - Integrated conflict resolver for resource contention
 * - Automatic next vehicle grant on release
 * - Dual-layer control with rollback semantics
 * - Blocking timeout replan (REQ-TC-007)
 * <p>
 * Note: PathType and Movement interface are planned for future implementation.
 * Current version supports OHT_TRACK strategy via TrackMovement class.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-08
 */
public class TrafficManager {

    /**
     * Path type enum for domain strategy selection (REQ-TC-009).
     * <p>
     * Defines the type of path/movement for routing requests to
     * the appropriate traffic control strategy.
     */
    public enum PathType {
        /** OHT track movement (currently supported) */
        OHT_TRACK,
        /** AGV network movement (future) */
        AGV_NETWORK,
        /** Human-operated vehicle movement (future) */
        HUMAN_PATH
    }

    private final Map<String, ControlPoint> controlPoints;
    private final Map<String, ControlArea> controlAreas;
    private final Map<String, EdgeReservation> edges;
    private final Map<PathType, Boolean> registeredStrategies;
    private ConflictResolver conflictResolver;
    private BlockTimeoutMonitor blockTimeoutMonitor;

    /**
     * Create a traffic manager with default configuration.
     */
    public TrafficManager() {
        this.controlPoints = new HashMap<>();
        this.controlAreas = new HashMap<>();
        this.edges = new HashMap<>();
        this.registeredStrategies = new HashMap<>();
        this.conflictResolver = new ConflictResolver();
        this.blockTimeoutMonitor = null; // Will be initialized when PathPlanner is set

        // Register default OHT strategy (REQ-TC-009: OHT strategy by default)
        registeredStrategies.put(PathType.OHT_TRACK, true);
    }

    // ==================== Unified Entry Point ====================

    /**
     * Request passage through a control point (REQ-TC-009: unified entry).
     * <p>
     * This is the single entry point for all traffic requests regardless of
     * vehicle type (OHT, AGV, HUMAN). Processing is synchronous and event-driven.
     * <p>
     * <b>NOTE:</b> This method uses wall clock time (System.currentTimeMillis()) for
     * backward compatibility. For production use with simulation time, use
     * {@link #requestPassage(Vehicle, String, double)} instead.
     *
     * @param vehicle vehicle requesting passage
     * @param controlPointId ID of the control point
     * @return true if passage granted, false otherwise
     * @deprecated Use {@link #requestPassage(Vehicle, String, double)} with simulation time
     */
    @Deprecated
    public boolean requestPassage(Vehicle vehicle, String controlPointId) {
        return requestPassage(vehicle, controlPointId, System.currentTimeMillis());
    }

    /**
     * Request passage through a control point with simulation time (REQ-TC-008, REQ-TC-009).
     * <p>
     * This is the single entry point for all traffic requests regardless of
     * vehicle type (OHT, AGV, HUMAN). Processing is synchronous and event-driven.
     * <p>
     * When the control point is at capacity and rejects the request, the TrafficManager
     * performs conflict arbitration using the ConflictResolver to determine which
     * waiting vehicle should be granted access next (REQ-TC-008).
     * <p>
     * If the request is rejected and the vehicle has been waiting longer than
     * the timeout threshold, the blocking timeout replan mechanism is triggered (REQ-TC-007).
     *
     * @param vehicle vehicle requesting passage
     * @param controlPointId ID of the control point
     * @param currentTime current simulation time (from env.now())
     * @return true if passage granted, false otherwise
     */
    public boolean requestPassage(Vehicle vehicle, String controlPointId, double currentTime) {
        ControlPoint cp = controlPoints.get(controlPointId);
        if (cp == null) {
            return false; // Control point not found (graceful handling)
        }

        // Event-driven: process request immediately with simulation time
        boolean granted = cp.requestEntry(vehicle, currentTime);

        // If not granted due to capacity, perform conflict arbitration (REQ-TC-008)
        if (!granted) {
            // Get waiting vehicles from control point
            List<Vehicle> waitingVehicles = getWaitingVehicles(cp);
            if (waitingVehicles.size() > 1) {
                // Use conflict resolver to determine who should win
                Vehicle winner = conflictResolver.resolve(waitingVehicles);
                // Note: The actual granting happens when the current occupier releases
                // and releaseAndGetNext() is called. This arbitration ensures priority
                // is considered when selecting the next vehicle.
            }

            // REQ-TC-007: Check for blocking timeout and trigger replan if needed
            // If replan succeeds, retry passage request immediately
            boolean shouldRetry = checkAndTriggerReplan(vehicle, cp, currentTime);
            if (shouldRetry) {
                // Replan succeeded, retry passage request with new path
                granted = cp.requestEntry(vehicle, currentTime);
            }
        }

        return granted;
    }

    /**
     * Check if vehicle has been blocked too long and trigger replan (REQ-TC-007).
     * <p>
     * This is called automatically when requestPassage() fails due to capacity.
     * If replan succeeds, immediately retries passage request with new path.
     *
     * @param vehicle vehicle that was blocked
     * @param cp control point where vehicle is waiting
     * @param currentTime current simulation time (from env.now())
     * @return true if replan was triggered and retry should be attempted, false otherwise
     */
    private boolean checkAndTriggerReplan(Vehicle vehicle, ControlPoint cp, double currentTime) {
        if (blockTimeoutMonitor == null) {
            return false; // No monitor configured
        }

        Map<String, Double> waitStartTime = cp.getWaitStartTime();
        if (blockTimeoutMonitor.shouldTriggerReplan(vehicle, waitStartTime, currentTime)) {
            // Trigger replan: execute path planning with vehicle's transport type
            // Use vehicle's current and destination node IDs for replanning
            String startNodeId = vehicle.getCurrentNodeId();
            String endNodeId = vehicle.getDestinationNodeId();

            if (startNodeId != null && endNodeId != null) {
                boolean replanSuccess = blockTimeoutMonitor.executeReplan(vehicle, startNodeId, endNodeId);
                if (replanSuccess) {
                    // REQ-TC-007: After successful replan, retry passage request
                    // This ensures the new path is attempted immediately
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get list of waiting vehicles from a control point (for conflict arbitration).
     * <p>
     * Uses ControlPoint.getWaitingVehicles() to get the actual Vehicle objects
     * for conflict resolution via ConflictResolver (REQ-TC-008).
     *
     * @param cp control point
     * @return list of waiting vehicles
     */
    private List<Vehicle> getWaitingVehicles(ControlPoint cp) {
        return new ArrayList<>(cp.getWaitingVehicles());
    }

    /**
     * Release passage through a control point (REQ-TC-008: resource release with event-driven grant).
     * <p>
     * When a vehicle releases a control point, this method immediately triggers
     * the next waiting vehicle to be granted access (event-driven processing).
     * <p>
     * <b>NOTE:</b> This method uses wall clock time (System.currentTimeMillis()) for
     * backward compatibility. For production use with simulation time, use
     * {@link #releasePassage(Vehicle, String, double)} instead.
     *
     * @param vehicle vehicle releasing the resource
     * @param controlPointId ID of the control point
     * @return ID of the next vehicle granted access, or null if no vehicles waiting
     * @deprecated Use {@link #releasePassage(Vehicle, String, double)} with simulation time
     */
    @Deprecated
    public String releasePassage(Vehicle vehicle, String controlPointId) {
        return releasePassage(vehicle, controlPointId, System.currentTimeMillis());
    }

    /**
     * Release passage through a control point with simulation time (REQ-TC-008: event-driven).
     * <p>
     * When a vehicle releases a control point, this method immediately triggers
     * the next waiting vehicle to be granted access (event-driven processing).
     * <p>
     * Uses ControlPoint.releaseAndGetNext() which performs arbitration and
     * grants access to the highest priority waiting vehicle.
     * <p>
     * Also resets the blocking timeout replan counter for the vehicle that just passed (REQ-TC-007).
     *
     * @param vehicle vehicle releasing the resource
     * @param controlPointId ID of the control point
     * @param currentTime current simulation time (from env.now())
     * @return ID of the next vehicle granted access, or null if no vehicles waiting
     */
    public String releasePassage(Vehicle vehicle, String controlPointId, double currentTime) {
        ControlPoint cp = controlPoints.get(controlPointId);
        if (cp == null) {
            return null;
        }

        // Event-driven: release and immediately grant to next vehicle (REQ-TC-008)
        String grantedVehicleId = cp.releaseAndGetNext(vehicle, currentTime);

        // REQ-TC-007: Reset replan counter for vehicle that successfully passed through CP
        if (blockTimeoutMonitor != null) {
            blockTimeoutMonitor.onVehiclePassed(vehicle.id());
        }

        return grantedVehicleId;
    }

    /**
     * Request passage with timing measurement (for testing event-driven behavior).
     * <p>
     * <b>NOTE:</b> Uses wall clock time. Use requestPassage(vehicle, controlPointId, currentTime)
     * for production with simulation time.
     *
     * @param vehicle vehicle requesting passage
     * @param controlPointId ID of the control point
     * @return processing time in milliseconds
     * @deprecated Use {@link #requestPassage(Vehicle, String, double)} with simulation time
     */
    @Deprecated
    public long requestPassageWithTiming(Vehicle vehicle, String controlPointId) {
        long startTime = System.nanoTime();
        boolean granted = requestPassage(vehicle, controlPointId);
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Release passage with timing measurement (for testing event-driven behavior).
     * <p>
     * <b>NOTE:</b> Uses wall clock time. Use releasePassage(vehicle, controlPointId, currentTime)
     * for production with simulation time.
     *
     * @param vehicle vehicle releasing the resource
     * @param controlPointId ID of the control point
     * @return processing time in milliseconds
     * @deprecated Use {@link #releasePassage(Vehicle, String, double)} with simulation time
     */
    @Deprecated
    public long releasePassageWithTiming(Vehicle vehicle, String controlPointId) {
        long startTime = System.nanoTime();
        releasePassage(vehicle, controlPointId);
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    // ==================== Domain-Based Strategy Selection ====================

    /**
     * Request passage for a specific path (REQ-TC-009: domain strategy selection).
     * <p>
     * Routes the request to the appropriate strategy based on path type:
     * - OHT_TRACK: OHT traffic strategy (currently supported)
     * - AGV_NETWORK: AGV traffic strategy (future)
     * - Other: Default handling
     *
     * @param vehicle vehicle requesting passage
     * @param track movement track (OHT track)
     * @return true if passage granted, false otherwise
     */
    public boolean requestPassageForPath(Vehicle vehicle, TrackMovement track) {
        if (track == null) {
            return false;
        }

        // Check if OHT_TRACK strategy is registered (REQ-TC-009: pluggable strategies)
        if (!registeredStrategies.containsKey(PathType.OHT_TRACK) ||
            !registeredStrategies.get(PathType.OHT_TRACK)) {
            return false; // Strategy not registered
        }

        // OHT strategy: for now, grant passage if no control points block
        // In future, will check control points along the track path
        return true;
    }

    /**
     * Check if a strategy is registered for the given path type (REQ-TC-009).
     *
     * @param pathType path type to check
     * @return true if strategy is registered
     */
    public boolean hasStrategy(PathType pathType) {
        return registeredStrategies.containsKey(pathType) && registeredStrategies.get(pathType);
    }

    // ==================== Control Point Management ====================

    /**
     * Register a control point with the traffic manager (REQ-TC-008).
     *
     * @param controlPoint control point to register
     */
    public void registerControlPoint(ControlPoint controlPoint) {
        if (controlPoint != null && controlPoint.getId() != null) {
            controlPoints.put(controlPoint.getId(), controlPoint);
        }
    }

    /**
     * Get a registered control point by ID.
     *
     * @param controlPointId control point ID
     * @return the control point, or null if not found
     */
    public ControlPoint getControlPoint(String controlPointId) {
        return controlPoints.get(controlPointId);
    }

    /**
     * Get all registered control points.
     *
     * @return list of registered control points
     */
    public List<ControlPoint> getRegisteredControlPoints() {
        return new ArrayList<>(controlPoints.values());
    }

    // ==================== Edge Management ====================

    /**
     * Register an edge with the traffic manager (REQ-TC-010).
     *
     * @param edge edge reservation to register
     */
    public void registerEdge(EdgeReservation edge) {
        if (edge != null && edge.getEdgeId() != null) {
            edges.put(edge.getEdgeId(), edge);
        }
    }

    /**
     * Get a registered edge by ID.
     *
     * @param edgeId edge ID
     * @return the edge reservation, or null if not found
     */
    public EdgeReservation getEdge(String edgeId) {
        return edges.get(edgeId);
    }

    /**
     * Request passage through both control point and edge (REQ-TC-010: dual-layer coordination).
     * <p>
     * Implements dual-layer control with proper rollback semantics:
     * 1. Apply for ControlPoint first (REQ-TC-010: resource order)
     * 2. If CP succeeds, apply for Edge
     * 3. If Edge fails, rollback CP occupation
     * 4. Return true only if both succeed
     * <p>
     * This method ensures atomic acquisition of both resources or neither.
     *
     * @param vehicle vehicle requesting passage
     * @param controlPointId ID of the control point
     * @param edgeId ID of the edge
     * @param currentTime current simulation time (from env.now())
     * @return true if passage granted through both CP and Edge, false otherwise
     */
    public boolean requestPassageWithEdge(Vehicle vehicle, String controlPointId,
                                         String edgeId, double currentTime) {
        // Get control point and edge
        ControlPoint cp = controlPoints.get(controlPointId);
        EdgeReservation edge = edges.get(edgeId);

        if (cp == null || edge == null) {
            return false; // Resource not found (graceful handling)
        }

        // REQ-TC-010: Apply ControlPoint first, then Edge
        boolean cpGranted = cp.requestEntry(vehicle, currentTime);
        if (!cpGranted) {
            return false; // CP rejected, no need to try Edge
        }

        // CP succeeded, now try Edge
        boolean edgeGranted = edge.tryReserve(vehicle);
        if (!edgeGranted) {
            // REQ-TC-010: Rollback CP occupation when Edge fails
            // IMPORTANT: Use cp.release() NOT cp.releaseAndGetNext() for rollback
            // Rollback should NOT trigger next vehicle grant (spec: "任意申请失败时不占用任何资源")
            cp.release(vehicle);
            return false;
        }

        // Both succeeded
        return true;
    }

    /**
     * Release passage through both control point and edge (REQ-TC-010).
     * <p>
     * Releases both resources in proper order and triggers next vehicle grant.
     *
     * @param vehicle vehicle releasing the resources
     * @param controlPointId ID of the control point
     * @param edgeId ID of the edge
     * @param currentTime current simulation time (from env.now())
     * @return ID of the next vehicle granted access, or null if no vehicles waiting
     */
    public String releasePassageWithEdge(Vehicle vehicle, String controlPointId,
                                        String edgeId, double currentTime) {
        // Get control point and edge
        ControlPoint cp = controlPoints.get(controlPointId);
        EdgeReservation edge = edges.get(edgeId);

        if (cp == null || edge == null) {
            return null;
        }

        // Release both resources
        edge.release(vehicle);
        return cp.releaseAndGetNext(vehicle, currentTime);
    }

    // ==================== Control Area Management ====================

    /**
     * Register a control area with the traffic manager (REQ-TC-008).
     *
     * @param area control area to register
     */
    public void registerControlArea(ControlArea area) {
        if (area != null && area.getId() != null) {
            controlAreas.put(area.getId(), area);
        }
    }

    /**
     * Get a registered control area by ID.
     *
     * @param areaId area ID
     * @return the control area, or null if not found
     */
    public ControlArea getControlArea(String areaId) {
        return controlAreas.get(areaId);
    }

    /**
     * Enter a control area through the traffic manager.
     *
     * @param vehicle vehicle entering the area
     * @param areaId area ID
     * @return true if entry allowed, false otherwise
     */
    public boolean enterArea(Vehicle vehicle, String areaId) {
        ControlArea area = controlAreas.get(areaId);
        if (area == null) {
            return false;
        }
        return area.tryEnter(vehicle);
    }

    /**
     * Leave a control area through the traffic manager.
     *
     * @param vehicle vehicle leaving the area
     * @param areaId area ID
     */
    public void leaveArea(Vehicle vehicle, String areaId) {
        ControlArea area = controlAreas.get(areaId);
        if (area != null) {
            area.leave(vehicle);
        }
    }

    // ==================== Conflict Resolution ====================

    /**
     * Resolve conflict among competing vehicles (REQ-TC-007, REQ-TC-008).
     * <p>
     * Uses the configured conflict resolver to determine which vehicle wins.
     *
     * @param competingVehicles list of vehicles competing for the resource
     * @param <T> vehicle type
     * @return the winning vehicle, or null if list is empty
     */
    public <T extends Vehicle> T resolveConflict(List<T> competingVehicles) {
        return conflictResolver.resolve(competingVehicles);
    }

    /**
     * Set a custom conflict resolver (REQ-TC-008).
     *
     * @param resolver custom conflict resolver
     */
    public void setConflictResolver(ConflictResolver resolver) {
        if (resolver != null) {
            this.conflictResolver = resolver;
        }
    }

    /**
     * Get the current conflict resolver.
     *
     * @return the conflict resolver
     */
    public ConflictResolver getConflictResolver() {
        return conflictResolver;
    }

    // ==================== Blocking Timeout Replan (REQ-TC-007) ====================

    /**
     * Set the PathPlanner for blocking timeout replan (REQ-TC-007).
     * <p>
     * This enables automatic replanning when vehicles are blocked for too long.
     * The PathPlanner is used to generate alternative routes when timeout occurs.
     *
     * @param pathPlanner the path planner to use (can be null to disable replanning)
     */
    public void setPathPlanner(PathPlanner pathPlanner) {
        if (pathPlanner == null) {
            this.blockTimeoutMonitor = null;
        } else {
            this.blockTimeoutMonitor = new BlockTimeoutMonitor(pathPlanner);
        }
    }

    /**
     * Set a custom BlockTimeoutMonitor (REQ-TC-007).
     * <p>
     * This allows full control over the blocking timeout replan behavior.
     *
     * @param monitor the monitor to use (null to disable)
     */
    public void setBlockTimeoutMonitor(BlockTimeoutMonitor monitor) {
        this.blockTimeoutMonitor = monitor;
    }

    /**
     * Get the current BlockTimeoutMonitor (REQ-TC-007).
     *
     * @return the monitor, or null if not configured
     */
    public BlockTimeoutMonitor getBlockTimeoutMonitor() {
        return blockTimeoutMonitor;
    }

    // ==================== Statistics and Monitoring ====================

    /**
     * Get traffic manager statistics (REQ-TC-008).
     *
     * @return formatted statistics string
     */
    public String getStatistics() {
        return String.format(
                "TrafficManager[controlPoints=%d, controlAreas=%d, edges=%d, strategies=%d, timeoutMonitor=%s]",
                controlPoints.size(),
                controlAreas.size(),
                edges.size(),
                registeredStrategies.size(),
                blockTimeoutMonitor != null ? "enabled" : "disabled"
        );
    }

    @Override
    public String toString() {
        return getStatistics();
    }
}
