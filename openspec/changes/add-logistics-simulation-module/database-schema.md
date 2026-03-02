# 物流仿真模块 - 数据库 Schema 设计

## 文档信息
- **Change ID**: `add-logistics-simulation-module`
- **Author**: shentw
- **Date**: 2026-02-06
- **Version**: 1.4 (MySQL 8.0.16+ + Redis 7.0 + Redisson + MinIO)
- **Last Updated**: 2026-02-06 (新增 workflow-automation 表：process_flows, process_flow_bindings)

## 版本兼容性说明

**MySQL 版本要求**: **8.0.16+**
- CHECK 约束在 8.0.16 开始生效（本文档中所有表约束使用 CHECK）
- JSON 类型默认值在 8.0.13+ 支持，建议应用层设置默认值以获得最大兼容性（兼容性以 8.0.16+ 为准）

---

## 1. 数据持久化策略（已决策）

### 1.1 数据分类原则

| 数据类型 | 是否落库 | 存储策略 | 理由 |
|---------|---------|----------|------|
| **场景定义** | ✅ 是 | MySQL | 用户创建的场景需要长期保存、版本管理 |
| **仿真配置** | ✅ 是 | MySQL | 仿真参数、种子、时长等配置需持久化 |
| **仿真结果** | ✅ 是 | MySQL | 历史仿真结果用于分析、对比、报表 |
| **KPI 指标** | ✅ 是 | MySQL | 趋势分析、性能基准、审计需求 |
| **运行时状态** | ❌ 否 | 内存 | 仿真运行中数据，暂停/停止时快照到数据库 |
| **事件日志** | ✅ 是 | MySQL | 可配置级别（默认 INFO+WARNING+ERROR） |
| **实体状态历史** | ✅ 是 | MySQL | 仅关键实体（车辆）的关键状态变化 |
| **审计日志** | ✅ 是 | MySQL | 记录关键操作（场景/仿真/任务） |
| **用户/权限** | ✅ 是 | MySQL | 多用户、多租户支持 |
| **临时缓存** | ❌ 否 | Redis | 路径缓存、会话缓存等，使用 Redisson |
| **报表文件** | ✅ 是 | MinIO | PDF/Excel 报表存储，数据库存 URL |

---

## 2. 数据库选型（已决策）

### 2.1 推荐方案

**主数据库**: MySQL 8.0.16+
- **版本要求**: 8.0.16+ （CHECK 约束在 8.0.16 开始生效）
- 成熟稳定、社区活跃
- 支持 JSON 类型（灵活存储实体属性）
- 支持事务、外键、索引、CHECK 约束
- 工具链完善（监控、备份、迁移）

**缓存**: Redis 7.0+ + Redisson
- 专用缓存层（路径缓存、会话缓存）
- Redisson 提供分布式锁、限流器等高级特性
- 支持集群模式扩展

**对象存储**: MinIO
- S3 兼容的对象存储
- 用于存储报表导出文件、3D 模型
- 数据库存储 URL + checksum + size

**开发测试**: H2 (可选)
- 开发阶段内存数据库
- 无需额外安装

---

## 3. MySQL Schema 设计

### 3.1 核心表结构

#### 3.1.1 租户表 (tenants)

