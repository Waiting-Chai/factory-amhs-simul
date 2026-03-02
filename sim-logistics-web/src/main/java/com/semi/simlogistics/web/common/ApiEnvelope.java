package com.semi.simlogistics.web.common;

/**
 * Unified API response envelope.
 * Wraps all API responses with code, message, and data.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ApiEnvelope<T> {

    private String code;
    private String message;
    private T data;
    private String traceId;

    public ApiEnvelope() {
    }

    public ApiEnvelope(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>("SUCCESS", "Operation successful", data);
    }

    public static <T> ApiEnvelope<T> success(String message, T data) {
        return new ApiEnvelope<>("SUCCESS", message, data);
    }

    public static <T> ApiEnvelope<T> error(String code, String message) {
        return new ApiEnvelope<>(code, message, null);
    }

    public static <T> ApiEnvelope<T> error(ErrorCode errorCode) {
        return new ApiEnvelope<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "ApiEnvelope{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
