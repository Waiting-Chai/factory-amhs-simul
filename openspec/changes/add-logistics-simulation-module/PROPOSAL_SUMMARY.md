# 物流仿真模块提案 - 决策总结

## 文档信息
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Status**: 提案完成，待审批

---

## 1. 决策矩阵

### 1.1 高优先级决策

| 编号 | 决策项 | 选择 | 说明 |
|------|--------|------|------|
| **D1** | 实体模型架构 | 组合优先，允许轻量继承 | 能力接口组合，继承层次≤2层 |
| **D2** | 仿真时钟模型 | 混合可调 | 支持 DES/实时/可变 timeScale |
| **D3** | 吞吐需求定义 | 多维度指标 | 任务+物料+服务率+延迟+综合指数 |
| **D4** | API 契约定义 | Spec 优先 | 在 spec.md 中定义完整契约 |
| **D5** | Spring 依赖 | 仅 web 模块 | 核心模块零依赖 |

### 1.2 架构设计决策

| 编号 | 决策项 | 选择 | 说明 |
|------|--------|------|------|
| **M1** | 路网约束模型 | EdgeConstraint 动态查询 | 解耦约束与拓扑 |
| **M2** | 路径规划与预占 | 分离 | PathPlanner 算路，ReservationManager 预占 |
| **M3** | 任务持久化 | 数据库 | MySQL 8.0.16+ 存储 |
| **M4** | KPI 统计口径 | 双基准+自适应采样 | 仿真时间+墙钟时间，事件+聚合 |
| **M5** | 场景模型映射 | 枚举+固定 schema | type 枚举，properties 固定结构 |
| **M6** | 实体行为接口 | 核心行为在类+扩展能力接口 | Vehicle 组合 Process |
| **M7** | 3D 性能目标 | 分级 | 500实体 60FPS，1000实体 30FPS |
| **M8** | 路径缓存策略 | LRU 10k，事件失效 | (from, to, vehicleType) 缓存键 |
| **M9** | 实体类型判别 | 枚举+能力接口混合 | 核心类型稳定，能力可扩展 |
| **M10** | 事件总线机制 | 自定义 Observer | 零依赖，与 JSimul Event 区分 |

### 1.3 数据库决策

| 编号 | 决策项 | 选择 | 说明 |
|------|--------|------|------|
| **DB-1** | 场景版本管理 | 自动迁移 | schema_version 自动升级，保留原副本 |
| **DB-2** | 运行时状态 | 不落库，暂停时快照 | result_snapshot 字段 |
| **DB-3** | 任务保留期 | 3 个月归档 | 定期归档到 tasks_archive |
| **DB-4** | KPI 采样 | 自适应 | 关键事件触发 + 60秒聚合 |
| **DB-5** | 状态历史 | 仅关键实体 | 只记录车辆的关键状态变化 |
| **DB-6** | 事件日志级别 | 可配置 | 默认 INFO+WARNING+ERROR |
| **DB-7** | 多租户 | 预留 | tenant_id 字段，默认 'default' |
| **DB-8** | 审计日志 | 需要 | 记录关键操作 |
| **DB-9** | 文件存储 | MinIO | 模型/报表/导出文件全部存 MinIO |
| **DB-10** | 连接池 | 保守可配置 | max=20, min=5 |

### 1.4 前端/交互决策

