package com.semi.simlogistics.scheduler.metrics.analysis;

import com.semi.simlogistics.scheduler.metrics.calculator.ThroughputCalculator;
import com.semi.simlogistics.scheduler.metrics.calculator.ThroughputCalculator.ThroughputSummary;
import com.semi.simlogistics.scheduler.metrics.port.ConfigPort;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Vehicle count evaluator (REQ-KPI-006).
 * <p>
 * Evaluates optimal vehicle configuration using binary search:
 * <ul>
 *   <li>Minimum vehicles to meet throughput targets</li>
 *   <li>Binary search algorithm for efficient evaluation</li>
 *   <li>Multi-objective optimization interface (reserved for future)</li>
 * </ul>
 * <p>
 * Current implementation provides binary search framework.
 * Actual simulation execution requires integration with simulation engine.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class VehicleCountEvaluator {

    private final MetricsRepositoryPort repository;
    private final ConfigPort config;
    private final ThroughputCalculator throughputCalculator;

    /**
     * Throughput target definition.
     */
    public static class ThroughputTarget {
        private final double targetTasksPerHour;
        private final double targetMaterialPerHour;
        private final double tolerance;

        public ThroughputTarget(double targetTasksPerHour, double tolerance) {
            this.targetTasksPerHour = targetTasksPerHour;
            this.targetMaterialPerHour = targetTasksPerHour * 5;  // Simplified: 5 materials per task
            this.tolerance = tolerance;
        }

        public ThroughputTarget(double tasksPerHour, double materialPerHour, double tolerance) {
            this.targetTasksPerHour = tasksPerHour;
            this.targetMaterialPerHour = materialPerHour;
            this.tolerance = tolerance;
        }

        public double getTargetTasksPerHour() {
            return targetTasksPerHour;
        }

        public double getTargetMaterialPerHour() {
            return targetMaterialPerHour;
        }

        public double getTolerance() {
            return tolerance;
        }
    }

    /**
     * Evaluation result for a vehicle count.
     */
    public static class EvaluationResult {
        private final int vehicleCount;
        private final ThroughputSummary throughput;
        private final boolean meetsTarget;
        private final String reason;

        public EvaluationResult(
                int vehicleCount,
                ThroughputSummary throughput,
                boolean meetsTarget,
                String reason
        ) {
            this.vehicleCount = vehicleCount;
            this.throughput = throughput;
            this.meetsTarget = meetsTarget;
            this.reason = reason;
        }

        public int getVehicleCount() {
            return vehicleCount;
        }

        public ThroughputSummary getThroughput() {
            return throughput;
        }

        public boolean meetsTarget() {
            return meetsTarget;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Vehicle count evaluation report.
     */
    public static class EvaluationReport {
        private final int optimalVehicleCount;
        private final int minVehicleCount;
        private final int maxVehicleCount;
        private final List<EvaluationResult> evaluations;
        private final String summary;

        public EvaluationReport(
                int optimalVehicleCount,
                int minVehicleCount,
                int maxVehicleCount,
                List<EvaluationResult> evaluations,
                String summary
        ) {
            this.optimalVehicleCount = optimalVehicleCount;
            this.minVehicleCount = minVehicleCount;
            this.maxVehicleCount = maxVehicleCount;
            this.evaluations = evaluations;
            this.summary = summary;
        }

        public int getOptimalVehicleCount() {
            return optimalVehicleCount;
        }

        public int getMinVehicleCount() {
            return minVehicleCount;
        }

        public int getMaxVehicleCount() {
            return maxVehicleCount;
        }

        public List<EvaluationResult> getEvaluations() {
            return evaluations;
        }

        public String getSummary() {
            return summary;
        }

        /**
         * Get the throughput at optimal vehicle count.
         */
        public ThroughputSummary getOptimalThroughput() {
            return evaluations.stream()
                    .filter(e -> e.getVehicleCount() == optimalVehicleCount)
                    .findFirst()
                    .map(EvaluationResult::getThroughput)
                    .orElse(null);
        }
    }

    /**
     * Create a new vehicle count evaluator.
     *
     * @param repository the metrics repository
     * @param config     the configuration port
     */
    public VehicleCountEvaluator(
            MetricsRepositoryPort repository,
            ConfigPort config
    ) {
        this.repository = repository;
        this.config = config;
        this.throughputCalculator = new ThroughputCalculator(repository);
    }

    /**
     * Find the minimum vehicle count to meet throughput target using binary search.
     * <p>
     * This is a simplified implementation that uses historical data.
     * Full implementation would require simulation engine integration.
     *
     * @param simulationId       the simulation ID to use as reference
     * @param currentVehicleCount the current vehicle count
     * @param target             the throughput target
     * @return the evaluation report
     */
    public EvaluationReport findMinimumVehicles(
            String simulationId,
            int currentVehicleCount,
            ThroughputTarget target
    ) {
        // Get current throughput as reference
        ThroughputSummary currentThroughput = throughputCalculator.calculateThroughput(simulationId);
        double currentTasksPerHour = currentThroughput.getTasksPerHour();

        // If current already meets target, try to reduce
        if (currentTasksPerHour >= target.getTargetTasksPerHour() * (1 - target.getTolerance())) {
            // Current count works, try lower
            return searchDownward(simulationId, currentVehicleCount, target, currentTasksPerHour);
        } else {
            // Current count insufficient, need more vehicles
            return searchUpward(simulationId, currentVehicleCount, target, currentTasksPerHour);
        }
    }

    /**
     * Binary search downward to find minimum sufficient vehicles.
     */
    private EvaluationReport searchDownward(
            String simulationId,
            int currentCount,
            ThroughputTarget target,
            double currentThroughput
    ) {
        List<EvaluationResult> evaluations = new ArrayList<>();
        int optimalCount = currentCount;
        int low = 1;
        int high = currentCount;

        // Add current evaluation
        evaluations.add(new EvaluationResult(
                currentCount,
                new ThroughputSummary(simulationId, 0, currentThroughput, currentThroughput, null),
                true,
                "Current configuration meets target"
        ));

        // Binary search for minimum
        while (low < high) {
            int mid = (low + high) / 2;

            // Simplified: assume linear scaling
            // In production, this would run actual simulation
            double projectedThroughput = projectThroughput(currentThroughput, currentCount, mid);

            boolean meetsTarget = projectedThroughput >= target.getTargetTasksPerHour() * (1 - target.getTolerance());

            evaluations.add(new EvaluationResult(
                    mid,
                    new ThroughputSummary(simulationId, 0, projectedThroughput, projectedThroughput * 5, null),
                    meetsTarget,
                    meetsTarget ? "Projected throughput meets target" : "Projected throughput insufficient"
            ));

            if (meetsTarget) {
                optimalCount = mid;
                high = mid;  // Try fewer vehicles
            } else {
                low = mid + 1;  // Need more vehicles
            }
        }

        String summary = String.format(
                "Optimal vehicle count: %d (current: %d). Projected throughput: %.1f tasks/hour",
                optimalCount,
                currentCount,
                projectThroughput(currentThroughput, currentCount, optimalCount)
        );

        return new EvaluationReport(
                optimalCount,
                low,
                currentCount,
                evaluations,
                summary
        );
    }

    /**
     * Binary search upward to find sufficient vehicles.
     */
    private EvaluationReport searchUpward(
            String simulationId,
            int currentCount,
            ThroughputTarget target,
            double currentThroughput
    ) {
        List<EvaluationResult> evaluations = new ArrayList<>();
        int optimalCount = -1;
        int low = currentCount;
        int high = currentCount * 2;  // Upper bound

        evaluations.add(new EvaluationResult(
                currentCount,
                new ThroughputSummary(simulationId, 0, currentThroughput, currentThroughput, null),
                false,
                "Current configuration insufficient"
        ));

        while (low <= high && optimalCount < 0) {
            int mid = (low + high) / 2;
            double projectedThroughput = projectThroughput(currentThroughput, currentCount, mid);

            boolean meetsTarget = projectedThroughput >= target.getTargetTasksPerHour() * (1 - target.getTolerance());

            evaluations.add(new EvaluationResult(
                    mid,
                    new ThroughputSummary(simulationId, 0, projectedThroughput, projectedThroughput * 5, null),
                    meetsTarget,
                    meetsTarget ? "Projected throughput meets target" : "Projected throughput insufficient"
            ));

            if (meetsTarget) {
                optimalCount = mid;
                high = mid - 1;  // Try to find even lower
            } else {
                low = mid + 1;  // Need more vehicles
            }
        }

        if (optimalCount < 0) {
            optimalCount = high;  // Use upper bound as best estimate
        }

        String summary = String.format(
                "Optimal vehicle count: %d (current: %d insufficient). Projected throughput: %.1f tasks/hour",
                optimalCount,
                currentCount,
                projectThroughput(currentThroughput, currentCount, optimalCount)
        );

        return new EvaluationReport(
                optimalCount,
                currentCount,
                high,
                evaluations,
                summary
        );
    }

    /**
     * Project throughput based on vehicle count (simplified linear model).
     * <p>
     * In production, this would run actual simulation with the given vehicle count.
     * This is a placeholder implementation.
     *
     * @param baseThroughput    the baseline throughput
     * @param baseVehicleCount  the baseline vehicle count
     * @param targetVehicleCount the target vehicle count
     * @return projected throughput
     */
    private double projectThroughput(double baseThroughput, int baseVehicleCount, int targetVehicleCount) {
        // Simplified: assume linear scaling
        // In production, this would use actual simulation results
        return baseThroughput * ((double) targetVehicleCount / baseVehicleCount);
    }

    /**
     * Evaluate throughput for a specific vehicle count.
     * <p>
     * This is a simplified implementation using projection.
     * Full implementation would run actual simulation.
     *
     * @param simulationId       the reference simulation ID
     * @param currentVehicleCount the current vehicle count
     * @param targetVehicleCount  the vehicle count to evaluate
     * @param target             the throughput target
     * @return the evaluation result
     */
    public EvaluationResult evaluateVehicleCount(
            String simulationId,
            int currentVehicleCount,
            int targetVehicleCount,
            ThroughputTarget target
    ) {
        ThroughputSummary currentThroughput = throughputCalculator.calculateThroughput(simulationId);
        double currentTasksPerHour = currentThroughput.getTasksPerHour();

        double projectedTasksPerHour = projectThroughput(
                currentTasksPerHour,
                currentVehicleCount,
                targetVehicleCount
        );

        boolean meetsTarget = projectedTasksPerHour >= target.getTargetTasksPerHour() * (1 - target.getTolerance());

        String reason;
        if (meetsTarget) {
            reason = String.format(
                    "Projected throughput %.1f meets target %.1f ±%.0f%%",
                    projectedTasksPerHour,
                    target.getTargetTasksPerHour(),
                    target.getTolerance() * 100
            );
        } else {
            reason = String.format(
                    "Projected throughput %.1f below target %.1f ±%.0f%%",
                    projectedTasksPerHour,
                    target.getTargetTasksPerHour(),
                    target.getTolerance() * 100
            );
        }

        return new EvaluationResult(
                targetVehicleCount,
                new ThroughputSummary(simulationId, 0, projectedTasksPerHour, projectedTasksPerHour * 5, null),
                meetsTarget,
                reason
        );
    }

    /**
     * Create a default throughput target from configuration.
     *
     * @return the default throughput target
     */
    public ThroughputTarget createDefaultTarget() {
        double targetTasksPerHour = config.getDouble(
                ConfigPort.Keys.THROUGHPUT_TARGET_TASKS_PER_HOUR,
                ConfigPort.Defaults.TARGET_TASKS_PER_HOUR
        );
        double tolerance = 0.05;  // 5% tolerance
        return new ThroughputTarget(targetTasksPerHour, tolerance);
    }
}
