package com.semi.simlogistics.control.traffic;

import com.semi.simlogistics.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manager for vehicle priority calculation with aging support.
 * <p>
 * Implements priority aging formula per REQ-TC-011:
 * effectivePriority = basePriority + floor(waited / agingStep) * agingBoost
 * <p>
 * Also provides priority queue sorting per REQ-TC-005:
 * - Sort by effective priority (highest first)
 * - FIFO for same effective priority (earlier wait start wins)
 * - Random tie-break for same priority and wait time
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-08
 */
public class PriorityManager {

    private final double agingStep; // seconds (default 30s per REQ-TC-000)
    private final int agingBoost; // priority boost per aging step (default 1)
    private final int maxBoost; // maximum boost allowed (default 5 per REQ-TC-000)
    private final Random random; // Random for tie-breaking (4th arbitration rule per REQ-TC-005)

    /**
     * Create a PriorityManager with default aging parameters.
     * <p>
     * Defaults from REQ-TC-000:
     * - agingStep: 30 seconds
     * - agingBoost: 1
     * - maxBoost: 5
     */
    public PriorityManager() {
        this(30.0, 1, 5, new Random());
    }

    /**
     * Create a PriorityManager with custom aging parameters.
     *
     * @param agingStep time step for aging in seconds (must be > 0)
     * @param agingBoost priority boost per aging step (must be > 0)
     * @param maxBoost maximum total boost allowed (must be > 0)
     */
    public PriorityManager(double agingStep, int agingBoost, int maxBoost) {
        this(agingStep, agingBoost, maxBoost, new Random());
    }

    /**
     * Create a PriorityManager with custom aging parameters and Random injection.
     * <p>
     * This constructor allows injecting a Random instance for testing (deterministic tie-break).
     *
     * @param agingStep time step for aging in seconds (must be > 0)
     * @param agingBoost priority boost per aging step (must be > 0)
     * @param maxBoost maximum total boost allowed (must be > 0)
     * @param random Random instance for tie-breaking (must not be null)
     */
    public PriorityManager(double agingStep, int agingBoost, int maxBoost, Random random) {
        if (agingStep <= 0) {
            throw new IllegalArgumentException("agingStep must be > 0, got: " + agingStep);
        }
        if (agingBoost <= 0) {
            throw new IllegalArgumentException("agingBoost must be > 0, got: " + agingBoost);
        }
        if (maxBoost <= 0) {
            throw new IllegalArgumentException("maxBoost must be > 0, got: " + maxBoost);
        }
        if (random == null) {
            throw new IllegalArgumentException("Random cannot be null");
        }
        this.agingStep = agingStep;
        this.agingBoost = agingBoost;
        this.maxBoost = maxBoost;
        this.random = random;
    }

    /**
     * Calculate effective priority for a vehicle with aging applied.
     * <p>
     * Per REQ-TC-011: effectivePriority = basePriority + floor(waited / agingStep) * agingBoost
     * The boost is capped at maxBoost.
     *
     * @param vehicle vehicle to calculate priority for
     * @param waitStartTime simulation time when vehicle started waiting
     * @param currentTime current simulation time (from env.now())
     * @return effective priority (1-10, where 10 is highest)
     */
    public int calculateEffectivePriority(Vehicle vehicle, double waitStartTime, double currentTime) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        if (currentTime < waitStartTime) {
            throw new IllegalArgumentException(
                "currentTime cannot be less than waitStartTime: " + currentTime + " < " + waitStartTime);
        }

        int basePriority = vehicle.getPriority();
        double waited = currentTime - waitStartTime;

        // Calculate aging boost
        int boost = (int) Math.floor(waited / agingStep) * agingBoost;
        boost = Math.min(boost, maxBoost); // Cap at maxBoost

        int effectivePriority = basePriority + boost;

