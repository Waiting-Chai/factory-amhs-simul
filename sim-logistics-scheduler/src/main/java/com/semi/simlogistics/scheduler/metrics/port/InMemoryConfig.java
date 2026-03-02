package com.semi.simlogistics.scheduler.metrics.port;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of ConfigPort for testing.
 * <p>
 * This implementation stores all configuration in memory
 * and uses default values for any keys not explicitly set.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class InMemoryConfig implements ConfigPort {

    private final Map<String, Double> doubleValues = new ConcurrentHashMap<>();
    private final Map<String, Integer> intValues = new ConcurrentHashMap<>();
    private final Map<String, Boolean> booleanValues = new ConcurrentHashMap<>();
    private final Map<String, String> stringValues = new ConcurrentHashMap<>();

    public InMemoryConfig() {
        // Initialize with default values
        setDefaultValues();
    }

    private void setDefaultValues() {
        doubleValues.put(Keys.METRICS_AGGREGATION_INTERVAL, Defaults.AGGREGATION_INTERVAL);
        booleanValues.put(Keys.METRICS_SAMPLING_ENABLED, Defaults.SAMPLING_ENABLED);
        booleanValues.put(Keys.METRICS_ENABLE_EVENT_LOGGING, Defaults.EVENT_LOGGING_ENABLED);
        intValues.put(Keys.METRICS_RETENTION_DAYS, Defaults.RETENTION_DAYS);
        doubleValues.put(Keys.THROUGHPUT_TARGET_TASKS_PER_HOUR, Defaults.TARGET_TASKS_PER_HOUR);
        doubleValues.put(Keys.THROUGHPUT_TARGET_MATERIAL_PER_HOUR, Defaults.TARGET_MATERIAL_PER_HOUR);
        doubleValues.put(Keys.UTILIZATION_WARNING_THRESHOLD, Defaults.UTILIZATION_WARNING);
        doubleValues.put(Keys.UTILIZATION_CRITICAL_THRESHOLD, Defaults.UTILIZATION_CRITICAL);
        intValues.put(Keys.WIP_WARNING_THRESHOLD, Defaults.WIP_WARNING);
        intValues.put(Keys.EVALUATION_MAX_SIMULATIONS, Defaults.MAX_SIMULATIONS);
        doubleValues.put(Keys.EVALUATION_CONFIDENCE_LEVEL, Defaults.CONFIDENCE_LEVEL);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return doubleValues.getOrDefault(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return intValues.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return booleanValues.getOrDefault(key, defaultValue);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return stringValues.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean hasKey(String key) {
        return doubleValues.containsKey(key)
                || intValues.containsKey(key)
                || booleanValues.containsKey(key)
                || stringValues.containsKey(key);
    }

    /**
     * Set a double value.
     */
    public InMemoryConfig setDouble(String key, double value) {
        doubleValues.put(key, value);
        return this;
    }

    /**
     * Set an integer value.
     */
    public InMemoryConfig setInt(String key, int value) {
        intValues.put(key, value);
        return this;
    }

    /**
     * Set a boolean value.
     */
    public InMemoryConfig setBoolean(String key, boolean value) {
        booleanValues.put(key, value);
        return this;
    }

    /**
     * Set a string value.
     */
    public InMemoryConfig setString(String key, String value) {
        stringValues.put(key, value);
        return this;
    }

    /**
     * Clear all values and reset to defaults.
     */
    public void reset() {
        doubleValues.clear();
        intValues.clear();
        booleanValues.clear();
        stringValues.clear();
        setDefaultValues();
    }
}
