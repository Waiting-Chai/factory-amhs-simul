# 调度系统能力规范

## Metadata
- **Spec ID**: `dispatch-system`
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Status**: 提案阶段

---

## ADDED Requirements

### Requirement: REQ-DS-001 Task 任务模型

系统MUST实现REQ-DS-001（Task 任务模型）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 定义任务数据结构

#### Scenario: 创建运输任务
**Given** 运输请求参数 (起点、终点、物料)
**When** 创建一个 Task
**Then** 任务应具有唯一 ID
**And** 任务状态应为 PENDING
**And** 任务应记录创建时间

#### Scenario: 任务状态转换
**Given** 一个 PENDING 状态的任务
**When** 任务被分配给车辆
**Then** 状态应变为 ASSIGNED
**When** 车辆开始执行
**Then** 状态应变为 IN_PROGRESS
**When** 任务完成
**Then** 状态应变为 COMPLETED

#### Scenario: 任务取消
**Given** 一个未完成的任务
**When** 请求取消任务
**Then** 如果任务未开始，状态应变为 CANCELLED
**And** 如果任务进行中，应通知车辆中断

---

### Requirement: REQ-DS-002 TaskQueue 任务队列

系统MUST实现REQ-DS-002（TaskQueue 任务队列）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 管理待处理任务队列

#### Scenario: 任务入队
**Given** 一个任务队列
**When** 添加新任务
**Then** 任务应根据优先级插入队列
**And** 高优先级任务应在队列前端

#### Scenario: 任务出队
**Given** 一个有任务的队列
**When** 请求下一个任务
**Then** 应返回最高优先级的待处理任务
**And** 任务应从队列中移除

#### Scenario: 任务队列持久化
**Given** 一个任务队列
**When** 仿真停止
**Then** 队列状态应被保存
**When** 仿真恢复
**Then** 队列应恢复到保存状态

---

### Requirement: REQ-DS-003 DispatchRule 调度规则

系统MUST实现REQ-DS-003（DispatchRule 调度规则）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 定义任务分配规则接口

#### Scenario: 最短距离规则
**Given** 一个运输任务
**And** 多辆可用车辆
**When** 使用 ShortestDistanceRule 分配
**Then** 应选择距离任务起点最近的车辆

#### Scenario: 最高优先级规则
**Given** 多个待分配任务
**And** 有限车辆资源
**When** 使用 HighestPriorityRule 分配
**Then** 应优先分配高优先级任务

#### Scenario: 最低利用率规则
**Given** 一个任务
**And** 多辆可用车辆
**When** 使用 LeastUtilizedRule 分配
**Then** 应选择利用率最低的车辆


#### Scenario: 多候选设备按距离优先选择（流程目标设备）
**Given** 工序目标设备列表 [Machine-A, Machine-B, Machine-C]
**And** 设备坐标分别为 A(10,10)、B(30,30)、C(50,50)
**And** 当前位置为 (0,0)
**And** SelectionPolicy = DISTANCE_FIRST
**When** 执行目标设备选择
**Then** 应返回 Machine-A（距离最近）
---

### Requirement: REQ-DS-004 DispatchEngine 调度引擎

系统MUST实现REQ-DS-004（DispatchEngine 调度引擎）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现核心调度逻辑

#### Scenario: 任务自动分配
**Given** 一个运行中的调度引擎
**And** 一个待处理任务
**And** 多辆可用车辆
**When** 调度周期触发
**Then** 应根据调度规则选择车辆
**And** 任务应被分配给选中车辆
**And** 车辆应接收任务通知

#### Scenario: 无可用车辆处理
**Given** 一个待处理任务
**And** 无可用车辆
**When** 调度引擎尝试分配
**Then** 任务应保持在队列中
**And** 应记录告警

#### Scenario: 车辆不可用处理
**Given** 一个任务被分配给车辆
**And** 车辆变为不可用 (故障、充电等)
**When** 检测到车辆不可用
**Then** 任务应被重新分配给其他车辆
**And** 原车辆的任务应被清除

---

### Requirement: REQ-DS-008 维修任务调度

系统MUST实现REQ-DS-008（维修任务调度）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 故障车辆生成维修任务并调度 Human/Operator

#### Scenario: 维修任务创建
**Given** 一辆车辆发生故障
**When** 生成维修任务
**Then** 任务类型应为 MAINTENANCE
**And** 任务应进入任务队列

#### Scenario: 维修时长分布
**Given** 一个维修任务
**When** 执行维修
**Then** 维修时长应由分布模型生成
**And** 默认支持分布为：Normal / LogNormal / Exponential / Triangular

---

### Requirement: REQ-DS-005 RealTimeDispatcher 实时派单

系统MUST实现REQ-DS-005（RealTimeDispatcher 实时派单）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 支持运行时动态添加任务

#### Scenario: 运行时添加任务
**Given** 一个正在运行的仿真
**When** 通过 Web 界面添加新任务
**Then** 任务应立即进入队列
**And** 调度引擎应在下一周期处理

#### Scenario: 紧急任务插入
**Given** 一个运行中的任务队列
**And** 多个待处理任务
**When** 插入一个紧急任务
**Then** 紧急任务应被标记为最高优先级
**And** 应抢占正在执行的低优先级任务 (如果配置允许)

#### Scenario: 手动任务分配
**Given** 一个任务
**And** Web 用户界面上可用车辆列表
**When** 用户手动选择车辆并分配
**Then** 任务应被分配给指定车辆
**And** 调度引擎不应覆盖此分配

---

### Requirement: REQ-DS-006 DynamicReplanning 动态重规划

系统MUST实现REQ-DS-006（DynamicReplanning 动态重规划）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 支持路径动态重规划

#### Scenario: 路径阻塞重规划
**Given** 一辆正在沿路径行驶的车辆
**And** 路径上的某段发生阻塞
**When** 检测到阻塞
**Then** 应为车辆计算新的路径
**And** 车辆应切换到新路径

#### Scenario: 任务取消重规划
**Given** 一辆正在执行任务的车辆
**When** 任务被取消
**Then** 车辆应停止当前移动
**And** 如果有新任务，应规划到新任务起点的路径
**And** 如果无新任务，应返回待命点

#### Scenario: 实时交通重规划
**Given** 多辆车辆在同一区域行驶
**And** 检测到潜在冲突
**When** 交通管理器请求重规划
**Then** 部分车辆应选择替代路径
**And** 应避免死锁

---

### Requirement: REQ-DS-007 VehiclePool 车辆池管理

系统MUST实现REQ-DS-007（VehiclePool 车辆池管理）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 管理车辆状态和可用性

#### Scenario: 车辆状态跟踪
**Given** 一个车辆池
**And** 多辆车辆
**When** 车辆状态改变 (IDLE, BUSY, CHARGING, MAINTENANCE)
**Then** 车辆池应更新状态
**And** 调度引擎应看到最新状态

#### Scenario: 车辆查询
**Given** 一个车辆池
**When** 查询可用车辆
**Then** 应返回所有 IDLE 状态的车辆
**When** 查询特定类型车辆
**Then** 应返回匹配类型的车辆

---

## 交叉引用

- 依赖 `logistics-entities` 规范: 需要车辆实体定义
- 依赖 `traffic-control` 规范: 路径重规划需要交通状态
- 被 `kpi-metrics` 规范使用: 调度统计用于 KPI 计算
- 被 `web-visualization` 规范使用: Web 界面控制调度