| 编号 | 决策项 | 选择 | 说明 |
|------|--------|------|------|
| **S1** | JSON Schema | 分离文件 | 每种实体单独 schema 文件 |
| **S2** | 任务序列化 | 避免循环引用 | 只存 ID，关联对象查询获取 |
| **S3** | 编辑器验证 | 前端+后端 | 前端即时，后端完整 |
| **S4** | 缓存失效 | 保守策略 | 清空所有缓存 |
| **S5** | 3D 模型 | GLB 模型来自 MinIO，无模型时使用占位模型（fallback primitive） | 三层回退策略：绑定 > 默认 > 占位 |
| **S6** | 消息顺序 | 序列号+时间戳 | 前端排序处理乱序 |
| **S7** | 暂停推送 | 仅用户操作 | 暂停期间只推送人工操作+心跳 |
| **S8** | 实时派单延迟 | ≤1s 仿真时间 | 调度周期 0.5-1s |
| **S9** | 内存管理 | 可选详细程度 | historyLevel=NONE\|KEY\|FULL |
| **S10** | 并发编辑 | 乐观锁 | 版本号机制，冲突时返回差异 |
| **S11** | 模型管理 | GLB + MinIO | 所有可视化实体需模型库管理 |
| **S12** | 模型绑定 | 多版本 + 实体级绑定 | 同类型实体可使用不同模型 |
| **S13** | 可视化展示 | 3D + 状态画板 + 2D 热力图 | OHT 轨道与 AGV 路网分层渲染 |
| **S14** | 场景导入 | 支持 JSON 导入 | 新建场景并生成新版本 |
| **S15** | 场景草稿 | 1 分钟自动保存 | 有变化才保存 |
| **S16** | 模型渲染回退 | 绑定 > 默认 > 占位 | 无模型时使用占位渲染 |
| **S17** | 概率分布模型 | Normal/LogNormal/Exponential/Triangular | 故障/维修/加工时长采用分布驱动 |
| **S18** | 配置中心 | system_config 表 | 全局/租户级配置落库管理 |
| **S19** | 配置覆盖范围 | 交通/调度/仿真/模型/前端/草稿/分布 | 统一配置入口维护 |
| **S20** | 配置页面 | 车辆/日志/模型版本配置 | 前端提供独立配置页面 |
| **S21** | 设备选择策略 | SelectionPolicy = DISTANCE_FIRST / TIME_FIRST / WIP_FIRST / PRIORITY_BASED / HYBRID | 权重来自 system_config：dispatch.selector.weight.distance=0.4, time=0.4, wip=0.2 |

---

## 2. 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                     Frontend (React + Three.js)                  │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────────┐  │
│  │ Scene Editor  │  │ 3D Viewer     │  │ KPI Dashboard       │  │
│  │ (拖拽式编辑)   │  │ (实时动画)    │  │ (图表+报表)         │  │
│  └───────────────┘  └───────────────┘  └─────────────────────┘  │
└───────────────────────────────────────┬─────────────────────────┘
                                        │ WebSocket + REST API
┌───────────────────────────────────────┴─────────────────────────┐
│                     Backend (Java)                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              sim-logistics-web (Spring Boot 3.x)           │  │
│  │  REST Controllers, WebSocket Handler, DTOs               │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              sim-logistics-core                           │  │
│  │  LogisticsEntity, Vehicle, OHTVehicle, AGVVehicle         │  │
│  │  Stocker, Conveyor, Operator, Capability                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              sim-logistics-control                        │  │
│  │  EdgeConstraint, PathPlanner, ReservationManager         │  │
│  │  ControlPoint, ControlArea, ConflictResolver             │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              sim-logistics-scheduler                      │  │
│  │  DispatchEngine, TaskQueue, DispatchRule                 │  │
│  │  RealTimeDispatcher, Task                               │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              sim-logistics-metrics                        │  │
│  │  MetricsCollector, ThroughputCalculator                 │  │
│  │  VehicleCountEvaluator, BottleneckAnalyzer              │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    JSimul (现有)                           │  │
│  │  Environment, Event, Process, Resource, Store            │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────┬─────────────────────────┘
                                        │ JDBC
┌───────────────────────────────────────┴─────────────────────────┐
│              MySQL 8.0.16+ (主数据库)                            │
│  scenes, simulations, tasks, kpi_metrics, event_log, ...        │
└───────────────────────────────────────┬─────────────────────────┘
                                        │ Redis Protocol
┌───────────────────────────────────────┴─────────────────────────┐
│              Redis 7.0+ + Redisson (缓存专用)                    │
│  路径缓存, 会话缓存, 分布式锁, 发布订阅, 限流                      │
└───────────────────────────────────────┬─────────────────────────┘
                                        │ S3 Protocol
┌───────────────────────────────────────┴─────────────────────────┐
│              MinIO (对象存储)                                   │
│  报表文件, 3D 模型, 导出文件                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心实体定义

### 3.1 实体层次结构

