package com.semi.simlogistics.web.exception;

import com.semi.simlogistics.web.common.ErrorCode;

/**
 * Exception for object storage related failures.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class ObjectStorageException extends BusinessException {

    public ObjectStorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
