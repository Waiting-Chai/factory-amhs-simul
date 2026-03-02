# 物流仿真模块 - 架构设计文档

## 文档信息
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Version**: 1.0

---

## 1. 设计原则

### 1.1 组合优于继承

遵循项目核心设计原则，实体模型**优先使用组合，允许适当继承**：

**原则**:
- 能力接口使用组合 (`TransportCapability`, `BatteryCapability`)
- 公共状态和方法可抽象为基类 (`LogisticsEntity`)
- 避免深层继承，继承层次不超过 2 层

```java
// 不推荐：深层继承
public class OHTVehicle extends Vehicle extends TransportDevice extends Entity ...

// 推荐：组合 + 轻量继承
public abstract class LogisticsEntity {
    protected String id;
    protected Position position;
    // 公共状态和方法
}

public class OHTVehicle extends LogisticsEntity {
    private final TransportCapability transport; // 组合运输能力
    private final TrackMovement movement;        // 组合轨道移动
    private final BatteryCapability battery;     // 组合电池能力 (如需要)
}
```

**优势**:
- 能力可独立变化
- 易于测试 (可 Mock 各个能力)
- 支持运行时能力组合
- 保留继承的类型判别便利性

### 1.2 接口驱动

所有核心能力定义为接口：

```java
public interface TransportCapability {
    void move(Position destination);
    void load(Cargo cargo);
    void unload();
}

public interface ConflictAware {
    boolean checkConflict(Position position);
    void reserveControlPoint(String cpId);
    void releaseControlPoint(String cpId);
}
```

### 1.3 事件驱动架构

基于 JSimul 的 `Environment` 和 `Event` 模型：

```java
// 车辆移动是事件驱动的
env.process(() -> {
    yield env.timeout(10);  // 行驶 10 秒
    onArrival();            // 到达回调
});
```

---

## 2. 核心架构决策

### 2.1 实体模型架构

#### 决策点 1: 实体状态表示

**选项 A**: 使用枚举状态机
```java
public enum VehicleState { IDLE, MOVING, LOADING, UNLOADING, CHARGING, BLOCKED }
```

**选项 B**: 使用状态模式
```java
public interface VehicleState {
    void enter(Vehicle vehicle);
    void update(Vehicle vehicle);
    void exit(Vehicle vehicle);
}
```

**选择**: **选项 A (枚举 + 状态转换表)**

**理由**:
- 状态数量有限且相对固定
- 实现简单，调试方便
- 易于序列化和可视化
- 与 FlexSim 的状态模型一致
- 无额外依赖，保持模块轻量

**实现**: 使用状态转换表管理合法转换
```java
public class StateTransitionTable {
    private final Map<StateTransition, VehicleState> transitions;

    public boolean canTransition(VehicleState from, VehicleState to) {
        return transitions.containsKey(new StateTransition(from, to));
    }
}
```

#### 决策点 2: 路径表示

**选项 A**: 邻接矩阵
```java
double[][] adjacencyMatrix; // matrix[i][j] = cost
```

**选项 B**: 邻接表
```java
Map<String, List<Edge>> adjacencyList;
```

**选项 C**: 拓扑图库 (JGraphT, Guava Graph)

**选择**: **选项 C (JGraphT)**

**理由**:
- JGraphT 提供成熟的图算法 (最短路径、连通性)
- 支持动态图修改
- 社区活跃，文档完善
- 性能足够 (非实时系统)

#### 决策点 2.1: 路网约束模型

**选择**: **EdgeConstraint 动态查询接口**

**实现**:
```java
// Edge 仅存储拓扑结构
public class Edge {
    private final String id;
    private final String from;
    private final String to;
    private final double baseWeight;  // 基础权重 (距离)
}

// 双向路段必须拆成两条有向 Edge，方向相反，edgeId 独立

// 约束通过接口动态查询
public interface EdgeConstraintProvider {
    int getCapacity(String edgeId);
    double getSpeedLimit(String edgeId, VehicleType vehicleType);
    boolean isTraversable(String edgeId, Vehicle vehicle);
    // 支持时段约束、车种限制等扩展
    boolean isTraversable(String edgeId, Vehicle vehicle, long simulatedTime);
}

// 默认实现
public class DefaultEdgeConstraintProvider implements EdgeConstraintProvider {
    private final Map<String, EdgeConstraints> constraints;

    @Override
    public boolean isTraversable(String edgeId, Vehicle vehicle, long simulatedTime) {
        EdgeConstraints c = constraints.get(edgeId);
        if (c == null) return true;
        return c.isTraversable(vehicle.getType(), simulatedTime);
    }
}
```

**优势**:
- 路网拓扑与约束解耦
- 支持多来源约束（静态配置 + 实时管制）
- 易于扩展新约束类型（时段限制、车种限制）

#### 决策点 2.2: 路径规划与资源预占分离

**选择**: **PathPlanner 仅负责计算路径，ReservationManager 负责资源预占**

**实现**:
```java
// PathPlanner 只返回路径
public interface PathPlanner {
    Path planPath(String from, String to, VehicleType vehicleType);
}

public class Path {
    private final List<String> nodes;
    private final double estimatedTime;
    // 不包含预占信息
}

// ReservationManager 独立管理资源预占（即时占用模型）
public interface ReservationManager {
    ReservationResult reserve(Path path, Vehicle vehicle);
    void release(String reservationId);
}

public class ReservationResult {
    private final boolean success;
    private final String reservationId;
    private final List<String> reservedResources;  // 已占用的资源列表（ControlPoint/Edge ID）
}
```

**优势**:
- 职责清晰：PathPlanner 算路，ReservationManager 管资源
- 采用即时占用模型：资源在授权后立即被占用（非时间窗预约）
- 路径算法可替换不影响管制逻辑

---

## 2.5 全局单位规范

**坐标单位**: 米（m）
**角度单位**: 弧度制，范围 [-π, π]
所有路径、坐标、旋转与速度计算必须遵循统一单位。

#### 决策点 2.3: EdgeReservation 双层管制

**选择**: **ControlPoint + Edge 双层控制，即时占用模型**

**实现**:
```java
// EdgeReservation 路段占用控制
public class EdgeReservation {
    private final String edgeId;
    private final int capacity;  // 路段容量（整数，默认 1，可配置）

    public boolean tryReserve(Vehicle vehicle) {
        if (usedCapacity < capacity) {
            usedCapacity++;
            return true;
        }
        return false;
    }

    public void release(Vehicle vehicle) {
        usedCapacity--;
        notifyWaiting();
    }
}

// TrafficManager 双层协调
public class TrafficManager {
    public synchronized boolean tryEnter(Vehicle vehicle, String cpId, String edgeId) {
        // 资源申请顺序：先申请 ControlPoint，再申请 Edge
        ControlPoint cp = controlPoints.get(cpId);
        if (!cp.hasCapacity()) {
            return false;  // ControlPoint 不可用
        }

        EdgeReservation edge = edgeReservations.get(edgeId);
        if (!edge.tryReserve(vehicle)) {
            return false;  // Edge 不可用
        }

        // 两者都可用时，才占用资源（避免死锁）
        cp.enter(vehicle);
        return true;
    }
}

// 故障车辆资源占用策略
public class VehicleFailureHandler {
    public void handleFailure(Vehicle vehicle) {
        // 1. 保留当前占用资源（ControlPoint/Edge）
        String currentCp = vehicle.getCurrentControlPoint();
        String currentEdge = vehicle.getCurrentEdge();

        // 2. 释放未来预占资源
        vehicle.releaseFutureReservations();

        // 3. 触发维修任务
        dispatchRepairTask(vehicle);

        // 4. 维修完成后释放占用资源
        vehicle.onRepairComplete(() -> {
            releaseResources(currentCp, currentEdge);
        });
    }
}
```

**优势**:
- 双层管制：ControlPoint 控制关键节点，Edge 控制路段容量
- 资源申请顺序：先 ControlPoint 后 Edge，避免部分占用导致的死锁
- 故障处理：保留当前占用，释放未来预占，维修完成后释放
- 容量配置：优先使用 system_config 配置，未配置则使用默认值

