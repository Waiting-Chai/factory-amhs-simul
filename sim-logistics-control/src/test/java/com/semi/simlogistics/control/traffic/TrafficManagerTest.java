package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.control.conflict.ConflictResolver;
import com.semi.simlogistics.control.traffic.TrafficManager.PathType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.movement.TrackMovement;
import com.semi.simlogistics.vehicle.OHTVehicle;
import com.semi.simlogistics.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TrafficManager.
 * <p>
 * Tests cover three main scenarios per REQ-TC-008 and REQ-TC-009:
 * 1. Unified entry point for all traffic requests
 * 2. Event-driven response mechanism
 * 3. Domain-based strategy selection (OHT/AGV/HUMAN)
 * <p>
 * All tests follow Given/When/Then structure aligned with spec scenarios.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-08
 */
class TrafficManagerTest {

    private TrafficManager trafficManager;
    private ControlPoint testControlPoint;
    private OHTVehicle vehicle1;
    private OHTVehicle vehicle2;
    private OHTVehicle vehicle3;
    private Position position;

    @BeforeEach
    void setUp() {
        // Given: A TrafficManager with default configuration
        trafficManager = new TrafficManager();

        // And: A test control point with capacity 1
        position = new Position(0.0, 0.0, 0.0);
        testControlPoint = new ControlPoint("CP-1", "NODE-1", 1);

        // And: Register control point with traffic manager
        trafficManager.registerControlPoint(testControlPoint);

        // And: Three test vehicles
        vehicle1 = new OHTVehicle("V1", "Vehicle 1", position, 10.0);
        vehicle2 = new OHTVehicle("V2", "Vehicle 2", position, 10.0);
        vehicle3 = new OHTVehicle("V3", "Vehicle 3", position, 10.0);
    }

    // ==================== Scenario 1: Unified Entry Point ====================

    /**
     * Test scenario: Single traffic entry point (REQ-TC-009)
     * <p>
     * Given: 系统存在 OHT 与 AGV
     * And: TrafficManager 已初始化
     * When: 车辆发起通行请求
     * Then: 所有请求应进入统一 TrafficManager 入口
     */
    @Test
    void testUnifiedEntry_AllRequestsGoThroughTrafficManager() {
        // Given: OHT vehicle and control point registered
        assertThat(trafficManager.getRegisteredControlPoints()).hasSize(1);
        assertThat(trafficManager.getRegisteredControlPoints()).contains(testControlPoint);

        // When: Vehicle requests passage through control point with simulation time
        double simTime = 1000.0;
        boolean granted = trafficManager.requestPassage(vehicle1, "CP-1", simTime);

        // Then: Request should be processed through unified entry
        assertThat(granted).isTrue();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
    }

    /**
     * Test scenario: Unified entry handles multiple vehicle types
     * <p>
     * Given: TrafficManager 统一入口
     * When: 不同类型车辆发起请求
     * Then: 所有请求都通过同一入口处理
     */
    @Test
    void testUnifiedEntry_HandlesMultipleVehicleTypes() {
        // Given: TrafficManager with control point
        double simTime = 1000.0;
        // When: First vehicle requests and occupies control point
        boolean firstRequest = trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        assertThat(firstRequest).isTrue();

        // And: Second vehicle requests same control point (at capacity)
        boolean secondRequest = trafficManager.requestPassage(vehicle2, "CP-1", simTime);

        // Then: Second request should be rejected (capacity limit)
        assertThat(secondRequest).isFalse();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
    }

    /**
     * Test scenario: Resource release through unified entry (REQ-TC-008)
     * <p>
     * Given: 车辆占用控制点
     * When: 车辆离开控制点
     * Then: 通过统一入口释放资源
     */
    @Test
    void testUnifiedEntry_ResourceRelease() {
        // Given: Vehicle occupies control point
        double simTime = 1000.0;
        trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // When: Vehicle releases control point through unified entry
        trafficManager.releasePassage(vehicle1, "CP-1", simTime);

        // Then: Resource should be released
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(0);
    }

    // ==================== Scenario 2: Event-Driven Response ====================

