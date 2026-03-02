package com.semi.simlogistics.capability;

import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.PositionUpdater;

/**
 * Default implementation of TransportCapability.
 * <p>
 * This capability tracks current cargo and uses PositionUpdater to update
 * entity position when moving.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class DefaultTransportCapability implements TransportCapability {

    private Cargo currentCargo;
    private final PositionUpdater positionUpdater;

    /**
     * Create transport capability with position updater.
     *
     * @param positionUpdater callback to update entity position
     */
    public DefaultTransportCapability(PositionUpdater positionUpdater) {
        this.positionUpdater = positionUpdater;
    }

    @Override
    public void moveTo(Position destination) {
        if (positionUpdater != null) {
            positionUpdater.update(destination);
        }
    }

    @Override
    public void load(Cargo cargo) {
        if (currentCargo != null) {
            throw new IllegalStateException("Vehicle already loaded");
        }
        this.currentCargo = cargo;
    }

    @Override
    public void unload() {
        if (currentCargo == null) {
            throw new IllegalStateException("Vehicle is not loaded");
        }
        this.currentCargo = null;
    }

    @Override
    public boolean isLoaded() {
        return currentCargo != null;
    }

    @Override
    public Cargo getCurrentCargo() {
        return currentCargo;
    }
}
