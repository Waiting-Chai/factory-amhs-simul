package com.semi.simlogistics.scheduler.metrics.report.service;

import com.semi.simlogistics.scheduler.metrics.analysis.BottleneckAnalyzer;
import com.semi.simlogistics.scheduler.metrics.calculator.EnergyCalculator;
import com.semi.simlogistics.scheduler.metrics.calculator.ThroughputCalculator;
import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;
import com.semi.simlogistics.scheduler.metrics.port.ConfigPort;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryConfig;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import com.semi.simlogistics.scheduler.metrics.report.model.DashboardSnapshot;
import com.semi.simlogistics.scheduler.metrics.report.model.KpiMetrics;
import com.semi.simlogistics.scheduler.metrics.report.model.SummaryReport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Report generator service (REQ-KPI-007).
 * <p>
 * Assembles KPI metrics data for report export.
 * Provides unified snapshot objects for export layer reuse.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate summary reports with all required fields</li>
 *   <li>Build dashboard snapshots with current KPI and trend data</li>
 *   <li>Calculate average completion time from task events</li>
 *   <li>Identify bottlenecks for summary reports</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ReportGenerator {

    private final MetricsRepositoryPort repository;
    private final ThroughputCalculator throughputCalculator;
    private final EnergyCalculator energyCalculator;
    private final BottleneckAnalyzer bottleneckAnalyzer;

    /**
     * Create a new report generator.
     *
     * @param repository           the metrics repository
     * @param throughputCalculator the throughput calculator
     * @param energyCalculator     the energy calculator
     */
    public ReportGenerator(
            MetricsRepositoryPort repository,
            ThroughputCalculator throughputCalculator,
            EnergyCalculator energyCalculator
    ) {
        this.repository = repository;
        this.throughputCalculator = throughputCalculator;
        this.energyCalculator = energyCalculator;
        ConfigPort config = new InMemoryConfig();
        this.bottleneckAnalyzer = new BottleneckAnalyzer(repository, config);
    }

    /**
     * Generate summary report for a simulation.
     * <p>
     * Assembles required fields:
     * <ul>
     *   <li>Throughput (tasksPerHour / materialThroughput)</li>
     *   <li>Average completion time</li>
     *   <li>Resource utilization (vehicle/equipment)</li>
     *   <li>Bottleneck identification</li>
     * </ul>
     *
     * @param simulationId the simulation ID
     * @return the summary report
     */
    public SummaryReport generateSummaryReport(String simulationId) {
        MetricsRepositoryPort.MetricsSummary summary =
                repository.getSummary(simulationId).orElse(null);

        if (summary == null) {
            return SummaryReport.empty(simulationId);
        }

        // Calculate average completion time from task events
        double avgCompletionTime = calculateAverageCompletionTime(simulationId);

        // Get energy summary
        EnergyCalculator.EnergySummary energySummary =
                energyCalculator.calculateEnergyConsumption(simulationId);

        // Analyze bottlenecks
        BottleneckAnalyzer.BottleneckReport bottleneckReport =
                bottleneckAnalyzer.analyzeBottlenecks(simulationId);
        String bottleneckSummary = bottleneckReport.getMessage();

        return new SummaryReport(
                simulationId,
                LocalDateTime.now(),
                summary.getTotalTasksCompleted(),
                summary.getTasksPerHour(),
                summary.getMaterialThroughput(),
                summary.getVehicleUtilization(),
                summary.getEquipmentUtilization(),
                summary.getWipTotal(),
                energySummary.getTotalEnergy(),
                avgCompletionTime,
                bottleneckSummary
        );
    }

    /**
     * Generate dashboard snapshot for real-time display.
     * <p>
     * Provides current KPI snapshot + historical trend data.
     * Output structure is stable with fixed field names for frontend consumption.
     *
     * @param simulationId the simulation ID
     * @return the dashboard snapshot
     */
    public DashboardSnapshot generateDashboardSnapshot(String simulationId) {
        // Get all aggregates for trend data
        List<MetricAggregate> aggregates = repository.queryBySimulationIdAndTimeRange(
                simulationId,
                0.0,
                Double.MAX_VALUE
        );

        if (aggregates.isEmpty()) {
            return DashboardSnapshot.empty(simulationId);
        }

        // Get latest aggregate for current KPI
        MetricAggregate latest = aggregates.get(aggregates.size() - 1);

        // Build trend data
        List<KpiMetrics> trendData = buildTrendData(aggregates);

        // Calculate average completion time
        double avgCompletionTime = calculateAverageCompletionTime(simulationId);

        // Build current KPI
        KpiMetrics currentKpi = new KpiMetrics(
                simulationId,
                latest.getRecordedAt(),
                latest.getWallClockTime(),
                latest.getTasksCompleted(),
                latest.getTasksPerHour(),
                latest.getMaterialThroughput(),
                latest.getVehicleUtilization(),
                latest.getEquipmentUtilization(),
                latest.getWipTotal(),
                latest.getEnergyTotal(),
                avgCompletionTime
        );

        return new DashboardSnapshot(
                simulationId,
                currentKpi,
                trendData,
                LocalDateTime.now()
        );
    }

    /**
     * Calculate average completion time from task events.
     *
     * @param simulationId the simulation ID
     * @return average completion time in seconds (simulated time)
     */
    private double calculateAverageCompletionTime(String simulationId) {
        List<MetricEvent> completedEvents = repository.queryByEventType(
                MetricEvent.EventTypes.TASK_COMPLETED
        );

        double totalTime = 0.0;
        int count = 0;

        // For simplicity, use wall clock time difference if available
        // In production, this would use simulated_time from task events
        for (MetricEvent event : completedEvents) {
            if (!simulationId.equals(event.getSimulationId())) {
                continue;
            }

            // If event contains completion time data, use it
            if (event.getData() != null) {
                double completionTime = event.getData().getDouble("completionTime", 0.0);
                if (completionTime > 0) {
                    totalTime += completionTime;
                    count++;
                }
            }
        }

        return count > 0 ? totalTime / count : 0.0;
    }

    /**
     * Build trend data from aggregates.
     *
     * @param aggregates the metric aggregates
     * @return list of KPI metrics for trend chart
     */
    private List<KpiMetrics> buildTrendData(List<MetricAggregate> aggregates) {
        List<KpiMetrics> trendData = new ArrayList<>();
        String simulationId = aggregates.isEmpty() ? "" : aggregates.get(0).getSimulationId();

        for (MetricAggregate aggregate : aggregates) {
            KpiMetrics kpi = new KpiMetrics(
                    simulationId,
                    aggregate.getRecordedAt(),
                    aggregate.getWallClockTime(),
                    aggregate.getTasksCompleted(),
                    aggregate.getTasksPerHour(),
                    aggregate.getMaterialThroughput(),
                    aggregate.getVehicleUtilization(),
                    aggregate.getEquipmentUtilization(),
                    aggregate.getWipTotal(),
                    aggregate.getEnergyTotal(),
                    0.0  // Average completion time not tracked per period
            );
            trendData.add(kpi);
        }

        return trendData;
    }
}
