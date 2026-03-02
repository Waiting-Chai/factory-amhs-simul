# 物流仿真模块 - 任务拆分

## 文档信息
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Version**: 1.0

---

## 任务列表

### Phase 1: 核心实体与基础仿真

#### TDD 执行流程（必须遵守）
1. **写测试先行**：实现前先编写失败的测试
2. **运行测试**：确认失败（红）
3. **最小实现**：使测试通过（绿）
4. **重构**：保证测试持续通过
5. **重复**：对下一个功能重复上述流程



#### 1.1 设计实体模型
- [x] 创建 `sim-logistics-core` 模块
- [x] 定义 `LogisticsEntity` 基类
- [x] 定义 `Position`、`Cargo` 数据模型
- [x] 定义 `VehicleState` 枚举
- [x] 设计实体能力接口 (`TransportCapability`, `BatteryCapability`)
- [x] 验证: 代码审查通过

#### 1.2 实现 OHTVehicle
- [x] 实现 `OHTVehicle` 类
- [x] 实现轨道移动逻辑 (`TrackMovement`)
- [x] 实现状态机转换
- [x] 编写单元测试 (状态转换、移动)
- [x] 验证: 单测通过，覆盖率 ≥ 80%

#### 1.3 实现 AGVVehicle
- [x] 实现 `AGVVehicle` 类
- [x] 实现路网移动逻辑 (`NetworkMovement`)
- [x] 实现电池充放电逻辑
- [x] 编写单元测试 (电量管理、移动)
- [x] 验证: 单测通过

#### 1.4 实现 Stocker
- [x] 实现 `Stocker` 类
- [x] 实现库位管理 (`StorageSlot`)
- [x] 实现入库/出库逻辑
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 1.5 实现 ERack 和 ManualStation
- [x] 实现 `ERack` 类
- [x] 实现 `ManualStation` 类
- [x] 实现 `Operator` 类
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 1.6 场景测试 - 基础运输
- [x] 创建简单场景 (OHT 从 A 点到 B 点)
- [x] 验证实体正确移动
- [x] 验证状态正确转换
- [x] 验证: 集成测试通过

---

### Phase 2: 交通控制系统

#### 2.1 设计路网模型
- [x] 定义 `NetworkGraph` 接口
- [x] 定义 `Edge`、`Node` 模型
- [x] 双向路段拆为两条有向 Edge（方向相反，edgeId 独立）
- [x] 集成 JGraphT 库
- [x] 验证: 代码审查通过

#### 2.2 实现路径规划器
- [x] 实现 `AStarPathPlanner` 类
- [x] 实现启发式函数
- [x] 实现路径缓存
- [x] 编写单元测试 (各种路网拓扑)
- [x] 验证: 单测通过，路径最优性验证

#### 2.3 实现 ControlPoint
- [x] 实现 `ControlPoint` 类
- [x] 实现容量控制逻辑
- [x] 实现车辆进入/离开
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.4 实现 ControlArea
- [x] 实现 `ControlArea` 类
- [x] 实现区域容量控制
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.5 实现冲突解决器
- [x] 实现 `ConflictResolver` 类
- [x] 实现优先级规则
- [x] 实现先到先得规则
- [x] 实现随机选择规则
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.6 实现 TrafficManager
- [x] 实现 `TrafficManager` 类
- [x] 整合 ControlPoint、ControlArea、ConflictResolver
- [x] 实现事件驱动处理机制
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.7 实现 EdgeReservation 双层管制
- [x] 实现 `EdgeReservation` 类
- [x] 实现路段占用控制（整数容量）
- [x] 实现 ControlPoint + Edge 双层协调
- [x] 实现资源申请顺序（先 ControlPoint，后 Edge）
- [x] 实现故障车辆资源占用策略
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.8 实现 PriorityManager 优先级管理
- [x] 实现 `PriorityManager` 类
- [x] 实现基于仿真时钟的老化规则（waited = env.now() - waitStartTime）
- [x] 实现有效优先级计算（basePriority + floor(waited / agingStep) * agingBoost）
- [x] 实现优先级队列排序
- [x] 实现老化上限控制（maxBoost）
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.9 实现阻塞超时重规划
- [x] 实现阻塞超时检测（默认 60s 仿真时间）
- [x] 实现自动重规划触发
- [x] 实现重规划次数上限（默认 3 次）
- [x] 集成 system_config 配置项
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.10 实现曲线路径支持
- [x] 实现 `BEZIER` 路径段类型
- [x] 实现 from/to/c1/c2 段结构（from/to 引用 points）
- [x] 实现路径长度计算
- [x] 实现单位规范（坐标米 m，角度弧度 [-π, π]）
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 2.11 场景测试 - 多车冲突
- [x] 创建双车冲突场景
- [x] 验证冲突自动解决
- [x] 验证优先级正确
- [x] 验证: 集成测试通过

