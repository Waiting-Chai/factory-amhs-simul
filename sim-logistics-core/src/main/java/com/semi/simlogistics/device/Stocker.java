package com.semi.simlogistics.device;

import com.semi.simlogistics.capability.LoadUnloadCapability;
import com.semi.simlogistics.core.Cargo;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.entity.DeviceEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Automated Storage and Retrieval System (ASRS) / Stocker.
 * <p>
 * Stocker provides automated storage with slot-based storage management.
 * Each slot can be EMPTY or OCCUPIED, and cargo can be retrieved by ID.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Stocker extends DeviceEntity implements LoadUnloadCapability {

    /**
     * Slot state enumeration.
     */
    public enum SlotState {
        EMPTY,
        OCCUPIED
    }

    private final int capacity;
    private final Cargo[] slots;
    private final SlotState[] slotStates;
    private int usedCapacity;
    private final int craneCount; // Number of cranes for parallel operations

    public Stocker(String id, String name, Position position, int capacity, int craneCount,
                   List<TransportType> supportedTransportTypes) {
        super(id, name, EntityType.STOCKER, position, supportedTransportTypes);
        this.capacity = capacity;
        this.craneCount = craneCount;
        this.slots = new Cargo[capacity];
        this.slotStates = new SlotState[capacity];
        this.usedCapacity = 0;

        // Initialize all slots as EMPTY
        for (int i = 0; i < capacity; i++) {
            slotStates[i] = SlotState.EMPTY;
        }

        // Register capability
        addCapability(LoadUnloadCapability.class, this);
    }

    public int getCraneCount() {
        return craneCount;
    }

    /**
     * Get slot state by index.
     *
     * @param slotIndex slot index (0 to capacity-1)
     * @return slot state
     */
    public SlotState getSlotState(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= capacity) {
            throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
        return slotStates[slotIndex];
    }

    /**
     * Get all occupied slot indices.
     *
     * @return list of occupied slot indices
     */
    public List<Integer> getOccupiedSlots() {
        List<Integer> occupied = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            if (slotStates[i] == SlotState.OCCUPIED) {
                occupied.add(i);
            }
        }
        return occupied;
    }

    /**
     * Get all empty slot indices.
     *
     * @return list of empty slot indices
     */
    public List<Integer> getEmptySlots() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            if (slotStates[i] == SlotState.EMPTY) {
                empty.add(i);
            }
        }
        return empty;
    }

    @Override
    public boolean load(Cargo cargo) {
        if (cargo == null) {
            throw new IllegalArgumentException("Cargo cannot be null");
        }

        if (isFull()) {
            onCapacityFull();
            return false;
        }

        // Find first empty slot
        for (int i = 0; i < capacity; i++) {
            if (slotStates[i] == SlotState.EMPTY) {
                slots[i] = cargo;
                slotStates[i] = SlotState.OCCUPIED;
                usedCapacity++;
                return true;
            }
        }

        return false;
    }

    /**
     * Handle capacity full event.
     * <p>
     * This is a placeholder for Phase 2 implementation which will:
     * <ul>
     * <li>Emit capacity alert event via event bus</li>
     * <li>Queue incoming cargo requests for later processing</li>
     * <li>Notify external systems of capacity constraints</li>
     * </ul>
     * TODO: Integrate with event bus in Phase 2
     */
    protected void onCapacityFull() {
        // TODO: emit capacity alert event (Phase 2)
        // Phase 2: Will integrate with SimulationEventBus to emit:
        // - CapacityAlertEvent with stocker ID, current load, timestamp
        // - Queue management for waiting cargo requests
    }

    @Override
    public Cargo unload() {
        // Unload from first occupied slot (FIFO by slot index)
        // Rule: Always unloads from the lowest-numbered occupied slot (0, 1, 2, ...)
        // This is approximate FIFO because load() always fills from the lowest-numbered empty slot
        for (int i = 0; i < capacity; i++) {
            if (slotStates[i] == SlotState.OCCUPIED) {
                return unloadFromSlot(i);
            }
        }
        return null;
    }

    /**
     * Unload cargo by cargo ID.
     *
     * @param cargoId cargo ID to unload
     * @return unloaded cargo, or null if not found
     */
    public Cargo unloadByCargoId(String cargoId) {
        if (cargoId == null) {
            throw new IllegalArgumentException("Cargo ID cannot be null");
        }

        for (int i = 0; i < capacity; i++) {
            if (slotStates[i] == SlotState.OCCUPIED && slots[i] != null) {
                if (cargoId.equals(slots[i].id())) {
                    return unloadFromSlot(i);
                }
            }
        }
        return null;
    }

    /**
     * Unload cargo from specific slot.
     *
     * @param slotIndex slot index
     * @return unloaded cargo, or null if slot is empty
     */
    public Cargo unloadFromSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= capacity) {
            throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }

        if (slotStates[slotIndex] == SlotState.EMPTY) {
            return null;
        }

        Cargo cargo = slots[slotIndex];
        slots[slotIndex] = null;
        slotStates[slotIndex] = SlotState.EMPTY;
        usedCapacity--;
        return cargo;
    }

    @Override
    public int getCurrentLoad() {
        return usedCapacity;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isFull() {
        return usedCapacity >= capacity;
    }

    @Override
    public boolean isEmpty() {
        return usedCapacity == 0;
    }

    /**
     * Get cargo at specific slot without removing it.
     *
     * @param slotIndex slot index
     * @return cargo at slot, or null if empty
     */
    public Cargo getCargoAtSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= capacity) {
            throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
        return slots[slotIndex];
    }

    /**
     * Get available capacity (number of free slots).
     *
     * @return available capacity
     */
    public int getAvailableCapacity() {
        return capacity - usedCapacity;
    }
}
