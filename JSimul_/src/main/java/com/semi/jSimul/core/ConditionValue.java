package com.semi.jSimul.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;

/**
 * Value wrapper for Condition events providing event->value mapping.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class ConditionValue {

    private final Map<Event, Object> values = new LinkedHashMap<>();

    void add(Event e) {
        values.put(e, e.value());
    }

    public Map<Event, Object> toMap() {
        return values;
    }

    public boolean contains(Event e) {
        return values.containsKey(e);
    }

    public Object get(Event e) {
        if (!values.containsKey(e)) {
            throw new NoSuchElementException("Event not present in ConditionValue: " + e);
        }
        return values.get(e);
    }

    /**
     * @return iterable over events in insertion order.
     */
    public Iterable<Event> events() {
        return values.keySet();
    }

    /**
     * @return iterable over values in event insertion order.
     */
    public Iterable<Object> values() {
        return values.values();
    }

    /**
     * @return iterable over event/value pairs in insertion order.
     */
    public Iterable<Map.Entry<Event, Object>> items() {
        return values.entrySet();
    }

    /**
     * SimPy parity: equality compares underlying event->value mapping.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return switch (obj) {
            case ConditionValue other -> Objects.equals(this.values, other.values);
            case Map<?, ?> map -> values.equals(map);
            case null, default -> false;
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "ConditionValue" + values;
    }

}
