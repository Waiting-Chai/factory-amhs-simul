# Web 可视化能力规范

## Metadata
- **Spec ID**: `web-visualization`
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Status**: 提案阶段

---

## ADDED Requirements

### Requirement: REQ-WEB-001 REST API 设计

系统MUST实现REQ-WEB-001（REST API 设计）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 定义后端 RESTful API

#### API 契约定义

**通用响应格式（统一）**:
```json
{
  "code": "OK",
  "message": "success",
  "data": { ... },
  "traceId": "req-xxx"
}
```

#### Scenario: 场景 CRUD

**POST /api/v1/scenes** - 创建场景

**请求**:
```json
{
  "name": "半导体工厂 01 号线",
  "description": "场景描述",
  "entities": [
    {
      "id": "OHT-01",
      "type": "OHTVehicle",
      "position": {"x": 0, "y": 0, "z": 10},
      "properties": {
        "trackId": "TRACK-A",
        "maxLoad": 50.0,
        "speed": 2.5
      }
    }
  ],
  "paths": [ ... ]
}
```

**响应** (201 Created):
```json
{
  "code": "OK",
  "message": "success",
  "data": { "sceneId": "scene-uuid-001", "name": "半导体工厂 01 号线", "version": 1 },
  "traceId": "req-xxx"
}
```

**GET /api/v1/scenes/{id}** - 查询场景

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": { "sceneId": "scene-uuid-001", "entities": [ ... ], "paths": [ ... ], "version": 1 },
  "traceId": "req-xxx"
}
```

**PUT /api/v1/scenes/{id}** - 更新场景

**请求**: 同 POST

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": { "sceneId": "scene-uuid-001", "version": 2 },
  "traceId": "req-xxx"
}
```

**DELETE /api/v1/scenes/{id}** - 删除场景

**响应** (204 No Content)

**GET /api/v1/scenes** - 分页语义
- `page` 参数采用 **0-based**（第一页为 `0`）
- 前端展示可为 1-based，但请求时必须显式转换为 0-based
- 响应中的 `data.page` 保持 0-based

#### Scenario: 仿真控制

**POST /api/v1/simulations** - 启动仿真

**请求**:
```json
{
  "sceneId": "scene-uuid-001",
  "config": {
    "duration": 3600,
    "timeScale": 10.0,
    "seed": 12345
  }
}
```

**响应** (201 Created):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "simulationId": "sim-uuid-001",
    "sceneId": "scene-uuid-001",
    "status": "RUNNING",
    "startedAt": "2026-02-06T10:30:00Z"
  },
  "traceId": "req-xxx"
}
```

**POST /api/v1/simulations/{id}/pause** - 暂停仿真

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "simulationId": "sim-uuid-001",
    "status": "PAUSED",
    "simulatedTime": 123.45
  },
  "traceId": "req-xxx"
}
```

**POST /api/v1/simulations/{id}/resume** - 继续仿真

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "simulationId": "sim-uuid-001",
    "status": "RUNNING"
  },
  "traceId": "req-xxx"
}
```

**POST /api/v1/simulations/{id}/stop** - 停止仿真

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "simulationId": "sim-uuid-001",
    "status": "STOPPED",
    "simulatedTime": 123.45,
    "stoppedAt": "2026-02-06T10:35:00Z"
  },
  "traceId": "req-xxx"
}
```

**PUT /api/v1/simulations/{id}/speed** - 调整仿真速度

**请求**:
```json
{
  "timeScale": 50.0
}
```

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "simulationId": "sim-uuid-001",
    "timeScale": 50.0
  },
  "traceId": "req-xxx"
}
```

#### Scenario: 实体查询

**GET /api/v1/simulations/{id}/entities** - 查询所有实体

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "entities": [
      {
        "id": "OHT-01",
        "type": "OHTVehicle",
        "state": "MOVING",
        "position": {"x": 10.5, "y": 20.3, "z": 10.0},
        "properties": { ... }
      }
    ],
    "count": 10
  },
  "traceId": "req-xxx"
}
```

**GET /api/v1/simulations/{id}/entities/{entityId}** - 查询单个实体

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "OHT-01",
    "type": "OHTVehicle",
    "state": "MOVING",
    "position": {"x": 10.5, "y": 20.3, "z": 10.0},
    "currentTask": "TASK-001",
    "path": ["P1", "P2", "P3"],
    "battery": 0.85,
    "load": 25.0,
    "speed": 2.5,
    "utilization": 0.75
  },
  "traceId": "req-xxx"
}
```

#### Scenario: KPI 查询

**GET /api/v1/simulations/{id}/metrics** - 查询当前指标

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "simulatedTime": 123.45,
    "throughput": {
      "tasksCompleted": 42,
      "tasksPerHour": 120.5,
      "materialThroughput": 210.0
    },
    "utilization": {
      "average": 0.68,
      "byEntity": {
        "OHT-01": 0.75,
        "OHT-02": 0.61
      }
    },
    "wip": {
      "total": 15,
      "byLocation": {
        "STOCKER-01": 5,
        "MACHINE-05": 3
      }
    },
    "energy": {
      "total": 45.2,
      "byVehicle": { ... }
    }
  },
  "traceId": "req-xxx"
}
```

