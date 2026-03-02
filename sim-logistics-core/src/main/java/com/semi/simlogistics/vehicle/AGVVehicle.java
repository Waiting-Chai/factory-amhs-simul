package com.semi.simlogistics.vehicle;

import com.semi.jSimul.core.Process;
import com.semi.simlogistics.capability.BatteryCapability;
import com.semi.simlogistics.capability.DefaultBatteryCapability;
import com.semi.simlogistics.capability.TransportCapability;
import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.VehicleState;

/**
 * Automated Guided Vehicle (AGV) entity.
 * <p>
 * AGV vehicles move on floor networks and have battery management.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class AGVVehicle extends Vehicle {

    private final double maxSpeed; // meters per second
    private final double batteryConsumptionRate; // per meter
    private final double chargingRate; // per second

    public AGVVehicle(String id, String name, Position position, double maxSpeed,
                      double batteryConsumptionRate, double chargingRate,
                      double initialBatteryLevel, double lowBatteryThreshold) {
        super(id, name, EntityType.AGV_VEHICLE, position,
              new DefaultBatteryCapability(initialBatteryLevel, lowBatteryThreshold));
        this.maxSpeed = maxSpeed;
        this.batteryConsumptionRate = batteryConsumptionRate;
        this.chargingRate = chargingRate;

        // Initialize transport capability after super() returns
        initTransportCapability();

        // Register capabilities
        addCapability(TransportCapability.class, getTransport());
        addCapability(BatteryCapability.class, getBattery());
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getBatteryConsumptionRate() {
        return batteryConsumptionRate;
    }

    public double getChargingRate() {
        return chargingRate;
    }

    @Override
    protected Object run(Process.ProcessContext ctx) throws Exception {
        // AGV vehicle behavior loop
        while (true) {
            // Check battery level
            if (battery != null && battery.isLowBattery() && state != VehicleState.CHARGING) {
                // Need to go to charging station
                setState(VehicleState.CHARGING);
                // In real simulation, would navigate to charging station
                // For now, just charge in place
                battery.charge(chargingRate);
                setState(VehicleState.IDLE);
                continue;
            }

            switch (state) {
                case IDLE:
                    // Wait for task assignment
                    ctx.await(ctx.env().timeout(1.0));
                    break;

                case MOVING:
                    // Moving is handled by moveTo() calls
                    ctx.await(ctx.env().timeout(0.1));
                    break;

                case LOADING:
                case UNLOADING:
                    // Loading/unloading is handled by load()/unload() calls
                    ctx.await(ctx.env().timeout(0.1));
                    break;

                case CHARGING:
                    // Charging battery
                    if (battery != null && battery.getBatteryLevel() < 1.0) {
                        battery.charge(chargingRate * 0.1); // Charge per 0.1s
                        ctx.await(ctx.env().timeout(0.1));
                    } else {
                        setState(VehicleState.IDLE);
                    }
                    break;

                case BLOCKED:
                    // Waiting for traffic control
                    ctx.await(ctx.env().timeout(0.1));
                    break;

                case FAILED:
                    // Vehicle has failed, exit process
                    return "AGV failed";

                default:
                    ctx.await(ctx.env().timeout(0.1));
            }
        }
    }

    /**
     * Move AGV to destination on network.
     * <p>
     * Movement time = distance / speed.
     * Battery consumption = distance * consumptionRate.
     *
     * @param destination target position
     */
    public void moveTo(Position destination) {
        if (state != VehicleState.IDLE && state != VehicleState.BLOCKED) {
            throw new IllegalStateException("AGV is not available for movement, current state: " + state);
        }

        if (battery != null && battery.isLowBattery()) {
            throw new IllegalStateException("AGV battery is too low for movement");
        }

        setState(VehicleState.MOVING);

        double distance = position().distanceTo(destination);
        double travelTime = distance / maxSpeed;

        // Consume battery for movement
        if (battery != null) {
            double consumption = distance * batteryConsumptionRate;
            battery.consume(consumption);
        }

        // Use transport capability to update position
        transport.moveTo(destination);

        setState(VehicleState.IDLE);
    }

    /**
     * Load cargo onto AGV.
     *
     * @param cargo cargo to load
     */
    public void load(Cargo cargo) {
        if (state != VehicleState.IDLE) {
            throw new IllegalStateException("AGV is not available for loading, current state: " + state);
        }

        setState(VehicleState.LOADING);
        transport.load(cargo);
        setState(VehicleState.IDLE);
    }

    /**
     * Unload cargo from AGV.
     */
    public void unload() {
        if (state != VehicleState.IDLE) {
            throw new IllegalStateException("AGV is not available for unloading, current state: " + state);
        }

        setState(VehicleState.UNLOADING);
        transport.unload();
        setState(VehicleState.IDLE);
    }
}
