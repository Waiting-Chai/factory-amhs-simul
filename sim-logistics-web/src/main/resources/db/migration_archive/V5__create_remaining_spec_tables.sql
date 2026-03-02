-- ============================================
-- Phase 5.x Remaining Spec Tables Bootstrap
-- Version: V5
-- Date: 2026-02-11
-- Author: shentw
--
-- Purpose:
-- Add remaining schema tables defined in OpenSpec database-schema.md
-- that are not yet created by V1~V4 migrations.
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------
-- 1) simulations
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS simulations (
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

    CONSTRAINT simulations_status_check CHECK (status IN ('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_simulations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_simulations_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_simulations_tenant_id ON simulations(tenant_id);
CREATE INDEX idx_simulations_scene_id ON simulations(scene_id);
CREATE INDEX idx_simulations_status ON simulations(status);
CREATE INDEX idx_simulations_created_at ON simulations(created_at);

-- ----------------------------------------------------------------------
-- 2) tasks and tasks_archive
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tasks (
    id               CHAR(36) PRIMARY KEY,
    tenant_id        CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id    CHAR(36) NOT NULL,
    type             VARCHAR(50) NOT NULL,
    priority         VARCHAR(20) NOT NULL,
    source           VARCHAR(100),
    destination      VARCHAR(100),
    cargo_info       JSON,
    deadline         DECIMAL(10, 2),
    created_time     DECIMAL(10, 2) NOT NULL,
    assigned_time    DECIMAL(10, 2),
    started_time     DECIMAL(10, 2),
    completed_time   DECIMAL(10, 2),
    status           VARCHAR(20) NOT NULL,
    assigned_vehicle VARCHAR(100),
    result           JSON,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT tasks_status_check CHECK (status IN ('PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_tasks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tasks_simulation FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tasks_tenant_id ON tasks(tenant_id);
CREATE INDEX idx_tasks_simulation_id ON tasks(simulation_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_assigned_vehicle ON tasks(assigned_vehicle);

CREATE TABLE IF NOT EXISTS tasks_archive (
    id               CHAR(36) PRIMARY KEY,
    tenant_id        CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id    CHAR(36) NOT NULL,
    type             VARCHAR(50) NOT NULL,
    priority         VARCHAR(20) NOT NULL,
    source           VARCHAR(100),
    destination      VARCHAR(100),
    cargo_info       JSON,
    deadline         DECIMAL(10, 2),
    created_time     DECIMAL(10, 2) NOT NULL,
    assigned_time    DECIMAL(10, 2),
    started_time     DECIMAL(10, 2),
    completed_time   DECIMAL(10, 2),
    status           VARCHAR(20) NOT NULL,
    assigned_vehicle VARCHAR(100),
    result           JSON,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tasks_archive_tenant_id ON tasks_archive(tenant_id);
CREATE INDEX idx_tasks_archive_simulation_id ON tasks_archive(simulation_id);
CREATE INDEX idx_tasks_archive_status ON tasks_archive(status);
CREATE INDEX idx_tasks_archive_priority ON tasks_archive(priority);
CREATE INDEX idx_tasks_archive_assigned_vehicle ON tasks_archive(assigned_vehicle);
CREATE INDEX idx_tasks_archive_created_at ON tasks_archive(created_at);

-- ----------------------------------------------------------------------
-- 3) kpi_metrics
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kpi_metrics (
    id                     CHAR(36) PRIMARY KEY,
    tenant_id              CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id          CHAR(36) NOT NULL,
    recorded_at            DECIMAL(10, 2) NOT NULL,
    wall_clock_time        DATETIME(3) NOT NULL,
    tasks_completed        INT DEFAULT 0,
    tasks_per_hour         DECIMAL(10, 2),
    material_throughput    DECIMAL(10, 2),
    vehicle_utilization    DECIMAL(5, 2),
    equipment_utilization  DECIMAL(5, 2),
    wip_total              INT DEFAULT 0,
    energy_total           DECIMAL(10, 2),
    custom_metrics         JSON,
    is_aggregated          BOOLEAN DEFAULT FALSE,
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT fk_kpi_metrics_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_kpi_metrics_simulation FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_kpi_metrics_tenant_id ON kpi_metrics(tenant_id);
CREATE INDEX idx_kpi_metrics_simulation_id ON kpi_metrics(simulation_id);
CREATE INDEX idx_kpi_metrics_recorded_at ON kpi_metrics(recorded_at);

-- ----------------------------------------------------------------------
-- 4) entity_state_history
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entity_state_history (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    recorded_at     DECIMAL(10, 2) NOT NULL,
    state           JSON NOT NULL,
    changes         JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT fk_entity_state_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_entity_state_history_sim FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_entity_history_tenant_id ON entity_state_history(tenant_id);
CREATE INDEX idx_entity_history_simulation_id ON entity_state_history(simulation_id);
CREATE INDEX idx_entity_history_entity_id ON entity_state_history(entity_id);
CREATE INDEX idx_entity_history_recorded_at ON entity_state_history(recorded_at);

-- ----------------------------------------------------------------------
-- 5) event_log
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_log (
    id               CHAR(36) PRIMARY KEY,
    tenant_id        CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id    CHAR(36) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    event_time       DECIMAL(10, 2) NOT NULL,
    severity         VARCHAR(20) NOT NULL,
    source_module    VARCHAR(50),
    source_entity    VARCHAR(100),
    related_entities JSON,
    data             JSON,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT event_log_severity_check CHECK (severity IN ('DEBUG', 'INFO', 'WARNING', 'ERROR')),
    CONSTRAINT fk_event_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_event_log_simulation FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_event_log_tenant_id ON event_log(tenant_id);
CREATE INDEX idx_event_log_simulation_id ON event_log(simulation_id);
CREATE INDEX idx_event_log_event_type ON event_log(event_type);
CREATE INDEX idx_event_log_severity ON event_log(severity);
CREATE INDEX idx_event_log_event_time ON event_log(event_time);

-- ----------------------------------------------------------------------
-- 6) audit_log
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
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
    CONSTRAINT fk_audit_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);

-- ----------------------------------------------------------------------
-- 7) log_level_config
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS log_level_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    `module`        VARCHAR(50) NOT NULL,
    level           VARCHAR(20) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT log_level_config_tenant_module UNIQUE (tenant_id, `module`),
    CONSTRAINT fk_log_level_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_log_level_tenant_module ON log_level_config(tenant_id, `module`);

-- ----------------------------------------------------------------------
-- 8) scene_model_versions
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scene_model_versions (
    version            INT PRIMARY KEY AUTO_INCREMENT,
    schema_definition  JSON NOT NULL,
    migration_script   TEXT,
    rollback_script    TEXT,
    created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO scene_model_versions (version, schema_definition)
SELECT 1,
       JSON_OBJECT(
           '$schema', 'http://json-schema.org/draft-07/schema#',
           'type', 'object',
           'required', JSON_ARRAY('entities', 'paths')
       )
WHERE NOT EXISTS (SELECT 1 FROM scene_model_versions WHERE version = 1);

SET FOREIGN_KEY_CHECKS = 1;