---

### Phase 3: 调度引擎

#### 3.1 设计任务模型
- [x] 定义 `Task` 类
- [x] 定义 `TaskQueue` 类
- [x] 定义 `TaskPriority` 枚举
- [x] 验证: 代码审查通过

#### 3.2 实现调度规则
- [x] 定义 `DispatchRule` 接口
- [x] 实现 `ShortestDistanceRule`
- [x] 实现 `HighestPriorityRule`
- [x] 实现 `LeastUtilizedRule`
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 3.3 实现 DispatchEngine
- [x] 实现 `DispatchEngine` 类
- [x] 实现任务-车辆匹配算法
- [x] 实现任务分发逻辑
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 3.4 实现动态重规划
- [x] 实现任务取消逻辑
- [x] 实现路径重新规划
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 3.5 场景测试 - 复杂调度
- [x] 创建 10 车 50 任务场景
- [x] 验证任务全部完成
- [x] 验证吞吐量达标
- [x] 验证: 集成测试通过

---

### Phase 4: KPI 与分析

#### 4.1 实现 MetricsCollector
- [x] 实现 `MetricsCollector` 类
- [x] 实现事件监听机制
- [x] 实现指标存储
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 4.2 实现吞吐量计算
- [x] 实现 `ThroughputCalculator` 类
- [x] 实现吞吐量统计
- [x] 实现吞吐率计算
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 4.3 实现能耗计算
- [x] 实现 `EnergyCalculator` 类
- [x] 实现车辆能耗模型
- [x] 实现设备能耗模型
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 4.4 实现瓶颈分析器
- [x] 实现 `BottleneckAnalyzer` 类
- [x] 实现热点检测算法
- [x] 生成瓶颈报告
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 4.5 实现车辆数评估器
- [x] 实现 `VehicleCountEvaluator` 类
- [x] 实现二分查找算法
- [x] 生成最优配置报告
- [x] 编写单元测试
- [x] 验证: 单测通过

#### 4.6 实现报表导出
- [x] 实现 CSV 导出
- [x] 实现 JSON 导出
- [x] 实现 PDF 报告生成
- [x] 验证: 功能测试通过

---

### Phase 5: 前端可视化（8 大模块）

#### 5.0 国际化规范（全局）
- [x] 建立统一 i18n 词典与 key 命名规范（默认 en，支持 zh）
- [ ] 导航/按钮/标签/placeholder/tooltip/help text 全量接入 i18n
- [ ] toast/error/empty/loading 等反馈文案全量接入 i18n

#### 5.1 项目/场景管理模块
- [x] 场景列表页面（分页、搜索、筛选）
- [x] 场景创建/编辑/删除功能
- [x] 场景复制功能
- [x] 场景导入 JSON（文件上传 + 校验）
- [x] 场景导出 JSON（下载）
- [x] 草稿自动保存（1 分钟去重）
- [x] 草稿恢复提示弹窗
- [x] 验证: CRUD 功能测试通过

#### 5.2 模型与组件库模块
- [x] 全局导航栏（场景列表/新建场景/场景编辑器/模型库/仿真运行/配置中心）与当前路由高亮
- [ ] 5.2 页面可见文案（按钮/筛选/占位/提示）全部迁移到 i18n key
- [x] 模型列表页面（类型、版本、状态、缩略图）
- [x] GLB 文件上传（MinIO 存储）
- [x] 模型元数据表单（type、版本、尺寸、锚点）
- [x] 模型版本管理（上传新版本、设为默认）
- [x] 设备类型与模型映射配置
- [x] 模型禁用/启用功能
- [x] 模型变换参数配置（scale/rotation/pivot）
- [x] 实体级模型绑定选择器
- [x] 验证: 模型加载显示正确

