package com.semi.simlogistics.scheduler.metrics.analysis;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.port.ConfigPort;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bottleneck analyzer (REQ-KPI-005).
 * <p>
 * Identifies system bottlenecks:
 * <ul>
 *   <li>High utilization resources (bottleneck entities)</li>
 *   <li>Congested paths/edges</li>
 *   <li>WIP accumulation points</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class BottleneckAnalyzer {

    private final MetricsRepositoryPort repository;
    private final ConfigPort config;

    private final double warningThreshold;
    private final double criticalThreshold;
    private final int wipWarningThreshold;

    /**
     * Create a new bottleneck analyzer.
     *
     * @param repository the metrics repository
     * @param config     the configuration port
     */
    public BottleneckAnalyzer(
            MetricsRepositoryPort repository,
            ConfigPort config
    ) {
        this.repository = repository;
        this.config = config;

        this.warningThreshold = config.getDouble(
                ConfigPort.Keys.UTILIZATION_WARNING_THRESHOLD,
                ConfigPort.Defaults.UTILIZATION_WARNING
        );
        this.criticalThreshold = config.getDouble(
                ConfigPort.Keys.UTILIZATION_CRITICAL_THRESHOLD,
                ConfigPort.Defaults.UTILIZATION_CRITICAL
        );
        this.wipWarningThreshold = config.getInt(
                ConfigPort.Keys.WIP_WARNING_THRESHOLD,
                ConfigPort.Defaults.WIP_WARNING
        );
    }

    /**
     * Analyze bottlenecks for a simulation.
     *
     * @param simulationId the simulation ID
     * @return the bottleneck report
     */
    public BottleneckReport analyzeBottlenecks(String simulationId) {
        List<MetricAggregate> aggregates = repository.queryBySimulationIdAndTimeRange(
                simulationId,
                0,
                Double.MAX_VALUE
        );

        if (aggregates.isEmpty()) {
            return new BottleneckReport(
                    simulationId,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    "No metrics data available"
            );
        }

        // Get latest aggregate for current state
        MetricAggregate latest = aggregates.get(aggregates.size() - 1);

        // Analyze bottlenecks
        List<BottleneckItem> utilizationBottlenecks = analyzeUtilizationBottlenecks(latest);
        List<BottleneckItem> wipBottlenecks = analyzeWIPBottlenecks(latest);
        List<BottleneckItem> congestionBottlenecks = analyzeCongestionBottlenecks(aggregates);

        // Generate overall message
        String message = generateSummaryMessage(
                utilizationBottlenecks,
                wipBottlenecks,
                congestionBottlenecks
        );

        return new BottleneckReport(
                simulationId,
                utilizationBottlenecks,
                wipBottlenecks,
                congestionBottlenecks,
                message
        );
    }

    /**
     * Analyze utilization-based bottlenecks.
     */
    private List<BottleneckItem> analyzeUtilizationBottlenecks(MetricAggregate aggregate) {
        List<BottleneckItem> bottlenecks = new ArrayList<>();

        double vehicleUtil = aggregate.getVehicleUtilization();
        double equipmentUtil = aggregate.getEquipmentUtilization();

        if (vehicleUtil >= criticalThreshold) {
            bottlenecks.add(new BottleneckItem(
                    "VEHICLES",
                    "Vehicle Fleet",
                    vehicleUtil,
                    BottleneckSeverity.CRITICAL,
                    "Vehicle utilization at critical level: " + String.format("%.1f%%", vehicleUtil * 100)
            ));
        } else if (vehicleUtil >= warningThreshold) {
            bottlenecks.add(new BottleneckItem(
                    "VEHICLES",
                    "Vehicle Fleet",
                    vehicleUtil,
                    BottleneckSeverity.WARNING,
                    "Vehicle utilization above warning threshold: " + String.format("%.1f%%", vehicleUtil * 100)
            ));
        }

        if (equipmentUtil >= criticalThreshold) {
            bottlenecks.add(new BottleneckItem(
                    "EQUIPMENT",
                    "Equipment Fleet",
                    equipmentUtil,
                    BottleneckSeverity.CRITICAL,
                    "Equipment utilization at critical level: " + String.format("%.1f%%", equipmentUtil * 100)
            ));
        } else if (equipmentUtil >= warningThreshold) {
            bottlenecks.add(new BottleneckItem(
                    "EQUIPMENT",
                    "Equipment Fleet",
                    equipmentUtil,
                    BottleneckSeverity.WARNING,
                    "Equipment utilization above warning threshold: " + String.format("%.1f%%", equipmentUtil * 100)
            ));
        }

        // Sort by utilization descending
        bottlenecks.sort(Comparator.comparingDouble(BottleneckItem::getValue).reversed());

        return bottlenecks;
    }

    /**
     * Analyze WIP-based bottlenecks.
     */
    private List<BottleneckItem> analyzeWIPBottlenecks(MetricAggregate aggregate) {
        List<BottleneckItem> bottlenecks = new ArrayList<>();

        int wipTotal = aggregate.getWipTotal();

        if (wipTotal >= wipWarningThreshold * 2) {
            bottlenecks.add(new BottleneckItem(
                    "WIP_GLOBAL",
                    "Global WIP",
                    wipTotal / (double) wipWarningThreshold,
                    BottleneckSeverity.CRITICAL,
                    "Global WIP at critical level: " + wipTotal + " (threshold: " + wipWarningThreshold + ")"
            ));
        } else if (wipTotal >= wipWarningThreshold) {
            bottlenecks.add(new BottleneckItem(
                    "WIP_GLOBAL",
                    "Global WIP",
                    wipTotal / (double) wipWarningThreshold,
                    BottleneckSeverity.WARNING,
                    "Global WIP above warning threshold: " + wipTotal + " (threshold: " + wipWarningThreshold + ")"
            ));
        }

        return bottlenecks;
    }

    /**
     * Analyze congestion-based bottlenecks.
     */
    private List<BottleneckItem> analyzeCongestionBottlenecks(List<MetricAggregate> aggregates) {
        List<BottleneckItem> bottlenecks = new ArrayList<>();

        // Check for declining throughput (potential congestion)
        if (aggregates.size() >= 3) {
            int recentCount = Math.min(5, aggregates.size());
            double recentAvg = 0;
            double olderAvg = 0;
            int olderSamples = 0;

            for (int i = 0; i < recentCount; i++) {
                recentAvg += aggregates.get(aggregates.size() - 1 - i).getTasksPerHour();
            }
            recentAvg /= recentCount;

            for (int i = recentCount; i < Math.min(recentCount * 2, aggregates.size()); i++) {
                olderAvg += aggregates.get(aggregates.size() - 1 - i).getTasksPerHour();
                olderSamples++;
            }
            if (olderSamples > 0) {
                olderAvg /= olderSamples;
            }

            // If throughput declining by more than 20%
            if (olderAvg > 0 && recentAvg < olderAvg * 0.8) {
                bottlenecks.add(new BottleneckItem(
                        "THROUGHPUT_DECLINE",
                        "System Throughput",
                        recentAvg / olderAvg,
                        BottleneckSeverity.WARNING,
                        String.format("Throughput declining: %.1f -> %.1f tasks/hour", olderAvg, recentAvg)
                ));
            }
        }

        return bottlenecks;
    }

    /**
     * Generate a summary message for the bottleneck report.
     */
    private String generateSummaryMessage(
            List<BottleneckItem> utilizationBottlenecks,
            List<BottleneckItem> wipBottlenecks,
            List<BottleneckItem> congestionBottlenecks
    ) {
        int criticalCount = 0;
        int warningCount = 0;

        for (BottleneckItem item : utilizationBottlenecks) {
            if (item.getSeverity() == BottleneckSeverity.CRITICAL) criticalCount++;
            else warningCount++;
        }

        for (BottleneckItem item : wipBottlenecks) {
            if (item.getSeverity() == BottleneckSeverity.CRITICAL) criticalCount++;
            else warningCount++;
        }

        for (BottleneckItem item : congestionBottlenecks) {
            if (item.getSeverity() == BottleneckSeverity.CRITICAL) criticalCount++;
            else warningCount++;
        }

        if (criticalCount > 0) {
            return "CRITICAL: " + criticalCount + " critical bottleneck(s) detected. " +
                   warningCount + " warning(s).";
        } else if (warningCount > 0) {
            return "WARNING: " + warningCount + " bottleneck(s) detected. " +
                   "Monitor system performance.";
        } else {
            return "No significant bottlenecks detected.";
        }
    }

    /**
     * Bottleneck severity levels.
     */
    public enum BottleneckSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * Bottleneck item data class.
     */
    public static class BottleneckItem {
        private final String id;
        private final String name;
        private final double value;  // utilization ratio or WIP ratio
        private final BottleneckSeverity severity;
        private final String description;

        public BottleneckItem(
                String id,
                String name,
                double value,
                BottleneckSeverity severity,
                String description
        ) {
            this.id = id;
            this.name = name;
            this.value = value;
            this.severity = severity;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getValue() {
            return value;
        }

        public BottleneckSeverity getSeverity() {
            return severity;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Bottleneck report data class.
     */
    public static class BottleneckReport {
        private final String simulationId;
        private final List<BottleneckItem> utilizationBottlenecks;
        private final List<BottleneckItem> wipBottlenecks;
        private final List<BottleneckItem> congestionBottlenecks;
        private final String message;

        public BottleneckReport(
                String simulationId,
                List<BottleneckItem> utilizationBottlenecks,
                List<BottleneckItem> wipBottlenecks,
                List<BottleneckItem> congestionBottlenecks,
                String message
        ) {
            this.simulationId = simulationId;
            this.utilizationBottlenecks = utilizationBottlenecks;
            this.wipBottlenecks = wipBottlenecks;
            this.congestionBottlenecks = congestionBottlenecks;
            this.message = message;
        }

        public String getSimulationId() {
            return simulationId;
        }

        public List<BottleneckItem> getUtilizationBottlenecks() {
            return utilizationBottlenecks;
        }

        public List<BottleneckItem> getWipBottlenecks() {
            return wipBottlenecks;
        }

        public List<BottleneckItem> getCongestionBottlenecks() {
            return congestionBottlenecks;
        }

        public String getMessage() {
            return message;
        }

        /**
         * Get all bottlenecks regardless of type.
         */
        public List<BottleneckItem> getAllBottlenecks() {
            List<BottleneckItem> all = new ArrayList<>();
            all.addAll(utilizationBottlenecks);
            all.addAll(wipBottlenecks);
            all.addAll(congestionBottlenecks);
            return all;
        }

        /**
         * Check if there are any critical bottlenecks.
         */
        public boolean hasCriticalBottlenecks() {
            return getAllBottlenecks().stream()
                    .anyMatch(item -> item.getSeverity() == BottleneckSeverity.CRITICAL);
        }
    }
}
