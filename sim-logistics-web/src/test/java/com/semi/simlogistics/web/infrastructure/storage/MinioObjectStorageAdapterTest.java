package com.semi.simlogistics.web.infrastructure.storage;

import com.semi.simlogistics.control.port.storage.ObjectMetadata;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MinIO object storage adapter.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@ExtendWith(MockitoExtension.class)
class MinioObjectStorageAdapterTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private StatObjectResponse statObjectResponse;

    private MinioObjectStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MinioObjectStorageAdapter(minioClient, "sim-artifacts");
    }

    @Test
    void shouldCreateBucketWhenBucketDoesNotExistThenUploadObject() throws Exception {
        byte[] payload = "glb".getBytes();
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(statObjectResponse);
        when(statObjectResponse.size()).thenReturn((long) payload.length);
        when(statObjectResponse.contentType()).thenReturn("model/gltf-binary");
        when(statObjectResponse.etag()).thenReturn("etag");
        when(statObjectResponse.lastModified()).thenReturn(ZonedDateTime.now());

        ObjectMetadata metadata = adapter.putObject(
                "sim-artifacts",
                "models/a.glb",
                new ByteArrayInputStream(payload),
                payload.length,
                "model/gltf-binary"
        );

        assertThat(metadata.bucket()).isEqualTo("sim-artifacts");
        assertThat(metadata.objectKey()).isEqualTo("models/a.glb");
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }
}
