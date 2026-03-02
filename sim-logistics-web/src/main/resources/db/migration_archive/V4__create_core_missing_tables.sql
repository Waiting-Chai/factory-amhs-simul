-- ============================================
-- Phase 5.x Core Missing Tables Bootstrap
-- Version: V4
-- Date: 2026-02-11
-- Author: shentw
--
-- Purpose:
-- 1) Add missing core tables required by current web module and frontend contracts
-- 2) Keep Flyway/JDBC-friendly SQL (no DELIMITER/procedure)
-- 3) Preserve idempotency with IF NOT EXISTS + INSERT guarded by NOT EXISTS
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------
-- 1) File metadata table (MinIO/object storage metadata)
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS files (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT NOT NULL DEFAULT 0,
    storage_bucket  VARCHAR(100) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    storage_url     VARCHAR(1000),
    checksum        VARCHAR(128),
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(100),
    expires_at      DATETIME(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(100),

    CONSTRAINT fk_files_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_files_tenant_id ON files(tenant_id);
CREATE INDEX idx_files_entity ON files(entity_type, entity_id);
CREATE INDEX idx_files_expires_at ON files(expires_at);

-- ----------------------------------------------------------------------
-- 2) System configuration table (used by MysqlSystemConfigAdapter)
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_config (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    config_key      VARCHAR(255) NOT NULL,
    config_value    JSON,
    description     VARCHAR(500),
    category        VARCHAR(100),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT uq_system_config UNIQUE (tenant_id, config_key),
    CONSTRAINT fk_system_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_system_config_tenant_id ON system_config(tenant_id);
CREATE INDEX idx_system_config_key ON system_config(config_key);

-- Seed minimal defaults used by scheduler/traffic selector; guarded to avoid duplicates.
INSERT INTO system_config (id, tenant_id, config_key, config_value, description, category)
SELECT UUID(), '00000000-0000-0000-0000-000000000000', 'dispatch.selector.weight.distance', JSON_OBJECT('value', 0.4),
       'Distance weight for hybrid selector', 'dispatch'
WHERE NOT EXISTS (
    SELECT 1 FROM system_config
    WHERE tenant_id = '00000000-0000-0000-0000-000000000000'
      AND config_key = 'dispatch.selector.weight.distance'
);

INSERT INTO system_config (id, tenant_id, config_key, config_value, description, category)
SELECT UUID(), '00000000-0000-0000-0000-000000000000', 'dispatch.selector.weight.time', JSON_OBJECT('value', 0.4),
       'Time weight for hybrid selector', 'dispatch'
WHERE NOT EXISTS (
    SELECT 1 FROM system_config
    WHERE tenant_id = '00000000-0000-0000-0000-000000000000'
      AND config_key = 'dispatch.selector.weight.time'
);

INSERT INTO system_config (id, tenant_id, config_key, config_value, description, category)
SELECT UUID(), '00000000-0000-0000-0000-000000000000', 'dispatch.selector.weight.wip', JSON_OBJECT('value', 0.2),
       'WIP weight for hybrid selector', 'dispatch'
WHERE NOT EXISTS (
    SELECT 1 FROM system_config
    WHERE tenant_id = '00000000-0000-0000-0000-000000000000'
      AND config_key = 'dispatch.selector.weight.wip'
);

-- ----------------------------------------------------------------------
-- 3) Model library / versions / bindings
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS model_library (
    id                  CHAR(36) PRIMARY KEY,
    tenant_id           CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    model_type          VARCHAR(50) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    default_version_id  CHAR(36),
    metadata            JSON,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),

    CONSTRAINT uq_model_library_name UNIQUE (tenant_id, model_type, name),
    CONSTRAINT fk_model_library_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_model_library_tenant_id ON model_library(tenant_id);
CREATE INDEX idx_model_library_type ON model_library(model_type);

CREATE TABLE IF NOT EXISTS model_versions (
    id                  CHAR(36) PRIMARY KEY,
    tenant_id           CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    model_id            CHAR(36) NOT NULL,
    version             VARCHAR(50) NOT NULL,
    file_id             CHAR(36) NOT NULL,
    transform_config    JSON,
    is_default          BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(100),

    CONSTRAINT uq_model_versions UNIQUE (model_id, version),
    CONSTRAINT fk_model_versions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_model_versions_model FOREIGN KEY (model_id) REFERENCES model_library(id) ON DELETE CASCADE,
    CONSTRAINT fk_model_versions_file FOREIGN KEY (file_id) REFERENCES files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_model_versions_model_id ON model_versions(model_id);
CREATE INDEX idx_model_versions_tenant_id ON model_versions(tenant_id);

CREATE TABLE IF NOT EXISTS entity_model_binding (
    id                  CHAR(36) PRIMARY KEY,
    tenant_id           CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id            CHAR(36) NOT NULL,
    entity_id           VARCHAR(100) NOT NULL,
    model_version_id    CHAR(36) NOT NULL,
    custom_transform    JSON,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT uq_entity_model_binding UNIQUE (tenant_id, scene_id, entity_id),
    CONSTRAINT fk_entity_model_binding_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_entity_model_binding_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE,
    CONSTRAINT fk_entity_model_binding_version FOREIGN KEY (model_version_id) REFERENCES model_versions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_entity_model_binding_scene_id ON entity_model_binding(scene_id);
CREATE INDEX idx_entity_model_binding_entity_id ON entity_model_binding(entity_id);

-- Add FK for model_library.default_version_id after model_versions is available.
-- Safe as long as referenced row is inserted before setting this column.
ALTER TABLE model_library
    ADD CONSTRAINT fk_model_library_default_version
    FOREIGN KEY (default_version_id) REFERENCES model_versions(id);

-- ----------------------------------------------------------------------
-- 4) Vehicle configuration table
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vehicle_configs (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    vehicle_id      VARCHAR(100) NOT NULL,
    config          JSON NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT vehicle_configs_scene_vehicle UNIQUE (scene_id, vehicle_id),
    CONSTRAINT fk_vehicle_configs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_vehicle_configs_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_vehicle_configs_tenant_id ON vehicle_configs(tenant_id);
CREATE INDEX idx_vehicle_configs_scene_id ON vehicle_configs(scene_id);

-- ----------------------------------------------------------------------
-- 5) Workflow tables
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS process_flows (
    id                  CHAR(36) NOT NULL,
    tenant_id           CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    version             INT NOT NULL DEFAULT 1,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    flow_definition     JSON NOT NULL,
    is_template         BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),

    PRIMARY KEY (tenant_id, id, version),
    CONSTRAINT fk_process_flows_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_process_flows_name ON process_flows(tenant_id, name);
CREATE INDEX idx_process_flows_status ON process_flows(tenant_id, status);

CREATE TABLE IF NOT EXISTS process_flow_bindings (
    id                  CHAR(36) PRIMARY KEY,
    tenant_id           CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id            CHAR(36) NOT NULL,
    flow_id             CHAR(36) NOT NULL,
    flow_version        INT NOT NULL,
    entry_point_id      VARCHAR(100) NOT NULL,
    trigger_condition   JSON,
    priority            INT NOT NULL DEFAULT 5,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT uq_flow_bindings_entry UNIQUE (tenant_id, scene_id, entry_point_id),
    CONSTRAINT fk_flow_bindings_scene FOREIGN KEY (tenant_id, scene_id) REFERENCES scenes(tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_flow_bindings_flow FOREIGN KEY (tenant_id, flow_id, flow_version)
        REFERENCES process_flows(tenant_id, id, version) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_flow_bindings_scene ON process_flow_bindings(tenant_id, scene_id);
CREATE INDEX idx_flow_bindings_flow ON process_flow_bindings(tenant_id, flow_id, flow_version);

SET FOREIGN_KEY_CHECKS = 1;
