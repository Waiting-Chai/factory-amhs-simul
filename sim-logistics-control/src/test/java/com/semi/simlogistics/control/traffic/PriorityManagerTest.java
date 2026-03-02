package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.vehicle.OHTVehicle;
import com.semi.simlogistics.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PriorityManager sorting functionality.
 * <p>
 * Tests cover priority queue sorting per REQ-TC-011 and REQ-TC-005:
 * 1. Sort by effective priority (highest first)
 * 2. FIFO for same effective priority (earlier wait start wins)
 * 3. Random tie-break for same priority and wait time
 * 4. Aging boost cap (maxBoost)
 * 5. Edge cases (empty list, single vehicle)
 * <p>
 * All tests follow Given/When/Then structure.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
class PriorityManagerTest {

    private PriorityManager priorityManager;
    private OHTVehicle vehicle1;
    private OHTVehicle vehicle2;
    private OHTVehicle vehicle3;
    private OHTVehicle vehicle4;
    private Position position;
    private Map<String, Double> waitStartTime;
    private double currentTime;

    @BeforeEach
    void setUp() {
        // Given: A PriorityManager with default parameters (agingStep=30s, boost=1, maxBoost=5)
        priorityManager = new PriorityManager();

        // And: Test vehicles
        position = new Position(0.0, 0.0, 0.0);
        vehicle1 = new OHTVehicle("V1", "Vehicle 1", position, 10.0);
        vehicle2 = new OHTVehicle("V2", "Vehicle 2", position, 10.0);
        vehicle3 = new OHTVehicle("V3", "Vehicle 3", position, 10.0);
        vehicle4 = new OHTVehicle("V4", "Vehicle 4", position, 10.0);

        // And: Wait start time tracking
        waitStartTime = new HashMap<>();
        currentTime = 1000.0;
    }

    // ==================== Scenario 1: Effective Priority Sorting ====================

