package com.semi.simlogistics.scheduler.metrics.report.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Dashboard snapshot data class (REQ-KPI-007).
 * <p>
 * Represents real-time dashboard data with current KPI and historical trend.
 * Output structure is stable with fixed field names for frontend consumption.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class DashboardSnapshot {

    private final String simulationId;
    private final KpiMetrics currentKpi;
    private final List<KpiMetrics> trendData;
    private final LocalDateTime generatedAt;

    public DashboardSnapshot(
            String simulationId,
            KpiMetrics currentKpi,
            List<KpiMetrics> trendData,
            LocalDateTime generatedAt
    ) {
        this.simulationId = simulationId;
        this.currentKpi = currentKpi != null ? currentKpi : KpiMetrics.empty(simulationId);
        this.trendData = trendData != null ? trendData : Collections.emptyList();
        this.generatedAt = generatedAt != null ? generatedAt : LocalDateTime.now();
    }

    /**
     * Create empty dashboard snapshot.
     *
     * @param simulationId the simulation ID
     * @return empty dashboard snapshot
     */
    public static DashboardSnapshot empty(String simulationId) {
        return new DashboardSnapshot(
                simulationId,
                KpiMetrics.empty(simulationId),
                Collections.emptyList(),
                LocalDateTime.now()
        );
    }

    public String getSimulationId() {
        return simulationId;
    }

    public KpiMetrics getCurrentKpi() {
        return currentKpi;
    }

    public List<KpiMetrics> getTrendData() {
        return trendData;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardSnapshot that = (DashboardSnapshot) o;
        return Objects.equals(simulationId, that.simulationId)
                && Objects.equals(currentKpi, that.currentKpi)
                && Objects.equals(trendData, that.trendData)
                && Objects.equals(generatedAt, that.generatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simulationId, currentKpi, trendData, generatedAt);
    }

    @Override
    public String toString() {
        return "DashboardSnapshot{" +
                "simulationId='" + simulationId + '\'' +
                ", currentKpi=" + currentKpi +
                ", trendData=" + trendData +
                ", generatedAt=" + generatedAt +
                '}';
    }
}
