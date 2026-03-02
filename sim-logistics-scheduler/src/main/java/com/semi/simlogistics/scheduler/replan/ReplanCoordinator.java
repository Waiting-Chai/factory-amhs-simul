package com.semi.simlogistics.scheduler.replan;

import com.semi.simlogistics.control.path.Path;
import com.semi.simlogistics.control.path.PathPlanner;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.scheduler.replan.strategy.ReplanStrategy;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskStatus;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinator for dynamic path replanning (REQ-DS-006).
 * <p>
 * Central point for handling all replanning requests. Manages replan
 * attempt counters and delegates to appropriate strategies based on trigger type.
 * <p>
 * Supported triggers:
 * - PATH_BLOCKED: Vehicle blocked at control point, needs alternative route
 * - TASK_CANCELLED: Current task cancelled, vehicle needs new destination
 * - TRAFFIC_CONFLICT: Traffic manager requests replan to avoid deadlock
 * <p>
 * Constraints (from spec):
 * - Uses simulation time (not wall clock)
 * - Respects max replan attempts from system_config
 * - Does not cause dispatch loop to spin or block
 * - Handles task state machine transitions correctly
 * - Preserves DispatchEngine's single-snapshot processing semantics
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-09
 */
public class ReplanCoordinator {

    private final PathPlanner pathPlanner;
    private final Map<String, Integer> replanAttempts;
    private final Map<ReplanTrigger, ReplanStrategy> strategies;
    private TaskNodeResolver taskNodeResolver;
    private String standbyPoint;
    private int maxReplanAttempts;

    /**
     * Create a replan coordinator with default configuration.
     *
     * @param pathPlanner path planner for calculating new routes
     * @throws NullPointerException if pathPlanner is null
     */
    public ReplanCoordinator(PathPlanner pathPlanner) {
        this(pathPlanner, null);
    }

    /**
     * Create a replan coordinator with custom task node resolver.
     *
     * @param pathPlanner path planner for calculating new routes
     * @param taskNodeResolver resolver for mapping task positions to node IDs
     * @throws NullPointerException if pathPlanner is null
     */
    public ReplanCoordinator(PathPlanner pathPlanner, TaskNodeResolver taskNodeResolver) {
        this.pathPlanner = Objects.requireNonNull(pathPlanner, "PathPlanner cannot be null");
        this.taskNodeResolver = taskNodeResolver;
        this.replanAttempts = new HashMap<>();
        this.strategies = new HashMap<>();
        this.maxReplanAttempts = 3; // Default from system_config

        // Use default resolver if none provided
        if (this.taskNodeResolver == null) {
            this.taskNodeResolver = new TaskNodeResolver.DefaultTaskNodeResolver();
        }

        // Register default strategies
        registerDefaultStrategies();
    }

    /**
     * Set the task node resolver.
     *
     * @param taskNodeResolver the resolver to use
     */
    public void setTaskNodeResolver(TaskNodeResolver taskNodeResolver) {
        this.taskNodeResolver = Objects.requireNonNull(taskNodeResolver, "TaskNodeResolver cannot be null");
        // Re-register strategies with new resolver
        registerDefaultStrategies();
    }

