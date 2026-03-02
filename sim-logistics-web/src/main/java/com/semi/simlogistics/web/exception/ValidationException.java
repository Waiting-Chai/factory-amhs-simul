package com.semi.simlogistics.web.exception;

import com.semi.simlogistics.web.common.ErrorCode;

/**
 * Exception thrown when validation fails.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public ValidationException(String field, String reason) {
        super(ErrorCode.BAD_REQUEST, String.format("Validation failed for field '%s': %s", field, reason));
    }
}
