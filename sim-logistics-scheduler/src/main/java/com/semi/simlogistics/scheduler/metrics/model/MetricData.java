package com.semi.simlogistics.scheduler.metrics.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metric data container for key-value metric attributes.
 * <p>
 * Provides a flexible data structure for storing various metric values
 * such as task counts, utilization rates, energy consumption, etc.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class MetricData {

    private final Map<String, Object> attributes;

    public MetricData() {
        this.attributes = new HashMap<>();
    }

    public MetricData(Map<String, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
    }

    /**
     * Put an integer value.
     */
    public MetricData putInt(String key, int value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Put a double value.
     */
    public MetricData putDouble(String key, double value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Put a long value.
     */
    public MetricData putLong(String key, long value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Put a boolean value.
     */
    public MetricData putBoolean(String key, boolean value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Put a string value.
     */
    public MetricData putString(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Get an integer value.
     */
    public int getInt(String key, int defaultValue) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a double value.
     */
    public double getDouble(String key, double defaultValue) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Get a long value.
     */
    public long getLong(String key, long defaultValue) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Get a boolean value.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = attributes.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Get a string value.
     */
    public String getString(String key, String defaultValue) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Check if a key exists.
     */
    public boolean has(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Get all attributes.
     */
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    /**
     * Get the number of attributes.
     */
    public int size() {
        return attributes.size();
    }

    /**
     * Check if data is empty.
     */
    public boolean isEmpty() {
        return attributes.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricData that = (MetricData) o;
        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    @Override
    public String toString() {
        return "MetricData{attributes=" + attributes + "}";
    }
}
