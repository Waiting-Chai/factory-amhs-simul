package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.control.port.config.SystemConfigPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory provider for traffic control configuration.
 * <p>
 * This is a transitional implementation used for tests and local runtime overrides.
 * It supports DB-priority read through {@link SystemConfigPort} and falls back
 * to in-memory defaults for local testing.
 * <p>
 * Configuration can be set programmatically via {@link #setConfig(String, String)}.
 * <p>
 * Configuration keys (from REQ-TC-000):
 * - traffic.replan.timeoutSeconds (default: 60)
 * - traffic.replan.maxAttempts (default: 3)
 * - traffic.priority.aging.step (default: 30)
 * - traffic.priority.aging.boost (default: 1)
 * - traffic.priority.aging.max (default: 5)
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-09
 */
public class SystemConfigProvider {

    private static final double DEFAULT_REPLAN_TIMEOUT_SECONDS = 60.0;
    private static final int DEFAULT_REPLAN_MAX_ATTEMPTS = 3;
    private static final double DEFAULT_AGING_STEP = 30.0;
    private static final int DEFAULT_AGING_BOOST = 1;
    private static final int DEFAULT_AGING_MAX = 5;
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    // Singleton instance
    private static volatile SystemConfigProvider instance;

    // Runtime configuration storage (supports programmatic override)
    private final Map<String, String> config;
    private volatile SystemConfigPort primaryPort;

    /**
     * Get the singleton instance of SystemConfigProvider.
     *
     * @return the provider instance
     */
    public static SystemConfigProvider getInstance() {
        if (instance == null) {
            synchronized (SystemConfigProvider.class) {
                if (instance == null) {
                    instance = new SystemConfigProvider();
                }
            }
        }
        return instance;
    }

    /**
     * Private constructor for singleton.
     * <p>
     * Initializes with default configuration values.
     */
    private SystemConfigProvider() {
        this.config = new ConcurrentHashMap<>();
        // Initialize with default values
        loadDefaults();
    }

    /**
     * Load default configuration values.
     * <p>
     * This is called during initialization. In production, this would be
     * replaced with loading from external configuration sources.
     */
    private void loadDefaults() {
        config.put("traffic.replan.timeoutSeconds", String.valueOf(DEFAULT_REPLAN_TIMEOUT_SECONDS));
        config.put("traffic.replan.maxAttempts", String.valueOf(DEFAULT_REPLAN_MAX_ATTEMPTS));
        config.put("traffic.priority.aging.step", String.valueOf(DEFAULT_AGING_STEP));
        config.put("traffic.priority.aging.boost", String.valueOf(DEFAULT_AGING_BOOST));
        config.put("traffic.priority.aging.max", String.valueOf(DEFAULT_AGING_MAX));
    }

    /**
     * Set a configuration value programmatically.
     * <p>
     * This updates the in-memory map only. It does NOT persist to database
     * or read from system_config table.
     *
     * @param key configuration key (e.g., "traffic.replan.timeoutSeconds")
     * @param value configuration value as string
     */
    public void setConfig(String key, String value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }
        config.put(key, value);
    }

    /**
     * Set primary configuration repository (DB-first source).
     *
     * @param port repository implementation
     */
    public void setPrimaryPort(SystemConfigPort port) {
        this.primaryPort = port;
    }

    /**
     * Clear primary repository and keep in-memory fallback only.
     */
    public void clearPrimaryPort() {
        this.primaryPort = null;
    }

    /**
     * Get a configuration value as string.
     * <p>
     * Reads from in-memory map only (no database access).
     *
     * @param key configuration key
     * @return configuration value, or null if not found
     */
    public String getConfig(String key) {
        if (primaryPort != null) {
            Optional<String> dbValue = primaryPort.findConfigValue(DEFAULT_TENANT_ID, key);
            if (dbValue.isPresent() && !dbValue.get().isEmpty()) {
                return dbValue.get();
            }
        }
        return config.get(key);
    }

    /**
     * Get a configuration value as string with default.
     *
     * @param key configuration key
     * @param defaultValue default value if key not found
     * @return configuration value, or defaultValue if not found
     */
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get replan timeout in seconds.
     * <p>
     * Configuration key: traffic.replan.timeoutSeconds
     * <p>
     * Transitional behavior: reads from in-memory map only, does not query
     * system_config table yet. Future versions will read from real config source.
     * <p>
     * Returns the configured value, or default (60) if:
     * - Configuration key is not set
     * - Value is invalid (<= 0)
     *
     * @return timeout in seconds (default 60)
     */
    public double getReplanTimeoutSeconds() {
        String value = getConfig("traffic.replan.timeoutSeconds");
        if (value != null) {
            try {
                double parsed = Double.parseDouble(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Invalid format, use default
            }
        }
        return DEFAULT_REPLAN_TIMEOUT_SECONDS;
    }

    /**
     * Get maximum replan attempts.
     * <p>
     * Configuration key: traffic.replan.maxAttempts
     * <p>
     * Transitional behavior: reads from in-memory map only, does not query
     * system_config table yet. Future versions will read from real config source.
     * <p>
     * Returns the configured value, or default (3) if:
     * - Configuration key is not set
     * - Value is invalid (<= 0)
     *
     * @return max attempts (default 3)
     */
    public int getReplanMaxAttempts() {
        String value = getConfig("traffic.replan.maxAttempts");
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Invalid format, use default
            }
        }
        return DEFAULT_REPLAN_MAX_ATTEMPTS;
    }

    /**
     * Get priority aging step in seconds.
     * <p>
     * Configuration key: traffic.priority.aging.step
     *
     * @return aging step in seconds (default 30)
     */
    public double getPriorityAgingStep() {
        String value = getConfig("traffic.priority.aging.step");
        if (value != null) {
            try {
                double parsed = Double.parseDouble(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Invalid format, use default
            }
        }
        return DEFAULT_AGING_STEP;
    }

    /**
     * Get priority aging boost per step.
     * <p>
     * Configuration key: traffic.priority.aging.boost
     *
     * @return aging boost (default 1)
     */
    public int getPriorityAgingBoost() {
        String value = getConfig("traffic.priority.aging.boost");
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Invalid format, use default
            }
        }
        return DEFAULT_AGING_BOOST;
    }

    /**
     * Get maximum priority aging boost.
     * <p>
     * Configuration key: traffic.priority.aging.max
     *
     * @return max boost (default 5)
     */
    public int getPriorityAgingMax() {
        String value = getConfig("traffic.priority.aging.max");
        if (value != null) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException e) {
                // Invalid format, use default
            }
        }
        return DEFAULT_AGING_MAX;
    }

    /**
     * Reset all configuration to default values.
     * <p>
     * This is useful for testing to ensure clean state between tests.
     */
    public void resetToDefaults() {
        config.clear();
        loadDefaults();
    }

    /**
     * Check if a configuration value exists and is valid (> 0 for numeric values).
     *
     * @param value the value to check
     * @return true if value is valid, false otherwise
     */
    public static boolean isValidConfigValue(double value) {
        return value > 0;
    }

    /**
     * Check if a configuration value exists and is valid (> 0 for numeric values).
     *
     * @param value the value to check
     * @return true if value is valid, false otherwise
     */
    public static boolean isValidConfigValue(int value) {
        return value > 0;
    }
}