    /**
     * Request replanning for a vehicle.
     * <p>
     * This is the main entry point for replanning requests. It:
     * 1. Validates input parameters
     * 2. Checks max replan attempts limit
     * 3. Delegates to appropriate strategy based on trigger type
     * 4. Tracks replan attempt counters
     * 5. Returns result indicating success or failure
     * <p>
     * Time semantics: Uses simulation time (currentTime parameter), not wall clock.
     *
     * @param vehicle vehicle to replan for (must not be null)
     * @param currentTask current task (may be null)
     * @param newTask new task if available (null for non-task-cancel scenarios)
     * @param trigger what triggered this replan request
     * @param currentTime current simulation time from env.now()
     * @return replan result
     */
    public ReplanResult requestReplan(Vehicle vehicle, Task currentTask,
                                      Task newTask, ReplanTrigger trigger, double currentTime) {
        // Validate inputs
        if (vehicle == null) {
            return ReplanResult.failure("Vehicle cannot be null", trigger);
        }

        String vehicleId = vehicle.id();

        // Check max replan attempts (except for task cancellation which should always be handled)
        if (trigger != ReplanTrigger.TASK_CANCELLED) {
            int attempts = replanAttempts.getOrDefault(vehicleId, 0);
            if (attempts >= maxReplanAttempts) {
                return ReplanResult.failure(
                        String.format("Maximum replan attempts (%d) reached for vehicle %s",
                                maxReplanAttempts, vehicleId), trigger);
            }
            // Increment attempt counter
            replanAttempts.put(vehicleId, attempts + 1);
        }

        // Get strategy for trigger type
        ReplanStrategy strategy = strategies.get(trigger);
        if (strategy == null) {
            return ReplanResult.failure("No strategy registered for trigger: " + trigger, trigger);
        }

        // Execute strategy
        try {
            ReplanResult result = strategy.execute(vehicle, currentTask, newTask, currentTime);

            // If successful, update vehicle with new path
            if (result.isSuccess() && result.getNewPath().isPresent()) {
                vehicle.setPath(result.getNewPath().get());
            }

            return result;

        } catch (Exception e) {
            // Catch any strategy exceptions to prevent dispatch loop from hanging
            return ReplanResult.failure("Strategy execution failed: " + e.getMessage(), trigger);
        }
    }

    /**
     * Handle task cancellation - DispatchEngine integration point (REQ-DS-006).
     * <p>
     * Called by DispatchEngine when a task is cancelled. This method:
     * 1. Validates task state transition
     * 2. Stops the vehicle
     * 3. Replans to new task or standby point
     * <p>
     * This is a convenience wrapper around requestReplan() for task cancellation.
     *
     * @param vehicle vehicle assigned to cancelled task
     * @param cancelledTask the task that was cancelled
     * @param newTask new task to assign (null if no new task)
     * @param currentTime current simulation time from env.now()
     * @return replan result
     */
    public ReplanResult onTaskCancelled(Vehicle vehicle, Task cancelledTask,
                                       Task newTask, double currentTime) {
        if (vehicle == null) {
            return ReplanResult.failure("Vehicle cannot be null", ReplanTrigger.TASK_CANCELLED);
        }
        if (cancelledTask == null) {
            return ReplanResult.failure("Cancelled task cannot be null", ReplanTrigger.TASK_CANCELLED);
        }

        // Validate task state - should be CANCELLED or CANCELLING
        TaskStatus status = cancelledTask.getStatus();
        if (status != TaskStatus.CANCELLED && status != TaskStatus.CANCELLING) {
            return ReplanResult.failure("Task is not in cancelled state: " + status,
                    ReplanTrigger.TASK_CANCELLED);
        }

        return requestReplan(vehicle, cancelledTask, newTask, ReplanTrigger.TASK_CANCELLED, currentTime);
    }

    /**
     * Handle path blocking - DispatchEngine integration point (REQ-DS-006).
     * <p>
     * Called when a vehicle is blocked at a control point and needs an alternative route.
     *
     * @param vehicle blocked vehicle
     * @param currentTask current task (may be null)
     * @param currentTime current simulation time from env.now()
     * @return replan result
     */
    public ReplanResult onPathBlocked(Vehicle vehicle, Task currentTask, double currentTime) {
        return requestReplan(vehicle, currentTask, null, ReplanTrigger.PATH_BLOCKED, currentTime);
    }

    /**
     * Handle traffic conflict - TrafficManager integration point (REQ-DS-006).
     * <p>
     * Called by TrafficManager when replanning is needed to avoid deadlock.
     *
     * @param vehicle vehicle to replan
     * @param currentTask current task
     * @param currentTime current simulation time from env.now()
     * @return replan result
     */
    public ReplanResult onTrafficConflict(Vehicle vehicle, Task currentTask, double currentTime) {
        return requestReplan(vehicle, currentTask, null, ReplanTrigger.TRAFFIC_CONFLICT, currentTime);
    }

    /**
     * Reset replan counter for a vehicle.
     * <p>
     * Called when vehicle successfully passes through a control point
     * or completes a task, allowing it to replan again if needed.
     *
     * @param vehicleId vehicle ID
     */
    public void onVehiclePassed(String vehicleId) {
        replanAttempts.remove(vehicleId);
    }

