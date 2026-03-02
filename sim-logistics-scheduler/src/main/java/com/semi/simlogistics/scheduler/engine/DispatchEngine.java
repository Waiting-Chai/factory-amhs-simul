package com.semi.simlogistics.scheduler.engine;

import com.semi.simlogistics.scheduler.matching.MatchingContext;
import com.semi.simlogistics.scheduler.matching.MatchingResult;
import com.semi.simlogistics.scheduler.matching.VehicleMatcher;
import com.semi.simlogistics.scheduler.port.DispatchNotificationPort;
import com.semi.simlogistics.scheduler.pool.VehiclePool;
import com.semi.simlogistics.scheduler.replan.ReplanCoordinator;
import com.semi.simlogistics.scheduler.replan.ReplanResult;
import com.semi.simlogistics.scheduler.rule.DispatchContext;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskQueue;
import com.semi.simlogistics.scheduler.task.TaskStatus;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core dispatch engine for task-vehicle assignment (REQ-DS-004).
 * <p>
 * The DispatchEngine is responsible for:
 * <ul>
 *   <li>Executing dispatch cycles at configured intervals</li>
 *   <li>Matching tasks to available vehicles using the configured matcher</li>
 *   <li>Assigning tasks to vehicles (PENDING -> ASSIGNED state transition)</li>
 *   <li>Notifying vehicles of task assignments</li>
 *   <li>Optionally auto-starting tasks (ASSIGNED -> IN_PROGRESS state transition)</li>
 *   <li>Tracking assignment statistics and warnings</li>
 *   <li>Handling dynamic replanning for cancelled/blocked tasks (REQ-DS-006)</li>
 * </ul>
 * <p>
 * Dispatch cycle algorithm:
 * <ol>
 *   <li>Get all pending tasks from queue</li>
 *   <li>For each task, attempt to match with available vehicle</li>
 *   <li>If vehicle found, assign task and notify vehicle</li>
 *   <li>If auto-start enabled, start the task</li>
 *   <li>Track statistics (assigned/unassigned/failed)</li>
 *   <li>Handle cancelled tasks with replanning (if coordinator enabled)</li>
 * </ol>
 * <p>
 * State machine compliance:
 * <ul>
 *   <li>PENDING -> ASSIGNED: When a vehicle is successfully assigned</li>
 *   <li>ASSIGNED -> IN_PROGRESS: When autoStart is enabled</li>
 *   <li>ASSIGNED/IN_PROGRESS -> FAILED: When notification/assignment fails</li>
 *   <li>CANCELLED -> Replan triggered (if coordinator enabled), or warning recorded</li>
 * </ul>
 * <p>
 * Thread-safety: This class is designed for single-threaded use within
 * a simulation environment. Concurrent access is not supported.
 *
 * @author shentw
 * @version 1.3
 * @since 2026-02-09
 */
public class DispatchEngine {

    private final DispatchEngineConfig config;
    private final TaskQueue taskQueue;
    private final List<Vehicle> vehicles;
    private final VehicleMatcher matcher;
    private final DispatchNotificationPort notificationPort;
    private final ReplanCoordinator replanCoordinator;
    private final SimulationTimeProvider simulationTimeProvider;
    private final VehiclePool vehiclePool; // Optional, for REQ-DS-005/007 integration

    /**
     * Default maximum capacity for replan warnings to prevent unbounded growth.
     * <p>
     * When exceeded, oldest warnings are evicted FIFO-style.
     */
    private static final int DEFAULT_REPLAN_WARNING_CAPACITY = 1000;

    /**
     * Thread-safe warning list for replanning entry point observability (Issue #3 fix).
     * <p>
     * Records warnings from handlePathBlocked() and handleTrafficConflict() when
     * exceptions occur during replanning. Maintains FIFO eviction when capacity is reached.
     */
    private final List<String> replanWarnings = new CopyOnWriteArrayList<>();

