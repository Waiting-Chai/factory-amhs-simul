package com.semi.simlogistics.vehicle;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;
import com.semi.simlogistics.capability.BatteryCapability;
import com.semi.simlogistics.capability.TransportCapability;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.core.PositionUpdater;
import com.semi.simlogistics.core.TransportType;
import com.semi.simlogistics.core.VehicleState;
import com.semi.simlogistics.entity.LogisticsEntity;

/**
 * Base class for vehicle entities (OHT, AGV).
 * <p>
 * Vehicles compose a JSimul Process for simulation behavior (not inheritance).
 * This follows the design principle of composition over inheritance.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public abstract class Vehicle extends LogisticsEntity implements PositionUpdater {

    protected VehicleState state;
    protected Process process;
    protected TransportCapability transport;
    protected final BatteryCapability battery; // Optional, null for OHT
    protected int priority; // Base priority for traffic control arbitration
    protected String currentPath; // Current planned path (node IDs concatenated)
    protected String destinationNodeId; // Target destination node ID for replanning (REQ-TC-007)
    protected String currentNodeId; // Current node ID for replanning (REQ-TC-007)

    /**
     * Constructor for vehicles with existing transport and battery capabilities.
     * <p>
     * This constructor is provided for backward compatibility but does not support
     * automatic position updates. Use {@link #Vehicle(String, String, EntityType, Position, BatteryCapability)}
     * for vehicles that need position updates.
     *
     * @param id unique identifier
     * @param name human-readable name
     * @param type entity type
     * @param position initial position
     * @param transport transport capability (cannot be null)
     * @param battery battery capability (null for vehicles without battery)
     */
    protected Vehicle(String id, String name, EntityType type, Position position,
                      TransportCapability transport, BatteryCapability battery) {
        super(id, name, type, position);
        this.transport = transport;
        this.battery = battery;
        this.state = VehicleState.IDLE;
        this.priority = 5; // Default priority (middle range: 1-10)
    }

    /**
     * Constructor for vehicles with battery capability and automatic position updates.
     * <p>
     * This constructor creates a TransportCapability that automatically updates
     * the vehicle's position when moveTo() is called. Subclasses should call this
     * constructor and then call {@link #initTransportCapability()} after super() returns.
     *
     * @param id unique identifier
     * @param name human-readable name
     * @param type entity type
     * @param position initial position
     * @param battery battery capability (null for vehicles without battery)
     */
    protected Vehicle(String id, String name, EntityType type, Position position,
                      BatteryCapability battery) {
        super(id, name, type, position);
        this.transport = null; // Will be initialized by initTransportCapability()
        this.battery = battery;
        this.state = VehicleState.IDLE;
        this.priority = 5; // Default priority (middle range: 1-10)
    }

    /**
     * Initialize transport capability with automatic position updates.
     * <p>
     * Subclasses should call this method after their super() constructor if they
     * used the {@link #Vehicle(String, String, EntityType, Position, BatteryCapability)}
     * constructor.
     */
    protected void initTransportCapability() {
        if (this.transport == null) {
            this.transport = new com.semi.simlogistics.capability.DefaultTransportCapability(this);
        }
    }

    /**
     * Get current vehicle state.
     *
     * @return current state
     */
    public VehicleState getState() {
        return state;
    }

    /**
     * Set vehicle state.
     *
     * @param state new state
     */
    public void setState(VehicleState state) {
        this.state = state;
    }

    /**
     * Get transport capability.
     *
     * @return transport capability
     */
    public TransportCapability getTransport() {
        return transport;
    }

    /**
     * Get battery capability (optional).
     *
     * @return battery capability, or null if vehicle has no battery
     */
    public BatteryCapability getBattery() {
        return battery;
    }

    /**
     * Check if vehicle has battery capability.
     *
     * @return true if vehicle has battery
     */
    public boolean hasBattery() {
        return battery != null;
    }

    /**
     * Start simulation process for this vehicle.
     * <p>
     * This method creates a JSimul Process that composes with this vehicle.
     * The vehicle behavior logic is defined in {@link #run(Process.ProcessContext)}.
     *
     * @param env simulation environment
     */
    public void startSimulation(Environment env) {
        this.process = env.process(this::run, id());
    }

    /**
     * Vehicle behavior logic to be implemented by subclasses.
     * <p>
     * This method defines the main simulation loop for the vehicle.
     * It will be executed as a JSimul Process.
     *
     * @param ctx process context
     * @return result value
     * @throws Exception if process fails
     */
    protected abstract Object run(Process.ProcessContext ctx) throws Exception;

    /**
     * Get the JSimul Process associated with this vehicle.
     *
     * @return process instance
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Check if vehicle is available for task assignment.
     *
     * @return true if vehicle is idle
     */
    public boolean isAvailable() {
        return state == VehicleState.IDLE;
    }

    /**
     * Create PositionUpdater that updates this vehicle's position.
     * <p>
     * This method is used to pass position update capability to TransportCapability.
     *
     * @return position updater for this vehicle
     */
    protected PositionUpdater createPositionUpdater() {
        return this;
    }

    /**
     * Update position (implements PositionUpdater interface).
     * <p>
     * This method is called by TransportCapability to update the vehicle's position.
     *
     * @param destination new position
     */
    @Override
    public void update(Position destination) {
        setPosition(destination);
    }

    /**
     * Get vehicle base priority.
     * <p>
     * Priority range: 1-10, where 10 is highest priority.
     * This is the base priority; effective priority may be adjusted
     * by aging rules in PriorityManager.
     *
     * @return base priority (1-10)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set vehicle base priority.
     * <p>
     * Priority range: 1-10, where 10 is highest priority.
     *
     * @param priority base priority (1-10)
     * @throws IllegalArgumentException if priority not in range 1-10
     */
    public void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 1 and 10, got: " + priority);
        }
        this.priority = priority;
    }

    /**
     * Get current planned path.
     * <p>
     * Returns the path as a string of node IDs. This is used for traffic control
     * and replanning purposes.
     *
     * @return current path string, or null if no path is set
     */
    public String getPath() {
        return currentPath;
    }

    /**
     * Set the planned path for this vehicle.
     * <p>
     * This is called after path planning or replanning. The path should be
     * a string representation of node IDs to traverse.
     *
     * @param path path string (node IDs concatenated)
     */
    public void setPath(String path) {
        this.currentPath = path;
    }

    /**
     * Get the transport type for this vehicle (REQ-TC-007.1).
     * <p>
     * Maps the vehicle's EntityType to the corresponding TransportType for
     * path planning and traffic control purposes.
     *
     * @return transport type (OHT, AGV, HUMAN, or CONVEYOR)
     * @throws IllegalArgumentException if vehicle type has no mapping
     */
    public TransportType getTransportType() {
        EntityType entityType = type();
        return switch (entityType) {
            case OHT_VEHICLE -> TransportType.OHT;
            case AGV_VEHICLE -> TransportType.AGV;
            case OPERATOR -> TransportType.HUMAN;
            case CONVEYOR -> TransportType.CONVEYOR;
            default -> throw new IllegalArgumentException(
                    "No TransportType mapping for EntityType: " + entityType);
        };
    }

    /**
     * Get the destination node ID for replanning (REQ-TC-007).
     * <p>
     * This is used by TrafficManager to trigger replanning when a vehicle is blocked.
     *
     * @return destination node ID, or null if not set
     */
    public String getDestinationNodeId() {
        return destinationNodeId;
    }

    /**
     * Set the destination node ID for replanning (REQ-TC-007).
     * <p>
     * This should be set when a vehicle is assigned a task or route.
     *
     * @param destinationNodeId destination node ID
     */
    public void setDestinationNodeId(String destinationNodeId) {
        this.destinationNodeId = destinationNodeId;
    }

    /**
     * Get the current node ID for replanning (REQ-TC-007).
     * <p>
     * This is used by TrafficManager to trigger replanning when a vehicle is blocked.
     *
     * @return current node ID, or null if not set
     */
    public String getCurrentNodeId() {
        return currentNodeId;
    }

    /**
     * Set the current node ID for replanning (REQ-TC-007).
     * <p>
     * This should be updated as the vehicle moves through the network.
     *
     * @param currentNodeId current node ID
     */
    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
}
