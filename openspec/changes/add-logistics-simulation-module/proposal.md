# 物流仿真模块提案 (Logistics Simulation Module)

## Change Metadata
- **ID**: `add-logistics-simulation-module`
- **Type**: 新功能模块
- **Status**: 提案阶段
- **Author**: shentw
- **Date**: 2026-02-06
- **Related Specs**:
  - `specs/logistics-entities/spec.md` - 实体行为定义
  - `specs/traffic-control/spec.md` - 交通管制需求
  - `specs/dispatch-system/spec.md` - 调度系统需求
  - `specs/kpi-metrics/spec.md` - KPI 指标需求
  - `specs/web-visualization/spec.md` - Web 可视化需求
  - `specs/workflow-automation/spec.md` - 工艺流程自动化需求（12 个需求）

---

## 1. 背景与问题

### 1.1 业务痛点

半导体制造、自动化立体仓库等工业场景面临以下挑战：

1. **高昂的试错成本**: 实体工厂改造动辄数百万，无法承受物理试错
2. **复杂的调度决策**: OHT/AGV 数量评估、路径规划、交通管制需权衡吞吐、效率、成本
3. **缺乏国产自主方案**: FlexSim 等国外工具价格高昂且定制受限，本土化支持不足
4. **仿真与可视化脱节**: 现有方案偏重计算，缺乏直观的 3D 可视化与实时交互

### 1.2 对标 FlexSim 的差距

通过深度调研 FlexSim 平台（见第 9 节调研结论），现有项目存在以下差距：

| 维度 | FlexSim 能力 | 现有 JSimul 能力 | 差距 |
|------|--------------|------------------|------|
| **实体模型** | AGV、OHT、Conveyor、Stocker、Operator 等 | 通用 Resource/Store/Container | 缺少半导体专用实体 |
| **3D 可视化** | 拖拽式场景编辑、实时仿真动画 | 无可视化层 | 需从零构建 |
| **交通管制** | AGV 冲突避让、控制点、控制区、优先级 | 无交通控制逻辑 | 需完整实现 |
| **调度系统** | Process Flow 可视化编程、任务分发 | 基础事件调度 | 需增强调度引擎 |
| **KPI 报表** | 内置 Dashboard、Chart、统计输出 | 无报表系统 | 需构建数据分析层 |
| **二次开发** | Python/C++ 集成、Module SDK | Java 扩展点 | 需设计扩展架构 |

### 1.3 解决方案定位

基于现有 JSimul 离散事件仿真核心，新增**物流仿真模块**，提供：

- **后端仿真引擎**: 扩展 JSimul，支持 AGV/OHT、Stocker、Conveyor 等实体及交通控制
- **前端可视化层**: Three.js 实现 3D 场景编辑、实时仿真动画、KPI Dashboard
- **调度与交通控制**: 冲突避让、路径规划、优先级调度、实时派单

---

## 2. 需求范围与非目标

### 2.1 需求范围

#### 2.1.1 核心实体模型
- **OHT (Overhead Hoist Transport)**: 天车运输，轨道路径、起停控制
- **AGV (Automated Guided Vehicle)**: 自动导引车，路径网络、充电逻辑
- **Stocker**: 自动化立体仓库，入库/出库/库位管理
- **E-Rack**: 机架/缓存区，物料暂存
- **Manual Station**: 人工工位，操作员模型
- **Conveyor**: 输送线，分段控制、积放逻辑
- **Human**: 操作员，技能模型、班次管理

#### 2.1.2 仿真能力
- **产能仿真**: 吞吐量、WIP、瓶颈分析
- **能效仿真**: 设备能耗、车辆能耗评估
- **最少车辆数评估**: 基于吞吐需求计算最优 AGV/OHT 配置

#### 2.1.3 前端可视化（8 大模块）

| 模块 | 功能 |
|------|------|
| **1. 项目/场景管理** | 场景 CRUD、导入导出 JSON、草稿自动保存（1 分钟） |
| **2. 模型与组件库** | GLB 上传 MinIO、版本管理、设备类型映射、缩放/旋转配置 |
| **3. 场景编辑器** | OHT 轨道绘制（Bezier）、SafetyZone 绘制（CIRCLE/RECT/POLYGON）、工序配置（多选设备 + 运输类型校验） |
| **4. 仿真运行** | 启动/暂停/步进/重置、倍速控制、时间显示、实时可视化 |
| **5. KPI 与分析** | 2D 俯视热力图、任务追踪、回放、瓶颈报告 |
| **6. 任务管理** | 任务列表（自动 + 手动）、优先级调整、紧急任务插入 |
| **7. 配置中心** | 全局配置（system_config）、调度权重（distance/time/wip）、日志级别 |
| **8. 日志与事件** | 事件流查看（WebSocket）、筛选搜索、错误提示 |