#### 决策点 2.4: 优先级管理（基于仿真时钟）

**选择**: **有效优先级 = 基础优先级 + 老化提升**

**实现**:
```java
public class PriorityManager {
    private final double agingStep;    // 老化步长（秒），默认 30s 仿真时间
    private final int agingBoost;      // 每次提升量，默认 1
    private final int maxBoost;        // 最大提升量，默认 5

    public int getEffectivePriority(Vehicle vehicle) {
        int basePriority = vehicle.getBasePriority();
        long waited = env.now() - vehicle.getWaitStartTime();  // 仿真时钟
        int aging = (int) Math.floor(waited / agingStep) * agingBoost;
        return basePriority + Math.min(aging, maxBoost);
    }

    // 优先级队列按有效优先级排序
    public void enqueueWaitingVehicle(Vehicle vehicle, String resourceId) {
        PriorityQueue<WaitingVehicle> queue = waitingQueues.get(resourceId);
        queue.enqueue(new WaitingVehicle(vehicle, getEffectivePriority(vehicle)));
    }
}

// system_config 配置项（REQ-TC-000）
public class TrafficConfig {
    // 容量配置
    int controlpoint_default_capacity = 1;
    int controlarea_default_capacity = 3;
    int edge_default_capacity = 1;
    String edge_release_policy = "full_body";  // full_body / any_part

    // 预占模式
    String traffic_reservation_mode = "immediate";  // immediate（即时占用）

    // 死锁检测
    String traffic_deadlock_strategy = "wait_graph+timeout";
    int traffic_deadlock_timeout = 60;  // 秒（仿真时间）

    // 重规划
    int traffic_replan_timeout = 60;  // 秒（仿真时间）
    int traffic_replan_max_attempts = 3;  // 最大重规划次数

    // 优先级老化
    boolean traffic_priority_aging_enabled = true;
    int traffic_priority_aging_step = 30;  // 秒（仿真时间）
    int traffic_priority_aging_boost = 1;
    int traffic_priority_aging_max = 5;

    // 处理模式
    String traffic_processing_mode = "event";  // event（事件驱动）
}
```

**配置生效时机**:
- system_config 变更后立即生效（无需重启）

**阻塞超时重规划**:
```java
public class ReplanHandler {
    private final long timeout;  // 阻塞超时阈值（秒）
    private final int maxAttempts;

    public void checkBlocked(Vehicle vehicle) {
        long waited = env.now() - vehicle.getBlockStartTime();
        if (waited >= timeout && vehicle.getReplanCount() < maxAttempts) {
            triggerReplan(vehicle);  // 触发重规划
            vehicle.incrementReplanCount();
        }
    }
}
```

#### 决策点 2.5: 曲线路径支持（Bezier）

**选择**: **路径段支持 LINEAR 与 BEZIER 两种类型**

**实现**:
```java
public enum PathSegmentType {
    LINEAR,  // 直线段
    BEZIER   // 贝塞尔曲线段
}

public class PathSegment {
    private final String id;
    private final PathSegmentType type;

    // 端点引用（指向 points 集合中的点 ID）
    private final String from;
    private final String to;

    // 贝塞尔曲线段（控制点仅用于几何形状，不等同于交通控制点）
    private final Point c1;  // 控制点 1（绝对坐标）
    private final Point c2;  // 控制点 2（绝对坐标）

    public double calculateLength() {
        if (type == PathSegmentType.LINEAR) {
            return resolvePoint(from).distanceTo(resolvePoint(to));
        } else {
            return calculateBezierLength(resolvePoint(from), c1, c2, resolvePoint(to));
        }
    }
}
```

**单位规范**:
- 坐标单位：米
- 角度单位：弧度，范围 [-π, π]
- 贝塞尔控制点仅用于渲染和路径长度计算，不等同于交通控制点
- BEZIER 段结构为 from/to/c1/c2；LINEAR 段仅使用 from/to

---

### 2.6 交通控制架构

#### 决策点 3: 冲突检测策略

**选项 A**: 空间分区 (Spatial Partitioning / Grid)
```java
Grid2D<ControlPoint> grid = new Grid2D<>(resolution);
```

**选项 B**: 控制点/控制区 (FlexSim 方式)
```java
ControlPoint cp = new ControlPoint(id, capacity);
```

**选项 C**: 物理引擎 (JBox2D)

**选择**: **选项 B (控制点/控制区)**

**理由**:
- 对标 FlexSim，易于理解
- 适合工业场景 (道路/轨道是离散的)
- 不需要精确物理碰撞
- 性能可控

**实现**:
```java
public class TrafficManager {
    private final Map<String, ControlPoint> controlPoints;
    private final Map<String, ControlArea> controlAreas;

    public synchronized boolean tryEnter(Vehicle vehicle, String cpId) {
        ControlPoint cp = controlPoints.get(cpId);
        if (cp.hasCapacity()) {
            cp.enter(vehicle);
            return true;
        }
        return false;
    }
}
```

#### 决策点 4: 冲突解决策略

**选项 A**: 先到先得 (FIFO)
**选项 B**: 优先级 + FIFO
**选项 C**: 预测性避让 (预测未来冲突)

**选择**: **选项 B (优先级 + FIFO)**

**理由**:
- 支持紧急任务
- 实现简单
- 与 FlexSim 一致

**后续增强**: 可扩展为选项 C (预测性)

### 2.7 调度架构

#### 决策点 5: 任务分配策略

**选项 A**: 集中式调度器
```java
public class CentralDispatcher {
    public void assignTask(Task task, Vehicle vehicle) { ... }
}
```

**选项 B**: 分布式协商 (Contract Net Protocol)
**选项 C**: 市场机制 (拍卖)

**选择**: **选项 A (集中式)**

**理由**:
- 适合工厂环境 (中心化控制)
- 全局最优 (可计算)
- 易于监控和调试
- 与现有 MES/ERP 架构一致

**实现**: 自定义规则引擎，支持扩展
```java
public interface DispatchRule {
    String getName();
    Vehicle selectVehicle(Task task, List<Vehicle> candidates);
}

// 内置规则：最短距离、最高优先级、最低利用率
// 用户可通过实现接口自定义规则
```

#### 决策点 6: 路径规划算法

**选项 A**: Dijkstra
**选项 B**: A* (A-Star)
**选项 C**: D* (Dynamic A*)

**选择**: **选项 B (A*)**

**理由**:
- 性能优于 Dijkstra (有启发式)
- 静态路网足够 (A* 即可)
- 代码库成熟

**特殊情况**: 如需动态避障，后期可升级到 D* Lite

### 2.8 前后端通信架构

#### 决策点 7: 数据传输协议

**选项 A**: RESTful API (轮询)
**选项 B**: WebSocket (推送)
**选项 C**: gRPC (流式)

**选择**: **选项 B (WebSocket 主 + REST 辅)**

**理由**:
- WebSocket: 实时推送仿真状态 (高频更新)
- REST: 控制命令 (低频操作)
- 混合架构兼顾实时性和控制性

**实现**:
```java
@ServerEndpoint("/api/v1/simulations/{id}/ws")
public class SimulationWebSocket {
    @OnMessage
    public void onMessage(String message) { ... }

    public void broadcast(SimulationState state) { ... }
}
```

#### 2.8.1 REST API 规范

**统一响应结构**:
```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "req-xxx"
}
```

**SimulationController**
```
POST   /api/v1/simulations                         # 创建仿真（sceneId + config）
POST   /api/v1/simulations/{id}/start              # 启动
POST   /api/v1/simulations/{id}/pause              # 暂停
POST   /api/v1/simulations/{id}/resume             # 恢复
POST   /api/v1/simulations/{id}/stop               # 停止
GET    /api/v1/simulations/{id}                    # 查询仿真详情
GET    /api/v1/simulations/{id}/kpi                # KPI 汇总
```

