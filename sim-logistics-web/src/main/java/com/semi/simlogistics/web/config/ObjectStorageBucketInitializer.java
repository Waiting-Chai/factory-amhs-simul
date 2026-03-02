package com.semi.simlogistics.web.config;

import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.infrastructure.storage.MinioObjectStorageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize object storage bucket on application startup.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@Component
public class ObjectStorageBucketInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageBucketInitializer.class);

    private final ObjectStoragePort objectStoragePort;
    private final MinioProperties minioProperties;

    public ObjectStorageBucketInitializer(ObjectStoragePort objectStoragePort, MinioProperties minioProperties) {
        this.objectStoragePort = objectStoragePort;
        this.minioProperties = minioProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String bucket = minioProperties.getDefaultBucket();
        if (objectStoragePort instanceof MinioObjectStorageAdapter adapter) {
            adapter.ensureBucketExists(bucket);
            logger.info("MinIO bucket ready: {}", bucket);
        } else {
            logger.info("Object storage bucket initialization skipped (non-MinIO adapter)");
        }
    }
}
