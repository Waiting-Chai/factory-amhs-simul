# 复杂工艺流程与自动任务生成设计方案

> **文档版本**: v1.5 (第五次修订版)
> **创建日期**: 2026-02-06
> **更新日期**: 2026-02-06
> **作者**: shentw
> **状态**: 设计提案阶段（可进入 spec/落地）
>
> **v1.5 修订说明（一致性问题修正）**：
> - 修正 SimulationEnv.init 示例与实现不一致（init 返回 void，需用 getCurrent() 获取实例）
> - 修正 SafetyZone/ControlPoint 容量描述冲突（改为"可配置，默认 1"）
> - 修正迁移脚本命名符合 Flyway 规则（V001→V2，V002→V3，在 V1 之后）
> - 明确 SafetyZone 与实体 Schema 对齐（选择实体类型方案，需在 logistics-entities spec 中补充定义）
>
> **v1.4 修订说明**：
> - 补充 SafetyZone 与 ControlPoint/Edge 职责分离说明（含 4 个典型场景）
> - 补充 SimulationEnv.setTime/advanceTime 约束（仅测试用、业务禁用、用 schedule 驱动）
> - 补充数据库 DDL 更新计划（含完整 V001/V002 迁移脚本和回滚脚本）
>
> **v1.3 修订说明**：
> - 补齐多租户支持（tenant_id 字段、联合唯一键、多租户索引）
> - 新增 SafetyZone 数据结构完整定义（id、几何形状、容量、优先级等）
> - 修正 MaterialBindingManager 使用仿真时间（env.now()）而非墙钟时间
> - 新增资源可用性校验策略配置（BLOCK_AND_RETRY vs ENQUEUE_AND_WAIT）
> - 明确 TriggerCondition.entryPointId 与绑定表 entryPointId 的一致性规则
> - 统一登记 system_config 配置项清单（重试、资源、WIP、Human、SafetyZone 等）
>
> **v1.2 修订说明**：
> - 修正 Human 冲突仲裁逻辑（仅在 SafetyZone 仲裁，不在 ControlPoint 进行人车仲裁）
> - 修正多流程并行执行的物料级互斥策略
> - 扩展 EventTrigger 条件匹配机制（支持物料属性、批次、工艺标记等）
> - 补充数据库表设计与 database-schema 对齐说明
> - 明确统一随机源接口获取方式（SimulationEnv）
> - 将 DEFER_RETRY 退避策略改为可配置参数（RetryConfig）

---

## 目录

