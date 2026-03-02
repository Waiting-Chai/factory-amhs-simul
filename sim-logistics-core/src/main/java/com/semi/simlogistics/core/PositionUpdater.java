package com.semi.simlogistics.core;

/**
 * Interface for updating position of logistics entities.
 * <p>
 * This interface allows TransportCapability to update entity position
 * without tight coupling to specific entity types.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
@FunctionalInterface
public interface PositionUpdater {

    /**
     * Update position to the specified destination.
     *
     * @param destination new position
     */
    void update(Position destination);
}
