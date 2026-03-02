package com.semi.simlogistics.scheduler.metrics.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metric aggregate data model (定期聚合数据).
 * <p>
 * Represents aggregated metrics computed over a time period.
 * Typically, generated every 60 seconds of simulated time.
 * <p>
 * Uses dual time基准:
 * <ul>
 *   <li>recordedAt: 仿真时间（秒）</li>
 *   <li>wallClockTime: 墙钟时间</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class MetricAggregate {

    private final String id;
    private final String simulationId;
    private final double recordedAt;  // 仿真时间（秒）
    private final LocalDateTime wallClockTime;  // 墙钟时间

    // throughput metrics
    private final int tasksCompleted;
    private final double tasksPerHour;
    private final double materialThroughput;

    // Utilization metrics (time-weighted average)
    private final double vehicleUtilization;
    private final double equipmentUtilization;

    // WIP metrics
    private final int wipTotal;

    // Energy consumption indicators
    private final double energyTotal;

    // Extended metrics
    private final MetricData customMetrics;

    private MetricAggregate(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.simulationId = builder.simulationId;
        this.recordedAt = builder.recordedAt;
        this.wallClockTime = builder.wallClockTime != null
                ? builder.wallClockTime
                : LocalDateTime.now();
        this.tasksCompleted = builder.tasksCompleted;
        this.tasksPerHour = builder.tasksPerHour;
        this.materialThroughput = builder.materialThroughput;
        this.vehicleUtilization = builder.vehicleUtilization;
        this.equipmentUtilization = builder.equipmentUtilization;
        this.wipTotal = builder.wipTotal;
        this.energyTotal = builder.energyTotal;
        this.customMetrics = builder.customMetrics != null
                ? builder.customMetrics
                : new MetricData();
    }

    public String getId() {
        return id;
    }

    public String getSimulationId() {
        return simulationId;
    }

    public double getRecordedAt() {
        return recordedAt;
    }

    public LocalDateTime getWallClockTime() {
        return wallClockTime;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
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

    public MetricData getCustomMetrics() {
        return customMetrics;
    }

    /**
     * Builder for MetricAggregate.
     */
    public static class Builder {
        private String id;
        private String simulationId;
        private double recordedAt;
        private LocalDateTime wallClockTime;
        private int tasksCompleted;
        private double tasksPerHour;
        private double materialThroughput;
        private double vehicleUtilization;
        private double equipmentUtilization;
        private int wipTotal;
        private double energyTotal;
        private MetricData customMetrics;

        public Builder(String simulationId, double recordedAt) {
            this.simulationId = simulationId;
            this.recordedAt = recordedAt;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder wallClockTime(LocalDateTime wallClockTime) {
            this.wallClockTime = wallClockTime;
            return this;
        }

        public Builder tasksCompleted(int tasksCompleted) {
            this.tasksCompleted = tasksCompleted;
            return this;
        }

        public Builder tasksPerHour(double tasksPerHour) {
            this.tasksPerHour = tasksPerHour;
            return this;
        }

        public Builder materialThroughput(double materialThroughput) {
            this.materialThroughput = materialThroughput;
            return this;
        }

        public Builder vehicleUtilization(double vehicleUtilization) {
            this.vehicleUtilization = vehicleUtilization;
            return this;
        }

        public Builder equipmentUtilization(double equipmentUtilization) {
            this.equipmentUtilization = equipmentUtilization;
            return this;
        }

        public Builder wipTotal(int wipTotal) {
            this.wipTotal = wipTotal;
            return this;
        }

        public Builder energyTotal(double energyTotal) {
            this.energyTotal = energyTotal;
            return this;
        }

        public Builder customMetrics(MetricData customMetrics) {
            this.customMetrics = customMetrics;
            return this;
        }

        public MetricAggregate build() {
            return new MetricAggregate(this);
        }
    }
}
