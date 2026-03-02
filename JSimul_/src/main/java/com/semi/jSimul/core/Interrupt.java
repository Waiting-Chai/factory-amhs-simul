package com.semi.jSimul.core;

/**
 * Interrupt exception thrown into a process when interrupted.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Interrupt extends RuntimeException {

    private final Object cause;

    public Interrupt(Object cause) {
        super("Interrupt(" + cause + ")");
        this.cause = cause;
    }

    public Object cause() {
        return cause;
    }

}