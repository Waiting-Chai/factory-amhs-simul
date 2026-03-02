package com.semi.simlogistics.web.infrastructure.fallback;

import com.semi.simlogistics.control.port.cache.PathCachePort;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fallback adapter for path cache.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class InMemoryPathCacheAdapter implements PathCachePort {

    private final Map<String, TimedValue> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> getPath(String from, String to, String vehicleType) {
        String key = buildKey(from, to, vehicleType);
        TimedValue timedValue = store.get(key);
        if (timedValue == null || timedValue.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(timedValue.value());
    }

    @Override
    public void putPath(String from, String to, String vehicleType, String pathValue, Duration ttl) {
        store.put(buildKey(from, to, vehicleType), new TimedValue(pathValue, Instant.now().plus(ttl)));
    }

    private String buildKey(String from, String to, String vehicleType) {
        return "path:" + from + ":" + to + ":" + vehicleType;
    }

    private record TimedValue(String value, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
