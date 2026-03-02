package com.semi.simlogistics.core;

/**
 * Entity type for logistics simulation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public enum EntityType {
    /**
     * Processing machine (e.g., lithography, etching).
     */
    MACHINE,

    /**
     * Automated Storage and Retrieval System (ASRS).
     */
    STOCKER,

    /**
     * Equipment rack / buffer storage.
     */
    ERACK,

    /**
     * Manual workstation for operator operations.
     */
    MANUAL_STATION,

    /**
     * Conveyor system for material transport.
     */
    CONVEYOR,

    /**
     * Overhead Hoist Transport vehicle.
     */
    OHT_VEHICLE,

    /**
     * Automated Guided Vehicle.
     */
    AGV_VEHICLE,

    /**
     * Human operator for manual tasks.
     */
    OPERATOR,

    /**
     * Safety zone for human-vehicle separation.
     */
    SAFETY_ZONE,

    /**
     * Traffic control point on path.
     */
    CONTROL_POINT,

    /**
     * Traffic control area.
     */
    CONTROL_AREA
}