```sql
CREATE TABLE tenants (
    id              CHAR(36) PRIMARY KEY,                    -- UUID 存储为 CHAR(36)
    name            VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED

    -- 配置 (JSON 类型存储配置)
    -- 注意: JSON 默认值在 MySQL 8.0.13+ 支持，建议应用层设置默认值
    settings        JSON,                                   -- 应用层默认值为 {}

    -- 审计
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

**MySQL 数据类型说明**:
- `UUID`: 存储为 `CHAR(36)` 或 `BINARY(16)`
- `JSON`: MySQL 5.7+ 原生支持（JSON 类型，支持路径查询、部分更新）
- `DATETIME(3)`: 精确到毫秒
- `TIMESTAMP`: 使用 `DATETIME` 避免时区问题
- `ARRAY`: MySQL 不支持原生数组，使用 JSON 数组或关联表

#### 3.1.2 场景表 (scenes)

**决策 DB-1**: 自动迁移策略

```sql
CREATE TABLE scenes (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    version         INT NOT NULL DEFAULT 1,
    schema_version  INT NOT NULL DEFAULT 1,     -- 场景模型版本
    definition      JSON NOT NULL,              -- 场景完整定义
    metadata        JSON,                       -- 元数据（标签、分类等）

    -- 审计字段
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT scenes_name_version UNIQUE (tenant_id, name, version),
    CONSTRAINT scenes_tenant_id UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_scenes_tenant_id ON scenes(tenant_id);
CREATE INDEX idx_scenes_schema_version ON scenes(schema_version);
CREATE INDEX idx_scenes_created_at ON scenes(created_at);
-- scenes.definition JSON 结构说明（简要）:
-- {
--   "entities": [
--     {
--       "id": "Machine-A",
--       "type": "MACHINE",
--       "position": {"x": 10.0, "y": 20.0, "z": 0.0},
--       "supportedTransportTypes": ["OHT", "AGV"],
--       "properties": {"capacity": 2}
--     },
--     {
--       "id": "ZONE-FAB1-MAIN",
--       "type": "SAFETY_ZONE",
--       "properties": {"shape": "CIRCLE", "radius": 5.0}
--     }
--   ],
--   "paths": [...]
-- }


-- 场景模型版本表（完整定义见建表 SQL 脚本第 13 节）
-- 注意：正文仅作说明，实际建表以"建表 SQL 脚本（完整版）"第 13 节为准
-- Schema 文件存放路径: openspec/changes/add-logistics-simulation-module/schemas/
--   - entity-base.json     : 基础实体定义
--   - position.json        : 位置坐标定义
--   - path.json            : 路径定义（points + segments，BEZIER from/to/c1/c2，edges 有向）
--   - oht-vehicle.json     : OHT 车辆属性
--   - agv-vehicle.json     : AGV 车辆属性
--   - stocker.json         : Stocker 属性
--   - erack.json           : E-Rack 属性
--   - manual-station.json  : 人工工位属性
--   - conveyor.json        : 输送线属性
--   - operator.json        : 操作员属性
```

#### 3.1.3 场景草稿表 (scene_drafts)

**描述**: 编辑过程自动保存的草稿

```sql
CREATE TABLE scene_drafts (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    content         JSON NOT NULL,      -- 草稿内容（场景定义）
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_scene_drafts_scene_id ON scene_drafts(scene_id);
CREATE INDEX idx_scene_drafts_tenant_id ON scene_drafts(tenant_id);
```

#### 3.1.4 仿真运行表 (simulations)

**决策 DB-2**: 不落库运行时状态，暂停时快照

```sql
CREATE TABLE simulations (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    name            VARCHAR(255),

    -- 配置 (JSON 存储配置)
    config          JSON NOT NULL,              -- {duration, timeScale, seed, enableStateHistory}
    status          VARCHAR(20) NOT NULL,        -- PENDING, RUNNING, PAUSED, COMPLETED, FAILED

    -- 时间记录
    simulated_time  DECIMAL(10, 2),            -- 仿真时间（秒）
    started_at      DATETIME(3),
    completed_at    DATETIME(3),

    -- 快照（暂停时保存运行时状态）
    result_snapshot JSON,                      -- {entities: [], taskQueue: [], trafficState: {}}

    -- 结果摘要
    result_summary  JSON,                       -- {totalTasks, throughput, utilization}

    -- 审计
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),

    CONSTRAINT simulations_status_check CHECK (status IN (
        'PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_simulations_tenant_id ON simulations(tenant_id);
CREATE INDEX idx_simulations_scene_id ON simulations(scene_id);
CREATE INDEX idx_simulations_status ON simulations(status);
CREATE INDEX idx_simulations_created_at ON simulations(created_at);
```

#### 3.1.5 任务表 (tasks)

**决策 DB-3**: 定期归档（3 个月）

```sql
CREATE TABLE tasks (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,

    -- 任务定义
    type            VARCHAR(50) NOT NULL,       -- TRANSPORT, CHARGING, MAINTENANCE
    priority        VARCHAR(20) NOT NULL,       -- URGENT, HIGH, NORMAL, LOW
    source          VARCHAR(100),              -- 源实体 ID
    destination     VARCHAR(100),              -- 目标实体 ID
    cargo_info      JSON,                       -- {type, weight}

    -- 时间约束 (仿真时间，单位：秒)
    deadline        DECIMAL(10, 2),             -- 仿真时间
    created_time    DECIMAL(10, 2) NOT NULL,    -- 创建时间（仿真时间）
    assigned_time   DECIMAL(10, 2),
    started_time    DECIMAL(10, 2),
    completed_time  DECIMAL(10,  2),

    -- 状态
    status          VARCHAR(20) NOT NULL,       -- PENDING, ASSIGNED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    assigned_vehicle VARCHAR(100),             -- 分配的车辆 ID

    -- 结果
    result          JSON,                       -- {success, reason, metrics}

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT tasks_status_check CHECK (status IN (
        'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tasks_tenant_id ON tasks(tenant_id);
CREATE INDEX idx_tasks_simulation_id ON tasks(simulation_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_assigned_vehicle ON tasks(assigned_vehicle);

-- 归档表（3 个月后归档）
-- 注意: CREATE TABLE ... LIKE 不复制外键和索引，因此显式定义
CREATE TABLE tasks_archive (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    priority        VARCHAR(20) NOT NULL,
    source          VARCHAR(100),
    destination     VARCHAR(100),
    cargo_info      JSON,
    deadline        DECIMAL(10, 2),
    created_time    DECIMAL(10, 2) NOT NULL,
    assigned_time   DECIMAL(10, 2),
    started_time    DECIMAL(10, 2),
    completed_time  DECIMAL(10, 2),
    status          VARCHAR(20) NOT NULL,
    assigned_vehicle VARCHAR(100),
    result          JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    -- 索引（与 tasks 表一致）
    INDEX idx_tasks_archive_tenant_id (tenant_id),
    INDEX idx_tasks_archive_simulation_id (simulation_id),
    INDEX idx_tasks_archive_status (status),
    INDEX idx_tasks_archive_priority (priority),
    INDEX idx_tasks_archive_assigned_vehicle (assigned_vehicle),
    INDEX idx_tasks_archive_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 归档存储过程（3 个月后归档）
DELIMITER //
CREATE PROCEDURE archive_old_tasks()
BEGIN
    -- 移动旧数据到归档表
    INSERT INTO tasks_archive
    SELECT * FROM tasks
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 3 MONTH);

    -- 删除已归档数据
    DELETE FROM tasks
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 3 MONTH);
END //
DELIMITER ;
```

#### 3.1.6 KPI 指标表 (kpi_metrics)

**决策 DB-4**: 自适应采样（关键事件 + 60秒聚合）

```sql
CREATE TABLE kpi_metrics (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,

    -- 时间基准（双时间基准）
    recorded_at     DECIMAL(10, 2) NOT NULL,    -- 仿真时间（秒）
    wall_clock_time DATETIME(3) NOT NULL,     -- 墙钟时间

    -- 吞吐量指标
    tasks_completed INT DEFAULT 0,
    tasks_per_hour  DECIMAL(10, 2),
    material_throughput DECIMAL(10, 2),

    -- 利用率指标（时间加权平均）
    vehicle_utilization DECIMAL(5, 2),      -- 时间加权平均
    equipment_utilization DECIMAL(5, 2),

    -- WIP 指标
    wip_total       INT DEFAULT 0,

    -- 能耗指标
    energy_total    DECIMAL(10, 2),

    -- 扩展指标
    custom_metrics  JSON,

    -- 聚合标记
    is_aggregated   BOOLEAN DEFAULT FALSE,     -- TRUE=定期聚合数据

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_kpi_metrics_tenant_id ON kpi_metrics(tenant_id);
CREATE INDEX idx_kpi_metrics_simulation_id ON kpi_metrics(simulation_id);
CREATE INDEX idx_kpi_metrics_recorded_at ON kpi_metrics(recorded_at);
```

#### 3.1.7 实体状态历史表 (entity_state_history)

**决策 DB-5**: 只记录关键实体（车辆）关键状态变化

```sql
CREATE TABLE entity_state_history (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,

    entity_id       VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,       -- OHT_VEHICLE, AGV_VEHICLE

    -- 状态快照
    recorded_at     DECIMAL(10, 2) NOT NULL,    -- 仿真时间（秒）
    state           JSON NOT NULL,              -- 关键状态：{position, state, currentTask, battery, load}
    changes         JSON,                        -- 相对于上次的变化

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_entity_history_tenant_id ON entity_state_history(tenant_id);
CREATE INDEX idx_entity_history_simulation_id ON entity_state_history(simulation_id);
CREATE INDEX idx_entity_history_entity_id ON entity_state_history(entity_id);
CREATE INDEX idx_entity_history_recorded_at ON entity_state_history(recorded_at);
```

#### 3.1.8 事件日志表 (event_log)

**决策 DB-6**: 可配置级别（默认 INFO+WARNING+ERROR）

```sql
CREATE TABLE event_log (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,

    -- 事件信息
    event_type      VARCHAR(100) NOT NULL,
    event_time      DECIMAL(10, 2) NOT NULL,    -- 仿真时间（秒）
    severity        VARCHAR(20) NOT NULL,       -- DEBUG, INFO, WARNING, ERROR

    -- 事件数据
    source_module   VARCHAR(50),                -- 触发模块
    source_entity   VARCHAR(100),               -- 触发事件的实体
    related_entities JSON,                       -- 相关实体 ID 数组：["entity-1", "entity-2"]
    data            JSON,                        -- 事件详细数据

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT event_log_severity_check CHECK (severity IN (
        'DEBUG', 'INFO', 'WARNING', 'ERROR'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_event_log_tenant_id ON event_log(tenant_id);
CREATE INDEX idx_event_log_simulation_id ON event_log(simulation_id);
CREATE INDEX idx_event_log_event_type ON event_log(event_type);
CREATE INDEX idx_event_log_severity ON event_log(severity);
CREATE INDEX idx_event_log_event_time ON event_log(event_time);
```

#### 3.1.9 审计日志表 (audit_log)

**决策 DB-8**: 需要审计日志

```sql
CREATE TABLE audit_log (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    user_id         VARCHAR(100) NOT NULL,
    action          VARCHAR(50) NOT NULL,       -- CREATE, UPDATE, DELETE, START_SIMULATION, STOP_SIMULATION
    entity_type     VARCHAR(50) NOT NULL,       -- SCENE, SIMULATION, TASK, USER
    entity_id       CHAR(36),

    -- 变更内容（JSON 格式）
    changes         JSON,                       -- {before: {...}, after: {...}}
    reason          TEXT,                       -- 操作原因

    -- 上下文
    ip_address      VARCHAR(45),               -- IPv6 支持
    user_agent      TEXT,

    timestamp       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT audit_log_action_check CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'START_SIMULATION', 'STOP_SIMULATION',
        'EXPORT_REPORT', 'IMPORT_SCENE', 'LOGIN', 'LOGOUT'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);
```

#### 3.1.10 文件存储表 (files)

**决策 DB-9**: MinIO 对象存储

```sql
CREATE TABLE files (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    -- 文件信息
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(50) NOT NULL,       -- REPORT, SCENE_IMPORT, EXPORT, MODEL_3D
    mime_type       VARCHAR(100),
    file_size       BIGINT NOT NULL,

    -- 存储信息
    storage_type    VARCHAR(20) NOT NULL DEFAULT 'MINIO',  -- MINIO, FILESYSTEM
    storage_url     VARCHAR(500) NOT NULL,      -- MinIO URL 或文件路径
    bucket_name     VARCHAR(255),               -- MinIO bucket (可选)
    object_key      VARCHAR(500),               -- MinIO object key
    checksum        VARCHAR(64),                 -- SHA-256

    -- 关联
    entity_type     VARCHAR(50),                -- SIMULATION, SCENE
    entity_id       CHAR(36),

    -- 有效期
    expires_at      DATETIME(3),                 -- 可选，用于临时文件

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),

    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_files_tenant_id ON files(tenant_id);
CREATE INDEX idx_files_entity ON files(entity_type, entity_id);
CREATE INDEX idx_files_expires_at ON files(expires_at) WHERE expires_at IS NOT NULL;
```

#### 3.1.11 模型库表 (model_library)

**描述**: 管理可视化模型元数据

```sql
CREATE TABLE model_library (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    model_type      VARCHAR(50) NOT NULL,   -- OHT_VEHICLE, AGV_VEHICLE, STOCKER, ...
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DISABLED
    default_version_id CHAR(36),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_model_library_tenant_id ON model_library(tenant_id);
CREATE INDEX idx_model_library_type ON model_library(model_type);
```

#### 3.1.12 模型版本表 (model_versions)

**描述**: 管理模型版本与 MinIO 文件关联

```sql
CREATE TABLE model_versions (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    model_id        CHAR(36) NOT NULL,
    version         VARCHAR(50) NOT NULL,   -- v1.0.0
    file_id         CHAR(36) NOT NULL,      -- 关联 files 表
    scale           JSON,                   -- {x,y,z}
    rotation        JSON,                   -- {x,y,z}
    pivot           JSON,                   -- {x,y,z}
    is_default      BOOLEAN DEFAULT FALSE,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (model_id) REFERENCES model_library(id),
    FOREIGN KEY (file_id) REFERENCES files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_model_versions_model_id ON model_versions(model_id);
CREATE INDEX idx_model_versions_tenant_id ON model_versions(tenant_id);
```

#### 3.1.13 实体模型绑定表 (entity_model_binding)

**描述**: 场景实体与模型版本绑定

```sql
CREATE TABLE entity_model_binding (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    model_version_id CHAR(36) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id),
    FOREIGN KEY (model_version_id) REFERENCES model_versions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_entity_model_binding_scene_id ON entity_model_binding(scene_id);
CREATE INDEX idx_entity_model_binding_entity_id ON entity_model_binding(entity_id);
```

#### 3.1.14 车辆配置表 (vehicle_configs)

```sql
CREATE TABLE vehicle_configs (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,

    vehicle_id      VARCHAR(100) NOT NULL,
    vehicle_type    VARCHAR(50) NOT NULL,

    -- 配置参数
    config          JSON NOT NULL,               -- {speed, maxLoad, batteryCapacity, ...}

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT vehicle_configs_scene_vehicle UNIQUE (scene_id, vehicle_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vehicle_configs_tenant_id ON vehicle_configs(tenant_id);
CREATE INDEX idx_vehicle_configs_scene_id ON vehicle_configs(scene_id);
```

**vehicle_configs 配置项清单（建议）**:
```
# 通用配置
speed              # m/s
acceleration       # m/s^2
deceleration       # m/s^2
length             # m
safetyDistance     # m
priority           # 默认优先级

# OHT 专用
maxLoad            # kg
trackId            # 轨道标识
liftSpeed          # m/s

# AGV 专用
maxLoad            # kg
batteryCapacity    # kWh
batteryLevel       # 0~1 初始值
chargingRate       # kW
chargeThreshold    # 0~1 触发充电阈值
networkId          # 路网标识
```

#### 3.1.15 日志级别配置表 (log_level_config)

```sql
CREATE TABLE log_level_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',

    `module`        VARCHAR(50) NOT NULL,       -- 模块名（如 "traffic", "scheduler"）
    level           VARCHAR(20) NOT NULL,       -- DEBUG, INFO, WARNING, ERROR

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT log_level_config_tenant_module UNIQUE (tenant_id, `module`),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_log_level_tenant_module ON log_level_config(tenant_id, `module`);

-- 默认配置（INSERT 时没有配置的模块使用 INFO）
-- 系统配置可设置全局默认
```

#### 3.1.16 Schema 版本控制表

```sql
CREATE TABLE schema_migrations (
    version         INT PRIMARY KEY AUTO_INCREMENT,
    description     VARCHAR(255) NOT NULL,
    script          TEXT NOT NULL,              -- 迁移 SQL 脚本
    rollback_script TEXT,                      -- 回滚 SQL 脚本
    applied_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 初始版本
INSERT INTO schema_migrations (version, description, script) VALUES (
    1,
    'Initial schema with multi-tenant support',
    '-- Initial table creation script'
);
```

---

#### 3.1.17 系统配置表 (system_config)

**描述**: 统一管理全局/租户级配置参数

```sql
CREATE TABLE system_config (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    config_key      VARCHAR(100) NOT NULL,
    config_value    JSON NOT NULL,
    description     TEXT,
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_system_config UNIQUE (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_system_config_tenant_id ON system_config(tenant_id);
CREATE INDEX idx_system_config_key ON system_config(config_key);
```

**建议配置项清单（统一落库）**:
```
# 交通管制
controlpoint.default_capacity
controlarea.default_capacity
edge.default_capacity
edge.release_policy                 # head / full_body
traffic.preemption
traffic.reservation_mode            # immediate / time_window
traffic.deadlock.strategy           # wait_graph+timeout
traffic.deadlock.timeout            # seconds (sim)
traffic.replan.timeoutSeconds        # seconds (sim), default 60
traffic.replan.maxAttempts          # default 3
traffic.priority.aging.enabled
traffic.priority.aging.step         # seconds (sim)
traffic.priority.aging.boost
traffic.priority.aging.max
traffic.processing.mode             # event / periodic
traffic.acquire.order               # cp_then_edge
traffic.failure.release_policy      # keep_current_release_future

# 调度系统
dispatch.cycle.interval             # seconds (sim)
dispatch.preempt.enabled
dispatch.reassign.on_failure

# 仿真时钟
sim.timeScale.default
sim.duration.default
sim.random.seed

# 模型与渲染
model.render.fallback               # true/false
model.default_version.policy        # latest / markedDefault

# 前端可视化
ui.refresh.interval                 # 200/500/1000 ms
ui.heatmap.enabled
ui.statusboard.enabled

# 草稿
draft.autosave.enabled
draft.autosave.interval             # seconds

# 概率模型
dist.supported
dist.default.repair
dist.default.mtbf
dist.default.process

# Workflow 工作流自动化
workflow.retry.maxRetries               # 默认 3
workflow.retry.backoffStrategy         # EXPONENTIAL / LINEAR / FIXED
workflow.retry.initialDelay            # 默认 1.0 (秒，仿真时间)
workflow.retry.maxDelay                # 默认 60.0 (秒，仿真时间)
workflow.retry.backoffMultiplier       # 默认 2.0
workflow.retry.jitterEnabled           # 默认 true
workflow.resource.validationStrategy   # BLOCK_AND_RETRY / ENQUEUE_AND_WAIT (v1.5)
workflow.resource.timeout              # 默认 60.0 (秒，仿真时间)
workflow.wip.defaultLimit               # 默认 0 (无限制)
workflow.wip.globalEnabled              # 默认 false

# SafetyZone 安全区
safetyzone.default.maxHumans           # 默认 10
safetyzone.default.maxVehicles         # 默认 2
safetyzone.default.accessPriority      # HUMAN_FIRST / VEHICLE_FIRST / FIFO / PRIORITY_BASED (v1.5)

# 多候选设备选择（v2.0）
dispatch.selector.weight.distance      # 默认 0.4（距离因子权重）
dispatch.selector.weight.time          # 默认 0.4（时间因子权重）
dispatch.selector.weight.wip           # 默认 0.2（WIP 因子权重）
dispatch.selector.normalization        # 默认 min-max（归一化方法）
```

---

### 3.2 视图设计

#### 3.2.1 仿真汇总视图

**修正**: 每个仿真取最新聚合 KPI（使用窗口函数或 JOIN 子查询）

**依赖**: MySQL 8.0.16+ (窗口函数 `ROW_NUMBER() OVER` 要求)

```sql
CREATE VIEW v_simulation_summary AS
SELECT
    s.id,
    s.tenant_id,
    s.name,
    s.status,
    s.simulated_time,
    s.started_at,
    s.completed_at,
    sc.name AS scene_name,
    sc.version AS scene_version,
    km.tasks_completed,
    km.tasks_per_hour,
    km.vehicle_utilization
FROM simulations s
INNER JOIN scenes sc ON s.scene_id = sc.id
LEFT JOIN (
    -- 使用窗口函数，每个 simulation_id 取最新的聚合 KPI
    SELECT
        simulation_id,
        tasks_completed,
        tasks_per_hour,
        vehicle_utilization
    FROM (
        SELECT
            simulation_id,
            tasks_completed,
            tasks_per_hour,
            vehicle_utilization,
            ROW_NUMBER() OVER (
                PARTITION BY simulation_id
                ORDER BY recorded_at DESC
            ) AS rn
        FROM kpi_metrics
        WHERE is_aggregated = TRUE
    ) ranked
    WHERE rn = 1
) km ON s.id = km.simulation_id;
```

#### 3.2.2 任务统计视图

**修正**: 使用仿真时间口径（completed_time - created_time）

```sql
CREATE VIEW v_task_statistics AS
SELECT
    simulation_id,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
    SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_count,
    -- 仿真时间口径：completed_time - created_time（单位：秒）
    AVG(CASE WHEN status = 'COMPLETED'
        THEN completed_time - created_time
        ELSE NULL END) AS avg_completion_time_sim_seconds,
    -- 最长/最短完成时间（仿真时间）
    MAX(CASE WHEN status = 'COMPLETED'
        THEN completed_time - created_time
        ELSE NULL END) AS max_completion_time_sim_seconds,
    MIN(CASE WHEN status = 'COMPLETED'
        THEN completed_time - created_time
        ELSE NULL END) AS min_completion_time_sim_seconds
FROM tasks
GROUP BY simulation_id;
```

---

## 4. Redis 缓存策略

### 4.1 缓存架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                         │
│  ┌───────────────┐  ┌───────────────┐  ┌─────────────────┐  │
│  │ Repository    │  │ Service       │  │ Controller      │  │
│  └───────┬───────┘  └───────┬───────┘  └─────┬─────────────┘  │
└──────────┼──────────────────┼──────────────────┼────────────────┘
           │                  │                  │
┌──────────▼──────────────────▼──────────────────▼────────────────┐
│                   Redisson Client                               │
│  - 分布式锁                                                     │
│  - 缓存接口                                                     │
│  - Pub/Sub                                                      │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                    Redis Cluster                              │
│  - 路径缓存                                                     │
│  - 会话缓存                                                     │
│  - 分布式锁                                                     │
│  - 发布订阅                                                     │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Redis 缓存数据结构

```java
// Redisson 配置
@Configuration
public class RedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${redis.host}") String host,
            @Value("${redis.port}") int port) {

        Config config = new Config();
        config.useSingleServer()
              .setAddress(host + ":" + port)
              .setConnectionPoolSize(64)
              .setConnectionMinimumIdleSize(24)
              .setIdleConnectionTimeout(10000)
              .setConnectTimeout(10000)
              .setTimeout(3000)
              .setRetryAttempts(3)
              .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
```

#### 4.2.1 路径缓存

```java
/**
 * 路径缓存实现
 * Key: path:{from}:{to}:{vehicleType}
 * Value: JSON 序列化的 Path 对象
 * TTL: 1 小时
 */
public class PathCache {

    private final RedissonClient redisson;
    private static final String KEY_PREFIX = "path:";
    private static final long TTL_SECONDS = 3600; // 1 小时

    public void put(String from, String to, VehicleType vehicleType, Path path) {
        String key = KEY_PREFIX + from + ":" + to + ":" + vehicleType;
        RBatchedObject<Long> bucket = redisson.getBatchedObject(Long.class);

        // 使用 RMap 实现本地缓存 + Redis 分布式缓存
        RMapCache<String, Path> cache = redisson.getMapCache(key, Codec.default());
        cache.put("value", path);
        cache.expire(1, TimeUnit.HOURS);
    }

    public Optional<Path> get(String from, String to, VehicleType vehicleType) {
        String key = KEY_PREFIX + from + ":" + to + ":" + vehicleType;
        RMapCache<String, Path> cache = redisson.getMapCache(key, Codec.default());
        return Optional.ofNullable(cache.get("value"));
    }

    // 失效所有路径缓存（路网变化时调用）
    public void invalidateAll() {
        RKeys keys = redisson.getKeys(KEY_PREFIX + "*");
        keys.forEach(key -> {
            redisson.getMapCache(key, Codec.default()).delete();
        });
    }
}
```

#### 4.2.2 会话缓存

```java
/**
 * WebSocket 会话缓存
 * Key: session:{simulationId}:{clientId}
 * Value: Session 信息
 * TTL: 24 小时
 */
public class SessionCache {

    private final RedissonClient redisson;
    private static final String KEY_PREFIX = "session:";

    public void put(String simulationId, String clientId, SessionInfo sessionInfo) {
        String key = KEY_PREFIX + simulationId + ":" + clientId;
        RBucket<SessionInfo> bucket = redisson.getBucket(key);
        bucket.set(sessionInfo);
        bucket.expire(24, TimeUnit.HOURS);
    }

    public Optional<SessionInfo> get(String simulationId, String clientId) {
        String key = KEY_PREFIX + simulationId + ":" + clientId;
        RBucket<SessionInfo> bucket = redisson.getBucket(key);
        return Optional.ofNullable(bucket.get());
    }
}
```

#### 4.2.3 分布式锁

```java
/**
 * 场景编辑锁（乐观锁的补充）
 * Key: lock:scene:{sceneId}
 */
public class SceneEditLock {

    private final RedissonClient redisson;

    public boolean tryLock(String sceneId, long leaseTime, TimeUnit unit) {
        String lockKey = "lock:scene:" + sceneId;
        RLock lock = redisson.getLock(lockKey);
        return lock.tryLock();
    }

    public void unlock(String sceneId) {
        String lockKey = "lock:scene:" + sceneId;
        RLock lock = redisson.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

#### 4.2.4 发布订阅（仿真事件）

```java
/**
 * 仿真事件发布
 * Topic: simulation:{simulationId}:events
 */
public class SimulationEventPublisher {

    private final RedissonClient redisson;

    public void publishEvent(String simulationId, SimulationEvent event) {
        String topic = "simulation:" + simulationId + ":events";
        RTopic topic = redisson.getTopic(topic);
        topic.publish(event);
    }
}

/**
 * 仿真事件订阅
 */
public class SimulationEventSubscriber {

    private final RedissonClient redisson;

    public void subscribe(String simulationId, Consumer<SimulationEvent> handler) {
        String topic = "simulation:" + simulationId + ":events";
        RTopic simulationTopic = redisson.getTopic(topic);

        simulationTopic.addListener(new MessageListener<SimulationEvent>() {
            @Override
            public void onMessage(CharSequence channel, SimulationEvent msg) {
                handler.accept(msg);
            }
        });
    }
}
```

### 4.3 Redis 数据结构汇总

| 用途 | Key 模式 | 数据类型 | TTL | 说明 |
|------|---------|----------|-----|------|
| 路径缓存 | `path:{from}:{to}:{vehicleType}` | RMapCache | 1h | LRU 淘汰 |
| 会话缓存 | `session:{simulationId}:{clientId}` | RBucket | 24h | WebSocket 会话 |
| 场景编辑锁 | `lock:scene:{sceneId}` | RLock | 自动释放 | 分布式锁 |
| 仿真事件 | `simulation:{simulationId}:events` | RTopic | - | Pub/Sub |
| 用户会话 | `session:user:{userId}` | RBucket | 24h | 用户登录会话 |
| 限流器 | `ratelimit:{api}:{ip}` | RRateLimiter | - | API 限流 |

### 4.4 Redis 配置

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 1000ms

# Redisson 配置（独立配置）
redisson:
  address: redis://localhost:6379
  connection-pool-size: 64
  connection-minimum-idle-size: 24
  idle-connection-timeout: 10000
  connect-timeout: 10000
  timeout: 3000
  retry-attempts: 3
  retry-interval: 1500
  threads: 16
  nettyThreads: 32
```

### 4.5 缓存更新策略

| 场景 | 缓存操作 | 说明 |
|------|----------|------|
| 路径规划完成 | PUT | 计算完成后写入缓存 |
| 路网变化 | DELETE all | 路网变更时清空路径缓存 |
| 仿真开始 | PUT | 预加载常用路径 |
| 定期失效 | TTL | 1 小时后自动失效 |
| 内存压力 | LRU | Redis maxmemory-policy |

---

## 5. MinIO 对象存储

### 5.1 Bucket 设计

```bash
# Buckets
simulation-files       # 用户上传文件、报表导出
simulation-models       # 3D 模型资产
simulation-backups      # 数据库备份
```

### 5.2 文件组织结构

```
simulation-files/
├── {tenant_id}/
│   ├── reports/
│   │   ├── simulation/
│   │   │   └── {simulation_id}/
│   │   │       └── {file_id}.pdf
│   │   └── scene/
│   │       └── {scene_id}/
│   │           └── {file_id}.xlsx
│   └── uploads/
│       └── {date}/
│           └── {filename}

simulation-models/
├── oht_vehicle/
│   ├── basic/      # 程序化基础模型（Three.js 几何体）
│   └── detailed/   # 精细模型（MinIO 存储）
├── agv_vehicle/
├── stocker/
└── ...
```

### 5.3 MinIO 客户端配置

```java
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.accessKey}") String accessKey,
            @Value("${minio.secretKey}") String secretKey,
            @Value("${minio.bucket}") String bucket) {

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

```yaml
# application.yml
minio:
  endpoint: http://localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin
  bucket: simulation-files
```

---

## 6. 数据访问层设计

### 6.1 Repository 接口

```java
// 场景仓储
public interface SceneRepository {
    Scene save(Scene scene);
    Optional<Scene> findById(UUID id);
    List<Scene> findByTenantId(UUID tenantId);
    List<Scene> findByTenantIdAndCreatedBy(UUID tenantId, String userId);
    void deleteById(UUID id);

    // 版本管理
    Scene migrateToLatest(Scene scene);
}

// 仿真仓储
public interface SimulationRepository {
    Simulation save(Simulation simulation);
    Optional<Simulation> findById(UUID id);
    List<Simulation> findBySceneId(UUID sceneId);
    void updateStatus(UUID id, SimulationStatus status);

    // 快照管理
    void saveSnapshot(UUID simulationId, SimulationSnapshot snapshot);
    Optional<SimulationSnapshot> loadSnapshot(UUID simulationId);
}

// 指标仓储
public interface MetricsRepository {
    void save(Metrics metrics);
    void saveBatch(List<Metrics> metrics);
    List<Metrics> findBySimulationId(UUID simulationId);
    MetricsSummary getSummary(UUID simulationId);

    // 聚合数据
    void aggregateMetrics(UUID simulationId, double fromTime, double toTime);
}

// 任务仓储
public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(UUID id);
    List<Task> findBySimulationId(UUID simulationId);
    List<Task> findByStatus(TaskStatus status);
}

// 审计日志仓储
public interface AuditLogRepository {
    void log(AuditLogEntry entry);
    List<AuditLogEntry> findByEntityTypeAndId(String entityType, UUID entityId);
    List<AuditLogEntry> findByUserId(String userId);
}
```

### 6.2 缓存 Repository 装饰器

```java
/**
 * 带缓存的场景仓储
 */
public class CachedSceneRepository implements SceneRepository {

    private final SceneRepository delegate;
    private final RedissonClient redisson;

    private static final String CACHE_PREFIX = "scene:";
    private static final long CACHE_TTL_HOURS = 24;

    @Override
    public Optional<Scene> findById(UUID id) {
        String key = CACHE_PREFIX + id;
        RBucket<Scene> bucket = redisson.getBucket(key);

        Scene cached = bucket.get();
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<Scene> result = delegate.findById(id);
        result.ifPresent(scene -> {
            bucket.set(scene);
            bucket.expire(CACHE_TTL_HOURS, TimeUnit.HOURS);
        });

        return result;
    }

    @Override
    public Scene save(Scene scene) {
        Scene saved = delegate.save(scene);

        // 更新缓存
        String key = CACHE_PREFIX + scene.getId();
        RBucket<Scene> bucket = redisson.getBucket(key);
        bucket.set(saved);
        bucket.expire(CACHE_TTL_HOURS, TimeUnit.HOURS);

        return saved;
    }

    @Override
    public void deleteById(UUID id) {
        delegate.deleteById(id);

        // 清除缓存
        String key = CACHE_PREFIX + id;
        redisson.getBucket(key).delete();
    }
}
```

### 6.3 数据库连接池配置

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/simulation?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: simulation
    password: simulation
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 600000
      connection-timeout: 30000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
      validation-timeout: 3000
      register-mbeans: true

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        use_sql_comments: true
```

---

## 7. 数据迁移与版本管理

### 7.1 Flyway 迁移

```sql
-- V1__Initial_schema.sql
-- 创建所有初始表

-- V2__Add_index.sql
-- 添加索引

-- V3__Add_tenant_id.sql
-- 添加多租户支持
```

### 7.2 Flyway 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    encoding: UTF-8
```

---

## 8. 备份与恢复

### 8.1 MySQL 备份

```bash
# 全量备份
mysqldump -u simulation -p simulation > backup_$(date +%Y%m%d).sql

# 增量备份（binlog）
mysqladmin flush-logs
mysqlbinlog --start-datetime="2024-01-01 00:00:00" /var/lib/mysql/mysql-bin.000001 > backup_inc.sql
```

### 8.2 MinIO 备份

```bash
# 使用 mc 工具
mc mirror minio/simulation-files /backup/simulation-files-$(date +%Y%m%d)
```

---

## 9. 监控与维护

### 9.1 定期维护任务

```sql
-- 定期清理过期文件
DELETE FROM files WHERE expires_at < NOW();

-- 更新统计信息
ANALYZE TABLE scenes;
ANALYZE TABLE simulations;
ANALYZE TABLE tasks;

-- 清理碎片表
OPTIMIZE TABLE event_log;
OPTIMIZE TABLE kpi_metrics;

-- 归档旧任务（定时任务）
CALL archive_old_tasks();
```

### 9.2 监控指标

**MySQL 监控**:
- 连接池使用率
- 慢查询日志（>1s）
- 表大小增长
- 死锁检测

**Redis 监控**:
- 内存使用率
- 键数量
- 命令延迟
- 缓存命中率

**MinIO 监控**:
- 存储用量
- API 请求延迟
- 对象数量

---

## 10. 性能优化

### 10.1 索引策略

```sql
-- JSON 字段索引（MySQL 5.7+ 使用函数索引）
-- 创建虚拟列 + 索引（推荐）
ALTER TABLE scenes ADD COLUMN definition_type VARCHAR(50)
    AS (JSON_UNQUOTE(JSON_EXTRACT(definition, '$.type'))) STORED;
CREATE INDEX idx_scenes_definition_type ON scenes(definition_type);
```

### 10.2 分区表（数据量大时启用）

```sql
-- KPI 按月分区（使用墙钟时间 wall_clock_time，而非仿真时间 recorded_at）
ALTER TABLE kpi_metrics
PARTITION BY RANGE (TO_DAYS(wall_clock_time)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**分区字段说明**:
- 使用 `wall_clock_time`（墙钟时间）而非 `recorded_at`（仿真时间）
- 理由: 仿真时间可能回滚或跳跃，不适合分区；墙钟时间单调递增，便于数据管理

---

## 11. 技术选型汇总

| 组件 | 技术选型 | 版本/配置 |
|------|----------|-----------|
| **数据库** | MySQL | 8.0.16+ |
| **连接池** | HikariCP | max=20, min=5 |
| **缓存** | Redis | 7.0+ |
| **缓存客户端** | Redisson | 3.x+ (专用缓存) |
| **对象存储** | MinIO | 最新稳定版 |
| **ORM** | MyBatis-Plus | 3.5.5+ |
| **迁移工具** | Flyway/Liquibase | - |

**版本兼容性**:
- MySQL 8.0.16+ (CHECK 约束在 8.0.16 开始生效)
- JSON 默认值在 MySQL 8.0.13+ 支持，建议应用层设置默认值以获得最大兼容性（兼容性以 8.0.16+ 为准）

---

## 12. 部署架构

```
                    ┌─────────────────┐
                    │   Nginx (反向代理) │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                              │
         ┌──────▼──────────┐         ┌──────▼────────────┐
         │  Spring Boot   │         │   MySQL 8.0.16+  │
         │  Application   │◄───────►│  (主数据库)       │
         │                │         │                  │
         └────────┬───────┘         └───────────────────┘
                  │
         ┌────────▼──────────┐
         │  Redis 7.0+      │
         │  + Redisson      │
         │  (缓存层)        │
         └──────────────────┘
                  │
         ┌────────▼──────────┐
         │  MinIO          │
         │  (对象存储)      │
         └──────────────────┘
```

---

## 13. 建表 SQL 脚本（完整版）

```sql
-- ============================================
-- 物流仿真模块 - MySQL 建表脚本
-- Version: 1.0
-- Date: 2026-02-06
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 租户表
-- ============================================
CREATE TABLE tenants (
    id              CHAR(36) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 配置 (JSON 类型存储配置)
    -- 注意: JSON 默认值在 MySQL 8.0.13+ 支持，建议应用层设置默认值
    settings        JSON,                                   -- 应用层默认值为 {}

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. 场景表
-- ============================================
CREATE TABLE scenes (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    version         INT NOT NULL DEFAULT 1,
    schema_version  INT NOT NULL DEFAULT 1,
    definition      JSON NOT NULL,
    metadata        JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT scenes_name_version UNIQUE (tenant_id, name, version),
    CONSTRAINT scenes_tenant_id UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_scenes_tenant_id ON scenes(tenant_id);
CREATE INDEX idx_scenes_schema_version ON scenes(schema_version);
CREATE INDEX idx_scenes_created_at ON scenes(created_at);

-- scenes.definition JSON 结构说明（v2.0）:
-- {
--   "entities": [                                   // 实体列表
--     {
--       "id": "Machine-A",
--       "type": "MACHINE",                          // OHT_VEHICLE, AGV_VEHICLE, MACHINE, STOCKER, ERACK, MANUAL_STATION, CONVEYOR, OPERATOR, SAFETY_ZONE, CONTROL_POINT
--       "name": "光刻机 A",
--       "position": {"x": 10.0, "y": 20.0, "z": 0.0},
--       "enabled": true,
--       "supportedTransportTypes": ["OHT", "AGV"],  // v2.0: 设备类实体必填字段（MACHINE, STOCKER, ERACK, MANUAL_STATION, CONVEYOR）
--       "properties": {
--         "capacity": 2,
--         "processingTime": {...}
--       }
--     },
--     {
--       "id": "OHT-01",
--       "type": "OHT_VEHICLE",
--       "name": "天车 01",
--       "position": {"x": 0.0, "y": 0.0, "z": 10.0},
--       "properties": {
--         "trackId": "TRACK-A",
--         "maxLoad": 50.0,
--         "speed": 2.5
--       }
--     },
--     {
--       "id": "ZONE-FAB1-MAIN",
--       "type": "SAFETY_ZONE",
--       "name": "Fab 1 Main Aisle",
--       "geometryType": "RECTANGLE",
--       "center": {"x": 100.0, "y": 50.0, "z": 0.0},
--       "width": 20.0,
--       "height": 5.0,
--       "maxHumans": 10,
--       "maxVehicles": 2,
--       "accessPriority": "HUMAN_FIRST",
--       "enabled": true
--     }
--   ],
--   "paths": [                                      // 路径列表
--     {
--       "id": "TRACK-A",
--       "type": "OHT_TRACK",
--       "points": [...],
--       "segments": [...],
--       "controlPoints": [...]
--     },
--     {
--       "id": "AGV-NET-1",
--       "type": "AGV_NETWORK",
--       "nodes": [...],
--       "edges": [...]
--     }
--   ]
-- }

-- ============================================
-- 4. 仿真运行表
-- ============================================
CREATE TABLE simulations (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    name            VARCHAR(255),
    config          JSON NOT NULL,
    status          VARCHAR(20) NOT NULL,
    simulated_time  DECIMAL(10, 2),
    started_at      DATETIME(3),
    completed_at    DATETIME(3),
    result_snapshot JSON,
    result_summary  JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),
    CONSTRAINT simulations_status_check CHECK (status IN (
        'PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_simulations_tenant_id ON simulations(tenant_id);
CREATE INDEX idx_simulations_scene_id ON simulations(scene_id);
CREATE INDEX idx_simulations_status ON simulations(status);
CREATE INDEX idx_simulations_created_at ON simulations(created_at);

-- ============================================
-- 5. 任务表
-- ============================================
CREATE TABLE tasks (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    priority        VARCHAR(20) NOT NULL,
    source          VARCHAR(100),
    destination     VARCHAR(100),
    cargo_info      JSON,
    deadline        DECIMAL(10, 2),
    created_time    DECIMAL(10, 2) NOT NULL,
    assigned_time   DECIMAL(10, 2),
    started_time    DECIMAL(10, 2),
    completed_time  DECIMAL(10, 2),
    status          VARCHAR(20) NOT NULL,
    assigned_vehicle VARCHAR(100),
    result          JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT tasks_status_check CHECK (status IN (
        'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tasks_tenant_id ON tasks(tenant_id);
CREATE INDEX idx_tasks_simulation_id ON tasks(simulation_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_assigned_vehicle ON tasks(assigned_vehicle);

-- 归档表（显式定义，包含索引）
CREATE TABLE tasks_archive (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    priority        VARCHAR(20) NOT NULL,
    source          VARCHAR(100),
    destination     VARCHAR(100),
    cargo_info      JSON,
    deadline        DECIMAL(10, 2),
    created_time    DECIMAL(10, 2) NOT NULL,
    assigned_time   DECIMAL(10, 2),
    started_time    DECIMAL(10, 2),
    completed_time  DECIMAL(10, 2),
    status          VARCHAR(20) NOT NULL,
    assigned_vehicle VARCHAR(100),
    result          JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_tasks_archive_tenant_id (tenant_id),
    INDEX idx_tasks_archive_simulation_id (simulation_id),
    INDEX idx_tasks_archive_status (status),
    INDEX idx_tasks_archive_priority (priority),
    INDEX idx_tasks_archive_assigned_vehicle (assigned_vehicle),
    INDEX idx_tasks_archive_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER //
CREATE PROCEDURE archive_old_tasks()
BEGIN
    INSERT INTO tasks_archive
    SELECT * FROM tasks
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 3 MONTH);

    DELETE FROM tasks
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 3 MONTH);
END //
DELIMITER ;

-- ============================================
-- 6. KPI 指标表
-- ============================================
CREATE TABLE kpi_metrics (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    recorded_at     DECIMAL(10, 2) NOT NULL,
    wall_clock_time DATETIME(3) NOT NULL,
    tasks_completed INT DEFAULT 0,
    tasks_per_hour  DECIMAL(10, 2),
    material_throughput DECIMAL(10, 2),
    vehicle_utilization DECIMAL(5, 2),
    equipment_utilization DECIMAL(5, 2),
    wip_total       INT DEFAULT 0,
    energy_total    DECIMAL(10, 2),
    custom_metrics  JSON,
    is_aggregated   BOOLEAN DEFAULT FALSE,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_kpi_metrics_tenant_id ON kpi_metrics(tenant_id);
CREATE INDEX idx_kpi_metrics_simulation_id ON kpi_metrics(simulation_id);
CREATE INDEX idx_kpi_metrics_recorded_at ON kpi_metrics(recorded_at);

-- ============================================
-- 7. 实体状态历史表
-- ============================================
CREATE TABLE entity_state_history (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    recorded_at     DECIMAL(10, 2) NOT NULL,
    state           JSON NOT NULL,
    changes         JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_entity_history_tenant_id ON entity_state_history(tenant_id);
CREATE INDEX idx_entity_history_simulation_id ON entity_state_history(simulation_id);
CREATE INDEX idx_entity_history_entity_id ON entity_state_history(entity_id);
CREATE INDEX idx_entity_history_recorded_at ON entity_state_history(recorded_at);

-- ============================================
-- 8. 事件日志表
-- ============================================
CREATE TABLE event_log (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    event_time      DECIMAL(10, 2) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    source_module   VARCHAR(50),
    source_entity   VARCHAR(100),
    related_entities JSON,                       -- 相关实体 ID 数组：["entity-1", "entity-2"]
    data            JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT event_log_severity_check CHECK (severity IN (
        'DEBUG', 'INFO', 'WARNING', 'ERROR'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_event_log_tenant_id ON event_log(tenant_id);
CREATE INDEX idx_event_log_simulation_id ON event_log(simulation_id);
CREATE INDEX idx_event_log_event_type ON event_log(event_type);
CREATE INDEX idx_event_log_severity ON event_log(severity);
CREATE INDEX idx_event_log_event_time ON event_log(event_time);

-- ============================================
-- 9. 审计日志表
-- ============================================
CREATE TABLE audit_log (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    user_id         VARCHAR(100) NOT NULL,
    action          VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       CHAR(36),
    changes         JSON,
    reason          TEXT,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    timestamp       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT audit_log_action_check CHECK (action IN (
        'CREATE', 'UPDATE', 'DELETE', 'START_SIMULATION', 'STOP_SIMULATION',
        'EXPORT_REPORT', 'IMPORT_SCENE', 'LOGIN', 'LOGOUT'
    )),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp DESC);

-- ============================================
-- 10. 文件存储表
-- ============================================
CREATE TABLE files (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(50) NOT NULL,
    mime_type       VARCHAR(100),
    file_size       BIGINT NOT NULL,
    storage_type    VARCHAR(20) NOT NULL DEFAULT 'MINIO',
    storage_url     VARCHAR(500) NOT NULL,
    bucket_name     VARCHAR(255),
    object_key      VARCHAR(500),
    checksum        VARCHAR(64),
    entity_type     VARCHAR(50),
    entity_id       CHAR(36),
    expires_at      DATETIME(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_files_tenant_id ON files(tenant_id);
CREATE INDEX idx_files_entity ON files(entity_type, entity_id);
CREATE INDEX idx_files_expires_at ON files(expires_at) WHERE expires_at IS NOT NULL;

-- ============================================
-- 11. 场景草稿表
-- ============================================
CREATE TABLE scene_drafts (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    content         JSON NOT NULL,
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_scene_drafts_scene_id ON scene_drafts(scene_id);
CREATE INDEX idx_scene_drafts_tenant_id ON scene_drafts(tenant_id);

-- ============================================
-- 12. 模型库表
-- ============================================
CREATE TABLE model_library (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    model_type      VARCHAR(50) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    default_version_id CHAR(36),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_model_library_tenant_id ON model_library(tenant_id);
CREATE INDEX idx_model_library_type ON model_library(model_type);

-- ============================================
-- 13. 模型版本表
-- ============================================
CREATE TABLE model_versions (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    model_id        CHAR(36) NOT NULL,
    version         VARCHAR(50) NOT NULL,
    file_id         CHAR(36) NOT NULL,
    scale           JSON,
    rotation        JSON,
    pivot           JSON,
    is_default      BOOLEAN DEFAULT FALSE,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (model_id) REFERENCES model_library(id),
    FOREIGN KEY (file_id) REFERENCES files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_model_versions_model_id ON model_versions(model_id);
CREATE INDEX idx_model_versions_tenant_id ON model_versions(tenant_id);

-- ============================================
-- 14. 实体模型绑定表
-- ============================================
CREATE TABLE entity_model_binding (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    model_version_id CHAR(36) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id),
    FOREIGN KEY (model_version_id) REFERENCES model_versions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_entity_model_binding_scene_id ON entity_model_binding(scene_id);
CREATE INDEX idx_entity_model_binding_entity_id ON entity_model_binding(entity_id);

-- ============================================
-- 15. 车辆配置表
-- ============================================
CREATE TABLE vehicle_configs (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    vehicle_id      VARCHAR(100) NOT NULL,
    vehicle_type    VARCHAR(50) NOT NULL,
    config          JSON NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT vehicle_configs_scene_vehicle UNIQUE (scene_id, vehicle_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vehicle_configs_tenant_id ON vehicle_configs(tenant_id);
CREATE INDEX idx_vehicle_configs_scene_id ON vehicle_configs(scene_id);

-- ============================================
-- 16. 日志级别配置表
-- ============================================
CREATE TABLE log_level_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    `module`        VARCHAR(50) NOT NULL,
    level           VARCHAR(20) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT log_level_config_tenant_module UNIQUE (tenant_id, `module`),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_log_level_tenant_module ON log_level_config(tenant_id, `module`);

-- ============================================
-- 17. 场景模型版本表（主 schema + $ref 引用）
-- ============================================
CREATE TABLE scene_model_versions (
    version INT PRIMARY KEY AUTO_INCREMENT,
    schema_definition JSON NOT NULL,
    migration_script TEXT,
    rollback_script TEXT,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO scene_model_versions (version, schema_definition) VALUES (
    1,
    '{
        "$schema": "http://json-schema.org/draft-07/schema#",
        "type": "object",
        "required": ["entities", "paths"],
        "properties": {
            "entities": {
                "type": "array",
                "items": {
                    "$ref": "schemas/entity-base.json#/definitions/entity"
                }
            },
            "paths": {
                "type": "array",
                "items": {
                    "$ref": "schemas/path.json#/definitions/path"
                }
            }
        }
    }'
);

-- ============================================
-- 18. Schema 版本控制表
-- ============================================
CREATE TABLE schema_migrations (
    version         INT PRIMARY KEY AUTO_INCREMENT,
    description     VARCHAR(255) NOT NULL,
    script          TEXT NOT NULL,
    rollback_script TEXT,
    applied_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO schema_migrations (version, description, script) VALUES (
    1,
    'Initial schema with multi-tenant support',
    '-- Initial table creation script'
);

-- ============================================
-- 19. 系统配置表
-- ============================================
CREATE TABLE system_config (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    config_key      VARCHAR(100) NOT NULL,
    config_value    JSON NOT NULL,
    description     TEXT,
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_system_config UNIQUE (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_system_config_tenant_id ON system_config(tenant_id);
CREATE INDEX idx_system_config_key ON system_config(config_key);

-- ============================================
-- 20. 工艺流程表 (process_flows) - v1.5
-- ============================================
-- Flyway 迁移脚本: V2__create_process_flows.sql (在 V1__Initial_schema.sql 之后)
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
    INDEX idx_flows_tenant_template (tenant_id, is_template),
    INDEX idx_flows_tenant_created_by (tenant_id, created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- definition JSON 结构说明（v2.0）:
-- {
--   "steps": [                                       // 工序列表
--     {
--       "id": "step-001",
--       "name": "光刻",
--       "sequence": 1,
--       "targetEntityIds": ["Machine-A", "Machine-B"],  // v2.0: 支持多候选设备
--       "requiredTransportTypes": ["OHT", "AGV"],      // v2.0: 必需的运输类型
--       "processingTime": {
--         "type": "NORMAL",
--         "mean": 50,
--         "std": 5
--       },
--       "wipLimit": 3,
--       "outputStrategy": { ... },                      // 可选，覆盖默认策略
--       "transportSelector": { ... },                   // 可选，覆盖默认选择器
--       "predecessorIds": ["step-000"],                 // 可选
--       "successorId": "step-002"                       // 可选
--     }
--   ],
--   "defaultOutputStrategy": {                        // 默认产出策略
--     "type": "DIRECT_TO_NEXT",                       // 可选: INTERMEDIATE_STORAGE, BUFFER_ZONE, CONDITIONAL, DYNAMIC_SELECT
--     "singleDestination": "...",                     // 用于 DIRECT_TO_NEXT
--     "storageLocation": "...",                       // 用于 INTERMEDIATE_STORAGE/BUFFER_ZONE
--     "candidateList": [...],                         // 用于 DYNAMIC_SELECT
--     "conditionalRoutes": [...]                      // 用于 CONDITIONAL
--   },
--   "defaultTransportSelector": {                     // 默认搬运选择器
--     "policy": "HYBRID",                             // 可选: DISTANCE_FIRST, TIME_FIRST, WIP_FIRST, PRIORITY_BASED
--     "allowedTypes": ["OHT", "AGV", "HUMAN"],        // 允许的实体类型
--     "distanceWeight": 0.4,                          // 用于 HYBRID
--     "timeWeight": 0.4,
--     "wipWeight": 0.2
--   },
--   "enabled": true,
--   "properties": {...}
-- }

-- ============================================
-- 21. 工艺流程绑定表 (process_flow_bindings) - v1.5
-- ============================================
-- Flyway 迁移脚本: V3__create_process_flow_bindings.sql (在 V2__create_process_flows.sql 之后)
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
    FOREIGN KEY (tenant_id, flow_id, flow_version) REFERENCES process_flows(tenant_id, id, version) ON DELETE CASCADE,
    INDEX idx_bindings_tenant_scene (tenant_id, scene_id, enabled),
    INDEX idx_bindings_tenant_flow (tenant_id, flow_id, flow_version),
    INDEX idx_bindings_tenant_entry_point (tenant_id, entry_point_id),
    INDEX idx_bindings_tenant_priority (tenant_id, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- trigger_condition JSON 结构说明（v1.5）:
-- {
--   "entryPointId": "Bay-01",                         // 必填，与 entry_point_id 一致
--   "materialType": "WAFER",                         // 可选，物料类型
--   "materialGrade": "A",                            // 可选，物料等级
--   "materialBatch": "LOT-*",                        // 可选，批次号（支持通配符）
--   "materialAttributes": {...},                     // 可选，自定义物料属性
--   "processTag": "NORMAL",                          // 可选，工艺标记
--   "allowedProcessTags": ["NORMAL", "URGENT"],      // 可选，允许的工艺标记列表
--   "customCondition": "..."                         // 可选，自定义条件表达式
-- }

-- ============================================
-- 22. 默认租户
-- ============================================
-- 使用 INSERT IGNORE 避免重复执行时出错
INSERT IGNORE INTO tenants (id, name, status) VALUES
    ('00000000-0000-0000-0000-000000000000', 'default', 'ACTIVE');

-- ============================================
-- 23. 视图定义
-- ============================================

-- 仿真汇总视图（每个仿真取最新聚合 KPI）
CREATE VIEW v_simulation_summary AS
SELECT
    s.id,
    s.tenant_id,
    s.name,
    s.status,
    s.simulated_time,
    s.started_at,
    s.completed_at,
    sc.name AS scene_name,
    sc.version AS scene_version,
    km.tasks_completed,
    km.tasks_per_hour,
    km.vehicle_utilization
FROM simulations s
INNER JOIN scenes sc ON s.scene_id = sc.id
LEFT JOIN (
    SELECT
        simulation_id,
        tasks_completed,
        tasks_per_hour,
        vehicle_utilization
    FROM (
        SELECT
            simulation_id,
            tasks_completed,
            tasks_per_hour,
            vehicle_utilization,
            ROW_NUMBER() OVER (
                PARTITION BY simulation_id
                ORDER BY recorded_at DESC
            ) AS rn
        FROM kpi_metrics
        WHERE is_aggregated = TRUE
    ) ranked
    WHERE rn = 1
) km ON s.id = km.simulation_id;

-- 任务统计视图（仿真时间口径）
CREATE VIEW v_task_statistics AS
SELECT
    simulation_id,
    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
    SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress_count,
    AVG(CASE WHEN status = 'COMPLETED'
        THEN completed_time - created_time
        ELSE NULL END) AS avg_completion_time_sim_seconds,
    MAX(CASE WHEN status = 'COMPLETED'
        THEN completed_time - created_time
        ELSE NULL END) AS max_completion_time_sim_seconds,
    MIN(CASE WHEN status = 'COMPLETED'
        THEN completed_time - created_time
        ELSE NULL END) AS min_completion_time_sim_seconds
FROM tasks
GROUP BY simulation_id;

SET FOREIGN_KEY_CHECKS = 1;
```
