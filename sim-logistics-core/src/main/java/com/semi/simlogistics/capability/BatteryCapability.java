package com.semi.simlogistics.capability;

/**
 * Battery capability for electric vehicles (e.g., AGV).
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public interface BatteryCapability extends Capability {

    /**
     * Get current battery level (0.0 to 1.0).
     *
     * @return battery level
     */
    double getBatteryLevel();

    /**
     * Charge battery by specified amount.
     *
     * @param amount amount to charge (0.0 to 1.0)
     */
    void charge(double amount);

    /**
     * Check if vehicle is currently charging.
     *
     * @return true if charging
     */
    boolean isCharging();

    /**
     * Check if battery level is below low battery threshold.
     *
     * @return true if battery is low
     */
    boolean isLowBattery();

    /**
     * Get low battery threshold.
     *
     * @return threshold value (0.0 to 1.0)
     */
    double getLowBatteryThreshold();

    /**
     * Consume battery power.
     *
     * @param amount amount to consume (0.0 to 1.0)
     */
    void consume(double amount);
}