```
LogisticsEntity (abstract)
├── EntityType: OHT_VEHICLE, AGV_VEHICLE, STOCKER, ...
├── Capabilities: Map<Class<?>, Capability>
├── Process process (组合，非继承)
│
├── Vehicle (abstract) extends LogisticsEntity
│   ├── TransportCapability transport
│   ├── BatteryCapability battery (可选)
│   └── TrackMovement/NetworkMovement
│       ├── OHTVehicle extends Vehicle
│       └── AGVVehicle extends Vehicle
│
├── Stocker extends LogisticsEntity
├── ERack extends LogisticsEntity
├── ManualStation extends LogisticsEntity
├── Conveyor extends LogisticsEntity
└── Operator extends LogisticsEntity
```

### 3.2 核心能力接口

```java
// 运输能力
public interface TransportCapability {
    void move(Position destination);
    void load(Cargo cargo);
    void unload();
}

// 电池能力
public interface BatteryCapability {
    double getBatteryLevel();
    void charge(double amount);
    boolean isCharging();
}

// 冲突感知能力
public interface ConflictAware {
    boolean checkConflict(Position position);
    void reserveControlPoint(String cpId);
    void releaseControlPoint(String cpId);
}
```

---

## 4. 数据库表清单

| 表名 | 用途 | 关键字段 | MySQL 类型 |
|------|------|----------|-----------|
| tenants | 租户 | id, name, status, settings | CHAR(36), JSON |
| scenes | 场景定义 | id, tenant_id, name, version, schema_version, definition | CHAR(36), JSON |
| scene_model_versions | 场景模型版本 | version, schema_definition, migration_script | INT, JSON |
| scene_drafts | 场景草稿 | id, scene_id, content, updated_at | CHAR(36), JSON |
| system_config | 系统配置 | id, tenant_id, config_key, config_value | CHAR(36), JSON |
| simulations | 仿真运行 | id, tenant_id, scene_id, config, status, result_snapshot | CHAR(36), JSON |
| tasks | 任务 | id, tenant_id, simulation_id, type, priority, status | CHAR(36), JSON |
| tasks_archive | 任务归档 | (同 tasks，3 个月后) | CHAR(36), JSON |
| kpi_metrics | KPI 指标 | id, tenant_id, simulation_id, recorded_at, is_aggregated | CHAR(36), DATETIME(3) |
| entity_state_history | 实体状态历史 | id, tenant_id, simulation_id, entity_id, state | CHAR(36), JSON |
| event_log | 事件日志 | id, tenant_id, simulation_id, event_type, severity | CHAR(36) |
| audit_log | 审计日志 | id, tenant_id, user_id, action, changes | CHAR(36), JSON |
| files | 文件存储 | id, tenant_id, file_name, storage_url, checksum | CHAR(36), VARCHAR |
| model_library | 模型库 | id, tenant_id, type, name, status | CHAR(36), VARCHAR |
| model_versions | 模型版本 | id, model_id, version, file_id | CHAR(36), VARCHAR |
| entity_model_binding | 实体模型绑定 | id, scene_id, entity_id, model_version_id | CHAR(36), VARCHAR |
| vehicle_configs | 车辆配置 | id, tenant_id, scene_id, vehicle_id, config | CHAR(36), JSON |
| log_level_config | 日志级别 | id, tenant_id, module, level | CHAR(36), ENUM |
| schema_migrations | Schema 版本 | version, description, applied_at | INT, DATETIME(3) |

### 4.1 Redis 数据结构

| 键模式 | 数据类型 | TTL | 用途 |
|--------|----------|-----|------|
| `path:{from}:{to}:{vehicleType}` | RMapCache | 1h | 路径缓存 (LRU 10k) |
| `session:{simulationId}:{clientId}` | RBucket | 24h | WebSocket 会话状态 |
| `lock:scene:{sceneId}` | RLock | - | 场景编辑分布式锁 |
| `simulation:{simulationId}:events` | RTopic | - | 仿真事件发布订阅 |
| `ratelimit:{api}:{ip}` | RRateLimiter | - | API 限流 |
| `entity:{simulationId}:{entityId}` | RMapCache | 1h | 实体状态缓存 |

---

## 5. API 端点清单

