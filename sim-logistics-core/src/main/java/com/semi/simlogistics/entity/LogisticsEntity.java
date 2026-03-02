package com.semi.simlogistics.entity;

import com.semi.simlogistics.capability.Capability;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all logistics entities in the simulation.
 * <p>
 * This class provides common properties and capabilities for all entities.
 * Capabilities are composited using a map for flexibility.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public abstract class LogisticsEntity {

    private final String id;
    private final String name;
    private final EntityType type;
    private Position position;
    private final Map<Class<? extends Capability>, Capability> capabilities;

    protected LogisticsEntity(String id, String name, EntityType type, Position position) {
        this.id = Objects.requireNonNull(id, "Entity id cannot be null");
        this.name = Objects.requireNonNull(name, "Entity name cannot be null");
        this.type = Objects.requireNonNull(type, "Entity type cannot be null");
        this.position = Objects.requireNonNull(position, "Entity position cannot be null");
        this.capabilities = new HashMap<>();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public EntityType type() {
        return type;
    }

    public Position position() {
        return position;
    }

    /**
     * Update entity position.
     *
     * @param position new position
     */
    public void setPosition(Position position) {
        this.position = Objects.requireNonNull(position, "Position cannot be null");
    }

    /**
     * Add a capability to this entity.
     *
     * @param capabilityClass capability interface class
     * @param capability      capability implementation
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void addCapability(Class<? extends Capability> capabilityClass, Capability capability) {
        capabilities.put(capabilityClass, capability);
    }

    /**
     * Check if entity has a specific capability.
     *
     * @param capabilityClass capability interface class
     * @param <C>             capability type
     * @return true if capability exists
     */
    public <C extends Capability> boolean hasCapability(Class<C> capabilityClass) {
        return capabilities.containsKey(capabilityClass);
    }

    /**
     * Get a capability from this entity.
     *
     * @param capabilityClass capability interface class
     * @param <C>             capability type
     * @return capability instance, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <C extends Capability> C getCapability(Class<C> capabilityClass) {
        return (C) capabilities.get(capabilityClass);
    }

    /**
     * Get all capabilities as unmodifiable map.
     *
     * @return map of capabilities
     */
    public Map<Class<? extends Capability>, Capability> getCapabilities() {
        return Collections.unmodifiableMap(capabilities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogisticsEntity that = (LogisticsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", type, id);
    }
}