**GET /api/v1/simulations/{id}/metrics/history?from=0&to=3600** - 查询历史指标

**响应** (200 OK):
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "interval": 60,
    "points": [
      {
        "time": 60,
        "throughput": { ... },
        "utilization": { ... }
      },
      {
        "time": 120,
        "throughput": { ... },
        "utilization": { ... }
      }
    ]
  },
  "traceId": "req-xxx"
}
```

---

### Requirement: REQ-WEB-002 WebSocket 实时推送

系统MUST实现REQ-WEB-002（WebSocket 实时推送）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现仿真状态实时推送

#### WebSocket 消息契约

**连接**: `/api/v1/simulations/{id}/ws`

**统一消息结构**:
```json
{
  "type": "state.update",
  "seq": 1024,
  "simTime": 120.5,
  "ts": 1739012345678,
  "requestId": "req-xxx",
  "payload": {}
}
```

**消息类型**:
- `state.update`
- `entity.update`
- `kpi.update`
- `task.update`
- `control.ack`
- `paused` / `resumed`
- `completed`
- `error`
- `heartbeat`

**顺序保证**:
- `seq` 为单连接递增序号
- 客户端按 `seq` 处理乱序消息

#### Scenario: 连接建立
**Given** 一个运行中的仿真
**When** 客户端连接 WebSocket (/api/v1/simulations/{id}/ws)
**Then** 连接应建立成功
**And** 应发送初始状态快照

**服务端推送 (snapshot)**:
```json
{
  "type": "snapshot",
  "timestamp": "2026-02-06T10:30:00Z",
  "data": {
    "simulationId": "sim-uuid-001",
    "simulatedTime": 123.45,
    "status": "RUNNING",
    "timeScale": 10.0,
    "entities": [
      {
        "id": "OHT-01",
        "type": "OHTVehicle",
        "state": "MOVING",
        "position": {"x": 10.5, "y": 20.3, "z": 10.0},
        "currentTask": "TASK-001"
      }
    ]
  }
}
```

#### Scenario: 状态更新推送
**Given** 一个已连接的 WebSocket
**And** 仿真运行中
**When** 实体状态改变
**Then** 应推送状态更新消息
**And** 消息应包含实体 ID、新状态、时间戳

**服务端推送 (entity_update)**:
```json
{
  "type": "entity_update",
  "timestamp": "2026-02-06T10:30:05Z",
  "data": {
    "entityId": "OHT-01",
    "changes": {
      "state": "LOADING",
      "position": {"x": 15.0, "y": 20.3, "z": 10.0}
    }
  }
}
```

**批量更新模式** (timeScale > 10x):
```json
{
  "type": "entity_update",
  "timestamp": "2026-02-06T10:30:10Z",
  "data": {
    "batch": [
      {"entityId": "OHT-01", "changes": {...}},
      {"entityId": "OHT-02", "changes": {...}},
      {"entityId": "AGV-01", "changes": {...}}
    ]
  }
}
```

#### Scenario: 事件推送
**Given** 一个已连接的 WebSocket
**When** 仿真事件发生 (任务完成、冲突、告警)
**Then** 应推送事件消息
**And** 消息应包含事件类型、详细数据

**任务完成事件**:
```json
{
  "type": "event",
  "timestamp": "2026-02-06T10:30:15Z",
  "data": {
    "eventType": "TASK_COMPLETED",
    "taskId": "TASK-001",
    "vehicleId": "OHT-01",
    "completionTime": 123.45,
    "deadline": 150.0,
    "onTime": true
  }
}
```

**冲突事件**:
```json
{
  "type": "event",
  "timestamp": "2026-02-06T10:30:16Z",
  "data": {
    "eventType": "CONFLICT_DETECTED",
    "controlPointId": "CP1",
    "vehicles": ["OHT-01", "OHT-02"],
    "resolution": "OHT-01 prioritized"
  }
}
```

**告警事件**:
```json
{
  "type": "event",
  "timestamp": "2026-02-06T10:30:17Z",
  "data": {
    "eventType": "WARNING",
    "level": "HIGH",
    "message": "WIP threshold exceeded",
    "location": "STOCKER-01",
    "value": 150,
    "threshold": 100
  }
}
```

#### Scenario: 仿真状态变化
**Given** 一个已连接的 WebSocket
**When** 仿真暂停/恢复/停止
**Then** 应推送仿真状态消息

**暂停事件**:
```json
{
  "type": "simulation_status",
  "timestamp": "2026-02-06T10:30:20Z",
  "data": {
    "status": "PAUSED",
    "simulatedTime": 123.45,
    "reason": "USER_REQUEST"
  }
}
```

**停止事件**:
```json
{
  "type": "simulation_status",
  "timestamp": "2026-02-06T10:35:00Z",
  "data": {
    "status": "STOPPED",
    "simulatedTime": 456.78,
    "reason": "COMPLETED"
  }
}
```

#### Scenario: 心跳与重连
**Given** 一个已连接的 WebSocket
**When** 每 30 秒无其他消息
**Then** 应发送心跳

**心跳消息**:
```json
{
  "type": "heartbeat",
  "timestamp": "2026-02-06T10:30:30Z",
  "data": {}
}
```

**客户端重连**:
- 客户端自动重连，指数退避 (1s, 2s, 4s, 8s, 最大 30s)
- 重连成功后请求当前快照 (如果支持)

**客户端消息** (请求):
```json
{
  "type": "request",
  "timestamp": "2026-02-06T10:30:00Z",
  "data": {
    "action": "subscribe",
    "filters": {
      "entityTypes": ["OHTVehicle", "AGVVehicle"],
      "eventTypes": ["TASK_COMPLETED", "WARNING"]
    }
  }
}
```

---

### Requirement: REQ-WEB-003 3D 场景渲染

系统MUST实现REQ-WEB-003（3D 场景渲染）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 使用 Three.js 渲染 3D 场景

#### Scenario: 场景初始化
**Given** 一个加载的前端页面
**And** 场景数据
**When** 初始化 Three.js 场景
**Then** 应创建 3D 视口、相机、灯光
**And** 应加载场景中所有实体

#### Scenario: OHT 3D 模型
**Given** 场景中的 OHT 实体
**When** 渲染 OHT
**Then** 应显示 OHT 3D 模型
**And** 模型应包含轨道、车身、载货部分

#### Scenario: AGV 3D 模型
**Given** 场景中的 AGV 实体
**When** 渲染 AGV
**Then** 应显示 AGV 3D 模型
**And** 模型应反映 AGV 类型 (叉车、牵引车等)

#### Scenario: Stocker 3D 模型
**Given** 场景中的 Stocker
**When** 渲染 Stocker
**Then** 应显示立体仓库结构
**And** 应显示库位占用状态

#### Scenario: 轨道路网渲染
**Given** 场景中的轨道路网
**When** 渲染路网
**Then** 应显示轨道/路径线条
**And** 应标记控制点位置

---

### Requirement: REQ-WEB-004 实时动画

系统MUST实现REQ-WEB-004（实时动画）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现实时仿真动画

#### Scenario: 车辆移动动画
**Given** 运行中的仿真
**And** WebSocket 接收状态更新
**When** OHT/AGV 移动
**Then** 3D 模型应平滑移动到新位置
**And** 移动应插值 (避免跳变)

#### Scenario: 状态变化动画
**Given** 运行中的仿真
**When** 车辆状态改变 (如开始装载)
**Then** 应播放相应动画 (如升降、抓取)
**And** 应显示状态指示 (颜色、图标)

#### Scenario: 性能优化
**Given** 大规模场景 (500+ 实体)
**When** 渲染动画
**Then** 帧率应 ≥ 30 FPS
**And** 应使用 LOD、实例化渲染优化

---

### Requirement: REQ-WEB-005 场景编辑器

系统MUST实现REQ-WEB-005（场景编辑器）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 拖拽式场景编辑（基于 @react-three/fiber + drei 自建）

**技术架构**:
- 渲染引擎: three.js (v0.160+)
- React封装: @react-three/fiber (v8.x) + @react-three/drei (v9.x)
- 状态管理: Zustand
- 模型加载: useGLTF (支持GLB)
- 场景控制: OrbitControls / TransformControls

#### Scenario: 场景编辑器初始化
**Given** 打开的场景编辑器页面
**When** 页面加载完成
**Then** 应初始化 Canvas 组件
**And** 应显示 3D 视口（OrbitControls）
**And** 应显示参考网格（Grid）
**And** 应显示坐标轴辅助器（GizmoHelper）
**And** 应加载场景中已有实体

#### Scenario: 实体拖拽添加
**Given** 打开的场景编辑器
**And** 左侧实体面板（设备库）
**When** 拖拽 OHT/AGV/Stocker 实体到场景
**Then** 实体应添加到场景
**And** 应使用 useGLTF 加载对应 GLB 模型
**And** 应显示 TransformControls 变换控制器
**And** 应显示右侧属性面板

#### Scenario: 实体选中与变换
**Given** 场景中已有实体
**When** 点击选中实体
**Then** 应高亮显示选中状态
**And** 应显示 TransformControls
**And** 支持平移（Translate）、旋转（Rotate）、缩放（Scale）模式切换
**And** 变换结果应实时更新到属性面板

#### Scenario: 实体属性编辑
**Given** 选中的实体
**And** 右侧属性面板
**When** 修改属性 (如速度、容量)
**Then** 属性应更新
**And** 应验证输入有效性
**And** 实体状态应通过 Zustand store 同步

#### Scenario: 设备运输类型配置（v2.0）
**Given** 选中的设备实体（Machine, Stocker 等）
**And** 属性面板
**When** 配置 supportedTransportTypes
**Then** 应显示运输类型多选框（OHT, AGV, HUMAN, CONVEYOR）
**And** 至少选择一种运输类型
**And** 保存时校验字段不为空

#### Scenario: 工序配置多选设备（v2.0）
**Given** 编辑工艺流程
**And** 工序配置面板
**When** 配置工序的目标设备
**Then** 应支持多选设备（targetEntityIds）
**And** UI 应显示设备选择列表（支持搜索/筛选）
**And** 应允许选择 1 台或多台等价设备
**And** 保存前校验运输类型兼容性

#### Scenario: 运输类型兼容性校验提示（v2.0）
**Given** 工序配置 requiredTransportTypes = ["OHT", "AGV"]
**And** 用户选择了设备 Machine-A（supportedTransportTypes = ["OHT", "HUMAN"]）
**And** 用户选择了设备 Machine-B（supportedTransportTypes = ["AGV"]）
**When** 用户点击保存
**Then** 保存成功（交集非空）
**And** 系统显示"运输类型兼容：OHT, AGV"

#### Scenario: 运输类型不兼容错误提示（v2.0）
**Given** 工序配置 requiredTransportTypes = ["OHT"]
**And** 用户选择了设备 Machine-A（supportedTransportTypes = ["AGV", "HUMAN"]）
**When** 用户点击保存
**Then** 保存失败
**And** 系统显示错误："运输类型不匹配：工序需要 [OHT]，设备支持 [AGV, HUMAN]"
**And** 高亮显示不兼容的设备

#### Scenario: 多候选设备权重配置（v2.0）
**Given** 系统配置页面
**And** 多候选设备选择权重配置项
**When** 用户修改权重配置
**Then** 应显示 distanceWeight, timeWeight, wipWeight 输入框
**And** 权重和应等于 1.0
**And** 权重范围为 [0.0, 1.0]
**And** 配置保存到 system_config 表

#### Scenario: 路网编辑
**Given** 场景编辑器
**When** 添加/删除/连接节点
**Then** 路网应实时更新
**And** 应验证路网连通性
**And** 连线仅允许 `CONTROL_POINT -> CONTROL_POINT`
**And** 应支持“首点选择 -> 终点选择 -> 生成段”的状态机
**And** 应支持取消当前连线（ESC 或重复点击首点）

#### Scenario: 车辆节点吸附约束
**Given** 场景编辑器处于实体放置模式
**When** 放置 `OHT_VEHICLE` 或 `AGV_VEHICLE`
**Then** 车辆应自动吸附到最近合法节点
**And** 未命中合法节点时不应落库到场景数据
**And** 系统应给出可理解的提示信息

#### Scenario: 曲线路径绘制
**Given** 场景编辑器
**When** 绘制贝塞尔曲线路径
**Then** 应支持设置控制点
**And** 3D/2D 视图应以曲线渲染
**And** 贝塞尔控制点仅用于几何形状
**And** 交通控制点为独立配置（用于限流/优先级）
**And** 路径段需落库为 `segments`
**And** BEZIER 段结构为 `from/to/c1/c2`
**And** `from/to` 引用 points 中点 ID
**And** `c1/c2` 为绝对坐标（单位 m）

#### Scenario: 单位约束
**Given** 场景编辑器
**When** 输入坐标/角度
**Then** 坐标单位为米（m）
**And** 角度采用弧度制 [-π, π]

#### Scenario: 场景保存
**Given** 编辑的场景
**When** 点击保存
**Then** 场景应通过 API 保存
**And** 应显示保存成功提示
**And** 提交前必须执行运行态字段清理
**And** `PUT /api/v1/scenes/{id}` 请求体不得包含 `extensions`、`_` 前缀字段或临时编辑态字段

#### Scenario: GLB模型加载与缓存
**Given** 场景编辑器
**And** MinIO 中存储的 GLB 模型
**When** 加载实体对应的 GLB 模型
**Then** 应使用 useGLTF 钩子加载
**And** 模型应缓存到浏览器内存
**And** 相同模型复用实例（InstancedMesh 优化）

---

### Requirement: REQ-WEB-006 KPI Dashboard

系统MUST实现REQ-WEB-006（KPI Dashboard）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: KPI 仪表板

#### Scenario: 实时吞吐量图表
**Given** 运行中的仿真
**And** Dashboard 页面
**When** 仿真运行
**Then** 应显示实时吞吐量折线图
**And** 图表应随时间自动更新

#### Scenario: 利用率图表
**Given** 运行中的仿真
**When** 查看利用率
**Then** 应显示各车辆/设备的利用率条形图
**And** 应标记高/低利用率项

#### Scenario: 瓶颈报告展示
**Given** 完成的仿真
**When** 查看瓶颈报告
**Then** 应显示识别的瓶颈列表
**And** 应显示瓶颈严重程度

#### Scenario: 车辆配置建议
**Given** 完成的评估仿真
**When** 查看配置建议
**Then** 应显示推荐车辆数
**And** 应显示成本效益分析

---

### Requirement: REQ-WEB-007 实时派单界面

系统MUST实现REQ-WEB-007（实时派单界面）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 运行时任务管理界面

#### Scenario: 任务列表显示
**Given** 运行中的仿真
**When** 打开任务管理界面
**Then** 应显示所有待处理和进行中的任务
**And** 应显示任务状态、优先级、分配车辆

#### Scenario: 添加新任务
**Given** 任务管理界面
**When** 填写任务表单并提交
**Then** 任务应通过 API 创建
**And** 任务应出现在列表中

#### Scenario: 任务优先级调整
**Given** 任务列表
**And** 选中的任务
**When** 修改优先级
**Then** 优先级应更新
**And** 调度引擎应重新排序

#### Scenario: 紧急任务插入
**Given** 任务管理界面
**When** 插入紧急任务
**Then** 任务应标记为最高优先级
**And** Dashboard 应显示告警
**And** 调度引擎应在下一调度周期内处理（≤1s 仿真时间）

---

### Requirement: REQ-WEB-008 车辆监控界面

系统MUST实现REQ-WEB-008（车辆监控界面）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 车辆状态监控

#### Scenario: 车辆状态列表
**Given** 运行中的仿真
**When** 打开车辆监控
**Then** 应显示所有车辆及其状态
**And** 应显示当前位置、任务、电量

#### Scenario: 车辆轨迹显示
**Given** 3D 场景
**And** 选中的车辆
**When** 显示轨迹
**Then** 应在 3D 场景中绘制车辆历史路径
**And** 应标记起点、终点、途经点

#### Scenario: 手动控制
**Given** 车辆监控界面
**And** 选中的车辆
**When** 点击"停止"
**Then** 车辆应停止当前任务
**When** 点击"派发任务"
**Then** 应打开任务分配对话框

---

### Requirement: REQ-WEB-009 模型库管理

系统MUST实现REQ-WEB-009（模型库管理）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 统一管理可视化模型资源（GLB）

#### Scenario: 模型列表
**Given** 打开的模型管理页面
**When** 查询模型列表
**Then** 应返回所有可用模型（含类型、版本、状态、缩略图）

#### Scenario: 上传模型
**Given** 一个 GLB 模型文件
**When** 上传模型
**Then** 模型文件应存储到 MinIO
**And** 模型元数据应写入模型库

#### Scenario: 禁用模型
**Given** 一个已存在的模型
**When** 设置为禁用
**Then** 该模型不应再出现在默认可选列表中

---

### Requirement: REQ-WEB-010 模型版本与实体绑定

系统MUST实现REQ-WEB-010（模型版本与实体绑定）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 支持模型版本管理与实例级绑定

#### Scenario: 版本管理
**Given** 一个模型
**When** 上传新版本
**Then** 应生成版本号（如 v1.0.1）
**And** 可设置为默认版本

#### Scenario: 实体绑定模型
**Given** 一个场景实体（如 Stocker-01）
**When** 选择某个模型版本
**Then** 该实体应使用该模型版本渲染
**And** 同类型其他实体可使用不同模型

#### Scenario: 模型渲染优先级
**Given** 一个实体需要渲染
**When** 存在实体级模型绑定
**Then** 使用该绑定模型
**When** 不存在实体级模型绑定但存在默认模型
**Then** 使用默认模型（model_library.default_version）
**When** 既无绑定也无默认模型
**Then** 使用系统占位模型渲染（程序化几何体）

---

### Requirement: REQ-WEB-011 OHT 轨道与 AGV 路网共存

系统MUST实现REQ-WEB-011（OHT 轨道与 AGV 路网共存）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 在同一场景中同时渲染 OHT 轨道与 AGV 路网

#### Scenario: 路径分层渲染
**Given** 场景包含 OHT 轨道与 AGV 路网
**When** 渲染 3D 场景
**Then** OHT 轨道应显示在空中层（如 y=H）
**And** AGV 路网应显示在地面层（如 y=0）

#### Scenario: 路径数据存储
**Given** 场景 JSON 数据
**When** 定义路径
**Then** 应区分路径类型（OHT_PATH / AGV_NETWORK）

**示例**:
```json
{
  "paths": [
    {"id": "PATH-OHT-1", "type": "OHT_PATH", "points": [ ... ], "segments": [ ... ]},
    {"id": "PATH-AGV-1", "type": "AGV_NETWORK", "points": [ ... ], "segments": [ ... ]}
  ]
}
```
**And** 路径段以 `segments` 表示，`from/to` 引用 `points` 中点 ID
**And** 双向路段必须拆为两条反向段

#### Scenario: 路网编辑上下文隔离
**Given** 场景同时存在 OHT 与 AGV 路网
**When** 用户在 OHT 路网上下文中连线
**Then** 新增线段只能写入 `OHT_PATH`
**And** 不得跨类型自动串接到 `AGV_NETWORK`
**When** 用户切换到 AGV 路网上下文
**Then** 新增线段只能写入 `AGV_NETWORK`

---

### Requirement: REQ-WEB-012 2D 俯视图路径热力图

系统MUST实现REQ-WEB-012（2D 俯视图路径热力图）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 提供 2D 俯视图与路径热力图

#### Scenario: 热力图渲染
**Given** 仿真运行数据（路段流量/等待）
**When** 打开 2D 俯视图
**Then** 应显示路径热力图（颜色表示拥堵程度）
**And** 支持与 3D 场景联动定位

---

### Requirement: REQ-WEB-013 状态画板与刷新策略

系统MUST实现REQ-WEB-013（状态画板与刷新策略）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 统一状态展示与刷新频率

#### Scenario: 状态画板
**Given** 运行中的仿真
**When** 打开状态画板
**Then** 应展示实体状态与关键指标
**And** 状态集合为：IDLE / MOVING / LOADING / UNLOADING / CHARGING / BLOCKED / ERROR

#### Scenario: 刷新频率
**Given** 运行中的仿真
**When** 用户切换刷新频率
**Then** 支持 200ms / 500ms / 1000ms 三档
**And** 默认 500ms
**And** 仿真暂停时仅推送用户操作变化与心跳

---

### Requirement: REQ-WEB-014 场景导入

系统MUST实现REQ-WEB-014（场景导入）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 支持场景 JSON 导入

#### Scenario: 导入场景
**Given** 一个合法的场景 JSON 文件
**When** 用户执行导入
**Then** 场景应被创建为新场景
**And** 返回新场景 ID 与版本号

---

### Requirement: REQ-WEB-015 场景草稿与自动保存

系统MUST实现REQ-WEB-015（场景草稿与自动保存）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 支持场景编辑草稿与自动保存

#### Scenario: 自动保存草稿
**Given** 用户正在编辑场景
**When** 场景发生变更
**Then** 系统每 1 分钟自动保存草稿
**And** 若 1 分钟内无变化则不保存

#### Scenario: 草稿恢复
**Given** 已存在草稿
**When** 用户重新进入场景编辑器
**Then** 应提示是否恢复草稿

---

### Requirement: REQ-WEB-016 系统配置管理页面

系统MUST实现REQ-WEB-016（系统配置管理页面）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 提供可视化配置页面，维护全局/租户级参数

#### Scenario: 配置列表展示
**Given** 配置管理页面
**When** 打开页面
**Then** 应展示所有配置项（key/value/说明）

#### Scenario: 配置更新
**Given** 一个配置项
**When** 用户修改并保存
**Then** 配置应写入 system_config 表
**And** 修改结果应立即生效（无需重启）

#### Scenario: 配置分类与搜索
**Given** 系统配置页面
**When** 用户查看配置
**Then** 应按模块分类展示（交通/调度/仿真/前端/模型/草稿/分布）
**And** 支持关键字搜索与筛选
**And** 应展示每项配置的默认策略与当前值

---

### Requirement: REQ-WEB-017 车辆配置管理页面

系统MUST实现REQ-WEB-017（车辆配置管理页面）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 管理 OHT/AGV 车辆配置参数

#### Scenario: 车辆配置列表
**Given** 车辆配置页面
**When** 查看列表
**Then** 应按车辆类型分组展示

#### Scenario: 配置编辑
**Given** 一个车辆配置项
**When** 用户修改参数并保存
**Then** 配置应写入 vehicle_configs

---

### Requirement: REQ-WEB-018 日志级别配置页面

系统MUST实现REQ-WEB-018（日志级别配置页面）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 管理各模块日志级别

#### Scenario: 模块级配置
**Given** 日志级别页面
**When** 设置 traffic/scheduler/metrics/web/sim 的日志级别
**Then** 配置应写入 log_level_config

---

### Requirement: REQ-WEB-019 模型版本参数配置页面

系统MUST实现REQ-WEB-019（模型版本参数配置页面）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 配置模型版本的缩放/旋转/轴心

#### Scenario: 模型变换配置
**Given** 一个模型版本
**When** 配置 scale/rotation/pivot
**Then** 参数应保存到 model_versions
**And** rotation 使用弧度制 [-π, π]

---

### Requirement: REQ-WEB-020 前端模块清单

系统MUST实现REQ-WEB-020（前端模块清单）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 完整的前端功能模块清单，涵盖 8 大核心模块

#### 模块概览

| 模块 | 功能描述 | 主要交互 | 依赖后端 |
|------|----------|----------|----------|
| 1. 项目/场景管理 | 场景 CRUD、导入导出、草稿自动保存 | REST API | `/api/v1/scenes/*` |
| 2. 模型与组件库 | GLB 上传、版本管理、设备类型映射 | REST API + MinIO | `/api/v1/models/*` |
| 3. 场景编辑器 | OHT 轨道绘制、安全区配置、工序配置 | REST API | `/api/v1/scenes/*`, `/api/v1/flows/*` |
| 4. 仿真运行 | 启动/暂停/步进/重置、倍速控制 | REST + WebSocket | `/api/v1/simulations/*`, `/api/v1/simulations/{id}/ws` |
| 5. KPI 与分析 | 2D 热力图、任务追踪、回放 | REST + WebSocket | `/api/v1/metrics/*` |
| 6. 任务管理 | 任务列表、创建、优先级调整 | REST + WebSocket | `/api/v1/tasks/*` |
| 7. 配置中心 | 全局配置、调度权重、日志级别 | REST API | `/api/v1/config/*` |
| 8. 日志与事件 | 事件流查看、错误提示 | WebSocket | `/api/v1/simulations/{id}/ws` |

#### 技术栈

- **前端框架**: React 18 + Vite
- **3D 渲染**: three.js + @react-three/fiber + @react-three/drei
- **样式**: Tailwind CSS
- **图表**: Chart.js
- **状态管理**: Zustand
- **通信**: REST API (axios) + WebSocket (native)

**3D编辑器技术架构**:

```
┌─────────────────────────────────────────────────────────────────┐
│                    前端3D技术架构                                 │
├─────────────────────────────────────────────────────────────────┤
│  渲染引擎：  three.js (v0.160+)                                  │
│  React封装： @react-three/fiber (v8.x) + @react-three/drei (v9.x)│
│  状态管理：  Zustand (轻量级，适合3D场景状态)                      │
│  模型加载：  useGLTF (drei内置，支持GLB/GLTF)                     │
│  场景控制：  OrbitControls / TransformControls (drei内置)        │
│  序列化：    JSON → 后端存储                                      │
└─────────────────────────────────────────────────────────────────┘
```

**核心依赖 (MIT许可证)**:
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
1. **MIT许可证** - 商业友好，无AGPL-3.0限制
2. **React原生生态** - 与现有技术栈无缝集成
3. **社区活跃** - pmndrs团队持续维护
4. **GLB原生支持** - drei内置useGLTF

**参考实现**: Three.js Editor (github.com/mrdoob/three.js/tree/dev/editor)

---

### Requirement: REQ-WEB-021 事件流查看器

系统MUST实现REQ-WEB-021（事件流查看器）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实时查看仿真事件流，支持筛选和搜索

#### Scenario: 事件流列表显示
**Given** 运行中的仿真
**And** 已连接 WebSocket
**When** 打开事件流查看器
**Then** 应显示事件列表（时间、类型、级别、描述）
**And** 按时间倒序排列
**And** 不同级别使用不同颜色（INFO=蓝、WARNING=黄、ERROR=红）

#### Scenario: 事件筛选
**Given** 事件流查看器
**And** 多种事件类型（TASK_COMPLETED、CONFLICT_DETECTED、WARNING）
**When** 用户筛选只显示 WARNING 级别事件
**Then** 列表只显示 WARNING 事件
**And** 其他事件被隐藏但不删除

#### Scenario: 事件搜索
**Given** 事件流查看器
**And** 事件流中包含 100+ 条事件
**When** 用户搜索 "OHT-01"
**Then** 只显示包含 "OHT-01" 的事件
**And** 高亮匹配关键词

#### Scenario: 事件详情
**Given** 事件流查看器
**And** 用户点击某个事件
**When** 查看事件详情
**Then** 应显示完整事件数据（JSON 格式）
**And** 支持复制到剪贴板

---

### Requirement: REQ-WEB-022 SafetyZone 编辑器

系统MUST实现REQ-WEB-022（SafetyZone 编辑器）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 可视化编辑安全区（CIRCLE/RECT/POLYGON）

#### Scenario: 圆形安全区创建
**Given** 场景编辑器
**When** 用户选择创建圆形安全区
**And** 点击设置圆心、拖拽设置半径
**Then** 应在 3D 场景中预览圆形区域
**And** 保存后 SafetyZone 作为实体落入 `scenes.entities` 数组（type = "SAFETY_ZONE"）

#### Scenario: 矩形安全区创建
**Given** 场景编辑器
**When** 用户选择创建矩形安全区
**And** 拖拽设置矩形边界
**Then** 应在 3D 场景中预览矩形区域
**And** 显示矩形四角坐标

#### Scenario: 多边形安全区创建
**Given** 场景编辑器
**When** 用户选择创建多边形安全区
**And** 依次点击多个顶点
**And** 双击完成绘制
**Then** 应在 3D 场景中预览多边形区域
**And** 至少需要 3 个顶点
**And** 保存后 SafetyZone 作为实体落入 `scenes.entities` 数组（type = "SAFETY_ZONE"）

#### Scenario: 安全区优先级配置
**Given** 选中的安全区
**And** 属性面板
**When** 配置 accessPriority
**Then** 应显示下拉框：HUMAN_FIRST / VEHICLE_FIRST / FIFO / PRIORITY_BASED
**And** 默认值为 HUMAN_FIRST

---

### Requirement: REQ-WEB-023 任务回放功能

系统MUST实现REQ-WEB-023（任务回放功能）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 支持仿真运行回放，查看历史状态

#### Scenario: 回放控制
**Given** 已完成的仿真
**When** 打开回放界面
**Then** 应显示时间轴滑块
**And** 支持播放/暂停/拖动进度

#### Scenario: 回放状态查询
**Given** 回放界面
**When** 拖动到时间 T
**Then** 应调用 REST API 查询时间 T 的状态快照
**And** 3D 场景应更新到该时间点的状态

---

### Requirement: REQ-WEB-024 图层与可见性控制

系统MUST实现REQ-WEB-024（图层与可见性控制）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 3D 场景图层管理，支持显示/隐藏不同类型实体

#### Scenario: 图层列表
**Given** 3D 场景查看器
**When** 打开图层面板
**Then** 应显示图层列表：OHT 轨道、AGV 路网、设备、安全区、车辆

#### Scenario: 图层显示/隐藏
**Given** 图层面板
**When** 用户取消勾选 "OHT 轨道"
**Then** 3D 场景中隐藏 OHT 轨道
**And** 再次勾选则重新显示

#### Scenario: 图层透明度
**Given** 图层面板
**When** 用户调整 "设备" 图层透明度
**Then** 3D 场景中设备模型应按透明度渲染
**And** 透明度范围 0% - 100%

---

### Requirement: REQ-WEB-025 草稿自动保存机制

系统MUST实现REQ-WEB-025（草稿自动保存机制）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 场景编辑时自动保存草稿，防止数据丢失

#### Scenario: 自动保存触发
**Given** 用户正在编辑场景
**When** 用户修改任意配置（实体、路径、安全区等）
**And** 距离上次保存超过 1 分钟
**Then** 系统自动保存草稿
**And** 显示 "已自动保存" 提示（3 秒后消失）

#### Scenario: 无变化不保存
**Given** 用户打开场景编辑器
**When** 用户未进行任何修改
**And** 经过 1 分钟
**Then** 不触发自动保存

#### Scenario: 草稿恢复提示
**Given** 存在未发布的草稿
**When** 用户重新进入场景编辑器
**Then** 应弹出提示："检测到未保存的草稿，是否恢复？"
**And** 用户选择"恢复"则加载草稿内容
**And** 用户选择"放弃"则删除草稿

---

### Requirement: REQ-WEB-026 全局导航与模块可达性

系统MUST实现REQ-WEB-026（全局导航与模块可达性）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 提供统一导航入口，减少通过手动修改 URL 进入模块页面

#### Scenario: 顶部导航栏可见
**Given** 用户进入前端系统任意页面
**When** 页面完成加载
**Then** 应显示全局导航栏
**And** 至少包含：场景列表(`/scenes`)、新建场景(`/scenes/new`)、模型库(`/models`)、仿真运行(`/simulation`)、配置中心(`/config`)

#### Scenario: 导航切换页面
**Given** 全局导航栏
**When** 用户点击某个导航项（如“模型库”）
**Then** 应跳转到对应路由页面
**And** 保持当前页面的可访问性和状态一致性

#### Scenario: 当前路由高亮
**Given** 用户当前在某个模块页面
**When** 导航栏渲染
**Then** 当前模块对应导航项应高亮显示
**And** 其他导航项保持未选中样式

---

### Requirement: REQ-WEB-027 前端文案国际化（i18n）统一规范

系统MUST实现REQ-WEB-027（前端文案国际化（i18n）统一规范）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 所有可见 UI 文案必须走统一国际化机制，禁止在页面组件中硬编码文本。

#### Scenario: 默认语言与语言来源
**Given** 用户首次进入系统
**When** 前端初始化 i18n
**Then** 默认语言应为 `en`
**And** 语言解析优先级应为：`localStorage` > 默认值
**And** 用户切换语言后应持久化到 `localStorage`

#### Scenario: 导航与按钮文案国际化
**Given** 全局导航栏与页面操作区
**When** 页面渲染
**Then** 导航项、按钮文案必须通过 i18n key 渲染
**And** 不允许在组件中直接写死中文或英文文本

#### Scenario: 表单与筛选文案国际化
**Given** 任意表单或筛选区域
**When** 页面渲染输入控件
**Then** label、placeholder、tooltip、help text 必须走 i18n
**And** 筛选项名称与空状态文案必须走 i18n

#### Scenario: 反馈文案国际化
**Given** 用户触发提示反馈
**When** 出现 toast、error、empty、loading 文案
**Then** 反馈文案必须通过 i18n key 渲染
**And** 缺失翻译时应回退到默认语言文案

#### Scenario: 工程约束
**Given** 前端代码评审与验收
**When** 提交 UI 相关改动
**Then** 新增可见文本必须附带 i18n key
**And** 不符合 i18n 规范的硬编码文本应视为不通过

---

## 交叉引用

- 依赖 `logistics-entities` 规范: 需要实体数据
- 依赖 `traffic-control` 规范: 需要交通状态数据
- 依赖 `dispatch-system` 规范: 需要任务调度数据
- 依赖 `kpi-metrics` 规范: 需要 KPI 数据
