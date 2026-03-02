package com.semi.simlogistics.web.infrastructure;

import com.semi.simlogistics.control.port.cache.PathCachePort;
import com.semi.simlogistics.control.port.cache.SessionCachePort;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for adapter fallback strategy.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
class InfrastructureAdapterFactoryTest {

    @Test
    void testFallbackAdapters_WhenInfrastructureClientsAreNull() {
        PathCachePort pathCache = InfrastructureAdapterFactory.createPathCachePort(null);
        SessionCachePort sessionCache = InfrastructureAdapterFactory.createSessionCachePort(null);
        ObjectStoragePort storage = InfrastructureAdapterFactory.createObjectStoragePort(null, "sim-artifacts");

        pathCache.putPath("A", "B", "OHT", "A->B", Duration.ofMinutes(3));
        sessionCache.putSession("SIM-1", "C-1", "connected", Duration.ofMinutes(3));
        byte[] payload = "data".getBytes(StandardCharsets.UTF_8);
        storage.putObject("reports", "run/result.txt", new ByteArrayInputStream(payload), payload.length, "text/plain");

        assertThat(pathCache.getPath("A", "B", "OHT")).contains("A->B");
        assertThat(sessionCache.getSession("SIM-1", "C-1")).contains("connected");
        assertThat(storage.statObject("reports", "run/result.txt")).isPresent();
    }
}
