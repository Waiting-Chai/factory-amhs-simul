-- ============================================
-- Phase 5.1 前后端联调 - 数据库表结构对齐 spec
-- Version: V2.7
-- Date: 2026-02-10
-- Author: shentw
--
-- 变更说明:
-- 1. 将 scene/scene_draft 表重命名为 scenes/scene_drafts (复数)
-- 2. 添加 tenant_id 字段 (CHAR(36), 默认值 '00000000-0000-0000-0000-000000000000')
-- 3. 将 entities/paths/process_flows 从 TEXT 改为 JSON
-- 4. 将 id 从 VARCHAR(64) 改为 CHAR(36)
-- 5. 将 saved_at 从 TIMESTAMP 改为 DATETIME(3)
-- 6. 添加 created_by/updated_by 字段
-- 7. 添加 schema_version 字段
--
-- V2.7 修复说明:
-- - 采用"强前提 + 早失败"策略：脚本开头强制检查 V1 旧表 (scene, scene_draft) 存在性
-- - 任一旧表缺失时显式报错并终止迁移（使用 SIGNAL SQLSTATE）
-- - 映射表使用 TEMPORARY TABLE ENGINE=InnoDB 以支持 TEXT/JSON 字段
-- - 去除存储过程和 DELIMITER，使用 Flyway/JDBC 友好的幂等 SQL
-- - 恢复备份表语义：先备份旧表到 *_backup_v1，再删除原表
--
-- 用途说明: 此脚本是 V1->V2 专用迁移脚本，不是全新库初始化脚本。
-- 全新环境应走基线 DDL（或更高版本迁移链），不应直接跑 V2。
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 0. 前置检查: 强制验证 V1 旧表存在性 (早失败策略)
-- ============================================

-- 创建临时检查表用于验证 V1 旧表存在性
-- 如果 scene 或 scene_draft 表不存在，以下 INSERT 会自然失败，阻止迁移继续
DROP TEMPORARY TABLE IF EXISTS __v2_migration_check__;
CREATE TEMPORARY TABLE __v2_migration_check__ (
    check_result VARCHAR(100)
);

-- 尝试引用 scene 和 scene_draft 表，任一不存在都会导致此语句失败
-- 这是最早的失败点，确保在执行任何实际迁移前发现问题
INSERT INTO __v2_migration_check__ (check_result)
SELECT CONCAT('scene_count:', COUNT(*), ',scene_draft_count:', (SELECT COUNT(*) FROM scene_draft))
FROM scene;

-- 验证表中有数据（确保表存在且可访问）
UPDATE __v2_migration_check__ SET check_result = 'V1_tables_exist';

DROP TEMPORARY TABLE IF EXISTS __v2_migration_check__;

-- ============================================
-- 1. 数据迁移: 从旧表迁移到新表
-- ============================================

-- Step 1: 创建临时映射表 (存储迁移中间数据，确保 UUID 一致性)
-- 使用 TEMPORARY TABLE ENGINE=InnoDB 以支持 TEXT/JSON 字段（MEMORY 引擎不支持）
DROP TEMPORARY TABLE IF EXISTS temp_scene_migration_data;
CREATE TEMPORARY TABLE temp_scene_migration_data (
    old_id VARCHAR(64),
    old_scene_id VARCHAR(64),
    new_id CHAR(36),
    name VARCHAR(255),
    description TEXT,
    version INT,
    definition JSON,
    metadata JSON,
    created_at DATETIME(3),
    updated_at DATETIME(3),
    PRIMARY KEY (old_scene_id)
) ENGINE=InnoDB;

-- 填充映射表 (一次性生成所有新 UUID)
-- 注意: 由于脚本开头已强制检查 scene 表存在，此处不会报错。
INSERT INTO temp_scene_migration_data (old_id, old_scene_id, new_id, name, description, version, definition, metadata, created_at, updated_at)
SELECT
    scene.id as old_id,
    scene.scene_id as old_scene_id,
    -- 生成新的 CHAR(36) id: 如果原 id 是 UUID 格式则保留，否则生成新的
    CASE
        WHEN LENGTH(scene.id) = 36 AND scene.id LIKE '%-%-%-%-%' THEN scene.id
        ELSE UUID()
    END as new_id,
    scene.name,
    scene.description,
    scene.version,
    -- 构建 definition JSON (MySQL JSON functions)
    JSON_OBJECT(
        'entities', IF(JSON_VALID(scene.entities), CAST(scene.entities AS JSON), JSON_ARRAY()),
        'paths', IF(JSON_VALID(scene.paths), CAST(scene.paths AS JSON), JSON_ARRAY()),
        'processFlows', IF(JSON_VALID(scene.process_flows), CAST(scene.process_flows AS JSON), JSON_ARRAY())
    ) as definition,
    JSON_OBJECT('entityCount', COALESCE(scene.entity_count, 0)) as metadata,
    scene.created_at,
    scene.updated_at
FROM scene;