**EntityController**
```
POST   /api/v1/scenes                              # 创建/保存场景
GET    /api/v1/scenes/{id}                         # 获取场景
POST   /api/v1/scenes/{id}/import                  # 导入场景
GET    /api/v1/entities/{id}                       # 获取实体
PATCH  /api/v1/entities/{id}                       # 更新实体属性
POST   /api/v1/entities/batch                      # 批量更新实体
```

**MetricsController**
```
GET    /api/v1/metrics/simulations/{id}/summary    # 指标汇总
GET    /api/v1/metrics/simulations/{id}/timeseries # 时序指标（metric/from/to）
GET    /api/v1/metrics/simulations/{id}/bottlenecks# 瓶颈分析
```

**ModelController**
```
GET    /api/v1/models                              # 模型列表（分页、类型筛选、版本筛选）
POST   /api/v1/models/upload                       # 上传 GLB 模型文件（存储到 MinIO）
GET    /api/v1/models/{id}                         # 获取模型详情
PUT    /api/v1/models/{id}                         # 更新模型元数据（type、版本、尺寸、锚点）
POST   /api/v1/models/{id}/versions                # 上传新版本
PUT    /api/v1/models/{id}/versions/{versionId}    # 设为默认版本
PATCH  /api/v1/models/{id}/enable                  # 启用模型
PATCH  /api/v1/models/{id}/disable                 # 禁用模型
DELETE /api/v1/models/{id}                         # 删除模型
```

#### 2.8.2 WebSocket 消息协议

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

**服务端推送类型**:
```
state.update       # 仿真整体状态
entity.update      # 实体位置/状态变化
kpi.update         # KPI 更新
task.update        # 任务状态变化
control.ack        # 控制命令回执
paused/resumed     # 暂停/恢复事件
completed          # 仿真完成
error              # 错误消息
heartbeat          # 心跳
```

**客户端上行类型**:
```
control.start | control.pause | control.resume | control.stop
task.create
entity.update
subscribe (订阅通道/过滤条件)
```

**顺序保证**:
- `seq` 为单连接递增序号
- 前端按 `seq` 处理乱序消息

#### 决策点 8: 3D 渲染技术

**选项 A**: Three.js (原生)
**选项 B**: Babylon.js
**选项 C**: WebGL 原生
**选项 D**: @react-three/fiber + drei (React封装)

**选择**: **选项 D (@react-three/fiber + @react-three/drei)**

**理由**:
- React原生生态，组件化开发体验
- 社区活跃 (pmndrs团队维护，@react-three/fiber 20k+ stars)
- 文档完善，示例丰富
- 性能优秀，支持实例化渲染
- 内置GLB模型加载 (useGLTF)
- 内置场景控制 (OrbitControls, TransformControls)
- MIT许可证，商业友好

**技术架构**:
```
three.js (底层渲染)
    ↓
@react-three/fiber (React声明式封装)
    ↓
@react-three/drei (实用工具库)
    ↓
自建场景编辑器 (业务层)
```

**关键依赖**:
| 包名 | 版本 | 用途 |
|------|------|------|
| three | ^0.160.0 | 底层3D渲染引擎 |
| @react-three/fiber | ^8.15.0 | React声明式3D |
| @react-three/drei | ^9.88.0 | 工具库(GLTF加载、控制器等) |
| zustand | ^4.4.0 | 轻量状态管理 |
| @types/three | ^0.160.0 | TypeScript类型 |

**GLB模型加载示例**:
```tsx
import { useGLTF } from '@react-three/drei'

function Model({ url }: { url: string }) {
  const { scene } = useGLTF(url)
  return <primitive object={scene} />
}
```

**场景编辑器核心示例**:
```tsx
import { Canvas } from '@react-three/fiber'
import { OrbitControls, TransformControls, Grid, GizmoHelper } from '@react-three/drei'

function SceneEditor() {
  const selected = useStore(state => state.selectedEntity)

  return (
    <Canvas>
      <OrbitControls makeDefault />
      <Grid infiniteGrid />

      {/* 选中实体的变换控制 */}
      {selected && (
        <TransformControls object={selected}>
          <Model url={selected.modelUrl} />
        </TransformControls>
      )}

      <GizmoHelper alignment="bottom-right" margin={[80, 80]}>
        <GizmoViewport />
      </GizmoHelper>
    </Canvas>
  )
}
```

**自建编辑器 vs 现成方案**:

| 方案 | 许可证 | React生态 | 运行时编辑 | 推荐度 |
|------|--------|-----------|-----------|--------|
| **自建 (R3F)** | MIT ✅ | 原生支持 ✅ | ✅ | ⭐⭐⭐⭐⭐ |
| vis-three | AGPL-3.0 ❌ | Vue ❌ | ✅ | ❌ |
| react-three-editor | MIT ✅ | 原生 ✅ | ❌ 仅开发时 | ⭐⭐ |
| triplex | MIT ✅ | R3F ✅ | ❌ VS Code扩展 | ⭐⭐ |

**调研结论**: 选择自建编辑器，基于@react-three/fiber + drei，参考Three.js Editor的设计模式

#### 决策点 8.1: 场景编辑器与现有后端契约对齐（强制）

**选择**: 前端编辑交互增强，但不修改既有场景接口契约。

**对齐约束**:
- 仅使用既有接口：`GET/PUT /api/v1/scenes/{id}` 与 draft 接口。
- 路网类型只允许：`OHT_PATH`、`AGV_NETWORK`（与现有 Scene DTO 保持一致）。
- 路径段结构统一：`segments[].{id,type,from,to,c1,c2}`，其中 `type ∈ {LINEAR, BEZIER}`。
- 节点实体统一：`CONTROL_POINT` 作为可连线节点，连线仅允许节点到节点。
- 坐标语义统一：Y 轴为高度；AGV 地面层 `y=0`，OHT 可使用高架层 `y>0`。
- 保存前必须 sanitize：剔除前端运行态字段（`extensions`、`_` 前缀、临时编辑态）。

**实现策略**:
1. 工具栏增加“路网类型上下文”（OHT/AGV），新建线段继承当前上下文。
2. 连线采用两段式状态机：首点选择 -> 终点选择；支持 ESC/再次点击取消。
3. 车辆放置采用“节点吸附”策略：落点时寻找最近合法节点并绑定。
4. 前端提交场景前执行白名单序列化，确保请求体仅含业务字段。

**DoD**:
- 编辑器可完成：放置节点 -> 连接路径 -> 绑定车辆 -> 保存 -> 刷新还原。
- 抓包验证：`PUT /api/v1/scenes/{id}` 请求体不含运行态字段。
- 契约验证：`docs/frontend-contract.md` 无需新增或变更字段语义。

#### 决策点 9: 仿真时钟模型

**选项 A**: 纯离散事件仿真 (DES)
- 仿真时间完全独立于墙钟
- 可快速仿真 (1 小时场景 → 几秒计算)
- 不支持实时交互

**选项 B**: 实时仿真 (Real-time)
- 仿真时间与墙钟 1:1 同步
- 支持真实人员交互、实时派单
- 运行时间长

**选项 C**: 混合可调 (Hybrid Adjustable)
- 实时因子可配置 (0.1x ~ 100x)
- 支持调试时加速、演示时实时
- 支持运行时动态切换

**选择**: **选项 C (混合可调)**

**理由**:
- 同时支持纯 DES 批量分析和实时交互演示
- 灵活性最高，满足不同使用场景
- 可根据需要在运行时调整速度

**实现**:
```java
public class SimulationClock {
    private double timeScale = 1.0;  // 实时因子，1.0 = 实时，10.0 = 10倍速
    private long simTime;            // 仿真时间 (秒)
    private long lastWallTime;       // 上次墙钟时间 (毫秒)

    public void tick() {
        long now = System.currentTimeMillis();
        long deltaWallMs = now - lastWallTime;
        double deltaSimSec = (deltaWallMs / 1000.0) * timeScale;
        simTime += deltaSimSec;
        lastWallTime = now;
    }

    public void setTimeScale(double scale) {
        this.timeScale = scale;
    }
}
```

