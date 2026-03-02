package com.semi.simlogistics.web.infrastructure.cache;

import com.semi.simlogistics.control.port.cache.PathCachePort;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis adapter for path cache.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class RedisPathCacheAdapter implements PathCachePort {

    private static final String MAP_NAME = "cache:path";
    private final RMapCache<String, String> cache;

    public RedisPathCacheAdapter(RedissonClient redissonClient) {
        this.cache = redissonClient.getMapCache(MAP_NAME);
    }

    @Override
    public Optional<String> getPath(String from, String to, String vehicleType) {
        return Optional.ofNullable(cache.get(buildKey(from, to, vehicleType)));
    }

    @Override
    public void putPath(String from, String to, String vehicleType, String pathValue, Duration ttl) {
        cache.fastPut(buildKey(from, to, vehicleType), pathValue, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    private String buildKey(String from, String to, String vehicleType) {
        return "path:" + from + ":" + to + ":" + vehicleType;
    }
}
