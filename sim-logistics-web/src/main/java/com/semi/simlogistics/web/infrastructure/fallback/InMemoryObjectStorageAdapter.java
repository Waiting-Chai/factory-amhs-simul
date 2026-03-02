package com.semi.simlogistics.web.infrastructure.fallback;

import com.semi.simlogistics.control.port.storage.ObjectMetadata;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.infrastructure.storage.ObjectStorageCompensationPort;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fallback adapter for object storage.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class InMemoryObjectStorageAdapter implements ObjectStoragePort, ObjectStorageCompensationPort {

    private final Map<String, ObjectMetadata> metadataStore = new ConcurrentHashMap<>();

    @Override
    public ObjectMetadata putObject(String bucket, String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            while (inputStream.read() != -1) {
                // Consume stream to simulate upload side effect in fallback mode.
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read input stream", e);
        }
        ObjectMetadata metadata = new ObjectMetadata(
            bucket, objectKey, size, contentType, "inmemory-etag", Instant.now()
        );
        metadataStore.put(buildKey(bucket, objectKey), metadata);
        return metadata;
    }

    @Override
    public Optional<ObjectMetadata> statObject(String bucket, String objectKey) {
        return Optional.ofNullable(metadataStore.get(buildKey(bucket, objectKey)));
    }

    @Override
    public InputStream getObject(String bucket, String objectKey) {
        // Fallback adapter does not store actual content, return null to trigger 404
        return null;
    }

    @Override
    public void deleteObject(String bucket, String objectKey) {
        metadataStore.remove(buildKey(bucket, objectKey));
    }

    private String buildKey(String bucket, String objectKey) {
        return bucket + "/" + objectKey;
    }
}
