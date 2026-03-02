package com.semi.simlogistics.device;

import com.semi.simlogistics.capability.LoadUnloadCapability;
import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.entity.DeviceEntity;

import java.util.List;
import java.util.Objects;

/**
 * Equipment Rack / Buffer storage.
 * <p>
 * E-Rack provides temporary buffer storage with FIFO (First In First Out) behavior.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class ERack extends DeviceEntity implements LoadUnloadCapability {

    private final int capacity;
    private final Cargo[] slots;
    private int head; // Index for dequeue
    private int tail; // Index for enqueue
    private int size;

    public ERack(String id, String name, Position position, int capacity,
                 List<TransportType> supportedTransportTypes) {
        super(id, name, EntityType.ERACK, position, supportedTransportTypes);
        this.capacity = capacity;
        this.slots = new Cargo[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;

        // Register capability
        addCapability(LoadUnloadCapability.class, this);
    }

    @Override
    public boolean load(Cargo cargo) {
        Objects.requireNonNull(cargo, "Cargo cannot be null");

        if (isFull()) {
            return false;
        }

        slots[tail] = cargo;
        tail = (tail + 1) % capacity;
        size++;
        return true;
    }

    @Override
    public Cargo unload() {
        if (isEmpty()) {
            return null;
        }

        Cargo cargo = slots[head];
        slots[head] = null; // Help GC
        head = (head + 1) % capacity;
        size--;
        return cargo;
    }

    @Override
    public int getCurrentLoad() {
        return size;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isFull() {
        return size >= capacity;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Peek at the next cargo to be unloaded without removing it.
     *
     * @return next cargo, or null if empty
     */
    public Cargo peek() {
        if (isEmpty()) {
            return null;
        }
        return slots[head];
    }

    /**
     * Get available capacity (number of free slots).
     *
     * @return available capacity
     */
    public int getAvailableCapacity() {
        return capacity - size;
    }
}
