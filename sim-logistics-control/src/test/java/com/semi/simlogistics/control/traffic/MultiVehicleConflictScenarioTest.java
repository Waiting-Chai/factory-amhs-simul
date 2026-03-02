package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.vehicle.OHTVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario-level integration tests for multi-vehicle conflict resolution (Task 2.11).
 * <p>
 * Tests the complete event-driven flow of traffic control with multiple vehicles:
 * - TrafficManager unified entry point (REQ-TC-008, REQ-TC-009)
 * - ControlPoint capacity and priority arbitration (REQ-TC-005)
 * - PriorityManager aging calculation (REQ-TC-011)
 * - ConflictResolver arbitration (REQ-TC-007)
 * - Release chain correctness (automatic grant on release)
 * - Failed vehicle resource retention (REQ-TC-008, REQ-TC-010)
 * <p>
 * Each scenario is independent and follows Given/When/Then structure.
 * All tests use simulation time (env.now()) instead of wall clock time.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@DisplayName("Multi-Vehicle Conflict Scenarios (Task 2.11)")
class MultiVehicleConflictScenarioTest {

    private TrafficManager trafficManager;
    private ControlPoint testControlPoint;
    private OHTVehicle vehicleA;
    private OHTVehicle vehicleB;
    private OHTVehicle vehicleC;
    private OHTVehicle vehicleD;
    private Position position;

    /**
     * Set up common test fixtures for all scenarios.
     * <p>
     * Creates:
     * - TrafficManager with default configuration
     * - ControlPoint with capacity 1 (single resource)
     * - Three test vehicles (A, B, C)
     */
    @BeforeEach
    void setUp() {
        // Given: A TrafficManager with default configuration
        trafficManager = new TrafficManager();

        // And: A test control point with capacity 1 (single resource for conflict scenarios)
        position = new Position(0.0, 0.0, 0.0);
        testControlPoint = new ControlPoint("CP-1", "NODE-1", 1);

        // And: Register control point with traffic manager
        trafficManager.registerControlPoint(testControlPoint);

        // And: Three test vehicles
        vehicleA = new OHTVehicle("V-A", "Vehicle A", position, 10.0);
        vehicleB = new OHTVehicle("V-B", "Vehicle B", position, 10.0);
        vehicleC = new OHTVehicle("V-C", "Vehicle C", position, 10.0);
        vehicleD = new OHTVehicle("V-D", "Vehicle D", position, 10.0);
    }

    // ==================== Scenario A: 双车同控制点冲突（基础） ====================

    /**
     * Scenario A: 双车同控制点冲突（基础）
     * <p>
     * Given: ControlPoint 容量=1，Vehicle-A 已占用，Vehicle-B 请求进入
     * When: A 释放
     * Then: B 自动获准（通过 releaseAndGetNext 事件链）
     * <p>
     * Coverage:
     * - Event-driven release chain (REQ-TC-008)
     * - Automatic grant on release (no re-request needed)
     * - Simulation time parameter usage
     */
    @Test
    @DisplayName("Scenario A: 双车同控制点冲突（基础） - Given A 占用 B 等待，When A 释放，Then B 自动获准")
    void scenarioA_TwoVehiclesSameControlPoint_BAutoGrantedOnARelease() {
        // Given: ControlPoint 容量=1
        assertThat(testControlPoint.getCapacity()).isEqualTo(1);

        // And: Vehicle-A 已占用
        double simTime = 1000.0;
        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
        // A 占用成功（currentLoad=1 且 A 不在等待队列中）
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-A");

        // And: Vehicle-B 请求进入（因容量满被拒绝）
        boolean bGranted = trafficManager.requestPassage(vehicleB, "CP-1", simTime + 1);
        assertThat(bGranted).isFalse();
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-B");

        // When: A 释放（触发 releaseAndGetNext 事件链）
        String grantedVehicleId = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 10);

        // Then: B 自动获准（通过 releaseAndGetNext 事件链）
        assertThat(grantedVehicleId).isEqualTo("V-B");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
        // B 不再在等待队列中（说明已获准并占用）
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-B");