    /**
     * Explicitly reset replan counter (for testing or manual intervention).
     *
     * @param vehicleId vehicle ID
     */
    public void resetReplanCounter(String vehicleId) {
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
     * Set maximum replan attempts per vehicle.
     * <p>
     * Default is 3 from system_config (traffic.replan.maxAttempts).
     *
     * @param maxAttempts maximum attempts (must be > 0)
     * @throws IllegalArgumentException if maxAttempts <= 0
     */
    public void setMaxReplanAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be > 0, got: " + maxAttempts);
        }
        this.maxReplanAttempts = maxAttempts;
    }

    /**
     * Get maximum replan attempts.
     *
     * @return max attempts
     */
    public int getMaxReplanAttempts() {
        return maxReplanAttempts;
    }

    /**
     * Set the standby point for vehicles with no task.
     * <p>
     * When a task is cancelled and no new task is available, vehicles
     * will return to this standby point.
     *
     * @param standbyPoint node ID of standby location
     */
    public void setStandbyPoint(String standbyPoint) {
        this.standbyPoint = standbyPoint;
    }

    /**
     * Get the standby point.
     *
     * @return standby point node ID, or null if not set
     */
    public String getStandbyPoint() {
        return standbyPoint;
    }

    /**
     * Register a custom replan strategy.
     * <p>
     * Allows overriding default strategies or adding new ones.
     *
     * @param strategy strategy to register
     * @throws NullPointerException if strategy is null
     */
    public void registerStrategy(ReplanStrategy strategy) {
        Objects.requireNonNull(strategy, "Strategy cannot be null");
        strategies.put(strategy.getTriggerType(), strategy);
    }

    /**
     * Get the path planner used by this coordinator.
     *
     * @return the path planner
     */
    public PathPlanner getPathPlanner() {
        return pathPlanner;
    }

    /**
     * Get the task node resolver.
     *
     * @return the task node resolver
     */
    public TaskNodeResolver getTaskNodeResolver() {
        return taskNodeResolver;
    }

    /**
     * Register default replan strategies.
     */
    private void registerDefaultStrategies() {
        strategies.put(ReplanTrigger.PATH_BLOCKED,
                new PathBlockReplanStrategy(pathPlanner));
        strategies.put(ReplanTrigger.TASK_CANCELLED,
                new TaskCancelReplanStrategy(pathPlanner, this, taskNodeResolver));
        strategies.put(ReplanTrigger.TRAFFIC_CONFLICT,
                new TrafficConflictReplanStrategy(pathPlanner));
    }

    // ==================== Default Strategy Implementations ====================

    /**
     * Strategy for path blocked replanning.
     * <p>
     * Calculates alternative route from current position to original destination.
     */
    private static class PathBlockReplanStrategy implements ReplanStrategy {

        private final PathPlanner pathPlanner;

        PathBlockReplanStrategy(PathPlanner pathPlanner) {
            this.pathPlanner = pathPlanner;
        }

        @Override
        public ReplanResult execute(Vehicle vehicle, Task currentTask, Task newTask, double currentTime) {
            String currentNodeId = vehicle.getCurrentNodeId();
            String destinationNodeId = vehicle.getDestinationNodeId();

            if (currentNodeId == null) {
                return ReplanResult.failure("Vehicle has no current node ID", getTriggerType());
            }
            if (destinationNodeId == null) {
                return ReplanResult.failure("Vehicle has no destination node ID", getTriggerType());
            }

            // Plan alternative path
            TransportType transportType = vehicle.getTransportType();
            Path newPath = pathPlanner.planPath(currentNodeId, destinationNodeId, transportType);

            if (newPath == null || newPath.isEmpty()) {
                return ReplanResult.failure("No alternative path available from " +
                        currentNodeId + " to " + destinationNodeId, getTriggerType());
            }

            String pathString = pathToString(newPath);
            return ReplanResult.success(pathString, getTriggerType());
        }

        @Override
        public ReplanTrigger getTriggerType() {
            return ReplanTrigger.PATH_BLOCKED;
        }

        private String pathToString(Path path) {
            return String.join("->", path.getNodeIds());
        }
    }

    /**
     * Strategy for task cancellation replanning.
     * <p>
     * - If new task available: plan to new task start point using TaskNodeResolver
     * - If no new task: plan to standby point or stop at current location
     */
    private static class TaskCancelReplanStrategy implements ReplanStrategy {

        private final PathPlanner pathPlanner;
        private final ReplanCoordinator coordinator;
        private final TaskNodeResolver taskNodeResolver;

        TaskCancelReplanStrategy(PathPlanner pathPlanner, ReplanCoordinator coordinator,
                                 TaskNodeResolver taskNodeResolver) {
            this.pathPlanner = pathPlanner;
            this.coordinator = coordinator;
            this.taskNodeResolver = taskNodeResolver;
        }

        @Override
        public ReplanResult execute(Vehicle vehicle, Task currentTask, Task newTask, double currentTime) {
            // Stop current movement
            vehicle.setState(VehicleState.IDLE);

            String currentNodeId = vehicle.getCurrentNodeId();
            if (currentNodeId == null) {
                // Can't determine position, just stop
                return ReplanResult.failure("Vehicle has no current node ID", getTriggerType());
            }

            // If new task available, plan to new task start
            if (newTask != null && newTask.getStatus() != TaskStatus.CANCELLED) {
                // Use TaskNodeResolver to map task position to node ID
                String newTaskStartNode = taskNodeResolver.resolveSourceNode(newTask);

                if (newTaskStartNode == null) {
                    return ReplanResult.failure("Cannot resolve node ID for new task: " + newTask.getId(),
                            getTriggerType());
                }

                TransportType transportType = vehicle.getTransportType();
                Path newPath = pathPlanner.planPath(currentNodeId, newTaskStartNode, transportType);

                if (newPath == null || newPath.isEmpty()) {
                    return ReplanResult.failure("No path to new task start node: " + newTaskStartNode,
                            getTriggerType());
                }

                String pathString = pathToString(newPath);
                vehicle.setDestinationNodeId(newTaskStartNode);
                return ReplanResult.success(pathString, getTriggerType());
            }

            // No new task, plan to standby point
            String standbyPoint = coordinator.getStandbyPoint();
            if (standbyPoint != null) {
                TransportType transportType = vehicle.getTransportType();
                Path newPath = pathPlanner.planPath(currentNodeId, standbyPoint, transportType);

                if (newPath == null || newPath.isEmpty()) {
                    return ReplanResult.failure("No path to standby point: " + standbyPoint, getTriggerType());
                }

                String pathString = pathToString(newPath);
                vehicle.setDestinationNodeId(standbyPoint);
                return ReplanResult.success(pathString, getTriggerType());
            }

            // No standby point, just stop at current location
            return ReplanResult.failure("No standby point configured, vehicle stopped at current location",
                    getTriggerType());
        }

        @Override
        public ReplanTrigger getTriggerType() {
            return ReplanTrigger.TASK_CANCELLED;
        }

        private String pathToString(Path path) {
            return String.join("->", path.getNodeIds());
        }
    }

    /**
     * Strategy for traffic conflict replanning.
     * <p>
     * Calculates alternative path to avoid congestion while maintaining
     * original destination.
     */
    private static class TrafficConflictReplanStrategy implements ReplanStrategy {

        private final PathPlanner pathPlanner;

        TrafficConflictReplanStrategy(PathPlanner pathPlanner) {
            this.pathPlanner = pathPlanner;
        }

        @Override
        public ReplanResult execute(Vehicle vehicle, Task currentTask, Task newTask, double currentTime) {
            String currentNodeId = vehicle.getCurrentNodeId();
            String destinationNodeId = vehicle.getDestinationNodeId();

            if (currentNodeId == null) {
                return ReplanResult.failure("Vehicle has no current node ID", getTriggerType());
            }
            if (destinationNodeId == null) {
                return ReplanResult.failure("Vehicle has no destination node ID", getTriggerType());
            }

            // Plan alternative path (same as path blocked, but triggered by traffic manager)
            TransportType transportType = vehicle.getTransportType();
            Path newPath = pathPlanner.planPath(currentNodeId, destinationNodeId, transportType);

            if (newPath == null || newPath.isEmpty()) {
                return ReplanResult.failure("No alternative path available for traffic conflict resolution",
                        getTriggerType());
            }

            String pathString = pathToString(newPath);
            return ReplanResult.success(pathString, getTriggerType());
        }

        @Override
        public ReplanTrigger getTriggerType() {
            return ReplanTrigger.TRAFFIC_CONFLICT;
        }

        private String pathToString(Path path) {
            return String.join("->", path.getNodeIds());
        }
    }
}