    /**
     * Maximum capacity for replan warnings (default: 1000).
     */
    private final int replanWarningCapacity;

    /**
     * Create a new dispatch engine without replanning support.
     *
     * @param config            configuration (must not be null)
     * @param taskQueue         task queue to read from (must not be null)
     * @param vehicles          list of available vehicles (must not be null)
     * @param matcher           vehicle matcher to use (must not be null)
     * @param notificationPort  notification port (must not be null)
     * @throws NullPointerException if any required parameter is null
     */
    public DispatchEngine(DispatchEngineConfig config, TaskQueue taskQueue,
                          List<Vehicle> vehicles, VehicleMatcher matcher,
                          DispatchNotificationPort notificationPort) {
        this(config, taskQueue, vehicles, matcher, notificationPort, null, null);
    }

    /**
     * Create a new dispatch engine with replanning support (REQ-DS-006).
     *
     * @param config            configuration (must not be null)
     * @param taskQueue         task queue to read from (must not be null)
     * @param vehicles          list of available vehicles (must not be null)
     * @param matcher           vehicle matcher to use (must not be null)
     * @param notificationPort  notification port (must not be null)
     * @param replanCoordinator replan coordinator for dynamic replanning (null to disable)
     * @throws NullPointerException if any required parameter is null (except replanCoordinator)
     */
    public DispatchEngine(DispatchEngineConfig config, TaskQueue taskQueue,
                          List<Vehicle> vehicles, VehicleMatcher matcher,
                          DispatchNotificationPort notificationPort,
                          ReplanCoordinator replanCoordinator) {
        this(config, taskQueue, vehicles, matcher, notificationPort, replanCoordinator, null);
    }

    /**
     * Create a new dispatch engine with full configuration (REQ-DS-006).
     *
     * @param config               configuration (must not be null)
     * @param taskQueue            task queue to read from (must not be null)
     * @param vehicles             list of available vehicles (must not be null)
     * @param matcher              vehicle matcher to use (must not be null)
     * @param notificationPort     notification port (must not be null)
     * @param replanCoordinator    replan coordinator for dynamic replanning (null to disable)
     * @param simulationTimeProvider simulation time provider (null to use default)
     * @throws NullPointerException if any required parameter is null (except optional ones)
     */
    public DispatchEngine(DispatchEngineConfig config, TaskQueue taskQueue,
                          List<Vehicle> vehicles, VehicleMatcher matcher,
                          DispatchNotificationPort notificationPort,
                          ReplanCoordinator replanCoordinator,
                          SimulationTimeProvider simulationTimeProvider) {
        this(config, taskQueue, vehicles, matcher, notificationPort, replanCoordinator,
                simulationTimeProvider, null);
    }