**技术栈**: React 18 + Vite + @react-three/fiber + @react-three/drei + Tailwind CSS + Chart.js

**3D场景编辑器架构** (基于调研决策):

> 调研结论：选择**自建3D场景编辑器**而非使用现成方案（vis-three AGPL-3.0限制、react-three-editor仅面向开发阶段、triplex是VS Code扩展）。

```
┌─────────────────────────────────────────────────────────────────┐
│                    推荐前端3D技术架构                             │
├─────────────────────────────────────────────────────────────────┤
│  渲染引擎：  three.js (v0.160+)                                  │
│  React封装： @react-three/fiber (v8.x) + @react-three/drei (v9.x)│
│  状态管理：  Zustand (轻量级，适合3D场景状态)                      │
│  模型加载：  useGLTF (drei内置，支持GLB)                          │
│  场景控制：  OrbitControls / TransformControls (drei内置)        │
│  序列化：    JSON → 后端存储                                      │
└─────────────────────────────────────────────────────────────────┘
```

**核心依赖 (均为MIT许可证)**:
```json
{
  "three": "^0.160.0",
  "@react-three/fiber": "^8.15.0",
  "@react-three/drei": "^9.88.0",
  "zustand": "^4.4.0",
  "@types/three": "^0.160.0"
}
```

**选型理由**:
1. **MIT许可证** - 无AGPL-3.0商业限制，适合闭源商业项目
2. **React原生生态** - 与现有技术栈无缝集成
3. **社区活跃** - pmndrs团队持续维护，Star数20k+
4. **灵活可控** - 可按需扩展，无框架绑定
5. **GLB原生支持** - drei内置useGLTF，无需额外依赖

#### 2.1.4 后端控制
- **AGV/OHT 交通管制**: 冲突避让、路径规划、优先级、分区控制
- **调度引擎**: 规则引擎、任务分配、动态重规划

### 2.2 非目标

- **不实现物理引擎**: 不模拟真实物理碰撞（使用逻辑碰撞检测）
- **不实现 PLC 连接**: 本阶段不连接真实设备，仅仿真环境
- **不实现 AI 优化**: 不包含遗传算法、强化学习等高级优化（预留接口）
- **不支持分布式仿真**: 单机仿真即可（不跨节点）

---

## 3. 模块架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     Frontend Layer (Web)                        │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────┐  │
│  │ Scene Editor  │  │ 3D Viewer     │  │ KPI Dashboard       │  │
│  │ (Drag-Drop)   │  │ (Three.js)    │  │ (Charts/Reports)    │  │
│  └───────────────┘  └───────────────┘  └─────────────────────┘  │
└───────────────────────────────────────┬─────────────────────────┘
                                        │ WebSocket / REST API
┌───────────────────────────────────────┴─────────────────────────┐
│                     Backend Layer (Java)                         │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              Logistics Simulation Module                     ││
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐   ││
│  │  │   Entities  │ │    Control  │ │     Scheduler       │   ││
│  │  │ OHT/AGV/... │ │Traffic Mgmt │ │  Dispatch Engine    │   ││
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘   ││
│  └─────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    JSimul Core (Existing)                   ││
│  │  Environment / Event / Process / Resource / Store           ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 模块划分

#### 3.2.1 sim-logistics-core (新增)
**职责**: 核心实体模型定义
- 包路径: `com.semi.simlogistics.core`
- 主要类:
  - `LogisticsEntity`: 实体基类
  - `Vehicle`: 车辆抽象 (OHT/AGV 基类)
  - `OHTVehicle`, `AGVVehicle`
  - `Stocker`, `ERack`, `ManualStation`
  - `Conveyor`, `ConveyorSegment`
  - `Operator`: 操作员模型

