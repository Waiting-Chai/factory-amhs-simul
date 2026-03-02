package com.semi.simlogistics.web.exception;

import com.semi.simlogistics.web.common.ErrorCode;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String id) {
        super(ErrorCode.NOT_FOUND, String.format("%s not found: %s", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