**WebSocket 推送策略**:
- **纯 DES 模式** (timeScale > 10x): 降低推送频率，批量推送状态变化
- **实时模式** (timeScale ≈ 1x): 每个状态变化即时推送
- **暂停时**: 发送暂停事件，停止状态推送，保持连接

**运行时插入任务**:
- 新任务的创建时间 = 当前仿真时间
- 任务立即进入调度队列，在下一个仿真周期处理
- 如果是紧急任务，触发调度器重新评估

---

## 3. 模块划分详解

### 3.1 sim-logistics-core

**职责**: 实体定义，不包含业务逻辑

```
com.semi.simlogistics.core/
├── entity/
│   ├── LogisticsEntity.java          // 实体基类
│   ├── vehicle/
│   │   ├── Vehicle.java              // 车辆抽象
│   │   ├── OHTVehicle.java
│   │   └── AGVVehicle.java
│   ├── fixed/
│   │   ├── Stocker.java
│   │   ├── ERack.java
│   │   └── ManualStation.java
│   ├── operator/
│   │   └── Operator.java
│   └── path/
│       └── Conveyor.java
├── capability/
│   ├── TransportCapability.java
│   ├── BatteryCapability.java
│   └── LoadUnloadCapability.java
└── model/
    ├── Position.java
    ├── Cargo.java
    └── VehicleState.java
```

### 3.2 sim-logistics-control

**职责**: 交通控制，路径管理

```
com.semi.simlogistics.control/
├── traffic/
│   ├── TrafficManager.java           // 交通管制器
│   ├── ControlPoint.java
│   ├── ControlArea.java
│   ├── ConflictResolver.java         // 冲突解决
│   └── PriorityManager.java          // 优先级管理
├── path/
│   ├── PathPlanner.java              // 路径规划器
│   ├── Path.java
│   ├── Edge.java
│   └── NetworkGraph.java
└── reservation/
    └── ReservationManager.java       // 资源预留
```

### 3.3 sim-logistics-scheduler

**职责**: 任务调度

```
com.semi.simlogistics.scheduler/
├── engine/
│   ├── DispatchEngine.java           // 调度引擎
│   └── RealTimeDispatcher.java       // 实时派单器
├── task/
│   ├── Task.java
│   ├── TaskQueue.java
│   └── TaskPriority.java
├── rule/
│   ├── DispatchRule.java             // 调度规则接口
│   ├── ShortestDistanceRule.java
│   ├── HighestPriorityRule.java
│   └── LeastUtilizedRule.java
└── event/
    └── DispatchEvent.java
```

### 3.4 sim-logistics-metrics

**职责**: KPI 收集分析

```
com.semi.simlogistics.metrics/
├── collector/
│   ├── MetricsCollector.java
│   └── EventPublisher.java
├── calculator/
│   ├── ThroughputCalculator.java
│   ├── EnergyCalculator.java
│   └── UtilizationCalculator.java
├── analyzer/
│   ├── BottleneckAnalyzer.java
│   └── VehicleCountEvaluator.java
└── report/
    ├── KPIReport.java
    └── ChartData.java
```

### 3.5 sim-logistics-web

**职责**: Web 服务

```
com.semi.simlogistics.web/
├── controller/
│   ├── SimulationController.java
│   ├── EntityController.java
│   └── MetricsController.java
├── websocket/
│   └── SimulationWebSocketHandler.java
├── dto/
│   ├── SceneDTO.java
│   ├── EntityDTO.java
│   └── MetricsDTO.java
└── service/
    └── SimulationService.java
```

### 3.6 基础设施适配层边界（Phase 8 强制）

**目标**: 保持核心模块低耦合，避免基础设施依赖下沉到领域层。

**强制规则**:
- `sim-logistics-core/control/scheduler/metrics/workflow` 只允许定义领域模型、规则、Port 接口和 InMemory fallback
- MySQL/Redis/MinIO 的 SDK 与配置装配仅允许出现在 `sim-logistics-web`（或未来独立 `sim-logistics-infra`）
- 核心模块禁止直接引入 `spring-*`、`redisson-*`、`minio-*`、`jdbc/jpa` 依赖
- `system_config` 读取采用 Port + Adapter 形式：领域层依赖接口，Web/Infra 提供 DB 实现

**推荐端口与适配器**:
- Port（核心层）:
  - `SystemConfigPort`
  - `PathCachePort`
  - `ObjectStoragePort`
- Adapter（Web/Infra 层）:
  - `MysqlSystemConfigAdapter`
  - `RedisPathCacheAdapter`
  - `MinioObjectStorageAdapter`

**依赖方向（单向）**:
1. `sim-logistics-web` -> `sim-logistics-core/control/scheduler/metrics/workflow`
2. `sim-logistics-control` -> `sim-logistics-core`（允许）
3. `sim-logistics-control` -X-> `spring/redisson/minio`（禁止）
4. 基础设施实现通过 DI/工厂装配到应用服务，不反向污染核心模块

**验收标准（架构层）**:
- `sim-logistics-control` 模块 POM 中无 `spring-*`、`redisson-*`、`minio-*` 依赖
- 基础设施替换不影响领域层单元测试
- 无外部组件时可使用 InMemory fallback 启动本地测试

---

## 4. 数据模型设计

### 4.1 场景数据结构

```json
{
  "sceneId": "semiconductor-fab-01",
  "name": "半导体工厂 01 号线",
  "entities": [
    {
      "id": "STOCKER-01",
      "type": "Stocker",
      "position": {"x": 0, "y": 0, "z": 0},
      "properties": {
        "capacity": 1000,
        "craneCount": 2
      }
    },
    {
      "id": "OHT-01",
      "type": "OHTVehicle",
      "trackId": "TRACK-A",
      "properties": {
        "maxLoad": 50,
        "speed": 2.5
      }
    }
  ],
  "paths": [
    {
      "id": "TRACK-A",
      "type": "Track",
      "points": [
        {"id": "P1", "x": 0, "y": 10},
        {"id": "P2", "x": 50, "y": 10}
      ],
      "controlPoints": [
        {"id": "CP1", "at": "P1", "capacity": 1}
      ]
    }
  ]
}
```

### 4.2 任务数据结构

```json
{
  "taskId": "TASK-001",
  "type": "TRANSPORT",
  "priority": "URGENT",
  "source": "STOCKER-01",
  "destination": "MACHINE-05",
  "cargo": {
    "type": "FOUP",
    "weight": 10
  },
  "deadline": 3600,
  "status": "PENDING"
}
```

### 4.3 API 与通信协议

#### 4.3.1 REST API 规范

**基础路径**: `/api/v1`

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

##### 仿真控制 API

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | /simulations | 创建仿真 | SimulationRequest | SimulationDTO |
| GET | /simulations | 获取仿真列表 | - | SimulationDTO[] |
| GET | /simulations/{id} | 获取仿真状态 | - | SimulationDTO |
| PUT | /simulations/{id}/start | 启动仿真 | StartRequest | SuccessResponse |
| PUT | /simulations/{id}/pause | 暂停仿真 | - | SuccessResponse |
| PUT | /simulations/{id}/resume | 继续仿真 | - | SuccessResponse |
| PUT | /simulations/{id}/stop | 停止仿真 | - | SuccessResponse |
| DELETE | /simulations/{id} | 删除仿真 | - | SuccessResponse |
| PUT | /simulations/{id}/speed | 设置仿真速度 | SetSpeedRequest | SuccessResponse |

**请求示例**:
```json
// POST /simulations
{
  "sceneId": "semiconductor-fab-01",
  "config": {
    "duration": 3600,
    "timeScale": 10.0,
    "seed": 12345
  },
  "name": "仿真运行 01"
}

// PUT /simulations/{id}/speed
{
  "timeScale": 50.0
}
```

##### 任务管理 API

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | /simulations/{id}/tasks | 获取任务列表 | - | TaskDTO[] |
| POST | /simulations/{id}/tasks | 手动添加任务 | TaskRequest | TaskDTO |
| PUT | /tasks/{taskId}/priority | 设置任务优先级 | SetPriorityRequest | SuccessResponse |
| DELETE /tasks/{taskId} | 取消任务 | - | SuccessResponse |