#### 3.2.2 sim-logistics-control (新增)
**职责**: 交通管制与路径管理
- 包路径: `com.semi.simlogistics.control`
- 主要类:
  - `TrafficManager`: 交通管制器
  - `PathPlanner`: 路径规划器
  - `ConflictResolver`: 冲突解决器
  - `ControlPoint`: 控制点
  - `ControlArea`: 控制区
  - `PriorityManager`: 优先级管理

#### 3.2.3 sim-logistics-scheduler (新增)
**职责**: 调度与任务分发
- 包路径: `com.semi.simlogistics.scheduler`
- 主要类:
  - `DispatchEngine`: 调度引擎
  - `TaskQueue`: 任务队列
  - `DispatchRule`: 派发规则
  - `RealTimeDispatcher`: 实时派单器

#### 3.2.4 sim-logistics-metrics (新增)
**职责**: KPI 收集与报表
- 包路径: `com.semi.simlogistics.metrics`
- 主要类:
  - `MetricsCollector`: 指标收集器
  - `ThroughputCalculator`: 吞吐量计算
  - `EnergyCalculator`: 能耗计算
  - `BottleneckAnalyzer`: 瓶颈分析器
  - `VehicleCountEvaluator`: 车辆数评估器

#### 3.2.5 sim-logistics-web (新增)
**职责**: Web 服务与 API
- 包路径: `com.semi.simlogistics.web`
- 主要类:
  - `SimulationController`: 仿真 REST API
  - `WebSocketHandler`: 实时数据推送
  - `SceneDTO`: 场景数据传输对象

#### 3.2.6 frontend (新增，独立目录)
**职责**: 前端可视化
- 技术栈: React 18 + Vite + three.js + @react-three/fiber + @react-three/drei + Tailwind CSS
- 主要模块:
  - `SceneEditor`: 场景编辑器
  - `SimulationViewer`: 3D 仿真查看器
  - `KPIDashboard`: KPI 仪表板

#### 3.3 接口规范摘要

##### 3.3.1 REST API

**基础路径**: `/api/v1`

**核心 API 端点**:

| 类别 | 端点 | 说明 |
|-----|------|------|
| **仿真控制** | POST /simulations | 创建仿真 |
| | GET /simulations | 获取仿真列表 |
| | GET /simulations/{id} | 获取仿真状态 |
| | PUT /simulations/{id}/start | 启动仿真 |
| | PUT /simulations/{id}/pause | 暂停仿真 |
| | PUT /simulations/{id}/stop | 停止仿真 |
| **任务管理** | GET /simulations/{id}/tasks | 获取任务列表 |
| | POST /simulations/{id}/tasks | 手动添加任务 |
| | DELETE /tasks/{taskId} | 取消任务 |
| **场景管理** | GET /scenes | 获取场景列表 |
| | POST /scenes | 创建场景 |
| | GET /scenes/{id} | 获取场景详情 |
| | PUT /scenes/{id} | 更新场景 |
| | DELETE /scenes/{id} | 删除场景 |
| **实体查询** | GET /scenes/{sceneId}/entities | 获取场景实体 |
| | GET /simulations/{id}/entities | 获取运行时实体状态 |
| **KPI 指标** | GET /simulations/{id}/metrics | 获取仿真指标 |
| | GET /simulations/{id}/report | 生成报表 |

**通用响应格式**:
```json
{
  "success": true,
  "data": {...},
  "error": null,
  "timestamp": "2026-02-06T10:00:00Z"
}
```

**错误响应格式**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ENTITY_NOT_FOUND",
    "message": "Entity not found",
    "details": {"entityId": "MACHINE-01"}
  },
  "timestamp": "2026-02-06T10:00:00Z"
}
```

##### 3.3.2 WebSocket 协议

**连接端点**: `ws://host/api/v1/simulations/{simulationId}/ws`

**核心消息类型**:

| 方向 | 消息类型 | 说明 |
|-----|---------|------|
| 客户端→服务器 | SUBSCRIBE | 订阅实体状态更新 |
| | ADD_TASK | 添加任务 |
| | SET_SPEED | 设置仿真速度 |
| | PAUSE/RESUME | 暂停/继续仿真 |
| 服务器→客户端 | SIMULATION_STARTED | 仿真已启动 |
| | SIMULATION_COMPLETED | 仿真已完成 |
| | ENTITY_UPDATE | 实体状态更新 |
| | TASK_ASSIGNED | 任务已分配 |
| | TASK_COMPLETED | 任务已完成 |
| | METRICS_UPDATE | 指标更新 |