- [一、竞品调研总结](#一竞品调研总结)
- [二、模块边界设计](#二模块边界设计)
- [三、核心数据结构设计](#三核心数据结构设计)
- [四、设计方案详解](#四设计方案详解)
- [五、关键设计决策澄清（新增）](#五关键设计决策澄清)
- [六、需补充的 Spec 章节](#六需补充的-spec-章节)
- [七、完整流程示意](#七完整流程示意)
- [八、调研引用来源](#八调研引用来源)

---

## 一、竞品调研总结

### 1.1 FlexSim Process Flow 核心能力

- **Process Flow 类型**：通用流程、固定资源流程、实体流
- **Lists & Queue**：动态列表管理任务序列
- **任务序列**：可组合的原子操作（装载、移动、卸载）
- **路由逻辑**：基于属性/标签的智能路由
- **新特性**：Container Object、Task Sequence Queues（2025）

### 1.2 AnyLogic 混合建模

- **Agent + Process Flow**：Agent 行为与流程图结合
- **资源跟踪**：Flow Chart 中跟踪 Agent 和资源
- **随机路由**：支持动态路径选择

### 1.3 Siemens Plant Simulation 工艺链

- **Process Chain**：顺序工艺链建模
- **Source/Drain**：参数化生产比率、批量大小
- **虚拟调试**：数字孪生提升吞吐 15%、效率 10-20%

### 1.4 学术研究趋势（2025）

- **传感器驱动的自动化**：基于监控数据自动生成仿真模型
- **实时事件处理**：设备故障、工具问题的事件驱动响应
- **生产布局优化**：基于仿真的产线布局优化

---

## 二、模块边界设计

### 2.1 新增模块：`sim-logistics-workflow`

```
sim-logistics-workflow (新增)
├── core/                          # 核心模型
│   ├── ProcessFlow               # 工艺流程定义
│   ├── ProcessStep               # 工序定义
│   ├── RoutingRule               # 路由规则
│   └── OutputStrategy            # 产出策略
├── generator/                     # 任务生成器
│   ├── EventTrigger              # 事件触发器
│   ├── TaskGenerator             # 任务生成器
│   └── PullMechanism             # 拉动机制
├── executor/                      # 执行引擎
│   ├── WorkflowExecutor          # 流程执行器
│   ├── StepExecutor              # 工序执行器
│   └── TransportSelector         # 搬运主体选择器
└── policy/                        # 策略接口
    ├── RoutingPolicy             # 路由策略
    ├── OutputPolicy              # 产出策略
    └── TransportPolicy           # 搬运策略
```

### 2.2 与现有模块的关系

| 模块 | 依赖关系 | 说明 |
|------|----------|------|
| `sim-logistics-core` | 依赖 | 使用 LogisticsEntity、Vehicle 等 |
| `sim-logistics-control` | 依赖 | 使用 TrafficManager 控制交通 |
| `sim-logistics-scheduler` | 双向 | TaskQueue 既可手动添加也可自动生成 |
| `sim-logistics-metrics` | 支持 | 工艺流转时间、吞吐统计 |

### 2.3 职责边界图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           模块职责边界清晰定义                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────┐ │
│  │   sim-logistics-    │    │   sim-logistics-    │    │  sim-logistics- │ │
│  │     workflow        │    │     scheduler       │    │     control     │ │
│  │                     │    │                     │    │                 │ │
│  │  【只生成任务】      │    │  【只派单】          │    │  【只管交通】    │ │
│  │                     │    │                     │    │                 │ │
│  │  - ProcessFlow      │───▶│  - TaskQueue        │◀───│  - TrafficMgr    │ │
│  │  - EventTrigger     │    │  - DispatchEngine   │    │  - PathPlanner   │ │
│  │  - TaskGenerator    │    │  - DispatchRule     │    │  - CtrlPoint     │ │
│  │                     │    │                     │    │                 │ │
│  │  【不参与派单】      │    │  【不生成任务】      │    │  【不生成任务】  │ │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────┘ │
│           │                           │                           │         │
│           │ writes                    │ reads                     │ reads   │
│           ▼                           ▼                           ▼         │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         TaskQueue (共享)                             │   │
│  │                                                                     │   │
│  │   Workflow ──[enqueue]──▶ TaskQueue ◀──[dequeue]── Scheduler        │   │
│  │        │                      │                      ▲               │   │
│  │        │                      │                      │               │   │
│  │        └──────[手动任务]──────┴──────[自动任务]──────┘               │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  数据流向（单向，避免循环依赖）：                                            │
│  ┌─────────┐      ┌──────────┐      ┌───────────┐      ┌──────────┐      │
│  │ Event   │────▶ │ Workflow │────▶ │TaskQueue  │────▶ │Scheduler │      │
│  │ Source  │      │ Generator│      │           │      │          │      │
│  └─────────┘      └──────────┘      └───────────┘      └──────────┘      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.4 职责分工表

| 职责项 | Workflow 模块 | Scheduler 模块 | Control 模块 |
|--------|---------------|----------------|--------------|
| **任务生成** | ✅ 负责（自动生成） | ❌ 不参与 | ❌ 不参与 |
| **任务入队** | ✅ 写入 TaskQueue | ❌ 不操作 | ❌ 不操作 |
| **任务派单** | ❌ 不参与 | ✅ 负责 | ❌ 不参与 |
| **任务出队** | ❌ 不操作 | ✅ 读取 TaskQueue | ❌ 不操作 |
| **资源选择** | ⚠️ 建议优先级 | ✅ 最终决策 | ❌ 不参与 |
| **交通管制** | ❌ 不参与 | ⚠️ 发起请求 | ✅ 负责 |
| **手动任务** | ❌ 不处理 | ✅ 接收 Web/API | ❌ 不处理 |
| **流程定义** | ✅ 管理生命周期 | ❌ 不处理 | ❌ 不处理 |

---

## 三、核心数据结构设计

### 3.1 ProcessFlow（工艺流程）

```java
/**
 * Process flow definition for production workflow.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public class ProcessFlow {
    private String id;
    private String name;
    private String version;                     // 语义化版本 (1.0.0)
    private List<ProcessStep> steps;            // 工序列表（顺序）
    private OutputStrategy defaultOutputStrategy; // 默认产出策略
    private TransportSelector defaultTransportSelector; // v2.0: 默认搬运选择器
    private boolean enabled;
    private Map<String, Object> properties;     // 扩展属性
    private String parentVersionId;             // 父版本（用于追溯）
}
```

### 3.2 ProcessStep（工序，v2.0）

```java
/**
 * Single process step in a workflow.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public class ProcessStep {
    private String id;
    private String name;
    private int sequence;                       // 顺序号（0, 1, 2...）

    // 工序配置（v2.0: 多候选设备支持）
    private List<String> targetEntityIds;       // 目标机台/设备 ID 列表（多候选）
    private List<String> requiredTransportTypes; // 必需的运输类型（如 OHT, AGV, HUMAN）
    private TimeDistribution processingTime;     // 处理时间分布
    private int wipLimit;                       // WIP 限制（0=无限制）
    private BatchRule batchRule;                // 批量规则

    // 产出配置
    private OutputStrategy outputStrategy;      // 覆盖流程级策略

    // 搬运配置
    private TransportSelector transportSelector; // 覆盖流程级选择器

    // 上下游约束
    private List<String> predecessorIds;        // 前置工序 ID
    private String successorId;                 // 后置工序 ID
}
```

#### 3.2.1 多候选设备选择算法（v2.0）

```java
/**
 * Multi-factor weighted device selector.
 *
 * Uses min-max normalization for fair comparison across factors.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public class MultiFactorDeviceSelector {

    /**
     * Select best device from candidates using weighted scoring.
     *
     * Score = w_distance * (1 - normDistance)
     *       + w_time * (1 - normTime)
     *       + w_wip * (1 - normWip)
     *
     * @param candidates Candidate device IDs
     * @param source Source position
     * @param weights Selection weights (from system_config)
     * @return Selected device ID
     */
    public String selectDevice(List<String> candidates, Position source,
                              SelectorWeights weights) {

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No candidate devices");
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // Collect metrics for all candidates
        List<DeviceMetrics> metrics = candidates.stream()
            .map(id -> new DeviceMetrics(id,
                calculateDistance(source, getPosition(id)),
                calculateEstimatedTime(source, getPosition(id)),
                getCurrentWip(id)))
            .collect(Collectors.toList());

        // Min-max normalization
        double minDistance = metrics.stream().mapToDouble(DeviceMetrics::getDistance).min().orElse(0);
        double maxDistance = metrics.stream().mapToDouble(DeviceMetrics::getDistance).max().orElse(1);
        double minTime = metrics.stream().mapToDouble(DeviceMetrics::getTime).min().orElse(0);
        double maxTime = metrics.stream().mapToDouble(DeviceMetrics::getTime).max().orElse(1);
        double minWip = metrics.stream().mapToDouble(DeviceMetrics::getWip).min().orElse(0);
        double maxWip = metrics.stream().mapToDouble(DeviceMetrics::getWip).max().orElse(1);

        // Calculate scores and select best
        return metrics.stream()
            .max(Comparator.comparingDouble(m -> calculateScore(m,
                minDistance, maxDistance,
                minTime, maxTime,
                minWip, maxWip, weights)))
            .map(DeviceMetrics::getId)
            .orElse(candidates.get(0));
    }

    private double calculateScore(DeviceMetrics m,
                                 double minD, double maxD,
                                 double minT, double maxT,
                                 double minW, double maxW,
                                 SelectorWeights w) {
        double normDistance = (maxD - minD) > 0 ? (m.distance - minD) / (maxD - minD) : 0;
        double normTime = (maxT - minT) > 0 ? (m.time - minT) / (maxT - minT) : 0;
        double normWip = (maxW - minW) > 0 ? (m.wip - minW) / (maxW - minW) : 0;

        return w.distanceWeight * (1 - normDistance)
             + w.timeWeight * (1 - normTime)
             + w.wipWeight * (1 - normWip);
    }
}

public class SelectorWeights {
    private double distanceWeight;  // Default: 0.4 (from system_config)
    private double timeWeight;      // Default: 0.4
    private double wipWeight;       // Default: 0.2
}
```

#### 3.2.2 建模阶段强校验（v2.0）

**校验规则**: 设备支持的运输类型与工序要求的运输类型交集为空时，禁止保存/更新。

```java
/**
 * Validate transport type compatibility between step and devices.
 *
 * @param step ProcessStep with requiredTransportTypes
 * @param devices List of candidate devices with supportedTransportTypes
 * @throws ValidationException if intersection is empty
 */
public void validateTransportTypeCompatibility(ProcessStep step,
                                              List<LogisticsEntity> devices) {
    Set<String> requiredTypes = new HashSet<>(step.getRequiredTransportTypes());
    Set<String> supportedTypes = new HashSet<>();

    for (LogisticsEntity device : devices) {
        if (device.getSupportedTransportTypes() != null) {
            supportedTypes.addAll(device.getSupportedTransportTypes());
        }
    }

    // Calculate intersection
    Set<String> intersection = new HashSet<>(requiredTypes);
    intersection.retainAll(supportedTypes);

    if (intersection.isEmpty()) {
        throw new ValidationException(String.format(
            "Transport type mismatch: step requires %s but devices support %s",
            requiredTypes, supportedTypes));
    }
}
```

### 3.3 TimeDistribution（时间分布，v1.2 修正版）

```java
/**
 * Time distribution for stochastic processing time.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class TimeDistribution {
    public enum DistributionType {
        NORMAL, LOGNORMAL, EXPONENTIAL,
        TRIANGULAR, FIXED, UNIFORM
    }

    private DistributionType type;
    private Map<String, Double> params;         // 分布参数

    // 统一随机源接口（v1.2: 明确获取方式）
    private final Random random;

    /**
     * Constructor with unified random source.
     *
     * @param type Distribution type
     * @param params Distribution parameters
     * @param random Unified random source from simulation environment
     */
    public TimeDistribution(DistributionType type, Map<String, Double> params, Random random) {
        this.type = type;
        this.params = params;
        this.random = random;
    }

    /**
     * Factory method using simulation environment.
     *
     * This is the RECOMMENDED way to create TimeDistribution instances.
     * It ensures all random sources are seeded consistently for reproducibility.
     *
     * Usage:
     *   TimeDistribution dist = TimeDistribution.create(
     *       DistributionType.NORMAL,
     *       Map.of("mean", 50.0, "std", 5.0),
     *       env.getRandom("processing-time")  // Seed key for this distribution
     *   );
     *
     * @param type Distribution type
     * @param params Distribution parameters
     * @param seedKey Unique seed key for this random source
     * @return TimeDistribution instance
     */
    public static TimeDistribution create(DistributionType type,
                                          Map<String, Double> params,
                                          String seedKey) {
        // Get random source from simulation environment
        SimulationEnv env = SimulationEnv.getCurrent();
        Random random = env.getRandom(seedKey);  // Seeded by seedKey
        return new TimeDistribution(type, params, random);
    }

    public double nextSample() {
        switch (type) {
            case NORMAL:
                return random.nextGaussian() * params.get("std") + params.get("mean");
            case EXPONENTIAL:
                return -Math.log(1 - random.nextDouble()) / params.get("lambda");
            case TRIANGULAR:
                return triangularSample(
                    params.get("min"),
                    params.get("mode"),
                    params.get("max")
                );
            case UNIFORM:
                double a = params.get("a");
                double b = params.get("b");
                return a + (b - a) * random.nextDouble();
            case FIXED:
                return params.get("value");
            case LOGNORMAL:
                double logMean = params.get("logMean");
                double logStd = params.get("logStd");
                return Math.exp(random.nextGaussian() * logStd + logMean);
            default:
                throw new IllegalStateException("Unknown distribution: " + type);
        }
    }

    private double triangularSample(double min, double mode, double max) {
        double u = random.nextDouble();
        double c = (mode - min) / (max - min);
        if (u < c) {
            return min + Math.sqrt(u * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1 - u) * (max - min) * (max - mode));
        }
    }
}

/**
 * Simulation environment (singleton) providing unified random sources.
 *
 * Key features:
 * - Seeded random sources for reproducibility
 * - Seed key based lookup (e.g., "processing-time", "human-fatigue")
 * - Consistent across simulation runs with same seed
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class SimulationEnv {

    private static SimulationEnv instance;
    private final long globalSeed;
    private final Map<String, Random> randomSources = new ConcurrentHashMap<>();

    private SimulationEnv(long globalSeed) {
        this.globalSeed = globalSeed;
    }

    /**
     * Get current simulation environment instance.
     */
    public static SimulationEnv getCurrent() {
        if (instance == null) {
            throw new IllegalStateException("SimulationEnv not initialized. Call init(seed) first.");
        }
        return instance;
    }

    /**
     * Initialize simulation environment with global seed.
     *
     * @param globalSeed Global seed for the entire simulation
     */
    public static void init(long globalSeed) {
        instance = new SimulationEnv(globalSeed);
    }

    /**
     * Get random source for a specific seed key.
     *
     * Seed key naming conventions:
     * - "processing-time-{stepId}" : Processing time distributions
     * - "human-fatigue-{humanId}" : Human fatigue factors
     * - "transport-time-{routeId}" : Transport time variations
     * - "batch-size-{stepId}" : Batch size variations
     *
     * The same seed key will always return the same Random instance,
     * ensuring reproducibility across simulation runs.
     *
     * @param seedKey Unique seed key
     * @return Random instance with deterministic seed derived from globalSeed
     */
    public Random getRandom(String seedKey) {
        return randomSources.computeIfAbsent(seedKey, key -> {
            // Derive deterministic seed from global seed and key
            long derivedSeed = globalSeed + hashString(key);
            return new Random(derivedSeed);
        });
    }

    /**
     * Hash string to long for seed derivation.
     */
    private long hashString(String str) {
        long hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = 31L * hash + str.charAt(i);
        }
        return hash;
    }

    /**
     * Reset all random sources (useful for restarting simulation).
     */
    public void reset() {
        randomSources.clear();
    }

    // Simulation time management (v1.3: added for MaterialBindingManager)

    private double currentTime = 0.0;  // Current simulation time in seconds

    /**
     * Get current simulation time.
     *
     * @return Current simulation time in seconds
     */
    public double now() {
        return currentTime;
    }

    /**
     * Advance simulation time.
     *
     * ⚠️ USAGE CONSTRAINTS (v1.3):
     * - FORBIDDEN in production/business code
     * - ONLY for testing and replay scenarios
     * - In production, time MUST be advanced via SimulationEngine.scheduleEvent()
     *
     * Why this constraint?
     * - Direct time advancement bypasses event scheduling
     * - Can break causality in DES (Discrete Event Simulation)
     * - Should only be used in unit tests for time manipulation
     *
     * @param deltaSeconds Time to advance in seconds
     * @throws IllegalStateException if called in non-test mode
     */
    public void advanceTime(double deltaSeconds) {
        if (!isTestMode()) {
            throw new IllegalStateException(
                "advanceTime() is FORBIDDEN in production. Use SimulationEngine.scheduleEvent() instead.");
        }
        this.currentTime += deltaSeconds;
    }

    /**
     * Set simulation time to a specific value.
     *
     * ⚠️ USAGE CONSTRAINTS (v1.3):
     * - FORBIDDEN in production/business code
     * - ONLY for testing and replay scenarios
     * - In production, time MUST be advanced via SimulationEngine.scheduleEvent()
     *
     * Why this constraint?
     * - Direct time setting bypasses event scheduling
     * - Can break causality in DES (Discrete Event Simulation)
     * - Should only be used in unit tests for snapshot testing
     *
     * @param time Simulation time in seconds
     * @throws IllegalStateException if called in non-test mode
     */
    public void setTime(double time) {
        if (!isTestMode()) {
            throw new IllegalStateException(
                "setTime() is FORBIDDEN in production. Use SimulationEngine.scheduleEvent() instead.");
        }
        this.currentTime = time;
    }

    /**
     * Check if running in test mode.
     *
     * Test mode is enabled via system property: simulation.test.mode=true
     *
     * @return true if in test mode
     */
    private boolean isTestMode() {
        return Boolean.getBoolean("simulation.test.mode");
    }
}
```

> **仿真时间推进约束（v1.3 重要修正）**：
>
> **关键原则**：仿真时间必须由事件调度驱动，禁止直接推进。
>
> 1. **生产环境（业务运行）**
>    - ❌ **禁止** 调用 `env.setTime()` 或 `env.advanceTime()`
>    - ✅ **必须** 通过 `SimulationEngine.scheduleEvent(time, callback)` 驱动时间
>    - 理由：DES 引擎通过事件队列维护因果性，直接推进时间会破坏事件顺序
>
> 2. **测试/回放环境**
>    - ✅ **允许** 调用 `env.setTime()` 和 `env.advanceTime()`
>    - 需设置系统属性：`-Dsimulation.test.mode=true`
>    - 用途：单元测试、快照测试、场景回放
>
> 3. **正确的时间推进方式（生产）**
>    ```java
>    // ✅ 正确：通过事件调度推进时间
>    simulationEngine.scheduleEvent(eventTime, () -> {
>        // At this point, env.now() == eventTime
>        handleEvent(event);
>    });
>
>    // ❌ 错误：直接推进时间（生产环境禁用）
>    // env.advanceTime(100.0);  // Throws IllegalStateException
>    ```
>
> 4. **测试环境示例**
>    ```java
>    @Test
>    public void testMaterialBindingTimeout() {
>        System.setProperty("simulation.test.mode", "true");
>        SimulationEnv.init(12345L);  // Initialize environment
>        SimulationEnv env = SimulationEnv.getCurrent();  // Get instance
>
>        // 测试代码可以安全地操纵时间
>        env.setTime(100.0);
>        // Test timeout behavior at t=100
>        env.advanceTime(50.0);
>        // Test at t=150
>    }
>    ```
>
> **统一随机源接口说明（v1.2 新增）**：
>
> 为保证仿真可复现性（reproducibility），所有随机数必须从统一的随机源获取：
>
> 1. **初始化方式**
>    ```java
>    // 在仿真开始时，用全局种子初始化环境
>    SimulationEnv.init(12345L);  // 全局种子（init 返回 void）
>    SimulationEnv env = SimulationEnv.getCurrent();  // 获取实例
>    ```
>
> 2. **获取随机源**
>    ```java
>    // 推荐方式：通过 seedKey 获取
>    Random random = env.getRandom("processing-time-Machine-A");
>    TimeDistribution dist = new TimeDistribution(type, params, random);
>    ```
>
> 3. **Seed Key 命名规范**
>    - `processing-time-{stepId}` : 工序处理时间
>    - `human-fatigue-{humanId}` : 人员疲劳因子
>    - `transport-time-{routeId}` : 运输时间变化
>    - `batch-size-{stepId}` : 批量大小变化
>    - `routing-choice-{stepId}` : 路由选择概率
>
> 4. **可复现性保证**
>    - 相同的 globalSeed → 相同的仿真结果
>    - 每个 seedKey 对应一个确定性的 Random 实例
>    - Random 实例在仿真期间保持单例

### 3.4 BatchRule（批量规则）

```java
/**
 * Batching rule for process step.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class BatchRule {
    public enum BatchType {
        SINGLE,              // 单件处理
        FIXED_SIZE,          // 固定批量
        TIME_WINDOW,         // 时间窗口
        ACCUMULATE           // 累积到满足条件
    }

    private BatchType type;
    private int batchSize;                      // 固定批量大小
    private double timeWindowSeconds;           // 时间窗口
    private Predicate<List<Material>> condition; // 累积条件
}
```

### 3.5 OutputStrategy（产出策略）

```java
/**
 * Output strategy after process completion.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class OutputStrategy {
    public enum StrategyType {
        DIRECT_TO_NEXT,       // 直达下一台
        INTERMEDIATE_STORAGE, // 中间存储
        BUFFER_ZONE,          // 暂存区
        CONDITIONAL,          // 条件分支
        DYNAMIC_SELECT        // 动态选择（从候选列表）
    }

    private StrategyType type;

    // 目标配置（根据 type 选择使用）
    private String singleDestination;             // 单目标 ID
    private List<String> candidateList;           // 候选目标列表（显式配置）
    private CandidateSelector dynamicSelector;    // 动态选择器

    // 路由策略
    private RoutingPolicy routingPolicy;

    // 条件分支配置（CONDITIONAL 类型）
    private Map<String, ConditionalRoute> conditionalRoutes;

    // 下游不可用策略
    private DownstreamUnavailableStrategy downstreamStrategy;
}
```

### 3.6 TransportSelector（搬运主体选择器）

```java
/**
 * Transport entity selector for material movement.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class TransportSelector {
    public enum SelectionPolicy {
        DISTANCE_FIRST,       // 距离优先
        TIME_FIRST,           // 时效优先
        WIP_FIRST,            // 负载优先
        PRIORITY_BASED,       // 优先级
        HYBRID                // 混合策略（多因子加权）
    }

    private SelectionPolicy policy;
    private List<EntityType> allowedTypes;       // 允许的类型：OHT/AGV/HUMAN

    // v2.0: 权重配置（从 system_config 读取）
    private double distanceWeight;  // 默认 0.4 (dispatch.selector.weight.distance)
    private double timeWeight;      // 默认 0.4 (dispatch.selector.weight.time)
    private double wipWeight;       // 默认 0.2 (dispatch.selector.weight.wip)
    private String normalization;  // 默认 "min-max" (dispatch.selector.normalization)

    /**
     * v2.0: 多因子加权设备选择公式:
     * Score(x) = w_distance × (1 - normDistance(x))
     *          + w_time × (1 - normTime(x))
     *          + w_wip × (1 - normWip(x))
     *
     * 归一化方法: Min-Max
     * 权重来源: system_config
     */
    public Vehicle select(CandidateContext ctx) {
        // 根据策略选择最优车辆/操作员
    }
}
```

### 3.7 EventTrigger（事件触发器，v1.2 扩展版）

```java
/**
 * Event trigger for automatic task generation (v1.2 - extended matching).
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class EventTrigger {
    public enum EventType {
        MACHINE_COMPLETED,    // 机台完成
        INVENTORY_LOW,        // 库存低水位
        DOWNSTREAM_PULL,      // 下游拉动
        TIMER,                // 定时触发
        WIP_THRESHOLD         // WIP 阈值
    }

    private EventType type;
    private String sourceId;                      // 事件源 ID
    private TriggerCondition triggerCondition;    // 触发条件 (v1.2: 扩展匹配)
    private String targetProcessFlowId;           // 关联的工艺流程
    private boolean enabled;
}

/**
 * Trigger condition with flexible matching (NEW in v1.2).
 *
 * Supports:
 * - Material attribute matching (type, grade, batch)
 * - Process tag matching
 * - Custom predicate conditions
 *
 * This allows rework flows, alternate paths, and complex routing scenarios.
 *
 * IMPORTANT: EntryPointId Semantics (v1.3 CLARIFICATION)
 * -------------------------------------------------------
 * There are TWO places where entryPointId appears:
 *
 * 1. `process_flow_bindings.entry_point_id` (database column)
 *    - Purpose: Basic filtering, fast indexed lookup
 *    - Used in: WHERE clause when querying bindings
 *    - Priority: HIGHEST (first-level filter)
 *
 * 2. `trigger_condition.entryPointId` (JSON field)
 *    - Purpose: Extended matching, part of condition object
 *    - Used in: TriggerCondition.matches() method
 *    - Priority: SAME as #1 (MUST be consistent)
 *
 * CONSISTENCY RULE:
 * - trigger_condition.entryPointId MUST EQUAL process_flow_bindings.entry_point_id
 * - If they differ, the binding is considered INVALID
 * - FlowTriggerManager validates this consistency before matching
 *
 * RATIONALE:
 * - Database column enables indexed query performance
 * - JSON field enables unified condition object for complex matching
 * - Both serve same semantic purpose, just different storage levels
 *
 * Example of CORRECT usage:
 *   process_flow_bindings.entry_point_id = "Stocker-01"
 *   trigger_condition.entryPointId = "Stocker-01"  // MUST match
 *
 * Example of INCORRECT usage:
 *   process_flow_bindings.entry_point_id = "Stocker-01"
 *   trigger_condition.entryPointId = "Stocker-02"  // ERROR: mismatch!
 *
 * @author shentw
 * @version 1.3
 * @since 2026-02-06
 */
public class TriggerCondition {

    // Basic matching
    // v1.3: MUST be consistent with process_flow_bindings.entry_point_id
    private String entryPointId;                  // 入口点 ID（必填，与绑定表一致）

    // Material attribute matching
    private String materialType;                  // 物料类型（可选，如 "WAFER", "CASSETTE"）
    private String materialGrade;                 // 物料等级（可选，如 "A", "B", "C"）
    private String materialBatch;                 // 批次号（可选，支持通配符）
    private Map<String, String> materialAttributes; // 自定义物料属性（可选）

    // Process tag matching
    private String processTag;                    // 工艺标记（可选，如 "REWORK", "NORMAL", "URGENT"）
    private List<String> allowedProcessTags;      // 允许的工艺标记列表（可选）

    // Custom condition
    private Predicate<EventContext> customCondition; // 自定义条件（可选，完全自定义）

    /**
     * Check if this condition matches the event and material.
     *
     * Matching logic:
     * 1. entryPointId must match (required)
     * 2. If materialType is set, must match
     * 3. If materialGrade is set, must match
     * 4. If materialBatch is set, must match (supports wildcards)
     * 5. If materialAttributes is set, all must match
     * 6. If processTag is set, must match
     * 7. If allowedProcessTags is set, material tag must be in list
     * 8. If customCondition is set, must evaluate to true
     *
     * @param event Trigger event
     * @param material Material (can be null)
     * @return true if condition matches
     */
    public boolean matches(Event event, Material material) {
        // 1. Entry point ID must match
        if (!entryPointId.equals(event.getSourceId())) {
            return false;
        }

        // If no material, skip material-based matching
        if (material == null) {
            return true;
        }

        // 2. Material type matching
        if (materialType != null && !materialType.equals(material.getType())) {
            return false;
        }

        // 3. Material grade matching
        if (materialGrade != null && !materialGrade.equals(material.getGrade())) {
            return false;
        }

        // 4. Material batch matching (with wildcard support)
        if (materialBatch != null && !matchBatch(materialBatch, material.getBatch())) {
            return false;
        }

        // 5. Custom material attributes matching
        if (materialAttributes != null) {
            for (Map.Entry<String, String> entry : materialAttributes.entrySet()) {
                String attrValue = material.getAttribute(entry.getKey());
                if (!entry.getValue().equals(attrValue)) {
                    return false;
                }
            }
        }

        // 6. Process tag matching
        if (processTag != null && !processTag.equals(material.getProcessTag())) {
            return false;
        }

        // 7. Allowed process tags matching
        if (allowedProcessTags != null && !allowedProcessTags.isEmpty()) {
            if (material.getProcessTag() == null ||
                !allowedProcessTags.contains(material.getProcessTag())) {
                return false;
            }
        }

        // 8. Custom condition
        if (customCondition != null) {
            EventContext context = new EventContext(event, material);
            if (!customCondition.test(context)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Match batch with wildcard support.
     *
     * Wildcards:
     * - "*" matches any batch
     * - "BATCH-*" matches "BATCH-001", "BATCH-002", etc.
     *
     * @param pattern Batch pattern
     * @param actual Actual batch
     * @return true if matches
     */
    private boolean matchBatch(String pattern, String actual) {
        if ("*".equals(pattern)) {
            return true;
        }

        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return actual != null && actual.startsWith(prefix);
        }

        return pattern.equals(actual);
    }

    /**
     * Builder for TriggerCondition.
     */
    public static class Builder {
        private TriggerCondition condition = new TriggerCondition();

        public Builder entryPoint(String entryPointId) {
            condition.entryPointId = entryPointId;
            return this;
        }

        public Builder materialType(String type) {
            condition.materialType = type;
            return this;
        }

        public Builder materialGrade(String grade) {
            condition.materialGrade = grade;
            return this;
        }

        public Builder materialBatch(String batch) {
            condition.materialBatch = batch;
            return this;
        }

        public Builder materialAttribute(String key, String value) {
            if (condition.materialAttributes == null) {
                condition.materialAttributes = new HashMap<>();
            }
            condition.materialAttributes.put(key, value);
            return this;
        }

        public Builder processTag(String tag) {
            condition.processTag = tag;
            return this;
        }

        public Builder allowedProcessTags(List<String> tags) {
            condition.allowedProcessTags = tags;
            return this;
        }

        public Builder customCondition(Predicate<EventContext> predicate) {
            condition.customCondition = predicate;
            return this;
        }

        public TriggerCondition build() {
            if (condition.entryPointId == null) {
                throw new IllegalArgumentException("entryPointId is required");
            }
            return condition;
        }
    }

    static Builder builder() {
        return new Builder();
    }
}

/**
 * Event context for custom condition evaluation.
 */
public class EventContext {
    private final Event event;
    private final Material material;

    public EventContext(Event event, Material material) {
        this.event = event;
        this.material = material;
    }

    public Event getEvent() { return event; }
    public Material getMaterial() { return material; }
}
```

**使用示例**：

```java
// 示例 1: 基本匹配（仅入口点）
TriggerCondition basic = TriggerCondition.builder()
    .entryPoint("Stocker-01")
    .build();

// 示例 2: 匹配特定物料类型
TriggerCondition waferOnly = TriggerCondition.builder()
    .entryPoint("Stocker-01")
    .materialType("WAFER")
    .build();

// 示例 3: 匹配返工流程
TriggerCondition reworkFlow = TriggerCondition.builder()
    .entryPoint("Machine-A")
    .processTag("REWORK")
    .materialGrade("B")  // 仅 B 级晶圆需要返工
    .build();

// 示例 4: 匹配批次前缀
TriggerCondition batchFlow = TriggerCondition.builder()
    .entryPoint("Stocker-01")
    .materialBatch("BATCH-2025-*")  // 通配符匹配
    .build();

// 示例 5: 自定义条件
TriggerCondition custom = TriggerCondition.builder()
    .entryPoint("Machine-A")
    .materialType("WAFER")
    .customCondition(ctx -> {
        Material m = ctx.getMaterial();
        // 自定义逻辑: 仅当晶圆温度 < 100°C 时匹配
        return m.getTemperature() < 100.0;
    })
    .build();
```

---

## 四、设计方案详解

### A. 工艺流程建模

#### A.1 多机台串联建模

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ProcessFlow: "Wafer-Fab-Line-01"             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐     │
│   │  Step 1  │───▶│  Step 2  │───▶│  Step 3  │───▶│  Step 4  │     │
│   │ Lithography│   │ Etching    │   │ Deposition│   │ Inspection│     │
│   │ (Machine-A)│   │ (Machine-B)│   │ (Machine-C)│   │ (Machine-D)│     │
│   └──────────┘    └──────────┘    └──────────┘    └──────────┘     │
│        │               │               │               │             │
│        ▼               ▼               ▼               ▼             │
│   [WIP ≤ 3]      [WIP ≤ 2]       [WIP ≤ 1]       [WIP ≤ 5]          │
│   [Batch: 1]     [Batch: 5]      [Batch: 1]      [Batch: 1]         │
│   [Time: N(50,5)][Time: E(30)]  [Time: T(20,30,40)][Time: 60]       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**数据结构约束**：
- `ProcessStep.sequence` 必须严格递增（0, 1, 2...）
- `predecessorIds` 与 `successorId` 维护 DAG（有向无环图）
- 每个工序可配置独立的 `OutputStrategy` 覆盖流程级策略

#### A.2 流程复用与版本化

- `ProcessFlow.version`：语义化版本（1.0.0）
- 流程可导出为 JSON，支持导入/克隆
- 版本间继承与差异比较

---

### B. 产出策略

#### B.1 策略类型详解

| 策略类型 | 说明 | 使用场景 |
|----------|------|----------|
| `DIRECT_TO_NEXT` | 直达下一工序 | 连续流生产 |
| `INTERMEDIATE_STORAGE` | 中间存储（Stocker） | 需暂存的半成品 |
| `BUFFER_ZONE` | 暂存区（ERack） | 短暂缓存 |
| `CONDITIONAL` | 条件分支 | 多路径选择 |

#### B.2 路由策略

```java
public enum RoutingPolicy {
    NEAREST,           // 就近：选择距离最近的可用目标
    LINE_FIRST,        // 产线优先：优先同产线设备
    LOAD_BALANCE,      // 负载均衡：选择 WIP 最少的目标
    PRIORITY_BASED,    // 优先级：按设备优先级
    CUSTOM             // 自定义：用户实现接口
}
```

---

### C. 搬运主体选择

#### C.1 混合搬运支持

**场景**：同一流程中不同工序使用不同搬运主体

```java
// v2.0: 多因子加权选择示例
ProcessFlow flow = new ProcessFlow();
flow.setTransportSelector(TransportSelector.builder()
    .allowedTypes(Arrays.asList(OHT_VEHICLE, AGV_VEHICLE, HUMAN))
    .policy(SelectionPolicy.HYBRID)
    .distanceWeight(0.4)    // 默认从 system_config 读取
    .timeWeight(0.4)
    .wipWeight(0.2)
    .normalization("min-max")
    .build());

// 工序级覆盖
step2.setTransportSelector(TransportSelector.builder()
    .allowedTypes(Arrays.asList(HUMAN))        // 仅人工
    .policy(SelectionPolicy.HYBRID)
    .distanceWeight(0.5)    // 自定义权重
    .timeWeight(0.3)
    .wipWeight(0.2)
    .build());
```

---

### D. 自动任务生成闭环

#### D.1 事件驱动架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                      自动任务生成闭环                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐          │
│   │  Event      │────▶│  Trigger    │────▶│  Task       │          │
│   │  Source     │     │  Evaluator  │     │  Generator  │          │
│   └─────────────┘     └─────────────┘     └─────────────┘          │
│         │                     │                     │               │
│         ▼                     ▼                     ▼               │
│   EventTypes            TriggerEval          TaskQueue              │
│   - Complete            - Condition          (自动入队)              │
│   - LowStock            - Validation                                 │
│   - PullReq             - Action                                    │
│   - Timer                                                           │
│         │                     │                     │               │
│         └─────────────────────┴─────────────────────┘               │
│                               │                                     │
│                               ▼                                     │
│                      DispatchEngine                                 │
│                               │                                     │
│                               ▼                                     │
│                      VehicleAssignment                              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### D.2 事件类型与触发条件

| 事件类型 | 触发条件 | 生成的任务 |
|----------|----------|------------|
| `MACHINE_COMPLETED` | 机台加工完成 | 搬运到下一工序 |
| `INVENTORY_LOW` | Stocker 水位 < 阈值 | 补货任务 |
| `DOWNSTREAM_PULL` | 下游机台空闲 + 有 WIP | 拉料任务 |
| `TIMER` | 定时触发 | 周期性任务 |
| `WIP_THRESHOLD` | 工序 WIP > 阈值 | 负载均衡转移 |

---

### E. 派单与上下游约束

#### E.1 上下游依赖检查

```java
/**
 * Check if all predecessors are completed.
 */
boolean canStartStep(ProcessStep step, Material material) {
    for (String predId : step.getPredecessorIds()) {
        ProcessStep pred = getStep(predId);
        if (!isCompleted(pred, material)) {
            return false;
        }
    }
    return true;
}
```

#### E.2 下游不可用策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `BUFFER_TO_STORAGE` | 生成暂存任务 | 有可用暂存区 |
| `BLOCK_AND_WAIT` | 阻塞等待 | 无暂存区 |
| `DEFER_RETRY` | 放弃/延后重试 | 非关键任务 |
| `REROUTE_ALTERNATE` | 尝试替代路径 | 多条工艺路径 |
| `RETURN_UPSTREAM` | 退回上游 | 可逆工序 |

---

### F. 交管影响

#### F.1 人/车混合搬运的交通策略

| 维度 | OHT | AGV | Human |
|------|-----|-----|-------|
| **路径规划** | 轨道约束 | 路网约束 | 无（简化） |
| **控制点** | 独立 OHT_CP | 独立 AGV_CP | 共享/独立 |
| **控制区** | 空中区域 OHT_ZONE | 地面区域 AGV_ZONE | 共享区域 |
| **优先级** | URGENT | NORMAL | LOW（安全优先） |

---

## 五、关键设计决策澄清

> **本章节针对 Review 反馈的 3 个关键点进行详细澄清**

### 5.1 ProcessFlow 与场景的绑定方式（关键点 1）

#### 5.1.1 数据存储方案（决策）

**最终决策：独立表存储 + 场景引用**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ProcessFlow 数据存储方案（最终版）                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【核心决策】                                                                 │
│  - ProcessFlow 存储在独立表中（process_flows）                               │
│  - 场景通过引用关系绑定流程版本                                              │
│  - 支持一个场景绑定多个流程                                                  │
│  - 支持流程复用（多个场景共享同一流程）                                       │
│  - 【v1.3 新增】支持多租户隔离                                              │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     process_flows (表 - v1.5 更新)                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  tenant_id       │ CHAR(36)     │ (租户 ID，多租户隔离)              │   │
│  │  id              │ CHAR(36)     │ PK                               │   │
│  │  name            │ VARCHAR(255) │                                   │   │
│  │  version         │ VARCHAR(32)  │ (1.0.0, 语义化版本)               │   │
│  │  definition      │ JSON         │ (完整流程定义，包含 steps)         │   │
│  │  is_template     │ BOOLEAN      │ (是否为模板)                      │   │
│  │  created_at      │ DATETIME(3)  │                                   │   │
│  │  created_by      │ VARCHAR(100) │ (用户 ID)                         │   │
│  │  parent_version  │ VARCHAR(32)  │ (父版本，用于追溯)                │   │
│  │  UNIQUE KEY uk_flows_tenant_id_version (tenant_id, id, version)     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │               process_flow_bindings (绑定表 - v1.5 更新)            │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  tenant_id       │ CHAR(36)     │ (租户 ID，多租户隔离)              │   │
│  │  id              │ CHAR(36)     │ PK                               │   │
│  │  scene_id        │ CHAR(36)     │ FK → scenes(tenant_id, id)        │   │
│  │  flow_id         │ CHAR(36)     │ FK → process_flows(tenant_id, id) │   │
│  │  flow_version    │ VARCHAR(32)  │ (绑定的版本号)                    │   │
│  │  entry_point_id  │ VARCHAR(100) │ (入口实体 ID，如 Stocker-01)      │   │
│  │  enabled         │ BOOLEAN      │ (是否启用)                        │   │
│  │  priority        │ INT          │ (绑定优先级，支持多流程排序)       │   │
│  │  trigger_condition JSON │ (v1.2: 触发条件，见 3.7 节)              │   │
│  │  created_at      │ DATETIME(3)  │                                   │   │
│  │  UNIQUE KEY uk_bindings_tenant_id (tenant_id, id)                   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        scenes (表 - 已存在)                          │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  tenant_id       │ CHAR(36)     │ (租户 ID，已存在)                  │   │
│  │  id              │ CHAR(36)     │ PK                               │   │
│  │  name            │ VARCHAR(255) │                                   │   │
│  │  entities        │ JSON         │ (现有字段)                        │   │
│  │  paths           │ JSON         │ (现有字段)                        │   │
│  │  version         │ INT          │ (场景版本号)                      │   │
│  │  UNIQUE KEY uk_scenes_tenant_id (tenant_id, id)  ← 新增             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

> **数据库 Schema 对齐说明（v1.3 更新 - 添加多租户支持）**：
>
> 本设计方案定义了 `process_flows` 和 `process_flow_bindings` 两张新表。
> 在正式落地时，需要：
>
> 1. **在 database-schema 文件中添加 DDL 定义**
>    - `resources/db/migration/V2__create_process_flows.sql`
>    - `resources/db/migration/V3__create_process_flow_bindings.sql`
>
> 2. **完整 DDL 定义（v1.4: 含多租户支持，版本号符合 Flyway 规则）**
>    ```sql
>    -- ============================================================
>    -- Migration: V2__create_process_flows.sql
>    -- Description: Create process_flows table with multi-tenant support
>    -- Author: shentw
>    -- Date: 2026-02-06
>    -- Prefix: V2 (after V1__Initial_schema.sql)
>    -- ============================================================
>
>    CREATE TABLE process_flows (
>        tenant_id       CHAR(36)     NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000' COMMENT '租户 ID',
>        id              CHAR(36)     NOT NULL COMMENT '流程 ID',
>        name            VARCHAR(255) NOT NULL COMMENT '流程名称',
>        version         VARCHAR(32)  NOT NULL COMMENT '版本号（语义化版本）',
>        definition      JSON         NOT NULL COMMENT '流程定义（JSON）',
>        is_template     BOOLEAN      DEFAULT FALSE COMMENT '是否为模板',
>        created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
>        created_by      VARCHAR(100) COMMENT '创建用户 ID',
>        parent_version  VARCHAR(32) COMMENT '父版本号',
>        PRIMARY KEY (tenant_id, id, version),
>        INDEX idx_flows_tenant_template (tenant_id, is_template),
>        INDEX idx_flows_tenant_created_by (tenant_id, created_by)
>    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺流程定义表';
>
>    -- Foreign key to tenants table
>    ALTER TABLE process_flows ADD CONSTRAINT fk_flows_tenant
>        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
>
>    -- ============================================================
>    -- Migration: V3__create_process_flow_bindings.sql
>    -- Description: Create process_flow_bindings table with multi-tenant support
>    -- Author: shentw
>    -- Date: 2026-02-06
>    -- Prefix: V3 (after V2__create_process_flows.sql)
>    -- ============================================================
>
>    CREATE TABLE process_flow_bindings (
>        tenant_id       CHAR(36)     NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000' COMMENT '租户 ID',
>        id              CHAR(36)     NOT NULL COMMENT '绑定记录 ID',
>        scene_id        CHAR(36)     NOT NULL COMMENT '场景 ID',
>        flow_id         CHAR(36)     NOT NULL COMMENT '流程 ID',
>        flow_version    VARCHAR(32)  NOT NULL COMMENT '流程版本号',
>        entry_point_id  VARCHAR(100) NOT NULL COMMENT '入口点 ID',
>        enabled         BOOLEAN      DEFAULT TRUE COMMENT '是否启用',
>        priority        INT          DEFAULT 0 COMMENT '优先级（数字越小优先级越高）',
>        trigger_condition JSON COMMENT '触发条件（v1.2+）',
>        created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
>        PRIMARY KEY (tenant_id, id),
>        INDEX idx_bindings_tenant_scene (tenant_id, scene_id, enabled),
>        INDEX idx_bindings_tenant_flow (tenant_id, flow_id, flow_version),
>        INDEX idx_bindings_tenant_entry_point (tenant_id, entry_point_id),
>        INDEX idx_bindings_tenant_priority (tenant_id, priority)
>    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺流程绑定表';
>
>    -- Foreign keys (multi-tenant aware)
>    ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_bindings_scene
>        FOREIGN KEY (tenant_id, scene_id) REFERENCES scenes(tenant_id, id) ON DELETE CASCADE;
>
>    ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_bindings_flow
>        FOREIGN KEY (tenant_id, flow_id, flow_version)
>        REFERENCES process_flows(tenant_id, id, version) ON DELETE RESTRICT;
>    ```
>
> 3. **DDL 更新计划（v1.4: 符合 Flyway 规则）**：
>
>    当前状态：设计方案已完成，DDL 已定义。
>
>    下一步行动：
>    - **立即行动**：将上述 DDL 添加到 `resources/db/migration/` 目录
>    - **版本号**：使用 V2 和 V3 作为迁移脚本版本（在 V1__Initial_schema.sql 之后）
>    - **依赖顺序**：先执行 V2（process_flows），后执行 V3（process_flow_bindings）
>    - **测试计划**：在迁移后执行 `validate_schema.sql` 验证外键和索引
>    - **回滚计划**：准备对应的回滚脚本 `V2__rollback.sql` 和 `V3__rollback.sql`
>
>    回滚脚本示例：
>    ```sql
>    -- V3__rollback.sql
>    ALTER TABLE process_flow_bindings DROP FOREIGN KEY fk_bindings_scene;
>    ALTER TABLE process_flow_bindings DROP FOREIGN KEY fk_bindings_flow;
>    DROP TABLE IF EXISTS process_flow_bindings;
>
>    -- V2__rollback.sql
>    ALTER TABLE process_flows DROP FOREIGN KEY fk_flows_tenant;
>    DROP TABLE IF EXISTS process_flows;
>    ```
>
> 4. **外键约束（v1.3: 多租户感知）**
>    ```sql
>    -- process_flows 表
>    ALTER TABLE process_flows ADD CONSTRAINT fk_flows_tenant
>        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
>
>    -- process_flow_bindings 表
>    ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_bindings_scene
>        FOREIGN KEY (tenant_id, scene_id) REFERENCES scenes(tenant_id, id) ON DELETE CASCADE;
>    ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_bindings_flow
>        FOREIGN KEY (tenant_id, flow_id, flow_version) REFERENCES process_flows(tenant_id, id, version) ON DELETE RESTRICT;
>    ```
>
> 5. **索引设计（v1.3: 多租户索引）**
>    ```sql
>    -- process_flows 表
>    CREATE UNIQUE INDEX uk_flows_tenant_id_version ON process_flows(tenant_id, id, version);
>    CREATE INDEX idx_flows_tenant_template ON process_flows(tenant_id, is_template);
>    CREATE INDEX idx_flows_tenant_created_by ON process_flows(tenant_id, created_by);
>
>    -- process_flow_bindings 表
>    CREATE UNIQUE INDEX uk_bindings_tenant_id ON process_flow_bindings(tenant_id, id);
>    CREATE INDEX idx_bindings_tenant_scene ON process_flow_bindings(tenant_id, scene_id, enabled);
>    CREATE INDEX idx_bindings_tenant_flow ON process_flow_bindings(tenant_id, flow_id, flow_version);
>    CREATE INDEX idx_bindings_tenant_entry_point ON process_flow_bindings(tenant_id, entry_point_id);
>    CREATE INDEX idx_bindings_tenant_priority ON process_flow_bindings(tenant_id, priority);
>    ```
>
> **多租户隔离规则**：
> - 所有查询必须在 WHERE 条件中包含 `tenant_id`
> - 跨租户数据访问必须被应用层严格禁止
> - Repository 层应自动注入当前租户 ID
>
> 6. **JSON 字段结构（definition 列）**
>    ```json
>    {
>      "steps": [
>        {
>          "id": "step-001",
>          "name": "Lithography",
>          "sequence": 0,
>          "targetEntityIds": ["Machine-A", "Machine-B"],  // v2.0: 多候选设备
>          "requiredTransportTypes": ["OHT", "AGV"],      // v2.0: 必需的运输类型
>          "processingTime": {"type": "NORMAL", "mean": 50, "std": 5},
>          "wipLimit": 3,
>          "outputStrategy": {"type": "DIRECT_TO_NEXT"},
>          "transportSelector": {
>            "policy": "HYBRID",
>            "allowedTypes": ["OHT", "AGV"],
>            "distanceWeight": 0.4,
>            "timeWeight": 0.4,
>            "wipWeight": 0.2,
>            "normalization": "min-max"
>          }
>        }
>      ],
>      "defaultOutputStrategy": {"type": "DIRECT_TO_NEXT"},
>      "defaultTransportSelector": {                      // v2.0: 更新字段名
>        "policy": "HYBRID",
>        "allowedTypes": ["OHT", "AGV"],
>        "distanceWeight": 0.4,
>        "timeWeight": 0.4,
>        "wipWeight": 0.2,
>        "normalization": "min-max"
>      }
>    }
>    ```
>
> 7. **process_flow_bindings 扩展字段（v1.2 新增 TriggerCondition）**
>    ```sql
>    ALTER TABLE process_flow_bindings ADD COLUMN trigger_condition JSON;
>    ```
>
>    `trigger_condition` JSON 结构：
>    ```json
>    {
>      "entryPointId": "Stocker-01",
>      "materialType": "WAFER",
>      "materialGrade": "A",
>      "materialBatch": "BATCH-2025-*",
>      "processTag": "NORMAL",
>      "allowedProcessTags": ["NORMAL", "URGENT"],
>      "materialAttributes": {"region": "FAB-1", "priority": "HIGH"}
>    }
>    ```

#### 5.1.2 场景如何引用流程版本

**API 接口设计**：

```java
/**
 * 场景绑定流程
 */
public class SceneFlowBinding {

    /**
     * 绑定流程到场景
     * @param sceneId 场景 ID
     * @param flowId 流程 ID
     * @param flowVersion 流程版本 (e.g., "1.2.0")
     * @param entryPointId 入口实体 ID (物料从哪里开始进入流程)
     * @return 绑定记录 ID
     */
    public String bindFlowToScene(String sceneId, String flowId,
                                   String flowVersion, String entryPointId) {
        // 1. 验证流程版本是否存在
        ProcessFlow flow = processFlowRepository.findByIdAndVersion(flowId, flowVersion);
        if (flow == null) {
            throw new FlowVersionNotFoundException(flowId, flowVersion);
        }

        // 2. 验证入口实体是否存在于场景中
        if (!sceneRepository.entityExists(sceneId, entryPointId)) {
            throw new EntityNotFoundException(entryPointId);
        }

        // 3. 创建绑定记录
        ProcessFlowBinding binding = new ProcessFlowBinding();
        binding.setSceneId(sceneId);
        binding.setFlowId(flowId);
        binding.setFlowVersion(flowVersion);
        binding.setEntryPointId(entryPointId);
        binding.setEnabled(true);

        return processFlowBindingRepository.save(binding);
    }

    /**
     * 获取场景绑定的所有流程（按优先级排序）
     */
    public List<ProcessFlow> getSceneFlows(String sceneId) {
        List<ProcessFlowBinding> bindings = processFlowBindingRepository
            .findBySceneIdAndEnabledTrueOrderByPriorityAsc(sceneId);

        return bindings.stream()
            .map(binding -> processFlowRepository
                .findByIdAndVersion(binding.getFlowId(), binding.getFlowVersion()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

### 5.4 错误处理策略（新增）

**统一错误响应结构**:
```json
{
  "code": "WF-404",
  "message": "Flow version not found",
  "details": {"flowId": "flow-1", "version": "1.2.0"},
  "traceId": "req-xxx",
  "ts": 1739012345678
}
```

**错误码规范**:
```
WF-400  INVALID_REQUEST          # 参数或条件不合法
WF-404  FLOW_NOT_FOUND           # 流程版本不存在
WF-404  ENTITY_NOT_FOUND         # 场景实体不存在
WF-409  ENTRYPOINT_MISMATCH      # entryPointId 不一致
WF-409  MATERIAL_ALREADY_BOUND   # 物料已绑定
WF-423  RESOURCE_UNAVAILABLE     # 资源不可用
WF-429  RETRY_EXCEEDED           # 超过最大重试次数
WF-500  INTERNAL_ERROR           # 未知错误
```

**处理原则**:
- REST 返回结构化错误码与 traceId
- WebSocket `type=error`，payload 同结构
- 业务异常不直接抛到调用方，统一映射为错误码
- 关键错误记录到 error 日志并附带 traceId

---

#### Java 异常类定义

**基础异常类**:
```java
/**
 * Workflow automation exception base class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public abstract class WorkflowException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;
    private final String traceId;

    protected WorkflowException(String errorCode, String message,
                                Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : new HashMap<>();
        this.traceId = UUID.randomUUID().toString();
    }

    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getDetails() { return details; }
    public String getTraceId() { return traceId; }
}
```

**具体异常类**:
```java
// 流程相关异常
public class FlowVersionNotFoundException extends WorkflowException {
    public FlowVersionNotFoundException(String flowId, String version) {
        super("WF-404",
              String.format("Flow version not found: %s@%s", flowId, version),
              Map.of("flowId", flowId, "version", version));
    }
}

public class FlowValidationException extends WorkflowException {
    public FlowValidationException(String flowId, String reason) {
        super("WF-400",
              String.format("Flow validation failed: %s - %s", flowId, reason),
              Map.of("flowId", flowId, "reason", reason));
    }
}

// 实体相关异常
public class EntityNotFoundException extends WorkflowException {
    public EntityNotFoundException(String entityId) {
        super("WF-404",
              String.format("Entity not found: %s", entityId),
              Map.of("entityId", entityId));
    }
}

// 触发器相关异常
public class EntryPointMismatchException extends WorkflowException {
    public EntryPointMismatchException(String bindingId, String expected, String actual) {
        super("WF-409",
              String.format("EntryPoint mismatch in binding %s: expected=%s, actual=%s",
                          bindingId, expected, actual),
              Map.of("bindingId", bindingId, "expected", expected, "actual", actual));
    }
}

// 物料绑定异常
public class MaterialAlreadyBoundException extends WorkflowException {
    public MaterialAlreadyBoundException(String materialId, String currentFlowId) {
        super("WF-409",
              String.format("Material already bound to flow: %s -> %s",
                          materialId, currentFlowId),
              Map.of("materialId", materialId, "currentFlowId", currentFlowId));
    }
}

// 资源异常
public class ResourceUnavailableException extends WorkflowException {
    public ResourceUnavailableException(String entityId, String reason) {
        super("WF-423",
              String.format("Resource unavailable: %s - %s", entityId, reason),
              Map.of("entityId", entityId, "reason", reason));
    }
}

// 重试异常
public class RetryExceededException extends WorkflowException {
    public RetryExceededException(String taskId, int maxRetries, int attempts) {
        super("WF-429",
              String.format("Retry exceeded: task %s - %d/%d attempts",
                          taskId, attempts, maxRetries),
              Map.of("taskId", taskId, "maxRetries", maxRetries, "attempts", attempts));
    }
}

// 内部错误
public class WorkflowInternalException extends WorkflowException {
    public WorkflowInternalException(String message, Throwable cause) {
        super("WF-500",
              "Internal workflow error: " + message,
              Map.of("originalCause", cause.getClass().getSimpleName()));
    }
}
```

---

#### 错误码详细定义

| 错误码 | HTTP 状态 | 异常类 | 描述 | 用户提示 |
|-------|----------|--------|------|----------|
| **WF-400** | 400 | FlowValidationException | 流程验证失败 | 流程配置不合法，请检查工序定义 |
| **WF-404-FLOW** | 404 | FlowVersionNotFoundException | 流程版本不存在 | 请检查流程版本是否正确 |
| **WF-404-ENTITY** | 404 | EntityNotFoundException | 场景实体不存在 | 请检查场景中是否包含该实体 |
| **WF-409-ENTRY** | 409 | EntryPointMismatchException | 入口点不匹配 | 绑定配置与实际入口点不一致 |
| **WF-409-BOUND** | 409 | MaterialAlreadyBoundException | 物料已绑定 | 物料正在被其他流程处理 |
| **WF-423** | 423 | ResourceUnavailableException | 资源不可用 | 目标资源当前不可用，请稍后重试 |
| **WF-429** | 429 | RetryExceededException | 超过最大重试次数 | 任务重试次数已达上限，请检查配置 |
| **WF-500** | 500 | WorkflowInternalException | 内部错误 | 系统内部错误，请联系管理员 |

---

#### 错误处理流程

**REST API 错误处理**:
```java
@RestControllerAdvice
public class WorkflowExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExceptionHandler.class);

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<ErrorResponse> handleWorkflowException(WorkflowException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails(),
            ex.getTraceId(),
            Instant.now()
        );

        // 记录错误日志
        if (isClientError(ex.getErrorCode())) {
            log.warn("[{}] {} - details: {}",
                    ex.getTraceId(), ex.getMessage(), ex.getDetails());
        } else {
            log.error("[{}] {} - details: {}",
                    ex.getTraceId(), ex.getMessage(), ex.getDetails(), ex);
        }

        HttpStatus status = getHttpStatusForErrorCode(ex.getErrorCode());
        return ResponseEntity.status(status).body(error);
    }

    private boolean isClientError(String errorCode) {
        return errorCode.startsWith("WF-4") && !errorCode.equals("WF-500");
    }

    private HttpStatus getHttpStatusForErrorCode(String errorCode) {
        return switch (errorCode) {
            case "WF-400" -> HttpStatus.BAD_REQUEST;
            case "WF-404-FLOW", "WF-404-ENTITY" -> HttpStatus.NOT_FOUND;
            case "WF-409-ENTRY", "WF-409-BOUND" -> HttpStatus.CONFLICT;
            case "WF-423" -> HttpStatus.LOCKED;
            case "WF-429" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

**WebSocket 错误处理**:
```java
@Component
public class WebSocketErrorHandler {

    public void sendError(WebSocketSession session, WorkflowException ex) {
        try {
            Map<String, Object> error = Map.of(
                "type", "ERROR",
                "payload", Map.of(
                    "code", ex.getErrorCode(),
                    "message", ex.getMessage(),
                    "details", ex.getDetails(),
                    "traceId", ex.getTraceId()
                ),
                "timestamp", Instant.now().toString()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("Failed to send error to WebSocket client", e);
        }
    }
}
```

---

#### 重试策略

**重试配置**:
```java
public class RetryConfig {
    private int maxRetries = 3;              // 最大重试次数
    private long initialDelayMs = 1000;      // 初始延迟
    private double backoffMultiplier = 2.0;  // 退避倍数
    private long maxDelayMs = 30000;         // 最大延迟
    private Set<String> retryableErrors = Set.of(
        "WF-423",  // 资源不可用 - 可重试
        "WF-409-BOUND"  // 物料已绑定 - 可能短暂冲突
    );
}
```

**重试执行器**:
```java
public class WorkflowRetryExecutor {

    public void executeWithRetry(String taskId, Runnable task, RetryConfig config) {
        int attempt = 0;
        long delay = config.getInitialDelayMs();

        while (attempt <= config.getMaxRetries()) {
            try {
                task.run();
                return;
            } catch (WorkflowException ex) {
                attempt++;

                if (!config.getRetryableErrors().contains(ex.getErrorCode()) ||
                    attempt > config.getMaxRetries()) {
                    throw new RetryExceededException(taskId, config.getMaxRetries(), attempt);
                }

                log.info("Retry attempt {}/{} for task {} after {}ms",
                        attempt, config.getMaxRetries(), taskId, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new WorkflowInternalException("Retry interrupted", ie);
                }

                delay = (long) Math.min(delay * config.getBackoffMultiplier(),
                                        config.getMaxDelayMs());
            }
        }
    }
}
```

---

#### 错误日志记录规范

**日志级别**:
| 级别 | 场景 | 示例 |
|-----|------|------|
| ERROR | WF-500 内部错误、严重系统故障 | 数据库连接失败、空指针异常 |
| WARN | WF-4xx 客户端错误、重试 | 资源不可用、物料已绑定 |
| INFO | 重试成功、流程恢复 | 任务重试后成功 |
| DEBUG | 详细处理流程 | 事件匹配过程、状态转换 |

**日志格式**:
```java
// 错误日志
log.error("[{}] Flow execution failed - flowId: {}, stepId: {}, materialId: {}, error: {}",
          traceId, flowId, stepId, materialId, errorCode);

// 重试日志
log.warn("[{}] Resource unavailable for task {}, retrying in {}ms (attempt {}/{})",
         traceId, taskId, delay, attempt, maxRetries);

// 信息日志
log.info("[{}] Material {} bound to flow {} at step {}",
         traceId, materialId, flowId, stepId);
```

**TraceId 传递**:
```java
public class TraceContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String traceId) {
        CURRENT.set(traceId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static String generate() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        set(traceId);
        return traceId;
    }
}
```

**场景 JSON 格式（只读视图）**：

```json
{
  "sceneId": "scene-wafer-fab-01",
  "name": "晶圆厂 01 号线",
  "version": 3,
  "entities": [...],
  "paths": [...],
  "flowBindings": [
    {
      "bindingId": "binding-001",
      "flowId": "flow-wafer-processing",
      "flowVersion": "1.2.0",
      "flowName": "晶圆标准加工流程",
      "entryPointId": "Stocker-01",
      "enabled": true,
      "priority": 1
    },
    {
      "bindingId": "binding-002",
      "flowId": "flow-emergency-maintenance",
      "flowVersion": "2.0.1",
      "flowName": "紧急维护流程",
      "entryPointId": "ManualStation-01",
      "enabled": true,
      "priority": 2
    }
  ]
}
```

#### 5.1.3 是否允许多流程绑定

**决策：允许一个场景绑定多个流程**

**触发机制**：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    多流程绑定触发机制                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  场景 "scene-wafer-fab-01" 绑定了 2 个流程：                                 │
│                                                                             │
│  1. flow-wafer-processing (v1.2.0)                                          │
│     └─ entryPoint: Stocker-01                                              │
│     └─ 触发条件: Stocker-01 有物料出库                                      │
│                                                                             │
│  2. flow-emergency-maintenance (v2.0.1)                                     │
│     └─ entryPoint: ManualStation-01                                         │
│     └─ 触发条件: ManualStation-01 手动触发                                  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      触发流程判定逻辑                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  事件触发 (EventTrigger)                                            │   │
│  │       │                                                             │   │
│  │       ▼                                                             │   │
│  │  查询场景的绑定流程列表                                               │   │
│  │       │                                                             │   │
│  │       ├─────────────────────────────────────────────────────────┐   │   │
│  │       │                                                         │   │   │
│  │       ▼                                                         │   │   │
│  │  【修正 v1.2】条件匹配 (TriggerCondition 匹配，见 3.7 节)           │   │
│  │       │                                                         │   │
│  │       ├─ entryPointId == 事件源 ID                                │   │
│  │       ├─ material 属性匹配 (type, grade, batch)                   │   │
│  │       ├─ 工艺标记匹配 (processTag)                               │   │
│  │       └─ 自定义条件 (customCondition)                            │   │
│  │       │                                                         │   │
│  │       ▼                                                         │   │
│  │  若仅 1 个匹配 → 直接执行                                         │   │
│  │  若多个匹配 → 按 priority 降序执行                                │   │
│  │       │                                                         │   │
│  │       ▼                                                         │   │
│  │  【修正 v1.2】若 priority 相同 → 物料级互斥检查                     │   │
│  │       │                                                         │   │
│  │       ├─────────────────────────────────────────────────────────┤   │   │
│  │       │ 物料是否已被其他流程绑定?                                  │   │   │
│  │       │   │                                                     │   │   │
│  │       │   ├─ YES → 跳过（避免重复消费）                           │   │   │
│  │       │   │                                                     │   │   │
│  │       │   └─ NO → 绑定物料到流程，执行                            │   │   │
│  │       │                                                         │   │   │
│  │       └─────────────────────────────────────────────────────────┤   │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**实现示例（v1.2 修正版 - 添加物料级互斥）**：

```java
/**
 * Material binding manager (NEW in v1.2).
 *
 * Ensures that a material can only be bound to one flow at a time,
 * preventing duplicate consumption when multiple flows match.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class MaterialBindingManager {

    private final ConcurrentMap<String, MaterialBinding> bindings = new ConcurrentHashMap<>();
    private final SimulationEnv simulationEnv;  // v1.3: Added for simulation time

    /**
     * Constructor (v1.3).
     *
     * @param simulationEnv Simulation environment for time access
     */
    public MaterialBindingManager(SimulationEnv simulationEnv) {
        this.simulationEnv = simulationEnv;
    }

    /**
     * Try to bind material to a flow.
     *
     * @param materialId Material ID
     * @param flowId Flow ID
     * @param bindingId Binding ID
     * @return true if binding succeeded, false if already bound to another flow
     */
    public boolean tryBind(String materialId, String flowId, String bindingId) {
        // v1.3: Use simulation time instead of wall-clock time
        double currentTime = simulationEnv.now();
        MaterialBinding newBinding = new MaterialBinding(materialId, flowId, bindingId, currentTime);

        // Atomic putIfAbsent: only succeeds if key doesn't exist
        MaterialBinding existing = bindings.putIfAbsent(materialId, newBinding);

        if (existing != null) {
            // Material already bound to another flow
            log.debug("Material {} already bound to flow {} (binding: {}, boundAt: {}s), cannot bind to flow {}",
                materialId, existing.getFlowId(), existing.getBindingId(), existing.getBoundAt(), flowId);
            return false;
        }

        log.debug("Material {} bound to flow {} (binding: {}, boundAt: {}s)",
            materialId, flowId, bindingId, currentTime);
        return true;
    }

    /**
     * Release binding when flow completes or fails.
     *
     * @param materialId Material ID
     */
    public void releaseBinding(String materialId) {
        MaterialBinding removed = bindings.remove(materialId);
        if (removed != null) {
            double boundDuration = simulationEnv.now() - removed.getBoundAt();
            log.debug("Released binding for material {} from flow {} (bound for {}s)",
                materialId, removed.getFlowId(), boundDuration);
        }
    }

    /**
     * Check if material is currently bound.
     *
     * @param materialId Material ID
     * @return true if bound to a flow
     */
    public boolean isBound(String materialId) {
        return bindings.containsKey(materialId);
    }

    /**
     * Get current binding for material.
     *
     * @param materialId Material ID
     * @return Current binding, or null if not bound
     */
    public MaterialBinding getBinding(String materialId) {
        return bindings.get(materialId);
    }

    /**
     * Material binding record (v1.3: uses simulation time).
     *
     * IMPORTANT: boundAt is in SIMULATION TIME (seconds), not wall-clock time.
     * This ensures reproducibility across simulation runs.
     */
    public static class MaterialBinding {
        private final String materialId;
        private final String flowId;
        private final String bindingId;
        private final double boundAt;  // v1.3: Changed from long (wall-clock) to double (simulation seconds)

        /**
         * Constructor (v1.3).
         *
         * @param materialId Material ID
         * @param flowId Flow ID
         * @param bindingId Binding ID
         * @param boundAt Simulation time when binding was created (in seconds)
         */
        public MaterialBinding(String materialId, String flowId, String bindingId, double boundAt) {
            this.materialId = materialId;
            this.flowId = flowId;
            this.bindingId = bindingId;
            this.boundAt = boundAt;
        }

        // Getters
        public String getMaterialId() { return materialId; }
        public String getFlowId() { return flowId; }
        public String getBindingId() { return bindingId; }
        public double getBoundAt() { return boundAt; }  // v1.3: Returns simulation time
    }
}

/**
 * Flow trigger manager (v1.2 - with material-level mutual exclusion).
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class FlowTriggerManager {

    private final MaterialBindingManager materialBindingManager;

    /**
     * Handle event trigger with material-level mutual exclusion.
     *
     * Key changes in v1.2:
     * 1. Added TriggerCondition matching (see section 3.7)
     * 2. Added material-level binding to prevent duplicate consumption
     */
    public void handleEvent(Event event) {
        String sceneId = event.getSceneId();
        String sourceId = event.getSourceId();
        Material material = event.getMaterial();  // Get material from event

        // 1. Query all bindings for the scene
        List<ProcessFlowBinding> bindings = processFlowBindingRepository
            .findBySceneIdAndEnabledTrueOrderByPriorityAsc(sceneId);

        // 2. Match flows using TriggerCondition (v1.2: extended matching)
        List<ProcessFlowBinding> matchedBindings = bindings.stream()
            .filter(binding -> matchesCondition(binding, event, material))
            .collect(Collectors.toList());

        if (matchedBindings.isEmpty()) {
            log.debug("No matching flow for event source: {}", sourceId);
            return;
        }

        // 3. Group by priority
        Map<Integer, List<ProcessFlowBinding>> groupedByPriority =
            matchedBindings.stream()
                .collect(Collectors.groupingBy(
                    ProcessFlowBinding::getPriority,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

        // 4. Execute by priority (same priority: check material binding)
        for (Map.Entry<Integer, List<ProcessFlowBinding>> entry : groupedByPriority.entrySet()) {
            List<ProcessFlowBinding> samePriorityBindings = entry.getValue();

            if (samePriorityBindings.size() == 1) {
                // Single flow: check binding, then execute
                ProcessFlowBinding binding = samePriorityBindings.get(0);
                if (materialBindingManager.tryBind(material.getId(), binding.getFlowId(), binding.getId())) {
                    try {
                        executeFlow(binding, event);
                    } finally {
                        // Always release binding after execution
                        materialBindingManager.releaseBinding(material.getId());
                    }
                }
            } else {
                // Multiple flows with same priority: sequential check
                boolean executed = false;
                for (ProcessFlowBinding binding : samePriorityBindings) {
                    if (materialBindingManager.tryBind(material.getId(), binding.getFlowId(), binding.getId())) {
                        try {
                            executeFlow(binding, event);
                            executed = true;
                            break;  // Only one flow should process the material
                        } finally {
                            materialBindingManager.releaseBinding(material.getId());
                        }
                    }
                }

                if (!executed) {
                    log.warn("Material {} already bound to another flow, skipped all same-priority flows",
                        material.getId());
                }
            }
        }
    }

    /**
     * Check if binding matches event and material (v1.3: with consistency validation).
     *
     * Matching criteria:
     * 1. entryPointId == sourceId (basic, from process_flow_bindings.entry_point_id)
     * 2. v1.3: Validate trigger_condition.entryPointId consistency
     * 3. Material attributes match (type, grade, batch)
     * 4. Process tag match (processTag)
     * 5. Custom condition (customCondition)
     *
     * @param binding Process flow binding
     * @param event Trigger event
     * @param material Material (can be null)
     * @return true if binding matches
     */
    private boolean matchesCondition(ProcessFlowBinding binding, Event event, Material material) {
        // Basic check: entry point ID (from process_flow_bindings table)
        String bindingEntryPointId = binding.getEntryPointId();
        if (!bindingEntryPointId.equals(event.getSourceId())) {
            return false;
        }

        // Get trigger condition from binding (from process_flow_bindings.trigger_condition JSON)
        TriggerCondition condition = binding.getTriggerCondition();
        if (condition == null) {
            // No additional condition, basic match is sufficient
            return true;
        }

        // v1.3: Validate consistency between binding.entry_point_id and condition.entryPointId
        if (!bindingEntryPointId.equals(condition.getEntryPointId())) {
            // This is a CONFIGURATION ERROR - log warning and skip this binding
            log.warn("Binding {} has inconsistent entryPointId: binding.entry_point_id='{}' but trigger_condition.entryPointId='{}'. Skipping.",
                binding.getId(), bindingEntryPointId, condition.getEntryPointId());
            return false;  // Skip invalid binding
        }

        // Extended matching (v1.2+)
        return condition.matches(event, material);
    }
}
```

---

### 5.2 Human 搬运模型的实现级别（关键点 2）

#### 5.2.1 Human 是否进入 TrafficManager

**决策：Human 进入 TrafficManager，但使用简化模型**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Human 搬运模型实现级别（最终决策）                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【核心决策】                                                                 │
│  ✅ Human 进入 TrafficManager（统一入口）                                    │
│  ❌ Human 不使用 PathPlanner（无路径规划）                                   │
│  ⚠️  Human 仅在 SafetyZone 处申请通行（不占用 ControlPoint/Edge）              │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              Human vs AGV/OHT 交通管理差异对比                        │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  特性               │ AGV/OHT              │ Human                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  进入 TrafficManager │ 是                   │ 是                      │   │
│  │  使用 PathPlanner    │ 是（路网/轨道）      │ 否（直线移动）         │   │
│  │  占用 ControlPoint   │ 是                   │ 否                      │   │
│  │  占用 Edge           │ 是                   │ 否                      │   │
│  │  占用 SafetyZone     │ 否（概念不存在）      │ 是（安全优先）         │   │
│  │  移动时间计算        │ distance/speed       │ 直线距离 × 疲劳因子    │   │
│  │  冲突处理            │ 算法仲裁             │ 人工优先（安全）       │   │
│  │  路径记录            │ 详细节点序列          │ 起点 → 终点           │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 5.2.1a SafetyZone 数据结构定义（v1.3 新增）

> **背景**：Human 仅占用 SafetyZone，因此需要明确定义 SafetyZone 的数据结构和存储方式。

**SafetyZone 核心概念**：
- **用途**：人车混合区域的安全仲裁与限流
- **特性**：Human 在此区域有最高优先级（安全优先原则）
- **关键约束**：Human **不占用** ControlPoint/Edge，**只占用** SafetyZone

**SafetyZone 与 ControlPoint/Edge 职责分离（v1.3 澄清）**：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              SafetyZone vs ControlPoint/Edge 职责分离                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【核心原则】                                                                 │
│  ✅ ControlPoint/Edge  → 仅供 AGV/OHT 使用（车辆间冲突仲裁）                    │
│  ✅ SafetyZone        → 供 Human + AGV/OHT 使用（人车混合冲突仲裁）             │
│  ✅ Human 永远不占用 ControlPoint/Edge                                        │
│  ✅ AGV/OHT 可以同时占用 ControlPoint/Edge 和 SafetyZone                       │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              职责对比表                                              │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  维度               │ ControlPoint/Edge  │ SafetyZone              │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  适用实体           │ AGV, OHT            │ Human, AGV, OHT        │   │
│  │  主要用途           │ 路径冲突仲裁         │ 安全仲裁 + 限流        │   │
│  │  优先级规则         │ 按任务优先级         │ Human 优先（安全）     │   │
│  │  占用方式           │ 可配置，默认独占     │ 共享（可多人+车）       │   │
│  │  容量限制           │ 可配置，默认 1       │ 可配置（默认多实体）    │   │
│  │  几何约束           │ 点/线（路径节点）     │ 面（区域）              │   │
│  │  与 PathPlanner 关系 │ 强依赖（路径规划）   │ 弱依赖（区域检查）      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  【典型场景】                                                                 │
│                                                                             │
│  场景 1: AGV 在过道行驶                                                      │
│    ┌─────────────────────────────────────────────────────────────────┐     │
│    │  AGV 路径: CP-A → Edge-1 → CP-B → Edge-2 → CP-C                   │     │
│    │  仲裁: 在每个 CP 处与其他 AGV 仲裁（ControlPoint 冲突仲裁）         │     │
│    │  SafetyZone: 无涉及（AGV 不经过 SafetyZone）                        │     │
│    └─────────────────────────────────────────────────────────────────┘     │
│                                                                             │
│  场景 2: Human 在过道搬运晶圆盒                                              │
│    ┌─────────────────────────────────────────────────────────────────┐     │
│    │  Human 路径: Stocker → (直线移动) → Machine                       │     │
│    │  不占用: 无 ControlPoint，无 Edge                                  │     │
│    │  SafetyZone: 检查是否需要穿过 "ZONE-MAIN-AISLE"                   │     │
│    │            → 若穿过，申请 SafetyZone 访问（安全优先）               │     │
│    └─────────────────────────────────────────────────────────────────┘     │
│                                                                             │
│  场景 3: Human 与 AGV 在主过道相遇（人车混合）                               │
│    ┌─────────────────────────────────────────────────────────────────┐     │
│    │  AGV: CP-1 → Edge-MAIN → CP-2                                     │     │
│    │  Human: Stocker → Machine (穿过 Edge-MAIN 所在区域)                │     │
│    │                                                                  │     │
│    │  AGV 占用: CP-1, Edge-MAIN, CP-2                                  │     │
│    │  Human 占用: ZONE-MAIN-AISLE (覆盖 Edge-MAIN 区域)                 │     │
│    │                                                                  │     │
│    │  冲突点: SafetyZone.resolveSafetyZoneConflict()                   │     │
│    │  → Human 获得优先权（安全优先）                                    │     │
│    │  → AGV 在 Edge-MAIN 上等待                                        │     │
│    └─────────────────────────────────────────────────────────────────┘     │
│                                                                             │
│  场景 4: 多人同时进入同一区域                                                 │
│    ┌─────────────────────────────────────────────────────────────────┐     │
│    │  ZONE-STOCKER-ENTRANCE: maxHumans=5, maxVehicles=1               │     │
│    │                                                                  │     │
│    │  Human-A 进入: currentHumans=1/5 ✓                               │     │
│    │  Human-B 进入: currentHumans=2/5 ✓                               │     │
│    │  ...                                                             │     │
│    │  Human-E 进入: currentHumans=5/5 ✓                               │     │
│    │  Human-F 请求: REJECTED (容量限制)                                │     │
│    │  AGV-1 请求:   REJECTED (已达 maxVehicles 限制)                    │     │
│    └─────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**关键设计约束（v1.3 明确）**：

1. **Human 不调用 PathPlanner**
   - Human 移动是点对点直线，不经过路网节点
   - 因此 Human 无法"占用"路径上的 ControlPoint/Edge

2. **SafetyZone 是空间概念，不是路径概念**
   - SafetyZone 定义一个地理区域（面），不是路径上的点
   - Human 检查路径是否与 SafetyZone 相交，若有则申请访问

3. **AGV/OHT 可同时占用两者**
   - AGV 在路径上占用 ControlPoint/Edge（用于与其他 AGV 仲裁）
   - 如果 AGV 所在位置有 SafetyZone，同时也在 SafetyZone 中（用于与人仲裁）

4. **冲突仲裁分层处理**
   - 第一层：ControlPoint 冲突仲裁（仅 AGV/OHT）
   - 第二层：SafetyZone 冲突仲裁（Human + AGV/OHT，Human 优先）

**数据结构定义**：

```java
/**
 * Safety zone for human-vehicle shared areas.
 *
 * SafetyZone represents a geographic area where humans and vehicles may intersect.
 * Unlike ControlPoints (used for vehicle-vehicle conflicts), SafetyZone is
 * specifically designed for human-vehicle conflict resolution with safety-first priority.
 *
 * @author shentw
 * @version 1.3
 * @since 2026-02-06
 */
public class SafetyZone {

    // Basic identification
    private String id;                  // SafetyZone ID (e.g., "ZONE-FAB1-MAIN")
    private String name;                // Zone name (e.g., "Fab 1 Main Aisle")
    private String tenantId;            // Tenant ID (multi-tenant isolation)

    // Geometry definition
    private GeometryType geometryType;   // CIRCLE, RECTANGLE, POLYGON
    private List<Position> boundary;    // Boundary points (for polygon)
    private Position center;            // Center point (for circle)
    private double radius;              // Radius (for circle, in meters)
    private double width;               // Width (for rectangle, in meters)
    private double height;              // Height (for rectangle, in meters)

    // Capacity and priority
    private int maxHumans;              // Maximum humans allowed (0 = unlimited)
    private int maxVehicles;            // Maximum vehicles allowed (0 = unlimited)
    private int currentHumans;          // Current human count
    private int currentVehicles;        // Current vehicle count
    private Priority accessPriority;    // Default access priority (HUMAN_FIRST, VEHICLE_FIRST, FIFO)

    // Associated entities
    private List<String> connectedPathIds;     // Connected path IDs (for routing)
    private List<String> adjacentZoneIds;      // Adjacent zone IDs
    private List<String> monitoredEntityIds;   // Monitored entity IDs (e.g., nearby machines)

    // Configuration
    private boolean enabled;             // Is zone active
    private Map<String, Object> properties;  // Extension properties

    /**
     * Check if a position is within this safety zone.
     *
     * @param position Position to check
     * @return true if position is within zone boundaries
     */
    public boolean contains(Position position) {
        switch (geometryType) {
            case CIRCLE:
                double distance = center.distanceTo(position);
                return distance <= radius;

            case RECTANGLE:
                return position.getX() >= center.getX() - width / 2
                    && position.getX() <= center.getX() + width / 2
                    && position.getY() >= center.getY() - height / 2
                    && position.getY() <= center.getY() + height / 2;

            case POLYGON:
                return isPointInPolygon(position, boundary);

            default:
                return false;
        }
    }

    /**
     * Check if zone has capacity for additional entities.
     *
     * @param humanCount Additional humans
     * @param vehicleCount Additional vehicles
     * @return true if zone can accommodate the entities
     */
    public boolean hasCapacity(int humanCount, int vehicleCount) {
        if (maxHumans > 0 && currentHumans + humanCount > maxHumans) {
            return false;
        }
        if (maxVehicles > 0 && currentVehicles + vehicleCount > maxVehicles) {
            return false;
        }
        return true;
    }

    /**
     * Geometry types supported.
     */
    public enum GeometryType {
        CIRCLE,      // Circle defined by center + radius
        RECTANGLE,   // Rectangle defined by center + width + height
        POLYGON      // Polygon defined by list of boundary points
    }

    /**
     * Access priority modes.
     */
    public enum Priority {
        HUMAN_FIRST,   // Humans always have priority
        VEHICLE_FIRST, // Vehicles have priority (rare)
        FIFO,          // First-come-first-served
        PRIORITY_BASED // Based on task priority
    }
}
```

**SafetyZone 实体类型 Schema 对齐方案（v1.5 明确）**：

> **存储方案决策**：SafetyZone 作为实体类型存储在 `scenes.entities` JSON 数组中
>
> **选择理由**：
> - SafetyZone 是场景级别的空间定义，与 Machine、Stocker 等实体类似
> - 作为实体类型存储，便于前端统一管理和渲染
> - 不需要独立的数据库表（避免过度规范化）
>
> **需要在 logistics-entities spec 中补充**：
>
> 1. **新增实体类型定义**
>    ```markdown
>    ## SafetyZone（安全区实体）
>
>    ### 基本属性
>    - `id`: 安全区 ID（唯一）
>    - `name`: 安全区名称
>    - `type`: "SAFETY_ZONE"（实体类型标识）
>
>    ### 几何属性
>    - `geometryType`: "CIRCLE" | "RECTANGLE" | "POLYGON"
>    - `center`: { x, y, z } 中心点坐标
>    - `radius`: 半径（CIRCLE 类型）
>    - `width`, `height`: 宽高（RECTANGLE 类型）
>    - `boundary`: 边界点列表（POLYGON 类型）
>
>    ### 容量与优先级
>    - `maxHumans`: 最大人数（0 = 无限制）
>    - `maxVehicles`: 最大车数（0 = 无限制）
>    - `accessPriority`: "HUMAN_FIRST" | "VEHICLE_FIRST" | "FIFO" | "PRIORITY_BASED"
>
>    ### 关联信息
>    - `connectedPathIds`: 关联路径 ID 列表
>    - `adjacentZoneIds`: 相邻安全区 ID 列表
>    - `monitoredEntityIds`: 监控实体 ID 列表
>    ```
>
> 2. **前端读取路径**
>    - 前端从 `GET /api/v1/scenes/{sceneId}` 获取场景数据
>    - 解析 `response.entities` 数组，过滤 `type === "SAFETY_ZONE"` 的实体
>    - 初始化 SafetyZone 实例并渲染到 3D 视图
>
> 3. **后端序列化/反序列化**
>    - 场景保存时：将 List<SafetyZone> 序列化为 JSON 存入 `scenes.entities`
>    - 场景加载时：从 JSON 反序列化为 SafetyZone 对象列表
>
> **与独立表方案的对比**（v1.5 已拒绝独立表方案）：
>
> | 维度 | 实体数组方案（已选） | 独立表方案（已拒绝） |
> |------|---------------------|-------------------|
> | 数据一致性 | 强（与场景原子性） | 弱（需事务保证） |
> | 前端复杂度 | 低（统一读取） | 高（需额外请求） |
> | 查询性能 | N/A（场景级加载） | 高（可独立查询） |
> | 适用场景 | SafetyZone 数量少 | SafetyZone 数量多且复杂 |
>
> **结论**：选择实体数组方案（当前设计），暂不采用独立表方案。

**场景 JSON 存储结构（v1.5 修正：实体数组格式）**：

SafetyZone 作为场景的一部分，存储在 `scenes` 表的 `entities` JSON 数组中（注意：使用**实体数组格式**，而非分组对象格式）：

```json
{
  "sceneId": "scene-wafer-fab-01",
  "name": "晶圆厂 01 号线",
  "entities": [
    {
      "id": "Machine-A",
      "type": "MACHINE",
      "name": "Lithography Machine A",
      ...
    },
    {
      "id": "AGV-01",
      "type": "AGV_VEHICLE",
      "name": "AGV Vehicle 01",
      ...
    },
    {
      "id": "CP-MAIN-01",
      "type": "CONTROL_POINT",
      "name": "Main Control Point 01",
      ...
    },
    {
      "id": "ZONE-FAB1-MAIN",
      "type": "SAFETY_ZONE",
      "name": "Fab 1 Main Aisle",
      "geometryType": "RECTANGLE",
      "center": {"x": 100.0, "y": 50.0, "z": 0.0},
      "width": 20.0,
      "height": 5.0,
      "maxHumans": 10,
      "maxVehicles": 2,
      "accessPriority": "HUMAN_FIRST",
      "connectedPathIds": ["PATH-AGV-01", "PATH-AGV-02"],
      "monitoredEntityIds": ["Machine-A", "Machine-B"],
      "enabled": true
    },
    {
      "id": "ZONE-STOCKER-ENTRANCE",
      "type": "SAFETY_ZONE",
      "name": "Stocker Entrance Zone",
      "geometryType": "CIRCLE",
      "center": {"x": 200.0, "y": 100.0, "z": 0.0},
      "radius": 3.0,
      "maxHumans": 5,
      "maxVehicles": 1,
      "accessPriority": "HUMAN_FIRST",
      "enabled": true
    }
  ],
  "paths": [...]
}
```

**v1.5 重要说明**：
- `entities` 是**数组**，不是对象
- 每个实体通过 `type` 字段区分类型（`MACHINE`, `AGV_VEHICLE`, `CONTROL_POINT`, `SAFETY_ZONE` 等）
- SafetyZone 的 `type` 值为 `"SAFETY_ZONE"`
- 前端通过 `entities.filter(e => e.type === "SAFETY_ZONE")` 获取所有安全区

---

**【已弃用】独立表方案（v1.5 标记为历史方案，仅供参考）**：

> **弃用原因**：已选择实体数组方案（见上方），独立表方案不再考虑。

如果未来 SafetyZone 需求发生重大变化（例如数量大幅增加到数千个、需要复杂跨场景查询），可重新评估此方案。当前版本不采用。

```sql
-- ⚠️ 已弃用：以下 SQL 仅供参考，当前不采用此方案
-- 仅供参考：当前不采用此方案
-- 类型说明：已对齐全库 CHAR(36) / DATETIME(3) 规范（v1.5）
CREATE TABLE safety_zones (
    tenant_id       CHAR(36)     NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    id              CHAR(36)     NOT NULL,
    scene_id        CHAR(36)     NOT NULL,
    name            VARCHAR(255),
    geometry_type   VARCHAR(32)  NOT NULL,  -- CIRCLE, RECTANGLE, POLYGON
    center_x        DOUBLE       NOT NULL,
    center_y        DOUBLE       NOT NULL,
    center_z        DOUBLE       DEFAULT 0,
    radius          DOUBLE,                  -- For CIRCLE
    width           DOUBLE,                  -- For RECTANGLE
    height          DOUBLE,                  -- For RECTANGLE
    boundary        JSON,                    -- For POLYGON (list of points)
    max_humans      INT         DEFAULT 0,   -- 0 = unlimited
    max_vehicles    INT         DEFAULT 0,   -- 0 = unlimited
    access_priority VARCHAR(32)  DEFAULT 'HUMAN_FIRST',
    enabled         BOOLEAN      DEFAULT TRUE,
    properties      JSON,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (tenant_id, scene_id) REFERENCES scenes(tenant_id, id) ON DELETE CASCADE,
    INDEX idx_zones_scene (tenant_id, scene_id),
    INDEX idx_zones_enabled (tenant_id, enabled)
);
```

**SafetyZone 与 Human 的交互**：

```java
/**
 * Check which safety zones a human needs to traverse.
 *
 * @param human Human operator
 * @param from Start position
 * @param to End position
 * @return List of safety zones that intersect the path
 */
public List<SafetyZone> getRequiredSafetyZones(Human human, Position from, Position to) {
    List<SafetyZone> allZones = scene.getSafetyZones();
    List<SafetyZone> requiredZones = new ArrayList<>();

    // For each zone, check if the human's path intersects it
    for (SafetyZone zone : allZones) {
        if (!zone.isEnabled()) {
            continue;
        }

        // Simple check: if either endpoint is in zone, zone is required
        // (More sophisticated: check line segment intersection)
        if (zone.contains(from) || zone.contains(to)) {
            requiredZones.add(zone);
        }
    }

    return requiredZones;
}
```

#### 5.2.2 Human 移动时间计算

**基于调研的疲劳模型**：

参考 [Modelling physical fatigue and recovery patterns (2025)](https://www.tandfonline.com/doi/full/10.1080/00207543.2025.2580539) 和 [ISO 11228-1:2021](https://www.iso.org/obp/ui/#iso:std:iso:11228:-1:ed-2:v1:en)：

```java
/**
 * Human transport movement model (simplified, no path planning).
 *
 * Walking speed standards (ISO 11228-1:2021):
 * - Moderate walking: 0.5–1.0 m/s (horizontal, ≥ 3kg load)
 * - Industry typical: 0.25–1.5 m/s (variable based on conditions)
 * - Common simulation parameter: 1.0 m/s
 *
 * Fatigue modeling based on research:
 * - Speed degrades over time due to physical fatigue
 * - Recovery periods allow speed restoration
 * - Work/rest arrangements affect cumulative fatigue
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class HumanTransportModel {

    /**
     * Human movement calculation (direct path, no waypoints).
     */
    public static class MovementCalculation {

        /**
         * Calculate movement time for human operator.
         *
         * Factors considered:
         * 1. Distance (straight line)
         * 2. Base walking speed (ISO 11228-1:2021 standard)
         * 3. Fatigue factor (work time based)
         * 4. Load factor (carrying material)
         * 5. Recovery from breaks
         *
         * @param human Human operator
         * @param from Start position
         * @param to End position
         * @return Movement time in seconds (simulation time)
         */
        public static double calculateTime(Human human, Position from, Position to) {
            // Step 1: Calculate straight-line distance (meters)
            double distance = from.distanceTo(to);

            // Step 2: Get base walking speed (m/s)
            // ISO 11228-1:2021: 0.5–1.0 m/s for moderate walking with ≥ 3kg load
            double baseSpeed = human.getWalkingSpeed(); // Typically 1.0 m/s

            // Step 3: Apply fatigue factor (degrades speed over time)
            double fatigueFactor = calculateFatigueFactor(human);

            // Step 4: Apply load factor (carrying material slows down)
            double loadFactor = calculateLoadFactor(human);

            // Step 5: Calculate actual speed
            double actualSpeed = baseSpeed / (fatigueFactor * loadFactor);

            // Step 6: Calculate time = distance / speed
            return distance / actualSpeed;
        }

        /**
         * Fatigue factor based on work time.
         *
         * Model: Speed degrades as cumulative work time increases.
         * Recovery: Breaks and rest periods restore speed.
         *
         * @return Fatigue multiplier (1.0 = fresh, >1.0 = fatigued)
         */
        private static double calculateFatigueFactor(Human human) {
            long workedMinutes = human.getWorkedTimeMinutes();
            long lastBreakMinutesAgo = human.getTimeSinceLastBreak();

            // Base fatigue (work time based)
            double baseFatigue;
            if (workedMinutes < 120) {
                baseFatigue = 1.0;  // Fresh (0-2 hours)
            } else if (workedMinutes < 240) {
                baseFatigue = 1.1;  // Mild fatigue (2-4 hours)
            } else if (workedMinutes < 360) {
                baseFatigue = 1.2;  // Moderate fatigue (4-6 hours)
            } else {
                baseFatigue = 1.35; // Significant fatigue (>6 hours)
            }

            // Recovery factor (recent break reduces fatigue)
            double recoveryFactor;
            if (lastBreakMinutesAgo < 15) {
                recoveryFactor = 1.0;  // Fully recovered
            } else if (lastBreakMinutesAgo < 60) {
                recoveryFactor = 0.95; // Partially recovered
            } else {
                recoveryFactor = 1.0;  // No recovery effect
            }

            return Math.max(1.0, baseFatigue * recoveryFactor);
        }

        /**
         * Load factor based on carried material.
         *
         * Based on ISO 11228-1:2021 ergonomic guidelines:
         * - Light load (< 3kg): 1.0 (no effect)
         * - Medium load (3-10kg): 1.15
         * - Heavy load (10-20kg): 1.3
         * - Very heavy (> 20kg): 1.5 (not recommended)
         *
         * @return Load multiplier (≥ 1.0)
         */
        private static double calculateLoadFactor(Human human) {
            if (!human.hasLoad()) {
                return 1.0;
            }

            double loadWeight = human.getCurrentLoadWeight(); // kg

            if (loadWeight < 3.0) {
                return 1.0;   // Light load
            } else if (loadWeight < 10.0) {
                return 1.15;  // Medium load
            } else if (loadWeight < 20.0) {
                return 1.3;   // Heavy load
            } else {
                return 1.5;   // Very heavy (discourage)
            }
        }
    }

    /**
     * Human traffic request (simplified, no path planning).
     */
    public static class HumanTrafficRequest {
        private final String humanId;
        private final Position from;
        private final Position to;
        private final List<SafetyZone> requiredZones;

        /**
         * Request traffic access for human movement.
         *
         * Unlike AGV/OHT, humans only request SafetyZone access,
         * not ControlPoint or Edge reservation.
         */
        public TrafficRequestResult request(TrafficManager mgr) {
            // Step 1: Check if any SafetyZone needs to be crossed
            if (requiredZones.isEmpty()) {
                // No zone crossing needed, approve immediately
                return TrafficRequestResult.approved();
            }

            // Step 2: Request access for each required zone
            for (SafetyZone zone : requiredZones) {
                TrafficRequestResult result = mgr.requestZoneAccess(
                    zone.getId(),
                    humanId,
                    Priority.LOW,  // Human has low priority but safety-first
                    "human"
                );

                if (!result.isApproved()) {
                    // Zone access denied, must wait
                    return result;
                }
            }

            // All zones approved
            return TrafficRequestResult.approved();
        }
    }
}
```

**速度参数总结**：

| 参数 | 值 | 来源 |
|------|-----|------|
| 基础步行速度 | 1.0 m/s | ISO 11228-1:2021 (常用值) |
| 中等步行速度范围 | 0.5–1.0 m/s | ISO 11228-1:2021 (≥3kg 负载) |
| 行业典型范围 | 0.25–1.5 m/s | 研究文献 |
| 疲劳因子 | 1.0–1.35 | 累积工作时间 |
| 负载因子 | 1.0–1.5 | 负载重量 |

#### 5.2.3 Human 冲突处理

**安全优先仲裁（修正版 v1.2）**：

> **重要修正（v1.2）**：
> - Human **不占用** ControlPoint/Edge，因此 **不在 ControlPoint 上进行人车仲裁**
> - Human 冲突仲裁 **仅发生在 SafetyZone**
> - ControlPoint 冲突仲裁仅用于 AGV/OHT 之间的冲突

```java
/**
 * Conflict resolver with human safety priority.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class ConflictResolver {

    /**
     * Resolve conflict at ControlPoint (VEHICLES ONLY).
     *
     * IMPORTANT: Humans do NOT use ControlPoints, so human-vehicle
     * conflicts are NOT resolved here. Only AGV/OHT conflicts.
     *
     * @param cp ControlPoint (only for vehicles)
     * @param candidates List of vehicles (AGV/OHT, NO humans)
     * @return Selected vehicle
     */
    public Vehicle resolveControlPointConflict(ControlPoint cp, List<Vehicle> candidates) {
        // Assert: No humans in candidates (they don't use ControlPoints)
        if (candidates.stream().anyMatch(v -> v.getType() == EntityType.HUMAN)) {
            throw new IllegalStateException("Humans should not be in ControlPoint conflict resolution");
        }

        // Standard vehicle arbitration (AGV/OHT only)
        return resolveVehicleConflict(cp, candidates);
    }

    /**
     * Resolve conflict at SafetyZone (HUMANS + VEHICLES).
     *
     * Safety-first principle: Human always has priority over vehicles.
     * This is the ONLY place where human-vehicle arbitration occurs.
     *
     * Reference: [Design of AGV systems in shared environments](https://www.sciencedirect.com/science/article/pii/S2405896320335795)
     *
     * @param zone SafetyZone (shared by humans and vehicles)
     * @param candidates List of all entities requesting access
     * @return Selected entity
     */
    public Entity resolveSafetyZoneConflict(SafetyZone zone, List<Entity> candidates) {
        // Step 1: Separate humans and vehicles
        List<Entity> humans = filterByType(candidates, EntityType.HUMAN);
        List<Entity> vehicles = filterByType(candidates, EntityType.VEHICLE);

        // Step 2: Human-vehicle conflict → Human wins (safety priority)
        if (!humans.isEmpty() && !vehicles.isEmpty()) {
            log.debug("Human-vehicle conflict at SafetyZone {}: Human prioritized (safety)", zone.getId());
            return humans.get(0);  // First human gets access
        }

        // Step 3: Human-only conflict → Standard arbitration
        if (!humans.isEmpty()) {
            return resolveHumanConflict(zone, humans);
        }

        // Step 4: Vehicle-only conflict → Standard arbitration
        return resolveVehicleConflict(zone, vehicles);
    }

    /**
     * Resolve conflict between multiple humans at SafetyZone.
     *
     * Human-human arbitration factors:
     * 1. Task priority (emergency > normal)
     * 2. Waiting time (FIFO)
     * 3. Random (tie-breaker)
     */
    private Entity resolveHumanConflict(SafetyZone zone, List<Entity> humans) {
        return humans.stream()
            .max(Comparator.comparingInt(h -> h.getTask().getPriority())
                .thenComparingLong(h -> h.getWaitingTime()))
            .orElse(humans.get(0));
    }

    /**
     * Resolve conflict between vehicles (can be at ControlPoint OR SafetyZone).
     */
    private Entity resolveVehicleConflict(Object location, List<Entity> vehicles) {
        // Standard vehicle arbitration (AGV/OHT)
        // Factors: priority, waiting time, distance, etc.
        return vehicles.stream()
            .max(Comparator.comparingInt(v -> v.getTask().getPriority())
                .thenComparingLong(v -> v.getWaitingTime()))
            .orElse(vehicles.get(0));
    }
}
```

---

### 5.3 任务生成与上下游依赖检查的触发时机（关键点 3）

#### 5.3.1 MACHINE_COMPLETED 触发流程

**决策：先校验，后生成**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              MACHINE_COMPLETED 事件处理完整流程（最终版）                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  事件: Machine-A 完成加工                                                    │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 1: 事件验证 (Event Validation)                                │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  ✓ 验证事件源有效                                                     │   │
│  │  ✓ 验证物料存在                                                       │   │
│  │  ✓ 获取关联的 ProcessFlow                                            │   │
│  │  ✓ 确定当前工序与下一工序                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼                                                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 2: 上下游依赖检查 (Dependency Validation)                    │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  canStartStep(nextStep, material)?                                  │   │
│  │       │                                                             │   │
│  │       ├─ YES → 继续下一步                                            │   │
│  │       │                                                             │   │
│  │       └─ NO → 处理依赖未满足                                        │   │
│  │                  │                                                 │   │
│  │                  ├─ 记录物料状态为 WAITING_FOR_PREDECESSOR         │   │
│  │                  └─ 注册前置工序完成监听器                           │   │
│  │                    (前置完成时重新触发)                               │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼ (依赖已满足)                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 3: WIP 限制检查 (WIP Validation)                               │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  currentWIP < nextStep.getWipLimit()?                              │   │
│  │       │                                                             │   │
│  │       ├─ YES → 继续下一步                                            │   │
│  │       │                                                             │   │
│  │       └─ NO → 应用 WIP 超限策略                                      │   │
│  │                  │                                                 │   │
│  │                  ├─ DEFER_RETRY (默认)                              │   │
│  │                  │   └─ 注册 WIP 阈值监听器                         │   │
│  │                  │                                                │   │
│  │                  └─ BLOCK_AND_WAIT                                  │   │
│  │                      └─ 物料保持 WAITING 状态                        │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼ (WIP 检查通过)                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 4: 下游可用性检查 (Downstream Availability Validation)        │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  downstreamEntity.isAvailable()?                                   │   │
│  │       │                                                             │   │
│  │       ├─ YES → 继续下一步                                            │   │
│  │       │                                                             │   │
│  │       └─ NO → 应用下游不可用策略                                      │   │
│  │                  │                                                 │   │
│  │                  ├─ BUFFER_TO_STORAGE                               │   │
│  │                  │   └─ 生成暂存任务                                 │   │
│  │                  │                                                │   │
│  │                  ├─ BLOCK_AND_WAIT                                  │   │
│  │                  │   └─ 注册下游可用监听器                           │   │
│  │                  │                                                │   │
│  │                  ├─ DEFER_RETRY                                     │   │
│  │                  │   └─ 进入重试队列                                 │   │
│  │                  │                                                │   │
│  │                  └─ REROUTE_ALTERNATE                                │   │
│  │                      └─ 尝试替代路径                                 │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│       │                                                                     │
│       ▼ (所有校验通过)                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │  Step 5: 生成任务 (Task Generation)                                  │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  ✓ 构建 Task 对象                                                    │   │
│  │  ✓ TaskQueue.enqueue(task)                                         │   │
│  │  ✓ 返回 taskId                                                      │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**代码实现**：

```java
/**
 * Machine completed event handler with comprehensive validation.
 */
public class MachineCompletedEventHandler {

    /**
     * Handle MACHINE_COMPLETED event with full validation pipeline.
     *
     * Validation order:
     * 1. Event validation (event source, material existence)
     * 2. Dependency validation (predecessor completion)
     * 3. WIP validation (downstream capacity)
     * 4. Downstream availability validation
     * 5. Resource availability validation
     * 6. Path feasibility validation
     *
     * Only after ALL validations pass, task is generated.
     */
    public void handleMachineCompleted(MachineCompletedEvent event) {
        // Step 1: Event validation
        EventValidationResult evResult = validateEvent(event);
        if (!evResult.isValid()) {
            log.error("Event validation failed: {}", evResult.getErrorMessage());
            return;
        }

        Material material = evResult.getMaterial();
        ProcessFlow flow = evResult.getProcessFlow();
        ProcessStep currentStep = evResult.getCurrentStep();
        ProcessStep nextStep = evResult.getNextStep();

        // Step 2: Dependency validation
        if (!canStartStep(nextStep, material)) {
            handleDependencyNotSatisfied(nextStep, material);
            return;  // Don't generate task
        }

        // Step 3: WIP validation
        WIPValidationResult wipResult = validateWIP(nextStep);
        if (!wipResult.isValid()) {
            handleWIPLimitExceeded(nextStep, material, wipResult.getStrategy());
            return;  // Don't generate task (unless strategy says otherwise)
        }

        // Step 4: Downstream availability validation
        DownstreamValidationResult dsResult = validateDownstream(nextStep);
        if (!dsResult.isAvailable()) {
            handleDownstreamUnavailable(nextStep, material, dsResult.getStrategy());
            return;  // Don't generate task (handled by strategy)
        }

        // Step 5: Resource availability validation
        ResourceValidationResult resResult = validateResources(nextStep);
        if (!resResult.hasAvailableResources()) {
            // v1.3: Strategy selection - see handleNoResourcesAvailable() for details
            handleNoResourcesAvailable(nextStep, material);
            return;  // Don't generate task (or enqueue based on strategy)
        }

        // Step 6: Path feasibility validation
        PathValidationResult pathResult = validatePath(nextStep);
        if (!pathResult.isFeasible()) {
            handleNoFeasiblePath(nextStep, material);
            return;  // Don't generate task
        }

        // ALL VALIDATIONS PASSED - Generate task
        Task task = generateTask(nextStep, material);
        taskQueue.enqueue(task);

        log.info("Task generated successfully: {} for material: {}",
            task.getId(), material.getId());
    }

    /**
     * Check if all predecessor steps are completed.
     */
    private boolean canStartStep(ProcessStep step, Material material) {
        List<String> predecessorIds = step.getPredecessorIds();

        if (predecessorIds.isEmpty()) {
            return true;  // No dependencies
        }

        for (String predId : predecessorIds) {
            ProcessStep pred = flow.getStep(predId);
            if (!isCompleted(pred, material)) {
                log.debug("Predecessor {} not completed for material: {}",
                    predId, material.getId());
                return false;
            }
        }

        return true;
    }

    /**
     * Handle dependency not satisfied scenario.
     *
     * Strategy: Register listener for predecessor completion.
     * When predecessor completes, re-trigger this validation.
     */
    private void handleDependencyNotSatisfied(ProcessStep step, Material material) {
        // Set material state to waiting
        material.setState(MaterialState.WAITING_FOR_PREDECESSOR);
        material.setWaitingStepId(step.getId());

        // Register listener for predecessor completion
        for (String predId : step.getPredecessorIds()) {
            ProcessStep pred = flow.getStep(predId);
            if (!isCompleted(pred, material)) {
                // Register listener: when pred completes, re-check dependencies
                pred.registerCompletionListener(() -> {
                    if (canStartStep(step, material)) {
                        // Re-trigger event handling
                        eventBus.publish(new RetryTaskGenerationEvent(
                            step.getId(), material.getId()
                        ));
                    }
                });
            }
        }

        log.info("Material {} waiting for predecessors at step {}",
            material.getId(), step.getId());
    }

    /**
     * Handle no resources available scenario (v1.3: with strategy selection).
     *
     * DESIGN RATIONALE (v1.3):
     * -----------------------
     * When no transport resources are available, we have two strategies:
     *
     * 1. BLOCK_AND_RETRY (Default):
     *    - Don't generate task
     *    - Register listener for resource availability
     *    - Re-trigger validation when resource becomes available
     *    - Rationale: Prevents task queue from filling with tasks that can't be dispatched
     *
     * 2. ENQUEUE_AND_WAIT (Optional):
     *    - Generate task and enqueue it
     *    - Task sits in queue until resource becomes available
     *    - Scheduler will handle resource allocation
     *    - Rationale: Preserves event order, but may bloat queue
     *
     * The default is BLOCK_AND_RETRY because:
     * - DES engines typically don't benefit from "waiting tasks" in queue
     * - TaskQueue is more efficiently used for tasks ready to dispatch
     * - Re-triggering on resource availability maintains causality
     *
     * To change behavior, configure resourceValidationStrategy in ProcessStep.
     *
     * @param step Process step requiring resources
     * @param material The material
     */
    private void handleNoResourcesAvailable(ProcessStep step, Material material) {
        // Get strategy from step configuration (default: BLOCK_AND_RETRY)
        ResourceValidationStrategy strategy = step.getResourceValidationStrategy();
        if (strategy == null) {
            strategy = ResourceValidationStrategy.BLOCK_AND_RETRY;  // Default
        }

        switch (strategy) {
            case BLOCK_AND_RETRY:
                // Don't generate task, register listener
                material.setState(MaterialState.WAITING_FOR_RESOURCE);
                material.setWaitingStepId(step.getId());

                // Register listener for resource availability
                resourceManager.registerResourceAvailableListener(step.getTargetEntityId(), () -> {
                    if (validateResources(step).hasAvailableResources()) {
                        // Re-trigger task generation
                        eventBus.publish(new RetryTaskGenerationEvent(step.getId(), material.getId()));
                    }
                });

                log.info("Material {} waiting for resources at step {} (strategy: BLOCK_AND_RETRY)",
                    material.getId(), step.getId());
                break;

            case ENQUEUE_AND_WAIT:
                // Generate task and enqueue it anyway
                Task pendingTask = generateTask(step, material);
                pendingTask.setPendingResource(true);  // Mark as waiting for resource
                taskQueue.enqueue(pendingTask);

                log.info("Task {} generated for material {} at step {} (strategy: ENQUEUE_AND_WAIT, pending resource)",
                    pendingTask.getId(), material.getId(), step.getId());
                break;

            default:
                log.warn("Unknown resource validation strategy: {}, using default BLOCK_AND_RETRY", strategy);
                material.setState(MaterialState.WAITING_FOR_RESOURCE);
                break;
        }
    }

    /**
     * Resource validation strategy (v1.3: NEW).
     *
     * Defines behavior when transport resources are unavailable.
     */
    public enum ResourceValidationStrategy {
        /**
         * Don't generate task, wait for resources, then re-trigger.
         * Default behavior, prevents queue bloat.
         */
        BLOCK_AND_RETRY,

        /**
         * Generate task and enqueue it even if resources unavailable.
         * Task will wait in queue for resources.
         */
        ENQUEUE_AND_WAIT
    }
}
```

#### 5.3.2 DEFER_RETRY 的重试机制

**决策：事件驱动重试（非轮询）**

基于调研 [Understanding push vs poll in event-driven architectures](https://theburningmonk.com/2025/05/understanding-push-vs-poll-in-event-driven-architectures/)：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              DEFER_RETRY 重试机制（事件驱动）                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【决策理由】                                                                 │
│  ✅ 事件驱动更符合 DES 理念（事件驱动时间推进）                               │
│  ✅ 事件驱动延迟更低（实时响应）                                             │
│  ✅ 事件驱动无空转开销（资源效率高）                                          │
│  ❌ 轮询方式有延迟（检查间隔决定）                                           │
│  ❌ 轮询有空转开销（CPU 浪费）                                                │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                   重试触发事件类型                                   │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  事件类型                    │ 触发条件                              │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  PREDECESSOR_COMPLETED    │ 前置工序完成                            │   │
│  │  WIP_THRESHOLD_CLEARED    │ 下游 WIP 下降到阈值以下                  │   │
│  │  DOWNSTREAM_AVAILABLE     │ 下游设备变为可用                         │   │
│  │  RESOURCE_BECAME_AVAILABLE │ 搬运资源变得可用                        │   │
│  │  PATH_BECAME_FEASIBLE     │ 路径变为可行（阻塞解除）                 │   │
│  │  RETRY_TIMER_EXPIRED      │ 重试定时器到期（指数退避后）            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                  指数退避重试调度 (Exponential Backoff)            │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  重试次数           │ 延迟时间 (仿真秒)   │ 累计等待时间            │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  0 (初次尝试)     │ 0                   │ 0                       │   │
│  │  1 (第1次重试)     │ 5                   │ 5                       │   │
│  │  2 (第2次重试)     │ 10                  │ 15                      │   │
│  │  3 (第3次重试)     │ 20                  │ 35                      │   │
│  │  4 (第4次重试)     │ 40                  │ 75                      │   │
│  │  5+                │ maxDelay (最大延迟)  │ (根据配置)              │   │
│  │  >maxRetries       │ 放弃 (进入 Dead Letter Queue)                    │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  【v1.2 修正】参数可通过 system_config 配置：                                  │
│  - retry.maxRetries: 最大重试次数 (默认: 5)                                  │
│  - retry.baseDelay: 基础延迟秒数 (默认: 5)                                   │
│  - retry.maxDelay: 最大延迟秒数 (默认: 60)                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**代码实现（v1.2 修正版 - 参数可配置）**：

```java
/**
 * Retry configuration (v1.2: configurable via system_config).
 *
 * Configuration keys in system_config:
 * - retry.maxRetries: Maximum retry attempts (default: 5)
 * - retry.baseDelay: Base delay in seconds (default: 5)
 * - retry.maxDelay: Maximum delay in seconds (default: 60)
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class RetryConfig {

    private final int maxRetries;
    private final long baseDelaySeconds;
    private final long maxDelaySeconds;

    public RetryConfig(SystemConfig config) {
        this.maxRetries = config.getInt("retry.maxRetries", 5);
        this.baseDelaySeconds = config.getLong("retry.baseDelay", 5L);
        this.maxDelaySeconds = config.getLong("retry.maxDelay", 60L);
    }

    public int getMaxRetries() { return maxRetries; }
    public long getBaseDelaySeconds() { return baseDelaySeconds; }
    public long getMaxDelaySeconds() { return maxDelaySeconds; }
}

/**
 * Event-driven retry mechanism for deferred tasks (v1.2: configurable).
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-06
 */
public class DeferredTaskRetryManager {

    private final Map<String, DeferredTask> deferredTasks = new ConcurrentHashMap<>();
    private final EventBus eventBus;
    private final TaskQueue taskQueue;
    private final RetryConfig retryConfig;  // v1.2: Configurable

    /**
     * Constructor with retry configuration.
     */
    public DeferredTaskRetryManager(EventBus eventBus,
                                     TaskQueue taskQueue,
                                     RetryConfig retryConfig) {
        this.eventBus = eventBus;
        this.taskQueue = taskQueue;
        this.retryConfig = retryConfig;
    }

    /**
     * Register a deferred task for retry.
     *
     * @param task The task to retry
     * @param reason The reason for deferral
     * @param initialDelaySeconds Delay before first retry (simulation time)
     */
    public void registerDeferredTask(Task task, String reason, long initialDelaySeconds) {
        DeferredTask deferredTask = new DeferredTask();
        deferredTask.setTask(task);
        deferredTask.setReason(reason);
        deferredTask.setRetryCount(0);
        // v1.2: Use configured max retries
        deferredTask.setMaxRetries(retryConfig.getMaxRetries());
        deferredTask.setBaseDelaySeconds(initialDelaySeconds);

        String deferredTaskId = UUID.randomUUID().toString();
        deferredTasks.put(deferredTaskId, deferredTask);

        // Schedule retry event
        scheduleRetry(deferredTaskId, 0);

        log.info("Task {} deferred due to: {}, will retry in {}s (maxRetries: {})",
            task.getId(), reason, initialDelaySeconds, deferredTask.getMaxRetries());
    }

    /**
     * Schedule a retry event with exponential backoff.
     *
     * Based on industry best practices:
     * - [Queue-Based Exponential Backoff](https://dev.to/andreparis/queue-based-exponential-backoff-a-resilient-retry-pattern-for-distributed-systems-37f3)
     * - [A Guide to Retry Pattern](https://blog.bytebytego.com/p/a-guide-to-retry-pattern-in-distributed)
     *
     * @param deferredTaskId Deferred task ID
     * @param retryCount Current retry count
     */
    private void scheduleRetry(String deferredTaskId, int retryCount) {
        DeferredTask deferredTask = deferredTasks.get(deferredTaskId);
        if (deferredTask == null) {
            return;  // Task already completed or cancelled
        }

        // Calculate delay with exponential backoff (v1.2: uses configured max delay)
        long delaySeconds = calculateRetryDelay(deferredTask, retryCount);

        // Schedule retry event at future simulation time
        double retryTime = simulation.currentTime() + delaySeconds;

        RetryEvent retryEvent = new RetryEvent(
            deferredTaskId,
            retryCount + 1,
            retryTime
        );

        simulation.scheduleEvent(retryTime, () -> handleRetryEvent(retryEvent));

        log.debug("Scheduled retry #{} for task {} at {}s (delay: {}s)",
            retryCount + 1, deferredTask.getTask().getId(), retryTime, delaySeconds);
    }

    /**
     * Calculate retry delay with exponential backoff.
     *
     * Formula: min(baseDelay * 2^retryCount, maxDelay)
     *
     * v1.2: maxDelay is now configurable via retry.maxDelay
     *
     * @param deferredTask The deferred task
     * @param retryCount Current retry count
     * @return Delay in seconds (simulation time)
     */
    private long calculateRetryDelay(DeferredTask deferredTask, int retryCount) {
        long baseDelay = deferredTask.getBaseDelaySeconds();
        // v1.2: Use configured max delay instead of hardcoded value
        long maxDelay = retryConfig.getMaxDelaySeconds();
        long backoffDelay = (long) (baseDelay * Math.pow(2, retryCount));
        return Math.min(backoffDelay, maxDelay);
    }

    /**
     * Handle retry event triggered by timer.
     */
    private void handleRetryEvent(RetryEvent event) {
        String deferredTaskId = event.getDeferredTaskId();
        int retryCount = event.getRetryCount();

        DeferredTask deferredTask = deferredTasks.get(deferredTaskId);
        if (deferredTask == null) {
            return;  // Task already resolved
        }

        Task task = deferredTask.getTask();

        // Re-run validation pipeline
        ValidationResult result = validateTaskGeneration(task);

        if (result.isValid()) {
            // Success! Generate task and remove from deferred list
            taskQueue.enqueue(task);
            deferredTasks.remove(deferredTaskId);

            log.info("Retry #{} succeeded for task {}", retryCount, task.getId());
        } else {
            // Still failed
            if (retryCount >= deferredTask.getMaxRetries()) {
                // Max retries exceeded, give up
                deferredTasks.remove(deferredTaskId);
                moveToDeadLetterQueue(task, result.getFailureReason());

                log.warn("Task {} failed after {} retries, moved to DLQ: {}",
                    task.getId(), retryCount, result.getFailureReason());
            } else {
                // Schedule next retry
                scheduleRetry(deferredTaskId, retryCount);
            }
        }
    }

    /**
     * Event listeners for state changes that can unblock deferred tasks.
     *
     * These listeners are registered when tasks are deferred,
     * and they trigger immediate re-validation (no waiting for timer).
     */
    @EventListener
    public void onWIPThresholdCleared(WIPThresholdClearedEvent event) {
        // Find all deferred tasks waiting for this step's WIP to clear
        deferredTasks.values().stream()
            .filter(dt -> dt.getReason().equals("WIP_LIMIT_EXCEEDED"))
            .filter(dt -> dt.getTask().getDestinationStepId().equals(event.getStepId()))
            .forEach(dt -> {
                // Immediate re-validation (skip timer)
                handleRetryEvent(new RetryEvent(dt.getId(), dt.getRetryCount(), 0));
            });
    }

    @EventListener
    public void onDownstreamAvailable(DownstreamAvailableEvent event) {
        // Find all deferred tasks waiting for this downstream
        deferredTasks.values().stream()
            .filter(dt -> dt.getReason().equals("DOWNSTREAM_UNAVAILABLE"))
            .filter(dt -> dt.getTask().getDestinationEntityId().equals(event.getEntityId()))
            .forEach(dt -> {
                // Immediate re-validation (skip timer)
                handleRetryEvent(new RetryEvent(dt.getId(), dt.getRetryCount(), 0));
            });
    }
}
```

#### 5.3.3 下游不可用策略的任务处理

**决策：生成新任务 vs 更新任务**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│           下游不可用策略的任务处理方式（最终决策）                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【核心原则】                                                                 │
│  ✅ 任务生成后不再修改（Task 是不可变对象）                                  │
│  ✅ 策略决定是生成新任务还是不生成                                          │
│  ✅ 任务状态由 TaskQueue 统一管理                                           │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              策略与任务生成关系对照表                                 │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  策略                   │ 是否生成新任务 │ 任务类型                │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │  BUFFER_TO_STORAGE     │ 是           │ TRANSPORT (→暂存区)      │   │
│  │  BLOCK_AND_WAIT         │ 否           │ N/A (物料保持 WAITING)   │   │
│  │  DEFER_RETRY            │ 否           │ N/A (进入重试队列)       │   │
│  │  REROUTE_ALTERNATE      │ 是           │ TRANSPORT (→替代路径)     │   │
│  │  RETURN_UPSTREAM        │ 是           │ TRANSPORT (→上游)         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  【详细说明】                                                                 │
│                                                                             │
│  1. BUFFER_TO_STORAGE                                                      │
│     - 生成新任务：Task(type=TRANSPORT, source=当前, destination=暂存区)       │
│     - 任务标记为 temporary=true（表示这是临时任务）                           │
│     - 注册监听器：下游可用时，生成从暂存区到下游的任务                        │
│                                                                             │
│  2. BLOCK_AND_WAIT                                                         │
│     - 不生成任务                                                           │
│     - 物料状态设置为 WAITING_FOR_DOWNSTREAM                                  │
│     - 注册监听器：下游可用时，重新触发任务生成                                │
│     - 记录等待开始时间（用于 KPI 等待时间统计）                              │
│                                                                             │
│  3. DEFER_RETRY                                                            │
│     - 不生成任务                                                           │
│     - 创建 DeferredTask 记录（包含原始任务信息）                              │
│     - 指数退避调度重试事件                                                  │
│     - 最大重试次数后进入 Dead Letter Queue                                   │
│                                                                             │
│  4. REROUTE_ALTERNATE                                                      │
│     - 查找替代路径/设备                                                    │
│     - 若找到替代：生成新任务（Task with 替代目的地）                          │
│     - 若无替代：回退到 BLOCK_AND_WAIT                                       │
│                                                                             │
│  5. RETURN_UPSTREAM                                                       │
│     - 生成新任务：Task(type=TRANSPORT, source=当前, destination=上游)        │
│     - 上游必须支持接收（需校验上游可用性）                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**代码实现**：

```java
/**
 * Downstream unavailable handler with strategy-specific task handling.
 */
public class DownstreamUnavailableHandler {

    /**
     * Handle downstream unavailable scenario.
     *
     * Key decision: Whether to generate a new task or defer.
     * This is determined by the strategy, not by updating existing task.
     *
     * @param currentStep Current process step
     * @param downstreamStep Downstream step (unavailable)
     * @param material The material
     * @param strategy The configured strategy
     */
    public TaskGenerationResult handleDownstreamUnavailable(
            ProcessStep currentStep,
            ProcessStep downstreamStep,
            Material material,
            DownstreamUnavailableStrategy strategy) {

        switch (strategy.getType()) {
            case BUFFER_TO_STORAGE:
                return handleBufferToStorage(currentStep, material);

            case BLOCK_AND_WAIT:
                return handleBlockAndWait(currentStep, downstreamStep, material);

            case DEFER_RETRY:
                return handleDeferRetry(currentStep, downstreamStep, material, strategy);

            case REROUTE_ALTERNATE:
                return handleRerouteAlternate(currentStep, downstreamStep, material);

            case RETURN_UPSTREAM:
                return handleReturnUpstream(currentStep, material);

            default:
                return TaskGenerationResult.failed("Unknown strategy: " + strategy.getType());
        }
    }

    /**
     * Strategy: BUFFER_TO_STORAGE
     *
     * Action: Generate NEW task to buffer zone.
     * Task type: TRANSPORT (temporary)
     */
    private TaskGenerationResult handleBufferToStorage(ProcessStep currentStep, Material material) {
        // Find available buffer zone
        BufferZone buffer = findAvailableBufferZone();
        if (buffer == null) {
            // Fallback to BLOCK_AND_WAIT
            return handleBlockAndWait(currentStep, null, material);
        }

        // Generate NEW transport task to buffer
        Task bufferTask = Task.builder()
            .id(generateTaskId())
            .type(TaskType.TRANSPORT)
            .sourceId(material.getCurrentLocation())
            .destinationId(buffer.getId())
            .materialId(material.getId())
            .priority(Priority.LOW)
            .temporary(true)  // Mark as temporary
            .originalDestinationId(getOriginalDestination(currentStep))  // Remember original goal
            .build();

        // Register listener: when downstream available, generate task from buffer
        registerDownstreamAvailableListener(currentStep, buffer.getId(), material);

        // Task IS generated (return success)
        return TaskGenerationResult.success(bufferTask);
    }

    /**
     * Strategy: BLOCK_AND_WAIT
     *
     * Action: NO task generated. Material waits.
     */
    private TaskGenerationResult handleBlockAndWait(
            ProcessStep currentStep,
            ProcessStep downstreamStep,
            Material material) {

        // Update material state
        material.setState(MaterialState.WAITING_FOR_DOWNSTREAM);
        material.setWaitingForStepId(downstreamStep.getId());
        material.setWaitingSince(simulation.currentTime());

        // Register listener: when downstream available, retry task generation
        registerDownstreamAvailableListener(currentStep, material.getCurrentLocation(), material);

        // NO task generated (return deferred)
        return TaskGenerationResult.deferred("Material waiting for downstream: " + downstreamStep.getId());
    }

    /**
     * Strategy: DEFER_RETRY
     *
     * Action: NO task generated. Enter retry queue.
     */
    private TaskGenerationResult handleDeferRetry(
            ProcessStep currentStep,
            ProcessStep downstreamStep,
            Material material,
            DownstreamUnavailableStrategy strategy) {

        // Create pending task (not enqueued yet)
        Task pendingTask = Task.builder()
            .id(generateTaskId())
            .type(TaskType.TRANSPORT)
            .sourceId(material.getCurrentLocation())
            .destinationId(downstreamStep.getTargetEntityId())
            .materialId(material.getId())
            .priority(calculatePriority(material, strategy))
            .build();

        // Register for deferred retry (event-driven, not polling)
        long initialDelay = strategy.getInitialDelaySeconds();
        deferredTaskRetryManager.registerDeferredTask(
            pendingTask,
            "DOWNSTREAM_UNAVAILABLE",
            initialDelay
        );

        // NO task generated (return deferred)
        return TaskGenerationResult.deferred("Task deferred for retry: " + pendingTask.getId());
    }

    /**
     * Strategy: REROUTE_ALTERNATE
     *
     * Action: Try alternate route. Generate task if found, else fallback.
     */
    private TaskGenerationResult handleRerouteAlternate(
            ProcessStep currentStep,
            ProcessStep downstreamStep,
            Material material) {

        // Try to find alternate path
        List<AlternateRoute> alternateRoutes = findAlternateRoutes(currentStep, material);

        if (alternateRoutes.isEmpty()) {
            // No alternate available, fallback to BLOCK_AND_WAIT
            log.debug("No alternate route available, falling back to BLOCK_AND_WAIT");
            return handleBlockAndWait(currentStep, downstreamStep, material);
        }

        // Select best alternate route (by priority/cost)
        AlternateRoute selected = selectBestAlternateRoute(alternateRoutes);

        // Generate NEW task with alternate destination
        Task alternateTask = Task.builder()
            .id(generateTaskId())
            .type(TaskType.TRANSPORT)
            .sourceId(material.getCurrentLocation())
            .destinationId(selected.getTargetEntityId())
            .materialId(material.getId())
            .priority(Priority.NORMAL)
            .alternateRoute(true)  // Mark as alternate route
            .originalDestinationId(downstreamStep.getTargetEntityId())
            .build();

        // Task IS generated (return success)
        return TaskGenerationResult.success(alternateTask);
    }

    /**
     * Strategy: RETURN_UPSTREAM
     *
     * Action: Generate NEW task to return to upstream.
     */
    private TaskGenerationResult handleReturnUpstream(ProcessStep currentStep, Material material) {
        // Find upstream entity
        String upstreamId = findUpstreamEntity(currentStep);
        if (upstreamId == null) {
            return TaskGenerationResult.failed("No upstream entity available for return");
        }

        // Validate upstream availability
        Entity upstream = entityManager.get(upstreamId);
        if (!upstream.isAvailable()) {
            return TaskGenerationResult.failed("Upstream unavailable: " + upstreamId);
        }

        // Generate NEW transport task to upstream
        Task returnTask = Task.builder()
            .id(generateTaskId())
            .type(TaskType.TRANSPORT)
            .sourceId(material.getCurrentLocation())
            .destinationId(upstreamId)
            .materialId(material.getId())
            .priority(Priority.LOW)
            .returnTask(true)  // Mark as return task
            .build();

        // Task IS generated (return success)
        return TaskGenerationResult.success(returnTask);
    }
}
```

---

## 六、需补充的 Spec 章节

建议在 `openspec/changes/add-logistics-simulation-module/specs/` 下新增：

### 6.1 新增 Spec 文件：`workflow-automation/spec.md`

```
workflow-automation/
└── spec.md
    ├── REQ-WF-001: ProcessFlow 流程定义
    ├── REQ-WF-002: ProcessStep 工序配置
    ├── REQ-WF-003: OutputStrategy 产出策略
    ├── REQ-WF-004: TransportSelector 搬运主体选择
    ├── REQ-WF-005: EventTrigger 事件触发器
    ├── REQ-WF-006: TaskGenerator 自动任务生成
    ├── REQ-WF-007: WorkflowExecutor 流程执行器
    ├── REQ-WF-008: 上下游约束检查
    ├── REQ-WF-009: 下游拉动机制
    ├── REQ-WF-010: 人车混合交管策略
    ├── REQ-WF-011: ProcessFlow 与场景绑定
    ├── REQ-WF-012: Human 搬运模型
    └── REQ-WF-013: 任务重试机制
```

### 6.2 需修订的现有 Spec

| 文件 | 需修订内容 |
|------|-----------|
| `traffic-control/spec.md` | 新增 `REQ-TC-013: SafetyZone 安全区域`、`REQ-TC-014: Human 交通集成` |
| `dispatch-system/spec.md` | 新增 `REQ-DS-009: Workflow 任务队列集成` |
| `logistics-entities/spec.md` | 新增 `REQ-ENT-010: Machine 工序实体`、`REQ-ENT-011: Human 疲劳模型` |

---

## 七、完整流程示意

### 7.1 端到端流程：晶圆加工全流程

```
═══════════════════════════════════════════════════════════════════════════
                         晶圆加工自动化流程仿真
═══════════════════════════════════════════════════════════════════════════

【阶段 1：投料】
  │
  ├─ Stocker-01 出库请求
  │   └─ EventTrigger: INVENTORY_LOW (触发条件: FOUP < 10)
  │       └─ TaskGenerator: 生成出库任务
  │           └─ TaskQueue.enqueue(出库任务)
  │               └─ DispatchEngine: 分配 OHT-01
  │                   └─ OHT-01 从 Stocker-01 取货
  │
  ▼
【阶段 2：第一道工序 - 光刻】
  │
  ├─ 运输到 Machine-A (Lithography)
  │   └─ TransportSelector: 选择 OHT-01 (成本优先)
  │       └─ TrafficManager: 申请轨道通行
  │           └─ OHT-01 到达 Machine-A 装卸口
  │
  ├─ Machine-A 加工
  │   └─ ProcessStep-1 配置
  │       ├─ 处理时间: N(50, 5) 秒
  │       ├─ WIP 限制: 3
  │       └─ 批量规则: SINGLE
  │
  ▼
【阶段 3：事件触发 - 校验与任务生成】
  │
  ├─ Machine-A 完成事件
  │   └─ EventTrigger: MACHINE_COMPLETED
  │       │
  │       ▼
  │   ┌─────────────────────────────────────────────────────────────┐
  │   │ 校验 1: 依赖检查                                             │
  │   │ canStartStep(nextStep, material)?                           │
  │   │   ├─ NO → 注册监听器，等待前置完成，RETURN                    │
  │   │   └─ YES → 继续                                              │
  │   └─────────────────────────────────────────────────────────────┘
  │       │
  │       ▼
  │   ┌─────────────────────────────────────────────────────────────┐
  │   │ 校验 2: WIP 检查                                             │
  │   │ currentWIP < wipLimit?                                      │
  │   │   ├─ NO → DEFER_RETRY (注册 WIP 阈值监听器), RETURN          │
  │   │   └─ YES → 继续                                             │
  │   └─────────────────────────────────────────────────────────────┘
  │       │
  │       ▼
  │   ┌─────────────────────────────────────────────────────────────┐
  │   │ 校验 3: 下游可用性                                         │
  │   │ downstream.isAvailable()?                                  │
  │   │   ├─ NO → 应用下游不可用策略                                │
  │   │   │       ├─ BUFFER_TO_STORAGE → 生成暂存任务               │
  │   │   │       ├─ BLOCK_AND_WAIT → 物料 WAITING, 注册监听器    │
  │   │   │       ├─ DEFER_RETRY → 进入重试队列                    │
  │   │   │       └─ REROUTE_ALTERNATE → 尝试替代路径             │
  │   │   └─ YES → 继续                                            │
  │   └─────────────────────────────────────────────────────────────┘
  │       │
  │       ▼
  │   ┌─────────────────────────────────────────────────────────────┐
  │   │ 校验 4: 资源可用性                                         │
  │   │ hasAvailableTransporters()?                               │
  │   │   ├─ NO → DEFER_RETRY, RETURN                              │
  │   │   └─ YES → 继续                                            │
  │   └─────────────────────────────────────────────────────────────┘
  │       │
  │       ▼
  │   ┌─────────────────────────────────────────────────────────────┐
  │   │ 校验 5: 路径可行性                                         │
  │   │ isPathFeasible()?                                         │
  │   │   ├─ NO → DEFER_RETRY, RETURN                              │
  │   │   └─ YES → 继续                                            │
  │   └─────────────────────────────────────────────────────────────┘
  │       │
  │       ▼ (全部校验通过)
  │   生成任务: Machine-A → Machine-B
  │   └─ TaskQueue.enqueue(task)
  │
  ▼
【阶段 4：第二道工序 - 刻蚀】
  │
  ├─ 运输到 Machine-B (Etching)
  │   └─ TransportSelector: 选择 AGV-03 (混合策略)
  │
  ├─ Machine-B 加工
  │   └─ ProcessStep-2 配置
  │       ├─ 处理时间: E(30) 秒 (指数分布)
  │       ├─ WIP 限制: 2
  │       └─ 批量规则: FIXED_SIZE(5)
  │
  ▼
【阶段 5：最终产出】
  │
  ├─ OutputStrategy: INTERMEDIATE_STORAGE
  │   └─ 生成入库任务: ManualStation-01 → Stocker-02
  │
  ▼
【阶段 6：闭环反馈】
  │
  ├─ KPI 更新
  │   ├─ ThroughputCalculator: 记录完成数量
  │   ├─ WIPTracker: 更新各工序 WIP
  │   └─ BottleneckAnalyzer: 识别瓶颈工序
  │
  └─ 事件循环
      └─ 下游拉动触发 (如果 Machine-D 空闲)
          └─ 返回【阶段 1】

═══════════════════════════════════════════════════════════════════════════
```

---

## 八、调研引用来源

### 竞品文档

1. [FlexSim Software Primer 5th Edition (2025)](https://www.flexsim.com/wp-content/uploads/2025/03/FlexSim-Software-Primer_5th-ed.pdf)
2. [Types of Process Flows - FlexSim Docs](https://docs.flexsim.com/en/19.1/ModelLogic/ProcessFlowBasics/TypesOfProcessFlows/TypesOfProcessFlows.html)
3. [FlexSim 2025: Container Object, Task Sequence Queue](https://www.flexsim.com/news/flexsim-2025-container-object-task-sequence-queue/)
4. [Using Agents in a Process Flow - AnyLogic](https://www.anylogic.com/resources/educational-videos/using-agents-in-a-process-flow/)
5. [Driving Production Flow with Plant Simulation](https://blogs.sw.siemens.com/tecnomatix/driving-production-flow-and-virtual-commissioning-with-plant-simulation-process-simulate/)
6. [Plant Simulation Source and Drain Statistics](https://community.sw.siemens.com/s/article/Plant-Simulation-04---Source-and-Drain-statistics)

### 学术研究

7. [A Novel Method for Simulation Model Generation of Production Systems - MDPI (2025)](https://www.mdpi.com/2224-2708/14/3/55)
8. [Bridging the Gap Between DES and Stochastic Systems - ScienceDirect (2025)](https://www.sciencedirect.com/science/article/pii/S2213846325001798)
9. [Automated Generation of DES - Lugaresi PhD Thesis (2021)](https://www.politesi.polimi.it/bitstream/10589/180001/1/LUGARESI-PHD-PDF)
10. [On the reproducibility of DES studies (2025)](https://www.tandfonline.com/doi/full/10.1080/17477778.2025.2552177)
11. [Reproducibility in DES - Software Sustainability Institute](https://www.software.ac.uk/blog/reproducibility-discrete-event-simulation)

### 人车混合交通

12. [Design of AGV systems in shared environments](https://www.sciencedirect.com/science/article/pii/S2405896320335795)
13. [Dynamic Task Allocation with Human and AGV Teams](https://dl.acm.org/doi/full/10.1145/3716862)
14. [Interaction between Human and AGV Systems](https://pdfs.semanticscholar.org/a161/51201fe7c293829cb35872e9d0da02be53b5.pdf)

### Human 搬运与疲劳模型

15. [Modelling physical fatigue and recovery patterns within... (2025)](https://www.tandfonline.com/doi/full/10.1080/00207543.2025.2580539)
16. [A simulation-based approach for time allowances](https://www.sciencedirect.com/science/article/pii/S0360835219300312)
17. [ISO 11228-1:2021 Ergonomics — Manual handling](https://www.iso.org/obp/ui/#iso:std:iso:11228:-1:ed-2:v1:en)
18. [A Discrete-event modeling method to study human behavior (2025)](https://www.sciencedirect.com/science/article/pii/S0360835224008544)

### 重试机制与事件驱动

19. [Understanding push vs poll in event-driven architectures (2025)](https://theburningmonk.com/2025/05/understanding-push-vs-poll-in-event-driven-architectures/)
20. [Queue-Based Exponential Backoff Retry Pattern](https://dev.to/andreparis/queue-based-exponential-backoff-a-resilient-retry-pattern-for-distributed-systems-37f3)
21. [A Guide to Retry Pattern in Distributed Systems](https://blog.bytebytego.com/p/a-guide-to-retry-pattern-in-distributed)
22. [Event-Driven vs Polling - Stack Overflow](https://stackoverflow.com/questions/2115052/event-driven-versus-polling-scheduling)

### 架构参考

23. [Batsim Protocol Documentation](https://github.com/oar-team/batsim/blob/master/docs/protocol.rst)
24. [Workflow-Aware Task Scheduling](https://eprints.gla.ac.uk/350601/2/350601.pdf)

---

## 附录

### A. System Config 配置项清单（v1.3 新增）

> **说明**：本节列出所有与 workflow automation 相关的 system_config 配置项，确保配置项统一管理和可追溯。

| 配置键 | 类型 | 默认值 | 说明 | 模块 |
|--------|------|--------|------|------|
| **重试机制配置** |
| `retry.maxRetries` | int | 5 | 最大重试次数 | DEFER_RETRY |
| `retry.baseDelay` | long | 5 | 基础延迟秒数 | DEFER_RETRY |
| `retry.maxDelay` | long | 60 | 最大延迟秒数 | DEFER_RETRY |
| **资源验证配置** |
| `resource.validation.strategy` | string | BLOCK_AND_RETRY | 资源不可用时的策略 (BLOCK_AND_RETRY \| ENQUEUE_AND_WAIT) | TaskGenerator |
| `resource.validation.timeout` | long | 300 | 资源等待超时秒数 | TaskGenerator |
| **WIP 限制配置** |
| `wip.check.enabled` | boolean | true | 是否启用 WIP 检查 | TaskGenerator |
| `wip.default.limit` | int | 10 | 默认 WIP 上限 | ProcessStep |
| **下游验证配置** |
| `downstream.validation.enabled` | boolean | true | 是否启用下游可用性检查 | TaskGenerator |
| `downstream.unavailable.strategy` | string | DEFER_RETRY | 下游不可用策略 | OutputStrategy |
| **仿真时间配置** |
| `simulation.global.seed` | long | 12345 | 全局随机种子（用于可复现） | SimulationEnv |
| `simulation.time.scale` | double | 1.0 | 时间缩放因子 | SimulationEnv |
| **Human 配置** |
| `human.base.walking.speed` | double | 1.0 | 基础步行速度 (m/s) | Human |
| `human.fatigue.enabled` | boolean | true | 是否启用疲劳模型 | Human |
| `human.fatigue.max.factor` | double | 1.35 | 最大疲劳因子 | Human |
| `human.load.factor.enabled` | boolean | true | 是否启用负载因子 | Human |
| **SafetyZone 配置** |
| `safety.zone.default.priority` | string | HUMAN_FIRST | 默认访问优先级 | SafetyZone |
| `safety.zone.capacity.check` | boolean | true | 是否检查容量限制 | SafetyZone |
| **多租户配置** |
| `tenant.isolation.enabled` | boolean | true | 是否启用多租户隔离 | 全局 |
| `tenant.default.id` | string | "default" | 默认租户 ID | 全局 |
| **调试配置** |
| `debug.workflow.logging` | boolean | false | 是否启用详细日志 | 全局 |
| `debug.task.tracing` | boolean | false | 是否启用任务跟踪 | TaskQueue |

**配置示例（system_config.properties）**：

```properties
# 重试机制配置
retry.maxRetries=5
retry.baseDelay=5
retry.maxDelay=60

# 资源验证配置
resource.validation.strategy=BLOCK_AND_RETRY
resource.validation.timeout=300

# WIP 限制配置
wip.check.enabled=true
wip.default.limit=10

# 仿真时间配置
simulation.global.seed=12345
simulation.time.scale=1.0

# Human 配置
human.base.walking.speed=1.0
human.fatigue.enabled=true
human.fatigue.max.factor=1.35

# 调试配置
debug.workflow.logging=false
debug.task.tracing=false
```

**配置读取方式**：

```java
/**
 * Configuration reader for workflow automation settings.
 */
public class WorkflowConfig {

    private final SystemConfig systemConfig;

    public WorkflowConfig(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    // Retry config
    public int getRetryMaxRetries() {
        return systemConfig.getInt("retry.maxRetries", 5);
    }

    public long getRetryBaseDelay() {
        return systemConfig.getLong("retry.baseDelay", 5L);
    }

    public long getRetryMaxDelay() {
        return systemConfig.getLong("retry.maxDelay", 60L);
    }

    // Resource validation strategy
    public ResourceValidationStrategy getResourceValidationStrategy() {
        String strategy = systemConfig.getString("resource.validation.strategy", "BLOCK_AND_RETRY");
        return ResourceValidationStrategy.valueOf(strategy);
    }

    // Human config
    public double getHumanBaseWalkingSpeed() {
        return systemConfig.getDouble("human.base.walking.speed", 1.0);
    }

    public boolean isHumanFatigueEnabled() {
        return systemConfig.getBoolean("human.fatigue.enabled", true);
    }
}
```

### B. 实施优先级建议

**Phase 1（核心）**：
- ProcessFlow/ProcessStep 数据模型
- ProcessFlow 与场景绑定机制
- EventTrigger + TaskGenerator 基础实现
- 完整的校验流程（5 项校验）
- 简单的 DIRECT_TO_NEXT 策略

**Phase 2（增强）**：
- OutputStrategy 多策略支持
- DEFER_RETRY 事件驱动重试
- TransportSelector 混合搬运
- Human 搬运简化模型
- 下游拉动机制

**Phase 3（完善）**：
- SafetyZone 交通管理
- 人车混合交管冲突仲裁
- 复杂路由规则（CONDITIONAL, REROUTE）
- KPI 集成
- Dead Letter Queue 处理

### B. 关键设计决策总结

| 决策点 | 最终方案 | 理由 |
|--------|----------|------|
| **ProcessFlow 存储** | 独立表 + 场景引用 | 支持版本管理、流程复用、多流程绑定 |
| **场景流程绑定** | 允许多个流程 | 不同入口点可触发不同流程 |
| **Human 交通管理** | 进入 TrafficManager，使用 SafetyZone | 统一入口，安全优先，简化模型 |
| **Human 路径规划** | 无（直线移动） | 符合实际，简化实现 |
| **Human 移动时间** | 直线距离 × 疲劳因子 × 负载因子 | 基于 ISO 标准和疲劳研究 |
| **任务校验顺序** | 先校验，后生成（5 项校验） | 保证任务可执行性 |
| **DEFER_RETRY 机制** | 事件驱动（非轮询） | 符合 DES 理念，低延迟，高效率 |
| **下游不可用策略** | 根据策略决定是否生成新任务 | 任务不可变，策略驱动行为 |
