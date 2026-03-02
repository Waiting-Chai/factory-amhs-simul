package com.semi.simlogistics.control.port.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Port for session cache access.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface SessionCachePort {

    /**
     * Get session payload.
     *
     * @param simulationId simulation ID
     * @param clientId client ID
     * @return optional payload
     */
    Optional<String> getSession(String simulationId, String clientId);

    /**
     * Cache session payload.
     *
     * @param simulationId simulation ID
     * @param clientId client ID
     * @param payload session payload
     * @param ttl ttl duration
     */
    void putSession(String simulationId, String clientId, String payload, Duration ttl);
}
