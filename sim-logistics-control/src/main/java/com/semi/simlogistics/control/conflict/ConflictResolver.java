package com.semi.simlogistics.control.conflict;

import com.semi.simlogistics.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Conflict resolver for vehicle arbitration per REQ-TC-007.
 * <p>
 * Implements the following arbitration order:
 * 1. Base priority (Vehicle.getPriority()) - higher wins
 * 2. Arrival time (FIFO) - earlier arrival wins
 * 3. Distance to resource - closer wins
 * 4. Random tie-breaking - for complete ties
 * <p>
 * Note: This resolver uses base priority only. For effective priority with
 * aging boost, use PriorityManager.calculateEffectivePriority() before calling
 * this resolver, or integrate PriorityManager directly into the arbitration logic.
 * <p>
 * This resolver is used by ControlPoint and other traffic components
 * to decide which vehicle gets access to limited resources.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-08
 */
public class ConflictResolver {

    private final Random random;

    /**
     * Create a conflict resolver with random seed.
     *
     * @param seed random seed for tie-breaking (use fixed seed for reproducible tests)
     */
    public ConflictResolver(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Create a conflict resolver with random seed.
     */
    public ConflictResolver() {
        this(System.currentTimeMillis());
    }

    /**
     * Resolve conflict among competing vehicles based on priority only.
     * <p>
     * Arbitration order:
     * 1. Higher priority wins
     * 2. Random tie-break for equal priorities
     *
     * @param competingVehicles list of vehicles competing for the resource
     * @param <T> vehicle type
     * @return the winning vehicle, or null if list is empty
     */
    public <T extends Vehicle> T resolve(List<T> competingVehicles) {
        if (competingVehicles == null || competingVehicles.isEmpty()) {
            return null;
        }

        if (competingVehicles.size() == 1) {
            return competingVehicles.get(0);
        }

        // Find maximum priority
        int maxPriority = competingVehicles.stream()
                .mapToInt(Vehicle::getPriority)
                .max()
                .orElse(0);

        // Collect all vehicles with max priority (for potential tie-break)
        List<T> candidates = new ArrayList<>();
        for (T vehicle : competingVehicles) {
            if (vehicle.getPriority() == maxPriority) {
                candidates.add(vehicle);
            }
        }

        // If multiple candidates with same priority, random tie-break
        if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            int randomIndex = random.nextInt(candidates.size());
            return candidates.get(randomIndex);
        }
    }

    /**
     * Resolve conflict among competing vehicles considering arrival time.
     * <p>
     * Arbitration order per REQ-TC-007:
     * 1. Higher priority wins
     * 2. Earlier arrival time wins (FIFO)
     * 3. Random tie-break for complete ties
     *
     * @param competingVehicles list of vehicles competing for the resource
     * @param arrivalTime arrival time for vehicle at index 0
     * @param timeIncrement time increment between subsequent vehicles
     * @param <T> vehicle type
     * @return the winning vehicle, or null if list is empty
     */
    public <T extends Vehicle> T resolveWithArrivalTime(List<T> competingVehicles, long arrivalTime, long timeIncrement) {
        if (competingVehicles == null || competingVehicles.isEmpty()) {
            return null;
        }

        if (competingVehicles.size() == 1) {
            return competingVehicles.get(0);
        }

        // Collect all candidates with same priority and arrival time (for potential tie-break)
        List<T> candidates = new ArrayList<>();

        int bestPriority = Integer.MIN_VALUE;
        long bestArrivalTime = Long.MAX_VALUE;

        for (int i = 0; i < competingVehicles.size(); i++) {
            T currentVehicle = competingVehicles.get(i);
            int currentPriority = currentVehicle.getPriority();
            long currentArrivalTime = arrivalTime + (i * timeIncrement);

            // Clear candidates if we found better priority or earlier arrival
            if (currentPriority > bestPriority ||
                (currentPriority == bestPriority && currentArrivalTime < bestArrivalTime)) {
                candidates.clear();
                bestPriority = currentPriority;
                bestArrivalTime = currentArrivalTime;
            }

            // Add to candidates if matches best criteria
            if (currentPriority == bestPriority && currentArrivalTime == bestArrivalTime) {
                candidates.add(currentVehicle);
            }
        }

        // If multiple candidates (complete tie), random selection
        if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            int randomIndex = random.nextInt(candidates.size());
            return candidates.get(randomIndex);
        }
    }

    /**
     * Resolve conflict among competing vehicles considering arrival time and distance.
     * <p>
     * Arbitration order per REQ-TC-007:
     * 1. Base priority (higher wins)
     * 2. Arrival time (FIFO - earlier wins)
     * 3. Distance to resource (closer wins)
     * 4. Random tie-break for complete ties
     *
     * @param competingVehicles list of vehicles competing for the resource
     * @param arrivalTime arrival time for vehicle at index 0
     * @param timeIncrement time increment between subsequent vehicles
     * @param distances distance of each vehicle to the resource (same order as competingVehicles)
     * @param <T> vehicle type
     * @return the winning vehicle, or null if list is empty
     * @throws IllegalArgumentException if distances size doesn't match vehicles size
     */
    public <T extends Vehicle> T resolveWithArrivalAndDistance(
            List<T> competingVehicles,
            long arrivalTime,
            long timeIncrement,
            List<Double> distances) {

        if (competingVehicles == null || competingVehicles.isEmpty()) {
            return null;
        }

        if (competingVehicles.size() == 1) {
            return competingVehicles.get(0);
        }

        if (distances == null || distances.size() != competingVehicles.size()) {
            throw new IllegalArgumentException("Distances list must match vehicles list size");
        }

        // Collect all candidates with same priority, arrival time, and distance
        List<T> candidates = new ArrayList<>();

        int bestPriority = Integer.MIN_VALUE;
        long bestArrivalTime = Long.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < competingVehicles.size(); i++) {
            T currentVehicle = competingVehicles.get(i);
            int currentPriority = currentVehicle.getPriority();
            long currentArrivalTime = arrivalTime + (i * timeIncrement);
            double currentDistance = distances.get(i);

            // Clear candidates if we found better priority, earlier arrival, or shorter distance
            boolean betterPriority = currentPriority > bestPriority;
            boolean samePriorityBetterArrival = currentPriority == bestPriority && currentArrivalTime < bestArrivalTime;
            boolean samePriorityAndArrivalBetterDistance = currentPriority == bestPriority
                    && currentArrivalTime == bestArrivalTime && currentDistance < bestDistance;

            if (betterPriority || samePriorityBetterArrival || samePriorityAndArrivalBetterDistance) {
                candidates.clear();
                bestPriority = currentPriority;
                bestArrivalTime = currentArrivalTime;
                bestDistance = currentDistance;
            }

            // Add to candidates if matches all best criteria
            if (currentPriority == bestPriority && currentArrivalTime == bestArrivalTime && currentDistance == bestDistance) {
                candidates.add(currentVehicle);
            }
        }

        // If multiple candidates (complete tie), random selection
        if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            int randomIndex = random.nextInt(candidates.size());
            return candidates.get(randomIndex);
        }
    }
}
