package com.semi.simlogistics.web.infrastructure.storage;

import com.semi.simlogistics.control.port.storage.ObjectMetadata;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.common.ErrorCode;
import com.semi.simlogistics.web.exception.ObjectStorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.GetObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MinIO adapter for object storage.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class MinioObjectStorageAdapter implements ObjectStoragePort, ObjectStorageCompensationPort {
    private static final Logger logger = LoggerFactory.getLogger(MinioObjectStorageAdapter.class);
    private static final Map<String, Object> BUCKET_LOCKS = new ConcurrentHashMap<>();

    private final MinioClient minioClient;
    private final String defaultBucket;

    public MinioObjectStorageAdapter(MinioClient minioClient, String defaultBucket) {
        this.minioClient = minioClient;
        this.defaultBucket = defaultBucket;
    }

    @Override
    public ObjectMetadata putObject(String bucket, String objectKey, InputStream inputStream, long size, String contentType) {
        String targetBucket = normalizeBucket(bucket);
        String traceId = MDC.get("traceId");
        try {
            ensureBucketExists(targetBucket);
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );
            logger.info("Object uploaded to MinIO: bucket={}, objectKey={}, size={}, traceId={}",
                    targetBucket, objectKey, size, traceId);
            return statObject(targetBucket, objectKey)
                    .orElseThrow(() -> new ObjectStorageException(
                            ErrorCode.OBJECT_STORAGE_ERROR,
                            "Object uploaded but metadata not found: bucket=" + targetBucket + ", key=" + objectKey,
                            null
                    ));
        } catch (ObjectStorageException e) {
            throw e;
        } catch (Exception e) {
            throw toObjectStorageException(e, targetBucket, objectKey);
        }
    }

    @Override
    public Optional<ObjectMetadata> statObject(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
            return Optional.of(new ObjectMetadata(
                bucket,
                objectKey,
                stat.size(),
                stat.contentType(),
                stat.etag(),
                stat.lastModified().toInstant()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public InputStream getObject(String bucket, String objectKey) {
        String targetBucket = normalizeBucket(bucket);
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            logger.error("Failed to get object from MinIO: bucket={}, key={}, traceId={}",
                    targetBucket, objectKey, MDC.get("traceId"), e);
            return null;
        }
    }

    @Override
    public void deleteObject(String bucket, String objectKey) {
        String targetBucket = normalizeBucket(bucket);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(targetBucket).object(objectKey).build()
            );
            logger.info("Object deleted from MinIO: bucket={}, objectKey={}, traceId={}",
                    targetBucket, objectKey, MDC.get("traceId"));
        } catch (Exception e) {
            throw toObjectStorageException(e, targetBucket, objectKey);
        }
    }

    public void ensureBucketExists(String bucket) {
        String targetBucket = normalizeBucket(bucket);
        Object lock = BUCKET_LOCKS.computeIfAbsent(targetBucket, ignored -> new Object());
        synchronized (lock) {
            try {
                if (minioClient.bucketExists(BucketExistsArgs.builder().bucket(targetBucket).build())) {
                    return;
                }
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(targetBucket).build());
                logger.info("MinIO bucket created: bucket={}, traceId={}", targetBucket, MDC.get("traceId"));
            } catch (ErrorResponseException e) {
                String code = e.errorResponse() != null ? e.errorResponse().code() : "";
                if ("BucketAlreadyOwnedByYou".equals(code) || "BucketAlreadyExists".equals(code)) {
                    logger.info("MinIO bucket already exists after concurrent create: bucket={}, traceId={}",
                            targetBucket, MDC.get("traceId"));
                    return;
                }
                throw toObjectStorageException(e, targetBucket, null);
            } catch (Exception e) {
                throw toObjectStorageException(e, targetBucket, null);
            }
        }
    }

    private String normalizeBucket(String bucket) {
        if (bucket != null && !bucket.isBlank()) {
            return bucket;
        }
        if (defaultBucket == null || defaultBucket.isBlank()) {
            throw new ObjectStorageException(
                    ErrorCode.OBJECT_STORAGE_ERROR,
                    "Object storage bucket is not configured",
                    null
            );
        }
        return defaultBucket;
    }

    private ObjectStorageException toObjectStorageException(Exception e, String bucket, String objectKey) {
        if (e instanceof ObjectStorageException storageException) {
            return storageException;
        }

        if (e instanceof ErrorResponseException errorResponseException) {
            String code = errorResponseException.errorResponse() != null ? errorResponseException.errorResponse().code() : "";
            if ("NoSuchBucket".equals(code)) {
                return new ObjectStorageException(
                        ErrorCode.BUCKET_NOT_FOUND,
                        "Object storage bucket not found: " + bucket,
                        e
                );
            }
            return new ObjectStorageException(
                    ErrorCode.OBJECT_STORAGE_ERROR,
                    "Object storage operation failed: code=" + code + ", bucket=" + bucket + ", key=" + objectKey,
                    e
            );
        }

        if (e instanceof MinioException) {
            return new ObjectStorageException(
                    ErrorCode.OBJECT_STORAGE_ERROR,
                    "Object storage service unavailable: bucket=" + bucket + ", key=" + objectKey,
                    e
            );
        }

        return new ObjectStorageException(
                ErrorCode.OBJECT_STORAGE_ERROR,
                "Object storage operation failed: bucket=" + bucket + ", key=" + objectKey,
                e
        );
    }
}