    /**
     * Create a new dispatch engine with VehiclePool support (REQ-DS-005/007).
     *
     * @param config               configuration (must not be null)
     * @param taskQueue            task queue to read from (must not be null)
     * @param vehicles             list of available vehicles (must not be null)
     * @param matcher              vehicle matcher to use (must not be null)
     * @param notificationPort     notification port (must not be null)
     * @param replanCoordinator    replan coordinator for dynamic replanning (null to disable)
     * @param simulationTimeProvider simulation time provider (null to use default)
     * @param vehiclePool          vehicle pool for manual assignment tracking (null to disable)
     * @throws NullPointerException if any required parameter is null (except optional ones)
     */
    public DispatchEngine(DispatchEngineConfig config, TaskQueue taskQueue,
                          List<Vehicle> vehicles, VehicleMatcher matcher,
                          DispatchNotificationPort notificationPort,
                          ReplanCoordinator replanCoordinator,
                          SimulationTimeProvider simulationTimeProvider,
                          VehiclePool vehiclePool) {
        this.config = Objects.requireNonNull(config, "DispatchEngineConfig cannot be null");
        this.taskQueue = Objects.requireNonNull(taskQueue, "TaskQueue cannot be null");
        // Keep live reference so engine can see vehicles added after construction.
        this.vehicles = Objects.requireNonNull(vehicles, "Vehicles cannot be null");
        this.matcher = Objects.requireNonNull(matcher, "VehicleMatcher cannot be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "DispatchNotificationPort cannot be null");
        this.replanCoordinator = replanCoordinator;
        this.simulationTimeProvider = simulationTimeProvider != null ?
                simulationTimeProvider : SimulationTimeProvider.DEFAULT;
        this.vehiclePool = vehiclePool;
        this.replanWarningCapacity = DEFAULT_REPLAN_WARNING_CAPACITY;
    }

    /**
     * Get the replan coordinator (REQ-DS-006).
     *
     * @return replan coordinator, or null if not configured
     */
    public ReplanCoordinator getReplanCoordinator() {
        return replanCoordinator;
    }

    /**
     * Check if replanning is enabled.
     *
     * @return true if replan coordinator is configured
     */
    public boolean isReplanningEnabled() {
        return replanCoordinator != null;
    }

    /**
     * Get the simulation time provider.
     *
     * @return simulation time provider
     */
    public SimulationTimeProvider getSimulationTimeProvider() {
        return simulationTimeProvider;
    }

    /**
     * Execute a dispatch cycle with explicit simulation time (REQ-DS-005).
     * <p>
     * This variant allows the caller to provide the simulation time for this
     * dispatch cycle, which is useful for deterministic testing and integration
     * with external simulation clocks.
     *
     * @param simulationTime the simulation time in seconds for this cycle
     * @return dispatch result with statistics and warnings
     */
    public DispatchResult dispatch(double simulationTime) {
        return dispatchInternal(simulationTime);
    }

    /**
     * Internal dispatch method that uses the provided simulation time or the default provider.
     *
     * @param overrideSimTimeSeconds override simulation time, or null to use provider
     * @return dispatch result with statistics and warnings
     */
    private DispatchResult dispatchInternal(Double overrideSimTimeSeconds) {
        int assignedCount = 0;
        int unassignedCount = 0;
        int failedCount = 0;
        List<String> warnings = new ArrayList<>();

        // Track vehicles assigned in this cycle to prevent double assignment
        Set<String> assignedVehicleIds = new HashSet<>();

        // Snapshot current pending tasks once per cycle to avoid re-enqueue spin.
        List<Task> cycleTasks = collectCyclePendingTasks();

        // Use override time if provided, otherwise use simulation time provider
        double currentTime = (overrideSimTimeSeconds != null) ?
                overrideSimTimeSeconds : simulationTimeProvider.getCurrentSimulationTimeSeconds();

        DispatchContext dispatchContext = createDispatchContext(currentTime);

        // Process each snapshot task at most once in this cycle.
        for (Task task : cycleTasks) {
            try {
                // REQ-DS-006: Handle cancelled tasks with replanning
                if (task.getStatus() == TaskStatus.CANCELLED || task.getStatus() == TaskStatus.CANCELLING) {
                    handleCancelledTask(task, currentTime, warnings);
                    // Cancelled tasks are not reassigned, count as processed
                    unassignedCount++;
                    continue;
                }

                MatchingContext matchingContext = new MatchingContext(
                        cycleTasks,
                        getAssignableVehicles(assignedVehicleIds),
                        dispatchContext
                );

                // Attempt to match task with vehicle
                Optional<MatchingResult> matchResult = matcher.match(task, matchingContext);

                if (matchResult.isPresent() && matchResult.get().isSuccess()) {
                    MatchingResult result = matchResult.get();
                    String vehicleId = result.getVehicleId().orElseThrow();

                    // Check if vehicle was already assigned in this cycle
                    if (assignedVehicleIds.contains(vehicleId)) {
                        // Vehicle already assigned, put task back in queue
                        taskQueue.enqueue(task);
                        unassignedCount++;
                        continue;
                    }

                    // Find the vehicle
                    Optional<Vehicle> vehicleOpt = findVehicleById(vehicleId);
                    if (vehicleOpt.isEmpty()) {
                        // Vehicle not found, put task back in queue
                        taskQueue.enqueue(task);
                        unassignedCount++;
                        continue;
                    }

                    // Assign task to vehicle
                    if (!task.assignTo(vehicleId)) {
                        // Assignment failed (invalid state transition)
                        taskQueue.enqueue(task);
                        unassignedCount++;
                        continue;
                    }

                    // Send notification
                    try {
                        Vehicle vehicle = vehicleOpt.get();
                        notificationPort.notifyTaskAssigned(task, vehicle);

                        // Mark vehicle as assigned in this cycle
                        assignedVehicleIds.add(vehicleId);

                        // Auto-start if configured
                        if (config.isAutoStartTasks()) {
                            task.start();
                        }

                        assignedCount++;

                    } catch (Exception e) {
                        // Notification failed, mark task as failed
                        task.fail("Notification failed: " + e.getMessage());
                        String warning = String.format("Task %s: %s", task.getId(), e.getMessage());
                        warnings.add(warning);
                        failedCount++;
                    }

                } else {
                    // No vehicle available, put task back in queue
                    taskQueue.enqueue(task);
                    unassignedCount++;
                }

            } catch (Exception e) {
                // Unexpected error during matching
                String warning = String.format("Task %s: Matching error - %s", task.getId(), e.getMessage());
                warnings.add(warning);
                task.fail("Matching error: " + e.getMessage());
                failedCount++;
            }
        }

        return new DispatchResult(assignedCount, unassignedCount, failedCount, warnings);
    }

    /**
     * Get an immutable snapshot of replanning warnings (Issue #3 fix).
     * <p>
     * Returns warnings recorded by handlePathBlocked() and handleTrafficConflict()
     * when exceptions occur during replanning. This provides observability for
     * replanning failures at the entry points.
     *
     * @return unmodifiable list of warning messages
     */
    public List<String> getReplanWarnings() {
        return Collections.unmodifiableList(new ArrayList<>(replanWarnings));
    }

    /**
     * Clear all replanning warnings (Issue #3 fix).
     * <p>
     * Typically called between test scenarios or after collecting warnings
     * for logging/monitoring purposes.
     */
    public void clearReplanWarnings() {
        replanWarnings.clear();
    }

    /**
     * Add a warning with FIFO eviction when capacity exceeded (low-risk improvement).
     * <p>
     * Thread-safe: uses CopyOnWriteArrayList which provides snapshot isolation.
     *
     * @param warning the warning message to add
     */
    private void addReplanWarningWithEviction(String warning) {
        replanWarnings.add(warning);
        // Evict the oldest if capacity exceeded (FIFO)
        while (replanWarnings.size() > replanWarningCapacity) {
            replanWarnings.removeFirst();
        }
    }

    /**
     * Get the maximum capacity for replan warnings.
     * <p>
     * When this capacity is exceeded, oldest warnings are evicted FIFO-style.
     *
     * @return maximum warning capacity
     */
    public int getReplanWarningCapacity() {
        return replanWarningCapacity;
    }

    /**
     * Execute a single dispatch cycle.
     * <p>
     * This method processes all pending tasks in the queue, attempting to
     * assign each to an available vehicle. Tasks that cannot be assigned
     * remain in the queue for the next cycle.
     * <p>
     * Algorithm:
     * <ol>
     *   <li>Collect all PENDING tasks from queue</li>
     *   <li>For each task, attempt to match with available vehicle</li>
     *   <li>If match found, assign task and notify vehicle</li>
     *   <li>If auto-start enabled, transition to IN_PROGRESS</li>
     *   <li>Track statistics (assigned/unassigned/failed)</li>
     *   <li>Handle cancelled tasks with replanning (REQ-DS-006)</li>
     * </ol>
     *
     * @return dispatch result with statistics and warnings
     */
    public DispatchResult dispatch() {
        return dispatchInternal(null);
    }

    /**
     * Handle a cancelled task with replanning (REQ-DS-006).
     * <p>
     * Called during dispatch cycle when a task in CANCELLED or CANCELLING state
     * is encountered. Triggers replanning for the assigned vehicle if replan coordinator
     * is available. If no coordinator, records a warning for observability.
     *
     * @param task the cancelled task
     * @param currentTime current simulation time (in seconds)
     * @param warnings list to append warnings to
     */
    private void handleCancelledTask(Task task, double currentTime, List<String> warnings) {
        if (replanCoordinator == null) {
            // No replan coordinator - record warning for observability (Issue #2 fix)
            warnings.add(String.format("Task %s is cancelled but no replan coordinator configured",
                    task.getId()));
            return;
        }

        // Get the vehicle assigned to this task
        String assignedVehicleId = task.getAssignedVehicleId();
        if (assignedVehicleId == null) {
            // Task was never assigned, record warning
            warnings.add(String.format("Task %s was cancelled but never assigned to a vehicle",
                    task.getId()));
            return;
        }

        // Find the vehicle
        Optional<Vehicle> vehicleOpt = findVehicleById(assignedVehicleId);
        if (vehicleOpt.isEmpty()) {
            warnings.add(String.format("Task %s cancelled but vehicle %s not found for replanning",
                    task.getId(), assignedVehicleId));
            return;
        }

        Vehicle vehicle = vehicleOpt.get();

        // Trigger replanning via coordinator
        try {
            // Use null for newTask - will plan to standby point
            ReplanResult replanResult = replanCoordinator.onTaskCancelled(vehicle, task, null, currentTime);

            if (!replanResult.isSuccess()) {
                warnings.add(String.format("Task %s: Replanning failed for vehicle %s: %s",
                        task.getId(), assignedVehicleId,
                        replanResult.getErrorMessage().orElse("Unknown error")));
            }

        } catch (Exception e) {
            // Catch any replanning exceptions to prevent dispatch loop from hanging
            warnings.add(String.format("Task %s: Replanning error for vehicle %s: %s",
                    task.getId(), assignedVehicleId, e.getMessage()));
        }
    }

    /**
     * Handle path blocking event (REQ-DS-006).
     * <p>
     * Public entry point for external components (e.g., TrafficManager) to trigger
     * replanning when a vehicle is blocked at a control point.
     *
     * @param vehicleId ID of the blocked vehicle
     * @param currentTask current task (may be null)
     * @return true if replanning was triggered successfully, false otherwise
     */
    public boolean handlePathBlocked(String vehicleId, Task currentTask) {
        if (replanCoordinator == null) {
            return false;
        }

        Optional<Vehicle> vehicleOpt = findVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }

        Vehicle vehicle = vehicleOpt.get();
        // REQ-DS-006: Use simulation time provider (Issue #1 fix)
        double currentTime = simulationTimeProvider.getCurrentSimulationTimeSeconds();

        try {
            ReplanResult result = replanCoordinator.onPathBlocked(vehicle, currentTask, currentTime);
            return result.isSuccess();
        } catch (Exception e) {
            // Issue #3 fix: Record warning for observability
            String taskId = currentTask != null ? currentTask.getId() : "null";
            String warning = String.format("PATH_BLOCKED: Vehicle %s, task %s - %s",
                    vehicleId, taskId, e.getMessage());
            addReplanWarningWithEviction(warning);
            return false;
        }
    }

