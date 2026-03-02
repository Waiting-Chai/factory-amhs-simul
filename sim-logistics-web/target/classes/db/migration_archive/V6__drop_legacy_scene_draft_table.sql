-- ============================================
-- Drop legacy singular draft table
-- Version: V6
-- Date: 2026-02-11
-- Author: shentw
--
-- Purpose:
-- Remove obsolete V1 table `scene_draft`.
-- Current canonical table is `scene_drafts`.
-- ============================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS scene_draft;

SET FOREIGN_KEY_CHECKS = 1;