**推送策略**:
- 纯 DES 模式 (timeScale > 10x): 批量推送
- 实时模式 (timeScale ≈ 1x): 即时推送

详细 API 规范见 `design.md` 第 4.3 节。

---

## 4. 核心实体与模型定义

### 4.1 实体继承层次

```
LogisticsEntity (abstract)
├── FixedLocation (abstract)
│   ├── Stocker
│   ├── ERack
│   └── ManualStation
├── MovableEntity (abstract)
│   ├── Vehicle (abstract)
│   │   ├── OHTVehicle
│   │   └── AGVVehicle
│   └── Operator
└── TransportPath (abstract)
    └── Conveyor
```

### 4.2 核心实体定义

#### 4.2.1 OHTVehicle (天车)
```java
/**
 * Overhead Hoist Transport vehicle for semiconductor AMHS.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class OHTVehicle extends Vehicle {
    private String trackId;           // 所属轨道 ID
    private double maxLoad;           // 最大载重
    private double currentLoad;       // 当前载重
    private OHTState state;           // 状态 (IDLE, MOVING, LOADING, UNLOADING, CHARGING)
    private List<String> path;        // 当前路径
    private int currentPathIndex;     // 路径索引
}
```

#### 4.2.2 AGVVehicle (自动导引车)
```java
/**
 * Automated Guided Vehicle for floor-level transport.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class AGVVehicle extends Vehicle {
    private String networkId;         // 所属路网 ID
    private double batteryLevel;      // 电池电量 (0-1)
    private double chargingRate;      // 充电速率
    private boolean isCharging;       // 是否充电中
    private AGVType type;             // 类型 (FORKLIFT, TUGGER, etc.)
}
```

#### 4.2.3 Stocker (立体仓库)
```java
/**
 * Automated Storage and Retrieval System.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class Stocker extends FixedLocation {
    private int capacity;             // 总容量
    private int usedCapacity;         // 已用容量
    private List<StorageSlot> slots;  // 库位列表
    private int cranesCount;          // 堆垛机数量
    private double throughput;        // 吞吐量 (次/小时)
}
```

#### 4.2.4 Conveyor (输送线)
```java
/**
 * Conveyor system for material transport.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class Conveyor extends TransportPath {
    private List<ConveyorSegment> segments;  // 分段列表
    private double speed;                    // 速度 (m/s)
    private AccumulationMode accumulation;   // 积放模式
}
```

### 4.3 交通控制模型

#### 4.3.1 ControlPoint (控制点)
```java
/**
 * Control point for traffic management.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class ControlPoint {
    private String id;
    private Position position;
    private int capacity;              // 允许同时进入的车辆数
    private List<String> currentVehicles;
    private Priority priority;         // 默认优先级
}
```

#### 4.3.2 ControlArea (控制区)
```java
/**
 * Control area for zoning.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-06
 */
public class ControlArea {
    private String id;
    private List<ControlPoint> points;
    private int maxVehicles;
    private List<String> activeVehicles;
}
```

#### 4.3.3 路网与路径约束

**约束**:
- **双向路段必须拆成两条有向边**（方向相反、edgeId 独立）
- **路径段类型**：`LINEAR` / `BEZIER`
- **BEZIER 段结构**：`from`, `to`, `c1`, `c2`
  - `from/to` 引用 `points` 中的点 ID
  - `c1/c2` 为绝对坐标（单位 m）
- **单位规范**：
  - 坐标单位：米（m）
  - 角度单位：弧度，范围 [-π, π]

#### 4.3.4 交通配置管理

**约束**:
- 交通管制相关配置由 `system_config` 统一管理
- **配置变更立即生效**（无需重启）

---

## 5. 关键场景与验收标准

### 5.1 场景 1: OHT 运输仿真

**描述**: 半导体工厂中，OHT 在 Bay 和 Chute 之间运输晶圆盒

**流程**:
1. OHT 在 Stocker 接收物料
2. 规划路径到目标 Machine
3. 通过轨道网络行驶（避免冲突）
4. 在目标点卸载
5. 返回待命点

**验收标准**:
- [ ] OHT 可沿预定轨道移动
- [ ] 两辆 OHT 在控制点处自动避让
- [ ] 统计运输次数、平均等待时间
- [ ] 3D 可视化展示实时位置

### 5.2 场景 2: AGV 最优数量评估

