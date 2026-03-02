package com.semi.simlogistics.capability;

import com.semi.simlogistics.core.Cargo;

/**
 * Load/unload capability for device entities (e.g., Stocker, Machine).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public interface LoadUnloadCapability extends Capability {

    /**
     * Load cargo into device.
     *
     * @param cargo cargo to load
     * @return true if load successful
     */
    boolean load(Cargo cargo);

    /**
     * Unload cargo from device.
     *
     * @return unloaded cargo, or null if device is empty
     */
    Cargo unload();

    /**
     * Get current capacity usage.
     *
     * @return number of items currently stored
     */
    int getCurrentLoad();

    /**
     * Get maximum capacity.
     *
     * @return maximum capacity
     */
    int getCapacity();

    /**
     * Check if device is at full capacity.
     *
     * @return true if full
     */
    boolean isFull();

    /**
     * Check if device is empty.
     *
     * @return true if empty
     */
    boolean isEmpty();
}
