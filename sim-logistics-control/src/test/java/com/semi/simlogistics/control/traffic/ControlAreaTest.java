package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.vehicle.OHTVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ControlArea.
 * <p>
 * Tests cover three main scenarios per REQ-TC-006:
 * 1. Capacity limit enforcement
 * 2. Nested area capacity linkage
 * 3. Statistics tracking (current count, peak, average wait time)
 * <p>
 * All tests follow Given/When/Then structure aligned with spec scenarios.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class ControlAreaTest {

    private ControlArea area;
    private OHTVehicle vehicle1;
    private OHTVehicle vehicle2;
    private OHTVehicle vehicle3;
    private OHTVehicle vehicle4;
    private OHTVehicle vehicle5;

    @BeforeEach
    void setUp() {
        // Given: A ControlArea with capacity 3 (default per REQ-TC-000)
        area = new ControlArea("AREA-1", null, 3);

        // And: Five test vehicles
        Position pos = new Position(0.0, 0.0, 0.0);
        vehicle1 = new OHTVehicle("V1", "Vehicle 1", pos, 10.0);
        vehicle2 = new OHTVehicle("V2", "Vehicle 2", pos, 10.0);
        vehicle3 = new OHTVehicle("V3", "Vehicle 3", pos, 10.0);
        vehicle4 = new OHTVehicle("V4", "Vehicle 4", pos, 10.0);
        vehicle5 = new OHTVehicle("V5", "Vehicle 5", pos, 10.0);
    }

    // ==================== Scenario 1: Capacity Limit Enforcement ====================

    /**
     * Test scenario: Control area capacity limit (REQ-TC-006)
     * <p>
     * Given: 一个最大车辆数为 3 的 ControlArea
     * And: 3 辆车已在区域内
     * When: 第 4 辆车尝试进入
     * Then: 请求应被拒绝
     * And: 车辆应在区域外等待
     */
    @Test
    void testCapacityLimit_RejectsExcessVehicles() {
        // Given: 3 vehicles already in the area
        assertThat(area.tryEnter(vehicle1)).isTrue();
        assertThat(area.tryEnter(vehicle2)).isTrue();
        assertThat(area.tryEnter(vehicle3)).isTrue();

        // When: 4th vehicle tries to enter
        boolean canEnter = area.tryEnter(vehicle4);

        // Then: Request should be rejected
        assertThat(canEnter).isFalse();
        assertThat(area.getCurrentVehicleCount()).isEqualTo(3);
    }

    /**
     * Test scenario: Capacity recovery after vehicle leaves (REQ-TC-006)
     * <p>
     * Given: 一个满容量的 ControlArea (3/3)
     * When: 一辆车离开
     * Then: 容量应恢复
     * And: 新车辆可以进入
     */
    @Test
    void testCapacityRecovers_AfterVehicleLeaves() {
        // Given: Area at full capacity (3/3)
        area.tryEnter(vehicle1);
        area.tryEnter(vehicle2);
        area.tryEnter(vehicle3);
        assertThat(area.getCurrentVehicleCount()).isEqualTo(3);
        assertThat(area.tryEnter(vehicle4)).isFalse();

        // When: One vehicle leaves
        area.leave(vehicle1);

        // Then: Capacity should recover
        assertThat(area.getCurrentVehicleCount()).isEqualTo(2);
        assertThat(area.tryEnter(vehicle4)).isTrue();
        assertThat(area.getCurrentVehicleCount()).isEqualTo(3);
    }

    /**
     * Test scenario: Default capacity from system_config (REQ-TC-000, REQ-TC-006)
     * <p>
     * Given: ControlArea 容量优先来自 system_config
     * And: 若未配置则默认容量为 3
     */
    @Test
    void testDefaultCapacity_WhenNotConfigured() {
        // Given: ControlArea with capacity 0 (use default)
        ControlArea defaultArea = new ControlArea("DEFAULT-AREA", null, 0);

        // Then: Should use default capacity of 3 per REQ-TC-000
        assertThat(defaultArea.getCapacity()).isEqualTo(3);
    }

    // ==================== Scenario 2: Nested Area Capacity Linkage ====================

    /**
     * Test scenario: Control area nesting (REQ-TC-006)
     * <p>
     * Given: 一个嵌套的 ControlArea 结构
     * And: 区域 A 包含区域 B
     * When: 车辆从 A 进入 B
     * Then: 两者的容量都应正确更新（进入子区同时占用父区）
     */
    @Test
    void testNestedArea_ChildEntersUpdatesBoth() {
        // Given: Parent area A (capacity 5) and child area B (capacity 2)
        ControlArea parentArea = new ControlArea("PARENT-A", null, 5);
        ControlArea childArea = new ControlArea("CHILD-B", parentArea, 2);

        // When: Vehicle enters child area
        boolean canEnter = childArea.tryEnter(vehicle1);

        // Then: Both areas should count the vehicle
        assertThat(canEnter).isTrue();
        assertThat(parentArea.getCurrentVehicleCount()).isEqualTo(1);
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(1);

        // And: Parent should reference child
        assertThat(parentArea.getChildren()).contains(childArea);
        assertThat(childArea.getParent()).isSameAs(parentArea);
    }

    /**
     * Test scenario: Nested area - child capacity limit (REQ-TC-006)
     * <p>
     * Given: 父区域 A 容量充足，子区域 B 容量已满
     * When: 车辆尝试进入子区域 B
     * Then: 请求应被拒绝（受子区域容量限制）
     */
    @Test
    void testNestedArea_ChildCapacityLimitsEntry() {
        // Given: Parent (capacity 5) and child (capacity 2)
        ControlArea parentArea = new ControlArea("PARENT", null, 5);
        ControlArea childArea = new ControlArea("CHILD", parentArea, 2);

        // And: Child at capacity
        childArea.tryEnter(vehicle1);
        childArea.tryEnter(vehicle2);
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(2);

        // When: 3rd vehicle tries to enter child
        boolean canEnter = childArea.tryEnter(vehicle3);

        // Then: Should be rejected (child capacity limit)
        assertThat(canEnter).isFalse();
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(2);
        assertThat(parentArea.getCurrentVehicleCount()).isEqualTo(2); // Still only 2
    }

    /**
     * Test scenario: Nested area - leave child only (REQ-TC-006)
     * <p>
     * Given: 车辆在嵌套区域 B 中（B 在 A 中）
     * When: 车辆离开 B
     * Then: 只有 B 的容量恢复
     * And: A 的容量保持占用
     */
    @Test
    void testNestedArea_LeaveChildOnly() {
        // Given: Parent and child areas, vehicle in child
        ControlArea parentArea = new ControlArea("PARENT", null, 5);
        ControlArea childArea = new ControlArea("CHILD", parentArea, 2);

        childArea.tryEnter(vehicle1);
        assertThat(parentArea.getCurrentVehicleCount()).isEqualTo(1);
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(1);

        // When: Vehicle leaves child area
        childArea.leave(vehicle1);

        // Then: Only child's capacity recovers, parent still occupied
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(0);
        assertThat(parentArea.getCurrentVehicleCount()).isEqualTo(1); // Still in parent
    }

    /**
     * Test scenario: Deeply nested areas (3 levels)
     * <p>
     * Given: 三层嵌套区域 (A > B > C)
     * When: 车辆进入最内层区域 C
     * Then: 所有层级 (A, B, C) 的容量都应更新
     */
    @Test
    void testNestedArea_ThreeLevels() {
        // Given: Three-level nesting: A > B > C
        ControlArea levelA = new ControlArea("LEVEL-A", null, 10);
        ControlArea levelB = new ControlArea("LEVEL-B", levelA, 5);
        ControlArea levelC = new ControlArea("LEVEL-C", levelB, 2);

        // When: Vehicle enters innermost area C
        levelC.tryEnter(vehicle1);

        // Then: All three levels should count the vehicle
        assertThat(levelA.getCurrentVehicleCount()).isEqualTo(1);
        assertThat(levelB.getCurrentVehicleCount()).isEqualTo(1);
        assertThat(levelC.getCurrentVehicleCount()).isEqualTo(1);
    }

    /**
     * Test scenario: Parent full prevents child entry (REQ-TC-006)
     * <p>
     * Given: 父区域 A 容量已满，子区域 B 容量充足
     * When: 车辆尝试进入子区域 B
     * Then: 请求应被拒绝（父区域容量限制）
     * And: 子区域容量应保持不变
     */
    @Test
    void testNestedArea_ParentFullPreventsChildEntry() {
        // Given: Parent (capacity 2) and child (capacity 5)
        ControlArea parentArea = new ControlArea("PARENT", null, 2);
        ControlArea childArea = new ControlArea("CHILD", parentArea, 5);

        // And: Parent at capacity (2/2)
        childArea.tryEnter(vehicle1); // Occupies parent: 1/2
        parentArea.tryEnter(vehicle2); // Occupies parent directly: 2/2
        assertThat(parentArea.getCurrentVehicleCount()).isEqualTo(2);
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(1);

        // When: Vehicle tries to enter child (parent is full)
        boolean canEnter = childArea.tryEnter(vehicle3);

        // Then: Should be rejected due to parent capacity
        assertThat(canEnter).isFalse();
        assertThat(parentArea.getCurrentVehicleCount()).isEqualTo(2); // Unchanged
        assertThat(childArea.getCurrentVehicleCount()).isEqualTo(1); // Unchanged
    }

    // ==================== Scenario 3: Statistics Tracking ====================

    /**
     * Test scenario: Control area statistics (REQ-TC-006)
     * <p>
     * Given: 一个 ControlArea
     * And: 多辆车辆进出
     * When: 查询区域统计
     * Then: 应返回当前车辆数、历史峰值、平均等待时间
     */
    @Test
    void testStatistics_TracksCurrentAndPeak() {
        // Given: ControlArea with capacity 5
        ControlArea statsArea = new ControlArea("STATS", null, 5);

        // When: Multiple vehicles enter and leave
        statsArea.tryEnter(vehicle1);
        statsArea.tryEnter(vehicle2);
        statsArea.tryEnter(vehicle3);
        assertThat(statsArea.getCurrentVehicleCount()).isEqualTo(3);

        statsArea.leave(vehicle1);
        assertThat(statsArea.getCurrentVehicleCount()).isEqualTo(2);

        statsArea.tryEnter(vehicle4);
        statsArea.tryEnter(vehicle5);
        assertThat(statsArea.getCurrentVehicleCount()).isEqualTo(4);

        // Then: Peak should be 4 (maximum concurrent vehicles)
        assertThat(statsArea.getPeakVehicleCount()).isEqualTo(4);

        // And: Current should be 4
        assertThat(statsArea.getCurrentVehicleCount()).isEqualTo(4);
    }

    /**
     * Test scenario: Average wait time tracking (REQ-TC-006)
     * <p>
     * Given: 一个 ControlArea
     * And: 车辆因容量满而等待
     * When: 查询统计
     * Then: 应正确计算平均等待时间（使用仿真时间）
     */
    @Test
    void testStatistics_AverageWaitTime() {
        // Given: ControlArea with capacity 1
        ControlArea waitArea = new ControlArea("WAIT-AREA", null, 1);

        // When: Vehicle 1 enters at simulation time 0
        waitArea.tryEnter(vehicle1);

        // Vehicle 2 tries to enter at simulation time 10, but rejected (waits)
        boolean rejected = waitArea.tryEnter(vehicle2);
        assertThat(rejected).isFalse();

        // Vehicle 1 leaves at simulation time 20
        waitArea.leave(vehicle1);

        // Vehicle 2 enters at simulation time 25 (waited 15 time units)
        waitArea.recordWaitTime(vehicle2, 15L);
        waitArea.tryEnter(vehicle2);

        // Then: Average wait time should be 15
        assertThat(waitArea.getAverageWaitTimeMs()).isEqualTo(15);
        assertThat(waitArea.getTotalWaitingVehicles()).isEqualTo(1);
    }

    /**
     * Test scenario: Reset statistics
     * <p>
     * Given: 一个有历史统计的 ControlArea
     * When: 重置统计
     * Then: 统计数据应清零，但不清除当前车辆
     */
    @Test
    void testStatistics_Reset() {
        // Given: Area with some activity
        area.tryEnter(vehicle1);
        area.tryEnter(vehicle2);
        area.tryEnter(vehicle3);
        assertThat(area.getCurrentVehicleCount()).isEqualTo(3);
        assertThat(area.getPeakVehicleCount()).isEqualTo(3);

        // Record some wait time
        area.recordWaitTime(vehicle1, 100);
        area.recordWaitTime(vehicle2, 200);

        // When: Reset statistics
        area.resetStatistics();

        // Then: Statistics should be reset but vehicles remain
        assertThat(area.getCurrentVehicleCount()).isEqualTo(3); // Vehicles still inside
        assertThat(area.getPeakVehicleCount()).isEqualTo(0);
        assertThat(area.getAverageWaitTimeMs()).isEqualTo(0);
        assertThat(area.getTotalWaitingVehicles()).isEqualTo(0);
    }

    /**
     * Test scenario: Get area info
     * <p>
     * Given: 一个 ControlArea
     * When: 获取区域信息
     * Then: 应返回完整的区域统计数据
     */
    @Test
    void testGetAreaInfo() {
        // Given: Area with some vehicles
        area.tryEnter(vehicle1);
        area.tryEnter(vehicle2);

        // When: Get area info
        String info = area.getAreaInfo();

        // Then: Should contain key statistics
        assertThat(info).contains("AREA-1");
        assertThat(info).contains("2"); // Current count
        assertThat(info).contains("3"); // Capacity
    }
}
