package com.semi.simlogistics.web.exception;

import com.semi.simlogistics.web.common.ApiEnvelope;
import com.semi.simlogistics.web.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for REST API.
 * Converts exceptions to standardized API responses.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private String maxFileSize = "100MB";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleBusinessException(BusinessException ex) {
        logger.error("Business exception: {}", ex.getMessage(), ex);
        ErrorCode errorCode = ex.getErrorCode();
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? errorCode.getMessage()
                : ex.getMessage();
        return ResponseEntity
                .status(getHttpStatus(errorCode))
                .body(ApiEnvelope.error(errorCode.getCode(), message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Illegal argument exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiEnvelope.error(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        logger.warn("File upload exceeds maximum size: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiEnvelope.error(
                        ErrorCode.FILE_TOO_LARGE.getCode(),
                        "Uploaded file exceeds maximum allowed size: " + maxFileSize
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation: {}", ex.getMessage(), ex);
        String rootMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiEnvelope.error(
                        ErrorCode.CONSTRAINT_VIOLATION.getCode(),
                        "Database constraint violation: " + rootMessage
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope<Void>> handleException(Exception ex) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiEnvelope.error(ErrorCode.INTERNAL_ERROR));
    }

    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        switch (errorCode) {
            case NOT_FOUND:
            case SCENE_NOT_FOUND:
            case SCENE_DRAFT_NOT_FOUND:
                return HttpStatus.NOT_FOUND;
            case CONFLICT:
            case SCENE_NAME_EXISTS:
                return HttpStatus.CONFLICT;
            case BAD_REQUEST:
            case INVALID_ENTITY_TYPE:
            case INVALID_PATH_TYPE:
            case INVALID_COORDINATES:
            case MISSING_REQUIRED_FIELD:
            case INVALID_IMPORT_FORMAT:
            case IMPORT_VALIDATION_FAILED:
            case CONSTRAINT_VIOLATION:
                return HttpStatus.BAD_REQUEST;
            case OBJECT_STORAGE_ERROR:
            case BUCKET_NOT_FOUND:
                return HttpStatus.SERVICE_UNAVAILABLE;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