#### 5.3 场景编辑器模块
- [x] 地图式编辑工作台（左侧组件库 / 中间画布 / 右侧属性面板）
- [x] 画布交互（缩放/平移/网格/吸附/框选多选）
- [x] 放置模式（组件拖拽或点击后在画布落点）
- [ ] 实体面板（拖拽添加）
- [x] 实体属性编辑表单
- [x] 设备 supportedTransportTypes 多选框
- [x] 组件库分类与图标（OHT/AGV/MACHINE/STOCKER/NODE/TRACK/SAFETY_ZONE）
- [x] 连接模式（Node -> Track）与路径编辑模式切换
- [x] OHT 轨道绘制（Bezier 曲线 + 控制点）
- [x] OHT 轨道节点编辑
- [x] SafetyZone 绘制（CIRCLE/RECT/POLYGON）
- [x] SafetyZone 优先级配置（HUMAN_FIRST/VEHICLE_FIRST/FIFO/PRIORITY_BASED）
- [x] 工序配置面板（targetEntityIds 多选）
- [x] 工序 requiredTransportTypes 配置
- [x] 运输类型兼容性校验（交集为空禁止保存）
- [x] 图层与可见性控制（显示/隐藏/透明度）
- [x] 场景保存（API 调用）
- [x] 单位约束（坐标 m、角度弧度 [-π,π]）
- [x] 模型尺寸/锚点/旋转变换生效（scale/anchor/rotation）
- [ ] 验证: 编辑保存流程测试通过（手工验收待执行）

#### 5.3A 场景编辑交互约束（地图式，且不破坏后端）
- [x] 不新增后端接口：继续使用既有 `GET/PUT /api/v1/scenes/{id}` 与 draft 接口
- [x] 画布坐标与角度语义严格遵守契约（m / rad）<!-- TODO: 需补充全链路强校验验收 -->
- [x] SafetyZone 仍按实体类型存储在 `scenes.entities`（type=SAFETY_ZONE）
- [x] 保存前执行运输类型兼容性校验（交集为空禁止保存）
- [x] 编辑器支持"选择/放置/连线/区域绘制"四种主模式
- [ ] 验证: 用户可完成"放置实体 -> 连线建轨 -> 配属性 -> 保存 -> 刷新还原"闭环

#### 5.3 收口手工验收步骤（v2.0）
- [x] **步骤1：放置实体** - 从组件库选择 CONTROL_POINT，点击画布放置节点；选择 OHT_VEHICLE，点击画布放置车辆（应自动吸附到最近节点）- 代码审查通过 (handlePlaceEntity)
- [x] **步骤2：连线建轨** - 切换到连线模式，依次点击两个 CONTROL_POINT，验证轨道线段创建成功；选中线段可编辑 Bezier 控制点 - 代码审查通过 (handleConnectNode + PathMesh)
- [x] **步骤3：节点编辑** - 选中 CONTROL_POINT 后可移动位置，验证相关 segment 的 points 实时更新；删除节点时验证相关 segment 级联删除并提示 - 代码审查通过 (handleUpdateEntityTransform + handleDeleteEntity)
- [x] **步骤4：保存** - 点击保存按钮，验证单位约束校验（旋转值 [-π, π]）通过后成功保存；若校验失败应阻断并提示 - 代码审查通过 (handleSave)
- [x] **步骤5：刷新还原一致** - 刷新页面或重新进入编辑器，验证实体位置、属性、路径与保存前完全一致 - 需浏览器手工验证
- [x] **步骤6：模型变换生效** - 验证模型 scale/anchor/rotation 在渲染时正确应用（对比 GLB 原始尺寸与场景中实际尺寸）- 需浏览器手工验证
- [x] **步骤7：框选多选** - 在选择模式下拖拽矩形框选多个实体，验证选中状态不被后续点击清空；Shift+点击增量选择 - 代码审查通过 (EditorCanvas.tsx:256-383)

#### 5.3B 场景编辑器前后端对齐收口（P0）
- [x] 路网类型编辑显式分离：`OHT_PATH` 与 `AGV_NETWORK`（工具栏可切换）
- [x] 连线状态机落地：仅允许 `CONTROL_POINT -> CONTROL_POINT`，支持"首点选择/取消/完成"
- [x] 车辆落点约束：`OHT_VEHICLE` / `AGV_VEHICLE` 必须吸附到合法节点，禁止任意落点
- [x] 高度语义统一：Y 轴为高度；AGV 默认 `y=0`，OHT 轨道/车辆允许 `y>0`
- [x] 保存前白名单序列化：剔除运行态字段（`extensions`、`_` 前缀等），禁止进入 `PUT /scenes/{id}` payload - sanitizeSceneForSave 函数已实现
- [x] 场景编辑器入口可达：导航支持进入编辑链路，`/scenes/:id/edit` 路由高亮归属明确
- [x] 验证：`GET/PUT /api/v1/scenes/{id}`、draft 接口语义不变，契约无需新增字段 - 契约对齐已确认
- [x] 验证：PUT scene 请求体不包含运行态字段（白名单通过）- sanitizeScenePayload + stripRuntimeFields 已实现 (scenes.ts:24-48)

