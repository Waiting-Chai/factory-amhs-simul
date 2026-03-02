package com.semi.simlogistics.control.port.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Port for path cache access.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface PathCachePort {

    /**
     * Get cached path.
     *
     * @param from start node
     * @param to end node
     * @param vehicleType vehicle type
     * @return optional path value
     */
    Optional<String> getPath(String from, String to, String vehicleType);

    /**
     * Put cached path.
     *
     * @param from start node
     * @param to end node
     * @param vehicleType vehicle type
     * @param pathValue path payload
     * @param ttl ttl duration
     */
    void putPath(String from, String to, String vehicleType, String pathValue, Duration ttl);
}