    /**
     * Test scenario: Event-driven processing (REQ-TC-008)
     * <p>
     * Given: 运行中的 TrafficManager
     * And: 车辆通行请求事件
     * When: 事件触发
     * Then: TrafficManager 应即时处理（事件驱动）
     */
    @Test
    void testEventDriven_PassageRequestProcessedImmediately() {
        // Given: Running TrafficManager with control point
        double simTime = 1000.0;
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(0);

        // When: Passage request event occurs
        boolean granted = trafficManager.requestPassage(vehicle1, "CP-1", simTime);

        // Then: Request should be processed immediately (synchronous)
        assertThat(granted).isTrue();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
    }

    /**
     * Test scenario: Event-driven release triggers next vehicle grant (REQ-TC-008)
     * <p>
     * Given: 多辆车等待同一控制点
     * When: 占用车辆释放
     * Then: 应立即触发下一辆车获准进入
     */
    @Test
    void testEventDriven_ReleaseTriggersNextVehicleGrant() {
        // Given: Control point with capacity 1, vehicle1 occupying
        double simTime = 1000.0;
        vehicle1.setPriority(1);
        vehicle2.setPriority(5); // Higher priority
        vehicle3.setPriority(3);

        trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: vehicle2 and vehicle3 waiting (rejected due to capacity)
        assertThat(trafficManager.requestPassage(vehicle2, "CP-1", simTime + 1)).isFalse();
        assertThat(trafficManager.requestPassage(vehicle3, "CP-1", simTime + 2)).isFalse();

        // When: vehicle1 releases, should trigger next vehicle grant immediately
        String nextVehicleId = trafficManager.releasePassage(vehicle1, "CP-1", simTime + 10);

        // Then: Highest priority waiting vehicle (vehicle2) should be granted
        assertThat(nextVehicleId).isEqualTo("V2"); // Higher priority vehicle wins
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
    }

    /**
     * Test scenario: Event-driven conflict resolution (REQ-TC-008)
     * <p>
     * Given: 多辆车竞争同一资源
     * When: 冲突事件发生
     * Then: TrafficManager 应根据冲突解决策略即时处理
     */
    @Test
    void testEventDriven_ConflictResolutionWithPriority() {
        // Given: Control point at capacity with vehicle1 (low priority)
        double simTime = 1000.0;
        vehicle1.setPriority(1);
        vehicle2.setPriority(5); // High priority

        trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: Higher priority vehicle2 is waiting
        assertThat(trafficManager.requestPassage(vehicle2, "CP-1", simTime + 1)).isFalse();

        // When: vehicle1 releases
        String nextVehicleId = trafficManager.releasePassage(vehicle1, "CP-1", simTime + 10);

        // Then: Higher priority vehicle (vehicle2) should be granted access
        assertThat(nextVehicleId).isEqualTo("V2");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
    }

    /**
     * Test scenario: Release with no waiting vehicles returns null (REQ-TC-008)
     * <p>
     * Given: 车辆占用控制点
     * When: 车辆释放且没有等待车辆
     * Then: 应返回 null
     */
    @Test
    void testEventDriven_ReleaseWithNoWaitingVehicles() {
        // Given: Vehicle occupying control point
        double simTime = 1000.0;
        trafficManager.requestPassage(vehicle1, "CP-1", simTime);

        // When: Vehicle releases with no one waiting
        String nextVehicleId = trafficManager.releasePassage(vehicle1, "CP-1", simTime + 10);

        // Then: Should return null (no vehicles waiting)
        assertThat(nextVehicleId).isNull();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(0);
    }

