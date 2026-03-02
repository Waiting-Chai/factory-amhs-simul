# Workflow Automation Spec

## Metadata

| 属性 | 值 |
|------|-----|
| **Spec ID** | `workflow-automation` |
| **Change ID** | `add-logistics-simulation-module` |
| **Version** | 1.0 |
| **Status** | 设计提案阶段 |
| **Author** | shentw |
| **Created** | 2026-02-06 |
| **Related Specs** | `logistics-entities/spec.md`, `traffic-control/spec.md`, `dispatch-system/spec.md` |

---

## ADDED Requirements

### Requirement: REQ-WF-001 ProcessFlow 流程定义

系统MUST实现REQ-WF-001（ProcessFlow 流程定义）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P0 (核心功能)

**描述**: 系统必须支持 ProcessFlow（工艺流程）定义，用于描述生产物料的完整加工流程。

#### 数据模型

```java
/**
 * Process flow definition for production workflow.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class ProcessFlow {
    private String id;
    private String name;
    private String version;                     // 语义化版本 (1.0.0)
    private List<ProcessStep> steps;            // 工序列表（顺序）
    private OutputStrategy defaultOutputStrategy; // 默认产出策略
    private TransportSelector defaultTransportSelector; // 默认搬运选择器
    private boolean enabled;
    private Map<String, Object> properties;     // 扩展属性
    private String parentVersionId;             // 父版本（用于追溯）
}
```

#### Given/When/Then

**Scenario 1: 创建简单线性流程**

```gherkin
Given 用户已登录系统
And 场景中已有机台 "Machine-A" 和 "Machine-B"
When 用户创建 ProcessFlow，名称为 "晶圆加工流程"
And 添加工序 Step1，目标机台为 "Machine-A"
And 添加工序 Step2，目标机台为 "Machine-B"
Then 流程创建成功，包含 2 个工序
And 工序顺序为 Step1 → Step2
And 流程默认启用状态为 enabled = true
```

**Scenario 2: 流程版本管理**

```gherkin
Given 已存在流程版本 1.0.0
When 用户修改流程定义并保存
Then 系统自动创建新版本 1.1.0
And parentVersionId 指向 1.0.0 版本
And 历史版本保持不变
```

---

### Requirement: REQ-WF-002 ProcessStep 工序配置（多候选设备支持）

系统MUST实现REQ-WF-002（ProcessStep 工序配置（多候选设备支持））能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P0 (核心功能)

**描述**: 每个 ProcessStep 定义单个工序的加工参数、时间分布、WIP 限制等。支持多候选设备配置和运输类型校验。

#### 数据模型（v2.0）

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

    // 工序配置（多候选设备）
    private List<String> targetEntityIds;       // 目标机台/设备 ID 列表（多候选）
    private List<String> requiredTransportTypes; // 必需的运输类型（如 OHT, AGV, HUMAN）
    private TimeDistribution processingTime;    // 处理时间分布
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

#### 多候选设备选择算法（v2.0）

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
    private double distanceWeight;  // Default: 0.4
    private double timeWeight;      // Default: 0.4
    private double wipWeight;       // Default: 0.2
}
```

#### 建模阶段强校验（v2.0）

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

#### Given/When/Then

**Scenario 1: 配置正态分布处理时间**

```gherkin
Given 工序 "光刻" 目标机台列表为 ["Lithography-A", "Lithography-B"]
When 用户设置处理时间为正态分布，均值 50s，标准差 5s
Then 处理时间分布类型为 NORMAL
And params.mean = 50.0
And params.std = 5.0
```

**Scenario 2: 设置 WIP 限制**

```gherkin
Given 工序 "蚀刻" 目标机台列表为 ["Etch-B", "Etch-C"]
When 用户设置 WIP 限制为 3
Then 该工序最多允许 3 个物料同时加工
And 第 4 个物料到达时需等待
```

**Scenario 2.1: 运输类型强校验 - 成功**

```gherkin
Given 工序 "清洗" 配置 requiredTransportTypes = ["OHT", "AGV"]
And 候选设备 Machine-A 的 supportedTransportTypes = ["OHT", "HUMAN"]
And 候选设备 Machine-B 的 supportedTransportTypes = ["AGV", "CONVEYOR"]
When 用户保存流程定义
Then 保存成功
And 运输类型交集为 ["OHT", "AGV"]（非空）
```

**Scenario 2.2: 运输类型强校验 - 失败**

```gherkin
Given 工序 "清洗" 配置 requiredTransportTypes = ["OHT"]
And 候选设备 Machine-A 的 supportedTransportTypes = ["AGV", "HUMAN"]
And 候选设备 Machine-B 的 supportedTransportTypes = ["CONVEYOR"]
When 用户尝试保存流程定义
Then 保存失败
And 返回错误："Transport type mismatch: step requires [OHT] but devices support [AGV, HUMAN, CONVEYOR]"
```

**Scenario 2.3: 多候选设备选择（多因子加权）**

```gherkin
Given 工序 "光刻" 候选设备列表为 ["Machine-A", "Machine-B", "Machine-C"]
And 系统配置权重：distanceWeight=0.4, timeWeight=0.4, wipWeight=0.2
And 设备状态：
  - Machine-A: distance=10m, time=20s, wip=2
  - Machine-B: distance=5m, time=15s, wip=5
  - Machine-C: distance=15m, time=30s, wip=0