        // Ensure final priority is within valid range (1-10)
        return Math.min(effectivePriority, 10);
    }

    /**
     * Calculate effective priority for a waiting vehicle.
     * <p>
     * Convenience method that extracts wait start time from ControlPoint.
     *
     * @param vehicle vehicle to calculate priority for
     * @param waitedSeconds time waited in seconds
     * @return effective priority (1-10, where 10 is highest)
     */
    public int calculateEffectivePriority(Vehicle vehicle, double waitedSeconds) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        if (waitedSeconds < 0) {
            throw new IllegalArgumentException("waitedSeconds cannot be negative: " + waitedSeconds);
        }

        int basePriority = vehicle.getPriority();

        // Calculate aging boost
        int boost = (int) Math.floor(waitedSeconds / agingStep) * agingBoost;
        boost = Math.min(boost, maxBoost); // Cap at maxBoost

        int effectivePriority = basePriority + boost;

        // Ensure final priority is within valid range (1-10)
        return Math.min(effectivePriority, 10);
    }

    /**
     * Get aging step in seconds.
     *
     * @return aging step in seconds
     */
    public double getAgingStep() {
        return agingStep;
    }

    /**
     * Get aging boost per step.
     *
     * @return aging boost
     */
    public int getAgingBoost() {
        return agingBoost;
    }

    /**
     * Get maximum boost allowed.
     *
     * @return maximum boost
     */
    public int getMaxBoost() {
        return maxBoost;
    }

    // ==================== Priority Queue Sorting (REQ-TC-005, REQ-TC-011) ====================

    /**
     * Sort vehicles by priority queue ordering (REQ-TC-005, REQ-TC-011).
     * <p>
     * Arbitration rules per REQ-TC-005:
     * 1) Higher effective priority wins (base + aging)
     * 2) If tie, earlier wait start time (FIFO) wins
     * 3) If still tie, random selection (using injected Random)
     * <p>
     * This method creates a new sorted list and does not modify the input list.
     *
     * @param vehicles list of vehicles to sort (will not be modified)
     * @param waitStartTime map of vehicle ID to wait start time
     * @param currentTime current simulation time (from env.now())
     * @return new list sorted by priority (highest effective priority first)
     * @throws IllegalArgumentException if vehicles, waitStartTime, or currentTime is null
     */
    public List<Vehicle> sortByPriority(List<Vehicle> vehicles, Map<String, Double> waitStartTime, double currentTime) {
        if (vehicles == null) {
            throw new IllegalArgumentException("Vehicles list cannot be null");
        }
        if (waitStartTime == null) {
            throw new IllegalArgumentException("Wait start time map cannot be null");
        }

        // Create a copy to avoid modifying the original list
        List<Vehicle> sorted = new ArrayList<>(vehicles);

        // Sort using the priority comparator
        sorted.sort(createPriorityComparator(waitStartTime, currentTime));

        return sorted;
    }

    /**
     * Create a Comparator for sorting vehicles by priority queue ordering (REQ-TC-005, REQ-TC-011).
     * <p>
     * Arbitration rules per REQ-TC-005:
     * 1) Higher effective priority wins (base + aging)
     * 2) If tie, earlier wait start time (FIFO) wins
     * 3) If still tie, random selection (using injected Random)
     * <p>
     * The Random instance is used for tie-breaking, ensuring deterministic results with fixed seeds.
     *
     * @param waitStartTime map of vehicle ID to wait start time
     * @param currentTime current simulation time (from env.now())
     * @return Comparator for sorting vehicles by priority
     * @throws IllegalArgumentException if waitStartTime or currentTime is null
     */
    public Comparator<Vehicle> createPriorityComparator(Map<String, Double> waitStartTime, double currentTime) {
        if (waitStartTime == null) {
            throw new IllegalArgumentException("Wait start time map cannot be null");
        }

        return (v1, v2) -> {
            // Calculate effective priorities
            int priority1 = calculateEffectivePriority(v1, waitStartTime.get(v1.id()), currentTime);
            int priority2 = calculateEffectivePriority(v2, waitStartTime.get(v2.id()), currentTime);

            // Rule 1: Higher effective priority wins
            if (priority1 != priority2) {
                return Integer.compare(priority2, priority1); // Descending order
            }

            // Rule 2: If tie, earlier wait start time (FIFO) wins
            Double waitStart1 = waitStartTime.get(v1.id());
            Double waitStart2 = waitStartTime.get(v2.id());

            // Handle missing wait start times (treat as current time)
            if (waitStart1 == null) waitStart1 = currentTime;
            if (waitStart2 == null) waitStart2 = currentTime;

            int timeComparison = Double.compare(waitStart1, waitStart2);
            if (timeComparison != 0) {
                return timeComparison; // Ascending order (earlier first)
            }

            // Rule 3: If still tie, random selection (using injected Random)
            // This ensures true random behavior, deterministic with fixed seed
            return random.nextBoolean() ? -1 : 1;
        };
    }
}