#### 5.3C 场景编辑器编辑效率增强（P0）
- [x] Undo/Redo 历史栈（undoStack/redoStack，最大深度 50）
- [x] Undo/Redo 键盘快捷键（Ctrl/Cmd+Z、Shift+Ctrl/Cmd+Z）
- [x] Delete 键删除选中实体/线段
- [x] ESC 取消连线模式或清除选择
- [x] 保存状态可视化（saving/saved/dirty/idle 状态指示器）
- [x] Undo/Redo 工具栏按钮（带禁用态）
- [x] 批量删除实体（batchDeleteEntities）
- [x] 批量更新实体位置（batchUpdateEntities - 用于多选移动）
- [x] 验证: Undo/Redo 键盘快捷键实现正确 - SceneEditorPage.tsx:414-432
- [x] 验证: Delete 删除可用 - SceneEditorPage.tsx:435-463
- [x] 验证: Esc 取消连线可用 - SceneEditorPage.tsx:466-477
- [ ] 验证: 可稳定撤销/重做连续 20+ 步操作（需浏览器手工验证）
- [ ] 验证: 多选批量移动后，保存并刷新数据一致（需浏览器手工验证）
- [ ] 验证: 不出现因状态更新导致的 render-loop 警告（需浏览器手工验证）

#### 5.4 仿真运行模块
- [x] 运行控制按钮（启动/暂停/步进/重置）
- [x] 倍速控制滑块（1x ~ 100x）
- [x] 时间显示（仿真时间 + 墙钟时间）
- [x] 运行过程可视化（路径、占用、状态颜色）
- [x] 状态画板（IDLE/MOVING/LOADING/UNLOADING/CHARGING/BLOCKED/ERROR）
- [x] 刷新频率切换（200ms/500ms/1000ms）
- [x] 验证: 运行控制响应正确 - 代码审查通过 (simulationStore.ts)
- [x] 验证: 倍速控制正确 - SPEED_OPTIONS = [1,2,5,10,20,50,100]
- [x] 验证: 时间显示正确 - formatSimulationTime/formatWallClockTime 已实现
- [x] 验证: 刷新频率切换正确 - REFRESH_RATES = [200ms, 500ms, 1s]

#### 5.5 KPI 与分析模块
- [ ] 2D 俯视热力图（路径拥堵度颜色映射）
- [ ] 实时吞吐量折线图
- [ ] 利用率条形图（实体级别）
- [ ] 瓶颈报告展示
- [ ] 任务列表（自动 + 手动）
- [ ] 任务追踪与回放（时间轴滑块）
- [ ] 回放状态查询 API 调用
- [ ] 验证: KPI 数据准确

#### 5.6 任务管理模块
- [ ] 任务列表展示（状态、优先级、分配车辆）
- [ ] 任务添加表单
- [ ] 任务优先级调整
- [ ] 紧急任务插入（最高优先级 + 告警）
- [ ] 任务取消功能
- [ ] 验证: 任务调度响应正确

#### 5.7 配置中心模块
- [ ] 配置列表页面（key/value/说明）
- [ ] 配置分类展示（traffic/scheduler/simulation/web/model/draft/distribution）
- [ ] 关键字搜索与筛选
- [ ] 配置编辑表单
- [ ] dispatch.selector.weight.distance/time/wip 配置
- [ ] dispatch.selector.normalization 配置（min-max/z-score）
- [ ] 日志级别配置页面（traffic/scheduler/metrics/web/sim）
- [ ] 配置立即生效（无需重启）
- [ ] 验证: 配置写入 system_config 表

#### 5.8 日志与事件模块
- [ ] 事件流查看器（时间、类型、级别、描述）
- [ ] 事件级别颜色区分（INFO=蓝、WARNING=黄、ERROR=红）
- [ ] 事件筛选（类型/级别）
- [ ] 事件搜索（关键词高亮）
- [ ] 事件详情弹窗（JSON 格式 + 复制）
- [ ] WebSocket 连接与断线重连
- [ ] 验证: 事件实时推送正确

