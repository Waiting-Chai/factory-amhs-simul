package com.semi.jSimul.core;

/**
 * Internal control-flow exception used to emulate SimPy's {@code env.exit()}
 * semantics. Throwing this from user code inside a {@link Process} causes the
 * process to terminate successfully with the provided value.
 *
 * @author waiting
 * @date 2025/11/05
 */
final class ProcessExit extends RuntimeException {

    private final Object value;

    ProcessExit(Object value) {
        super("ProcessExit");
        this.value = value;
    }

    Object value() {
        return value;
    }

}
