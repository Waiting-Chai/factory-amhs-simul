# 交通管制能力规范

## Metadata
- **Spec ID**: `traffic-control`
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Status**: 提案阶段

---

## ADDED Requirements

### Requirement: REQ-TC-000 交通配置项（system_config）

系统MUST实现REQ-TC-000（交通配置项（system_config））能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 交通管制相关参数统一由 system_config 管理

**配置项清单（含默认策略）**:
- `controlpoint.default_capacity`（默认: 1）
- `controlarea.default_capacity`（默认: 3）
- `edge.default_capacity`（默认: 1）
- `edge.release_policy`（默认: full_body）
- `traffic.reservation_mode`（默认: immediate）
- `traffic.deadlock.strategy`（默认: wait_graph+timeout）
- `traffic.deadlock.timeout`（默认: 60s 仿真时间）
- `traffic.replan.timeoutSeconds`（默认: 60s 仿真时间）
- `traffic.replan.maxAttempts`（默认: 3 次）
- `traffic.priority.aging.enabled`（默认: true）
- `traffic.priority.aging.step`（默认: 30s 仿真时间）
- `traffic.priority.aging.boost`（默认: 1）
- `traffic.priority.aging.max`（默认: 5）
- `traffic.processing.mode`（默认: event）

#### Scenario: 配置生效时机
**Given** 交通管制相关配置发生变更
**When** system_config 更新完成
**Then** 配置应立即生效（无需重启）

### Requirement: REQ-TC-001 NetworkGraph 路网模型

系统MUST实现REQ-TC-001（NetworkGraph 路网模型）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 定义可导航的路网结构

#### Scenario: 创建路网
**Given** 一组节点和边
**When** 创建 NetworkGraph
**Then** 路网应正确存储拓扑结构
**And** 应支持动态添加/删除节点和边

#### Scenario: 路网连通性查询
**Given** 一个路网
**When** 查询两个节点间的连通性
**Then** 如果存在路径，应返回 true
**And** 如果不存在，应返回 false

#### Scenario: 双向路径存储
**Given** 一条双向路段
**When** 保存到路网中
**Then** 必须拆分为两条有向边存储
**And** 两条边方向相反、各自有独立 edgeId
**And** 不使用单条边的 bidirectional 标记

---

### Requirement: REQ-TC-002 PathPlanner 路径规划

系统MUST实现REQ-TC-002（PathPlanner 路径规划）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现车辆路径规划算法

#### Scenario: 最短路径计算
**Given** 一个路网
**And** 起点节点 A 和终点节点 B
**When** 计算从 A 到 B 的路径
**Then** 应返回最短路径 (节点列表)
**And** 路径应连续且可通行

#### Scenario: 无路径情况
**Given** 一个不连通的路网
**And** 两个不连通的节点
**When** 计算路径
**Then** 应返回空路径
**And** 应记录错误日志

#### Scenario: 路径缓存
**Given** 重复的路径查询
**When** 第一次计算 A 到 B 的路径
**Then** 结果应被缓存
**When** 再次查询相同路径
**Then** 应从缓存返回 (不重新计算)

### Requirement: REQ-TC-003 曲线路径支持（Bezier）

系统MUST实现REQ-TC-003（曲线路径支持（Bezier））能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: OHT/AGV 路径支持贝塞尔曲线

#### Scenario: 曲线路径定义
**Given** 一条路径段
**When** 定义为贝塞尔曲线
**Then** 路径段应包含控制点（p0, p1, p2, p3）
**And** 曲线应用于渲染与路径长度计算
**And** 贝塞尔控制点仅用于几何形状，不等同于交通控制点
**And** 段结构必须明确字段

#### Scenario: 贝塞尔段结构
**Given** 一条路径段
**When** 段类型为 BEZIER
**Then** 段结构应包含：`from`, `to`, `c1`, `c2`
**And** `from`/`to` 引用 points 中的点 ID
**And** `c1`/`c2` 为绝对坐标（单位 m）
**And** LINEAR 段仅包含 `from`, `to`

#### Scenario: 路径段类型
**Given** 路径段数据结构
**When** 指定段类型
**Then** 应支持 `LINEAR` 与 `BEZIER` 两种类型

### Requirement: REQ-TC-004 统一单位约束

系统MUST实现REQ-TC-004（统一单位约束）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 路径与角度单位统一规范

#### Scenario: 路径坐标单位
**Given** 路径点坐标
**When** 保存或计算路径
**Then** 坐标单位必须为米（m）

#### Scenario: 角度单位
**Given** 路径方向或转向角
**When** 计算角度
**Then** 角度必须采用弧度制
**And** 范围为 [-π, π]

### Requirement: REQ-TC-005 ControlPoint 控制点

