package com.semi.simlogistics.scheduler.metrics.port;

/**
 * Port interface for system configuration operations.
 * <p>
 * This port defines the contract for reading configuration values
 * from system_config table or fallback to default values.
 * <p>
 * All configuration values are read-only during runtime.
 * Changes to configuration require a restart or reload operation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public interface ConfigPort {

    /**
     * Get a configuration value as a double.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key not found
     * @return the configuration value
     */
    double getDouble(String key, double defaultValue);

    /**
     * Get a configuration value as an integer.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key not found
     * @return the configuration value
     */
    int getInt(String key, int defaultValue);

    /**
     * Get a configuration value as a boolean.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key not found
     * @return the configuration value
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Get a configuration value as a string.
     *
     * @param key          the configuration key
     * @param defaultValue the default value if key not found
     * @return the configuration value
     */
    String getString(String key, String defaultValue);

    /**
     * Check if a configuration key exists.
     *
     * @param key the configuration key
     * @return true if the key exists, false otherwise
     */
    boolean hasKey(String key);

    /**
     * Common configuration keys for metrics module.
     */
    interface Keys {
        String METRICS_AGGREGATION_INTERVAL = "metrics.aggregation.interval"; // seconds (simulated time)
        String METRICS_SAMPLING_ENABLED = "metrics.sampling.enabled";
        String METRICS_ENABLE_EVENT_LOGGING = "metrics.enable.event.logging";
        String METRICS_RETENTION_DAYS = "metrics.retention.days";
        String THROUGHPUT_TARGET_TASKS_PER_HOUR = "throughput.target.tasks_per_hour";
        String THROUGHPUT_TARGET_MATERIAL_PER_HOUR = "throughput.target.material_per_hour";
        String UTILIZATION_WARNING_THRESHOLD = "utilization.warning.threshold";
        String UTILIZATION_CRITICAL_THRESHOLD = "utilization.critical.threshold";
        String WIP_WARNING_THRESHOLD = "wip.warning.threshold";
        String EVALUATION_MAX_SIMULATIONS = "evaluation.max.simulations";
        String EVALUATION_CONFIDENCE_LEVEL = "evaluation.confidence.level";
    }

    /**
     * Default configuration values.
     */
    interface Defaults {
        double AGGREGATION_INTERVAL = 60.0; // 60 seconds
        boolean SAMPLING_ENABLED = true;
        boolean EVENT_LOGGING_ENABLED = true;
        int RETENTION_DAYS = 30;
        double TARGET_TASKS_PER_HOUR = 100.0;
        double TARGET_MATERIAL_PER_HOUR = 500.0;
        double UTILIZATION_WARNING = 0.8;
        double UTILIZATION_CRITICAL = 0.95;
        int WIP_WARNING = 100;
        int MAX_SIMULATIONS = 10;
        double CONFIDENCE_LEVEL = 0.95;
    }
}