### 5.1 REST API

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/v1/scenes | 创建场景 |
| GET | /api/v1/scenes/{id} | 查询场景 |
| PUT | /api/v1/scenes/{id} | 更新场景 |
| DELETE | /api/v1/scenes/{id} | 删除场景 |
| POST | /api/v1/simulations | 启动仿真 |
| POST | /api/v1/simulations/{id}/pause | 暂停仿真 |
| POST | /api/v1/simulations/{id}/resume | 继续仿真 |
| POST | /api/v1/simulations/{id}/stop | 停止仿真 |
| PUT | /api/v1/simulations/{id}/speed | 调整仿真速度 |
| GET | /api/v1/simulations/{id}/entities | 查询所有实体 |
| GET | /api/v1/simulations/{id}/entities/{entityId} | 查询单个实体 |
| GET | /api/v1/simulations/{id}/metrics | 查询当前 KPI |
| GET | /api/v1/simulations/{id}/metrics/history | 查询历史 KPI |

### 5.2 WebSocket

| 端点 | 消息类型 | 说明 |
|------|----------|------|
| ws://host/api/v1/simulations/{id}/ws | snapshot | 初始状态快照 |
| | entity_update | 实体状态更新 |
| | event | 仿真事件 |
| | simulation_status | 仿真状态变化 |
| | heartbeat | 心跳（30s） |

---

## 6. KPI 指标体系

### 6.1 吞吐量指标
- 任务完成率 (tasks/hour)
- 物料吞吐率 (FOUP/hour)
- 设备服务率 (95% 任务在 60s 内响应)
- 任务延迟率 (≤5% 任务超期)
- 综合吞吐指数 (加权评分)

### 6.2 利用率指标
- 车辆利用率 (时间加权平均)
- 设备利用率 (时间加权平均)
- 路段利用率 (占用频率)

### 6.3 其他指标
- WIP (在制品数量)
- 能耗 (total + by vehicle)
- 瓶颈识别 (高利用率资源)

---

## 7. 里程碑与任务拆分

### Phase 1: 核心实体与基础仿真 (4-6 周)

- [ ] 设计实体类层次结构
- [ ] 实现 LogisticsEntity、Vehicle、OHTVehicle、AGVVehicle
- [ ] 实现 Stocker、ERack、ManualStation、Conveyor、Operator
- [ ] 编写单元测试 (覆盖率 ≥ 80%)
- [ ] 创建简单场景测试 (OHT 从 A 到 B)

### Phase 2: 交通控制系统 (4-6 周)

- [ ] 实现 NetworkGraph、Edge (JGraphT)
- [ ] 实现 EdgeConstraintProvider
- [ ] 实现 PathPlanner (A* 算法)
- [ ] 实现 CachedPathPlanner (LRU 10k)
- [ ] 实现 ControlPoint、ControlArea
- [ ] 实现 ConflictResolver、PriorityManager
- [ ] 实现 TrafficManager、ReservationManager
- [ ] 编写单元测试和集成测试
- [ ] 创建多车冲突场景测试

### Phase 3: 调度引擎 (3-5 周)

- [ ] 实现 Task、TaskQueue、TaskPriority
- [ ] 实现 DispatchRule 接口和内置规则
- [ ] 实现 DispatchEngine
- [ ] 实现 RealTimeDispatcher
- [ ] 实现动态重规划
- [ ] 编写单元测试和集成测试
- [ ] 创建复杂调度场景测试 (10 车 50 任务)

### Phase 4: KPI 与分析 (3-4 周)

- [ ] 实现 SimulationEventBus
- [ ] 实现 MetricsCollector (自适应采样)
- [ ] 实现 ThroughputCalculator (多维度)
- [ ] 实现 UtilizationCalculator (时间加权平均)
- [ ] 实现 EnergyCalculator
- [ ] 实现 BottleneckAnalyzer
- [ ] 实现 VehicleCountEvaluator (二分查找)
- [ ] 实现报表导出 (CSV/JSON/PDF)

### Phase 5: 前端可视化 (6-8 周)

