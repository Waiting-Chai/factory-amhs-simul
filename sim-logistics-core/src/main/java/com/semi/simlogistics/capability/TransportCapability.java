package com.semi.simlogistics.capability;

import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.Position;

/**
 * Transport capability for vehicle movement and cargo handling.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public interface TransportCapability extends Capability {

    /**
     * Move vehicle to destination position.
     *
     * @param destination target position
     */
    void moveTo(Position destination);

    /**
     * Load cargo onto vehicle.
     *
     * @param cargo cargo to load
     */
    void load(Cargo cargo);

    /**
     * Unload cargo from vehicle.
     */
    void unload();

    /**
     * Check if vehicle has cargo loaded.
     *
     * @return true if vehicle is loaded
     */
    boolean isLoaded();

    /**
     * Get current cargo if loaded.
     *
     * @return current cargo, or null if not loaded
     */
    Cargo getCurrentCargo();
}
