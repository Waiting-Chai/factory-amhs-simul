package com.semi.simlogistics.device;

import com.semi.simlogistics.capability.LoadUnloadCapability;
import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.entity.DeviceEntity;
import com.semi.simlogistics.operator.Operator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Manual workstation for operator operations.
 * <p>
 * ManualStation provides a work area where operators perform manual tasks.
 * It has a buffer for incoming and outgoing materials.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class ManualStation extends DeviceEntity implements LoadUnloadCapability {

    private final int capacity;
    private final Deque<Cargo> buffer;
    private boolean operatorPresent;

    public ManualStation(String id, String name, Position position, int capacity,
                         List<TransportType> supportedTransportTypes) {
        super(id, name, EntityType.MANUAL_STATION, position, supportedTransportTypes);
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
        this.operatorPresent = false;

        // Register capability
        addCapability(LoadUnloadCapability.class, this);
    }

    /**
     * Check if operator is present at station.
     *
     * @return true if operator is present
     */
    public boolean isOperatorPresent() {
        return operatorPresent;
    }

    /**
     * Set operator presence at station.
     *
     * @param present true if operator is present
     */
    public void setOperatorPresent(boolean present) {
        this.operatorPresent = present;
    }

    @Override
    public boolean load(Cargo cargo) {
        if (isFull()) {
            return false;
        }

        // FIFO: add to end of queue
        buffer.addLast(cargo);
        return true;
    }

    @Override
    public Cargo unload() {
        if (isEmpty()) {
            return null;
        }

        // FIFO: remove from front of queue
        return buffer.removeFirst();
    }

    @Override
    public int getCurrentLoad() {
        return buffer.size();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isFull() {
        return buffer.size() >= capacity;
    }

    @Override
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    /**
     * Get available capacity (number of free slots).
     *
     * @return available capacity
     */
    public int getAvailableCapacity() {
        return capacity - buffer.size();
    }

    /**
     * Compute processing time based on operator skill and operation type.
     * <p>
     * This is a placeholder implementation for Phase 1. In Phase 2/3, this will be
     * replaced with a probabilistic distribution model that considers:
     * <ul>
     * <li>Operator skill level (beginner, intermediate, expert)</li>
     * <li>Operation complexity (simple, standard, complex)</li>
     * <li>Operator fatigue and experience factors</li>
     * <li>Statistical variation (normal, log-normal, or triangular distributions)</li>
     * </ul>
     *
     * @param operator the operator performing the operation (must be non-null and on shift)
     * @param operationType the type of operation being performed (e.g., "load", "unload", "inspect");
     *                      if null, treated as "no skill" and returns base time
     * @return estimated processing time in seconds
     * @throws IllegalArgumentException if operator is null
     * @throws IllegalStateException if operator is not on shift
     */
    public double computeProcessTime(Operator operator, String operationType) {
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }
        if (!operator.isOnShift()) {
            throw new IllegalStateException("Operator is not on shift");
        }

        // Placeholder logic: higher skill reduces time
        // null operationType is treated as "no skill" (same as untrained operator)
        // Phase 2/3: Will replace with probability distribution model
        double baseTime = 60.0; // Base time in seconds
        double skillFactor = (operationType != null && operator.hasSkill(operationType)) ? 0.8 : 1.0;
        return baseTime * skillFactor;
    }
}
