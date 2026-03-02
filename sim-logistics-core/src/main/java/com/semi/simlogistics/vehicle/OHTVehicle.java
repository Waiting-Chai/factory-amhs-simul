package com.semi.simlogistics.vehicle;

import com.semi.jSimul.core.Process;
import com.semi.simlogistics.capability.TransportCapability;
import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;

/**
 * Overhead Hoist Transport (OHT) vehicle entity.
 * <p>
 * OHT vehicles move on tracks and do not have batteries (powered by rails).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class OHTVehicle extends Vehicle {

    private final double maxSpeed; // meters per second

    public OHTVehicle(String id, String name, Position position, double maxSpeed) {
        super(id, name, EntityType.OHT_VEHICLE, position, null); // No battery for OHT
        this.maxSpeed = maxSpeed;

        // Initialize transport capability after super() returns
        initTransportCapability();

        // Register capabilities
        addCapability(TransportCapability.class, getTransport());
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    @Override
    protected Object run(Process.ProcessContext ctx) throws Exception {
        // OHT vehicle behavior loop
        while (true) {
            switch (state) {
                case IDLE:
                    // Wait for task assignment
                    ctx.await(ctx.env().timeout(1.0));
                    break;

                case MOVING:
                    // Moving is handled by moveTo() calls
                    // This loop just checks state changes
                    ctx.await(ctx.env().timeout(0.1));
                    break;

                case LOADING:
                case UNLOADING:
                    // Loading/unloading is handled by load()/unload() calls
                    ctx.await(ctx.env().timeout(0.1));
                    break;

                case BLOCKED:
                    // Waiting for traffic control
                    ctx.await(ctx.env().timeout(0.1));
                    break;

                case FAILED:
                    // Vehicle has failed, exit process
                    return "OHT failed";

                default:
                    ctx.await(ctx.env().timeout(0.1));
            }
        }
    }

    /**
     * Move OHT to destination along track.
     * <p>
     * Movement time = distance / speed.
     *
     * @param destination target position
     */
    public void moveTo(Position destination) {
        if (state != VehicleState.IDLE && state != VehicleState.BLOCKED) {
            throw new IllegalStateException("OHT is not available for movement, current state: " + state);
        }

        setState(VehicleState.MOVING);

        double distance = position().distanceTo(destination);
        double travelTime = distance / maxSpeed;

        // Use transport capability to update position
        transport.moveTo(destination);

        setState(VehicleState.IDLE);
    }

    /**
     * Load cargo onto OHT.
     *
     * @param cargo cargo to load
     */
    public void load(Cargo cargo) {
        if (state != VehicleState.IDLE) {
            throw new IllegalStateException("OHT is not available for loading, current state: " + state);
        }

        setState(VehicleState.LOADING);
        transport.load(cargo);
        setState(VehicleState.IDLE);
    }

    /**
     * Unload cargo from OHT.
     */
    public void unload() {
        if (state != VehicleState.IDLE) {
            throw new IllegalStateException("OHT is not available for unloading, current state: " + state);
        }

        setState(VehicleState.UNLOADING);
        transport.unload();
        setState(VehicleState.IDLE);
    }
}