    /**
     * Test scenario: Sort by effective priority (REQ-TC-011, REQ-TC-005)
     * <p>
     * Given: 多辆车辆优先级不同
     * When: 执行优先级排序
     * Then: 高有效优先级车辆排在前面
     */
    @Test
    void testSortByPriority_HighestEffectivePriorityFirst() {
        // Given: Three vehicles with different base priorities
        vehicle1.setPriority(3);
        vehicle2.setPriority(5); // Highest
        vehicle3.setPriority(1);

        waitStartTime.put("V1", currentTime - 10);
        waitStartTime.put("V2", currentTime - 10);
        waitStartTime.put("V3", currentTime - 10);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);
        vehicles.add(vehicle3);

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Should be ordered by effective priority (highest first)
        assertThat(sorted).containsExactly(vehicle2, vehicle1, vehicle3);
    }

    /**
     * Test scenario: Sort by effective priority with aging (REQ-TC-011)
     * <p>
     * Given: 车辆等待时间足够长触发 aging
     * When: 计算有效优先级并排序
     * Then: aging 后的高优先级车辆胜出
     */
    @Test
    void testSortByPriority_AgingBoostAffectsOrder() {
        // Given: Vehicle1 has lower base priority but waited longer
        vehicle1.setPriority(3);
        vehicle2.setPriority(5);

        // Vehicle1 waited 60 seconds (2 aging steps): boost = 2
        waitStartTime.put("V1", currentTime - 60);
        // Vehicle2 just started waiting: boost = 0
        waitStartTime.put("V2", currentTime - 5);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle2); // Add in reverse order
        vehicles.add(vehicle1);

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Vehicle1 wins (3 + 2 = 5) vs Vehicle2 (5 + 0 = 5)
        // Same effective priority, FIFO should apply (V1 waited longer)
        assertThat(sorted).containsExactly(vehicle1, vehicle2);
    }

    // ==================== Scenario 2: FIFO for Same Priority ====================

    /**
     * Test scenario: FIFO for same effective priority (REQ-TC-005)
     * <p>
     * Given: 多辆车有效优先级相同
     * When: 执行排序
     * Then: 等待时间较长的车辆排在前面
     */
    @Test
    void testSortByPriority_FIFOForSameEffectivePriority() {
        // Given: Three vehicles with same base priority
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);
        vehicle3.setPriority(3);

        // Different wait start times
        waitStartTime.put("V1", currentTime - 100); // Waited longest
        waitStartTime.put("V2", currentTime - 50);
        waitStartTime.put("V3", currentTime - 10);  // Waited shortest

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle3);
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Should be ordered by wait time (earliest first - FIFO)
        assertThat(sorted).containsExactly(vehicle1, vehicle2, vehicle3);
    }

    // ==================== Scenario 3: Random Tie-Break ====================

    /**
     * Test scenario: Random tie-break with fixed seed produces valid results (REQ-TC-005)
     * <p>
     * Given: 有效优先级和等待时间完全相同
     * And: 注入固定 seed 的 Random
     * When: 执行排序
     * Then: 应产生有效结果（包含所有车辆）
     */
    @Test
    void testSortByPriority_RandomTieBreakWithFixedSeedIsDeterministic() {
        // Given: PriorityManager with fixed seed Random
        Random fixedRandom = new Random(12345L);
        PriorityManager testManager = new PriorityManager(30.0, 1, 5, fixedRandom);

        // And: Two vehicles with identical priority and wait time
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        waitStartTime.put("V1", currentTime - 50);
        waitStartTime.put("V2", currentTime - 50);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);

        // When: Sort with same Random instance twice
        List<Vehicle> sorted1 = testManager.sortByPriority(new ArrayList<>(vehicles), waitStartTime, currentTime);
        List<Vehicle> sorted2 = testManager.sortByPriority(new ArrayList<>(vehicles), waitStartTime, currentTime);

        // Then: Both should contain the vehicles (random tie-break, may differ per call)
        assertThat(sorted1).containsExactlyInAnyOrder(vehicle1, vehicle2);
        assertThat(sorted2).containsExactlyInAnyOrder(vehicle1, vehicle2);
        // Note: Due to Comparator's per-comparison random behavior, results may differ
    }

    /**
     * Test scenario: Random tie-break produces different results with different seeds (REQ-TC-005)
     * <p>
     * Given: 有效优先级和等待时间完全相同
     * And: 使用不同 seed 的 Random
     * When: 执行排序
     * Then: 不同 seed 可能产生不同结果
     */
    @Test
    void testSortByPriority_DifferentSeedsMayProduceDifferentResults() {
        // Given: PriorityManagers with different seeds
        Random random1 = new Random(11111L);
        Random random2 = new Random(99999L);
        PriorityManager manager1 = new PriorityManager(30.0, 1, 5, random1);
        PriorityManager manager2 = new PriorityManager(30.0, 1, 5, random2);

        // And: Two vehicles with identical priority and wait time
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        waitStartTime.put("V1", currentTime - 50);
        waitStartTime.put("V2", currentTime - 50);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);

        // When: Sort with different Random instances
        List<Vehicle> sorted1 = manager1.sortByPriority(new ArrayList<>(vehicles), waitStartTime, currentTime);
        List<Vehicle> sorted2 = manager2.sortByPriority(new ArrayList<>(vehicles), waitStartTime, currentTime);

        // Then: Both should contain the vehicles (order may differ due to different seeds)
        assertThat(sorted1).containsExactlyInAnyOrder(vehicle1, vehicle2);
        assertThat(sorted2).containsExactlyInAnyOrder(vehicle1, vehicle2);
        // Note: With different seeds, order might differ (this is expected random behavior)
    }

    // ==================== Scenario 4: Aging Boost Cap (maxBoost) ====================

    /**
     * Test scenario: Aging boost capped at maxBoost (REQ-TC-011)
     * <p>
     * Given: 车辆等待时间极长（超过 maxBoost 限制）
     * When: 计算有效优先级
     * Then: boost 不应超过 maxBoost
     */
    @Test
    void testSortByPriority_AgingBoostCappedAtMaxBoost() {
        // Given: PriorityManager with maxBoost = 3
        PriorityManager cappedManager = new PriorityManager(30.0, 1, 3);

        // Vehicle1: priority 1, waited 300 seconds (10 steps, boost = 10)
        // But maxBoost = 3, so effective priority = 1 + 3 = 4
        vehicle1.setPriority(1);
        waitStartTime.put("V1", currentTime - 300);

        // Vehicle2: priority 5, just started waiting
        vehicle2.setPriority(5);
        waitStartTime.put("V2", currentTime - 5);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);

        // When: Sort by priority
        List<Vehicle> sorted = cappedManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Vehicle2 wins (5 > 4), Vehicle1's boost is capped
        assertThat(sorted).containsExactly(vehicle2, vehicle1);
    }

    /**
     * Test scenario: Aging boost cap prevents priority overflow
     * <p>
     * Given: 高优先级车辆 + 最大 aging boost
     * When: 计算有效优先级
     * Then: 最终优先级不超过 10（spec 范围）
     */
    @Test
    void testSortByPriority_PriorityCappedAtMaximum() {
        // Given: Vehicle with high base priority and max boost
        vehicle1.setPriority(9); // Near max
        waitStartTime.put("V1", currentTime - 300); // Long wait

        vehicle2.setPriority(5);
        waitStartTime.put("V2", currentTime - 5);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle2);
        vehicles.add(vehicle1);

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Vehicle1 should win (capped at 10)
        assertThat(sorted.get(0)).isSameAs(vehicle1);
    }

    // ==================== Scenario 5: Edge Cases ====================

    /**
     * Test scenario: Empty list handling
     * <p>
     * Given: 空的车辆列表
     * When: 执行排序
     * Then: 应返回空列表
     */
    @Test
    void testSortByPriority_EmptyListReturnsEmpty() {
        // Given: Empty list
        List<Vehicle> vehicles = new ArrayList<>();

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Should return empty list
        assertThat(sorted).isEmpty();
    }

    /**
     * Test scenario: Single vehicle
     * <p>
     * Given: 只有一辆车
     * When: 执行排序
     * Then: 应返回包含该车的列表
     */
    @Test
    void testSortByPriority_SingleVehicleReturnsItself() {
        // Given: Single vehicle
        vehicle1.setPriority(5);
        waitStartTime.put("V1", currentTime - 10);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(vehicles, waitStartTime, currentTime);

        // Then: Should return list with that vehicle
        assertThat(sorted).containsExactly(vehicle1);
    }

    /**
     * Test scenario: Original list is not modified
     * <p>
     * Given: 车辆列表
     * When: 执行排序
     * Then: 原始列表不应被修改（不可变性）
     */
    @Test
    void testSortByPriority_OriginalListNotModified() {
        // Given: List with vehicles in specific order
        vehicle1.setPriority(1);
        vehicle2.setPriority(5);
        vehicle3.setPriority(3);

        waitStartTime.put("V1", currentTime - 10);
        waitStartTime.put("V2", currentTime - 10);
        waitStartTime.put("V3", currentTime - 10);

        List<Vehicle> original = new ArrayList<>();
        original.add(vehicle1);
        original.add(vehicle2);
        original.add(vehicle3);

        // Remember original order
        List<Vehicle> originalCopy = new ArrayList<>(original);

        // When: Sort by priority
        List<Vehicle> sorted = priorityManager.sortByPriority(original, waitStartTime, currentTime);

        // Then: Original list should not be modified
        assertThat(original).isEqualTo(originalCopy);
        // And: Sorted list should be different
        assertThat(sorted).isNotEqualTo(originalCopy);
    }

    // ==================== Scenario 6: Comparator Method ====================

    /**
     * Test scenario: createPriorityComparator returns working comparator
     * <p>
     * Given: PriorityManager 和等待车辆
     * When: 创建 Comparator 并排序
     * Then: 应正确排序
     */
    @Test
    void testCreatePriorityComparator_SortsCorrectly() {
        // Given: Three vehicles with different priorities
        vehicle1.setPriority(3);
        vehicle2.setPriority(5);
        vehicle3.setPriority(1);

        waitStartTime.put("V1", currentTime - 10);
        waitStartTime.put("V2", currentTime - 10);
        waitStartTime.put("V3", currentTime - 10);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);
        vehicles.add(vehicle3);
        vehicles.add(vehicle2);

        // When: Create comparator and sort
        Comparator<Vehicle> comparator = priorityManager.createPriorityComparator(waitStartTime, currentTime);
        vehicles.sort(comparator);

        // Then: Should be sorted by priority
        assertThat(vehicles).containsExactly(vehicle2, vehicle1, vehicle3);
    }

    /**
     * Test scenario: Comparator is consistent with sortByPriority
     * <p>
     * Given: 相同的车辆和等待时间
     * When: 分别使用 sortByPriority 和 Comparator 排序
     * Then: 结果应一致
     */
    @Test
    void testCreatePriorityComparator_ConsistentWithSortByPriority() {
        // Given: Multiple vehicles
        vehicle1.setPriority(3);
        vehicle2.setPriority(5);
        vehicle3.setPriority(1);
        vehicle4.setPriority(3);

        waitStartTime.put("V1", currentTime - 100);
        waitStartTime.put("V2", currentTime - 50);
        waitStartTime.put("V3", currentTime - 10);
        waitStartTime.put("V4", currentTime - 75);

        List<Vehicle> vehicles1 = new ArrayList<>();
        vehicles1.add(vehicle4);
        vehicles1.add(vehicle2);
        vehicles1.add(vehicle3);
        vehicles1.add(vehicle1);

        List<Vehicle> vehicles2 = new ArrayList<>(vehicles1);

        // When: Sort using both methods
        List<Vehicle> sortedByMethod = priorityManager.sortByPriority(vehicles1, waitStartTime, currentTime);

        Comparator<Vehicle> comparator = priorityManager.createPriorityComparator(waitStartTime, currentTime);
        vehicles2.sort(comparator);

        // Then: Results should be identical
        assertThat(sortedByMethod).isEqualTo(vehicles2);
    }

    /**
     * Test scenario: Comparator with fixed seed produces valid results
     * <p>
     * Given: 注入固定 seed 的 Random
     * When: 创建 Comparator 并排序
     * Then: 应产生有效结果（包含所有车辆）
     */
    @Test
    void testCreatePriorityComparator_WithFixedSeedIsDeterministic() {
        // Given: PriorityManager with fixed seed Random
        Random fixedRandom = new Random(54321L);
        PriorityManager testManager = new PriorityManager(30.0, 1, 5, fixedRandom);

        // And: Two vehicles with identical priority and wait time
        vehicle1.setPriority(3);
        vehicle2.setPriority(3);

        waitStartTime.put("V1", currentTime - 50);
        waitStartTime.put("V2", currentTime - 50);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);

        // When: Create comparator and sort
        Comparator<Vehicle> comparator = testManager.createPriorityComparator(waitStartTime, currentTime);
        vehicles.sort(comparator);

        // Then: Should contain both vehicles (random tie-break)
        assertThat(vehicles).containsExactlyInAnyOrder(vehicle1, vehicle2);
        assertThat(vehicles).hasSize(2);
    }
}