- [ ] 搭建 React + Vite 项目
- [ ] 设计并实现 REST API (Spring Boot)
- [ ] 实现 WebSocket Handler
- [ ] 搭建 Three.js 场景
- [ ] 实现 OHT/AGV/Stocker 3D 模型 (程序化)
- [ ] 实现实时动画 (平滑插值)
- [ ] 实现场景编辑器 (拖拽)
- [ ] 实现 KPI Dashboard (Chart.js)
- [ ] 性能优化 (LOD、实例化渲染)

### Phase 6: 实时派单界面 (2-3 周)

- [ ] 实现任务管理界面
- [ ] 实现车辆监控界面
- [ ] 实现手动控制功能
- [ ] 端到端测试

### Phase 7: 工作流自动化 (4-6 周)

- [ ] 实现 ProcessFlow / ProcessStep 核心模型
- [ ] 实现 EventTrigger / TaskGenerator / WorkflowExecutor
- [ ] 实现 SafetyZone 人车混合交管
- [ ] 实现 RetryConfig / 资源策略
- [ ] 创建 workflow-automation spec 并通过评审

### Phase 7.1: 多候选设备选择（v2.0）(2-3 周)

- [ ] ProcessStep 支持 targetEntityIds（多候选设备）
- [ ] 设备实体支持 supportedTransportTypes（必填）
- [ ] 实现多因子加权选择器（distance/time/wip）
- [ ] 实现运输类型强校验（建模阶段 + 运行时）
- [ ] 更新 UI / specs / schemas 并完成场景验证

### Phase 8: 数据持久化 (2-3 周)

- [ ] 设计数据库 Schema
- [ ] 核心模块先定义 Port 接口（SystemConfig/Cache/ObjectStorage）
- [ ] 在 `sim-logistics-web` 实现 MyBatis-Plus Mapper + Repository/Adapter（MySQL + HikariCP）
- [ ] 在 `sim-logistics-web` 实现 Redis + Redisson 缓存 Adapter（路径/会话/锁/发布订阅）
- [ ] 实现场景版本自动迁移
- [ ] 实现审计日志
- [ ] 在 `sim-logistics-web` 实现 MinIO 文件存储 Adapter
- [ ] 实现归档任务 (定时任务)
- [ ] 保留 InMemory fallback，确保核心模块可离线单测

---

## 8. 技术选型汇总

| 领域 | 技术选型 | 版本/说明 |
|------|----------|-----------|
| Java | JDK | 21 |
| 构建工具 | Maven | 3.9+ |
| 测试框架 | JUnit | 5 |
| 仿真核心 | JSimul | 现有模块 |
| 图算法 | JGraphT | 最新稳定版 |
| Web 框架 | Spring Boot | 3.x (仅 web 模块) |
| 主数据库 | MySQL | 8.0.16+ |
| ORM / 数据访问 | MyBatis-Plus | 3.5.5+（Web/Infra 模块） |
| 缓存 | Redis | 7.0+ |
| Redis 客户端 | Redisson | 3.x (专用缓存) |
| 对象存储 | MinIO | 最新稳定版 |
| 连接池 | HikariCP | 内嵌 |
| 前端框架 | React | 18+ |
| 前端构建 | Vite | 5+ |
| 3D 渲染 | Three.js | r150+ |
| 图表库 | Chart.js | 固定 |
| 状态管理 | Zustand | 固定 |
| WebSocket | Spring WebSocket | WebSocket API |

---

## 9. 风险与缓解措施

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| JSimul 不满足物流需求 | 高 | 中 | 预留扩展点，必要时增强核心 |
| 3D 性能不足 | 高 | 中 | LOD、实例化渲染、实体池 |
| 路径规划性能 | 中 | 低 | 预计算 + LRU 缓存 |
| Redis 缓存一致性 | 中 | 中 | 事件失效 + TTL + 保守清除策略 |
| WebSocket 连接不稳定 | 中 | 低 | 自动重连 + 心跳机制 |
| 内存溢出 | 高 | 中 | 流式处理、可选详细程度 |
| MySQL 数据库性能 | 中 | 低 | 索引优化、分区、归档、Redis 缓存 |
| 场景模型迁移 | 中 | 中 | 自动迁移 + 版本控制 |
| 多用户冲突 | 低 | 低 | 乐观锁 + 版本号 + Redisson 分布式锁 |

