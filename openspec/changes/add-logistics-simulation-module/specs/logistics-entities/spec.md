# 物流实体能力规范

## Metadata
- **Spec ID**: `logistics-entities`
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Status**: 提案阶段

---

## ADDED Requirements

### Requirement: REQ-ENT-000 实体行为接口定义

系统MUST实现REQ-ENT-000（实体行为接口定义）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 定义实体的核心行为接口和能力接口组合模型

#### 接口定义原则
- **核心行为**: 直接在 LogisticsEntity 子类中实现（moveTo, load, unload 等）
- **扩展能力**: 通过 Capability 接口组合（TransportCapability, BatteryCapability 等）
- **与 JSimul Process 关系**: Vehicle 组合 Process 引用，非继承

#### Scenario: 核心行为方法调用
**Given** 一个 OHTVehicle 实体
**When** 调用核心行为方法
**Then** 以下方法应直接在 Vehicle 类中实现:
```java
// 核心行为（在 Vehicle 类中）
void moveTo(Position destination);
void load(Cargo cargo);
void unload();
Position getPosition();
VehicleState getState();
```

#### Scenario: 扩展能力接口查询
**Given** 一个 OHTVehicle 实体
**When** 查询扩展能力
**Then** 应支持能力接口查询:
```java
// 扩展能力（通过 Capability 接口）
if (vehicle.hasCapability(TransportCapability.class)) {
    vehicle.getCapability(TransportCapability.class).move(destination);
}

if (vehicle.hasCapability(BatteryCapability.class)) {
    double battery = vehicle.getCapability(BatteryCapability.class).getBatteryLevel();
}
```

#### Scenario: Vehicle 与 Process 组合关系
**Given** 一个 Vehicle 实体
**When** 启动仿真进程
**Then** Vehicle 应组合 JSimul Process 引用:
```java
public abstract class Vehicle extends LogisticsEntity {
    protected Process process;  // 组合 Process，非继承

    public void startSimulation(Environment env) {
        this.process = env.process(this::run);
    }

    protected abstract void run();  // 子类实现行为逻辑
}
```

#### Scenario: 实体类型判别
**Given** 多个实体实例
**When** 需要判别实体类型
**Then** 应支持核心类型枚举和能力接口双机制:
```java
// 核心类型判别（用于日志、序列化、UI 显示）
switch (entity.getType()) {
    case OHT_VEHICLE -> log.info("Processing OHT");
    case AGV_VEHICLE -> log.info("Processing AGV");
}

// 能力判别（用于行为调度）
if (entity.hasCapability(TransportCapability.class)) {
    entity.getCapability(TransportCapability.class).move(destination);
}
```

---

#### 实体类型枚举（包含 SafetyZone）
```java
/**
 * Entity type for logistics simulation.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-08
 */
public enum EntityType {
    MACHINE,
    STOCKER,
    ERACK,
    MANUAL_STATION,
    CONVEYOR,
    OHT_VEHICLE,
    AGV_VEHICLE,
    OPERATOR,
    SAFETY_ZONE,
    CONTROL_POINT,
    CONTROL_AREA
}
```

---

### Requirement: REQ-ENT-001 LogisticsEntity 基类

系统MUST实现REQ-ENT-001（LogisticsEntity 基类）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 定义所有物流实体的通用基类

#### Scenario: 创建基础实体
**Given** 实体模型初始化
**When** 创建一个新的 LogisticsEntity
**Then** 实体应具有唯一 ID、位置、名称属性
**And** 实体应支持位置更新

#### Scenario: 实体生命周期管理
**Given** 一个已创建的实体
**When** 实体被销毁
**Then** 相关资源应被释放
**And** 实体应从仿真环境中移除

---

### Requirement: REQ-ENT-001.1 设备运输类型支持（v2.0）

系统MUST实现REQ-ENT-001.1（设备运输类型支持（v2.0））能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 设备类实体必须声明支持的运输类型，用于与工序需求的运输类型进行校验。