**请求示例**:
```json
// POST /simulations/{id}/tasks
{
  "type": "TRANSPORT",
  "priority": "URGENT",
  "source": "STOCKER-01",
  "destination": "MACHINE-05",
  "cargo": {
    "type": "FOUP",
    "weight": 10
  }
}
```

##### 场景管理 API

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | /scenes | 获取场景列表 | - | SceneDTO[] |
| POST | /scenes | 创建场景 | SceneRequest | SceneDTO |
| GET | /scenes/{id} | 获取场景详情 | - | SceneDTO |
| PUT | /scenes/{id} | 更新场景 | SceneRequest | SceneDTO |
| DELETE | /scenes/{id} | 删除场景 | - | SuccessResponse |
| POST | /scenes/{id}/draft | 保存草稿 | SceneDefinition | SuccessResponse |
| GET | /scenes/{id}/draft | 获取草稿 | - | SceneDefinition |

##### 实体查询 API

| 方法 | 路径 | 描述 | 响应 |
|------|------|------|------|
| GET | /scenes/{sceneId}/entities | 获取场景所有实体 | EntityDTO[] |
| GET | /entities/{id} | 获取实体详情 | EntityDTO |
| GET | /simulations/{id}/entities | 获取仿真运行时实体状态 | EntityStateDTO[] |
| GET | /simulations/{id}/vehicles | 获取车辆状态 | VehicleStateDTO[] |

##### KPI 指标 API

| 方法 | 路径 | 描述 | 响应 |
|------|------|------|------|
| GET | /simulations/{id}/metrics | 获取仿真指标 | MetricsDTO |
| GET | /simulations/{id}/metrics/throughput | 获取吞吐量 | ThroughputDTO |
| GET | /simulations/{id}/metrics/utilization | 获取利用率 | UtilizationDTO |
| GET | /simulations/{id}/report | 生成报表 | ReportDTO |

#### 4.3.2 WebSocket 消息协议

**连接端点**: `ws://host/api/v1/simulations/{simulationId}/ws`

**消息格式**:
```json
{
  "type": "MESSAGE_TYPE",
  "payload": {...},
  "timestamp": "2026-02-06T10:00:00Z"
}
```

##### 客户端 → 服务器消息

| 消息类型 | 描述 | Payload 示例 |
|----------|------|-------------|
| SUBSCRIBE | 订阅仿真状态 | `{"entityTypes": ["OHT_VEHICLE", "AGV_VEHICLE"]}` |
| UNSUBSCRIBE | 取消订阅 | `{"entityTypes": ["OHT_VEHICLE"]}` |
| ADD_TASK | 添加任务 | 同 REST API TaskRequest |
| CANCEL_TASK | 取消任务 | `{"taskId": "TASK-001"}` |
| SET_SPEED | 设置仿真速度 | `{"timeScale": 50.0}` |
| PAUSE | 暂停仿真 | `{}` |
| RESUME | 继续仿真 | `{}` |

##### 服务器 → 客户端消息

| 消息类型 | 描述 | Payload 示例 |
|----------|------|-------------|
| SIMULATION_STARTED | 仿真已启动 | `{"simulationId": "...", "startTime": 12345}` |
| SIMULATION_PAUSED | 仿真已暂停 | `{"simulationId": "..."}` |
| SIMULATION_RESUMED | 仿真已继续 | `{"simulationId": "..."}` |
| SIMULATION_COMPLETED | 仿真已完成 | `{"simulationId": "...", "summary": {...}}` |
| SIMULATION_STOPPED | 仿真已停止 | `{"simulationId": "...", "reason": "..."}` |
| SIMULATION_ERROR | 仿真错误 | `{"simulationId": "...", "error": {...}}` |
| ENTITY_UPDATE | 实体状态更新 | `{"entities": [...]}` |
| TASK_ASSIGNED | 任务已分配 | `{"taskId": "...", "vehicleId": "..."}` |
| TASK_COMPLETED | 任务已完成 | `{"taskId": "...", "result": {...}}` |
| METRICS_UPDATE | 指标更新 | `{"metrics": {...}}` |
| CONFLICT_RESOLVED | 冲突已解决 | `{"conflictId": "...", "resolution": "..."}` |

**ENTITY_UPDATE 示例**:
```json
{
  "type": "ENTITY_UPDATE",
  "payload": {
    "entities": [
      {
        "id": "OHT-01",
        "type": "OHT_VEHICLE",
        "state": "MOVING",
        "position": {"x": 25.5, "y": 10.0, "z": 5.0},
        "speed": 2.5,
        "battery": 0.85,
        "load": null,
        "currentTask": "TASK-001"
      }
    ],
    "simulatedTime": 1234.56
  },
  "timestamp": "2026-02-06T10:00:00Z"
}
```

**推送策略**:
- **纯 DES 模式** (timeScale > 10x): 批量推送，每 100ms 或累积 10 个实体变化
- **实时模式** (timeScale ≈ 1x): 每个状态变化即时推送
- **暂停时**: 停止状态推送，保持连接
- **错误时**: 发送 SIMULATION_ERROR 消息，连接保持

**心跳机制**:
- 客户端每 30s 发送 PING
- 服务器响应 PONG
- 超过 60s 无心跳自动断开

---

## 5. 关键算法设计

### 5.1 A* 路径规划

```java
public class AStarPathPlanner implements PathPlanner {
    private final NetworkGraph graph;

    @Override
    public List<String> planPath(String from, String to, Vehicle vehicle) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<String, Node> allNodes = new HashMap<>();

        Node start = new Node(from, null, 0, heuristic(from, to));
        openSet.add(start);
        allNodes.put(from, start);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.id.equals(to)) {
                return reconstructPath(current);
            }

            for (Edge edge : graph.getNeighbors(current.id)) {
                if (!isTraversable(edge, vehicle)) continue;

                double newCost = current.gCost + edge.getWeight();
                Node neighbor = allNodes.computeIfAbsent(
                    edge.to(),
                    id -> new Node(id, null, Double.MAX_VALUE, 0)
                );

                if (newCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = newCost;
                    neighbor.hCost = heuristic(edge.to(), to);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    private boolean isTraversable(Edge edge, Vehicle vehicle) {
        // 检查控制点可用性、车辆类型兼容性等
        return true;
    }
}
```

### 5.2 冲突解决算法

```java
public class ConflictResolver {
    private final TrafficManager trafficManager;

    public Resolution resolveConflict(Vehicle v1, Vehicle v2, String cpId) {
        ControlPoint cp = trafficManager.getControlPoint(cpId);

        // 规则 1: 优先级
        int cmp = Integer.compare(v1.getPriority(), v2.getPriority());
        if (cmp != 0) {
            return cmp > 0 ? Resolution.FAVOR_V1 : Resolution.FAVOR_V2;
        }

        // 规则 2: 先到先得
        double d1 = v1.getDistanceTo(cpId);
        double d2 = v2.getDistanceTo(cpId);

        if (Math.abs(d1 - d2) < 0.1) {
            // 距离相近，随机选择
            return Resolution.RANDOM;
        }

        return d1 < d2 ? Resolution.FAVOR_V1 : Resolution.FAVOR_V2;
    }
}
```

### 5.3 路径规划缓存策略

**实现边界约束（强制）**:
- `PathPlanner` 接口与纯算法实现保留在 `sim-logistics-control`
- Redis/Redisson 缓存实现属于基础设施适配层（`sim-logistics-web`），以装饰器/Adapter 方式注入
- 禁止在 `sim-logistics-control` 直接依赖 `RedissonClient`、`RMapCache` 等基础设施类型

