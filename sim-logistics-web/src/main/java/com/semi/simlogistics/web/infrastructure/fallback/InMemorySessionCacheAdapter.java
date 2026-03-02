package com.semi.simlogistics.web.infrastructure.fallback;

import com.semi.simlogistics.control.port.cache.SessionCachePort;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fallback adapter for session cache.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class InMemorySessionCacheAdapter implements SessionCachePort {

    private final Map<String, TimedValue> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> getSession(String simulationId, String clientId) {
        String key = buildKey(simulationId, clientId);
        TimedValue value = store.get(key);
        if (value == null || value.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(value.payload());
    }

    @Override
    public void putSession(String simulationId, String clientId, String payload, Duration ttl) {
        store.put(buildKey(simulationId, clientId), new TimedValue(payload, Instant.now().plus(ttl)));
    }

    private String buildKey(String simulationId, String clientId) {
        return "session:" + simulationId + ":" + clientId;
    }

    private record TimedValue(String payload, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