#### 运输类型枚举

```java
/**
 * Transport types supported by logistics entities.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public enum TransportType {
    OHT,       // Overhead Hoist Transport (天车)
    AGV,       // Automated Guided Vehicle (自动导引车)
    HUMAN,     // Manual transport by operators (人工搬运)
    CONVEYOR   // Conveyor system (输送线)
}
```

#### 设备类实体要求

所有可参与物料搬运的设备类实体（Machine, Stocker, ERack, ManualStation, Conveyor）必须包含 `supportedTransportTypes` 字段：

```java
/**
 * Base class for device entities that can handle materials.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-07
 */
public abstract class DeviceEntity extends LogisticsEntity {
    /**
     * Transport types supported by this device.
     * Required field - must not be null or empty.
     */
    private List<TransportType> supportedTransportTypes;

    /**
     * Validate transport type compatibility.
     *
     * @param requiredTypes Required transport types
     * @return true if intersection is non-empty
     */
    public boolean supportsTransportTypes(List<TransportType> requiredTypes) {
        if (supportedTransportTypes == null || requiredTypes == null) {
            return false;
        }
        return supportedTransportTypes.stream()
            .anyMatch(requiredTypes::contains);
    }
}
```

#### 建模阶段校验规则

在保存或更新场景定义时，系统必须校验：

```java
/**
 * Validate transport type compatibility between process steps and devices.
 *
 * @param steps Process steps with requiredTransportTypes
 * @param devices Device entities with supportedTransportTypes
 * @throws ValidationException if any step has empty intersection with its devices
 */
public void validateStepDeviceTransportCompatibility(
        List<ProcessStep> steps,
        Map<String, LogisticsEntity> devices) {

    for (ProcessStep step : steps) {
        List<String> requiredTypes = step.getRequiredTransportTypes();
        List<String> deviceIds = step.getTargetEntityIds();

        // Collect all supported types from candidate devices
        Set<String> supportedTypes = new HashSet<>();
        for (String deviceId : deviceIds) {
            LogisticsEntity entity = devices.get(deviceId);
            if (entity instanceof DeviceEntity) {
                DeviceEntity device = (DeviceEntity) entity;
                if (device.getSupportedTransportTypes() != null) {
                    supportedTypes.addAll(
                        device.getSupportedTransportTypes().stream()
                            .map(Enum::name)
                            .collect(Collectors.toList())
                    );
                }
            }
        }

        // Check intersection
        Set<String> intersection = new HashSet<>(requiredTypes);
        intersection.retainAll(supportedTypes);

        if (intersection.isEmpty()) {
            throw new ValidationException(String.format(
                "Step '%s': Transport type mismatch. Required: %s, Supported by devices: %s",
                step.getName(), requiredTypes, supportedTypes));
        }
    }
}
```

#### Given/When/Then

**Scenario 1: 设备声明支持的运输类型**

```gherkin
Given 用户创建新设备 Machine-A
When 用户配置 supportedTransportTypes = ["OHT", "AGV"]
Then 设备保存成功
And Machine-A 支持天车和自动导引车运输
```

**Scenario 2: 运输类型校验 - 成功**

```gherkin
Given 工序 "清洗" 配置 requiredTransportTypes = ["OHT", "AGV"]
And 候选设备：
  - Machine-A: supportedTransportTypes = ["OHT", "HUMAN"]
  - Machine-B: supportedTransportTypes = ["AGV", "CONVEYOR"]
When 用户保存流程定义
Then 保存成功
And 运输类型交集为 ["OHT", "AGV"]（非空）
```

**Scenario 3: 运输类型校验 - 失败**