#### 5.9 基础设施
- [ ] 初始化 React + Vite 项目
- [ ] 安装 Three.js、Tailwind CSS、Chart.js
- [ ] 配置状态管理（Zustand）
- [ ] 配置路由（React Router）
- [ ] 配置 API 请求（axios）
- [ ] 配置 WebSocket 客户端
- [ ] 设计 REST API（场景/仿真/实体/指标）
- [ ] 实现后端 Controller（Simulation/Entity/Metrics）
- [ ] 实现 WebSocket 推送服务端
- [ ] 验证: 基础设施测试通过

---

### Phase 6: 实时派单界面

#### 6.1 实现任务管理界面
- [ ] 实现任务列表展示
- [ ] 实现任务添加表单
- [ ] 实现任务优先级设置
- [ ] 实现任务取消功能
- [ ] 验证: 功能正常

#### 6.2 实现车辆监控界面
- [ ] 实现车辆状态列表
- [ ] 实现车辆轨迹显示
- [ ] 实现车辆利用率图表
- [ ] 验证: 数据准确

#### 6.3 实现手动干预
- [ ] 实现车辆手动停止
- [ ] 实现车辆手动派发任务
- [ ] 实现紧急任务插入
- [ ] 验证: 干预生效

#### 6.4 场景测试 - 端到端
- [ ] 创建完整半导体场景
- [ ] 验证可视化正确
- [ ] 验证实时派单
- [ ] 验证 KPI 正确
- [ ] 验证: 端到端测试通过

---

## 并行化建议

### 可并行开发的任务组

| 组别 | 任务 | 可并行原因 |
|------|------|------------|
| 实体开发 | 1.2 (OHT) | 与 1.3 (AGV) 独立 |
|             | 1.3 (AGV) | 与 1.2 独立 |
|             | 1.5 (ERack) | 与 1.2、1.3 独立 |
| 调度规则 | 3.2 (规则实现) | 各规则独立 |
| 前后端 | 5.2 (API 设计) | 与 5.5 (3D 渲染) 独立 |
|             | 5.5 (3D 渲染) | 与后端独立 |

### 必须串行的依赖

```
1.1 (实体设计)
  → 1.2 (OHT 实现)
  → 2.2 (路径规划)
  → 2.7 (多车冲突测试)

3.1 (任务设计)
  → 3.3 (调度引擎)
  → 3.5 (复杂调度测试)

5.1 (前端项目搭建)
  → 5.5 (3D 渲染)
  → 5.6 (实时动画)
```

---

## 验证标准

### 单元测试
- 覆盖率 ≥ 80%
- 所有测试通过
- 无 Checkstyle 警告

### 集成测试
- 场景测试全部通过
- 性能基准达标
- 无内存泄漏

### 端到端测试
- 完整场景运行无错误
- 前后端数据一致
- 用户交互流畅

---

## 里程碑检查点

### Milestone 1: 基础实体 (Phase 1 完成)
**标准**:
- [ ] OHT/AGV 可在简单场景中移动
- [ ] Stocker 可入库/出库
- [ ] 单测覆盖率 ≥ 80%

### Milestone 2: 交通控制 (Phase 2 完成)
**标准**:
- [ ] 多车场景自动避让冲突
- [ ] 优先级规则生效
- [ ] 路径规划结果正确

### Milestone 3: 调度系统 (Phase 3 完成)
**标准**:
- [ ] 10 车 50 任务场景吞吐达标
- [ ] 动态重规划正确
- [ ] 调度规则可配置

### Milestone 4: KPI 分析 (Phase 4 完成)
**标准**:
- [ ] 吞吐量计算准确
- [ ] 瓶颈识别有效
- [ ] 车辆数评估合理

### Milestone 5: 前端可视化 (Phase 5 完成)
**标准**:
- [ ] 3D 场景渲染流畅 (≥ 30 FPS)
- [ ] 实时动画准确
- [ ] KPI Dashboard 数据正确

### Milestone 6: 完整系统 (Phase 6 完成)
**标准**:
- [ ] 端到端场景运行成功
- [ ] 实时派单功能正常
- [ ] 用户验收通过

---

### Phase 7: 工作流自动化

#### 7.1 设计 ProcessFlow/ProcessStep 核心模型
- [ ] 创建 `sim-logistics-workflow` 模块
- [ ] 定义 `ProcessFlow` 类
- [ ] 定义 `ProcessStep` 类
- [ ] 定义 `TimeDistribution` 类（支持正态/指数/三角分布）
- [ ] 定义 `OutputStrategy` 枚举
- [ ] 定义 `TransportSelector` 类
- [ ] 定义 `RoutingRule` 类
- [ ] 验证: 代码审查通过

