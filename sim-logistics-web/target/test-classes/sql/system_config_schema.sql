CREATE TABLE IF NOT EXISTS system_config (
    tenant_id VARCHAR(36) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(255) NOT NULL
);
