package com.semi.simlogistics.core;

/**
 * Transport types supported by logistics entities.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public enum TransportType {
    /**
     * Overhead Hoist Transport (天车).
     */
    OHT,

    /**
     * Automated Guided Vehicle (自动导引车).
     */
    AGV,

    /**
     * Manual transport by operators (人工搬运).
     */
    HUMAN,

    /**
     * Conveyor system (输送线).
     */
    CONVEYOR
}
