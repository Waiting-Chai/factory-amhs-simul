package com.semi.simlogistics.web.infrastructure.cache;

import com.semi.simlogistics.control.port.cache.SessionCachePort;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis adapter for session cache.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class RedisSessionCacheAdapter implements SessionCachePort {

    private static final String MAP_NAME = "cache:session";
    private final RMapCache<String, String> cache;

    public RedisSessionCacheAdapter(RedissonClient redissonClient) {
        this.cache = redissonClient.getMapCache(MAP_NAME);
    }

    @Override
    public Optional<String> getSession(String simulationId, String clientId) {
        return Optional.ofNullable(cache.get(buildKey(simulationId, clientId)));
    }

    @Override
    public void putSession(String simulationId, String clientId, String payload, Duration ttl) {
        cache.fastPut(buildKey(simulationId, clientId), payload, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    private String buildKey(String simulationId, String clientId) {
        return "session:" + simulationId + ":" + clientId;
    }
}