        // And: currentLoad 始终满足 <= capacity（REQ-TC-005）
        assertThat(testControlPoint.getCurrentLoad()).isLessThanOrEqualTo(testControlPoint.getCapacity());
    }

    // ==================== Scenario B: 双车优先级冲突（高优先级应胜出） ====================

    /**
     * Scenario B: 双车优先级冲突（高优先级应胜出）
     * <p>
     * Given: A 占用，B/C 同时等待且优先级不同
     * When: A 释放
     * Then: 高优先级车辆先获准
     * <p>
     * Coverage:
     * - Priority arbitration (REQ-TC-005)
     * - Effective priority (base + aging)
     * - releaseAndGetNext returns highest priority vehicle
     */
    @Test
    @DisplayName("Scenario B: 双车优先级冲突 - Given A 占用 B/C 等待优先级不同，When A 释放，Then 高优先级胜出")
    void scenarioB_PriorityConflict_HigherPriorityVehicleWins() {
        // Given: A 占用
        double simTime = 1000.0;
        vehicleA.setPriority(1);  // Low priority
        vehicleB.setPriority(5);  // Medium priority
        vehicleC.setPriority(10); // High priority

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();

        // And: B/C 同时等待且优先级不同
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime + 1)).isFalse();
        assertThat(trafficManager.requestPassage(vehicleC, "CP-1", simTime + 1)).isFalse();
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-B", "V-C");

        // When: A 释放
        String grantedVehicleId = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 10);

        // Then: 高优先级车辆（C，priority=10）先获准
        assertThat(grantedVehicleId).isEqualTo("V-C");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
        // C 不再在等待队列中（说明已获准）
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-C");

        // And: 等待队列中仍包含 V-B（未获准）
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-B");

        // And: currentLoad 不溢出
        assertThat(testControlPoint.getCurrentLoad()).isLessThanOrEqualTo(testControlPoint.getCapacity());
    }

    // ==================== Scenario C: 同优先级 FIFO ====================

    /**
     * Scenario C: 同优先级 FIFO
     * <p>
     * Given: A 占用，B/C 同优先级且 B 先到
     * When: A 释放
     * Then: B 先获准（FIFO 生效）
     * <p>
     * Coverage:
     * - FIFO arbitration rule (REQ-TC-005)
     * - Wait start time tracking
     * - Same priority tie-breaking by arrival time
     */
    @Test
    @DisplayName("Scenario C: 同优先级 FIFO - Given A 占用 B/C 同优先级 B 先到，When A 释放，Then B 先获准")
    void scenarioC_SamePriority_FIFO_ArrivalTimeWins() {
        // Given: A 占用
        double simTime = 1000.0;
        vehicleA.setPriority(5);
        vehicleB.setPriority(5);
        vehicleC.setPriority(5); // All same priority

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();

        // And: B/C 同优先级且 B 先到（B 先请求，C 后请求）
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime + 1)).isFalse();
        assertThat(trafficManager.requestPassage(vehicleC, "CP-1", simTime + 2)).isFalse();

        // Verify waiting order (B arrived before C)
        Map<String, Double> waitStartTime = testControlPoint.getWaitStartTime();
        assertThat(waitStartTime.get("V-B")).isEqualTo(simTime + 1);
        assertThat(waitStartTime.get("V-C")).isEqualTo(simTime + 2);
        assertThat(waitStartTime.get("V-B")).isLessThan(waitStartTime.get("V-C"));

        // When: A 释放
        String grantedVehicleId = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 10);

        // Then: B 先获准（FIFO 生效 - B 先到）
        assertThat(grantedVehicleId).isEqualTo("V-B");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
        // B 不再在等待队列中（说明已获准）
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-B");

        // And: 等待队列递减（C 仍在等待）
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-C");
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-B");

        // And: currentLoad 始终 <= capacity
        assertThat(testControlPoint.getCurrentLoad()).isLessThanOrEqualTo(testControlPoint.getCapacity());
    }

    // ==================== Scenario D: 优先级老化影响 ====================

    /**
     * Scenario D: 优先级老化影响
     * <p>
     * Given: 低优先级车辆等待更久，高优先级车辆等待更短
     * When: 触发仲裁
     * Then: 按 effectivePriority（含 aging）决策，结果符合公式
     * <p>
     * Coverage:
     * - Priority aging formula (REQ-TC-011): effectivePriority = base + floor(waited / agingStep) * agingBoost
     * - Aging boost cap (maxBoost)
     * - Simulation time usage (not wall clock)
     * <p>
     * Test setup:
     * - Vehicle-B: basePriority=1, waited=90s (agingStep=30s, boost=3, effective=4)
     * - Vehicle-C: basePriority=5, waited=10s (boost=0, effective=5)
     * - Result: C wins (effective 5 > 4)
     */
    @Test
    @DisplayName("Scenario D: 优先级老化影响 - Given 低优先级等久高优先级等短，When 仲裁，Then 按 effectivePriority 决策")
    void scenarioD_PriorityAging_EffectivePriorityDeterminesWinner() {
        // Given: A 占用
        double simTime = 1000.0;
        vehicleA.setPriority(10);
        vehicleB.setPriority(1);  // Low base priority
        vehicleC.setPriority(5);  // Medium base priority

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();

        // And: 低优先级车辆 B 等待更久（90s），高优先级车辆 C 等待更短（10s）
        // B: base=1, waited=90s, agingStep=30s => boost=floor(90/30)*1=3, effective=1+3=4
        // C: base=5, waited=10s, agingStep=30s => boost=floor(10/30)*1=0, effective=5+0=5
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime - 90)).isFalse(); // Waited 90s
        assertThat(trafficManager.requestPassage(vehicleC, "CP-1", simTime - 10)).isFalse(); // Waited 10s

        // Verify wait start times (simulation time)
        Map<String, Double> waitStartTime = testControlPoint.getWaitStartTime();
        assertThat(waitStartTime.get("V-B")).isEqualTo(simTime - 90);
        assertThat(waitStartTime.get("V-C")).isEqualTo(simTime - 10);

        // And: 验证 effectivePriority 计算符合公式（REQ-TC-011）在释放之前
        PriorityManager pm = new PriorityManager();
        int effectivePriorityB = pm.calculateEffectivePriority(vehicleB, waitStartTime.get("V-B"), simTime);
        int effectivePriorityC = pm.calculateEffectivePriority(vehicleC, waitStartTime.get("V-C"), simTime);
        assertThat(effectivePriorityB).isEqualTo(4); // 1 + floor(90/30)*1 = 4
        assertThat(effectivePriorityC).isEqualTo(5); // 5 + floor(10/30)*1 = 5
        assertThat(effectivePriorityC).isGreaterThan(effectivePriorityB);

        // When: A 释放（触发仲裁）
        String grantedVehicleId = trafficManager.releasePassage(vehicleA, "CP-1", simTime);

        // Then: 按 effectivePriority 决策，C 胜出（effective=5 > B's effective=4）
        assertThat(grantedVehicleId).isEqualTo("V-C");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: 等待队列变化（B 仍在等待）
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-B");
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-C");

        // And: 仿真时间参数路径（不回退到墙钟逻辑）
        // Verify all timestamps are from simulation time (env.now()), not System.currentTimeMillis()
        // Note: V-C 已获准并从等待队列移除，所以只验证 V-B
        assertThat(waitStartTime.get("V-B")).isNotNull();
    }

    /**
     * Scenario D 扩展: 优先级老化上限测试
     * <p>
     * Given: 车辆等待极长时间（超过 maxBoost 限制）
     * When: 触发仲裁
     * Then: 老化提升不超过 maxBoost（默认 5）
     * <p>
     * Coverage:
     * - Aging boost cap (REQ-TC-011)
     * - maxBoost constraint (default 5 per REQ-TC-000)
     */
    @Test
    @DisplayName("Scenario D 扩展: 优先级老化上限 - Given 车辆等待极长时间，When 仲裁，Then 老化提升不超过 maxBoost")
    void scenarioD_PriorityAging_MaxBoostCapApplied() {
        // Given: A 占用
        double simTime = 1000.0;
        vehicleA.setPriority(10);
        vehicleB.setPriority(1); // Low base priority

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();

        // And: B 等待极长时间（200s，远超 agingStep*maxBoost=30*5=150s）
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime - 200)).isFalse();

        Map<String, Double> waitStartTime = testControlPoint.getWaitStartTime();
        assertThat(waitStartTime.get("V-B")).isEqualTo(simTime - 200);

        // When: 计算 effective priority
        PriorityManager pm = new PriorityManager();
        int effectivePriority = pm.calculateEffectivePriority(vehicleB, waitStartTime.get("V-B"), simTime);

        // Then: 老化提升不超过 maxBoost（默认 5）
        // effective = 1 + floor(200/30)*1 = 1 + 6 = 7, but capped at 1+5=6 (maxBoost=5)
        assertThat(effectivePriority).isEqualTo(6); // 1 + 5 (maxBoost) = 6

        // Verify: 不超过最大优先级 10
        assertThat(effectivePriority).isLessThanOrEqualTo(10);
    }

    // ==================== Scenario E: 释放链路正确性（自动解决） ====================

    /**
     * Scenario E: 释放链路正确性（自动解决）
     * <p>
     * Given: 队列中有多个等待车辆
     * When: 连续释放
     * Then: 每次释放后只授予1辆，currentLoad 不溢出，等待队列递减
     * <p>
     * Coverage:
     * - Automatic grant on release (REQ-TC-008)
     * - Capacity constraint (never exceeds capacity)
     * - Waiting queue decrement
     * - Event-driven chain (release triggers grant)
     */
    @Test
    @DisplayName("Scenario E: 释放链路正确性 - Given 多车等待，When 连续释放，Then 每次授予1辆队列递减")
    void scenarioE_ReleaseChain_AutoGrantWithCapacityConstraint() {
        // Given: 队列中有多个等待车辆（A 占用，B/C/D 等待）
        double simTime = 1000.0;
        vehicleA.setPriority(1);
        vehicleB.setPriority(3);
        vehicleC.setPriority(5);
        vehicleD.setPriority(2);

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();

        // B/C/D 等待（按优先级：C=5 > B=3 > D=2）
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime + 1)).isFalse();
        assertThat(trafficManager.requestPassage(vehicleC, "CP-1", simTime + 2)).isFalse();
        assertThat(trafficManager.requestPassage(vehicleD, "CP-1", simTime + 3)).isFalse();

        assertThat(testControlPoint.getWaitingVehicleIds()).hasSize(3);
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // When: 连续释放（A 释放 -> C 获准 -> C 释放 -> B 获准 -> B 释放 -> D 获准）
        String granted1 = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 10);
        assertThat(granted1).isEqualTo("V-C"); // 最高优先级 C 获准
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1); // 不溢出
        assertThat(testControlPoint.getWaitingVehicleIds()).hasSize(2); // 队列递减（B, D）

        String granted2 = trafficManager.releasePassage(vehicleC, "CP-1", simTime + 20);
        assertThat(granted2).isEqualTo("V-B"); // 次高优先级 B 获准
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1); // 不溢出
        assertThat(testControlPoint.getWaitingVehicleIds()).hasSize(1); // 队列递减（D）

        String granted3 = trafficManager.releasePassage(vehicleB, "CP-1", simTime + 30);
        assertThat(granted3).isEqualTo("V-D"); // 最低优先级 D 获准
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1); // 不溢出
        assertThat(testControlPoint.getWaitingVehicleIds()).isEmpty(); // 队列清空

        // Then: 每次释放后只授予1辆，currentLoad 始终 <= capacity
        // Verify all intermediate states satisfied capacity constraint
        assertThat(testControlPoint.getCurrentLoad()).isLessThanOrEqualTo(testControlPoint.getCapacity());

        // And: 最终等待队列为空
        assertThat(testControlPoint.getWaitingVehicleIds()).isEmpty();
    }

    // ==================== Scenario F: 故障硬约束不破坏冲突解决 ====================

    /**
     * Scenario F: 故障硬约束不破坏冲突解决
     * <p>
     * Given: 故障车辆占用控制点
     * When: 调用 release
     * Then: 故障车辆资源不释放，不触发下一辆获准（与 REQ-TC-008/010 现有约束一致）
     * <p>
     * Coverage:
     * - Failed vehicle resource retention (REQ-TC-008, REQ-TC-010)
     * - Hard constraint: FAILED state vehicles keep resources
     * - No grant triggered when failed vehicle attempts to release
     */
    @Test
    @DisplayName("Scenario F: 故障硬约束 - Given 故障车辆占用，When 调用 release，Then 资源不释放不触发下辆获准")
    void scenarioF_FailedVehicleRetainsResources_NoGrantOnRelease() {
        // Given: 故障车辆 A 占用控制点
        double simTime = 1000.0;
        vehicleA.setPriority(1);
        vehicleB.setPriority(5);

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: B 等待
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime + 1)).isFalse();
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-B");

        // And: A 发生故障（状态变为 FAILED）
        vehicleA.setState(VehicleState.FAILED);
        assertThat(vehicleA.getState()).isEqualTo(VehicleState.FAILED);

        // When: 调用 release（故障车辆尝试释放）
        String grantedVehicleId = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 10);

        // Then: 故障车辆资源不释放（currentLoad 仍为 1）
        assertThat(grantedVehicleId).isNull(); // 不触发下一辆获准
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1); // A 仍占用
        // A 仍在占用（currentLoad=1，且 B 仍在等待说明 A 未释放）
        assertThat(testControlPoint.getWaitingVehicleIds()).contains("V-B");

        // And: 与 REQ-TC-008/010 现有约束一致（故障车辆占用策略）
        // Verify that the failed vehicle retains the resource per spec
        assertThat(vehicleA.getState()).isEqualTo(VehicleState.FAILED);
        assertThat(testControlPoint.getCurrentLoad()).isLessThanOrEqualTo(testControlPoint.getCapacity());
    }

    /**
     * Scenario F 扩展: 非故障车辆释放后正常授予
     * <p>
     * Given: 故障车辆占用修复后恢复正常
     * When: 调用 release
     * Then: 正常释放并触发下一辆获准
     * <p>
     * Coverage:
     * - Verify failed vehicle can release after repair
     * - Contrast with Scenario F (failed behavior vs normal behavior)
     */
    @Test
    @DisplayName("Scenario F 扩展: 故障修复后正常释放 - Given 故障修复，When release，Then 正常释放并触发下辆获准")
    void scenarioF_Extension_FailedVehicleRepaired_NormalReleaseAndGrant() {
        // Given: A 占用，B 等待
        double simTime = 1000.0;
        vehicleA.setPriority(1);
        vehicleB.setPriority(5);

        boolean aGranted = trafficManager.requestPassage(vehicleA, "CP-1", simTime);
        assertThat(aGranted).isTrue();
        assertThat(trafficManager.requestPassage(vehicleB, "CP-1", simTime + 1)).isFalse();

        // And: A 发生故障
        vehicleA.setState(VehicleState.FAILED);
        assertThat(vehicleA.getState()).isEqualTo(VehicleState.FAILED);

        // When: 尝试释放故障车辆（应被拒绝）
        String granted1 = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 10);
        assertThat(granted1).isNull();
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);

        // And: A 修复（状态恢复为 IDLE）
        vehicleA.setState(VehicleState.IDLE);
        assertThat(vehicleA.getState()).isEqualTo(VehicleState.IDLE);

        // When: 再次调用 release（正常释放）
        String granted2 = trafficManager.releasePassage(vehicleA, "CP-1", simTime + 20);

        // Then: 正常释放并触发下一辆获准
        assertThat(granted2).isEqualTo("V-B");
        assertThat(testControlPoint.getCurrentLoad()).isEqualTo(1);
        // B 不再在等待队列中（说明已获准）
        assertThat(testControlPoint.getWaitingVehicleIds()).doesNotContain("V-B");
    }

    // ==================== Helper Methods ====================

    /**
     * Get the waiting vehicle IDs from the control point.
     * <p>
     * This is a convenience method for test assertions.
     *
     * @return unmodifiable set of waiting vehicle IDs
     */
    private Set<String> getWaitingVehicleIds() {
        return testControlPoint.getWaitingVehicleIds();
    }
}
