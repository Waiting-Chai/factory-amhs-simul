package com.semi.simlogistics.scheduler.metrics.scenario;

import com.semi.simlogistics.scheduler.metrics.analysis.BottleneckAnalyzer;
import com.semi.simlogistics.scheduler.metrics.analysis.VehicleCountEvaluator;
import com.semi.simlogistics.scheduler.metrics.calculator.EnergyCalculator;
import com.semi.simlogistics.scheduler.metrics.calculator.ThroughputCalculator;
import com.semi.simlogistics.scheduler.metrics.collector.MetricsCollector;
import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryConfig;
import com.semi.simlogistics.scheduler.metrics.port.InMemoryMetricsRepository;
import com.semi.simlogistics.scheduler.metrics.port.MetricsRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Phase 4 KPI metrics (REQ-KPI-001 to REQ-KPI-006).
 * <p>
 * Tests the complete metrics collection, calculation, and analysis pipeline.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@DisplayName("Phase 4 KPI Integration Tests")
class Phase4KPIIntegrationTest {

    private static final String SIMULATION_ID = "phase4-test-simulation";
    private InMemoryMetricsRepository repository;
    private InMemoryConfig config;
    private MetricsCollector collector;
    private ThroughputCalculator throughputCalculator;
    private EnergyCalculator energyCalculator;
    private BottleneckAnalyzer bottleneckAnalyzer;
    private VehicleCountEvaluator vehicleCountEvaluator;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMetricsRepository();
        config = new InMemoryConfig();
        collector = new MetricsCollector(SIMULATION_ID, repository, config);
        throughputCalculator = new ThroughputCalculator(repository);
        energyCalculator = new EnergyCalculator(repository);
        bottleneckAnalyzer = new BottleneckAnalyzer(repository, config);
        vehicleCountEvaluator = new VehicleCountEvaluator(repository, config);
    }

    @Test
    @DisplayName("Should collect and calculate metrics for complete simulation cycle")
    void shouldCollectAndCalculateMetricsForCompleteSimulationCycle() {
        // Given: A simulation scenario with vehicles and tasks
        // Scenario: 5 vehicles, 50 tasks over 300 simulated seconds

        int numVehicles = 5;
        int numTasks = 50;
        double simulationDuration = 300.0;  // 5 minutes

        // Simulate vehicle states and task execution
        simulateScenario(numVehicles, numTasks, simulationDuration);

        // Force final aggregation
        collector.forceAggregation(simulationDuration);

        // When: Calculate throughput
        var throughputSummary = throughputCalculator.calculateThroughput(SIMULATION_ID);

        // Then: Throughput should be calculated correctly
        assertThat(throughputSummary.getTotalTasksCompleted()).isEqualTo(numTasks);
        // Tasks per hour = 50 / 60s (aggregation interval) * 3600 = 3000
        assertThat(throughputSummary.getTasksPerHour()).isEqualTo(3000.0);

        // When: Calculate energy
        var energySummary = energyCalculator.calculateEnergyConsumption(SIMULATION_ID);

        // Then: Energy should be calculated
        assertThat(energySummary.getTotalEnergy()).isGreaterThan(0);
        assertThat(energySummary.getEnergyPerTask()).isGreaterThan(0);

        // When: Analyze bottlenecks
        var bottleneckReport = bottleneckAnalyzer.analyzeBottlenecks(SIMULATION_ID);

        // Then: Should produce report
        assertThat(bottleneckReport).isNotNull();
        assertThat(bottleneckReport.getAllBottlenecks()).isNotNull();
    }

    @Test
    @DisplayName("Should detect high utilization bottleneck")
    void shouldDetectHighUtilizationBottleneck() {
        // Given: High utilization scenario (vehicles working 95% of time)
        simulateHighUtilizationScenario();

        // When: Analyze bottlenecks
        var report = bottleneckAnalyzer.analyzeBottlenecks(SIMULATION_ID);

        // Then: Should detect critical utilization bottleneck
        assertThat(report.hasCriticalBottlenecks()).isTrue();
        assertThat(report.getUtilizationBottlenecks()).anyMatch(item ->
                item.getSeverity() == BottleneckAnalyzer.BottleneckSeverity.CRITICAL
        );
    }

    @Test
    @DisplayName("Should evaluate minimum vehicle count")
    void shouldEvaluateMinimumVehicleCount() {
        // Given: Current simulation with known throughput
        // 10 vehicles achieve 100 tasks/hour
        double targetTasksPerHour = 100.0;
        int currentVehicleCount = 10;
        double currentThroughput = targetTasksPerHour;

        createSimulationWithThroughput(currentThroughput, currentVehicleCount);

        VehicleCountEvaluator.ThroughputTarget target =
                new VehicleCountEvaluator.ThroughputTarget(targetTasksPerHour, 0.05);

        // When: Find minimum vehicles for 50% target
        VehicleCountEvaluator.EvaluationReport report = vehicleCountEvaluator.findMinimumVehicles(
                SIMULATION_ID,
                currentVehicleCount,
                target
        );

        // Then: Should find optimal count
        assertThat(report.getOptimalVehicleCount()).isGreaterThan(0);
        assertThat(report.getOptimalVehicleCount()).isLessThanOrEqualTo(currentVehicleCount);
    }

    @Test
    @DisplayName("Should aggregate metrics at 60 second intervals")
    void shouldAggregateMetricsAt60SecondIntervals() {
        // Given: Collector with default 60 second interval

        // Record tasks at different times
        for (int i = 0; i < 10; i++) {
            collector.recordTaskCompletion("TASK-" + i, "TRANSPORT", "V-001", i * 10.0);
        }

        // When: Trigger aggregation at various times
        MetricAggregate agg1 = collector.triggerAggregation(59.0);   // Before interval
        MetricAggregate agg2 = collector.triggerAggregation(60.0);   // At interval
        MetricAggregate agg3 = collector.triggerAggregation(119.0);  // Before next interval
        MetricAggregate agg4 = collector.triggerAggregation(120.0);  // At next interval

        // Then: Should only aggregate at 60s and 120s
        assertThat(agg1).isNull();
        assertThat(agg2).isNotNull();
        assertThat(agg3).isNull();
        assertThat(agg4).isNotNull();

        // And: Aggregates should be saved
        assertThat(repository.getAggregatesBySimulationId(SIMULATION_ID)).hasSize(2);
    }

    @Test
    @DisplayName("Should use dual time基准 in events and aggregates")
    void shouldUseDualTimeBasisInEventsAndAggregates() {
        // When: Record event and create aggregate
        double simulatedTime = 123.456;
        collector.recordTaskCompletion("TASK-001", "TRANSPORT", "V-001", simulatedTime);
        collector.forceAggregation(simulatedTime);

        // Then: Event should have both times
        var events = repository.getEventsBySimulationId(SIMULATION_ID);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSimulatedTime()).isEqualTo(123.456);
        assertThat(events.get(0).getWallClockTime()).isNotNull();

        // And: Aggregate should have both times
        var aggregates = repository.queryBySimulationIdAndTimeRange(SIMULATION_ID, 0, Double.MAX_VALUE);
        assertThat(aggregates).hasSize(1);
        assertThat(aggregates.get(0).getRecordedAt()).isEqualTo(123.456);
        assertThat(aggregates.get(0).getWallClockTime()).isNotNull();
    }

    @Test
    @DisplayName("Should calculate time-weighted utilization correctly")
    void shouldCalculateTimeWeightedUtilizationCorrectly() {
        // Given: Vehicle state sequence
        // Tracking starts at first state change (10.0s)
        // MOVING[10-30s] + LOADING[30-35s] + MOVING[35-55s] + IDLE[55-60s]
        // Working time = 20 + 5 + 20 = 45s, Total tracking = 50s
        // Utilization = 45/50 = 0.9

        collector.recordVehicleStateChange("V-001", "IDLE", "MOVING", 10.0);
        collector.recordVehicleStateChange("V-001", "MOVING", "LOADING", 30.0);
        collector.recordVehicleStateChange("V-001", "LOADING", "MOVING", 35.0);
        collector.recordVehicleStateChange("V-001", "MOVING", "IDLE", 55.0);

        // When: Calculate utilization at 60s
        double utilization = collector.calculateAverageUtilization(60.0);

        // Then: Should be 0.9
        assertThat(utilization).isCloseTo(0.9, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should handle WIP accumulation detection")
    void shouldHandleWIPAccumulationDetection() {
        // Given: High WIP level (200 vs warning threshold of 100)
        collector.updateWIP(200);
        collector.forceAggregation(60.0);

        // When: Analyze bottlenecks
        var report = bottleneckAnalyzer.analyzeBottlenecks(SIMULATION_ID);

        // Then: Should detect WIP bottleneck
        assertThat(report.getWipBottlenecks()).anyMatch(item ->
                item.getId().equals("WIP_GLOBAL")
        );
    }

    // Helper methods

    private void simulateScenario(int numVehicles, int numTasks, double duration) {
        // Simulate task execution
        double timePerTask = duration / numTasks;
        String[] vehicles = new String[numVehicles];
        for (int i = 0; i < numVehicles; i++) {
            vehicles[i] = "V-" + String.format("%03d", i + 1);
            collector.recordVehicleStateChange(vehicles[i], "IDLE", "MOVING", 0.0);
        }

        // Execute tasks
        for (int i = 0; i < numTasks; i++) {
            String vehicleId = vehicles[i % numVehicles];
            double taskTime = i * timePerTask;

            // Start task
            collector.recordVehicleStateChange(vehicleId, "MOVING", "LOADING", taskTime);

            // Complete task
            double completionTime = taskTime + timePerTask * 0.8;
            collector.recordTaskCompletion("TASK-" + i, "TRANSPORT", vehicleId, completionTime);
            collector.recordVehicleMovement(vehicleId, 50.0, taskTime);
            collector.recordEnergyConsumption(vehicleId, "VEHICLE", 500.0, taskTime);

            // Return to idle
            collector.recordVehicleStateChange(vehicleId, "LOADING", "MOVING", completionTime);
            collector.recordVehicleStateChange(vehicleId, "MOVING", "IDLE", completionTime + timePerTask * 0.1);
        }

        collector.updateWIP(numTasks / 4);  // 25% of tasks still in progress
    }

    private void simulateHighUtilizationScenario() {
        // Vehicle works 95% of the time
        collector.recordVehicleStateChange("V-001", "IDLE", "MOVING", 0.0);
        collector.recordVehicleStateChange("V-001", "MOVING", "LOADING", 30.0);
        collector.recordVehicleStateChange("V-001", "LOADING", "MOVING", 35.0);
        // Keep moving until 57 (out of 60 seconds)
        collector.recordVehicleStateChange("V-001", "MOVING", "IDLE", 57.0);

        // Complete some tasks
        for (int i = 0; i < 10; i++) {
            collector.recordTaskCompletion("TASK-" + i, "TRANSPORT", "V-001", i * 5.0);
        }

        collector.forceAggregation(60.0);
    }

    private void createSimulationWithThroughput(double tasksPerHour, int numVehicles) {
        MetricAggregate aggregate = new MetricAggregate.Builder(SIMULATION_ID, 3600.0)
                .tasksCompleted((int) tasksPerHour)
                .tasksPerHour(tasksPerHour)
                .materialThroughput(tasksPerHour * 5.0)
                .vehicleUtilization(0.7)
                .equipmentUtilization(0.6)
                .wipTotal(50)
                .energyTotal(tasksPerHour * 100)
                .build();

        repository.saveAggregate(aggregate);
    }
}
