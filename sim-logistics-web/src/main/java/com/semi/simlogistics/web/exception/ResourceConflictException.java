package com.semi.simlogistics.web.exception;

import com.semi.simlogistics.web.common.ErrorCode;

/**
 * Exception thrown when a resource conflicts with existing state.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ResourceConflictException extends BusinessException {

    public ResourceConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
