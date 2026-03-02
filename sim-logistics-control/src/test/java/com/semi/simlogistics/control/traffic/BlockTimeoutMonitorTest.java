package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.control.path.Path;
import com.semi.simlogistics.control.path.PathPlanner;
import com.semi.simlogistics.vehicle.AGVVehicle;
import com.semi.simlogistics.vehicle.OHTVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BlockTimeoutMonitor.
 * <p>
 * Tests cover blocking timeout replan mechanism per REQ-TC-007:
 * 1. Timeout triggers replan (60s simulation time default)
 * 2. No timeout when within threshold
 * 3. Max replan attempts limit (3 default)
 * 4. Config override works
 * 5. Edge cases: no PathPlanner, no available path
 * 6. Real replan: path actually updated on vehicle (not just "called")
 * 7. TransportType correctly passed (OHT/AGV with real AGVVehicle)
 * 8. TrafficManager real integration test: full chain via requestPassage()
 * Integration acceptance rule: trigger must go through TrafficManager.requestPassage(...),
 * and assertions must not call BlockTimeoutMonitor.executeReplan(...) directly.
 * <p>
 * All tests follow Given/When/Then structure.
 *
 * @author shentw
 * @version 4.0
 * @since 2026-02-09
 */
class BlockTimeoutMonitorTest {

    private BlockTimeoutMonitor monitor;
    private TestPathPlanner pathPlanner;
    private OHTVehicle vehicleOHT;
    private AGVVehicle vehicleAGV;
    private Position position;
    private Map<String, Double> waitStartTime;
    private double currentTime;

    @BeforeEach
    void setUp() {
        // Given: A BlockTimeoutMonitor with default config (timeout=60s, maxAttempts=3)
        pathPlanner = new TestPathPlanner();
        monitor = new BlockTimeoutMonitor(pathPlanner, 60.0, 3);

        // And: Test vehicles
        position = new Position(0.0, 0.0, 0.0);
        vehicleOHT = new OHTVehicle("V-OHT", "OHT Vehicle", position, 10.0);
        // Create real AGV vehicle with battery parameters
        vehicleAGV = new AGVVehicle("V-AGV", "AGV Vehicle", position, 5.0,
                0.01, 0.5, 1.0, 0.2); // maxSpeed, consumptionRate, chargingRate, initialBattery, lowBatteryThreshold

        // And: Wait start time tracking (simulating ControlPoint's waitStartTime)
        waitStartTime = new HashMap<>();
        currentTime = 1000.0;
    }

    // ==================== Scenario 1: Timeout Triggers Replan ====================

    @Test
    void testTimeoutTriggersReplan_ExceedsThreshold() {
        // Given: Vehicle waiting for 70 seconds (exceeds 60s threshold)
        waitStartTime.put("V-OHT", currentTime - 70.0);

        // When: Check timeout
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should trigger replan
        assertThat(shouldReplan).isTrue();
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(1);
    }

    @Test
    void testNoTimeout_WithinThreshold() {
        // Given: Vehicle waiting for 30 seconds (within 60s threshold)
        waitStartTime.put("V-OHT", currentTime - 30.0);

        // When: Check timeout
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should NOT trigger replan
        assertThat(shouldReplan).isFalse();
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(0);
    }

    @Test
    void testNoTimeout_ExactlyAtThreshold() {
        // Given: Vehicle waiting for exactly 60 seconds
        waitStartTime.put("V-OHT", currentTime - 60.0);

        // When: Check timeout
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should NOT trigger replan (must EXCEED threshold)
        assertThat(shouldReplan).isFalse();
    }

    // ==================== Scenario 2: Max Replan Attempts ====================

    @Test
    void testMaxAttempts_StopsReplanning() {
        // Given: Vehicle has already triggered 3 replan attempts (max)
        waitStartTime.put("V-OHT", currentTime - 70.0);
        for (int i = 0; i < 3; i++) {
            monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);
        }
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(3);