系统MUST实现REQ-TC-005（ControlPoint 控制点）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现控制点用于交通容量控制

#### Scenario: 控制点容量限制
**Given** 一个容量为 1 的 ControlPoint
**And** 两辆车同时请求进入
**When** 第一辆车请求进入
**Then** 请求应被批准
**And** 容量应变为 0
**When** 第二辆车请求进入
**Then** 请求应被拒绝
**And** 车辆应等待
**And** 控制点容量优先来自 system_config
**And** 若未配置则默认容量为 1

#### Scenario: 控制点位置绑定
**Given** 一个 ControlPoint
**When** 定义其位置
**Then** 位置应绑定到路径点 ID（如 at: "P3"）

#### Scenario: 控制点释放
**Given** 一个被占用的 ControlPoint
**And** 一辆占用车辆
**When** 车辆离开控制点
**Then** 容量应恢复
**And** 等待的车辆 (如果有) 应被通知

#### Scenario: 状态占用规则
**Given** 一辆车处于 MOVING / LOADING / UNLOADING / BLOCKED
**When** 车辆位于控制点
**Then** 控制点应被视为占用

#### Scenario: 控制点优先级
**Given** 一个容量为 1 的 ControlPoint
**And** 两辆不同优先级的车辆等待
**When** 控制点变为可用
**Then** 高优先级车辆应优先获得访问权限
**And** 等待队列采用优先级队列（按有效优先级排序）

#### Scenario: 冲突仲裁顺序
**Given** 多辆车在同一 ControlPoint 等待
**When** 进行访问仲裁
**Then** 应按以下顺序仲裁：
**And** 1) 有效优先级（basePriority + agingBoost）
**And** 2) 等待时间（FIFO）
**And** 3) 距离控制点更近（可选）
**And** 4) 随机打破平局
**And** ControlPoint 不引入局部优先级权重

### Requirement: REQ-TC-006 ControlArea 控制区

系统MUST实现REQ-TC-006（ControlArea 控制区）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现控制区用于大范围交通管制

#### Scenario: 控制区容量限制
**Given** 一个最大车辆数为 3 的 ControlArea
**And** 3 辆车已在区域内
**When** 第 4 辆车尝试进入
**Then** 请求应被拒绝
**And** 车辆应在区域外等待
**And** ControlArea 容量默认值由 system_config 配置

#### Scenario: 控制区嵌套
**Given** 一个嵌套的 ControlArea 结构
**And** 区域 A 包含区域 B
**When** 车辆从 A 进入 B
**Then** 两者的容量都应正确更新（进入子区同时占用父区）
**When** 车辆离开 B
**Then** 只有 B 的容量恢复

#### Scenario: 控制区统计
**Given** 一个 ControlArea
**And** 多辆车辆进出
**When** 查询区域统计
**Then** 应返回当前车辆数、历史峰值、平均等待时间

### Requirement: REQ-TC-007 ConflictResolver 冲突解决

系统MUST实现REQ-TC-007（ConflictResolver 冲突解决）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现车辆冲突解决策略

#### Scenario: 优先级冲突解决
**Given** 两辆车竞争同一资源
**And** 车辆 A 优先级为 URGENT
**And** 车辆 B 优先级为 NORMAL
**When** 发生冲突
**Then** 车辆 A 应获得资源
**And** 车辆 B 应等待

#### Scenario: 先到先得冲突解决
**Given** 两辆车竞争同一资源
**And** 两车优先级相同
**And** 车辆 A 更早到达
**When** 发生冲突
**Then** 车辆 A 应获得资源
**And** 车辆 B 应等待

#### Scenario: 距离冲突解决
**Given** 两辆车竞争同一控制点
**And** 优先级相同
**And** 车辆 A 距离控制点更近
**When** 发生冲突
**Then** 车辆 A 应获得资源

#### Scenario: 死锁检测
**Given** 多辆车形成循环等待
**When** 检测到死锁
**Then** 应触发死锁解决机制
**And** 其中一辆车应后退或改道
**And** 死锁检测采用“等待图 + 超时兜底”的混合策略

#### Scenario: 阻塞超时重规划
**Given** 一辆车在等待控制点/路段
**When** 等待时间超过阈值
**Then** 应触发自动重规划
**And** 默认阈值为 60s 仿真时间（可配置）

#### Scenario: 重规划次数上限
**Given** 一辆车连续触发重规划
**When** 达到最大重规划次数
**Then** 应停止继续重规划并进入等待
**And** 最大次数由 system_config 配置（默认 3 次）

### Requirement: REQ-TC-007.1 交通请求前置校验（v2.0）

