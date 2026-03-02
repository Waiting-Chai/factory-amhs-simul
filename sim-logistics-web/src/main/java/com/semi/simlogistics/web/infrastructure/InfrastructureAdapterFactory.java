package com.semi.simlogistics.web.infrastructure;

import com.semi.simlogistics.control.port.cache.PathCachePort;
import com.semi.simlogistics.control.port.cache.SessionCachePort;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.infrastructure.cache.RedisPathCacheAdapter;
import com.semi.simlogistics.web.infrastructure.cache.RedisSessionCacheAdapter;
import com.semi.simlogistics.web.infrastructure.fallback.InMemoryObjectStorageAdapter;
import com.semi.simlogistics.web.infrastructure.fallback.InMemoryPathCacheAdapter;
import com.semi.simlogistics.web.infrastructure.fallback.InMemorySessionCacheAdapter;
import com.semi.simlogistics.web.infrastructure.storage.MinioObjectStorageAdapter;
import io.minio.MinioClient;
import org.redisson.api.RedissonClient;

/**
 * Factory for adapter assembly with in-memory fallback.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public final class InfrastructureAdapterFactory {

    private InfrastructureAdapterFactory() {
    }

    public static PathCachePort createPathCachePort(RedissonClient redissonClient) {
        if (redissonClient != null) {
            return new RedisPathCacheAdapter(redissonClient);
        }
        return new InMemoryPathCacheAdapter();
    }

    public static SessionCachePort createSessionCachePort(RedissonClient redissonClient) {
        if (redissonClient != null) {
            return new RedisSessionCacheAdapter(redissonClient);
        }
        return new InMemorySessionCacheAdapter();
    }

    public static ObjectStoragePort createObjectStoragePort(MinioClient minioClient, String defaultBucket) {
        if (minioClient != null) {
            return new MinioObjectStorageAdapter(minioClient, defaultBucket);
        }
        return new InMemoryObjectStorageAdapter();
    }
}
