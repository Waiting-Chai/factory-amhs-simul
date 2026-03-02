package com.semi.simlogistics.control.port.storage;

import java.io.InputStream;
import java.util.Optional;

/**
 * Port for object storage access.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface ObjectStoragePort {

    /**
     * Put object to storage.
     *
     * @param bucket bucket name
     * @param objectKey object key
     * @param inputStream object payload stream
     * @param size object size
     * @param contentType object content type
     * @return persisted metadata
     */
    ObjectMetadata putObject(String bucket, String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * Query object metadata.
     *
     * @param bucket bucket name
     * @param objectKey object key
     * @return optional metadata
     */
    Optional<ObjectMetadata> statObject(String bucket, String objectKey);

    /**
     * Get object content stream from storage.
     *
     * @param bucket bucket name
     * @param objectKey object key
     * @return object input stream, or null if not found
     */
    InputStream getObject(String bucket, String objectKey);

}