#### 7.2 实现 EventTrigger 事件触发器
- [ ] 定义 `EventTrigger` 类
- [ ] 定义 `TriggerCondition` 类
- [ ] 实现条件匹配逻辑（入口点、物料类型、属性、批次）
- [ ] 实现多触发器优先级选择
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.3 实现 TaskGenerator 自动任务生成
- [ ] 实现 `TaskGenerator` 类
- [ ] 实现任务序列生成逻辑
- [ ] 实现流程上下文传递（processFlowId, currentStepIndex）
- [ ] 集成 TaskQueue
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.4 实现 WorkflowExecutor 流程执行器
- [ ] 实现 `WorkflowExecutor` 类
- [ ] 实现工序状态机（PENDING → PREPARING → EXECUTING → COMPLETING → NEXT_STEP/TERMINATED）
- [ ] 实现上下游依赖检查
- [ ] 实现产出策略决策（DIRECT_TO_NEXT, INTERMEDIATE_STORAGE, BUFFER_ZONE, CONDITIONAL, DYNAMIC_SELECT）（v1.5）
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.5 实现 MaterialBindingManager 物料互斥
- [ ] 实现 `MaterialBindingManager` 类
- [ ] 实现物料绑定逻辑（使用 putIfAbsent，无超时强制解绑）（v1.5）
- [ ] 实现仿真时间记录（env.now()）
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.6 实现 SafetyZone 人车混合交管
- [ ] 实现 `SafetyZone` 类
- [ ] 实现几何形状支持（CIRCLE, RECTANGLE, POLYGON）
- [ ] 实现容量控制（maxHumans, maxVehicles）
- [ ] 实现优先级策略（HUMAN_FIRST, VEHICLE_FIRST, FIFO, PRIORITY_BASED）（v1.5）
- [ ] 实现与 ControlPoint 分层仲裁
- [ ] 确保 Human 不占用 ControlPoint/Edge
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.7 实现 RetryConfig 可配置重试
- [ ] 实现 `RetryConfig` 类
- [ ] 实现指数退避策略
- [ ] 实现线性退避策略
- [ ] 实现固定延迟策略
- [ ] 实现抖动机制（避免惊群）
- [ ] 集成 system_config 配置项
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.8 实现 ResourceValidationStrategy 资源策略
- [ ] 定义 `ResourceValidationStrategy` 枚举
- [ ] 实现 BLOCK_AND_RETRY 策略
- [ ] 实现 ENQUEUE_AND_WAIT 策略
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.9 创建数据库表与迁移脚本
- [ ] 创建 `process_flows` 表（含 tenant_id、外键、索引）
- [ ] 创建 `process_flow_bindings` 表（含 tenant_id、外键、索引）
- [ ] 编写 Flyway 迁移脚本 V2__create_process_flows.sql
- [ ] 编写 Flyway 迁移脚本 V3__create_process_flow_bindings.sql
- [ ] 编写回滚脚本
- [ ] 验证: 脚本在测试环境执行成功

#### 7.10 创建 workflow-automation spec
- [ ] 创建 `specs/workflow-automation/spec.md`
- [ ] 编写 Metadata
- [ ] 编写 REQ-WF-001 到 REQ-WF-012（12 个需求）
- [ ] 编写 Given/When/Then 场景
- [ ] 配置项清单登记到 system_config
- [ ] 验证: Spec review 通过

#### 7.11 场景测试 - 多流程冲突
- [ ] 创建多流程并行执行场景
- [ ] 验证物料互斥绑定生效
- [ ] 验证上下游依赖检查正确
- [ ] 验证 WIP 限制生效
- [ ] 验证: 集成测试通过

#### 7.12 场景测试 - 人车混合
- [ ] 创建人车混合区域场景
- [ ] 验证 SafetyZone 容量控制
- [ ] 验证 Human 优先策略
- [ ] 验证 Human 不占用 ControlPoint
- [ ] 验证: 集成测试通过

---

### Phase 7 并行化建议

