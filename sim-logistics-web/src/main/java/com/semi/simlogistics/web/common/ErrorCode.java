package com.semi.simlogistics.web.common;

/**
 * Error code enumeration for API responses.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public enum ErrorCode {

    // Success
    SUCCESS("SUCCESS", "Operation successful"),

    // Client errors (4xx)
    BAD_REQUEST("BAD_REQUEST", "Invalid request parameters"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    CONFLICT("CONFLICT", "Resource already exists or conflicts with existing state"),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required"),
    FORBIDDEN("FORBIDDEN", "Access denied"),

    // Scene specific errors
    SCENE_NOT_FOUND("SCENE_NOT_FOUND", "Scene not found"),
    SCENE_NAME_EXISTS("SCENE_NAME_EXISTS", "Scene name already exists"),
    SCENE_INVALID_DATA("SCENE_INVALID_DATA", "Invalid scene data"),
    SCENE_DRAFT_NOT_FOUND("SCENE_DRAFT_NOT_FOUND", "Scene draft not found"),

    // Model specific errors
    MODEL_NOT_FOUND("MODEL_NOT_FOUND", "Model not found"),
    MODEL_NAME_EXISTS("MODEL_NAME_EXISTS", "Model name already exists"),
    MODEL_INVALID_DATA("MODEL_INVALID_DATA", "Invalid model data"),
    MODEL_VERSION_NOT_FOUND("MODEL_VERSION_NOT_FOUND", "Model version not found"),
    MODEL_FILE_INVALID("MODEL_FILE_INVALID", "Invalid model file"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "Uploaded file exceeds maximum allowed size"),
    MODEL_UPLOAD_FAILED("MODEL_UPLOAD_FAILED", "Failed to upload model file"),
    OBJECT_STORAGE_ERROR("OBJECT_STORAGE_ERROR", "Object storage service is unavailable"),
    BUCKET_NOT_FOUND("BUCKET_NOT_FOUND", "Object storage bucket is missing"),

    // Validation errors
    INVALID_ENTITY_TYPE("INVALID_ENTITY_TYPE", "Invalid entity type"),
    INVALID_PATH_TYPE("INVALID_PATH_TYPE", "Invalid path type"),
    INVALID_COORDINATES("INVALID_COORDINATES", "Invalid coordinates"),
    MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD", "Missing required field"),
    CONSTRAINT_VIOLATION("CONSTRAINT_VIOLATION", "Database constraint violation"),

    // Import/Export errors
    INVALID_IMPORT_FORMAT("INVALID_IMPORT_FORMAT", "Invalid import file format"),
    IMPORT_VALIDATION_FAILED("IMPORT_VALIDATION_FAILED", "Import file validation failed"),
    EXPORT_FAILED("EXPORT_FAILED", "Failed to export scene"),

    // Server errors (5xx)
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service temporarily unavailable");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
