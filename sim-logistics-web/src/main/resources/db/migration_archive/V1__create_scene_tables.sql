-- Scene management tables for logistics simulation
-- Author: shentw
-- Date: 2026-02-10

-- Scene table
CREATE TABLE IF NOT EXISTS scene (
    id VARCHAR(64) PRIMARY KEY,
    scene_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    entities TEXT,
    paths TEXT,
    process_flows TEXT,
    entity_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scene_id (scene_id),
    INDEX idx_name (name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Scene draft table
CREATE TABLE IF NOT EXISTS scene_draft (
    id VARCHAR(64) PRIMARY KEY,
    scene_id VARCHAR(64) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    version INT NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scene_id (scene_id),
    FOREIGN KEY (scene_id) REFERENCES scene(scene_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
