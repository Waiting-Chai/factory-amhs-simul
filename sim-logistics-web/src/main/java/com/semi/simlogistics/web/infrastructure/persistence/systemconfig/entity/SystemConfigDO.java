package com.semi.simlogistics.web.infrastructure.persistence.systemconfig.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Data object for system_config table.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@TableName("system_config")
public class SystemConfigDO {

    @TableField("tenant_id")
    private String tenantId;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