    /**
     * Handle traffic conflict event (REQ-DS-006).
     * <p>
     * Public entry point for TrafficManager to trigger replanning when
     * a potential deadlock is detected.
     *
     * @param vehicleId ID of the vehicle to replan
     * @param currentTask current task
     * @return true if replanning was triggered successfully, false otherwise
     */
    public boolean handleTrafficConflict(String vehicleId, Task currentTask) {
        if (replanCoordinator == null) {
            return false;
        }

        Optional<Vehicle> vehicleOpt = findVehicleById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }

        Vehicle vehicle = vehicleOpt.get();
        // REQ-DS-006: Use simulation time provider (Issue #1 fix)
        double currentTime = simulationTimeProvider.getCurrentSimulationTimeSeconds();

        try {
            ReplanResult result = replanCoordinator.onTrafficConflict(vehicle, currentTask, currentTime);
            return result.isSuccess();
        } catch (Exception e) {
            // Issue #3 fix: Record warning for observability
            String taskId = currentTask != null ? currentTask.getId() : "null";
            String warning = String.format("TRAFFIC_CONFLICT: Vehicle %s, task %s - %s",
                    vehicleId, taskId, e.getMessage());
            addReplanWarningWithEviction(warning);
            return false;
        }
    }

    /**
     * Get the dispatch engine configuration.
     *
     * @return configuration
     */
    public DispatchEngineConfig getConfig() {
        return config;
    }

    private List<Task> collectCyclePendingTasks() {
        int initialSize = taskQueue.size();
        List<Task> pendingTasks = new ArrayList<>(initialSize);
        List<Task> nonPendingTasks = new ArrayList<>();

        for (int i = 0; i < initialSize; i++) {
            Task task = taskQueue.pollNext();
            if (task == null) {
                break;
            }
            if (task.getStatus() == TaskStatus.PENDING) {
                pendingTasks.add(task);
                continue;
            }
            // REQ-DS-006: Include cancelled tasks for replanning
            if (task.getStatus() == TaskStatus.CANCELLED || task.getStatus() == TaskStatus.CANCELLING) {
                pendingTasks.add(task);
                continue;
            }
            nonPendingTasks.add(task);
        }

        for (Task task : nonPendingTasks) {
            taskQueue.enqueue(task);
        }
        return pendingTasks;
    }

    /**
     * Find a vehicle by ID.
     *
     * @param vehicleId vehicle ID to find
     * @return optional vehicle, empty if not found
     */
    private Optional<Vehicle> findVehicleById(String vehicleId) {
        return vehicles.stream()
                .filter(v -> v.id().equals(vehicleId))
                .findFirst();
    }

    private List<Vehicle> getAssignableVehicles(Set<String> assignedVehicleIds) {
        // Use VehiclePool if available (REQ-DS-005/007), otherwise fall back to direct list
        if (vehiclePool != null) {
            // VehiclePool already filters out manually assigned vehicles
            return vehiclePool.getAvailableVehicles().stream()
                    .filter(vehicle -> !assignedVehicleIds.contains(vehicle.id()))
                    .toList();
        }
        return vehicles.stream()
                .filter(vehicle -> !assignedVehicleIds.contains(vehicle.id()))
                .toList();
    }

    /**
     * Create a dispatch context for rule calculations.
     * <p>
     * This implementation uses Euclidean distance and zero utilization.
     * A real implementation would integrate with path planner and metrics collector.
     *
     * @param currentTimeInSeconds the current simulation time in seconds for this dispatch cycle
     * @return dispatch context
     */
    private DispatchContext createDispatchContext(final double currentTimeInSeconds) {
        return new DispatchContext() {
            @Override
            public double getDistance(com.semi.simlogistics.core.Position from,
                                     com.semi.simlogistics.core.Position to) {
                double dx = from.x() - to.x();
                double dy = from.y() - to.y();
                double dz = from.z() - to.z();
                return Math.sqrt(dx * dx + dy * dy + dz * dz);
            }

            @Override
            public double getUtilization(String vehicleId) {
                // TODO: Integrate with metrics collector
                return 0.0;
            }

            @Override
            public long getCurrentTime() {
                // Use the provided simulation time for this dispatch cycle
                // Conversion rule: truncate (round toward zero) to milliseconds
                return (long) (currentTimeInSeconds * 1000.0);
            }
        };
    }
}
