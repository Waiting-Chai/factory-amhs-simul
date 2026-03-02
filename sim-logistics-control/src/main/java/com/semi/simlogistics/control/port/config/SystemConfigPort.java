package com.semi.simlogistics.control.port.config;

import java.util.Optional;

/**
 * Port for reading system configuration values.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface SystemConfigPort {

    /**
     * Find config value by tenant and key.
     *
     * @param tenantId tenant ID
     * @param key config key
     * @return optional config value
     */
    Optional<String> findConfigValue(String tenantId, String key);
}
