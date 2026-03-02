package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.vehicle.AGVVehicle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for priority aging (REQ-TC-011).
 * <p>
 * Tests that vehicle priority increases with wait time using the formula:
 * effectivePriority = basePriority + floor(waited / agingStep) * agingBoost
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class PriorityAgingTest {

    @Test
    void testPriorityAgingFormula() {
        // Given: Priority aging parameters
        int basePriority = 5;
        double agingStep = 30.0; // seconds
        int agingBoost = 1;
        int maxBoost = 5;

        // When: Vehicle waits for different durations
        // TODO: Implement PriorityManager to calculate effective priority

        // Case 1: Wait 0 seconds -> no boost
        double waitTime1 = 0.0;
        int effectivePriority1 = calculateEffectivePriority(basePriority, waitTime1, agingStep, agingBoost, maxBoost);
        assertThat(effectivePriority1).isEqualTo(5); // base + 0

        // Case 2: Wait 29 seconds -> no boost (less than agingStep)
        double waitTime2 = 29.0;
        int effectivePriority2 = calculateEffectivePriority(basePriority, waitTime2, agingStep, agingBoost, maxBoost);
        assertThat(effectivePriority2).isEqualTo(5); // base + 0

        // Case 3: Wait 30 seconds -> +1 boost
        double waitTime3 = 30.0;
        int effectivePriority3 = calculateEffectivePriority(basePriority, waitTime3, agingStep, agingBoost, maxBoost);
        assertThat(effectivePriority3).isEqualTo(6); // base + 1

        // Case 4: Wait 60 seconds -> +2 boost
        double waitTime4 = 60.0;
        int effectivePriority4 = calculateEffectivePriority(basePriority, waitTime4, agingStep, agingBoost, maxBoost);
        assertThat(effectivePriority4).isEqualTo(7); // base + 2
    }

    @Test
    void testPriorityAgingWithMaxBoost() {
        // Given: Priority aging parameters with max boost limit
        int basePriority = 5;
        double agingStep = 30.0; // seconds
        int agingBoost = 1;
        int maxBoost = 5;

        // When: Vehicle waits for a very long time
        double waitTime = 1000.0; // Would be +33 boost without limit

        // Then: Priority should be capped at maxBoost
        int effectivePriority = calculateEffectivePriority(basePriority, waitTime, agingStep, agingBoost, maxBoost);
        assertThat(effectivePriority).isEqualTo(10); // base + maxBoost (5 + 5)
    }

    @Test
    void testPriorityAgingWithDifferentAgingSteps() {
        // Given: Different aging step sizes
        int basePriority = 5;
        int agingBoost = 2;
        int maxBoost = 10;
        double waitTime = 60.0;

        // Case 1: agingStep = 20 seconds
        double agingStep1 = 20.0;
        int effectivePriority1 = calculateEffectivePriority(basePriority, waitTime, agingStep1, agingBoost, maxBoost);
        // floor(60 / 20) * 2 = 3 * 2 = 6
        assertThat(effectivePriority1).isEqualTo(11); // 5 + 6

        // Case 2: agingStep = 30 seconds
        double agingStep2 = 30.0;
        int effectivePriority2 = calculateEffectivePriority(basePriority, waitTime, agingStep2, agingBoost, maxBoost);
        // floor(60 / 30) * 2 = 2 * 2 = 4
        assertThat(effectivePriority2).isEqualTo(9); // 5 + 4
    }

    @Test
    void testPriorityAgingResetsAfterAcquisition() {
        // Given: A vehicle with aged priority
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle lowPriorityVehicle = new AGVVehicle("V-LOW", "Low Priority", pos, 1.0, 0.01, 0.1, 1.0, 0.2);
        AGVVehicle highPriorityVehicle = new AGVVehicle("V-HIGH", "High Priority", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double currentTime = 0.0;

        // Set base priorities (will fail - Vehicle doesn't have setPriority() yet)
        // TODO: Add priority field to Vehicle

        // Low priority vehicle waits and ages
        cp.requestEntry(highPriorityVehicle, currentTime);
        boolean granted = cp.requestEntry(lowPriorityVehicle, currentTime + 60.0);

        // When: Low priority vehicle finally acquires control point
        // Then: Its effective priority should reset to base priority
        // TODO: Implement PriorityManager with reset logic
    }

    @Test
    void testPriorityAgingUsesSimulationTime() {
        // Given: A vehicle waiting at control point
        ControlPoint cp = new ControlPoint("CP-1", "NODE-A", 1);

        Position pos = new Position(0.0, 0.0, 0.0);
        AGVVehicle vehicle = new AGVVehicle("V-1", "Vehicle 1", pos, 1.0, 0.01, 0.1, 1.0, 0.2);

        double requestTime = 100.0; // Simulation time
        double currentTime = 150.0; // Simulation time

        // When: Calculating priority aging
        // Then: Should use simulation time (not wall clock time)
        double waited = currentTime - requestTime; // 50 simulation seconds
        assertThat(waited).isEqualTo(50.0);

        // TODO: Implement PriorityManager with simulation clock integration
        // The effective priority should be based on waited time, not real time
    }

    /**
     * Helper method to calculate effective priority using the aging formula.
     * <p>
     * This is a placeholder implementation for testing.
     * The actual implementation will be in PriorityManager.
     *
     * @param basePriority base priority
     * @param waited wait time in seconds
     * @param agingStep aging step in seconds
     * @param agingBoost priority boost per aging step
     * @param maxBoost maximum boost allowed
     * @return effective priority
     */
    private int calculateEffectivePriority(int basePriority, double waited,
                                           double agingStep, int agingBoost, int maxBoost) {
        // effectivePriority = basePriority + floor(waited / agingStep) * agingBoost
        int boost = (int) Math.floor(waited / agingStep) * agingBoost;
        boost = Math.min(boost, maxBoost); // Cap at maxBoost
        return basePriority + boost;
    }
}