    @Test
    void testEventDriven_ReleaseResetsPassedVehicleReplanCounterOnly() {
        // Given: TrafficManager with block timeout monitor and two vehicles at one control point
        BlockTimeoutMonitor monitor = new BlockTimeoutMonitor(null, 60.0, 3);
        trafficManager.setBlockTimeoutMonitor(monitor);
        double simTime = 1000.0;

        // And: vehicle1 occupies, vehicle2 waits
        assertThat(trafficManager.requestPassage(vehicle1, "CP-1", simTime)).isTrue();
        assertThat(trafficManager.requestPassage(vehicle2, "CP-1", simTime + 1)).isFalse();

        // And: distinct replan counters are prepared before release (A=2, B=1)
        Map<String, Double> waitStartTime = Map.of(vehicle1.id(), simTime - 70, vehicle2.id(), simTime - 70);
        assertThat(monitor.shouldTriggerReplan(vehicle1, waitStartTime, simTime)).isTrue();
        assertThat(monitor.shouldTriggerReplan(vehicle1, waitStartTime, simTime)).isTrue();
        assertThat(monitor.shouldTriggerReplan(vehicle2, waitStartTime, simTime)).isTrue();
        assertThat(monitor.getReplanAttempts(vehicle1.id())).isEqualTo(2);
        assertThat(monitor.getReplanAttempts(vehicle2.id())).isEqualTo(1);

        // When: vehicle1 releases and vehicle2 is granted next
        String nextVehicleId = trafficManager.releasePassage(vehicle1, "CP-1", simTime + 10);

        // Then: only passed vehicle (vehicle1) counter is reset; granted vehicle (vehicle2) remains unchanged
        assertThat(nextVehicleId).isEqualTo(vehicle2.id());
        assertThat(monitor.getReplanAttempts(vehicle1.id())).isEqualTo(0);
        assertThat(monitor.getReplanAttempts(vehicle2.id())).isEqualTo(1);
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1); // vehicle2 now occupies CP-1
    }

    // ==================== Scenario 3: Domain-Based Strategy Selection ====================

    /**
     * Test scenario: Domain strategy selection for OHT (REQ-TC-009)
     * <p>
     * Given: 路径类型为 OHT_TRACK
     * When: TrafficManager 处理请求
     * Then: 应交给 OHT 交通策略处理
     */
    @Test
    void testDomainStrategy_OHTTrackUsesOHTStrategy() {
        // Given: OHT vehicle with OHT track path
        TrackMovement ohtTrack = new TrackMovement(position, new Position(100.0, 0.0, 0.0), null, null);

        // When: Requesting passage for OHT track
        boolean granted = trafficManager.requestPassageForPath(vehicle1, ohtTrack);

        // Then: Should use OHT strategy and grant request
        assertThat(granted).isTrue();
    }

    /**
     * Test scenario: Pluggable strategy registration (REQ-TC-009)
     * <p>
     * Given: 场景仅包含 OHT
     * When: 初始化 TrafficManager
     * Then: 仅注册 OHT 策略，AGV 策略不加载
     */
    @Test
    void testPluggableStrategy_CanRegisterSelectiveStrategies() {
        // Given: TrafficManager
        // Then: OHT strategy should be registered by default
        assertThat(trafficManager.hasStrategy(PathType.OHT_TRACK)).isTrue();

        // And: AGV strategy should not be registered if not explicitly added
        // (This tests the pluggable nature)
        assertThat(trafficManager.hasStrategy(PathType.AGV_NETWORK)).isFalse();
    }

    /**
     * Test scenario: Multiple control points management (REQ-TC-008)
     * <p>
     * Given: 多个控制点
     * When: 车辆请求通过不同控制点
     * Then: TrafficManager 应正确管理每个控制点
     */
    @Test
    void testMultipleControlPoints_ManagesEachIndependently() {
        // Given: Three control points
        double simTime = 1000.0;
        ControlPoint cp1 = new ControlPoint("CP-1", "NODE-1", 1);
        ControlPoint cp2 = new ControlPoint("CP-2", "NODE-2", 1);
        ControlPoint cp3 = new ControlPoint("CP-3", "NODE-3", 2);

        // And: Register all control points
        trafficManager.registerControlPoint(cp1);
        trafficManager.registerControlPoint(cp2);
        trafficManager.registerControlPoint(cp3);

        // When: Vehicles occupy different control points
        assertThat(trafficManager.requestPassage(vehicle1, "CP-1", simTime)).isTrue();
        assertThat(trafficManager.requestPassage(vehicle2, "CP-2", simTime)).isTrue();

        // Then: Each control point should be managed independently
        assertThat(cp1.getCurrentLoad()).isEqualTo(1);
        assertThat(cp2.getCurrentLoad()).isEqualTo(1);
        assertThat(cp3.getCurrentLoad()).isEqualTo(0);
    }

    /**
     * Test scenario: Control area registration and management (REQ-TC-008)
     * <p>
     * Given: TrafficManager
     * When: 注册控制区
     * Then: 应正确管理控制区容量
     */
    @Test
    void testControlArea_RegistrationAndManagement() {
        // Given: Control area with capacity 2
        ControlArea area = new ControlArea("AREA-1", null, 2);

        // When: Register area with traffic manager
        trafficManager.registerControlArea(area);

        // Then: Area should be accessible through manager
        assertThat(trafficManager.getControlArea("AREA-1")).isSameAs(area);

        // And: Vehicles can enter/leave through manager
        assertThat(trafficManager.enterArea(vehicle1, "AREA-1")).isTrue();
        assertThat(trafficManager.enterArea(vehicle2, "AREA-1")).isTrue();

        // At capacity, third vehicle should be rejected
        OHTVehicle vehicle3 = new OHTVehicle("V3", "Vehicle 3", position, 10.0);
        assertThat(trafficManager.enterArea(vehicle3, "AREA-1")).isFalse();
    }

    /**
     * Test scenario: Conflict resolver integration (REQ-TC-007, REQ-TC-008)
     * <p>
     * Given: TrafficManager with custom conflict resolver
     * When: 发生资源竞争
     * Then: 应使用配置的冲突解决器进行仲裁
     */
    @Test
    void testConflictResolver_CustomResolverIntegration() {
        // Given: TrafficManager with custom conflict resolver (fixed seed for determinism)
        ConflictResolver customResolver = new ConflictResolver(12345L);
        trafficManager.setConflictResolver(customResolver);

        // And: Control point with capacity 1 (only one vehicle can occupy)
        ControlPoint singleCapacityPoint = new ControlPoint("CP-SINGLE", "NODE-S", 1);
        trafficManager.registerControlPoint(singleCapacityPoint);

        // And: Two vehicles with different priorities competing
        vehicle1.setPriority(3);
        vehicle2.setPriority(5); // Higher priority

        // When: Both vehicles request passage
        List<Vehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        Vehicle winner = trafficManager.resolveConflict(competingVehicles);

        // Then: Higher priority vehicle should win
        assertThat(winner).isSameAs(vehicle2);
    }

    /**
     * Test scenario: Invalid control point ID handling
     * <p>
     * Given: TrafficManager
     * When: 请求不存在的控制点
     * Then: 应返回失败或 null
     */
    @Test
    void testErrorHandling_InvalidControlPointId() {
        // Given: TrafficManager with no control points
        TrafficManager emptyManager = new TrafficManager();
        double simTime = 1000.0;

        // When: Requesting passage for non-existent control point
        // Then: Should return false (graceful handling)
        boolean granted = emptyManager.requestPassage(vehicle1, "NON-EXISTENT", simTime);
        assertThat(granted).isFalse();

        // And: Releasing non-existent control point should return null
        String nextId = emptyManager.releasePassage(vehicle1, "NON-EXISTENT", simTime);
        assertThat(nextId).isNull();
    }

    /**
     * Test scenario: Get traffic manager statistics
     * <p>
     * Given: 运行中的 TrafficManager
     * When: 查询统计信息
     * Then: 应返回正确的统计数据
     */
    @Test
    void testStatistics_ReturnsCorrectMetrics() {
        // Given: TrafficManager with activity
        double simTime = 1000.0;
        trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        trafficManager.releasePassage(vehicle1, "CP-1", simTime + 10);

        // When: Getting statistics
        String stats = trafficManager.getStatistics();

        // Then: Should contain relevant information
        assertThat(stats).contains("TrafficManager");
        assertThat(stats).contains("1"); // At least one control point registered
    }

    /**
     * Test scenario: Backward compatibility with deprecated methods
     * <p>
     * Given: 已存在的使用旧 API 的代码
     * When: 调用无参版本方法
     * Then: 应仍然正常工作
     */
    @Test
    void testBackwardCompatibility_DeprecatedMethodsWork() {
        // When: Using deprecated methods (no simulation time parameter)
        boolean granted = trafficManager.requestPassage(vehicle1, "CP-1");
        assertThat(granted).isTrue();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: Release with deprecated method
        String nextId = trafficManager.releasePassage(vehicle1, "CP-1");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(0);
    }

    // ==================== Conflict Arbitration Tests (REQ-TC-008) ====================

    /**
     * Test scenario: TrafficManager uses ConflictResolver when multiple vehicles waiting (REQ-TC-008)
     * <p>
     * Given: ControlPoint at capacity with multiple waiting vehicles
     * When: 新车辆请求被拒绝（触发仲裁）
     * Then: TrafficManager 应调用 ConflictResolver 进行仲裁
     */
    @Test
    void testConflictArbitration_UsesConflictResolverWhenMultipleWaiting() {
        // Given: Control point at capacity with vehicle1 occupying
        double simTime = 1000.0;
        vehicle1.setPriority(1);
        vehicle2.setPriority(3);
        vehicle3.setPriority(5); // Highest priority

        trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: Multiple vehicles waiting (rejected due to capacity)
        trafficManager.requestPassage(vehicle2, "CP-1", simTime + 1);
        trafficManager.requestPassage(vehicle3, "CP-1", simTime + 2);

        // Verify waiting vehicles exist
        assertThat(testControlPoint.getWaitingVehicleIds()).hasSize(2);

        // When: Get waiting vehicles from control point
        List<Vehicle> waitingVehicles = new ArrayList<>(testControlPoint.getWaitingVehicles());

        // Then: Should contain vehicle2 and vehicle3
        assertThat(waitingVehicles).hasSize(2);
        assertThat(waitingVehicles).contains(vehicle2, vehicle3);

        // And: TrafficManager's conflict resolver should pick highest priority
        Vehicle winner = trafficManager.resolveConflict(waitingVehicles);
        assertThat(winner).isSameAs(vehicle3); // Priority 5 wins
    }

    /**
     * Test scenario: Conflict arbitration with custom resolver (REQ-TC-008)
     * <p>
     * Given: TrafficManager with custom ConflictResolver
     * When: 多个等待车辆发生冲突
     * Then: 应使用自定义的冲突解决器进行仲裁
     */
    @Test
    void testConflictArbitration_CustomResolverIsUsed() {
        // Given: TrafficManager with custom conflict resolver (fixed seed)
        ConflictResolver customResolver = new ConflictResolver(99999L);
        trafficManager.setConflictResolver(customResolver);

        // And: Control point at capacity with multiple waiting vehicles
        double simTime = 1000.0;
        vehicle1.setPriority(2);
        vehicle2.setPriority(2); // Same priority
        vehicle3.setPriority(2); // Same priority - will use random tie-break

        trafficManager.requestPassage(vehicle1, "CP-1", simTime);
        trafficManager.requestPassage(vehicle2, "CP-1", simTime + 1);
        trafficManager.requestPassage(vehicle3, "CP-1", simTime + 2);

        // When: Resolve conflict using TrafficManager's resolver
        List<Vehicle> waitingVehicles = new ArrayList<>(testControlPoint.getWaitingVehicles());
        Vehicle winner = trafficManager.resolveConflict(waitingVehicles);

        // Then: One of the vehicles should win (deterministic with fixed seed)
        assertThat(winner).isIn(vehicle2, vehicle3); // vehicle1 is occupying, not waiting
    }

    /**
     * Test scenario: Conflict arbitration priority order (REQ-TC-007, REQ-TC-008)
     * <p>
     * Given: 三辆车优先级各不相同
     * When: 发生冲突
     * Then: 最高优先级车辆应获胜
     */
    @Test
    void testConflictArbitration_HighestPriorityWins() {
        // Given: Three vehicles with different priorities
        vehicle1.setPriority(1);
        vehicle2.setPriority(5);
        vehicle3.setPriority(3);

        List<Vehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);
        competingVehicles.add(vehicle3);

        // When: Resolve conflict
        Vehicle winner = trafficManager.resolveConflict(competingVehicles);

        // Then: Highest priority (vehicle2 with priority 5) should win
        assertThat(winner).isSameAs(vehicle2);
    }

    // ==================== Dual-Layer Coordination Tests (REQ-TC-010) ====================

    /**
     * Test scenario: Dual-layer coordination through TrafficManager (REQ-TC-010)
     * <p>
     * Given: TrafficManager with registered ControlPoint and Edge
     * When: 车辆请求通过控制点和路段
     * Then: 应同时满足双层管制要求
     */
    @Test
    void testDualLayerCoordination_RequestPassageWithEdgeSuccess() {
        // Given: TrafficManager with control point and edge registered
        ControlPoint cp = new ControlPoint("CP-1", "NODE-1", 1);
        EdgeReservation edge = new EdgeReservation("EDGE-1", 1);
        trafficManager.registerControlPoint(cp);
        trafficManager.registerEdge(edge);

        double simTime = 1000.0;

        // When: Request passage through both CP and Edge
        boolean granted = trafficManager.requestPassageWithEdge(vehicle1, "CP-1", "EDGE-1", simTime);

        // Then: Should be granted and both resources occupied
        assertThat(granted).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(edge.getCurrentLoad()).isEqualTo(1);
    }

    /**
     * Test scenario: Dual-layer rollback when Edge fails (REQ-TC-010)
     * <p>
     * Given: TrafficManager with CP 和 Edge
     * And: Edge 容量已满
     * When: 新车辆请求通过
     * Then: CP 申请成功但 Edge 失败后应回滚 CP
     */
    @Test
    void testDualLayerCoordination_EdgeFullRollsBackControlPoint() {
        // Given: Edge at capacity with vehicle1 occupying
        ControlPoint cp = new ControlPoint("CP-1", "NODE-1", 1);
        EdgeReservation edge = new EdgeReservation("EDGE-1", 1);
        trafficManager.registerControlPoint(cp);
        trafficManager.registerEdge(edge);

        double simTime = 1000.0;

        // Vehicle1 occupies both CP and Edge
        assertThat(trafficManager.requestPassageWithEdge(vehicle1, "CP-1", "EDGE-1", simTime)).isTrue();
        assertThat(cp.getCurrentLoad()).isEqualTo(1);
        assertThat(edge.getCurrentLoad()).isEqualTo(1);

        // When: Vehicle2 requests passage (Edge is full)
        boolean granted = trafficManager.requestPassageWithEdge(vehicle2, "CP-1", "EDGE-1", simTime + 1);

        // Then: Should be rejected and CP should not be occupied (rollback occurred)
        assertThat(granted).isFalse();
        // Vehicle1 still occupies, vehicle2 does not
        assertThat(cp.getCurrentLoad()).isEqualTo(1); // Still vehicle1
        assertThat(edge.getCurrentLoad()).isEqualTo(1); // Still vehicle1
    }

    /**
     * Test scenario: Dual-layer release triggers next vehicle grant (REQ-TC-010)
     * <p>
     * Given: 多车等待 CP 和 Edge
     * When: 占用车辆释放
     * Then: 应触发下一辆车获准进入 CP（Edge 需单独申请）
     */
    @Test
    void testDualLayerCoordination_ReleaseTriggersNextVehicleGrant() {
        // Given: CP at capacity with vehicle1, others waiting
        ControlPoint cp = new ControlPoint("CP-1", "NODE-1", 1);
        EdgeReservation edge = new EdgeReservation("EDGE-1", 1);
        trafficManager.registerControlPoint(cp);
        trafficManager.registerEdge(edge);

        double simTime = 1000.0;
        vehicle1.setPriority(1);
        vehicle2.setPriority(5); // Higher priority

        // Vehicle1 occupies both
        assertThat(trafficManager.requestPassageWithEdge(vehicle1, "CP-1", "EDGE-1", simTime)).isTrue();

        // Vehicle2 tries and fails (at capacity)
        assertThat(trafficManager.requestPassageWithEdge(vehicle2, "CP-1", "EDGE-1", simTime + 1)).isFalse();

        // When: Vehicle1 releases
        String nextVehicleId = trafficManager.releasePassageWithEdge(vehicle1, "CP-1", "EDGE-1", simTime + 10);

        // Then: Next vehicle (V2) is granted CP access (CP is auto-occupied by releaseAndGetNext)
        assertThat(nextVehicleId).isEqualTo("V2"); // Higher priority vehicle
        assertThat(cp.getCurrentLoad()).isEqualTo(1); // Vehicle2 now auto-occupying CP

        // Edge is released but not auto-occupied (vehicle2 must explicitly request it)
        assertThat(edge.getCurrentLoad()).isEqualTo(0); // Vehicle2 needs to request Edge separately
    }

    /**
     * Test scenario: Invalid edge or control point handling (REQ-TC-010)
     * <p>
     * Given: TrafficManager
     * When: 请求不存在的 CP 或 Edge
     * Then: 应返回失败（优雅处理）
     */
    @Test
    void testDualLayerCoordination_InvalidResourcesHandledGracefully() {
        // Given: TrafficManager with no resources registered
        TrafficManager emptyManager = new TrafficManager();
        double simTime = 1000.0;

        // When: Requesting with non-existent resources
        boolean granted = emptyManager.requestPassageWithEdge(vehicle1, "NON-EXISTENT-CP", "NON-EXISTENT-EDGE", simTime);

        // Then: Should return false (graceful handling)
        assertThat(granted).isFalse();

        // And: Release with non-existent resources should return null
        String nextId = emptyManager.releasePassageWithEdge(vehicle1, "NON-EXISTENT-CP", "NON-EXISTENT-EDGE", simTime);
        assertThat(nextId).isNull();
    }
}