系统MUST实现REQ-TC-007.1（交通请求前置校验（v2.0））能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 在发起交通请求前，必须校验设备支持的运输类型与车辆类型是否匹配。

#### 校验规则

```java
/**
 * Validate transport type compatibility before traffic request.
 *
 * @param vehicle Vehicle requesting traffic access
 * @param targetDevice Target device entity
 * @throws ValidationException if transport types don't match
 */
public void validateTransportTypeCompatibility(Vehicle vehicle, DeviceEntity targetDevice) {
    // Get vehicle's transport type
    TransportType vehicleType = getVehicleTransportType(vehicle); // OHT, AGV, HUMAN, etc.

    // Check if target device supports this transport type
    if (!targetDevice.supportsTransportType(vehicleType)) {
        throw new ValidationException(String.format(
            "Vehicle type %s not supported by device %s. Supported types: %s",
            vehicleType, targetDevice.getId(), targetDevice.getSupportedTransportTypes()));
    }
}
```

#### 车辆类型映射

```java
/**
 * Map vehicle entity type to transport type.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public enum VehicleTransportMapping {
    OHT_VEHICLE(TransportType.OHT),
    AGV_VEHICLE(TransportType.AGV),
    OPERATOR(TransportType.HUMAN),
    CONVEYOR(TransportType.CONVEYOR);

    private final TransportType transportType;

    VehicleTransportMapping(TransportType transportType) {
        this.transportType = transportType;
    }

    public static TransportType fromEntityType(EntityType entityType) {
        for (VehicleTransportMapping mapping : values()) {
            if (mapping.name().equals(entityType.name())) {
                return mapping.transportType;
            }
        }
        throw new IllegalArgumentException("Unknown vehicle type: " + entityType);
    }
}
```

#### Given/When/Then

**Scenario 1: 运输类型校验 - 成功**

```gherkin
Given OHT 车辆 OHT-001 请求访问 Machine-A
And Machine-A 的 supportedTransportTypes = ["OHT", "AGV"]
When 系统执行运输类型校验
Then 校验通过
And OHT-001 可以发起交通请求
```

**Scenario 2: 运输类型校验 - 失败**

```gherkin
Given OHT 车辆 OHT-001 请求访问 ManualStation-B
And ManualStation-B 的 supportedTransportTypes = ["HUMAN"]
When 系统执行运输类型校验
Then 校验失败
And 返回错误："Vehicle type OHT not supported by device ManualStation-B. Supported types: [HUMAN]"
And OHT-001 不允许发起交通请求
```

**Scenario 3: AGV 访问仅支持 OHT 的设备**

```gherkin
Given AGV 车辆 AGV-001 请求访问 Machine-C
And Machine-C 的 supportedTransportTypes = ["OHT"]
When 系统执行运输类型校验
Then 校验失败
And 返回错误："Vehicle type AGV not supported by device Machine-C. Supported types: [OHT]"
```

**Scenario 4: 人工搬运访问人工工位**

```gherkin
Given 操作员 Operator-001 请求访问 ManualStation-A
And ManualStation-A 的 supportedTransportTypes = ["HUMAN", "OHT"]
When 系统执行运输类型校验
Then 校验通过（HUMAN 在支持列表中）
And Operator-001 可以发起交通请求
```

**Scenario 5: 输送线访问设备**

```gherkin
Given 输送线 Conveyor-001 连接到 Machine-A
And Machine-A 的 supportedTransportTypes = ["CONVEYOR", "AGV"]
When 系统执行运输类型校验
Then 校验通过（CONVEYOR 在支持列表中）
```

---

### Requirement: REQ-TC-008 TrafficManager 交通管制器

系统MUST实现REQ-TC-008（TrafficManager 交通管制器）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 统一管理交通控制组件

#### Scenario: 交通管制初始化
**Given** 配置好的控制点、控制区、冲突解决器
**When** 创建 TrafficManager
**Then** 所有组件应正确注册
**And** 管制器应进入运行状态

#### Scenario: 事件驱动处理
**Given** 车辆通行请求或资源释放事件
**When** 事件触发
**Then** TrafficManager 应即时处理（事件驱动）

#### Scenario: 车辆通行请求
**Given** 一个运行中的 TrafficManager
**And** 一辆车请求通过控制点
**When** 执行通行请求
**Then** 管制器应检查控制点容量
**And** 如可用，批准请求
**And** 如不可用，根据冲突解决策略决定

#### Scenario: 共享资源仲裁
**Given** OHT 与 AGV 争用同一装卸口/Stocker 口
**When** 发生资源冲突
**Then** 应由统一 TrafficManager 进行仲裁

#### Scenario: 车辆路径管理
**Given** 一辆车规划了一条路径
**When** 车辆开始沿路径移动
**Then** TrafficManager 应预占路径上的控制点
**And** 车辆通过后应释放控制点