---

## 10. 验收标准

### 10.1 功能验收

- [ ] 支持所有核心实体 (OHT, AGV, Stocker, E-Rack, Manual Station, Conveyor, Operator)
- [ ] 支持交通管制 (冲突避让、优先级、控制点)
- [ ] 支持任务调度 (最短距离、最高优先级、最低利用率规则)
- [ ] 支持多维度 KPI 计算
- [ ] 支持最少车辆数评估
- [ ] 3D 场景渲染流畅 (500 实体 60 FPS)
- [ ] 场景编辑器可用 (拖拽、属性编辑)
- [ ] KPI Dashboard 实时更新
- [ ] 实时派单功能正常

### 10.2 性能验收

- [ ] 1000 实体仿真 ≥ 30 FPS
- [ ] 500 实体仿真 ≥ 60 FPS
- [ ] 实时派单响应 ≤ 1s (仿真时间)
- [ ] MySQL 数据库查询响应 ≤ 100ms
- [ ] Redis 缓存命中率 ≥ 80% (路径缓存)
- [ ] 单元测试覆盖率 ≥ 80%

### 10.3 质量验收

- [ ] 所有公共 API 有 JavaDoc
- [ ] 无 Checkstyle 警告
- [ ] 所有场景测试通过
- [ ] 无已知严重 Bug

---

## 11. 下一步行动

### 11.1 立即行动

1. **提案审批**: 项目负责人审查本提案
2. **技术评审**: 团队评审架构设计和技术选型
3. **环境准备**:
   - 搭建 MySQL 8.0.16+ 数据库
   - 搭建 Redis 7.0+ 缓存服务
   - 搭建 MinIO 对象存储
   - 准备前端开发环境

### 11.2 Phase 1 启动

1. 创建 `sim-logistics-core` 模块
2. 实现 LogisticsEntity 基类
3. 实现 Vehicle 抽象类和 OHTVehicle
4. 编写第一个单元测试

### 11.3 并行准备工作

1. 设计数据库迁移脚本 (Flyway/Liquibase for MySQL)
2. 准备实体 JSON Schema 文件
3. 搭建前端项目脚手架
4. 配置 Redisson 客户端和缓存策略

---

## 12. 文档清单

| 文档 | 位置 | 状态 |
|------|------|------|
| 提案主文档 | `proposal.md` | ✅ 完成 |
| 架构设计文档 | `design.md` | ✅ 完成 |
| 任务拆分文档 | `tasks.md` | ✅ 完成 |
| 数据库设计 | `database-schema.md` | ✅ 完成 |
| 实体规范 | `specs/logistics-entities/spec.md` | ✅ 完成 |
| 交通管制规范 | `specs/traffic-control/spec.md` | ✅ 完成 |
| 调度系统规范 | `specs/dispatch-system/spec.md` | ✅ 完成 |
| KPI 规范 | `specs/kpi-metrics/spec.md` | ✅ 完成 |
| Web 可视化规范 | `specs/web-visualization/spec.md` | ✅ 完成 |
| 提案总结 | `PROPOSAL_SUMMARY.md` | ✅ 完成 |

---

## 13. 参考资料

### FlexSim 调研来源

1. [FlexSim Material Handling Simulation](https://www.flexsim.com/material-handling-simulation/)
2. [FlexSim Warehousing Simulation](https://www.flexsim.com/warehousing-simulation/)
3. [FlexSim AGV Network Logic](https://docs.flexsim.com/en/23.1/WorkingWithTasks/AGVNetworks/BuildingAGVLogic/BuildingAGVLogic.html)
4. [FlexSim Connecting to External Code](https://docs.flexsim.com/en/24.1/Reference/DeveloperAdvancedUser/ConnectingToExternalCode/ConnectingToExternalCode.html)
5. [FlexSim Documentation](https://docs.flexsim.com/)

### 项目文档

- README.md - 项目概述
- AGENTS.md - 开发规范
- JSimul_/README.md - 仿真引擎文档

---

**提案状态**: 🟡 待审批

**建议**: 请项目技术负责人审查本提案，确认技术选型和架构设计后，进入实施阶段。
