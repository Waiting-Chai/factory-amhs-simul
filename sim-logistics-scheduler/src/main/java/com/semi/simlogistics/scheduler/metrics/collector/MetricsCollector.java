package com.semi.simlogistics.scheduler.metrics.collector;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.model.MetricData;
import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;
import com.semi.simlogistics.scheduler.metrics.port.ConfigPort;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Metrics collector for sampling and aggregating simulation metrics (REQ-KPI-001).
 * <p>
 * Features:
 * <ul>
 *   <li>Event-driven real-time sampling (关键事件触发)</li>
 *   <li>Periodic aggregation (每60仿真秒聚合)</li>
 *   <li>Dual time基准 (simulatedTime + wallClockTime)</li>
 * </ul>
 * <p>
 * Sampling strategy:
 * <ul>
 *   <li>Key events (task completion, conflict, alarm): immediate sampling</li>
 *   <li>Periodic aggregation: every 60 simulated seconds</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class MetricsCollector {

    private final String simulationId;
    private final MetricsRepositoryPort repository;
    private final ConfigPort config;

    // Accumulators for current aggregation period
    private final AtomicInteger tasksCompletedInPeriod = new AtomicInteger(0);
    private final AtomicInteger tasksFailedInPeriod = new AtomicInteger(0);
    private final DoubleAdder totalDistanceInPeriod = new DoubleAdder();  // meters
    private final DoubleAdder totalEnergyInPeriod = new DoubleAdder();    // watt-seconds
    private final AtomicInteger wipTotal = new AtomicInteger(0);

    // State tracking for utilization calculation
    private final Map<String, EntityStateTracker> entityStates = new ConcurrentHashMap<>();

    // Time tracking
    private double lastAggregationTime = 0.0;
    private double aggregationInterval;

    /**
     * Create a new metrics collector.
     *
     * @param simulationId the simulation ID
     * @param repository    the metrics repository
     * @param config        the configuration port
     */
    public MetricsCollector(
            String simulationId,
            MetricsRepositoryPort repository,
            ConfigPort config
    ) {
        this.simulationId = simulationId;
        this.repository = repository;
        this.config = config;
        this.aggregationInterval = config.getDouble(
                ConfigPort.Keys.METRICS_AGGREGATION_INTERVAL,
                ConfigPort.Defaults.AGGREGATION_INTERVAL
        );
    }

    /**
     * Record a task completion event.
     *
     * @param taskId         the completed task ID
     * @param taskType       the task type
     * @param assignedVehicle the assigned vehicle ID
     * @param simulatedTime  the simulated time in seconds
     */
    public void recordTaskCompletion(
            String taskId,
            String taskType,
            String assignedVehicle,
            double simulatedTime
    ) {
        // Forward to overloaded method with default completionTime of 0
        recordTaskCompletion(taskId, taskType, assignedVehicle, simulatedTime, 0.0);
    }

    /**
     * Record a task completion event with completion time.
     *
     * @param taskId           the completed task ID
     * @param taskType         the task type
     * @param assignedVehicle  the assigned vehicle ID
     * @param simulatedTime    the simulated time in seconds
     * @param completionTime   the task completion time in seconds
     */
    public void recordTaskCompletion(
            String taskId,
            String taskType,
            String assignedVehicle,
            double simulatedTime,
            double completionTime
    ) {
        if (!isSamplingEnabled()) {
            return;
        }

        tasksCompletedInPeriod.incrementAndGet();

        MetricEvent event = new MetricEvent.Builder(
                        simulationId,
                        MetricEvent.EventTypes.TASK_COMPLETED,
                        simulatedTime)
                .entityId(taskId)
                .entityType(taskType)
                .data(new MetricData()
                        .putString("assignedVehicle", assignedVehicle != null ? assignedVehicle : "")
                        .putString("taskType", taskType)
                        .putDouble("completionTime", completionTime)
                )
                .build();

        repository.saveEvent(event);
    }

    /**
     * Record a task failure event.
     *
     * @param taskId        the failed task ID
     * @param taskType      the task type
     * @param failureReason the failure reason
     * @param simulatedTime the simulated time in seconds
     */
    public void recordTaskFailure(
            String taskId,
            String taskType,
            String failureReason,
            double simulatedTime
    ) {
        if (!isSamplingEnabled()) {
            return;
        }

        tasksFailedInPeriod.incrementAndGet();

        MetricEvent event = new MetricEvent.Builder(
                        simulationId,
                        MetricEvent.EventTypes.TASK_FAILED,
                        simulatedTime)
                .entityId(taskId)
                .entityType(taskType)
                .data(new MetricData()
                        .putString("failureReason", failureReason)
                        .putString("taskType", taskType)
                )
                .build();

        repository.saveEvent(event);
    }

    /**
     * Record a vehicle state change event.
     *
     * @param vehicleId     the vehicle ID
     * @param oldState      the old state
     * @param newState      the new state
     * @param simulatedTime the simulated time in seconds
     */
    public void recordVehicleStateChange(
            String vehicleId,
            String oldState,
            String newState,
            double simulatedTime
    ) {
        if (!isSamplingEnabled()) {
            return;
        }

        // Update state tracker for utilization calculation
        EntityStateTracker tracker = entityStates.computeIfAbsent(
                vehicleId,
                k -> new EntityStateTracker(vehicleId, simulatedTime)
        );
        tracker.recordStateChange(newState, simulatedTime);

        MetricEvent event = new MetricEvent.Builder(
                        simulationId,
                        MetricEvent.EventTypes.VEHICLE_STATE_CHANGED,
                        simulatedTime)
                .entityId(vehicleId)
                .entityType("VEHICLE")
                .data(new MetricData()
                        .putString("oldState", oldState)
                        .putString("newState", newState)
                )
                .build();

        repository.saveEvent(event);
    }

    /**
     * Record a vehicle movement event.
     *
     * @param vehicleId     the vehicle ID
     * @param distance      the distance moved in meters
     * @param simulatedTime the simulated time in seconds
     */
    public void recordVehicleMovement(
            String vehicleId,
            double distance,
            double simulatedTime
    ) {
        if (!isSamplingEnabled()) {
            return;
        }

        totalDistanceInPeriod.add(distance);

        MetricEvent event = new MetricEvent.Builder(
                        simulationId,
                        MetricEvent.EventTypes.VEHICLE_MOVED,
                        simulatedTime)
                .entityId(vehicleId)
                .entityType("VEHICLE")
                .data(new MetricData()
                        .putDouble("distance", distance)
                )
                .build();

        repository.saveEvent(event);
    }

    /**
     * Record an energy consumption event.
     *
     * @param entityId      the consuming entity ID
     * @param entityType    the entity type (VEHICLE, EQUIPMENT)
     * @param energyWattSeconds the energy consumed in watt-seconds
     * @param simulatedTime the simulated time in seconds
     */
    public void recordEnergyConsumption(
            String entityId,
            String entityType,
            double energyWattSeconds,
            double simulatedTime
    ) {
        if (!isSamplingEnabled()) {
            return;
        }

        totalEnergyInPeriod.add(energyWattSeconds);

        MetricEvent event = new MetricEvent.Builder(
                        simulationId,
                        MetricEvent.EventTypes.ENERGY_CONSUMED,
                        simulatedTime)
                .entityId(entityId)
                .entityType(entityType)
                .data(new MetricData()
                        .putDouble("energyWattSeconds", energyWattSeconds)
                )
                .build();

        repository.saveEvent(event);
    }

    /**
     * Update the current WIP count.
     *
     * @param wipCount the current WIP count
     */
    public void updateWIP(int wipCount) {
        wipTotal.set(wipCount);
    }

    /**
     * Trigger aggregation at the specified simulated time.
     * <p>
     * This method should be called periodically (e.g., every 1 simulated second)
     * to check if aggregation is due.
     *
     * @param simulatedTime the current simulated time in seconds
     * @return the aggregate if aggregation was triggered, null otherwise
     */
    public MetricAggregate triggerAggregation(double simulatedTime) {
        // Check if aggregation interval has passed
        if (simulatedTime - lastAggregationTime < aggregationInterval) {
            return null;
        }

        // Create aggregate
        MetricAggregate aggregate = createAggregate(simulatedTime);

        // Reset accumulators
        tasksCompletedInPeriod.set(0);
        tasksFailedInPeriod.set(0);
        totalDistanceInPeriod.reset();
        totalEnergyInPeriod.reset();
        lastAggregationTime = simulatedTime;

        // Save aggregate
        repository.saveAggregate(aggregate);

        return aggregate;
    }

    /**
     * Force aggregation regardless of interval.
     *
     * @param simulatedTime the current simulated time in seconds
     * @return the created aggregate
     */
    public MetricAggregate forceAggregation(double simulatedTime) {
        lastAggregationTime = simulatedTime - aggregationInterval; // Ensure aggregation runs
        return triggerAggregation(simulatedTime);
    }

    /**
     * Calculate time-weighted average utilization for all tracked entities.
     *
     * @param currentTime the current simulated time
     * @return the average utilization (0.0 to 1.0)
     */
    public double calculateAverageUtilization(double currentTime) {
        if (entityStates.isEmpty()) {
            return 0.0;
        }

        double totalUtilization = 0.0;
        int count = 0;

        for (EntityStateTracker tracker : entityStates.values()) {
            double utilization = tracker.calculateUtilization(currentTime);
            totalUtilization += utilization;
            count++;
        }

        return count > 0 ? totalUtilization / count : 0.0;
    }

    /**
     * Get the number of tasks completed in the current period.
     *
     * @return the task count
     */
    public int getTasksCompletedInPeriod() {
        return tasksCompletedInPeriod.get();
    }

    /**
     * Get the total energy consumed in the current period.
     *
     * @return the energy in watt-seconds
     */
    public double getTotalEnergyInPeriod() {
        return totalEnergyInPeriod.sum();
    }

    /**
     * Get the current WIP count.
     *
     * @return the WIP count
     */
    public int getWipTotal() {
        return wipTotal.get();
    }

    /**
     * Reset the collector state.
     */
    public void reset() {
        tasksCompletedInPeriod.set(0);
        tasksFailedInPeriod.set(0);
        totalDistanceInPeriod.reset();
        totalEnergyInPeriod.reset();
        wipTotal.set(0);
        entityStates.clear();
        lastAggregationTime = 0.0;
    }

    /**
     * Create an aggregate from current accumulators.
     */
    private MetricAggregate createAggregate(double simulatedTime) {
        double elapsedTime = simulatedTime - lastAggregationTime;
        if (elapsedTime <= 0) {
            elapsedTime = aggregationInterval;
        }

        int tasksCompleted = tasksCompletedInPeriod.get();
        double tasksPerHour = elapsedTime > 0
                ? (tasksCompleted / elapsedTime) * 3600.0
                : 0.0;

        // Material throughput: assuming 1 unit per task (simplified)
        double materialThroughput = tasksPerHour;

        // Calculate utilization
        double vehicleUtilization = calculateAverageUtilization(simulatedTime);
        double equipmentUtilization = vehicleUtilization; // Simplified, use same value

        return new MetricAggregate.Builder(simulationId, simulatedTime)
                .wallClockTime(LocalDateTime.now())
                .tasksCompleted(tasksCompleted)
                .tasksPerHour(tasksPerHour)
                .materialThroughput(materialThroughput)
                .vehicleUtilization(vehicleUtilization)
                .equipmentUtilization(equipmentUtilization)
                .wipTotal(wipTotal.get())
                .energyTotal(totalEnergyInPeriod.sum())
                .customMetrics(new MetricData()
                        .putInt("tasksFailed", tasksFailedInPeriod.get())
                        .putDouble("totalDistance", totalDistanceInPeriod.sum())
                )
                .build();
    }

    /**
     * Check if sampling is enabled.
     */
    private boolean isSamplingEnabled() {
        return config.getBoolean(
                ConfigPort.Keys.METRICS_SAMPLING_ENABLED,
                ConfigPort.Defaults.SAMPLING_ENABLED
        );
    }

    /**
     * Entity state tracker for utilization calculation.
     * Uses time-weighted average method.
     */
    private static class EntityStateTracker {

        private final String entityId;
        private final double trackingStartTime;  // When tracking started
        private String currentState;
        private double stateStartTime;
        private double totalWorkingTime = 0.0;

        // Working states (when vehicle is considered "utilized")
        private static final String[] WORKING_STATES = {
                "MOVING", "LOADING", "UNLOADING", "PROCESSING"
        };

        EntityStateTracker(String entityId, double startTime) {
            this.entityId = entityId;
            this.trackingStartTime = startTime;
            this.currentState = "IDLE";
            this.stateStartTime = startTime;
        }

        void recordStateChange(String newState, double simulatedTime) {
            // Update working time if previous state was working
            if (isWorkingState(currentState)) {
                totalWorkingTime += (simulatedTime - stateStartTime);
            }

            currentState = newState;
            stateStartTime = simulatedTime;
        }

        double calculateUtilization(double currentTime) {
            // Add current state working time
            double workingTime = totalWorkingTime;
            if (isWorkingState(currentState)) {
                workingTime += (currentTime - stateStartTime);
            }

            // Total tracking time from start to now
            double totalTime = currentTime - trackingStartTime;

            return totalTime > 0 ? workingTime / totalTime : 0.0;
        }

        private boolean isWorkingState(String state) {
            for (String workingState : WORKING_STATES) {
                if (workingState.equals(state)) {
                    return true;
                }
            }
            return false;
        }
    }
}