```java
/**
 * 路径规划缓存（Redis + Redisson 分布式缓存）
 * 缓存键: path:{from}:{to}:{vehicleType}
 * TTL: 1 小时
 */
public class CachedPathPlanner implements PathPlanner {
    private final PathPlanner delegate;
    private final RedissonClient redisson;
    private final Codec codec;  // Path 序列化/反序列化
    private static final String CACHE_NAME = "path-cache";

    public CachedPathPlanner(PathPlanner delegate, RedissonClient redisson) {
        this.delegate = delegate;
        this.redisson = redisson;
        this.codec = new JsonJacksonCodec();  // 或自定义 Kryo Codec
    }

    @Override
    public Path planPath(String from, String to, VehicleType vehicleType) {
        String cacheKey = from + ":" + to + ":" + vehicleType;

        // 先从 Redis 分布式缓存读取
        RMapCache<String, Path> cache = redisson.getMapCache(CACHE_NAME, codec);
        Path cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，调用委托计算路径
        Path path = delegate.planPath(from, to, vehicleType);
        if (!path.isEmpty()) {
            // 使用 put(key, value, ttl, timeUnit) 设置 entry TTL
            cache.put(cacheKey, path, 1, TimeUnit.HOURS);
        }
        return path;
    }

    // 路网/控制点变化时调用（清空所有路径缓存）
    public void invalidateOnNetworkChange(String... affectedEdgeIds) {
        // 正确方式：使用 clear() 清空 RMapCache 的所有 entry
        RMapCache<String, Path> cache = redisson.getMapCache(CACHE_NAME, codec);
        cache.clear();

        // 或使用 fastRemove() 批量删除（性能更好）
        // cache.fastRemove(cache.keySet().toArray(new String[0]));
    }
}

// 可选：本地 LRU 缓存作为 fallback（减少 Redis 网络开销）
public class HybridCachedPathPlanner implements PathPlanner {
    private final PathPlanner delegate;
    private final Cache<PathCacheKey, Path> localCache;  // Guava/Caffeine LRU
    private final RedissonClient redisson;
    private static final String CACHE_NAME = "path-cache";

    public Path planPath(String from, String to, VehicleType vehicleType) {
        PathCacheKey key = new PathCacheKey(from, to, vehicleType);

        // 1. 先查本地 LRU 缓存（最快）
        Path cached = localCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        // 2. 再查 Redis 分布式缓存
        String redisKey = from + ":" + to + ":" + vehicleType;
        RMapCache<String, Path> redisCache = redisson.getMapCache(CACHE_NAME, codec);
        Path redisCached = redisCache.get(redisKey);
        if (redisCached != null) {
            localCache.put(key, redisCached);  // 回填本地缓存
            return redisCached;
        }

        // 3. 计算并写入两层缓存
        Path path = delegate.planPath(from, to, vehicleType);
        if (!path.isEmpty()) {
            localCache.put(key, path);
            // 使用 put(key, value, ttl, timeUnit) 设置 entry TTL
            redisCache.put(redisKey, path, 1, TimeUnit.HOURS);
        }
        return path;
    }
}

// 缓存键：考虑起点、终点、车辆类型
public record PathCacheKey(String from, String to, VehicleType vehicleType) {}
```

**缓存策略**:
- **主缓存**: Redis 分布式缓存（RMapCache，entry TTL 1h）
- **可选本地 LRU**: 作为 fallback，减少 Redis 网络开销
- **缓存键**: `(from, to, vehicleType)` — 考虑不同车辆类型可能走不同路径
- **失效策略**: 事件失效 — 路网/控制点变化时清空相关缓存（使用 `cache.clear()`）
- **Redisson 特性**: 支持集群模式下的缓存共享

**关键修正**:
- TTL 设置: 使用 `cache.put(key, value, 1, TimeUnit.HOURS)` 而非 `cache.expire()`
- 缓存清理: 使用 `cache.clear()` 清空 RMapCache 的所有 entry

### 5.4 车辆数评估算法

```java
public class VehicleCountEvaluator {
    public EvaluationResult evaluate(int vehicleCount, Scenario scenario) {
        // 二分查找最优车辆数
        int low = 1;
        int high = vehicleCount * 2;
        int optimal = vehicleCount;

        while (low <= high) {
            int mid = (low + high) / 2;
            SimulationResult result = runSimulation(mid, scenario);

            if (result.meetsThroughput()) {
                optimal = mid;
                high = mid - 1; // 尝试更少
            } else {
                low = mid + 1; // 需要更多
            }
        }

        return new EvaluationResult(optimal, calculateUtilization(optimal));
    }
}
```

---

## 6. 性能考虑

### 6.1 仿真性能

**目标**: 支持 1000+ 实体，实时因子 ≥ 10x

**优化策略**:
1. **事件批处理**: 合并同一时刻的多个事件
2. **空间索引**: 使用 R-Tree 加速邻近查询
3. **惰性计算**: 路径规划结果缓存
4. **并行化**: 独立 Process 并行执行

### 6.2 3D 渲染性能

**目标**: 分级性能目标
- **500 实体**: ≥ 60 FPS
- **1000 实体**: ≥ 30 FPS

**优化策略**:
1. **LOD (Level of Detail)**: 远处实体使用简化模型
2. **实例化渲染**: 相同实体共享几何体
3. **视锥裁剪**: 只渲染视野内实体
4. **分帧渲染**: 复杂场景分多帧渲染
5. **实体池**: 复用实体对象，减少 GC

---

## 7. 扩展性设计

### 7.1 实体类型判别机制

**选择**: **核心类型枚举 + 能力接口混合**

```java
// 核心类型枚举（稳定、有限）
public enum EntityType {
    OHT_VEHICLE, AGV_VEHICLE, STOCKER, ERACK,
    MANUAL_STATION, CONVEYOR, OPERATOR
}

// 实体基类
public abstract class LogisticsEntity {
    private final String id;
    private final EntityType type;  // 核心类型
    private final Map<Class<?>, Capability> capabilities = new HashMap<>();

    public EntityType getType() {
        return type;
    }

    // 能力查询
    public <T extends Capability> boolean hasCapability(Class<T> capabilityClass) {
        return capabilities.containsKey(capabilityClass);
    }

    public <T extends Capability> T getCapability(Class<T> capabilityClass) {
        return capabilityClass.cast(capabilities.get(capabilityClass));
    }
}

// 使用示例
void processEntity(LogisticsEntity entity) {
    // 核心类型判别（用于日志、序列化、UI 显示）
    switch (entity.getType()) {
        case OHT_VEHICLE -> log.info("Processing OHT");
        case AGV_VEHICLE -> log.info("Processing AGV");
    }

    // 能力判别（用于行为调度）
    if (entity.hasCapability(TransportCapability.class)) {
        entity.getCapability(TransportCapability.class).move(destination);
    }
}
```

**优势**:
- 核心类型稳定，利于日志/序列化
- 能力接口扩展新功能，无需修改枚举
- 支持运行时动态添加能力

### 7.2 事件总线机制

**选择**: **自定义事件总线（Observer 模式）**

```java
// 业务事件（区别于 JSimul 的仿真事件）
public class SimulationEvent {
    private final String type;
    private final long simulatedTime;
    private final Map<String, Object> data;
}

// 事件监听器接口
@FunctionalInterface
public interface SimulationEventListener {
    void onEvent(SimulationEvent event);
}

// 事件总线
public class SimulationEventBus {
    private final Map<String, List<SimulationEventListener>> listeners = new ConcurrentHashMap<>();
    private final ExecutorService executor;  // 异步分发

    public void subscribe(String eventType, SimulationEventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
    }

    public void publish(SimulationEvent event) {
        List<SimulationEventListener> handlers = listeners.get(event.getType());
        if (handlers != null) {
            for (SimulationEventListener handler : handlers) {
                executor.execute(() -> handler.onEvent(event));
            }
        }
    }

    public void publishSync(SimulationEvent event) {
        // 同步发布，用于关键事件
        List<SimulationEventListener> handlers = listeners.get(event.getType());
        if (handlers != null) {
            handlers.forEach(handler -> handler.onEvent(event));
        }
    }
}

// 使用示例
eventBus.subscribe("TASK_COMPLETED", event -> {
    String taskId = event.getData("taskId");
    metricsCollector.recordTaskCompletion(taskId);
});
```