        // When: Check timeout again (still waiting, still over threshold)
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should NOT trigger replan (max attempts reached)
        assertThat(shouldReplan).isFalse();
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(3); // Remains at 3
    }

    @Test
    void testReplanAttempts_IncrementsOnTrigger() {
        // Given: Vehicle waiting over threshold
        waitStartTime.put("V-OHT", currentTime - 70.0);

        // When: First timeout check
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should trigger and counter should be 1
        assertThat(shouldReplan).isTrue();
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(1);

        // When: Second timeout check (still waiting)
        shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should trigger and counter should be 2
        assertThat(shouldReplan).isTrue();
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(2);
    }

    // ==================== Scenario 3: Configuration Override ====================

    @Test
    void testConfigOverride_CustomTimeoutWorks() {
        // Given: BlockTimeoutMonitor with custom timeout (30s instead of 60s)
        BlockTimeoutMonitor customMonitor = new BlockTimeoutMonitor(pathPlanner, 30.0, 3);

        // And: Vehicle waiting for 35 seconds (exceeds 30s, but not 60s)
        waitStartTime.put("V-OHT", currentTime - 35.0);

        // When: Check timeout with custom monitor
        boolean shouldReplan = customMonitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should trigger replan (using custom 30s threshold)
        assertThat(shouldReplan).isTrue();
        assertThat(customMonitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(1);

        // And: Default monitor should NOT trigger (35s < 60s)
        boolean defaultMonitorTrigger = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);
        assertThat(defaultMonitorTrigger).isFalse();
    }

    @Test
    void testConfigOverride_CustomMaxAttemptsWorks() {
        // Given: BlockTimeoutMonitor with custom maxAttempts (1 instead of 3)
        BlockTimeoutMonitor customMonitor = new BlockTimeoutMonitor(pathPlanner, 60.0, 1);

        // And: Vehicle waiting over threshold
        waitStartTime.put("V-OHT", currentTime - 70.0);

        // When: First timeout check triggers replan
        boolean shouldReplan1 = customMonitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);
        assertThat(shouldReplan1).isTrue();
        assertThat(customMonitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(1);

        // And: Second timeout check (max attempts = 1)
        boolean shouldReplan2 = customMonitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should NOT trigger replan (max attempts reached)
        assertThat(shouldReplan2).isFalse();
    }

    // ==================== Scenario 4: Simulation Time Usage ====================

    @Test
    void testSimulationTime_NotWallClockTime() {
        // Given: Vehicle with wait start time at 1000.0 (simulation time)
        waitStartTime.put("V-OHT", 1000.0);

        // And: Current simulation time is 1100.0 (100 seconds waited)
        currentTime = 1100.0;

        // When: Check timeout
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should use simulation time (100s > 60s threshold)
        assertThat(shouldReplan).isTrue();
    }

    // ==================== Scenario 5: Real Replan with Path Update ====================

    @Test
    void testExecuteReplan_UpdatesVehiclePath() {
        // Given: Vehicle with original path
        vehicleOHT.setPath("OLD_PATH");
        assertThat(vehicleOHT.getPath()).isEqualTo("OLD_PATH");

        // When: Execute replan
        boolean replanned = monitor.executeReplan(vehicleOHT, "NODE_A", "NODE_B");

        // Then: Path should be updated
        assertThat(replanned).isTrue();
        assertThat(vehicleOHT.getPath()).isNotEqualTo("OLD_PATH");
        assertThat(vehicleOHT.getPath()).contains("NODE_A");
        assertThat(vehicleOHT.getPath()).contains("NODE_B");
    }

    @Test
    void testExecuteReplan_CallsPathPlannerWithCorrectTransportType_OHT() {
        // Given: OHT vehicle
        waitStartTime.put("V-OHT", currentTime - 70.0);

        // When: Execute replan
        boolean replanned = monitor.executeReplan(vehicleOHT, "NODE_A", "NODE_B");

        // Then: PathPlanner should be called with OHT transport type
        assertThat(replanned).isTrue();
        assertThat(pathPlanner.getLastTransportType()).isEqualTo(TransportType.OHT);
    }

    @Test
    void testExecuteReplan_CallsPathPlannerWithCorrectTransportType_AGV() {
        // Given: AGV vehicle (real AGVVehicle instance)
        waitStartTime.put("V-AGV", currentTime - 70.0);

        // When: Execute replan
        boolean replanned = monitor.executeReplan(vehicleAGV, "NODE_A", "NODE_B");

        // Then: PathPlanner should be called with AGV transport type
        assertThat(replanned).isTrue();
        assertThat(pathPlanner.getLastTransportType()).isEqualTo(TransportType.AGV);
    }

    @Test
    void testExecuteReplan_MultipleVehiclesIndependent() {
        // Given: Two vehicles waiting over threshold
        waitStartTime.put("V-OHT", currentTime - 70.0);
        waitStartTime.put("V-AGV", currentTime - 80.0);

        // When: Vehicle1 triggers replan
        boolean v1Triggered = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);
        assertThat(v1Triggered).isTrue();

        // When: Vehicle2 triggers replan
        boolean v2Triggered = monitor.shouldTriggerReplan(vehicleAGV, waitStartTime, currentTime);
        assertThat(v2Triggered).isTrue();

        // Then: Counters should be independent
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(1);
        assertThat(monitor.getReplanAttempts(vehicleAGV.id())).isEqualTo(1);
    }

    // ==================== Scenario 6: Edge Cases ====================

    @Test
    void testEdgeCase_MissingWaitStartTime() {
        // Given: Vehicle NOT in wait queue (no wait start time)

        // When: Check timeout
        boolean shouldReplan = monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);

        // Then: Should handle gracefully (no exception, no replan)
        assertThat(shouldReplan).isFalse();
    }

    @Test
    void testEdgeCase_ReplanCounterResetsOnSuccess() {
        // Given: Vehicle with 2 replan attempts
        waitStartTime.put("V-OHT", currentTime - 70.0);
        monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);
        monitor.shouldTriggerReplan(vehicleOHT, waitStartTime, currentTime);
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(2);

        // When: Vehicle successfully passes through
        monitor.onVehiclePassed(vehicleOHT.id());

        // Then: Replan counter should reset
        assertThat(monitor.getReplanAttempts(vehicleOHT.id())).isEqualTo(0);
    }

    @Test
    void testEdgeCase_ZeroTimeoutUsesDefault() {
        // Given: BlockTimeoutMonitor with timeout=0 (use default)
        BlockTimeoutMonitor zeroMonitor = new BlockTimeoutMonitor(pathPlanner, 0.0, 3);

        // Then: Should use default timeout (60s)
        assertThat(zeroMonitor.getTimeoutSeconds()).isEqualTo(60.0);
    }

    @Test
    void testEdgeCase_NegativeMaxAttemptsUsesDefault() {
        // Given: BlockTimeoutMonitor with negative maxAttempts
        BlockTimeoutMonitor negativeMonitor = new BlockTimeoutMonitor(pathPlanner, 60.0, -1);

        // Then: Should use default maxAttempts (3)
        assertThat(negativeMonitor.getMaxAttempts()).isEqualTo(3);
    }

    // ==================== Scenario 7: PathPlanner Edge Cases ====================

    @Test
    void testEdgeCase_NullPathPlannerReturnsFalse() {
        // Given: BlockTimeoutMonitor with null PathPlanner
        BlockTimeoutMonitor nullPlannerMonitor = new BlockTimeoutMonitor(null, 60.0, 3);

        // And: Vehicle waiting over threshold
        waitStartTime.put("V-OHT", currentTime - 70.0);

        // When: Try to execute replan
        boolean replanned = nullPlannerMonitor.executeReplan(vehicleOHT, "START", "END");

        // Then: Should return false (no PathPlanner to call)
        assertThat(replanned).isFalse();
        assertThat(pathPlanner.getCallCount()).isEqualTo(0); // Original planner not called
    }

    @Test
    void testEdgeCase_EmptyPathReturnsFalse() {
        // Given: PathPlanner that returns empty path
        TestPathPlanner emptyPathPlanner = new TestPathPlanner();
        emptyPathPlanner.setReturnEmptyPath(true);
        BlockTimeoutMonitor emptyMonitor = new BlockTimeoutMonitor(emptyPathPlanner, 60.0, 3);

        // And: Vehicle with original path
        vehicleOHT.setPath("ORIGINAL_PATH");
        waitStartTime.put("V-OHT", currentTime - 70.0);

        // When: Try to execute replan
        boolean replanned = emptyMonitor.executeReplan(vehicleOHT, "START", "END");

        // Then: Should return false and path should remain unchanged
        assertThat(replanned).isFalse();
        assertThat(vehicleOHT.getPath()).isEqualTo("ORIGINAL_PATH"); // Path unchanged
    }

    @Test
    void testConfiguration_GettersReturnCorrectValues() {
        // Given: BlockTimeoutMonitor with custom config
        BlockTimeoutMonitor customMonitor = new BlockTimeoutMonitor(pathPlanner, 45.0, 5);

        // When: Get configuration
        double timeout = customMonitor.getTimeoutSeconds();
        int maxAttempts = customMonitor.getMaxAttempts();

        // Then: Should return custom values
        assertThat(timeout).isEqualTo(45.0);
        assertThat(maxAttempts).isEqualTo(5);
    }

    @Test
    void testGetPathPlanner() {
        // Given: Monitor with path planner
        // When: Get path planner
        // Then: Should return the planner
        assertThat(monitor.getPathPlanner()).isSameAs(pathPlanner);
    }

    // ==================== Test Helpers ====================

    /**
     * Test PathPlanner implementation for testing.
     */
    private static class TestPathPlanner implements PathPlanner {
        private int callCount = 0;
        private boolean returnEmptyPath = false;
        private TransportType lastTransportType;

        @Override
        public Path planPath(String startNodeId, String endNodeId, TransportType transportType) {
            callCount++;
            this.lastTransportType = transportType;
            if (returnEmptyPath) {
                return new Path(); // Empty path
            }
            // Return a valid path with start and end nodes
            List<String> nodes = new ArrayList<>();
            nodes.add(startNodeId);
            nodes.add("INTERMEDIATE");
            nodes.add(endNodeId);
            return new Path(nodes, 10.0);
        }

        @Override
        public void onGraphChanged() {
            // No-op for test
        }

        @Override
        public int getCacheSize() {
            return 0; // No caching in test implementation
        }

        public int getCallCount() {
            return callCount;
        }

        public void setReturnEmptyPath(boolean returnEmpty) {
            this.returnEmptyPath = returnEmpty;
        }

        public TransportType getLastTransportType() {
            return lastTransportType;
        }
    }

    // ==================== Scenario 8: TrafficManager Real Integration ====================
    // Integration acceptance constraint:
    // Replan in assertions must be triggered by TrafficManager.requestPassage(...),
    // not by directly invoking BlockTimeoutMonitor.executeReplan(...).

    @Test
    void testRealIntegration_FullPathWithVehicleNodeIds() {
        // Given: TrafficManager with BlockTimeoutMonitor and ControlPoint
        TrafficManager tm = new TrafficManager();
        tm.setPathPlanner(pathPlanner);

        ControlPoint cp = new ControlPoint("CP1", "NODE_A", 1); // Capacity 1
        tm.registerControlPoint(cp);

        // And: First vehicle occupies the control point
        OHTVehicle vehicle1 = new OHTVehicle("V1", "Vehicle 1", position, 10.0);
        vehicle1.setCurrentNodeId("NODE_A");
        vehicle1.setDestinationNodeId("NODE_D");
        boolean granted1 = tm.requestPassage(vehicle1, "CP1", 1000.0);
        assertThat(granted1).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);

        // And: Second vehicle requests passage (will be blocked)
        OHTVehicle vehicle2 = new OHTVehicle("V2", "Vehicle 2", position, 10.0);
        vehicle2.setCurrentNodeId("NODE_B");
        vehicle2.setDestinationNodeId("NODE_C");
        vehicle2.setPath("INITIAL_PATH");
        boolean granted2 = tm.requestPassage(vehicle2, "CP1", 1000.0);
        assertThat(granted2).isFalse(); // Blocked (capacity full)

        // When: Vehicle2 has been waiting over timeout threshold (70 seconds)
        // This simulates the real blocking scenario where vehicle2 is stuck
        boolean grantedAfterTimeout = tm.requestPassage(vehicle2, "CP1", 1070.0);

        // Then: Path should have been updated (replan occurred via real path)
        assertThat(vehicle2.getPath()).isNotEqualTo("INITIAL_PATH");
        assertThat(vehicle2.getPath()).contains("NODE_B");
        assertThat(vehicle2.getPath()).contains("NODE_C");

        // And: Path planner was called with vehicle's transport type
        assertThat(pathPlanner.getCallCount()).isGreaterThan(0);
        assertThat(pathPlanner.getLastTransportType()).isEqualTo(TransportType.OHT);
    }

    @Test
    void testRealIntegration_AGVVehicleTransportType() {
        // Given: TrafficManager with BlockTimeoutMonitor
        TrafficManager tm = new TrafficManager();
        tm.setPathPlanner(pathPlanner);

        ControlPoint cp = new ControlPoint("CP1", "NODE_A", 1);
        tm.registerControlPoint(cp);

        // And: AGV vehicle with node IDs
        AGVVehicle agv = new AGVVehicle("AGV1", "AGV 1", position, 5.0,
                0.01, 0.5, 1.0, 0.2);
        agv.setCurrentNodeId("NODE_X");
        agv.setDestinationNodeId("NODE_Y");
        agv.setPath("AGV_INITIAL_PATH");

        // And: Control point is occupied
        OHTVehicle blocker = new OHTVehicle("BLOCKER", "Blocker", position, 10.0);
        blocker.setCurrentNodeId("NODE_Z");
        blocker.setDestinationNodeId("NODE_W");
        tm.requestPassage(blocker, "CP1", 1000.0);

        // When: AGV requests passage and gets blocked, then timeout occurs
        boolean granted = tm.requestPassage(agv, "CP1", 1000.0);
        assertThat(granted).isFalse();

        boolean grantedAfterTimeout = tm.requestPassage(agv, "CP1", 1080.0); // 80 seconds wait

        // Then: Path should be updated with AGV transport type
        assertThat(agv.getPath()).isNotEqualTo("AGV_INITIAL_PATH");
        assertThat(pathPlanner.getLastTransportType()).isEqualTo(TransportType.AGV);
    }

    @Test
    void testSystemConfigProvider_ConfigOverrideWorks() {
        // Given: SystemConfigProvider with custom config
        SystemConfigProvider configProvider = SystemConfigProvider.getInstance();
        configProvider.setConfig("traffic.replan.timeoutSeconds", "30.0");
        configProvider.setConfig("traffic.replan.maxAttempts", "5");

        // When: Create BlockTimeoutMonitor (uses SystemConfigProvider defaults)
        BlockTimeoutMonitor configuredMonitor = new BlockTimeoutMonitor(pathPlanner);

        // Then: Should use configured values
        assertThat(configuredMonitor.getTimeoutSeconds()).isEqualTo(30.0);
        assertThat(configuredMonitor.getMaxAttempts()).isEqualTo(5);

        // Clean up: reset to defaults
        configProvider.resetToDefaults();
    }

    @Test
    void testSystemConfigProvider_InvalidValuesFallbackToDefault() {
        // Given: SystemConfigProvider with invalid config values
        SystemConfigProvider configProvider = SystemConfigProvider.getInstance();
        configProvider.setConfig("traffic.replan.timeoutSeconds", "-10"); // Invalid: negative
        configProvider.setConfig("traffic.replan.maxAttempts", "0");    // Invalid: zero

        // When: Get config values
        double timeout = configProvider.getReplanTimeoutSeconds();
        int maxAttempts = configProvider.getReplanMaxAttempts();

        // Then: Should fallback to defaults
        assertThat(timeout).isEqualTo(60.0);  // Default
        assertThat(maxAttempts).isEqualTo(3); // Default

        // Clean up
        configProvider.resetToDefaults();
    }
}