```gherkin
Given 工序 "清洗" 配置 requiredTransportTypes = ["OHT"]
And 候选设备：
  - Machine-A: supportedTransportTypes = ["AGV", "HUMAN"]
  - Machine-B: supportedTransportTypes = ["CONVEYOR"]
When 用户尝试保存流程定义
Then 保存失败
And 返回错误："Step '清洗': Transport type mismatch. Required: [OHT], Supported by devices: [AGV, HUMAN, CONVEYOR]"
```

**Scenario 4: 设备缺少 supportedTransportTypes 字段**

```gherkin
Given 用户创建新设备 Machine-A
And 用户未配置 supportedTransportTypes
When 用户尝试保存
Then 保存失败
And 返回错误："supportedTransportTypes is required for device entities"
```

---

### Requirement: REQ-ENT-002 OHTVehicle 实体

系统MUST实现REQ-ENT-002（OHTVehicle 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现天车 (OHT) 运输实体

#### Scenario: OHT 轨道移动
**Given** 一条定义好的轨道
**And** 一个初始化的 OHTVehicle
**When** OHT 从轨道起点移动到终点
**Then** OHT 状态应从 IDLE 变为 MOVING
**And** 移动时间应根据距离和速度计算
**And** 到达后状态应变为 IDLE

#### Scenario: OHT 装载物料
**Given** 一个空的 OHTVehicle
**And** 一个可用的 Stocker
**When** OHT 在 Stocker 处执行装载操作
**Then** currentLoad 应增加
**And** 状态应变为 LOADING 然后回到 IDLE

#### Scenario: OHT 卸载物料
**Given** 一个装载的 OHTVehicle
**When** OHT 执行卸载操作
**Then** currentLoad 应清零
**And** 状态应变为 UNLOADING 然后回到 IDLE

#### Scenario: OHT 轨道约束
**Given** 一条轨道网络
**And** 一个 OHTVehicle
**When** OHT 移动时
**Then** OHT 应始终在轨道上移动
**And** OHT 不能跨越不连续的轨道段

---

### Requirement: REQ-ENT-003 AGVVehicle 实体

系统MUST实现REQ-ENT-003（AGVVehicle 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现自动导引车 (AGV) 实体

#### Scenario: AGV 路网移动
**Given** 一个定义的路网
**And** 一个初始化的 AGVVehicle
**When** AGV 从节点 A 移动到节点 B
**Then** AGV 应计算最短路径
**And** AGV 应沿路径移动
**And** 到达后应触发到达事件

#### Scenario: AGV 电池管理
**Given** 一个 AGVVehicle 初始电量 100%
**When** AGV 执行移动任务
**Then** batteryLevel 应根据距离和能耗模型减少
**And** 当电池低于阈值时，AGV 应前往充电站

#### Scenario: AGV 充电逻辑
**Given** 一个低电量 AGVVehicle
**And** 一个可用的充电站
**When** AGV 到达充电站
**Then** AGV 状态应变为 CHARGING
**And** batteryLevel 应逐步增加
**And** 充满后 AGV 应恢复可用

---

### Requirement: REQ-ENT-009 故障与维修的概率模型

系统MUST实现REQ-ENT-009（故障与维修的概率模型）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 车辆故障与维修时间采用概率分布模型

#### Scenario: 故障间隔分布（MTBF）
**Given** 一辆车辆配置了故障分布
**When** 仿真运行
**Then** 故障间隔应由分布模型生成
**And** 默认支持分布为：Normal / LogNormal / Exponential / Triangular

#### Scenario: 维修时长分布
**Given** 一辆车辆进入故障状态
**When** 生成维修时长
**Then** 维修时长应由分布模型生成
**And** 默认支持分布为：Normal / LogNormal / Exponential / Triangular

---

### Requirement: REQ-ENT-004 Stocker 实体

系统MUST实现REQ-ENT-004（Stocker 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现自动化立体仓库 (ASRS)

#### Scenario: Stocker 入库
**Given** 一个未满的 Stocker
**And** 一个入库请求
**When** 执行入库操作
**Then** 物料应分配到一个空闲库位
**And** usedCapacity 应增加
**And** 库位状态应更新为 OCCUPIED

