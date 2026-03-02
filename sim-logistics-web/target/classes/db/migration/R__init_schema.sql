-- ============================================
-- Repeatable Schema Initialization Script
-- Author: shentw
-- Version: 1.0
-- Date: 2026-02-11
--
-- Purpose:
-- - Idempotent schema initialization for development environment
-- - Creates all required tables if they do not exist
-- - Safe to run multiple times (no data migration, only DDL)
-- - No version chain - always maintains current schema state
--
-- Notes:
-- - Uses CREATE TABLE IF NOT EXISTS for all tables
-- - Uses INFORMATION_SCHEMA checks for indexes and constraints
-- - All DDL is idempotent and repeatable
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 0) Application-managed schema versioning table (custom business table)
-- Note: Flyway metadata history table is flyway_schema_history (managed by Flyway)
-- ============================================
CREATE TABLE IF NOT EXISTS schema_migrations (
    installed_rank   INT NOT NULL,
    version          VARCHAR(50),
    description      VARCHAR(200) NOT NULL,
    type             VARCHAR(20) NOT NULL,
    script           VARCHAR(255) NOT NULL,
    checksum         INT,
    installed_by     VARCHAR(100) NOT NULL,
    installed_on     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    execution_time   INT NOT NULL,
    success          BOOLEAN NOT NULL,

    PRIMARY KEY (installed_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Align legacy column length to avoid oversized index key in utf8mb4 (MySQL 1071)
SET @script_len = (SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schema_migrations' AND COLUMN_NAME = 'script');
SET @sql = IF(@script_len IS NOT NULL AND @script_len > 255,
    'ALTER TABLE schema_migrations MODIFY COLUMN script VARCHAR(255) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add unique index on version + script for idempotency
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schema_migrations' AND INDEX_NAME = 'schema_migrations_s_idx');
SET @sql = IF(@index_exists = 0,
    'CREATE UNIQUE INDEX schema_migrations_s_idx ON schema_migrations(version, script)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'schema_migrations' AND INDEX_NAME = 'schema_migrations_installed_rank_idx');
SET @sql = IF(@index_exists = 0,
    'CREATE INDEX schema_migrations_installed_rank_idx ON schema_migrations(installed_rank)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 1) Tenants table
-- ============================================
CREATE TABLE IF NOT EXISTS tenants (
    id              CHAR(36) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    settings        JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default tenant (guarded for idempotency)
INSERT IGNORE INTO tenants (id, name, status)
VALUES ('00000000-0000-0000-0000-000000000000', 'default', 'ACTIVE');

-- ============================================
-- 2) Scenes table
-- ============================================
CREATE TABLE IF NOT EXISTS scenes (
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

    CONSTRAINT scenes_name_version UNIQUE (tenant_id, name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add scenes indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scenes' AND INDEX_NAME = 'idx_scenes_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_scenes_tenant_id ON scenes(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scenes' AND INDEX_NAME = 'idx_scenes_schema_version');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_scenes_schema_version ON scenes(schema_version)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scenes' AND INDEX_NAME = 'idx_scenes_created_at');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_scenes_created_at ON scenes(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add scenes foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scenes' AND CONSTRAINT_NAME = 'fk_scenes_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scenes ADD CONSTRAINT fk_scenes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 3) Scene drafts table
-- ============================================
CREATE TABLE IF NOT EXISTS scene_drafts (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    content         JSON NOT NULL,
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    CONSTRAINT uq_scene_drafts_tenant_scene UNIQUE (tenant_id, scene_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add scene_drafts indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scene_drafts' AND INDEX_NAME = 'idx_scene_drafts_scene_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_scene_drafts_scene_id ON scene_drafts(scene_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scene_drafts' AND INDEX_NAME = 'idx_scene_drafts_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_scene_drafts_tenant_id ON scene_drafts(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add scene_drafts foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scene_drafts' AND CONSTRAINT_NAME = 'fk_scene_drafts_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scene_drafts ADD CONSTRAINT fk_scene_drafts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scene_drafts' AND CONSTRAINT_NAME = 'fk_scene_drafts_scene' AND REFERENCED_TABLE_NAME = 'scenes');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scene_drafts ADD CONSTRAINT fk_scene_drafts_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 4) Files table (MinIO object storage metadata)
-- ============================================
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
    created_by      VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add files indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'files' AND INDEX_NAME = 'idx_files_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_files_tenant_id ON files(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'files' AND INDEX_NAME = 'idx_files_entity');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_files_entity ON files(entity_type, entity_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'files' AND INDEX_NAME = 'idx_files_expires_at');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_files_expires_at ON files(expires_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add files foreign key constraint (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'files' AND CONSTRAINT_NAME = 'fk_files_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE files ADD CONSTRAINT fk_files_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 5) System config table
-- ============================================
CREATE TABLE IF NOT EXISTS system_config (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    config_key      VARCHAR(255) NOT NULL,
    config_value    JSON,
    description     VARCHAR(500),
    category        VARCHAR(100),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT uq_system_config UNIQUE (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add system_config indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_config' AND INDEX_NAME = 'idx_system_config_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_system_config_tenant_id ON system_config(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_config' AND INDEX_NAME = 'idx_system_config_key');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_system_config_key ON system_config(config_key)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add system_config foreign key constraint (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_config' AND CONSTRAINT_NAME = 'fk_system_config_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE system_config ADD CONSTRAINT fk_system_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Seed minimal defaults (guarded for idempotency)
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

-- ============================================
-- 6) Model library table
-- ============================================
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

    CONSTRAINT uq_model_library_name UNIQUE (tenant_id, model_type, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add model_library indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_library' AND INDEX_NAME = 'idx_model_library_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_model_library_tenant_id ON model_library(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_library' AND INDEX_NAME = 'idx_model_library_type');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_model_library_type ON model_library(model_type)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add model_library foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_library' AND CONSTRAINT_NAME = 'fk_model_library_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE model_library ADD CONSTRAINT fk_model_library_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 7) Model versions table
-- ============================================
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

    CONSTRAINT uq_model_versions UNIQUE (model_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add model_versions indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_versions' AND INDEX_NAME = 'idx_model_versions_model_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_model_versions_model_id ON model_versions(model_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_versions' AND INDEX_NAME = 'idx_model_versions_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_model_versions_tenant_id ON model_versions(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add model_versions foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_versions' AND CONSTRAINT_NAME = 'fk_model_versions_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE model_versions ADD CONSTRAINT fk_model_versions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_versions' AND CONSTRAINT_NAME = 'fk_model_versions_model' AND REFERENCED_TABLE_NAME = 'model_library');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE model_versions ADD CONSTRAINT fk_model_versions_model FOREIGN KEY (model_id) REFERENCES model_library(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_versions' AND CONSTRAINT_NAME = 'fk_model_versions_file' AND REFERENCED_TABLE_NAME = 'files');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE model_versions ADD CONSTRAINT fk_model_versions_file FOREIGN KEY (file_id) REFERENCES files(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add model_library.default_version_id foreign key (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'model_library' AND CONSTRAINT_NAME = 'fk_model_library_default_version' AND REFERENCED_TABLE_NAME = 'model_versions');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE model_library ADD CONSTRAINT fk_model_library_default_version FOREIGN KEY (default_version_id) REFERENCES model_versions(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 8) Entity model binding table
-- ============================================
CREATE TABLE IF NOT EXISTS entity_model_binding (
    id                  CHAR(36) PRIMARY KEY,
    tenant_id           CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id            CHAR(36) NOT NULL,
    entity_id           VARCHAR(100) NOT NULL,
    model_version_id    CHAR(36) NOT NULL,
    custom_transform    JSON,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT uq_entity_model_binding UNIQUE (tenant_id, scene_id, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add entity_model_binding indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_model_binding' AND INDEX_NAME = 'idx_entity_model_binding_scene_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_entity_model_binding_scene_id ON entity_model_binding(scene_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_model_binding' AND INDEX_NAME = 'idx_entity_model_binding_entity_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_entity_model_binding_entity_id ON entity_model_binding(entity_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add entity_model_binding foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_model_binding' AND CONSTRAINT_NAME = 'fk_entity_model_binding_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE entity_model_binding ADD CONSTRAINT fk_entity_model_binding_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_model_binding' AND CONSTRAINT_NAME = 'fk_entity_model_binding_scene' AND REFERENCED_TABLE_NAME = 'scenes');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE entity_model_binding ADD CONSTRAINT fk_entity_model_binding_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_model_binding' AND CONSTRAINT_NAME = 'fk_entity_model_binding_version' AND REFERENCED_TABLE_NAME = 'model_versions');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE entity_model_binding ADD CONSTRAINT fk_entity_model_binding_version FOREIGN KEY (model_version_id) REFERENCES model_versions(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 9) Vehicle configs table
-- ============================================
CREATE TABLE IF NOT EXISTS vehicle_configs (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    vehicle_id      VARCHAR(100) NOT NULL,
    config          JSON NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT vehicle_configs_scene_vehicle UNIQUE (scene_id, vehicle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add vehicle_configs indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle_configs' AND INDEX_NAME = 'idx_vehicle_configs_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_vehicle_configs_tenant_id ON vehicle_configs(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle_configs' AND INDEX_NAME = 'idx_vehicle_configs_scene_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_vehicle_configs_scene_id ON vehicle_configs(scene_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add vehicle_configs foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle_configs' AND CONSTRAINT_NAME = 'fk_vehicle_configs_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE vehicle_configs ADD CONSTRAINT fk_vehicle_configs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vehicle_configs' AND CONSTRAINT_NAME = 'fk_vehicle_configs_scene' AND REFERENCED_TABLE_NAME = 'scenes');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE vehicle_configs ADD CONSTRAINT fk_vehicle_configs_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 10) Process flows table
-- ============================================
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

    PRIMARY KEY (tenant_id, id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add process_flows indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flows' AND INDEX_NAME = 'idx_process_flows_name');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_process_flows_name ON process_flows(tenant_id, name)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flows' AND INDEX_NAME = 'idx_process_flows_status');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_process_flows_status ON process_flows(tenant_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add process_flows foreign key constraint (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flows' AND CONSTRAINT_NAME = 'fk_process_flows_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE process_flows ADD CONSTRAINT fk_process_flows_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 11) Process flow bindings table
-- ============================================
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

    CONSTRAINT uq_flow_bindings_entry UNIQUE (tenant_id, scene_id, entry_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add process_flow_bindings indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flow_bindings' AND INDEX_NAME = 'idx_flow_bindings_scene');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_flow_bindings_scene ON process_flow_bindings(tenant_id, scene_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flow_bindings' AND INDEX_NAME = 'idx_flow_bindings_flow');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_flow_bindings_flow ON process_flow_bindings(tenant_id, flow_id, flow_version)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add process_flow_bindings foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flow_bindings' AND CONSTRAINT_NAME = 'fk_flow_bindings_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_flow_bindings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_flow_bindings' AND CONSTRAINT_NAME = 'fk_flow_bindings_scene' AND REFERENCED_TABLE_NAME = 'scenes');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_flow_bindings_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Note: Composite foreign key (tenant_id, flow_id, flow_version) -> process_flows(tenant_id, id, version)
-- Check all three columns match the referenced composite key
SET @column_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu1
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'process_flow_bindings'
      AND CONSTRAINT_NAME = 'fk_flow_bindings_flow'
);
SET @sql = IF(@column_count = 0,
    'ALTER TABLE process_flow_bindings ADD CONSTRAINT fk_flow_bindings_flow FOREIGN KEY (tenant_id, flow_id, flow_version) REFERENCES process_flows(tenant_id, id, version) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 12) Simulations table
-- ============================================
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

    CONSTRAINT simulations_status_check CHECK (status IN ('PENDING', 'RUNNING', 'PAUSED', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add simulations indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'simulations' AND INDEX_NAME = 'idx_simulations_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_simulations_tenant_id ON simulations(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'simulations' AND INDEX_NAME = 'idx_simulations_scene_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_simulations_scene_id ON simulations(scene_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'simulations' AND INDEX_NAME = 'idx_simulations_status');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_simulations_status ON simulations(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'simulations' AND INDEX_NAME = 'idx_simulations_created_at');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_simulations_created_at ON simulations(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add simulations foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'simulations' AND CONSTRAINT_NAME = 'fk_simulations_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE simulations ADD CONSTRAINT fk_simulations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'simulations' AND CONSTRAINT_NAME = 'fk_simulations_scene' AND REFERENCED_TABLE_NAME = 'scenes');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE simulations ADD CONSTRAINT fk_simulations_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 13) Tasks table
-- ============================================
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

    CONSTRAINT tasks_status_check CHECK (status IN ('PENDING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add tasks indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND INDEX_NAME = 'idx_tasks_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_tenant_id ON tasks(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND INDEX_NAME = 'idx_tasks_simulation_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_simulation_id ON tasks(simulation_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND INDEX_NAME = 'idx_tasks_status');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_status ON tasks(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND INDEX_NAME = 'idx_tasks_priority');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_priority ON tasks(priority)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND INDEX_NAME = 'idx_tasks_assigned_vehicle');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_assigned_vehicle ON tasks(assigned_vehicle)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add tasks foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND CONSTRAINT_NAME = 'fk_tasks_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE tasks ADD CONSTRAINT fk_tasks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks' AND CONSTRAINT_NAME = 'fk_tasks_simulation' AND REFERENCED_TABLE_NAME = 'simulations');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE tasks ADD CONSTRAINT fk_tasks_simulation FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 14) Tasks archive table
-- ============================================
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

-- Add tasks_archive indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks_archive' AND INDEX_NAME = 'idx_tasks_archive_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_archive_tenant_id ON tasks_archive(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks_archive' AND INDEX_NAME = 'idx_tasks_archive_simulation_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_archive_simulation_id ON tasks_archive(simulation_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks_archive' AND INDEX_NAME = 'idx_tasks_archive_status');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_archive_status ON tasks_archive(status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks_archive' AND INDEX_NAME = 'idx_tasks_archive_priority');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_archive_priority ON tasks_archive(priority)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks_archive' AND INDEX_NAME = 'idx_tasks_archive_assigned_vehicle');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_archive_assigned_vehicle ON tasks_archive(assigned_vehicle)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tasks_archive' AND INDEX_NAME = 'idx_tasks_archive_created_at');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_archive_created_at ON tasks_archive(created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 15) KPI metrics table
-- ============================================
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
    created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add kpi_metrics indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kpi_metrics' AND INDEX_NAME = 'idx_kpi_metrics_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_kpi_metrics_tenant_id ON kpi_metrics(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kpi_metrics' AND INDEX_NAME = 'idx_kpi_metrics_simulation_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_kpi_metrics_simulation_id ON kpi_metrics(simulation_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kpi_metrics' AND INDEX_NAME = 'idx_kpi_metrics_recorded_at');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_kpi_metrics_recorded_at ON kpi_metrics(recorded_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add kpi_metrics foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kpi_metrics' AND CONSTRAINT_NAME = 'fk_kpi_metrics_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE kpi_metrics ADD CONSTRAINT fk_kpi_metrics_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kpi_metrics' AND CONSTRAINT_NAME = 'fk_kpi_metrics_simulation' AND REFERENCED_TABLE_NAME = 'simulations');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE kpi_metrics ADD CONSTRAINT fk_kpi_metrics_simulation FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 16) Entity state history table
-- ============================================
CREATE TABLE IF NOT EXISTS entity_state_history (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    simulation_id   CHAR(36) NOT NULL,
    entity_id       VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    recorded_at     DECIMAL(10, 2) NOT NULL,
    state           JSON NOT NULL,
    changes         JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add entity_state_history indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_state_history' AND INDEX_NAME = 'idx_entity_history_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_entity_history_tenant_id ON entity_state_history(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_state_history' AND INDEX_NAME = 'idx_entity_history_simulation_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_entity_history_simulation_id ON entity_state_history(simulation_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_state_history' AND INDEX_NAME = 'idx_entity_history_entity_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_entity_history_entity_id ON entity_state_history(entity_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_state_history' AND INDEX_NAME = 'idx_entity_history_recorded_at');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_entity_history_recorded_at ON entity_state_history(recorded_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add entity_state_history foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_state_history' AND CONSTRAINT_NAME = 'fk_entity_state_history_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE entity_state_history ADD CONSTRAINT fk_entity_state_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'entity_state_history' AND CONSTRAINT_NAME = 'fk_entity_state_history_sim' AND REFERENCED_TABLE_NAME = 'simulations');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE entity_state_history ADD CONSTRAINT fk_entity_state_history_sim FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 17) Event log table
-- ============================================
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

    CONSTRAINT event_log_severity_check CHECK (severity IN ('DEBUG', 'INFO', 'WARNING', 'ERROR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add event_log indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND INDEX_NAME = 'idx_event_log_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_event_log_tenant_id ON event_log(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND INDEX_NAME = 'idx_event_log_simulation_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_event_log_simulation_id ON event_log(simulation_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND INDEX_NAME = 'idx_event_log_event_type');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_event_log_event_type ON event_log(event_type)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND INDEX_NAME = 'idx_event_log_severity');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_event_log_severity ON event_log(severity)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND INDEX_NAME = 'idx_event_log_event_time');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_event_log_event_time ON event_log(event_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add event_log foreign key constraints (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND CONSTRAINT_NAME = 'fk_event_log_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE event_log ADD CONSTRAINT fk_event_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND CONSTRAINT_NAME = 'fk_event_log_simulation' AND REFERENCED_TABLE_NAME = 'simulations');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE event_log ADD CONSTRAINT fk_event_log_simulation FOREIGN KEY (simulation_id) REFERENCES simulations(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 18) Audit log table
-- ============================================
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
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add audit_log indexes (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND INDEX_NAME = 'idx_audit_log_tenant_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_audit_log_tenant_id ON audit_log(tenant_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND INDEX_NAME = 'idx_audit_log_user_id');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_audit_log_user_id ON audit_log(user_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND INDEX_NAME = 'idx_audit_log_entity');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND INDEX_NAME = 'idx_audit_log_timestamp');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add audit_log foreign key constraint (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'audit_log' AND CONSTRAINT_NAME = 'fk_audit_log_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE audit_log ADD CONSTRAINT fk_audit_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 19) Log level config table
-- ============================================
CREATE TABLE IF NOT EXISTS log_level_config (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    `module`        VARCHAR(50) NOT NULL,
    level           VARCHAR(20) NOT NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    CONSTRAINT log_level_config_tenant_module UNIQUE (tenant_id, `module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add log_level_config index (idempotent)
SET @index_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'log_level_config' AND INDEX_NAME = 'idx_log_level_tenant_module');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_log_level_tenant_module ON log_level_config(tenant_id, `module`)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Add log_level_config foreign key constraint (idempotent)
SET @constraint_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'log_level_config' AND CONSTRAINT_NAME = 'fk_log_level_config_tenant' AND REFERENCED_TABLE_NAME = 'tenants');
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE log_level_config ADD CONSTRAINT fk_log_level_config_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 20) Scene model versions table
-- ============================================
CREATE TABLE IF NOT EXISTS scene_model_versions (
    version            INT PRIMARY KEY AUTO_INCREMENT,
    schema_definition  JSON NOT NULL,
    migration_script   TEXT,
    rollback_script    TEXT,
    created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed initial schema version (guarded for idempotency)
INSERT INTO scene_model_versions (version, schema_definition)
SELECT 1,
       JSON_OBJECT(
           '$schema', 'http://json-schema.org/draft-07/schema#',
           'type', 'object',
           'required', JSON_ARRAY('entities', 'paths')
       )
WHERE NOT EXISTS (SELECT 1 FROM scene_model_versions WHERE version = 1);

SET FOREIGN_KEY_CHECKS = 1;