**描述**: 给定吞吐需求，计算最少 AGV 数量

**流程**:
1. 设置任务队列（100 次运输/小时）
2. 从 5 辆 AGV 开始仿真
3. 逐步增加直到满足吞吐
4. 输出最优配置

**验收标准**:
- [ ] 输出最少 AGV 数量
- [ ] 输出利用率分布
- [ ] 识别瓶颈路径

### 5.3 场景 3: 实时派单

**描述**: 运行中动态分配新任务

**流程**:
1. 仿真运行中
2. 用户通过 Web 界面添加紧急任务
3. 调度引擎根据规则分配车辆
4. 车辆改变路径执行新任务

**验收标准**:
- [ ] Web 界面可添加任务
- [ ] 车辆实时响应
- [ ] 优先级任务优先执行

---

## 6. TDD 研发流程约束

### 6.1 测试优先原则

1. **先写测试，后写实现**: 每个功能必须先有失败的测试
2. **单测覆盖率**: 核心代码覆盖率 ≥ 80%
3. **集成测试**: 端到端场景必须有集成测试

### 6.2 测试结构

```
sim-logistics-test (新增模块)
├── src/test/java/
│   ├── unit/           # 单元测试
│   ├── integration/    # 集成测试
│   └── scenario/       # 场景测试
```

### 6.3 测试示例

```java
@Test
void testOHTConflictAvoidance() {
    // Given
    Environment env = new Environment();
    OHTVehicle oht1 = new OHTVehicle("OHT1");
    OHTVehicle oht2 = new OHTVehicle("OHT2");
    ControlPoint cp = new ControlPoint("CP1", 1); // 容量 1

    // When
    env.process(oht1.moveTo(cp));
    env.process(oht2.moveTo(cp));
    env.run(100);

    // Then
    assertThat(oht1.hasPassed(cp)).isTrue();
    assertThat(oht2.hasPassed(cp)).isFalse();
    assertThat(oht2.getWaitTime()).isGreaterThan(0);
}
```

---

## 7. 里程碑与任务拆分

### 里程碑 1: 核心实体与基础仿真 (Phase 1)
**目标**: 实现基本实体模型和简单仿真

**任务**:
1. 设计实体类层次结构
2. 实现 OHTVehicle、AGVVehicle
3. 实现 Stocker、ERack
4. 编写单元测试
5. 创建简单场景测试（OHT 从 A 到 B）

### 里程碑 2: 交通控制系统 (Phase 2)
**目标**: 实现 AGV/OHT 冲突避让

**任务**:
1. 实现 ControlPoint、ControlArea
2. 实现 ConflictResolver
3. 实现路径规划算法
4. 集成测试：多车冲突场景

### 里程碑 3: 调度引擎 (Phase 3)
**目标**: 实现任务调度与分发

**任务**:
1. 实现 TaskQueue、DispatchEngine
2. 实现调度规则（最短距离、最高优先级）
3. 实现动态重规划
4. 集成测试：复杂调度场景

### 里程碑 4: KPI 与分析 (Phase 4)
**目标**: 实现指标收集与分析

**任务**:
1. 实现 MetricsCollector
2. 实现吞吐量、能耗计算
3. 实现车辆数评估器
4. 导出报表功能

### 里程碑 5: 前端可视化 (Phase 5)
**目标**: 实现 Web 可视化界面（基于 @react-three/fiber + drei）

**任务**:
1. 设计 REST API
2. 实现 WebSocket 推送
3. 搭建前端3D架构（three.js + @react-three/fiber + @react-three/drei）
4. 实现 GLB 模型加载（useGLTF）
5. 自建拖拽式场景编辑器（TransformControls、Zustand状态管理）
6. KPI Dashboard

### 里程碑 6: 实时派单界面 (Phase 6)
**目标**: 实现实时交互功能

**任务**:
1. 实时任务添加接口
2. 车辆状态监控界面
3. 手动干预功能

---

## 8. 风险与依赖

### 8.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| **3D 可视化性能** | 大规模场景渲染卡顿 | 使用 LOD、实例化渲染，限制实体数量 |
| **路径规划复杂度** | 计算时间过长 | 预计算路径网格，使用缓存 |
| **并发控制** | 多车冲突死锁 | 超时机制、回退策略 |
| **内存占用** | 长时间仿真 OOM | 定期清理历史数据，流式处理 |

