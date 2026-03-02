package com.semi.simlogistics.capability;

/**
 * Default implementation of BatteryCapability.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class DefaultBatteryCapability implements BatteryCapability {

    private double batteryLevel; // 0.0 to 1.0
    private boolean charging;
    private final double lowBatteryThreshold;

    public DefaultBatteryCapability(double initialLevel, double lowBatteryThreshold) {
        if (initialLevel < 0.0 || initialLevel > 1.0) {
            throw new IllegalArgumentException("Initial battery level must be between 0.0 and 1.0");
        }
        if (lowBatteryThreshold < 0.0 || lowBatteryThreshold > 1.0) {
            throw new IllegalArgumentException("Low battery threshold must be between 0.0 and 1.0");
        }

        this.batteryLevel = initialLevel;
        this.lowBatteryThreshold = lowBatteryThreshold;
        this.charging = false;
    }

    public DefaultBatteryCapability() {
        this(1.0, 0.2); // Full charge, 20% low battery threshold
    }

    @Override
    public double getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public void charge(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Charge amount cannot be negative");
        }

        batteryLevel = Math.min(1.0, batteryLevel + amount);

        if (batteryLevel >= 1.0) {
            charging = false;
        }
    }

    @Override
    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    @Override
    public boolean isLowBattery() {
        return batteryLevel <= lowBatteryThreshold;
    }

    @Override
    public double getLowBatteryThreshold() {
        return lowBatteryThreshold;
    }

    /**
     * Consume battery power.
     *
     * @param amount amount to consume (0.0 to 1.0)
     */
    @Override
    public void consume(double amount) {
        if (amount < 0.0) {
            throw new IllegalArgumentException("Consumption amount cannot be negative");
        }

        batteryLevel = Math.max(0.0, batteryLevel - amount);
    }
}
