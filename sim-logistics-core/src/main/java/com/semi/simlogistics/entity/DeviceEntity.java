package com.semi.simlogistics.entity;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;

import java.util.List;
import java.util.Objects;

/**
 * Base class for device entities that can handle materials.
 * <p>
 * Devices must declare supported transport types for validation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public abstract class DeviceEntity extends LogisticsEntity {

    private final List<TransportType> supportedTransportTypes;

    protected DeviceEntity(String id, String name, EntityType type, Position position,
                           List<TransportType> supportedTransportTypes) {
        super(id, name, type, position);
        Objects.requireNonNull(supportedTransportTypes, "Supported transport types cannot be null");

        if (supportedTransportTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Device entity must have at least one supported transport type");
        }

        // Create unmodifiable copy to prevent external modification
        this.supportedTransportTypes = List.copyOf(supportedTransportTypes);
    }

    /**
     * Get supported transport types for this device.
     *
     * @return unmodifiable list of supported transport types
     */
    public List<TransportType> getSupportedTransportTypes() {
        return supportedTransportTypes;
    }

    /**
     * Check if device supports a specific transport type.
     *
     * @param transportType transport type to check
     * @return true if supported
     */
    public boolean supportsTransportType(TransportType transportType) {
        return supportedTransportTypes.contains(transportType);
    }

    /**
     * Check if device supports any of the required transport types.
     *
     * @param requiredTypes list of required transport types
     * @return true if intersection is non-empty
     */
    public boolean supportsTransportTypes(List<TransportType> requiredTypes) {
        if (requiredTypes == null || requiredTypes.isEmpty()) {
            return false;
        }
        return requiredTypes.stream().anyMatch(supportedTransportTypes::contains);
    }
}
