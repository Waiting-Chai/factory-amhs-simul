package com.semi.simlogistics.web.infrastructure.storage;

/**
 * Compensation contract for object storage cleanup.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public interface ObjectStorageCompensationPort {

    /**
     * Delete object from storage as compensation.
     *
     * @param bucket bucket name
     * @param objectKey object key
     */
    void deleteObject(String bucket, String objectKey);
}