**事件类型定义**:
```java
public class EventTypes {
    // 实体事件
    public static final String ENTITY_CREATED = "entity.created";
    public static final String ENTITY_STATE_CHANGED = "entity.state_changed";
    public static final String ENTITY_MOVED = "entity.moved";

    // 任务事件
    public static final String TASK_CREATED = "task.created";
    public static final String TASK_ASSIGNED = "task.assigned";
    public static final String TASK_COMPLETED = "task.completed";
    public static final String TASK_FAILED = "task.failed";

    // 交通事件
    public static final String CONFLICT_DETECTED = "traffic.conflict";
    public static final String CONFLICT_RESOLVED = "traffic.resolved";

    // 系统事件
    public static final String SIMULATION_STARTED = "simulation.started";
    public static final String SIMULATION_PAUSED = "simulation.paused";
    public static final String SIMULATION_STOPPED = "simulation.stopped";
}
```

**优势**:
- 零依赖，不引入 RxJava 等重型库
- 与 JSimul 的 Event 区分清晰（仿真事件 vs 业务事件）
- 支持同步/异步分发
- 模块解耦，易于测试

### 7.3 插件机制

```java
public interface LogisticsPlugin {
    void onLoad(SimulationContext context);
    void onUnload();
}

// 用户可自定义调度算法
public interface DispatchRule {
    String getName();
    Vehicle selectVehicle(Task task, List<Vehicle> candidates);
}
```

### 7.3 概率分布模型（仿真用）

**目标**: 统一管理故障间隔、维修时长、加工时长的随机分布

**默认支持分布**:
- Normal
- LogNormal
- Exponential
- Triangular

**接口示例**:
```java
public interface Distribution {
    double sample();     // 生成样本（仿真时间单位）
    String name();       // 分布名称
}
```

**适用范围**:
- 故障间隔（MTBF）
- 维修时长
- 加工时长

### 7.4 脚本支持

预留 GraalVM JavaScript 引擎接口：

```java
public class ScriptDispatcher {
    private final ScriptEngine engine;

    public Vehicle selectVehicle(Task task, List<Vehicle> candidates) {
        String script = loadUserScript();
        return (Vehicle) engine.eval(script);
    }
}
```

---

## 8. 测试策略

### 8.1 单元测试

- 实体状态转换测试
- 路径规划算法测试
- 冲突解决逻辑测试

### 8.2 集成测试

- 多车协同场景
- 复杂路网仿真
- 长时间稳定性测试

### 8.3 性能测试

- 不同规模场景的性能基准
- 内存占用分析
- 并发压力测试

---

## 9. 技术选型总结

| 领域 | 技术选型 | 理由 |
|------|----------|------|
| 仿真核心 | JSimul (现有) | 项目已实现 DES 核心 |
| 图算法 | JGraphT | 成熟稳定，算法丰富 |
| 状态管理 | 枚举 + 状态转换表 | 轻量、无依赖、易调试 |
| 调度规则 | 自定义规则接口 | 可扩展、无外部依赖 |
| Web 框架 | (仅 web 模块) 按需选择 | 核心模块零依赖 |
| 数据访问层 | MyBatis-Plus | SQL 可控、适配复杂查询与多租户 |
| 前端框架 | React | 组件化，生态好 |
| 3D 渲染 | Three.js | 最流行的 Web 3D 库 |
| 图表 | Chart.js | 简单易用 |

**依赖原则**:
- 核心模块 (core/control/scheduler/metrics): 零外部依赖，纯 Java 实现
- Web 模块: 可按需引入 Web 框架 (如 Jakarta EE / Spring Boot)
- 模块间通过接口解耦，支持独立测试和复用
- 基础设施依赖（MySQL/Redis/MinIO）仅允许在 Web/Infra 模块
- 核心模块禁止出现 Spring/Redisson/MinIO/JPA/JDBC API 直接依赖
- 若出现跨层需求，必须通过 Port 接口向内收敛，不得在核心层直接调用外部 SDK
- Web/Infra 模块的数据访问实现统一采用 MyBatis-Plus（不采用 JPA 作为主实现）

**Phase 8 设计约束**:
- 在接入 `system_config`、缓存和对象存储时，优先补齐 Port 接口，再实现 Adapter
- 所有新引入的基础设施实现必须可被 InMemory fallback 替换（便于单测与离线运行）
- MySQL 持久化必须通过 MyBatis-Plus Mapper + Repository Adapter 组合实现

---

## 10. 风险缓解

| 风险 | 缓解措施 |
|------|----------|
| JSimul 不满足物流场景需求 | 预留扩展点，必要时增强核心 |
| 3D 性能不足 | 使用 LOD、实例化渲染 |
| 路径规划性能 | 预计算 + 缓存 |
| WebSocket 连接稳定性 | 自动重连 + 心跳机制 |
| 内存溢出 | 流式处理历史数据 |

---

## 11. 工作流自动化模块（sim-logistics-workflow）

### 11.1 模块概述

**新增模块**: `sim-logistics-workflow`

**核心能力**: 复杂工艺流程建模、事件驱动的自动任务生成、人车混合交通管制。

**对标 FlexSim Process Flow**: 提供可编程的流程定义能力，支持 Lists & Queue、任务序列、智能路由。

### 11.2 核心数据结构

#### 11.2.1 OutputStrategy 产出策略（v1.5）

```java
public enum StrategyType {
    DIRECT_TO_NEXT,       // 直达下一台
    INTERMEDIATE_STORAGE, // 中间存储
    BUFFER_ZONE,          // 暂存区
    CONDITIONAL,          // 条件分支
    DYNAMIC_SELECT        // 动态选择（从候选列表）
}

public class OutputStrategy {
    private StrategyType type;
    private String singleDestination;           // 用于 DIRECT_TO_NEXT
    private String storageLocation;             // 用于 INTERMEDIATE_STORAGE/BUFFER_ZONE
    private List<String> candidateList;         // 用于 DYNAMIC_SELECT
    private DynamicSelector dynamicSelector;     // 用于 DYNAMIC_SELECT
    private List<ConditionalRoute> conditionalRoutes;  // 用于 CONDITIONAL
    private RoutingPolicy routingPolicy;
    private DownstreamStrategy downstreamStrategy;
}
```

#### 11.2.2 TransportSelector 搬运选择器（v1.5）

```java
public enum SelectionPolicy {
    DISTANCE_FIRST,       // 距离优先
    TIME_FIRST,           // 时效优先
    WIP_FIRST,            // 负载优先
    PRIORITY_BASED,       // 优先级
    HYBRID                // 混合策略
}

public class TransportSelector {
    private SelectionPolicy policy;
    private List<EntityType> allowedTypes;  // OHT, AGV, HUMAN

    // HYBRID 策略权重
    private double distanceWeight;
    private double timeWeight;
    private double wipWeight;
}
```

#### 11.2.3 TriggerCondition 触发条件（v1.5）

```java
public class TriggerCondition {
    // 基础匹配（必填）
    private String entryPointId;              // 入口点 ID（必填，与绑定表一致）

    // 物料属性匹配（可选）
    private String materialType;              // 物料类型
    private String materialGrade;             // 物料等级
    private String materialBatch;             // 批次号（支持通配符）
    private Map<String, String> materialAttributes;  // 自定义物料属性

    // 工艺标记匹配（可选）
    private String processTag;                // 工艺标记
    private List<String> allowedProcessTags;  // 允许的工艺标记列表

    // 自定义条件（可选）
    private Predicate<EventContext> customCondition;
}
```

#### 11.2.4 SafetyZone 安全区（v1.5）

