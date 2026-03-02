package com.semi.simlogistics.scheduler.metrics.report.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * KPI metrics data class (REQ-KPI-007).
 * <p>
 * Represents current KPI metrics for dashboard display.
 * Contains dual time基准: simulatedTime and wallClockTime.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class KpiMetrics {

    private final String simulationId;
    private final double simulatedTime;  // 仿真时间（秒）
    private final LocalDateTime wallClockTime;  // 墙钟时间
    private final int totalTasksCompleted;
    private final double tasksPerHour;
    private final double materialThroughput;
    private final double vehicleUtilization;
    private final double equipmentUtilization;
    private final int wipTotal;
    private final double totalEnergy;
    private final double averageCompletionTime;

    public KpiMetrics(
            String simulationId,
            double simulatedTime,
            LocalDateTime wallClockTime,
            int totalTasksCompleted,
            double tasksPerHour,
            double materialThroughput,
            double vehicleUtilization,
            double equipmentUtilization,
            int wipTotal,
            double totalEnergy,
            double averageCompletionTime
    ) {
        this.simulationId = simulationId;
        this.simulatedTime = simulatedTime;
        this.wallClockTime = wallClockTime;
        this.totalTasksCompleted = totalTasksCompleted;
        this.tasksPerHour = tasksPerHour;
        this.materialThroughput = materialThroughput;
        this.vehicleUtilization = vehicleUtilization;
        this.equipmentUtilization = equipmentUtilization;
        this.wipTotal = wipTotal;
        this.totalEnergy = totalEnergy;
        this.averageCompletionTime = averageCompletionTime;
    }

    /**
     * Create empty KPI metrics (default values).
     *
     * @param simulationId the simulation ID
     * @return empty KPI metrics
     */
    public static KpiMetrics empty(String simulationId) {
        return new KpiMetrics(
                simulationId,
                0.0,
                LocalDateTime.now(),
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0,
                0.0,
                0.0
        );
    }

    public String getSimulationId() {
        return simulationId;
    }

    public double getSimulatedTime() {
        return simulatedTime;
    }

    public LocalDateTime getWallClockTime() {
        return wallClockTime;
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

    public double getTotalEnergy() {
        return totalEnergy;
    }

    public double getAverageCompletionTime() {
        return averageCompletionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KpiMetrics kpiMetrics = (KpiMetrics) o;
        return Double.compare(kpiMetrics.simulatedTime, simulatedTime) == 0
                && totalTasksCompleted == kpiMetrics.totalTasksCompleted
                && Double.compare(kpiMetrics.tasksPerHour, tasksPerHour) == 0
                && Double.compare(kpiMetrics.materialThroughput, materialThroughput) == 0
                && Double.compare(kpiMetrics.vehicleUtilization, vehicleUtilization) == 0
                && Double.compare(kpiMetrics.equipmentUtilization, equipmentUtilization) == 0
                && wipTotal == kpiMetrics.wipTotal
                && Double.compare(kpiMetrics.totalEnergy, totalEnergy) == 0
                && Double.compare(kpiMetrics.averageCompletionTime, averageCompletionTime) == 0
                && Objects.equals(simulationId, kpiMetrics.simulationId)
                && Objects.equals(wallClockTime, kpiMetrics.wallClockTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simulationId, simulatedTime, wallClockTime, totalTasksCompleted,
                tasksPerHour, materialThroughput, vehicleUtilization, equipmentUtilization,
                wipTotal, totalEnergy, averageCompletionTime);
    }

    @Override
    public String toString() {
        return "KpiMetrics{" +
                "simulationId='" + simulationId + '\'' +
                ", simulatedTime=" + simulatedTime +
                ", wallClockTime=" + wallClockTime +
                ", totalTasksCompleted=" + totalTasksCompleted +
                ", tasksPerHour=" + tasksPerHour +
                ", materialThroughput=" + materialThroughput +
                ", vehicleUtilization=" + vehicleUtilization +
                ", equipmentUtilization=" + equipmentUtilization +
                ", wipTotal=" + wipTotal +
                ", totalEnergy=" + totalEnergy +
                ", averageCompletionTime=" + averageCompletionTime +
                '}';
    }
}