### 8.2 依赖关系

- **JSimul 核心模块**: 需确认 Environment、Process 等类支持物流场景扩展
- **3D技术栈**: 已确定使用 three.js + @react-three/fiber + @react-three/drei (均为MIT许可证)
- **后端 Web 框架**: 需选定 Spring Boot 或其他框架

### 8.3 人员依赖

- 需熟悉半导体物流场景的专家提供需求
- 需前端工程师配合 React Three Fiber 开发

---

## 9. 调研结论与引用

### 9.1 FlexSim 核心能力调研

通过深度调研 FlexSim 官方文档，确认以下核心能力：

#### 9.1.1 物料搬运仿真
FlexSim 提供完整的物料搬运仿真能力，包括：
- **拖拽式 3D 建模**: 直观的可视化环境
- **AGV 模块**: 完整的 AGV 网络建模能力
- **输送线系统**: 支持复杂输送线网络
- **实时仿真动画**: 精确的时间同步展示

**来源**: [FlexSim Material Handling Simulation](https://www.flexsim.com/material-handling-simulation/)

#### 9.1.2 仓储仿真
FlexSim 仓储模块支持：
- **立体仓库 (ASRS)**: 自动化存取系统建模
- **库存管理**: 实时库存跟踪
- **吞吐量分析**: 入库/出库性能分析

**来源**: [FlexSim Warehousing Simulation](https://www.flexsim.com/warehousing-simulation/)

#### 9.1.3 AGV 交通管制
FlexSim AGV 模块的核心能力：
- **Control Points (控制点)**: 限制特定区域的车辆数量
- **Control Areas (控制区)**: 定义大范围的交通管制区域
- **Collision Avoidance (冲突避让)**: 自动检测并避免车辆碰撞
- **Path Planning (路径规划)**: 自动计算最优路径
- **Priority Management (优先级管理)**: 基于优先级的交通控制

**来源**: [FlexSim AGV Network Logic](https://docs.flexsim.com/en/23.1/WorkingWithTasks/AGVNetworks/BuildingAGVLogic/BuildingAGVLogic.html)

#### 9.1.4 KPI 与报表
FlexSim 内置数据分析功能：
- **Dashboard**: 实时仪表板
- **Charts**: 多种图表类型
- **Statistics Output**: 统计数据导出

**来源**: FlexSim 24.1 官方文档

#### 9.1.5 二次开发能力
FlexSim 支持扩展开发：
- **Python 集成**: 可调用 Python 代码
- **C/C++ 集成**: 高性能计算支持
- **Module SDK**: 自定义模块开发

**来源**: [FlexSim Connecting to External Code](https://docs.flexsim.com/en/24.1/Reference/DeveloperAdvancedUser/ConnectingToExternalCode/ConnectingToExternalCode.html)

### 9.2 对标结论

基于调研，本项目需实现以下对等功能：

| FlexSim 功能 | 本项目实现方案 |
|--------------|----------------|
| 拖拽式 3D 建模 | Three.js + React 场景编辑器 |
| AGV 模块 | sim-logistics-control 交通管制 |
| 输送线系统 | Conveyor 实体 + 分段控制 |
| Control Point | ControlPoint 类 |
| Control Area | ControlArea 类 |
| 冲突避让 | ConflictResolver 算法 |
| 路径规划 | PathPlanner (A* / Dijkstra) |
| Dashboard | KPI Dashboard (React + Chart.js) |
| Python 集成 | 预留脚本接口 (JavaScript/GraalVM) |

### 9.3 差异化优势

相比 FlexSim，本项目的优势：
- **开源免费**: 降低企业成本
- **本土化**: 中文界面、国内技术支持
- **Web 架构**: 无需安装客户端，支持协同
- **Java 生态**: 方便集成现有 MES/ERP 系统

### 9.4 参考链接汇总

**FlexSim 调研**:
1. [FlexSim Material Handling Simulation](https://www.flexsim.com/material-handling-simulation/)
2. [FlexSim Warehousing Simulation](https://www.flexsim.com/warehousing-simulation/)
3. [FlexSim AGV Network Logic](https://docs.flexsim.com/en/23.1/WorkingWithTasks/AGVNetworks/BuildingAGVLogic/BuildingAGVLogic.html)
4. [FlexSim Connecting to External Code](https://docs.flexsim.com/en/24.1/Reference/DeveloperAdvancedUser/ConnectingToExternalCode/ConnectingToExternalCode.html)
5. [FlexSim Documentation](https://docs.flexsim.com/)

**3D场景编辑器技术调研**:
6. [Three.js 官方文档](https://threejs.org/docs/)
7. [Three.js Editor 源码](https://github.com/mrdoob/three.js/tree/dev/editor) - MIT许可证，可参考架构
8. [React Three Fiber 文档](https://docs.pmnd.rs/react-three-fiber) - React声明式3D
9. [@react-three/drei 工具库](https://github.com/pmndrs/drei) - GLTF加载、控制器等
10. [vis-three](https://github.com/vis-three/vis-three) - AGPL-3.0，已排除（商业限制）

---

## 10. 工艺流程自动化模块（sim-logistics-workflow）

### 10.1 模块概述

**新增模块**: `sim-logistics-workflow`

**核心能力**: 复杂工艺流程建模、事件驱动的自动任务生成、人车混合交通管制。

**对标 FlexSim Process Flow**: 提供可编程的流程定义能力，支持 Lists & Queue、任务序列、智能路由。

### 10.2 模块架构

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

### 10.3 职责边界

| 职责项 | Workflow 模块 | Scheduler 模块 | Control 模块 |
|--------|---------------|----------------|--------------|
| **任务生成** | ✅ 负责（自动生成） | ❌ 不参与 | ❌ 不参与 |
| **任务入队** | ✅ 写入 TaskQueue | ❌ 不操作 | ❌ 不操作 |
| **任务派单** | ❌ 不参与 | ✅ 负责 | ❌ 不参与 |
| **交通管制** | ❌ 不参与 | ⚠️ 发起请求 | ✅ 负责 |

### 10.4 关键设计决策

#### 决策 WF-1: SafetyZone 存储方式

**决策**: SafetyZone 作为实体类型存储在 `scenes.entities` JSON 数组中

```json
{
  "entities": [
    {"id": "Machine-A", "type": "MACHINE", ...},
    {"id": "ZONE-FAB1-MAIN", "type": "SAFETY_ZONE", ...}
  ]
}
```

**理由**:
- SafetyZone 数量少（通常几十个）
- 与场景原子性一致
- 前端统一读取，降低复杂度

**拒绝的方案**: 独立 `safety_zones` 表（已标记为已弃用）

#### 决策 WF-2: Human 不占用 ControlPoint/Edge

**决策**: Human 只占用 SafetyZone，不参与 ControlPoint/Edge 的车辆冲突仲裁

**理由**:
- Human 移动速度慢、路径不确定，不适合细粒度路径管制
- 人车冲突统一在 SafetyZone 层面处理
- 简化 ControlPoint/Edge 逻辑，避免人车混合仲裁复杂度

#### 决策 WF-3: 物料级互斥绑定（v1.5）

**决策**: 使用 MaterialBindingManager 确保同一物料同一时刻只绑定到一个工序，使用 `putIfAbsent` 原子操作，**不实现超时强制解绑**

**实现**:
```java
MaterialBindingManager.tryBind(materialId, stepId)
// 使用 putIfAbsent，无 timeout 参数
// 只在流程完成或失败时释放绑定
```

**理由**:
- 防止多流程并行执行时物料被重复分配
- 超时强制解绑可能导致状态不一致
- 绑定只在流程完成或失败时释放，行为可预测

#### 决策 WF-4: 事件驱动 + 可配置重试

**决策**:
- 事件驱动：EventTrigger 监听物料事件，匹配后自动生成任务
- 可配置重试：RetryConfig 支持指数退避、最大重试次数
- 非轮询：不使用定时轮询检查条件

#### 决策 WF-5: 多候选设备选择与运输类型校验（v2.0）

**决策**:
- ProcessStep 支持 `targetEntityIds: List<String>`，允许多候选设备
- 新增 `requiredTransportTypes` 字段：工序要求的运输类型
- 设备类实体新增 `supportedTransportTypes` 字段（必填）
- 建模阶段强制校验：交集为空时禁止保存
- 运行时使用多因子加权算法选择最优设备

**数据结构变更**:

```json
// ProcessFlow definition (v2.0)
{
  "steps": [
    {
      "id": "step-001",
      "name": "光刻",
      "sequence": 1,
      "targetEntityIds": ["Machine-A", "Machine-B", "Machine-C"],  // NEW: 数组
      "requiredTransportTypes": ["OHT", "AGV"],                    // NEW: 运输类型
      "processingTime": {"type": "NORMAL", "mean": 50, "std": 5}
    }
  ],
  "defaultTransportSelector": {
    "policy": "HYBRID",
    "allowedTypes": ["OHT", "AGV"]
  }
}

// Device entity (v2.0)
{
  "id": "Machine-A",
  "type": "MACHINE",
  "position": {"x": 10.0, "y": 20.0, "z": 0.0},
  "supportedTransportTypes": ["OHT", "AGV", "HUMAN"],  // NEW: 必填字段
  "properties": {
    "capacity": 2,
    "processingTime": ...
  }
}
```

**多因子加权选择算法**:

```
Score = w_distance × (1 - normDistance)
      + w_time × (1 - normTime)
      + w_wip × (1 - normWip)

归一化方法: Min-Max
默认权重: distance=0.4, time=0.4, wip=0.2
```

**全局配置**:

```
dispatch.selector.weight.distance = 0.4
dispatch.selector.weight.time = 0.4
dispatch.selector.weight.wip = 0.2
dispatch.selector.normalization = min-max
```

**理由**:
- **灵活性**: 多候选设备提高系统可靠性，避免单点故障
- **早期校验**: 建模阶段发现运输类型不匹配，避免运行时错误
- **全局优化**: 多因子加权综合考虑距离、时间、WIP，选择最优设备
- **可配置**: 支持不同场景调整权重策略

### 10.5 数据存储（v2.0）

#### 10.5.1 process_flows 表

```sql
CREATE TABLE process_flows (
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    id              CHAR(36) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    version         VARCHAR(32) NOT NULL,
    definition      JSON NOT NULL,
    is_template     BOOLEAN DEFAULT FALSE,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),
    parent_version  VARCHAR(32),
    PRIMARY KEY (tenant_id, id, version),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    -- Note: parent_version is a VARCHAR(32) reference for tracking purposes only
    -- Cannot create FK constraint as process_flows PK is composite (tenant_id, id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 10.5.2 process_flow_bindings 表

```sql
CREATE TABLE process_flow_bindings (
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    id              CHAR(36) NOT NULL,
    scene_id        CHAR(36) NOT NULL,
    flow_id         CHAR(36) NOT NULL,
    flow_version    VARCHAR(32) NOT NULL,
    entry_point_id  VARCHAR(100) NOT NULL,
    enabled         BOOLEAN DEFAULT TRUE,
    priority        INT DEFAULT 0,
    trigger_condition JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (tenant_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (tenant_id, scene_id) REFERENCES scenes(tenant_id, id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id, flow_id, flow_version) REFERENCES process_flows(tenant_id, id, version) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 10.6 Related Specs

- `specs/workflow-automation/spec.md` - 完整需求规格说明（12 个需求）

### 10.7 接口规范（REST + WebSocket）

**REST 端点（摘要）**:
```
POST   /api/v1/simulations
POST   /api/v1/simulations/{id}/start|pause|resume|stop
GET    /api/v1/simulations/{id}
GET    /api/v1/simulations/{id}/kpi
POST   /api/v1/scenes
GET    /api/v1/scenes/{id}
POST   /api/v1/scenes/{id}/import
PATCH  /api/v1/entities/{id}
GET    /api/v1/metrics/simulations/{id}/summary
```

**WebSocket（摘要）**:
- 连接：`/api/v1/simulations/{id}/ws`
- 上行：`control.start|pause|resume|stop`、`task.create`、`entity.update`
- 下行：`state.update`、`entity.update`、`kpi.update`、`task.update`、`error`、`heartbeat`

---

## 附录

### A. 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| OHT | Overhead Hoist Transport | 天车，悬挂式运输系统 |
| AGV | Automated Guided Vehicle | 自动导引车 |
| AMHS | Automated Material Handling System | 自动化物料搬运系统 |
| Stocker | ASRS | 自动化立体仓库 |
| WIP | Work In Process | 在制品数量 |
| KPI | Key Performance Indicator | 关键绩效指标 |

### B. 参考资料

- SimPy 官方文档: https://simpy.readthedocs.io/
- Three.js 文档: https://threejs.org/docs/
- Java 21 语言特性: https://openjdk.org/projects/jdk/21/
