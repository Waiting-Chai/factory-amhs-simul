# KPI 指标与分析规范

## Metadata
- **Spec ID**: `kpi-metrics`
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Status**: 提案阶段

---

## ADDED Requirements

### Requirement: REQ-KPI-001 MetricsCollector 指标收集

系统MUST实现REQ-KPI-001（MetricsCollector 指标收集）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 收集仿真过程中的各类指标

#### 采样策略（自适应采样）

**关键事件触发**（实时记录）:
- 任务完成/失败
- 冲突检测与解决
- 告警触发（WIP 超阈值、设备故障等）
- 车辆状态关键转换

**定期聚合**（60 仿真秒）:
- 利用率计算（时间加权平均）
- 能耗统计
- WIP 统计

#### 双时间基准

**记录时间**:
- `simulated_time`: 仿真时间（秒），用于仿真分析
- `wall_clock_time`: 墙钟时间，用于实时监控
- 所有指标同时记录两种时间基准

#### Scenario: 事件监听收集
**Given** 一个运行中的仿真
**And** 一个 MetricsCollector
**When** 仿真事件发生 (车辆移动、任务完成、资源占用)
**Then** MetricsCollector 应记录事件数据
**And** 数据应包含仿真时间戳、墙钟时间戳、实体 ID、事件类型

#### Scenario: 自适应采样
**Given** 一个配置了自适应采样的 MetricsCollector
**When** 关键事件发生（任务完成、冲突）
**Then** 应立即记录该事件的指标
**When** 达到聚合周期（60 仿真秒）
**Then** 应计算周期内的聚合指标（时间加权平均利用率）

#### Scenario: 双时间基准记录
**Given** 运行中的仿真（timeScale = 10x）
**When** 记录指标
**Then** 应同时记录:
- `simulated_time`: 仿真经过的时间（如 123.45 秒）
- `wall_clock_time`: 现实时间（如 2026-02-06T10:30:00Z）

#### Scenario: 数据存储
**Given** 收集的指标数据
**When** 仿真结束
**Then** 数据应被持久化到数据库
**And** 应支持导出为 CSV/JSON 格式

---

### Requirement: REQ-KPI-002 ThroughputCalculator 吞吐量计算

系统MUST实现REQ-KPI-002（ThroughputCalculator 吞吐量计算）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 计算系统吞吐量指标

#### Scenario: 任务吞吐量
**Given** 一个完成的仿真
**And** 记录的任务完成事件
**When** 计算吞吐量
**Then** 应返回总完成任务数
**And** 应返回单位时间吞吐率 (任务/小时)
**And** 应返回按任务类型分类的吞吐量

#### Scenario: 物料吞吐量
**Given** 一个完成的仿真
**And** 物料运输记录
**When** 计算物料吞吐量
**Then** 应返回总运输物料量
**And** 应返回单位时间物料运输率

#### Scenario: 实时吞吐量监控
**Given** 一个运行中的仿真
**When** 查询当前吞吐量
**Then** 应返回当前时刻的吞吐率
**And** 应返回最近 N 时间段的吞吐量趋势

---

### Requirement: REQ-KPI-003 UtilizationCalculator 利用率计算

系统MUST实现REQ-KPI-003（UtilizationCalculator 利用率计算）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 计算资源利用率（时间加权平均）

#### 计算方法
- **时间加权平均**: 考虑每个状态持续时间
- **公式**: `利用率 = Σ(工作时间区间) / 总时间`
- **采样**: 每 60 仿真秒聚合一次

#### Scenario: 车辆利用率计算
**Given** 一个完成的仿真
**And** 车辆状态历史（带时间戳）
**When** 计算车辆利用率（时间加权平均）
**Then** 应返回每辆车的利用率:
```java
// 示例计算:
// 状态序列: IDLE[0-10s] + MOVING[10-30s] + LOADING[30-35s] + MOVING[35-55s] + IDLE[55-60s]
// 工作时间 = (30-10) + (55-35) = 40s
// 总时间 = 60s
// 利用率 = 40/60 = 0.667
```
**And** 应返回平均利用率
**And** 应返回利用率分布（低、中、高利用率车辆数）

#### Scenario: 设备利用率计算
**Given** Stocker、Machine 等设备
**And** 设备状态历史（带时间戳）
**When** 计算设备利用率（时间加权平均）
**Then** 应返回每个设备的利用率
**And** 应识别利用率最高和最低的设备

#### Scenario: 路段利用率计算
**Given** 一个路网
**And** 车辆通行记录（时间戳）
**When** 计算路段利用率
**Then** 应返回每段路径的使用频率
**And** 应识别拥堵路段（按时间加权占用率）

#### Scenario: 实时利用率监控
**Given** 运行中的仿真
**When** 查询当前利用率
**Then** 应返回当前时刻的瞬时利用率
**And** 应返回最近一段时间（如最近 300 仿真秒）的时间加权平均利用率

---

### Requirement: REQ-KPI-004 EnergyCalculator 能耗计算

系统MUST实现REQ-KPI-004（EnergyCalculator 能耗计算）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 计算系统能耗

#### Scenario: 车辆能耗
**Given** 车辆移动记录
**And** 能耗模型 (距离/速度 → 能耗)
**When** 计算车辆能耗
**Then** 应返回每辆车的总能耗
**And** 应返回单位距离平均能耗

#### Scenario: 设备能耗
**Given** Stocker、Conveyor 等设备
**And** 设备运行时间
**When** 计算设备能耗
**Then** 应返回每个设备的能耗
**And** 应返回总能耗

#### Scenario: 能效分析
**Given** 吞吐量和能耗数据
**When** 计算能效
**Then** 应返回单位任务能耗 (能耗 / 任务数)
**And** 应返回单位物料能耗