```java
public enum Priority {
    HUMAN_FIRST,   // Humans always have priority
    VEHICLE_FIRST, // Vehicles have priority (rare)
    FIFO,          // First-come-first-served
    PRIORITY_BASED // Based on task priority
}

public class SafetyZone {
    private String id;
    private String name;
    private GeometryType geometryType;  // CIRCLE, RECTANGLE, POLYGON
    private Position center;
    private double radius;             // For CIRCLE
    private double width;              // For RECTANGLE
    private double height;             // For RECTANGLE
    private List<Position> boundary;   // For POLYGON

    // 容量配置
    private int maxHumans;
    private int maxVehicles;
    private Priority accessPriority;   // HUMAN_FIRST, VEHICLE_FIRST, FIFO, PRIORITY_BASED

    // 关联
    private List<String> connectedPathIds;
    private List<String> monitoredEntityIds;
    private boolean enabled;
}
```

**存储格式**: SafetyZone 作为实体类型存储在 `scenes.entities` JSON 数组中：

```json
{
  "entities": [
    {"id": "Machine-A", "type": "MACHINE", ...},
    {"id": "ZONE-FAB1-MAIN", "type": "SAFETY_ZONE", ...}
  ]
}
```

#### 11.2.5 MaterialBindingManager 物料互斥（v1.5）

```java
public class MaterialBindingManager {
    private SimulationEnv env;
    private Map<String, MaterialBinding> bindings;

    /**
     * Try to bind material to a step.
     * Uses putIfAbsent for atomic binding operation.
     * No timeout force unbind - binding only released on completion/failure.
     */
    public boolean tryBind(String materialId, String stepId) {
        MaterialBinding newBinding = new MaterialBinding(materialId, stepId, env.now());
        MaterialBinding existing = bindings.putIfAbsent(materialId, newBinding);
        return existing == null;
    }

    public void unbind(String materialId) {
        bindings.remove(materialId);
    }
}
```

**关键行为（v1.5）**:
- 使用 `putIfAbsent` 确保原子性绑定操作
- **没有超时强制解绑**逻辑
- 只在流程完成或失败时释放绑定
- 使用仿真时间（`env.now()`）

### 11.3 职责边界

| 职责项 | Workflow 模块 | Scheduler 模块 | Control 模块 |
|--------|---------------|----------------|--------------|
| **任务生成** | ✅ 负责（自动生成） | ❌ 不参与 | ❌ 不参与 |
| **任务入队** | ✅ 写入 TaskQueue | ❌ 不操作 | ❌ 不操作 |
| **任务派单** | ❌ 不参与 | ✅ 负责 | ❌ 不参与 |
| **交通管制** | ❌ 不参与 | ⚠️ 发起请求 | ✅ 负责 |

### 11.4 数据存储

#### 11.4.1 process_flows 表

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

#### 11.4.2 process_flow_bindings 表

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

### 11.5 关键设计决策

#### 决策 WF-1: SafetyZone 存储方式

**决策**: SafetyZone 作为实体类型存储在 `scenes.entities` JSON 数组中

**理由**:
- SafetyZone 数量少（通常几十个）
- 与场景原子性一致
- 前端统一读取，降低复杂度

#### 决策 WF-2: Human 不占用 ControlPoint/Edge

**决策**: Human 只占用 SafetyZone，不参与 ControlPoint/Edge 的车辆冲突仲裁

**理由**:
- Human 移动速度慢、路径不确定，不适合细粒度路径管制
- 人车冲突统一在 SafetyZone 层面处理
- 简化 ControlPoint/Edge 逻辑，避免人车混合仲裁复杂度

#### 决策 WF-3: 物料级互斥绑定（无超时强制解绑）

**决策**: 使用 MaterialBindingManager 确保同一物料同一时刻只绑定到一个工序，使用 `putIfAbsent` 原子操作，**不实现超时强制解绑**

**理由**:
- 防止多流程并行执行时物料被重复分配
- 超时强制解绑可能导致状态不一致
- 绑定只在流程完成或失败时释放，行为可预测

#### 决策 WF-4: 事件驱动 + 可配置重试

**决策**:
- 事件驱动：EventTrigger 监听物料事件，匹配后自动生成任务
- 可配置重试：RetryConfig 支持指数退避、最大重试次数
- 非轮询：不使用定时轮询检查条件

#### 决策 WF-5: 多候选设备选择（v2.0）

**决策**:
- ProcessStep 支持 `targetEntityIds: List<String>`，允许多候选设备
- 建模阶段强制校验：设备 `supportedTransportTypes` 与工序 `requiredTransportTypes` 交集为空时禁止保存
- 运行时使用多因子加权算法选择最优设备（min-max 归一化）

**数据结构变更**:

```java
/**
 * Process step with multi-candidate device support (v2.0).
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public class ProcessStep {
    // OLD: private String targetEntityId;
    // NEW: Support multiple candidate devices
    private List<String> targetEntityIds;

    // NEW: Required transport types for this step
    private List<String> requiredTransportTypes;

    // ... other fields
}

/**
 * Device entity base class (v2.0).
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public abstract class DeviceEntity extends LogisticsEntity {
    // NEW: Transport types supported by this device (REQUIRED)
    private List<TransportType> supportedTransportTypes;
}
```

**多因子加权选择算法**:

```java
/**
 * Multi-factor weighted device selector.
 *
 * Score = w_distance * (1 - normDistance)
 *       + w_time * (1 - normTime)
 *       + w_wip * (1 - normWip)
 *
 * Normalization: Min-Max
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public class MultiFactorDeviceSelector {

    private final SelectorWeights weights;  // From system_config

    public String selectDevice(List<String> candidates, Position source) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // Collect metrics
        List<DeviceMetrics> metrics = collectMetrics(candidates, source);

        // Min-max normalization
        double minD = metrics.stream().mapToDouble(DeviceMetrics::getDistance).min().orElse(0);
        double maxD = metrics.stream().mapToDouble(DeviceMetrics::getDistance).max().orElse(1);
        double minT = metrics.stream().mapToDouble(DeviceMetrics::getTime).min().orElse(0);
        double maxT = metrics.stream().mapToDouble(DeviceMetrics::getTime).max().orElse(1);
        double minW = metrics.stream().mapToDouble(DeviceMetrics::getWip).min().orElse(0);
        double maxW = metrics.stream().mapToDouble(DeviceMetrics::getWip).max().orElse(1);

        // Select best by score
        return metrics.stream()
            .max(Comparator.comparingDouble(m -> calculateScore(m, minD, maxD, minT, maxT, minW, maxW)))
            .map(DeviceMetrics::getId)
            .orElse(candidates.get(0));
    }
}

public class SelectorWeights {
    private double distanceWeight = 0.4;  // Default from system_config
    private double timeWeight = 0.4;
    private double wipWeight = 0.2;
}
```

**运输类型强校验**:

```java
/**
 * Validate transport type compatibility (modeling phase).
 *
 * Intersection of required types and supported types must be non-empty.
 *
 * @throws ValidationException if intersection is empty
 */
public void validateTransportTypeCompatibility(ProcessStep step,
                                              List<DeviceEntity> devices) {
    Set<String> required = new HashSet<>(step.getRequiredTransportTypes());
    Set<String> supported = new HashSet<>();

    for (DeviceEntity device : devices) {
        if (device.getSupportedTransportTypes() != null) {
            supported.addAll(device.getSupportedTransportTypes().stream()
                .map(Enum::name)
                .collect(Collectors.toList()));
        }
    }

    Set<String> intersection = new HashSet<>(required);
    intersection.retainAll(supported);

    if (intersection.isEmpty()) {
        throw new ValidationException(String.format(
            "Transport type mismatch: step requires %s, devices support %s",
            required, supported));
    }
}
```

**全局配置（system_config）**:

```
# Multi-candidate device selection weights
dispatch.selector.weight.distance = 0.4
dispatch.selector.weight.time = 0.4
dispatch.selector.weight.wip = 0.2
dispatch.selector.normalization = min-max
```

**理由**:
- **多候选设备**: 提高系统灵活性和可靠性，避免单点故障
- **强校验**: 建模阶段发现问题，避免运行时错误
- **多因子加权**: 综合考虑距离、时间、WIP，选择全局最优设备
- **Min-max 归一化**: 公平比较不同量级的指标
- **全局可配置**: 支持不同场景的权重调整