#### Scenario: 资源申请顺序
**Given** 下一步路径同时涉及 ControlPoint 与 Edge
**When** 车辆申请通行
**Then** 应先申请 ControlPoint，再申请 Edge
**And** 任意申请失败时不占用任何资源（避免死锁）

#### Scenario: 故障车辆资源占用
**Given** 车辆在 ControlPoint/Edge 上发生故障
**When** 进入故障状态
**Then** 应保留当前占用资源
**And** 释放未来预占资源
**And** 触发维修任务并等待修复后释放占用

### Requirement: REQ-TC-009 统一入口 + 分域策略（OHT/AGV 可插拔）

系统MUST实现REQ-TC-009（统一入口 + 分域策略（OHT/AGV 可插拔））能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 交通管制统一入口，但按路径类型使用不同策略

#### Scenario: 单一交通入口
**Given** 系统存在 OHT 与 AGV
**When** 车辆发起通行请求
**Then** 所有请求应进入统一 TrafficManager 入口

#### Scenario: 分域策略选择
**Given** 路径类型为 OHT_TRACK
**When** TrafficManager 处理请求
**Then** 应交给 OHT 交通策略处理
**Given** 路径类型为 AGV_NETWORK
**When** TrafficManager 处理请求
**Then** 应交给 AGV 交通策略处理

#### Scenario: 可插拔策略
**Given** 场景仅包含 OHT
**When** 初始化 TrafficManager
**Then** 仅注册 OHT 策略，AGV 策略不加载

### Requirement: REQ-TC-010 EdgeReservation 路段占用控制

系统MUST实现REQ-TC-010（EdgeReservation 路段占用控制）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 在 ControlPoint 基础上增加路段占用控制

#### Scenario: 路段占用申请
**Given** 一段 Edge（路段）
**And** 车辆准备进入该路段
**When** 申请占用
**Then** 若容量允许应批准
**And** 若容量不足应等待或重规划
**And** 占用为即时生效（非时间窗预约）
**And** 路段容量使用固定配置值（整数容量）
**And** 路段容量优先来自 system_config，未配置则默认 1

#### Scenario: 路段占用释放
**Given** 车辆通过路段
**When** 离开路段
**Then** 车身完全离开路段后才释放占用

#### Scenario: 双层管制
**Given** 路段包含关键控制点
**When** 车辆通行
**Then** 必须同时满足 EdgeReservation 与 ControlPoint

### Requirement: REQ-TC-011 PriorityManager 优先级管理

系统MUST实现REQ-TC-011（PriorityManager 优先级管理）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 管理车辆和任务的优先级

#### Scenario: 动态优先级调整
**Given** 一个正在等待的车辆
**When** 车辆的优先级被提高
**Then** 车辆应在资源竞争中获得更高优先级
**And** 如果已有车辆在等待，应重新排序

#### Scenario: 等待时间老化（仿真时间）
**Given** 一个正在等待的车辆
**When** 根据等待时间重新计算优先级
**Then** 应使用仿真时钟计算等待时长（waited = env.now() - waitStartTime）
**And** 有效优先级 = basePriority + floor(waited / agingStep) * agingBoost
**And** 提升不应超过 maxBoost

#### Scenario: 紧急任务优先
**Given** 一个紧急任务被创建
**And** 多个车辆可用
**When** 分配任务
**Then** 任务应被分配给最合适的车辆
**And** 车辆当前任务 (如果有) 应被抢占或延迟

---

## 交叉引用

- 依赖 `logistics-entities` 规范: 需要车辆实体定义
- 支持 `dispatch-system` 规范: 为调度提供交通状态信息
- 被 `kpi-metrics` 规范使用: 交通统计用于 KPI 计算

### Requirement: REQ-TC-012 路径数据结构必填字段

系统MUST实现REQ-TC-012（路径数据结构必填字段）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: OHT/AGV 路径结构必须字段明确

#### Scenario: OHT 轨道结构
**Given** 一条 OHT_TRACK
**When** 定义路径
**Then** 必填字段包含：`id`, `type`, `points`, `segments`, `controlPoints`
**And** `controlPoints` 为交通控制点（容量/优先级用途）
**And** `segments` 中必须标注 `type`（LINEAR/BEZIER）并满足 REQ-TC-003 段结构约束

#### Scenario: AGV 路网结构
**Given** 一条 AGV_NETWORK
**When** 定义路径
**Then** 必填字段包含：`id`, `type`, `nodes`, `edges`
**And** `controlPoints` 可选，用于关键节点限流
**And** `edges` 为有向边记录，双向路段必须存两条反向 edge