When 任务分配
Then 计算归一化分数：
  - Machine-A: normDist=0.33, normTime=0.29, normWip=0.40, score=0.56
  - Machine-B: normDist=0.00, normTime=0.00, normWip=1.00, score=0.60
  - Machine-C: normDist=1.00, normTime=1.00, normWip=0.00, score=0.20
And 选择 Machine-B（分数最高）
```

**Scenario 3: 上下游依赖约束**

```gherkin
Given 工序流程：清洗 → 光刻 → 蚀刻
When "光刻" 工序配置 predecessorIds = ["清洗"]
And "蚀刻" 工序配置 predecessorIds = ["光刻"]
Then 系统确保工序按依赖顺序执行
And "光刻" 不会在 "清洗" 完成前开始
And "蚀刻" 不会在 "光刻" 完成前开始
```

---

### Requirement: REQ-WF-003 OutputStrategy 产出策略

系统MUST实现REQ-WF-003（OutputStrategy 产出策略）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P0 (核心功能)

**描述**: 支持多种产出策略，控制工序完成后的物料处理方式。

#### 策略类型（v1.5）

| 策略 | 说明 | 使用场景 |
|------|------|----------|
| `DIRECT_TO_NEXT` | 直达下一台 | 线性流程，直接进入下一工序 |
| `INTERMEDIATE_STORAGE` | 中间存储 | 物料进入中间缓冲区 |
| `BUFFER_ZONE` | 暂存区 | 物料进入指定暂存区等待 |
| `CONDITIONAL` | 条件分支 | 基于条件动态选择下游 |
| `DYNAMIC_SELECT` | 动态选择 | 从候选列表中动态选择目标 |

#### 策略详细配置

```java
/**
 * Output strategy for processed material.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class OutputStrategy {
    private StrategyType type;

    // DIRECT_TO_NEXT: 单目标
    private String singleDestination;

    // INTERMEDIATE_STORAGE / BUFFER_ZONE: 存储位置
    private String storageLocation;

    // DYNAMIC_SELECT: 候选列表
    private List<String> candidateList;
    private DynamicSelector dynamicSelector;

    // CONDITIONAL: 条件分支
    private List<ConditionalRoute> conditionalRoutes;

    // 通用：路由策略
    private RoutingPolicy routingPolicy;

    // 下游不可用策略
    private DownstreamStrategy downstreamStrategy;
}

public enum StrategyType {
    DIRECT_TO_NEXT,       // 直达下一台
    INTERMEDIATE_STORAGE, // 中间存储
    BUFFER_ZONE,          // 暂存区
    CONDITIONAL,          // 条件分支
    DYNAMIC_SELECT        // 动态选择（从候选列表）
}
```

#### Given/When/Then

**Scenario 1: 直达下一工序**

```gherkin
Given 工序流程：Step1 → Step2 → Step3
And Step1 产出策略为 DIRECT_TO_NEXT
When Step1 加工完成
Then 物料自动进入 Step2
And 不需要人工干预
```

**Scenario 2: 条件分支**

```gherkin
Given 工序 "质检" 完成后
And 产出策略为 CONDITIONAL
And 条件路由：grade=A → 包装，grade=B → 返工
When 物料 grade = "A"
Then 物料进入 "包装" 工序
When 物料 grade = "B"
Then 物料进入 "返工" 工序
```

**Scenario 3: 动态选择**

```gherkin
Given 工序 "分配" 产出策略为 DYNAMIC_SELECT
And 候选列表：[Machine-A, Machine-B, Machine-C]
And 动态选择器为 WIP_FIRST
When 工序完成
Then 系统选择当前可用的机台
If 只有 Machine-B 可用
Then 物料进入 Machine-B
```

---

### Requirement: REQ-WF-004 TransportSelector 搬运主体选择

系统MUST实现REQ-WF-004（TransportSelector 搬运主体选择）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P1 (重要功能)

**描述**: 支持多种搬运主体选择策略，决定由哪个车辆/设备执行搬运任务。

#### 选择策略类型（v1.5）

| 策略 | 说明 | 选择逻辑 |
|------|------|----------|
| `DISTANCE_FIRST` | 距离优先 | 选择距离最近的运输实体 |
| `TIME_FIRST` | 时效优先 | 选择完成时间最快的运输实体 |
| `WIP_FIRST` | 负载优先 | 优先选择 WIP 最少的运输实体 |
| `PRIORITY_BASED` | 优先级 | 根据任务优先级选择运输实体 |
| `HYBRID` | 混合策略 | 使用加权公式综合评分 |

#### 策略详细配置

```java
/**
 * Transport selector for choosing transport entity.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class TransportSelector {
    private SelectionPolicy policy;

    // 允许的实体类型
    private List<EntityType> allowedTypes;  // OHT, AGV, HUMAN

    // HYBRID 策略权重
    private double distanceWeight;  // 距离权重
    private double timeWeight;      // 时间权重
    private double wipWeight;       // WIP权重

    // PRIORITY_BASED 策略
    private PriorityMapping priorityMapping;
}

public enum SelectionPolicy {
    DISTANCE_FIRST,       // 距离优先
    TIME_FIRST,           // 时效优先
    WIP_FIRST,            // 负载优先
    PRIORITY_BASED,       // 优先级
    HYBRID                // 混合策略
}
```

#### HYBRID 策略评分公式

```
Score(x) = w_distance × (1 - normDistance(x))
         + w_time × (1 - normTime(x))
         + w_wip × (1 - normWip(x))
```

where:
- `normDistance`, `normTime`, `normWip` 使用 min-max 归一化到 [0,1]
- `w_distance + w_time + w_wip = 1.0`

#### Given/When/Then

**Scenario 1: 距离优先选择**

```gherkin
Given 车辆 AGV-01 距离 100m，预计耗时 50s，WIP=3
And 车辆 AGV-02 距离 50m，预计耗时 30s，WIP=1
And 搬运选择器为 DISTANCE_FIRST
When 系统分配搬运任务
Then 选择 AGV-02（距离最近）
```

**Scenario 2: 时效优先选择**

```gherkin
Given 车辆 AGV-01 成本 10，预计耗时 50s
And 车辆 AGV-02 成本 20，预计耗时 30s
And 搬运选择器为 TIME_FIRST
When 系统分配搬运任务
Then 选择 AGV-02（耗时最短）
```

**Scenario 3: 工序级覆盖流程级策略**

```gherkin
Given 流程默认搬运策略为 DISTANCE_FIRST
And 工序 "紧急运输" 配置搬运策略为 TIME_FIRST
When 执行 "紧急运输" 工序
Then 使用 TIME_FIRST 策略（覆盖默认）
```

---

### Requirement: REQ-WF-005 EventTrigger 事件触发器

系统MUST实现REQ-WF-005（EventTrigger 事件触发器）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P0 (核心功能)

**描述**: 系统必须支持事件驱动的任务生成，基于 TriggerCondition 条件匹配。

#### TriggerCondition 完整结构（v1.5）

```java
/**
 * Trigger condition for matching events.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class TriggerCondition {
    // ==================== 基础匹配 ====================
    private String entryPointId;                  // 入口点 ID（必填，与绑定表一致）

    // ==================== 物料属性匹配 ====================
    private String materialType;                  // 物料类型（可选，如 "WAFER", "CASSETTE"）
    private String materialGrade;                 // 物料等级（可选，如 "A", "B", "C"）
    private String materialBatch;                 // 批次号（可选，支持通配符）
    private Map<String, String> materialAttributes;  // 自定义物料属性（可选）

    // ==================== 工艺标记匹配 ====================
    private String processTag;                    // 工艺标记（可选，如 "REWORK", "NORMAL", "URGENT"）
    private List<String> allowedProcessTags;      // 允许的工艺标记列表（可选）

    // ==================== 自定义条件 ====================
    private Predicate<EventContext> customCondition;  // 自定义条件（可选，完全自定义）

    /**
     * Check if this condition matches the given event.
     *
     * @param event Material entry event
     * @return true if condition matches
     */
    public boolean matches(MaterialEvent event) {
        // 1. Check entry point (required)
        if (!event.getEntryPointId().equals(entryPointId)) {
            return false;
        }

        // 2. Check material type (if specified)
        if (materialType != null && !materialType.equals(event.getMaterialType())) {
            return false;
        }

        // 3. Check material grade (if specified)
        if (materialGrade != null && !materialGrade.equals(event.getMaterialGrade())) {
            return false;
        }

        // 4. Check material batch (if specified, supports wildcard)
        if (materialBatch != null && !matchWildcard(materialBatch, event.getBatchId())) {
            return false;
        }

        // 5. Check material attributes (if specified)
        if (materialAttributes != null && !matchAttributes(materialAttributes, event.getAttributes())) {
            return false;
        }

        // 6. Check process tag (if specified)
        if (processTag != null && !processTag.equals(event.getProcessTag())) {
            return false;
        }

        // 7. Check allowed process tags (if specified)
        if (allowedProcessTags != null && !allowedProcessTags.contains(event.getProcessTag())) {
            return false;
        }

        // 8. Check custom condition (if specified)
        if (customCondition != null && !customCondition.test(new EventContext(event))) {
            return false;
        }

        return true;
    }
}
```

#### EventTrigger 数据模型

```java
/**
 * Event trigger for automatic task generation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class EventTrigger {
    private String id;
    private String processFlowId;                // 关联流程 ID
    private TriggerCondition condition;          // 触发条件
    private boolean enabled;
    private int priority;                        // 触发优先级（数值小 = 优先级高）
}
```

#### Given/When/Then

**Scenario 1: 入口点触发（entryPointId 匹配）**

```gherkin
Given 场景入口点 "Bay-01" 配置了触发器
And 触发条件 entryPointId = "Bay-01"
And 关联流程 "晶圆入库流程"
When 物料通过 "Bay-01" 进入场景
Then 触发器匹配成功
And 系统自动生成 "晶圆入库流程" 的第一个任务
```

**Scenario 2: 物料类型 + 等级匹配**

```gherkin
Given 触发器条件 materialType = "WAFER"
And 触发器条件 materialGrade = "A"
And 关联流程 "A级晶圆加工流程"
When 物料进入场景，type="WAFER", grade="A"
Then 触发器匹配成功
And 系统自动生成任务
When 物料进入场景，type="WAFER", grade="B"
Then 触发器匹配失败
And 不生成任务
```

**Scenario 3: 工艺标记匹配**

```gherkin
Given 触发器条件 processTag = "URGENT"
And 关联流程 "紧急流程"
When 物料进入场景，processTag = "URGENT"
Then 触发器匹配成功
And 使用 "紧急流程"
```

**Scenario 4: 多触发器优先级**

```gherkin
Given 入口点 "Bay-01" 配置了 2 个触发器
And 触发器 A 优先级 10，关联 "标准流程"
And 触发器 B 优先级 1，关联 "紧急流程"
And 两个触发器条件都满足
When 物料通过 "Bay-01" 进入场景
Then 选择触发器 B（优先级数值小 = 优先级高）
And 只生成 "紧急流程" 的任务
```

**Scenario 5: entryPointId 与绑定表一致性**

```gherkin
Given process_flow_bindings 表中记录 entry_point_id = "Bay-01"
And 关联的 trigger_condition.entryPointId = "Bay-01"
When 物料通过 "Bay-01" 进入场景
Then 触发器正确匹配
When binding 表中 entry_point_id 与 trigger_condition.entryPointId 不一致
Then 系统返回错误："entryPointId 不一致"
```

---

### Requirement: REQ-WF-006 TaskGenerator 自动任务生成（多候选设备支持）

系统MUST实现REQ-WF-006（TaskGenerator 自动任务生成（多候选设备支持））能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P0 (核心功能)

**描述**: 当 EventTrigger 匹配成功后，TaskGenerator 必须自动生成任务并加入 TaskQueue。支持从候选设备列表中选择最优设备。

#### 生成流程（v2.0）

```
1. EventTrigger.matches(material) → true
2. 查找 process_flow_bindings，获取绑定配置
3. 读取 ProcessFlow 定义（通过 flow_id + flow_version）
4. 获取第一个 ProcessStep 的 targetEntityIds 列表
5. 执行设备选择算法（多因子加权）：
   - 收集所有候选设备的 distance, time, wip 指标
   - Min-max 归一化
   - 计算加权分数，选择最优设备
6. 生成第一个 ProcessStep 对应的任务（目标为选中的设备）
7. 任务写入 TaskQueue
8. 返回 taskId 给调用方
```

#### Given/When/Then

**Scenario 1: 单步任务生成（单候选）**

```gherkin
Given 流程 "简单运输" 包含 1 个工序
And 工序目标设备列表为 ["Machine-A"]
And 触发器匹配成功
When TaskGenerator 执行任务生成
Then 生成 1 个 TransportTask
And TransportTask.source = entryPointId
And TransportTask.destination = "Machine-A"（唯一候选）
And 任务写入 TaskQueue
```

**Scenario 1.1: 单步任务生成（多候选选择）**

```gherkin
Given 流程 "简单运输" 包含 1 个工序
And 工序目标设备列表为 ["Machine-A", "Machine-B", "Machine-C"]
And 系统配置权重：distanceWeight=0.4, timeWeight=0.4, wipWeight=0.2
And 设备状态：
  - Machine-A: distance=10m, time=20s, wip=2
  - Machine-B: distance=5m, time=15s, wip=5
  - Machine-C: distance=15m, time=30s, wip=0
And 触发器匹配成功
When TaskGenerator 执行任务生成
Then 执行多因子加权选择
And 选择 Machine-B（最优分数）
And TransportTask.destination = "Machine-B"
And 任务写入 TaskQueue
```

**Scenario 2: 多步任务序列生成**

```gherkin
Given 流程 "完整加工" 包含 3 个工序：清洗 → 光刻 → 蚀刻
And 每个工序都有多候选设备列表
And 触发器匹配成功
When TaskGenerator 执行任务生成
Then 只生成第一个工序的任务（清洗）
And 从清洗工序的候选设备中选择最优设备
And 任务包含后续流程上下文（flowId, flowVersion, currentStepIndex）
And 后续任务在当前任务完成时自动生成
```

---

### Requirement: REQ-WF-007 WorkflowExecutor 流程执行器

系统MUST实现REQ-WF-007（WorkflowExecutor 流程执行器）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P0 (核心功能)

**描述**: WorkflowExecutor 负责执行流程中的每个工序，管理工序状态转换。

#### 执行状态机

**注**: 以下状态名（PENDING, PREPARING, EXECUTING, COMPLETING, NEXT_STEP, TERMINATED）是工序状态机的状态名称，与 OutputStrategy 枚举值（DIRECT_TO_NEXT, INTERMEDIATE_STORAGE 等）不同。OutputStrategy 决定从 COMPLETING 状态转移到哪个后续状态。

```
┌──────────────┐
│   PENDING    │ (等待执行)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  PREPARING   │ (准备资源、车辆)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  EXECUTING   │ (执行中：加工/运输)
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  COMPLETING  │ (完成处理、产出决策)
└──────┬───────┘
       │
       ├─────────────────┐
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│   NEXT_STEP  │  │  TERMINATED  │
└──────────────┘  └──────────────┘
```

#### Given/When/Then

**Scenario 1: 工序执行成功**

```gherkin
Given 当前工序为 "光刻"
And 目标机台 "Lithography-A" 可用
And 处理时间分布为 NORMAL(mean=50, std=5)
When WorkflowExecutor 开始执行工序
Then 状态转换为 PREPARING
Then 资源准备完成后转换为 EXECUTING
Then 等待处理时间后转换为 COMPLETING
Then 产出策略决策后转换为 NEXT_STEP 或 TERMINATED
```

**Scenario 2: 工序执行失败重试**

```gherkin
Given 当前工序为 "光刻"
And 重试配置为指数退避（maxRetries=3）
When 机台不可用导致执行失败
Then 触发重试机制
And 第一次重试延迟 1s
And 第二次重试延迟 2s
And 第三次重试延迟 4s
And 第三次仍失败后标记为 FAILED
```

---

### Requirement: REQ-WF-008 上下游依赖检查

系统MUST实现REQ-WF-008（上下游依赖检查）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P1 (重要功能)

**描述**: 在执行工序前，系统必须检查所有前置工序是否已完成。

#### 检查逻辑

```java
public boolean areAllPredecessorsCompleted(ProcessStep step) {
    if (step.getPredecessorIds() == null || step.getPredecessorIds().isEmpty()) {
        return true;  // 无前置依赖
    }

    for (String predId : step.getPredecessorIds()) {
        ProcessStep pred = getStepById(predId);
        if (pred.getState() != StepState.COMPLETED) {
            return false;  // 有前置工序未完成
        }
    }
    return true;
}
```

#### Given/When/Then

**Scenario 1: 前置工序未完成**

```gherkin
Given 工序流程：Step1 → Step2 → Step3
And Step1 状态为 COMPLETED
And Step2 状态为 EXECUTING
And Step3 配置 predecessorIds = ["Step2"]
When 系统检查 Step3 的前置依赖
Then 检查结果为 false（Step2 未完成）
And Step3 保持 PENDING 状态
And 不开始执行 Step3
```

**Scenario 2: 前置工序已完成**

```gherkin
Given 工序流程：Step1 → Step2 → Step3
And Step1 状态为 COMPLETED
And Step2 状态为 COMPLETED
And Step3 配置 predecessorIds = ["Step2"]
When 系统检查 Step3 的前置依赖
Then 检查结果为 true
And Step3 可以开始执行
```

**Scenario 3: 循环依赖检测**

```gherkin
Given 用户尝试创建循环依赖：Step1 → Step2 → Step3 → Step1
When 系统保存流程定义
Then 拒绝保存
And 返回错误："检测到循环依赖"
```

---

### Requirement: REQ-WF-009 MaterialBindingManager 物料互斥绑定

系统MUST实现REQ-WF-009（MaterialBindingManager 物料互斥绑定）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P1 (重要功能)

**描述**: 同一物料在同一时刻只能绑定到一个工序，防止多流程并行冲突。

#### 互斥机制（v1.5）

```java
/**
 * Material binding manager for exclusive material binding.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class MaterialBindingManager {
    private SimulationEnv env;
    private Map<String, MaterialBinding> bindings;  // materialId → Binding

    /**
     * Try to bind material to a step.
     *
     * Uses putIfAbsent for atomic binding operation.
     * No timeout force unbind - binding only released on completion/failure.
     *
     * @param materialId Material ID
     * @param stepId Step ID
     * @return true if bind successful, false if already bound
     */
    public boolean tryBind(String materialId, String stepId) {
        // Atomic putIfAbsent operation
        MaterialBinding newBinding = new MaterialBinding(materialId, stepId, env.now());
        MaterialBinding existing = bindings.putIfAbsent(materialId, newBinding);

        return existing == null;  // null means success (no existing binding)
    }

    /**
     * Unbind material from step.
     * Called only when step completes or fails.
     */
    public void unbind(String materialId) {
        bindings.remove(materialId);
    }
}
```

#### Given/When/Then

**Scenario 1: 正常绑定与解绑**

```gherkin
Given 物料 "WAFER-001" 未绑定
When 工序 Step1 尝试绑定物料
Then 绑定成功
And 物料状态为 BOUND_TO_STEP1
When Step1 完成
Then 物料自动解绑
And 物料状态为 UNBOUND
```

**Scenario 2: 绑定冲突（无超时强制解绑）**

```gherkin
Given 物料 "WAFER-001" 已绑定到工序 Step1
When 工序 Step2 尝试绑定同一物料
Then 绑定失败（物料已被占用）
And Step2 返回 false
And Step2 需要等待或返回错误
And 系统不会强制解绑（v1.5 行为）
```

**Scenario 3: 使用仿真时间**

```gherkin
Given 仿真环境当前时间为 100.0s
When 物料绑定到工序
Then 绑定时间记录为 100.0s（仿真时间）
When 检查绑定持续时间
Then 使用 env.now() - bindTime 计算时长
```

---

### Requirement: REQ-WF-010 SafetyZone 人车混合交管

系统MUST实现REQ-WF-010（SafetyZone 人车混合交管）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P1 (重要功能)

**描述**: SafetyZone 用于人车混合区域的交通管制，与 ControlPoint 分层管理。

#### SafetyZone 实体定义（v1.5）

```java
/**
 * Safety zone for human-vehicle mixed areas.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class SafetyZone {
    private String id;
    private String name;
    private GeometryType geometryType;           // CIRCLE, RECTANGLE, POLYGON
    private Position center;
    private double radius;                       // For CIRCLE
    private double width;                        // For RECTANGLE
    private double height;                       // For RECTANGLE
    private List<Position> boundary;             // For POLYGON

    // 容量配置
    private int maxHumans;                       // 最大人数（0=无限制）
    private int maxVehicles;                     // 最大车数（0=无限制）
    private Priority accessPriority;             // HUMAN_FIRST, VEHICLE_FIRST, FIFO, PRIORITY_BASED

    // 关联
    private List<String> connectedPathIds;
    private List<String> monitoredEntityIds;
    private boolean enabled;
}

public enum Priority {
    HUMAN_FIRST,   // Humans always have priority
    VEHICLE_FIRST, // Vehicles have priority (rare)
    FIFO,          // First-come-first-served
    PRIORITY_BASED // Based on task priority
}
```

#### 职责分离

| 功能 | SafetyZone | ControlPoint/Edge |
|------|-----------|------------------|
| **占用主体** | Human + Vehicle | 仅 Vehicle |
| **占用方式** | 可配置，默认共享 | 可配置，默认独占 |
| **容量限制** | 可配置（默认多实体） | 可配置，默认 1 |
| **典型场景** | 人车混合区域 | 车车冲突避让 |

#### 存储格式

SafetyZone 作为实体类型存储在 `scenes.entities` JSON 数组中：

```json
{
  "sceneId": "scene-wafer-fab-01",
  "name": "晶圆厂 01 号线",
  "entities": [
    {"id": "Machine-A", "type": "MACHINE", ...},
    {"id": "AGV-01", "type": "AGV_VEHICLE", ...},
    {"id": "CP-MAIN-01", "type": "CONTROL_POINT", ...},
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

#### Given/When/Then

**Scenario 1: 人车混合区域容量控制**

```gherkin
Given 安全区 "MAIN-AISLE" 配置 maxHumans = 10, maxVehicles = 2
And accessPriority = HUMAN_FIRST
And 当前占用：5 人，1 车
When Human-001 尝试进入
Then 允许进入（人数未满）
When AGV-001 尝试进入
Then 允许进入（车数未满）
When AGV-002 尝试进入
Then 允许进入（车数满）
When AGV-003 尝试进入
Then 拒绝进入（车数已满），需等待
```

**Scenario 2: Human 优先策略**

```gherkin
Given 安全区配置 HUMAN_FIRST
And Human-001 和 AGV-001 同时请求进入
Then Human-001 优先进入
And AGV-001 等待
```

**Scenario 3: FIFO 策略**

```gherkin
Given 安全区配置 FIFO
And Human-001 在时间 100s 申请进入
And AGV-001 在时间 105s 申请进入
And AGV-002 在时间 110s 申请进入
Then 按 100s → 105s → 110s 顺序处理
And 先到先服务
```

**Scenario 4: PRIORITY_BASED 策略**

```gherkin
Given 安全区配置 PRIORITY_BASED
And Human-001 优先级 5（低），在时间 100s 申请
And AGV-001 优先级 1（高），在时间 105s 申请
Then AGV-001 优先进入（优先级高）
```

**Scenario 5: Human 不占用 ControlPoint**

```gherkin
Given Human-001 位于 ControlPoint "CP-MAIN" 范围内
And ControlPoint 配置容量 1
When AGV-001 请求进入 ControlPoint
Then Human-001 不占用 ControlPoint 容量
And AGV-001 可以进入（无冲突）
```

---

### Requirement: REQ-WF-011 RetryConfig 可配置重试

系统MUST实现REQ-WF-011（RetryConfig 可配置重试）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P1 (重要功能)

**描述**: 支持可配置的重试策略，包括指数退避、最大重试次数、重试延迟。

#### 重试配置

```java
/**
 * Retry configuration for task execution.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class RetryConfig {
    private int maxRetries;                     // 最大重试次数（0=不重试）
    private RetryBackoffStrategy backoffStrategy; // 退避策略
    private double initialDelay;                // 初始延迟（秒，仿真时间）
    private double maxDelay;                    // 最大延迟（秒，仿真时间）
    private double backoffMultiplier;           // 退避乘数
    private boolean jitterEnabled;              // 是否启用抖动
}

public enum RetryBackoffStrategy {
    FIXED,          // 固定延迟
    EXPONENTIAL,    // 指数退避
    LINEAR          // 线性退避
}
```

#### 配置项（system_config）

```
workflow.retry.maxRetries = 3
workflow.retry.backoffStrategy = EXPONENTIAL
workflow.retry.initialDelay = 1.0
workflow.retry.maxDelay = 60.0
workflow.retry.backoffMultiplier = 2.0
workflow.retry.jitterEnabled = true
```

#### Given/When/Then

**Scenario 1: 指数退避重试**

```gherkin
Given 重试配置：maxRetries=3, backoffStrategy=EXPONENTIAL
And initialDelay=1.0, backoffMultiplier=2.0
When 任务执行失败
Then 第 1 次重试延迟 1s
And 第 2 次重试延迟 2s
And 第 3 次重试延迟 4s
And 第 3 次仍失败后标记为 FAILED
```

**Scenario 2: 抖动避免惊群**

```gherkin
Given 重试配置：jitterEnabled = true
And 10 个任务同时失败
When 触发重试
Then 重试时间加入随机抖动
And 避免所有任务同时重试（惊群效应）
```

---

### Requirement: REQ-WF-012 ResourceValidationStrategy 资源可用性策略

系统MUST实现REQ-WF-012（ResourceValidationStrategy 资源可用性策略）能力，具体输入输出与行为约束以本节场景定义为准。

**优先级**: P2 (增强功能)

**描述**: 支持多种资源可用性校验策略，控制资源不可用时的处理方式（v1.5）。

#### 策略类型

| 策略 | 说明 | 行为 |
|------|------|------|
| `BLOCK_AND_RETRY` | 阻塞并重试 | 不生成任务，注册资源监听器，资源可用时重新触发验证 |
| `ENQUEUE_AND_WAIT` | 入队等待 | 生成任务并入队，任务标记为等待资源，资源可用时由调度器处理 |

#### 策略详细说明

```java
/**
 * Resource validation strategy (v1.5).
 *
 * Defines behavior when transport resources are unavailable.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
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
```

#### Given/When/Then

**Scenario 1: 阻塞并重试（BLOCK_AND_RETRY）**

```gherkin
Given 资源策略为 BLOCK_AND_RETRY
And 目标机台 "Machine-A" 不可用（故障）
When 任务尝试获取资源
Then 不生成任务
And 系统注册资源监听器
And 当 "Machine-A" 恢复可用
Then 重新触发验证
And 验证通过后生成任务
```

**Scenario 2: 入队等待（ENQUEUE_AND_WAIT）**

```gherkin
Given 资源策略为 ENQUEUE_AND_WAIT
And 目标机台 "Machine-A" 被占用
When 任务尝试获取资源
Then 生成任务并标记为等待资源
And 任务入队
And 当 "Machine-A" 释放时
Then 调度器处理等待中的任务
And 任务开始执行
```

---

## 附录

### A. 配置项清单（system_config）

```
# Workflow 重试配置
workflow.retry.maxRetries = 3
workflow.retry.backoffStrategy = EXPONENTIAL
workflow.retry.initialDelay = 1.0
workflow.retry.maxDelay = 60.0
workflow.retry.backoffMultiplier = 2.0
workflow.retry.jitterEnabled = true

# Workflow 资源策略
workflow.resource.validationStrategy = BLOCK_AND_RETRY
workflow.resource.timeout = 60.0

# Workflow WIP 配置
workflow.wip.defaultLimit = 0
workflow.wip.globalEnabled = false

# SafetyZone 配置
safetyzone.default.maxHumans = 10
safetyzone.default.maxVehicles = 2
safetyzone.default.accessPriority = HUMAN_FIRST  # 可选: VEHICLE_FIRST, FIFO, PRIORITY_BASED

# 多候选设备选择权重配置（v2.0）
dispatch.selector.weight.distance = 0.4    # 距离因子权重
dispatch.selector.weight.time = 0.4        # 时间因子权重
dispatch.selector.weight.wip = 0.2         # WIP 因子权重
dispatch.selector.normalization = min-max  # 归一化方法：min-max（默认）, z-score
```

### B. 数据库表

| 表名 | 说明 |
|------|------|
| `process_flows` | 工艺流程定义 |
| `process_flow_bindings` | 流程与场景/入口点绑定 |

### C. 多租户字段

所有表必须包含：
- `tenant_id` 字段（多租户隔离）
- 联合主键/唯一键包含 `tenant_id`
- 索引包含 `tenant_id`

### D. Flyway 迁移脚本命名

- `V2__create_process_flows.sql`（在 V1__Initial_schema.sql 之后）
- `V3__create_process_flow_bindings.sql`