#### Scenario: Stocker 出库
**Given** 一个有库存的 Stocker
**And** 一个出库请求 (指定物料 ID)
**When** 执行出库操作
**Then** 对应库位的物料应被取出
**And** usedCapacity 应减少
**And** 库位状态应更新为 EMPTY

#### Scenario: Stocker 容量限制
**Given** 一个已满的 Stocker
**When** 尝试入库
**Then** 操作应失败或排队等待
**And** 应产生容量告警事件

#### Scenario: Stocker 堆垛机并发
**Given** 一个有多个堆垛机的 Stocker
**And** 多个并发出入库请求
**When** 执行操作
**Then** 每个堆垛机应独立处理任务
**And** 堆垛机之间应避免冲突

---

### Requirement: REQ-ENT-005 ERack 实体

系统MUST实现REQ-ENT-005（ERack 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现机架/缓存区实体

#### Scenario: E-Rack 存储物料
**Given** 一个空的 E-Rack
**And** 一个物料
**When** 放置物料到 E-Rack
**Then** 物料应被存储
**And** 容量使用应增加

#### Scenario: E-Rack 取出物料
**Given** 一个有物料的 E-Rack
**When** 取出物料
**Then** 物料应被移除
**And** 容量使用应减少

---

### Requirement: REQ-ENT-006 ManualStation 实体

系统MUST实现REQ-ENT-006（ManualStation 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现人工工位实体

#### Scenario: 人工操作处理
**Given** 一个 ManualStation
**And** 一个指派的 Operator
**And** 一个到达的物料
**When** Operator 执行操作
**Then** 操作时间应根据操作类型和 Operator 技能计算
**And** 完成后物料应离开工位

#### Scenario: 多 Operator 竞争
**Given** 一个 ManualStation
**And** 多个可用 Operator
**And** 一个任务
**When** 分配任务
**Then** 应选择最合适的 Operator (根据技能、当前负载)
**And** 其他 Operator 应保持空闲

---

### Requirement: REQ-ENT-007 Conveyor 实体

系统MUST实现REQ-ENT-007（Conveyor 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现输送线实体

#### Scenario: 输送线传输
**Given** 一条配置好的 Conveyor
**And** 一个放置在入口的物料
**When** Conveyor 运行
**Then** 物料应以定义的速度沿输送线移动
**And** 移动时间 = 距离 / 速度

#### Scenario: 输送线积放
**Given** 一条启用的积放 Conveyor
**And** 一个阻塞的出口
**When** 多个物料进入输送线
**Then** 物料应在输送线上排队
**And** 不应发生碰撞或重叠

#### Scenario: 输送线分段控制
**Given** 一条多段 Conveyor
**And** 某段故障
**When** 物料传输到故障段
**Then** 物料应在故障段前停止
**And** 其他段应继续运行

---

### Requirement: REQ-ENT-008 Operator 实体

系统MUST实现REQ-ENT-008（Operator 实体）能力，具体输入输出与行为约束以本节场景定义为准。

**描述**: 实现操作员实体

#### Scenario: Operator 技能模型
**Given** 一个具有特定技能集的 Operator
**And** 一个需要特定技能的任务
**When** 分配任务
**Then** 如果 Operator 具有所需技能，应接受任务
**And** 如果没有，应拒绝任务

#### Scenario: Operator 班次管理
**Given** 一个定义了班次的 Operator
**When** 班次时间开始
**Then** Operator 应变为可用
**When** 班次时间结束
**Then** Operator 应变为不可用

---

## 交叉引用

- 依赖 `traffic-control` 规范: 车辆移动需要交通管制
- 依赖 `dispatch-system` 规范: 实体任务分配需要调度系统
- 被 `kpi-metrics` 规范使用: 实体状态用于 KPI 计算
