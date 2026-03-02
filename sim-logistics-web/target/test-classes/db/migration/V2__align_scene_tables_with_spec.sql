-- ============================================
-- H2-compatible V2 migration script for testing
-- This file simulates V2__align_scene_tables_with_spec.sql logic
-- using H2-compatible SQL syntax.
-- Version: V2.7 (test)
--
-- V2.7 修复说明:
-- - 采用"强前提 + 早失败"策略：脚本开头强制检查 V1 旧表 (scene, scene_draft) 存在性
-- - 任一旧表缺失时显式报错并终止迁移
-- - 映射表使用 TEMPORARY TABLE
--
-- 用途说明: 此脚本是 V1->V2 专用迁移脚本，不是全新库初始化脚本。
-- 全新环境应走基线 DDL（或更高版本迁移链），不应直接跑 V2。
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 0. 前置检查: 强制验证 V1 旧表存在性 (早失败策略)
-- ============================================

-- 创建临时检查表用于验证 V1 旧表存在性
-- 如果 scene 或 scene_draft 表不存在，以下 INSERT 会自然失败，阻止迁移继续
DROP TABLE IF EXISTS __v2_migration_check__;
CREATE TABLE __v2_migration_check__ (
    check_result VARCHAR(100)
);

-- 尝试引用 scene 和 scene_draft 表，任一不存在都会导致此语句失败
-- 这是最早的失败点，确保在执行任何实际迁移前发现问题
INSERT INTO __v2_migration_check__ (check_result)
SELECT CONCAT('scene_count:', COUNT(*), ',scene_draft_count:', (SELECT COUNT(*) FROM scene_draft))
FROM scene;

-- 验证表中有数据（确保表存在且可访问）
UPDATE __v2_migration_check__ SET check_result = 'V1_tables_exist';

DROP TABLE IF EXISTS __v2_migration_check__;

-- ============================================
-- 1. 数据迁移: 从旧表迁移到新表
-- ============================================

-- Step 1: Create temporary migration table
CREATE TEMPORARY TABLE temp_scene_migration_data (
    old_id VARCHAR(64),
    old_scene_id VARCHAR(64),
    new_id CHAR(36),
    name VARCHAR(255),
    description TEXT,
    version INT,
    definition VARCHAR(1000),
    metadata VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (old_scene_id)
);

-- Populate migration table
INSERT INTO temp_scene_migration_data (old_id, old_scene_id, new_id, name, description, version, definition, metadata, created_at, updated_at)
SELECT
    scene.id as old_id,
    scene.scene_id as old_scene_id,
    -- Preserve UUID format, generate new for others using H2 RANDOM_UUID()
    CASE
        WHEN LENGTH(scene.id) = 36 AND scene.id LIKE '%-%-%-%-%' THEN scene.id
        ELSE RANDOM_UUID()
    END as new_id,
    scene.name,
    scene.description,
    scene.version,
    -- Build simplified definition JSON string
    '{"entities":[],"paths":[],"processFlows":[]}' as definition,
    '{"entityCount":0}' as metadata,
    scene.created_at,
    scene.updated_at
FROM scene;

-- Step 2: Create new scenes table
DROP TABLE IF EXISTS scenes;
CREATE TABLE scenes (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    version         INT NOT NULL DEFAULT 1,
    schema_version  INT NOT NULL DEFAULT 1,
    definition      VARCHAR(1000) NOT NULL,
    metadata        VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- Migrate to scenes
INSERT INTO scenes (id, tenant_id, name, description, version, schema_version, definition, metadata, created_at, updated_at, created_by, updated_by)
SELECT
    new_id,
    '00000000-0000-0000-0000-000000000000',
    name,
    description,
    version,
    1,
    definition,
    metadata,
    created_at,
    updated_at,
    NULL,
    NULL
FROM temp_scene_migration_data;

-- Step 3: Create new scene_drafts table
DROP TABLE IF EXISTS scene_drafts;
CREATE TABLE scene_drafts (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Migrate drafts using temp mapping table
INSERT INTO scene_drafts (id, tenant_id, scene_id, content, updated_at, created_at)
SELECT
    CASE
        WHEN LENGTH(d.id) = 36 AND d.id LIKE '%-%-%-%-%' THEN d.id
        ELSE RANDOM_UUID()
    END,
    '00000000-0000-0000-0000-000000000000',
    temp.new_id,
    d.content,
    d.saved_at,
    d.saved_at
FROM scene_draft d
INNER JOIN temp_scene_migration_data temp ON temp.old_scene_id = d.scene_id;

-- Step 4: Backup old tables
DROP TABLE IF EXISTS scene_backup_v1;
CREATE TABLE scene_backup_v1 AS SELECT * FROM scene;

DROP TABLE IF EXISTS scene_draft_backup_v1;
CREATE TABLE scene_draft_backup_v1 AS SELECT * FROM scene_draft;

-- Step 5: Drop old tables
DROP TABLE IF EXISTS scene_draft;
DROP TABLE IF EXISTS scene;

SET FOREIGN_KEY_CHECKS = 1;
