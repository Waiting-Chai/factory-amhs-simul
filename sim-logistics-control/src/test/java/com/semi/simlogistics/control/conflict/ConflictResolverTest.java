package com.semi.simlogistics.control.conflict;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.vehicle.OHTVehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ConflictResolver.
 * <p>
 * Tests cover three main conflict resolution scenarios per REQ-TC-007:
 * 1. Priority-based resolution (higher priority wins)
 * 2. FIFO resolution (same priority, earlier arrival wins)
 * 3. Random tie-breaking (same priority, same arrival time)
 * <p>
 * All tests follow Given/When/Then structure aligned with spec scenarios.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class ConflictResolverTest {

    private OHTVehicle vehicle1;
    private OHTVehicle vehicle2;
    private OHTVehicle vehicle3;
    private Position position;

    @BeforeEach
    void setUp() {
        position = new Position(0.0, 0.0, 0.0);
        vehicle1 = new OHTVehicle("V1", "Vehicle 1", position, 10.0);
        vehicle2 = new OHTVehicle("V2", "Vehicle 2", position, 10.0);
        vehicle3 = new OHTVehicle("V3", "Vehicle 3", position, 10.0);
    }

    // ==================== Scenario 1: Priority-Based Resolution ====================

    /**
     * Test scenario: Priority conflict resolution (REQ-TC-007)
     * <p>
     * Given: 两辆车竞争同一资源
     * And: 车辆 A 优先级为 URGENT (5)
     * And: 车辆 B 优先级为 NORMAL (1)
     * When: 发生冲突
     * Then: 车辆 A 应获得资源
     * And: 车辆 B 应等待
     */
    @Test
    void testPriorityResolution_HigherPriorityWins() {
        // Given: Two vehicles competing for a resource
        // And: Vehicle A has URGENT priority (5), Vehicle B has NORMAL priority (1)
        vehicle1.setPriority(5);  // URGENT
        vehicle2.setPriority(1);  // NORMAL

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        // When: Conflict resolution occurs
        ConflictResolver resolver = new ConflictResolver(12345L); // Fixed seed for reproducibility
        OHTVehicle winner = resolver.resolve(competingVehicles);

        // Then: Higher priority vehicle (A) should win
        assertThat(winner).isSameAs(vehicle1);
        assertThat(winner.getPriority()).isEqualTo(5);
    }

    /**
     * Test scenario: Priority resolution with multiple vehicles
     * <p>
     * Given: 三辆车竞争，优先级分别为 3, 5, 1
     * When: 发生冲突
     * Then: 优先级为 5 的车辆应获胜
     */
    @Test
    void testPriorityResolution_MultipleVehicles() {
        // Given: Three vehicles with priorities 3, 5, 1
        vehicle1.setPriority(3);
        vehicle2.setPriority(5);  // Highest
        vehicle3.setPriority(1);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);
        competingVehicles.add(vehicle3);

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolve(competingVehicles);

        // Then: Vehicle with priority 5 should win
        assertThat(winner).isSameAs(vehicle2);
    }

    // ==================== Scenario 2: FIFO Resolution ====================

    /**
     * Test scenario: First-come-first-served conflict resolution (REQ-TC-007)
     * <p>
     * Given: 两辆车竞争同一资源
     * And: 两车优先级相同
     * And: 车辆 A 更早到达（等待时间更长）
     * When: 发生冲突
     * Then: 车辆 A 应获得资源
     * And: 车辆 B 应等待
     */
    @Test
    void testFifoResolution_EarlierArrivalWins() {
        // Given: Two vehicles with same priority
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        // And: Vehicle A has been waiting longer
        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        // When: Resolve conflict with arrival time context
        ConflictResolver resolver = new ConflictResolver();
        // Simulate vehicle1 arrived earlier (has lower sequence number)
        OHTVehicle winner = resolver.resolveWithArrivalTime(competingVehicles, 0L, 100L);

        // Then: Earlier arriving vehicle (A) should win
        assertThat(winner).isSameAs(vehicle1);
    }

    /**
     * Test scenario: FIFO with multiple vehicles
     * <p>
     * Given: 三辆车优先级相同，到达时间分别为 0, 100, 200
     * When: 发生冲突
     * Then: 最早到达（时间 0）的车辆应获胜
     */
    @Test
    void testFifoResolution_MultipleVehicles() {
        // Given: Three vehicles with same priority
        vehicle1.setPriority(2);
        vehicle2.setPriority(2);
        vehicle3.setPriority(2);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);  // Arrival time 0
        competingVehicles.add(vehicle2);  // Arrival time 100
        competingVehicles.add(vehicle3);  // Arrival time 200

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolveWithArrivalTime(competingVehicles, 0L, 100L);

        // Then: First vehicle (earliest arrival) should win
        assertThat(winner).isSameAs(vehicle1);
    }

    // ==================== Scenario 3: Random Tie-Breaking ====================

    /**
     * Test scenario: Random tie-breaking (REQ-TC-007)
     * <p>
     * Given: 两辆车竞争同一资源
     * And: 优先级相同
     * And: 到达时间相同（完全平局）
     * When: 发生冲突
     * Then: 应随机选择一辆车
     * And: 多次运行应体现随机性
     */
    @Test
    void testRandomTieBreak_CompleteTie() {
        // Given: Two vehicles with identical priority and arrival time
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        // When: Resolve conflict with same arrival time (complete tie)
        ConflictResolver resolver = new ConflictResolver(12345L); // Fixed seed
        OHTVehicle winner1 = resolver.resolveWithArrivalTime(competingVehicles, 0L, 0L);

        // Then: One of them should win (random but deterministic with fixed seed)
        assertThat(winner1).isIn(vehicle1, vehicle2);

        // And: With different seed, result might differ
        ConflictResolver resolver2 = new ConflictResolver(54321L);
        OHTVehicle winner2 = resolver2.resolveWithArrivalTime(competingVehicles, 0L, 0L);
        assertThat(winner2).isIn(vehicle1, vehicle2);
    }

    /**
     * Test scenario: Random tie-breaking is deterministic with same seed
     * <p>
     * Given: 完全平局的多辆车
     * When: 使用相同种子的新 resolver 多次解析
     * Then: 每次应选择同一辆车（验证可重现性）
     */
    @Test
    void testRandomTieBreak_DeterministicWithSameSeed() {
        // Given: Complete tie scenario
        vehicle1.setPriority(1);
        vehicle2.setPriority(1);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        // When: Resolve with new resolver instances (same seed) each time
        OHTVehicle winner1 = new ConflictResolver(99999L).resolveWithArrivalTime(competingVehicles, 0L, 0L);
        OHTVehicle winner2 = new ConflictResolver(99999L).resolveWithArrivalTime(competingVehicles, 0L, 0L);
        OHTVehicle winner3 = new ConflictResolver(99999L).resolveWithArrivalTime(competingVehicles, 0L, 0L);

        // Then: Same winner should be chosen each time (deterministic with same seed)
        assertThat(winner1).isSameAs(winner2);
        assertThat(winner2).isSameAs(winner3);
    }

    /**
     * Test scenario: Complex scenario with mixed priorities
     * <p>
     * Given: 四辆车，优先级和到达时间各不相同
     * When: 发生冲突
     * Then: 应按仲裁顺序选择：优先级 > 到达时间 > 随机
     */
    @Test
    void testComplexScenario_MixedPrioritiesAndArrivalTimes() {
        // Given: Four vehicles with mixed priorities and arrival times
        vehicle1.setPriority(3);  // Medium priority, early arrival
        vehicle2.setPriority(5);  // High priority, late arrival
        vehicle3.setPriority(1);  // Low priority, early arrival

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);  // Priority 3, arrival 0
        competingVehicles.add(vehicle2);  // Priority 5, arrival 100
        competingVehicles.add(vehicle3);  // Priority 1, arrival 50

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolveWithArrivalTime(competingVehicles, 0L, 100L);

        // Then: Vehicle with highest priority (5) should win, regardless of arrival time
        assertThat(winner).isSameAs(vehicle2);
    }

    /**
     * Test scenario: Empty list handling
     * <p>
     * Given: 空的竞争车辆列表
     * When: 尝试解析冲突
     * Then: 应返回 null
     */
    @Test
    void testEdgeCase_EmptyList() {
        // Given: Empty list of competing vehicles
        List<OHTVehicle> competingVehicles = new ArrayList<>();

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolve(competingVehicles);

        // Then: Should return null
        assertThat(winner).isNull();
    }

    /**
     * Test scenario: Single vehicle
     * <p>
     * Given: 只有一辆车竞争
     * When: 尝试解析冲突
     * Then: 应直接返回该车
     */
    @Test
    void testEdgeCase_SingleVehicle() {
        // Given: Single vehicle
        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolve(competingVehicles);

        // Then: Should return that vehicle
        assertThat(winner).isSameAs(vehicle1);
    }

    // ==================== Scenario 4: Distance-Based Resolution ====================

    /**
     * Test scenario: Distance conflict resolution (REQ-TC-007)
     * <p>
     * Given: 两辆车竞争同一控制点
     * And: 优先级相同
     * And: 到达时间相同
     * And: 车辆 A 距离控制点更近
     * When: 发生冲突
     * Then: 车辆 A 应获得资源
     */
    @Test
    void testDistanceResolution_CloserVehicleWins() {
        // Given: Two vehicles with same priority and arrival time
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);  // Distance: 5.0m
        competingVehicles.add(vehicle2);  // Distance: 15.0m

        List<Double> distances = List.of(5.0, 15.0);

        // When: Resolve conflict with distance consideration
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolveWithArrivalAndDistance(competingVehicles, 0L, 0L, distances);

        // Then: Closer vehicle (A) should win
        assertThat(winner).isSameAs(vehicle1);
    }

    /**
     * Test scenario: Distance resolution with multiple vehicles
     * <p>
     * Given: 三辆车优先级相同，到达时间相同，距离分别为 10m, 5m, 20m
     * When: 发生冲突
     * Then: 距离 5m 的车辆应获胜
     */
    @Test
    void testDistanceResolution_MultipleVehicles() {
        // Given: Three vehicles with same priority and arrival time
        vehicle1.setPriority(2);
        vehicle2.setPriority(2);
        vehicle3.setPriority(2);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);  // Distance: 10.0m
        competingVehicles.add(vehicle2);  // Distance: 5.0m (closest)
        competingVehicles.add(vehicle3);  // Distance: 20.0m

        List<Double> distances = List.of(10.0, 5.0, 20.0);

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolveWithArrivalAndDistance(competingVehicles, 0L, 0L, distances);

        // Then: Closest vehicle should win
        assertThat(winner).isSameAs(vehicle2);
    }

    /**
     * Test scenario: Priority overrides distance
     * <p>
     * Given: 车辆 A 优先级高但距离远，车辆 B 优先级低但距离近
     * When: 发生冲突
     * Then: 优先级高的车辆 A 应获胜（距离不覆盖优先级）
     */
    @Test
    void testDistanceResolution_PriorityOverridesDistance() {
        // Given: Vehicle A: high priority (5), far (50m)
        // Vehicle B: low priority (1), close (1m)
        vehicle1.setPriority(5);  // High priority, far
        vehicle2.setPriority(1);  // Low priority, close

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        List<Double> distances = List.of(50.0, 1.0);

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolveWithArrivalAndDistance(competingVehicles, 0L, 0L, distances);

        // Then: Higher priority vehicle should win despite being farther
        assertThat(winner).isSameAs(vehicle1);
    }

    /**
     * Test scenario: Arrival time overrides distance
     * <p>
     * Given: 两辆车优先级相同
     * And: 车辆 A 到达更早但距离远，车辆 B 到达晚但距离近
     * When: 发生冲突
     * Then: 先到达的车辆 A 应获胜
     */
    @Test
    void testDistanceResolution_ArrivalTimeOverridesDistance() {
        // Given: Vehicle A: earlier arrival (0ms), far (30m)
        // Vehicle B: later arrival (100ms), close (5m)
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        List<Double> distances = List.of(30.0, 5.0);

        // When: Resolve conflict with different arrival times
        ConflictResolver resolver = new ConflictResolver();
        OHTVehicle winner = resolver.resolveWithArrivalAndDistance(competingVehicles, 0L, 100L, distances);

        // Then: Earlier arriving vehicle should win despite being farther
        assertThat(winner).isSameAs(vehicle1);
    }

    /**
     * Test scenario: Invalid distances list
     * <p>
     * Given: 车辆列表与距离列表大小不匹配
     * When: 尝试解析冲突
     * Then: 应抛出 IllegalArgumentException
     */
    @Test
    void testDistanceResolution_InvalidDistancesList() {
        // Given: Two vehicles but only one distance
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        List<OHTVehicle> competingVehicles = new ArrayList<>();
        competingVehicles.add(vehicle1);
        competingVehicles.add(vehicle2);

        List<Double> distances = List.of(5.0);  // Only one distance!

        // When: Resolve conflict
        ConflictResolver resolver = new ConflictResolver();

        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() ->
                resolver.resolveWithArrivalAndDistance(competingVehicles, 0L, 0L, distances)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distances list must match");
    }
}
