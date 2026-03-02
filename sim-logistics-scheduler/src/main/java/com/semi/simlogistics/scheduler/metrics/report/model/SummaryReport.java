package com.semi.simlogistics.scheduler.metrics.report.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Summary report data class (REQ-KPI-007).
 * <p>
 * Represents aggregated KPI metrics for summary reporting.
 * Provides unified snapshot for export layer reuse.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class SummaryReport {

    private final String simulationId;
    private final LocalDateTime generatedAt;
    private final int totalTasksCompleted;
    private final double tasksPerHour;
    private final double materialThroughput;
    private final double vehicleUtilization;
    private final double equipmentUtilization;
    private final int wipTotal;
    private final double totalEnergy;
    private final double averageCompletionTime;
    private final String bottleneckSummary;

    public SummaryReport(
            String simulationId,
            LocalDateTime generatedAt,
            int totalTasksCompleted,
            double tasksPerHour,
            double materialThroughput,
            double vehicleUtilization,
            double equipmentUtilization,
            int wipTotal,
            double totalEnergy,
            double averageCompletionTime,
            String bottleneckSummary
    ) {
        this.simulationId = simulationId;
        this.generatedAt = generatedAt;
        this.totalTasksCompleted = totalTasksCompleted;
        this.tasksPerHour = tasksPerHour;
        this.materialThroughput = materialThroughput;
        this.vehicleUtilization = vehicleUtilization;
        this.equipmentUtilization = equipmentUtilization;
        this.wipTotal = wipTotal;
        this.totalEnergy = totalEnergy;
        this.averageCompletionTime = averageCompletionTime;
        this.bottleneckSummary = bottleneckSummary != null ? bottleneckSummary : "";
    }

    /**
     * Create empty summary report (default values).
     *
     * @param simulationId the simulation ID
     * @return empty summary report
     */
    public static SummaryReport empty(String simulationId) {
        return new SummaryReport(
                simulationId,
                LocalDateTime.now(),
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0,
                0.0,
                0.0,
                ""
        );
    }

    public String getSimulationId() {
        return simulationId;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
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

    public String getBottleneckSummary() {
        return bottleneckSummary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SummaryReport that = (SummaryReport) o;
        return totalTasksCompleted == that.totalTasksCompleted
                && Double.compare(that.tasksPerHour, tasksPerHour) == 0
                && Double.compare(that.materialThroughput, materialThroughput) == 0
                && Double.compare(that.vehicleUtilization, vehicleUtilization) == 0
                && Double.compare(that.equipmentUtilization, equipmentUtilization) == 0
                && wipTotal == that.wipTotal
                && Double.compare(that.totalEnergy, totalEnergy) == 0
                && Double.compare(that.averageCompletionTime, averageCompletionTime) == 0
                && Objects.equals(simulationId, that.simulationId)
                && Objects.equals(generatedAt, that.generatedAt)
                && Objects.equals(bottleneckSummary, that.bottleneckSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simulationId, generatedAt, totalTasksCompleted, tasksPerHour,
                materialThroughput, vehicleUtilization, equipmentUtilization, wipTotal,
                totalEnergy, averageCompletionTime, bottleneckSummary);
    }

    @Override
    public String toString() {
        return "SummaryReport{" +
                "simulationId='" + simulationId + '\'' +
                ", generatedAt=" + generatedAt +
                ", totalTasksCompleted=" + totalTasksCompleted +
                ", tasksPerHour=" + tasksPerHour +
                ", materialThroughput=" + materialThroughput +
                ", vehicleUtilization=" + vehicleUtilization +
                ", equipmentUtilization=" + equipmentUtilization +
                ", wipTotal=" + wipTotal +
                ", totalEnergy=" + totalEnergy +
                ", averageCompletionTime=" + averageCompletionTime +
                ", bottleneckSummary='" + bottleneckSummary + '\'' +
                '}';
    }
}
