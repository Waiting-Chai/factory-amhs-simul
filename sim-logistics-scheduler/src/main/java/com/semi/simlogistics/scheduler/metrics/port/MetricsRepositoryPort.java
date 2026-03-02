package com.semi.simlogistics.scheduler.metrics.port;

import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;
import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for metrics repository operations.
 * <p>
 * This port defines the contract for storing and retrieving metrics data.
 * Implementations can be in-memory, MySQL, Redis, or other storage mechanisms.
 * <p>
 * All operations use dual time基准:
 * <ul>
 *   <li>simulatedTime: Simulation time (seconds) for simulation analysis</li>
 *   <li>wallClockTime: Wall clock time for real-time monitoring</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public interface MetricsRepositoryPort {

    /**
     * Save a metric event (Real-time sampling).
     *
     * @param event the metric event to save
     */
    void saveEvent(MetricEvent event);

    /**
     * Save a metric aggregate (Regular aggregation).
     *
     * @param aggregate the metric aggregate to save
     */
    void saveAggregate(MetricAggregate aggregate);

    /**
     * Save multiple metric events in batch.
     *
     * @param events the metric events to save
     */
    void saveEventsBatch(List<MetricEvent> events);

    /**
     * Query metrics by simulation ID and time range.
     *
     * @param simulationId the simulation ID
     * @param fromTime     the start time (simulated time in seconds)
     * @param toTime       the end time (simulated time in seconds)
     * @return list of metric aggregates in the time range
     */
    List<MetricAggregate> queryBySimulationIdAndTimeRange(
            String simulationId,
            double fromTime,
            double toTime
    );

    /**
     * Get the latest aggregate for a simulation.
     *
     * @param simulationId the simulation ID
     * @return the latest metric aggregate, or empty if not found
     */
    Optional<MetricAggregate> getLatestAggregate(String simulationId);

    /**
     * Query metrics by entity ID.
     *
     * @param entityId the entity ID
     * @return list of metric events for the entity
     */
    List<MetricEvent> queryByEntityId(String entityId);

    /**
     * Query metrics by event type.
     *
     * @param eventType the event type
     * @return list of metric events of the specified type
     */
    List<MetricEvent> queryByEventType(String eventType);

    /**
     * Delete all metrics for a simulation.
     *
     * @param simulationId the simulation ID
     */
    void deleteBySimulationId(String simulationId);

    /**
     * Get aggregated metrics summary for a simulation.
     *
     * @param simulationId the simulation ID
     * @return the metrics summary, or empty if not found
     */
    Optional<MetricsSummary> getSummary(String simulationId);

    /**
     * Metrics summary data class.
     */
    class MetricsSummary {
        private final String simulationId;
        private final int totalTasksCompleted;
        private final double tasksPerHour;
        private final double materialThroughput;
        private final double vehicleUtilization;
        private final double equipmentUtilization;
        private final int wipTotal;
        private final double energyTotal;

        public MetricsSummary(
                String simulationId,
                int totalTasksCompleted,
                double tasksPerHour,
                double materialThroughput,
                double vehicleUtilization,
                double equipmentUtilization,
                int wipTotal,
                double energyTotal
        ) {
            this.simulationId = simulationId;
            this.totalTasksCompleted = totalTasksCompleted;
            this.tasksPerHour = tasksPerHour;
            this.materialThroughput = materialThroughput;
            this.vehicleUtilization = vehicleUtilization;
            this.equipmentUtilization = equipmentUtilization;
            this.wipTotal = wipTotal;
            this.energyTotal = energyTotal;
        }

        public String getSimulationId() {
            return simulationId;
        }

        public int getTotalTasksCompleted() {
            return totalTasksCompleted;
        }

        public double getTasksPerHour() {
            return tasksPerHour;
        }

        public double getMaterialThroughput() {
            return materialThroughput;
        }

        public double getVehicleUtilization() {
            return vehicleUtilization;
        }

        public double getEquipmentUtilization() {
            return equipmentUtilization;
        }

        public int getWipTotal() {
            return wipTotal;
        }

        public double getEnergyTotal() {
            return energyTotal;
        }
    }
}
