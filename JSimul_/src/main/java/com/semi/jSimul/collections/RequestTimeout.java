package com.semi.jSimul.collections;

/**
 * Exception used when a resource request times out before being granted.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class RequestTimeout extends RuntimeException {
    public RequestTimeout(String msg) {
        super(msg);
    }
}