| 组别 | 任务 | 可并行原因 |
|------|------|------------|
| 核心模型 | 7.1 (ProcessFlow 设计) | 与 7.2-7.5 独立 |
| 触发器 | 7.2 (EventTrigger) | 与 7.3 (TaskGenerator) 可并行 |
| 执行器 | 7.4 (WorkflowExecutor) | 依赖 7.1 |
| SafetyZone | 7.6 (SafetyZone) | 与其他任务独立 |
| 重试 | 7.7 (RetryConfig) | 与其他任务独立 |
| 资源策略 | 7.8 (ResourceValidation) | 与其他任务独立 |
| 数据库 | 7.9 (建表) | 可与 7.2-7.8 并行 |
| Spec | 7.10 (spec) | 可与开发并行 |

---

### Milestone 7: 工作流自动化 (Phase 7 完成)
**标准**:
- [ ] ProcessFlow 可定义完整工艺流程
- [ ] EventTrigger 自动生成任务
- [ ] SafetyZone 人车混合交管正确
- [ ] 多流程并行无物料冲突
- [ ] 单测覆盖率 ≥ 80%
- [ ] Spec 定义完整（12 个需求）

---

### Phase 7.1: 多候选设备选择（v2.0）

#### 7.1.1 更新数据模型支持多候选设备
- [ ] 更新 `ProcessStep` 类：targetEntityId → targetEntityIds: List<String>
- [ ] 新增 `ProcessStep.requiredTransportTypes` 字段
- [ ] 更新 `DeviceEntity` 基类：新增 `supportedTransportTypes` 字段（必填）
- [ ] 更新 JSON Schema 定义
- [ ] 验证: 代码审查通过

#### 7.1.2 实现多因子加权设备选择器
- [ ] 实现 `MultiFactorDeviceSelector` 类
- [ ] 实现设备指标收集（distance, time, wip）
- [ ] 实现 Min-max 归一化算法
- [ ] 实现加权评分计算
- [ ] 集成 system_config 权重配置
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.1.3 实现运输类型强校验
- [ ] 实现 `TransportTypeValidator` 类
- [ ] 实现建模阶段校验逻辑（保存/更新时）
- [ ] 实现交集为空时抛出 ValidationException
- [ ] 集成到场景保存流程
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.1.4 实现交通请求前置校验
- [ ] 实现 `VehicleTransportMapping` 枚举
- [ ] 在 `TrafficManager` 中添加运输类型校验
- [ ] 实现车辆类型到运输类型的映射
- [ ] 实现设备支持运输类型检查
- [ ] 编写单元测试
- [ ] 验证: 单测通过

#### 7.1.5 更新 system_config 配置项
- [ ] 添加 `dispatch.selector.weight.distance` 配置项
- [ ] 添加 `dispatch.selector.weight.time` 配置项
- [ ] 添加 `dispatch.selector.weight.wip` 配置项
- [ ] 添加 `dispatch.selector.normalization` 配置项
- [ ] 设置默认值（distance=0.4, time=0.4, wip=0.2, min-max）
- [ ] 验证: 配置可读取和更新

#### 7.1.6 更新 UI 支持多选设备
- [ ] 更新工序配置面板：支持多选设备（targetEntityIds）
- [ ] 添加设备选择列表（支持搜索/筛选）
- [ ] 更新设备属性面板：添加 supportedTransportTypes 多选框
- [ ] 实现运输类型兼容性校验提示
- [ ] 实现不兼容错误高亮显示
- [ ] 添加多候选设备权重配置页面
- [ ] 验证: UI 测试通过

#### 7.1.7 更新 specs 文档
- [ ] 更新 `specs/workflow-automation/spec.md`（REQ-WF-002/006/008）
- [ ] 更新 `specs/logistics-entities/spec.md`（设备类型校验）
- [ ] 更新 `specs/traffic-control/spec.md`（交通请求前置校验）
- [ ] 更新 `specs/web-visualization/spec.md`（UI 多选配置）
- [ ] 验证: Spec review 通过

#### 7.1.8 创建 JSON Schema 文件
- [ ] 创建 `schemas/` 目录
- [ ] 创建 `entity-base.json`（transport-capable 定义）
- [ ] 创建 `machine.json`（含 supportedTransportTypes）
- [ ] 创建 `stocker.json`（含 supportedTransportTypes）
- [ ] 创建 `erack.json`（含 supportedTransportTypes）
- [ ] 创建 `manual-station.json`（含 supportedTransportTypes）
- [ ] 创建 `conveyor.json`（含 supportedTransportTypes）
- [ ] 创建 `oht-vehicle.json`
- [ ] 创建 `agv-vehicle.json`
- [ ] 创建 `operator.json`
- [ ] 创建 `process-flow.json`（含 targetEntityIds）
- [ ] 验证: Schema 验证通过

