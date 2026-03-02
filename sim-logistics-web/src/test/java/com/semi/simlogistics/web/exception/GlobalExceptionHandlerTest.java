package com.semi.simlogistics.web.exception;

import com.semi.simlogistics.web.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for global exception handler.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturnServiceUnavailableForObjectStorageError() {
        ObjectStorageException ex = new ObjectStorageException(
                ErrorCode.OBJECT_STORAGE_ERROR,
                "Object storage service unavailable: bucket=sim-artifacts, key=models/a.glb",
                new RuntimeException("connect failed")
        );

        ResponseEntity<?> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("code", "OBJECT_STORAGE_ERROR");
    }

    @Test
    void shouldReturnBadRequestForDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("fk_model_versions_model");

        ResponseEntity<?> response = handler.handleDataIntegrityViolationException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("code", "CONSTRAINT_VIOLATION");
    }
}
