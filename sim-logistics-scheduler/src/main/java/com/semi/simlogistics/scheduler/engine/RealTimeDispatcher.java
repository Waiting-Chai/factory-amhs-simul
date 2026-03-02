package com.semi.simlogistics.scheduler.engine;

import com.semi.simlogistics.scheduler.matching.VehicleMatcher;
import com.semi.simlogistics.scheduler.port.DispatchNotificationPort;
import com.semi.simlogistics.scheduler.pool.VehiclePool;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskQueue;
import com.semi.simlogistics.scheduler.task.TaskStatus;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real-time dispatcher for runtime task management (REQ-DS-005).
 * <p>
 * Provides capabilities for:
 * <ul>
 *   <li>Adding tasks at runtime (immediate queue entry)</li>
 *   <li>Handling urgent tasks (highest priority)</li>
 *   <li>Manual task-vehicle assignment with locks</li>
 *   <li>Dispatch cycle integration with DispatchEngine</li>
 * </ul>
 * <p>
 * Urgent task behavior (REQ-DS-005):
 * <ul>
 *   <li>Urgent tasks are marked as URGENT priority (higher than NORMAL)</li>
 *   <li>Preemption is controlled by configuration (default: disabled)</li>
 *   <li>When enabled, urgent tasks may preempt lower-priority in-progress tasks</li>
 * </ul>
 * <p>
 * Manual assignment behavior (REQ-DS-005):
 * <ul>
 *   <li>Manually assigned vehicles are marked in the vehicle pool</li>
 *   <li>Manual assignments are never overridden by automatic dispatch</li>
 *   <li>Tasks can be locked to prevent automatic dispatch</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class RealTimeDispatcher {

    private static final Logger LOGGER = Logger.getLogger(RealTimeDispatcher.class.getName());

    private final TaskQueue taskQueue;
    private final DispatchEngine dispatchEngine;
    private final VehiclePool vehiclePool;
    private final DispatchNotificationPort notificationPort;
    private final boolean preemptionEnabled;
    private final SimulationTimeProvider simulationTimeProvider;

    /**
     * Create a new real-time dispatcher with full configuration.
     *
     * @param taskQueue         task queue for adding tasks (must not be null)
     * @param vehicles          list of vehicles (must not be null)
     * @param matcher           vehicle matcher (must not be null)
     * @param vehiclePool       vehicle pool for managing availability (must not be null)
     * @param notificationPort  notification port for task assignments (must not be null)
     * @param preemptionEnabled whether urgent tasks can preempt in-progress tasks
     * @throws NullPointerException if any required parameter is null
     */
    public RealTimeDispatcher(TaskQueue taskQueue,
                              List<Vehicle> vehicles,
                              VehicleMatcher matcher,
                              VehiclePool vehiclePool,
                              DispatchNotificationPort notificationPort,
                              boolean preemptionEnabled) {
        this(taskQueue, vehicles, matcher, vehiclePool, notificationPort,
                new DispatchEngineConfig(1000L, 3, true), preemptionEnabled, SimulationTimeProvider.DEFAULT);
    }

    /**
     * Create a new real-time dispatcher with preemption disabled.
     *
     * @param taskQueue        task queue for adding tasks (must not be null)
     * @param vehicles         list of vehicles (must not be null)
     * @param matcher          vehicle matcher (must not be null)
     * @param vehiclePool      vehicle pool for managing availability (must not be null)
     * @param notificationPort notification port for task assignments (must not be null)
     * @throws NullPointerException if any required parameter is null
     */
    public RealTimeDispatcher(TaskQueue taskQueue,
                              List<Vehicle> vehicles,
                              VehicleMatcher matcher,
                              VehiclePool vehiclePool,
                              DispatchNotificationPort notificationPort) {
        this(taskQueue, vehicles, matcher, vehiclePool, notificationPort,
                new DispatchEngineConfig(1000L, 3, true), false, SimulationTimeProvider.DEFAULT);
    }

    /**
     * Create a new real-time dispatcher with custom config.
     *
     * @param taskQueue    task queue for adding tasks (must not be null)
     * @param vehicles     list of vehicles (must not be null)
     * @param matcher      vehicle matcher (must not be null)
     * @param vehiclePool  vehicle pool for managing availability (must not be null)
     * @param notificationPort notification port for task assignments (must not be null)
     * @param config       dispatch engine configuration (must not be null)
     * @param preemptionEnabled whether urgent tasks can preempt in-progress tasks
     * @throws NullPointerException if any required parameter is null
     */
    public RealTimeDispatcher(TaskQueue taskQueue,
                              List<Vehicle> vehicles,
                              VehicleMatcher matcher,
                              VehiclePool vehiclePool,
                              DispatchNotificationPort notificationPort,
                              DispatchEngineConfig config,
                              boolean preemptionEnabled) {
        this(taskQueue, vehicles, matcher, vehiclePool, notificationPort,
                config, preemptionEnabled, SimulationTimeProvider.DEFAULT);
    }

    /**
     * Create a new real-time dispatcher with full configuration including simulation time provider.
     *
     * @param taskQueue            task queue for adding tasks (must not be null)
     * @param vehicles             list of vehicles (must not be null)
     * @param matcher              vehicle matcher (must not be null)
     * @param vehiclePool          vehicle pool for managing availability (must not be null)
     * @param notificationPort     notification port for task assignments (must not be null)
     * @param config               dispatch engine configuration (must not be null)
     * @param preemptionEnabled    whether urgent tasks can preempt in-progress tasks
     * @param simulationTimeProvider provider for simulation time (null to use default)
     * @throws NullPointerException if any required parameter is null (except simulationTimeProvider)
     */
    public RealTimeDispatcher(TaskQueue taskQueue,
                              List<Vehicle> vehicles,
                              VehicleMatcher matcher,
                              VehiclePool vehiclePool,
                              DispatchNotificationPort notificationPort,
                              DispatchEngineConfig config,
                              boolean preemptionEnabled,
                              SimulationTimeProvider simulationTimeProvider) {
        this.taskQueue = Objects.requireNonNull(taskQueue, "TaskQueue cannot be null");
        this.vehiclePool = Objects.requireNonNull(vehiclePool, "VehiclePool cannot be null");
        this.notificationPort = Objects.requireNonNull(notificationPort, "DispatchNotificationPort cannot be null");
        this.preemptionEnabled = preemptionEnabled;
        this.simulationTimeProvider = simulationTimeProvider != null ?
                simulationTimeProvider : SimulationTimeProvider.DEFAULT;

        // Create DispatchEngine with VehiclePool support
        this.dispatchEngine = new DispatchEngine(
                config, taskQueue, vehicles, matcher, notificationPort,
                null, this.simulationTimeProvider, vehiclePool);
    }

    /**
     * Add a task at runtime (REQ-DS-005).
     * <p>
     * The task is immediately enqueued and will be processed in the next
     * dispatch cycle. Urgent tasks are enqueued with URGENT priority.
     *
     * @param task   task to add (must not be null)
     * @param urgent whether this is an urgent task
     * @throws NullPointerException if task is null
     */
    public void addTask(Task task, boolean urgent) {
        Objects.requireNonNull(task, "Task cannot be null");

        // Check if task is locked (prevents override of manual assignments)
        if (vehiclePool.isTaskLocked(task.getId())) {
            LOGGER.warning("Task " + task.getId() + " is locked, skipping automatic dispatch");
            return;
        }

        // Enqueue for immediate processing in next cycle
        // Urgent tasks use enqueueUrgent to ensure priority ordering
        if (urgent) {
            taskQueue.enqueueUrgent(task);
        } else {
            taskQueue.enqueue(task);
        }

        LOGGER.log(Level.INFO, "Task {0} added at runtime (urgent={1})",
                new Object[]{task.getId(), urgent});
    }

    /**
     * Manually assign a task to a specific vehicle (REQ-DS-005).
     * <p>
     * This creates a manual lock that prevents the dispatch engine from
     * overriding the assignment. The vehicle is marked as manually assigned
     * and the task is locked.
     *
     * @param taskId   ID of the task to assign (must not be null)
     * @param vehicleId ID of the vehicle to assign (must not be null)
     * @return true if assignment succeeded, false otherwise
     * @throws NullPointerException if taskId or vehicleId is null
     */
    public boolean manualAssign(String taskId, String vehicleId) {
        Objects.requireNonNull(taskId, "Task ID cannot be null");
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");

        // Find the task in queue
        Task task = findTaskInQueue(taskId);
        if (task == null) {
            LOGGER.warning("Task " + taskId + " not found in queue");
            return false;
        }

        // Check if task is in correct state for assignment
        if (task.getStatus() != TaskStatus.PENDING) {
            LOGGER.warning("Task " + taskId + " is not in PENDING state, current: " + task.getStatus());
            return false;
        }

        // Find the vehicle
        Vehicle vehicle = vehiclePool.getVehicleById(vehicleId);
        if (vehicle == null) {
            LOGGER.warning("Vehicle " + vehicleId + " not found");
            return false;
        }

        // Assign task to vehicle
        if (!task.assignTo(vehicleId)) {
            LOGGER.warning("Failed to assign task " + taskId + " to vehicle " + vehicleId);
            return false;
        }

        // Mark vehicle as manually assigned (prevents automatic dispatch override)
        vehiclePool.markManuallyAssigned(vehicleId);

        // Lock the task (prevents automatic dispatch)
        vehiclePool.lockTask(taskId);

        // Send notification
        try {
            notificationPort.notifyTaskAssigned(task, vehicle);

            LOGGER.log(Level.INFO, "Manual assignment: task {0} -> vehicle {1}",
                    new Object[]{taskId, vehicleId});
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Notification failed for manual assignment " + taskId + ": " + e.getMessage());
            task.fail("Notification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute a dispatch cycle (REQ-DS-005).
     * <p>
     * Delegates to the DispatchEngine to process all pending tasks.
     * This ensures that runtime-added tasks are processed in the
     * next cycle.
     *
     * @return dispatch result with statistics and warnings
     */
    public DispatchResult onDispatchCycle() {
        // Delegate to dispatch engine
        DispatchResult result = dispatchEngine.dispatch();

        LOGGER.log(Level.FINE, "Dispatch cycle completed: assigned={0}, unassigned={1}, failed={2}",
                new Object[]{result.getAssignedCount(), result.getUnassignedCount(), result.getFailedCount()});

        return result;
    }

    /**
     * Execute a dispatch cycle with explicit simulation time.
     * <p>
     * This variant is used when the simulation time needs to be
     * explicitly provided (e.g., in test scenarios).
     * The provided time is logged and used for observability.
     *
     * @param currentSimTime current simulation time in seconds
     * @return dispatch result with statistics and warnings
     */
    public DispatchResult onDispatchCycle(double currentSimTime) {
        LOGGER.log(Level.FINE, "Dispatch cycle with sim time={0} seconds", currentSimTime);

        // Delegate to dispatch engine with the provided simulation time
        DispatchResult result = dispatchEngine.dispatch(currentSimTime);

        LOGGER.log(Level.FINE, "Dispatch cycle completed: assigned={0}, unassigned={1}, failed={2}",
                new Object[]{result.getAssignedCount(), result.getUnassignedCount(), result.getFailedCount()});

        return result;
    }

    /**
     * Check if preemption is enabled for urgent tasks.
     *
     * @return true if urgent tasks can preempt in-progress tasks
     */
    public boolean isPreemptionEnabled() {
        return preemptionEnabled;
    }

    /**
     * Get the task queue.
     *
     * @return task queue
     */
    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    /**
     * Get the vehicle pool.
     *
     * @return vehicle pool
     */
    public VehiclePool getVehiclePool() {
        return vehiclePool;
    }

    /**
     * Get the dispatch engine.
     *
     * @return dispatch engine
     */
    public DispatchEngine getDispatchEngine() {
        return dispatchEngine;
    }

    /**
     * Find a task by ID in the queue.
     * <p>
     * This is a utility method for manual assignment (REQ-DS-005).
     *
     * @param taskId task ID to find
     * @return task if found, null otherwise
     */
    private Task findTaskInQueue(String taskId) {
        return taskQueue.getById(taskId);
    }

    /**
     * Directly assign a task reference to a vehicle (internal method).
     * <p>
     * This method is used when the caller has a direct reference to the task.
     *
     * @param task     task to assign (must not be null)
     * @param vehicleId ID of the vehicle to assign (must not be null)
     * @return true if assignment succeeded, false otherwise
     */
    public boolean manualAssign(Task task, String vehicleId) {
        Objects.requireNonNull(task, "Task cannot be null");
        Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");

        // Check if task is in correct state for assignment
        if (task.getStatus() != TaskStatus.PENDING) {
            LOGGER.warning("Task " + task.getId() + " is not in PENDING state, current: " + task.getStatus());
            return false;
        }

        // Find the vehicle
        Vehicle vehicle = vehiclePool.getVehicleById(vehicleId);
        if (vehicle == null) {
            LOGGER.warning("Vehicle " + vehicleId + " not found");
            return false;
        }

        // Assign task to vehicle
        if (!task.assignTo(vehicleId)) {
            LOGGER.warning("Failed to assign task " + task.getId() + " to vehicle " + vehicleId);
            return false;
        }

        // Mark vehicle as manually assigned (prevents automatic dispatch override)
        vehiclePool.markManuallyAssigned(vehicleId);

        // Lock the task (prevents automatic dispatch)
        vehiclePool.lockTask(task.getId());

        // Send notification
        try {
            notificationPort.notifyTaskAssigned(task, vehicle);

            LOGGER.log(Level.INFO, "Manual assignment: task {0} -> vehicle {1}",
                    new Object[]{task.getId(), vehicleId});
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Notification failed for manual assignment " + task.getId() + ": " + e.getMessage());
            task.fail("Notification failed: " + e.getMessage());
            return false;
        }
    }
}
