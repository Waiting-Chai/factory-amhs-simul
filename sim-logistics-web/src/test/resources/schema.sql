-- Test schema aligned with scene spec
-- Author: shentw
-- Date: 2026-02-10

CREATE TABLE IF NOT EXISTS tenants (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    settings JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

MERGE INTO tenants KEY(id)
VALUES ('00000000-0000-0000-0000-000000000000', 'default', 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE IF NOT EXISTS scenes (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    schema_version INT NOT NULL DEFAULT 1,
    definition JSON NOT NULL,
    metadata JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT scenes_name_version UNIQUE (tenant_id, name, version),
    CONSTRAINT scenes_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_scenes_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE IF NOT EXISTS scene_drafts (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    scene_id CHAR(36) NOT NULL,
    content JSON NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_scene_drafts_tenant_scene UNIQUE (tenant_id, scene_id),
    CONSTRAINT fk_scene_drafts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_scene_drafts_scene FOREIGN KEY (scene_id) REFERENCES scenes(id) ON DELETE CASCADE
);
