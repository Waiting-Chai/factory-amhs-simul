package com.semi.simlogistics.scheduler.metrics.calculator;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Energy calculator (REQ-KPI-004).
 * <p>
 * Calculates energy consumption metrics:
 * <ul>
 *   <li>Vehicle energy consumption</li>
 *   <li>Equipment energy consumption</li>
 *   <li>Energy per task (efficiency metric)</li>
 *   <li>Total energy consumption</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class EnergyCalculator {

    private final MetricsRepositoryPort repository;

    /**
     * Create a new energy calculator.
     *
     * @param repository the metrics repository
     */
    public EnergyCalculator(MetricsRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Calculate total energy consumption for a simulation.
     *
     * @param simulationId the simulation ID
     * @return the energy summary
     */
    public EnergySummary calculateEnergyConsumption(String simulationId) {
        MetricsRepositoryPort.MetricsSummary summary =
                repository.getSummary(simulationId).orElse(null);

        if (summary == null) {
            return new EnergySummary(
                    simulationId,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    new HashMap<>()
            );
        }

        double totalTasks = summary.getTotalTasksCompleted();
        double totalEnergy = summary.getEnergyTotal();
        double materialThroughput = summary.getMaterialThroughput();

        // Calculate vehicle vs equipment energy from events
        Map<String, Double> energyByEntity = calculateEnergyByEntity(simulationId);
        double vehicleEnergy = 0.0;
        double equipmentEnergy = 0.0;

        for (Map.Entry<String, Double> entry : energyByEntity.entrySet()) {
            // Key format: "entityType:entityId"
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length >= 1) {
                String entityType = parts[0];
                if ("VEHICLE".equals(entityType)) {
                    vehicleEnergy += entry.getValue();
                } else if ("EQUIPMENT".equals(entityType)) {
                    equipmentEnergy += entry.getValue();
                }
            }
        }

        // If events recorded, use event-based total for consistency
        // Otherwise use aggregate total with default split
        double eventBasedTotal = vehicleEnergy + equipmentEnergy;
        if (eventBasedTotal > 0.0) {
            totalEnergy = eventBasedTotal;
        } else {
            vehicleEnergy = totalEnergy;
            equipmentEnergy = 0.0;
        }

        return new EnergySummary(
                simulationId,
                totalEnergy,
                vehicleEnergy,
                equipmentEnergy,
                totalTasks > 0 ? totalEnergy / totalTasks : 0.0,
                materialThroughput > 0.0 ? totalEnergy / materialThroughput : 0.0,
                energyByEntity
        );
    }

    /**
     * Calculate energy consumption over a time range.
     *
     * @param simulationId the simulation ID
     * @param fromTime     the start time (simulated time in seconds)
     * @param toTime       the end time (simulated time in seconds)
     * @return the energy summary for the time range
     */
    public EnergySummary calculateEnergyInRange(
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
            return new EnergySummary(
                    simulationId,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    new HashMap<>()
            );
        }

        // Sum up energy consumption from aggregates
        double aggregateTotalEnergy = 0.0;
        int totalTasks = 0;
        double materialThroughput = 0.0;
        int count = 0;

        for (MetricAggregate aggregate : aggregates) {
            aggregateTotalEnergy += aggregate.getEnergyTotal();
            totalTasks += aggregate.getTasksCompleted();
            materialThroughput += aggregate.getMaterialThroughput();
            count++;
        }

        // Calculate vehicle vs equipment energy from events in time range
        Map<String, Double> energyByEntity = calculateEnergyByEntityInRange(simulationId, fromTime, toTime);
        double vehicleEnergy = 0.0;
        double equipmentEnergy = 0.0;

        for (Map.Entry<String, Double> entry : energyByEntity.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length >= 1) {
                String entityType = parts[0];
                if ("VEHICLE".equals(entityType)) {
                    vehicleEnergy += entry.getValue();
                } else if ("EQUIPMENT".equals(entityType)) {
                    equipmentEnergy += entry.getValue();
                }
            }
        }

        // If events recorded in range, use event-based total for consistency
        // Otherwise use aggregate total with default split
        double totalEnergy;
        double eventBasedTotal = vehicleEnergy + equipmentEnergy;
        if (eventBasedTotal > 0.0) {
            totalEnergy = eventBasedTotal;
        } else {
            totalEnergy = aggregateTotalEnergy;
            vehicleEnergy = totalEnergy;
            equipmentEnergy = 0.0;
        }

        return new EnergySummary(
                simulationId,
                totalEnergy,
                vehicleEnergy,
                equipmentEnergy,
                totalTasks > 0 ? totalEnergy / totalTasks : 0.0,
                materialThroughput > 0 ? totalEnergy / materialThroughput : 0.0,
                energyByEntity
        );
    }

    /**
     * Calculate energy consumption by entity.
     *
     * @param simulationId the simulation ID
     * @return map of "entityType:entityId" to energy consumption
     */
    public Map<String, Double> calculateEnergyByEntity(String simulationId) {
        Map<String, Double> energyByEntity = new HashMap<>();

        // Query ENERGY_CONSUMED events and group by entityType:entityId
        List<com.semi.simlogistics.scheduler.metrics.model.MetricEvent> events =
                repository.queryByEventType(com.semi.simlogistics.scheduler.metrics.model.MetricEvent.EventTypes.ENERGY_CONSUMED);

        // Filter by simulationId and group by entity
        for (com.semi.simlogistics.scheduler.metrics.model.MetricEvent event : events) {
            if (!simulationId.equals(event.getSimulationId())) {
                continue;
            }

            String entityType = event.getEntityType() != null ? event.getEntityType() : "UNKNOWN";
            String entityId = event.getEntityId() != null ? event.getEntityId() : "unknown";
            String key = entityType + ":" + entityId;

            double energy = event.getData() != null
                    ? event.getData().getDouble("energyWattSeconds", 0.0)
                    : 0.0;

            energyByEntity.put(key, energyByEntity.getOrDefault(key, 0.0) + energy);
        }

        return energyByEntity;
    }

    /**
     * Calculate energy consumption by entity within a time range.
     *
     * @param simulationId the simulation ID
     * @param fromTime     the start time (simulated time in seconds)
     * @param toTime       the end time (simulated time in seconds)
     * @return map of "entityType:entityId" to energy consumption
     */
    private Map<String, Double> calculateEnergyByEntityInRange(
            String simulationId,
            double fromTime,
            double toTime
    ) {
        Map<String, Double> energyByEntity = new HashMap<>();

        // Query ENERGY_CONSUMED events and group by entityType:entityId
        List<com.semi.simlogistics.scheduler.metrics.model.MetricEvent> events =
                repository.queryByEventType(com.semi.simlogistics.scheduler.metrics.model.MetricEvent.EventTypes.ENERGY_CONSUMED);

        // Filter by simulationId, time range, and group by entity
        for (com.semi.simlogistics.scheduler.metrics.model.MetricEvent event : events) {
            if (!simulationId.equals(event.getSimulationId())) {
                continue;
            }

            // Filter by time range
            double simulatedTime = event.getSimulatedTime();
            if (simulatedTime < fromTime || simulatedTime > toTime) {
                continue;
            }

            String entityType = event.getEntityType() != null ? event.getEntityType() : "UNKNOWN";
            String entityId = event.getEntityId() != null ? event.getEntityId() : "unknown";
            String key = entityType + ":" + entityId;

            double energy = event.getData() != null
                    ? event.getData().getDouble("energyWattSeconds", 0.0)
                    : 0.0;

            energyByEntity.put(key, energyByEntity.getOrDefault(key, 0.0) + energy);
        }

        return energyByEntity;
    }

    /**
     * Calculate energy efficiency score.
     * <p>
     * Higher score means better efficiency (more tasks per unit of energy).
     *
     * @param simulationId the simulation ID
     * @param baselineEnergyPerTask the baseline energy per task
     * @return efficiency score (1.0 = baseline, >1.0 = better, <1.0 = worse)
     */
    public double calculateEfficiencyScore(
            String simulationId,
            double baselineEnergyPerTask
    ) {
        EnergySummary summary = calculateEnergyConsumption(simulationId);
        double actualEnergyPerTask = summary.getEnergyPerTask();

        if (actualEnergyPerTask <= 0) {
            return 0.0;
        }

        return baselineEnergyPerTask / actualEnergyPerTask;
    }

    /**
     * Energy summary data class.
     */
    public static class EnergySummary {
        private final String simulationId;
        private final double totalEnergy;          // watt-seconds
        private final double vehicleEnergy;         // watt-seconds
        private final double equipmentEnergy;       // watt-seconds
        private final double energyPerTask;         // watt-seconds per task
        private final double energyPerMaterialUnit; // watt-seconds per unit
        private final Map<String, Double> energyByEntity;

        public EnergySummary(
                String simulationId,
                double totalEnergy,
                double vehicleEnergy,
                double equipmentEnergy,
                double energyPerTask,
                double energyPerMaterialUnit,
                Map<String, Double> energyByEntity
        ) {
            this.simulationId = simulationId;
            this.totalEnergy = totalEnergy;
            this.vehicleEnergy = vehicleEnergy;
            this.equipmentEnergy = equipmentEnergy;
            this.energyPerTask = energyPerTask;
            this.energyPerMaterialUnit = energyPerMaterialUnit;
            this.energyByEntity = energyByEntity;
        }

        public String getSimulationId() {
            return simulationId;
        }

        public double getTotalEnergy() {
            return totalEnergy;
        }

        public double getVehicleEnergy() {
            return vehicleEnergy;
        }

        public double getEquipmentEnergy() {
            return equipmentEnergy;
        }

        public double getEnergyPerTask() {
            return energyPerTask;
        }

        public double getEnergyPerMaterialUnit() {
            return energyPerMaterialUnit;
        }

        public Map<String, Double> getEnergyByEntity() {
            return energyByEntity;
        }

        /**
         * Convert watt-seconds to kilowatt-hours.
         */
        public double getTotalEnergyKwh() {
            return totalEnergy / 3_600_000.0;  // 1 kWh = 3,600,000 watt-seconds
        }
    }
}