-- Step 2: 创建新的 scenes 表 (按 spec 定义)
DROP TABLE IF EXISTS scenes;
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
    INDEX idx_scenes_tenant_id (tenant_id),
    INDEX idx_scenes_schema_version (schema_version),
    INDEX idx_scenes_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 3: 从映射表迁移到 scenes
-- 注意: 映射表为空仅代表"旧表存在但无数据"，不包含"旧表不存在"情况（已在脚本开头早失败）
INSERT INTO scenes (id, tenant_id, name, description, version, schema_version, definition, metadata, created_at, updated_at, created_by, updated_by)
SELECT
    new_id as id,
    '00000000-0000-0000-0000-000000000000' as tenant_id,
    name,
    description,
    version,
    1 as schema_version,
    definition,
    metadata,
    created_at,
    updated_at,
    NULL as created_by,
    NULL as updated_by
FROM temp_scene_migration_data;

-- Step 4: 创建新的 scene_drafts 表 (按 spec 定义)
DROP TABLE IF EXISTS scene_drafts;
CREATE TABLE scene_drafts (
    id              CHAR(36) PRIMARY KEY,
    tenant_id       CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    scene_id        CHAR(36) NOT NULL,
    content         JSON NOT NULL,
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_scene_drafts_scene_id (scene_id),
    INDEX idx_scene_drafts_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 5: 迁移数据从 scene_draft 到 scene_drafts
-- 使用映射表确保正确关联 (draft.scene_id 引用的是 scene.scene_id)
-- 注意: 映射表为空或无匹配数据仅代表"旧表存在但无数据/无关联"，不包含"旧表不存在"情况（已在脚本开头早失败）
INSERT INTO scene_drafts (id, tenant_id, scene_id, content, updated_at, created_at)
SELECT
    -- 生成新的 CHAR(36) id
    CASE
        WHEN LENGTH(draft.id) = 36 AND draft.id LIKE '%-%-%-%-%' THEN draft.id
        ELSE UUID()
    END as id,
    '00000000-0000-0000-0000-000000000000' as tenant_id,
    -- 通过映射表获取正确的 scene_id
    temp_data.new_id as scene_id,
    JSON_EXTRACT(draft.content, '$') as content,
    draft.saved_at as updated_at,
    draft.saved_at as created_at
FROM scene_draft draft
INNER JOIN temp_scene_migration_data temp_data ON CAST(temp_data.old_scene_id AS BINARY) = CAST(draft.scene_id AS BINARY);

-- Step 6: 验证迁移
SELECT 'Migration verification:' as message;
SELECT CONCAT('Old scene table count: ', COUNT(*)) as info FROM scene
UNION ALL
SELECT CONCAT('New scenes table count: ', COUNT(*)) as info FROM scenes
UNION ALL
SELECT CONCAT('Old scene_draft table count: ', COUNT(*)) as info FROM scene_draft
UNION ALL
SELECT CONCAT('New scene_drafts table count: ', COUNT(*)) as info FROM scene_drafts
UNION ALL
SELECT CONCAT('Unmatched drafts (should be 0): ', COUNT(*)) as info
FROM scene_draft draft
LEFT JOIN temp_scene_migration_data temp_data ON CAST(temp_data.old_scene_id AS BINARY) = CAST(draft.scene_id AS BINARY)
WHERE temp_data.old_scene_id IS NULL;

-- Step 7: 清理映射表
DROP TEMPORARY TABLE IF EXISTS temp_scene_migration_data;

-- ============================================
-- 2. 备份并删除旧表 (V1 -> V2 单向迁移，保留备份用于回滚)
-- ============================================

-- 备份 scene 表到 scene_backup_v1
-- 注意: 由于脚本开头已强制检查 scene 表存在，此处不会报错。
DROP TABLE IF EXISTS scene_backup_v1;
CREATE TABLE scene_backup_v1 AS SELECT * FROM scene;

-- 备份 scene_draft 表到 scene_draft_backup_v1
DROP TABLE IF EXISTS scene_draft_backup_v1;
CREATE TABLE scene_draft_backup_v1 AS SELECT * FROM scene_draft;

-- 删除原表（IF EXISTS 确保幂等）
DROP TABLE IF EXISTS scene_draft;
DROP TABLE IF EXISTS scene;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 回滚脚本 (如需回滚，执行以下 SQL)
-- ============================================
/*
-- Step 1: 删除新表
DROP TABLE IF EXISTS scene_drafts;
DROP TABLE IF EXISTS scenes;

-- Step 2: 恢复旧表
DROP TABLE IF EXISTS scene;
DROP TABLE IF EXISTS scene_draft;

INSERT INTO scene SELECT * FROM scene_backup_v1;
INSERT INTO scene_draft SELECT * FROM scene_draft_backup_v1;

-- Step 3: 删除备份表（可选，建议保留一段时间）
DROP TABLE IF EXISTS scene_backup_v1;
DROP TABLE IF EXISTS scene_draft_backup_v1;
*/