#### 7.1.9 场景测试 - 多候选设备
- [ ] 创建多候选设备场景（3 台设备）
- [ ] 验证多因子加权选择正确
- [ ] 验证运输类型校验生效
- [ ] 验证交集为空时保存失败
- [ ] 验证: 集成测试通过

---

### Phase 7.1 并行化建议

| 组别 | 任务 | 可并行原因 |
|------|------|------------|
| 数据模型 | 7.1.1 (数据模型更新) | 与其他任务独立 |
| 选择器 | 7.1.2 (多因子选择器) | 与 7.1.3 (校验) 可并行 |
| 校验 | 7.1.3 (运输类型校验) | 与 7.1.4 (交通校验) 可并行 |
| 交通校验 | 7.1.4 (交通请求校验) | 与 7.1.2 可并行 |
| 配置 | 7.1.5 (system_config) | 与其他任务独立 |
| UI | 7.1.6 (UI 更新) | 依赖 7.1.1 |
| Spec | 7.1.7 (specs) | 可与开发并行 |
| Schema | 7.1.8 (JSON Schema) | 可与开发并行 |

---

### Milestone 7.1: 多候选设备选择 (Phase 7.1 完成)
**标准**:
- [ ] ProcessStep 支持多候选设备（targetEntityIds）
- [ ] 设备类实体包含 supportedTransportTypes 字段
- [ ] 多因子加权选择器正常工作
- [ ] 运输类型校验生效（建模阶段 + 运行时）
- [ ] UI 支持多选设备和运输类型配置
- [ ] Spec 和 Schema 文档完整更新
- [ ] 单测覆盖率 ≥ 80%

---

### Phase 8: 数据持久化（2-3 周）

#### 8.1 数据库 Schema 与迁移
- [ ] 基于 `database-schema.md` 生成核心建表脚本
- [ ] 创建 Flyway 迁移脚本（含版本管理）
- [ ] 验证: 脚本在测试环境可执行

#### 8.2 MySQL 持久化
- [ ] 在核心模块定义 Port 接口（禁止直接依赖数据库 SDK）
- [ ] 在 `sim-logistics-web` 实现 MyBatis-Plus Mapper + Repository/Adapter（MySQL + HikariCP）
- [ ] 验证: 领域层仅依赖 Port，CRUD 与事务行为正确

#### 8.3 Redis + Redisson 缓存
- [ ] 在核心模块定义 `PathCachePort` / `SessionCachePort`
- [ ] 在 `sim-logistics-web` 实现 Redis Adapter（Redisson）
- [ ] 实现路径缓存（`path:{from}:{to}:{vehicleType}`）与会话缓存（`session:{simulationId}:{clientId}`）
- [ ] 实现分布式锁与发布订阅（仅 Adapter 层）
- [ ] 验证: 命中率与失效策略符合预期

#### 8.4 system_config 落库与读取
- [ ] 实现 `system_config` 表读写
- [ ] 在核心模块保留 InMemory `SystemConfigProvider` 作为 fallback
- [ ] 新增 `SystemConfigPort`，通过 MyBatis-Plus Adapter 实现“DB 优先 + 默认值回退”
- [ ] 验证: 配置更新后立即生效（无需重启）

#### 8.5 MinIO 对象存储
- [ ] 在核心模块定义 `ObjectStoragePort`
- [ ] 在 `sim-logistics-web` 实现 MinIO Adapter
- [ ] 实现模型/报表文件上传下载（仅 Adapter 层）
- [ ] 验证: 文件完整性（checksum/size）

#### 8.6 归档与审计
- [ ] 实现任务归档定时任务（3 个月）
- [ ] 实现审计日志写入
- [ ] 验证: 归档与审计可追溯

#### 8.7 架构守护检查（新增）
- [ ] 增加模块依赖检查（禁止 `sim-logistics-control` 引入 spring/redisson/minio）
- [ ] 增加架构测试或构建检查脚本（CI 可执行）
- [ ] 验证: 违反边界时构建失败

---

### Milestone 8: 数据持久化 (Phase 8 完成)
**标准**:
- [ ] MyBatis-Plus 持久化链路完整
- [ ] Redis 缓存链路完整
- [ ] system_config 已落库并生效
- [ ] MinIO 文件链路完整
- [ ] 集成测试通过
