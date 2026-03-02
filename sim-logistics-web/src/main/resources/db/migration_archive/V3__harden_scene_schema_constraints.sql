-- ============================================
-- Phase 5.1 Scene Schema Hardening (Spec Alignment)
-- Version: V3
-- Date: 2026-02-10
-- Author: shentw
--
-- V3.1 修复说明:
-- - 修复 DROP COLUMN IF EXISTS 语法兼容性问题（改用 PREPARE/EXECUTE 动态 SQL）
-- - 为所有约束添加幂等性检查（基于 INFORMATION_SCHEMA）
-- - 保持与原有业务语义完全一致
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Ensure tenants table exists for scene multi-tenant foreign keys
CREATE TABLE IF NOT EXISTS tenants (
    id              CHAR(36) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    settings        JSON,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO tenants (id, name, status)
VALUES ('00000000-0000-0000-0000-000000000000', 'default', 'ACTIVE');

-- ============================================
-- 1. 动态删除 scene_drafts.version 列（如果存在）
-- ============================================

-- 检查列是否存在，如果存在则删除
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'scene_drafts'
      AND COLUMN_NAME = 'version'
);

SET @sql = IF(@column_exists > 0,
    'ALTER TABLE scene_drafts DROP COLUMN version',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 2. 添加 scenes 约束（幂等）
-- ============================================

-- 2.1 添加 scenes_tenant_id 唯一约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'scenes'
      AND CONSTRAINT_NAME = 'scenes_tenant_id'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scenes ADD CONSTRAINT scenes_tenant_id UNIQUE (tenant_id, id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2.2 添加 fk_scenes_tenant 外键约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'scenes'
      AND CONSTRAINT_NAME = 'fk_scenes_tenant'
      AND REFERENCED_TABLE_NAME = 'tenants'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scenes ADD CONSTRAINT fk_scenes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 3. 添加 scene_drafts 约束（幂等）
-- ============================================

-- 3.1 添加 uq_scene_drafts_tenant_scene 唯一约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'scene_drafts'
      AND CONSTRAINT_NAME = 'uq_scene_drafts_tenant_scene'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scene_drafts ADD CONSTRAINT uq_scene_drafts_tenant_scene UNIQUE (tenant_id, scene_id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.2 添加 fk_scene_drafts_tenant 外键约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'scene_drafts'
      AND CONSTRAINT_NAME = 'fk_scene_drafts_tenant'
      AND REFERENCED_TABLE_NAME = 'tenants'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scene_drafts ADD CONSTRAINT fk_scene_drafts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3.3 添加 fk_scene_drafts_scene 外键约束
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'scene_drafts'
      AND CONSTRAINT_NAME = 'fk_scene_drafts_scene'
      AND REFERENCED_TABLE_NAME = 'scenes'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE scene_drafts ADD CONSTRAINT fk_scene_drafts_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
