package com.semi.simlogistics.scheduler.metrics.calculator;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Throughput calculator (REQ-KPI-002).
 * <p>
 * Calculates task and material throughput metrics:
 * <ul>
 *   <li>Total tasks completed</li>
 *   <li>Tasks per hour rate</li>
 *   <li>Material throughput (units per hour)</li>
 *   <li>Throughput by task type</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ThroughputCalculator {

    private final MetricsRepositoryPort repository;

    /**
     * Create a new throughput calculator.
     *
     * @param repository the metrics repository
     */
    public ThroughputCalculator(MetricsRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Calculate total throughput for a simulation.
     *
     * @param simulationId the simulation ID
     * @return the throughput summary
     */
    public ThroughputSummary calculateThroughput(String simulationId) {
        MetricsRepositoryPort.MetricsSummary summary =
                repository.getSummary(simulationId).orElse(null);

        if (summary == null) {
            return new ThroughputSummary(
                    simulationId,
                    0,
                    0.0,
                    0.0,
                    new HashMap<>()
            );
        }

        return new ThroughputSummary(
                simulationId,
                summary.getTotalTasksCompleted(),
                summary.getTasksPerHour(),
                summary.getMaterialThroughput(),
                calculateThroughputByType(simulationId)
        );
    }

    /**
     * Calculate throughput over a time range.
     *
     * @param simulationId the simulation ID
     * @param fromTime     the start time (simulated time in seconds)
     * @param toTime       the end time (simulated time in seconds)
     * @return the throughput summary for the time range
     */
    public ThroughputSummary calculateThroughputInRange(
            String simulationId,
            double fromTime,
            double toTime
    ) {
        List<MetricAggregate> aggregates = repository.queryBySimulationIdAndTimeRange(
                simulationId,
                fromTime,
                toTime
        );

        if (aggregates.isEmpty()) {
            return new ThroughputSummary(
                    simulationId,
                    0,
                    0.0,
                    0.0,
                    new HashMap<>()
            );
        }

        // Sum up all aggregates in range
        int totalTasks = 0;
        double totalTasksPerHour = 0.0;
        double totalMaterialThroughput = 0.0;
        int count = 0;

        for (MetricAggregate aggregate : aggregates) {
            totalTasks += aggregate.getTasksCompleted();
            totalTasksPerHour += aggregate.getTasksPerHour();
            totalMaterialThroughput += aggregate.getMaterialThroughput();
            count++;
        }

        // Average the rate metrics
        double avgTasksPerHour = count > 0 ? totalTasksPerHour / count : 0.0;
        double avgMaterialThroughput = count > 0 ? totalMaterialThroughput / count : 0.0;

        return new ThroughputSummary(
                simulationId,
                totalTasks,
                avgTasksPerHour,
                avgMaterialThroughput,
                calculateThroughputByType(simulationId)
        );
    }

    /**
     * Calculate throughput by task type.
     *
     * @param simulationId the simulation ID
     * @return map of task type to throughput count
     */
    public Map<String, Integer> calculateThroughputByType(String simulationId) {
        Map<String, Integer> throughputByType = new HashMap<>();

        // Query TASK_COMPLETED events and group by taskType
        List<com.semi.simlogistics.scheduler.metrics.model.MetricEvent> events =
                repository.queryByEventType(com.semi.simlogistics.scheduler.metrics.model.MetricEvent.EventTypes.TASK_COMPLETED);

        // Filter by simulationId and group by taskType
        for (com.semi.simlogistics.scheduler.metrics.model.MetricEvent event : events) {
            if (!simulationId.equals(event.getSimulationId())) {
                continue;
            }

            String taskType = event.getData() != null
                    ? event.getData().getString("taskType", "UNKNOWN")
                    : "UNKNOWN";

            throughputByType.put(taskType, throughputByType.getOrDefault(taskType, 0) + 1);
        }

        return throughputByType;
    }

    /**
     * Check if throughput meets target.
     *
     * @param simulationId        the simulation ID
     * @param targetTasksPerHour  the target tasks per hour
     * @param tolerance           the tolerance percentage (e.g., 0.05 for 5%)
     * @return true if throughput meets target within tolerance
     */
    public boolean meetsTarget(
            String simulationId,
            double targetTasksPerHour,
            double tolerance
    ) {
        ThroughputSummary summary = calculateThroughput(simulationId);
        double lowerBound = targetTasksPerHour * (1.0 - tolerance);
        double upperBound = targetTasksPerHour * (1.0 + tolerance);

        return summary.getTasksPerHour() >= lowerBound
                && summary.getTasksPerHour() <= upperBound;
    }

    /**
     * Throughput summary data class.
     */
    public static class ThroughputSummary {
        private final String simulationId;
        private final int totalTasksCompleted;
        private final double tasksPerHour;
        private final double materialThroughput;
        private final Map<String, Integer> throughputByType;

        public ThroughputSummary(
                String simulationId,
                int totalTasksCompleted,
                double tasksPerHour,
                double materialThroughput,
                Map<String, Integer> throughputByType
        ) {
            this.simulationId = simulationId;
            this.totalTasksCompleted = totalTasksCompleted;
            this.tasksPerHour = tasksPerHour;
            this.materialThroughput = materialThroughput;
            this.throughputByType = throughputByType;
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

        public Map<String, Integer> getThroughputByType() {
            return throughputByType;
        }
    }
}