---

### Requirement: REQ-KPI-005 BottleneckAnalyzer 瓶颈分析

系统MUST实现REQ-KPI-005（BottleneckAnalyzer 瓶颈分析）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 识别系统瓶颈

#### Scenario: 资源瓶颈识别
**Given** 利用率数据
**When** 分析瓶颈
**Then** 应识别利用率最高的资源
**And** 应返回按利用率排序的资源列表
**And** 高利用率资源应被标记为瓶颈

#### Scenario: 路径瓶颈识别
**Given** 路段通行数据
**And** 等待时间数据
**When** 分析瓶颈
**Then** 应识别等待时间最长的控制点
**And** 应识别流量最大的路段
**And** 应返回瓶颈报告

#### Scenario: WIP 瓶颈分析
**Given** 各位置的在制品 (WIP) 数据
**When** 分析 WIP 瓶颈
**Then** 应识别 WIP 积压最严重的位置
**And** 应分析 WIP 波动趋势

---

### Requirement: REQ-KPI-006 VehicleCountEvaluator 车辆数评估

系统MUST实现REQ-KPI-006（VehicleCountEvaluator 车辆数评估）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 评估最优车辆配置

**吞吐需求定义** (多维度指标):

1. **任务完成率** (Task Completion Rate):
   - 定义: 每仿真小时完成的运输任务数量
   - 目标示例: ≥ 100 任务/小时
   - 允许误差: ±5%

2. **物料吞吐率** (Material Throughput Rate):
   - 定义: 每仿真小时运输的物料总量 (单位: 吨/件/FOUP)
   - 目标示例: ≥ 500 FOUP/小时
   - 允许误差: ±5%

3. **设备服务率** (Equipment Service Rate):
   - 定义: 工位物料需求在规定时间内得到响应的比例
   - 目标示例: 95% 任务在 60 秒内响应
   - 允许误差: 响应时间 ±10 秒

4. **任务延迟率** (Task Delay Rate):
   - 定义: 超过 deadline 的任务比例
   - 目标示例: ≤ 5% 任务超期
   - 允许误差: ±2%

5. **综合吞吐指数** (Composite Throughput Index):
   - 定义: 加权综合上述指标的评分 (0-100)
   - 目标示例: ≥ 80 分
   - 权重: 任务完成率 40% + 物料吞吐率 30% + 设备服务率 20% + 延迟率 10%

#### Scenario: 最少车辆数计算
**Given** 场景吞吐需求 (定义上述指标)
**And** 车辆配置 (初始猜测)
**And** 允许误差范围
**When** 运行评估
**Then** 应返回满足所有需求的最少车辆数
**And** 应返回对应配置下的各指标实际值
**And** 应返回置信区间 (运行 N 次仿真的统计值)

#### Scenario: 车辆数-吞吐量曲线
**Given** 不同车辆数配置
**When** 运行多次仿真
**Then** 应生成车辆数 vs 各项吞吐指标的曲线
**And** 应识别边际收益递减点 (增加车辆带来的收益 < 5%)
**And** 应返回每项指标的达成情况

#### Scenario: 成本效益分析
**Given** 车辆成本模型 (购置/运营成本)
**And** 吞吐量收益模型
**When** 计算最优配置
**Then** 应返回净效益最高的车辆数
**And** 应返回投资回报率 (ROI)
**And** 应返回盈亏平衡点

#### Scenario: 多目标优化
**Given** 相互冲突的指标 (如成本 vs 响应时间)
**When** 运行帕累托优化
**Then** 应返回帕累托前沿解集
**And** 用户可选择偏好的配置点

---

### Requirement: REQ-KPI-007 KPIReport 报表生成

系统MUST实现REQ-KPI-007（KPIReport 报表生成）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 生成 KPI 报表

#### Scenario: 汇总报表
**Given** 完成的仿真
**When** 生成汇总报表
**Then** 报表应包含:
  - 总任务数
  - 吞吐量
  - 平均完成时间
  - 资源利用率
  - 总能耗
  - 瓶颈识别

#### Scenario: 实时 Dashboard 数据
**Given** 运行中的仿真
**When** 请求 Dashboard 数据
**Then** 应返回当前 KPI 快照
**And** 应包含历史趋势数据
**And** 数据格式应适合前端图表渲染

#### Scenario: 报表导出
**Given** 生成的 KPI 报表
**When** 导出为不同格式
**Then** 应支持 PDF 报告
**And** 应支持 Excel 电子表格
**And** 应支持 JSON 数据

---

### Requirement: REQ-KPI-008 WIPTracker 在制品跟踪

系统MUST实现REQ-KPI-008（WIPTracker 在制品跟踪）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 跟踪在制品数量

#### Scenario: 全局 WIP 跟踪
**Given** 一个运行中的仿真
**When** 物料进入系统
**Then** WIP 计数应增加
**When** 物料离开系统
**Then** WIP 计数应减少

#### Scenario: 位置 WIP 跟踪
**Given** 多个位置 (Stocker、Machine 等)
**When** 物料移动到某位置
**Then** 该位置的 WIP 应增加
**And** 原位置的 WIP 应减少

#### Scenario: WIP 阈值告警
**Given** 配置的 WIP 阈值
**And** WIP 超过阈值
**When** 检测到超阈值
**Then** 应产生告警事件
**And** 应记录超阈值位置和时间

---

## 交叉引用

- 依赖 `logistics-entities` 规范: 需要实体状态数据
- 依赖 `traffic-control` 规范: 需要交通统计
- 依赖 `dispatch-system` 规范: 需要任务完成数据
- 支持 `web-visualization` 规范: 提供 Dashboard 数据
