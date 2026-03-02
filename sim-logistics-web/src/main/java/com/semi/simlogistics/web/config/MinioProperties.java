package com.semi.simlogistics.web.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO client properties.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@ConfigurationProperties(prefix = "sim.minio")
public class MinioProperties {
    private static final Logger logger = LoggerFactory.getLogger(MinioProperties.class);

    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String defaultBucket = "sim-artifacts";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    @PostConstruct
    public void validate() {
        if (defaultBucket == null || defaultBucket.isBlank()) {
            logger.error("MinIO default bucket is empty. Please configure sim.minio.default-bucket.");
            throw new IllegalStateException("sim.minio.default-bucket must not be blank");
        }
    }
}
