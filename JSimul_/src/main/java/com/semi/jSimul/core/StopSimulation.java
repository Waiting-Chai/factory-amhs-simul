package com.semi.jSimul.core;

/**
 * Exception used internally to stop Environment.run at an until event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class StopSimulation extends RuntimeException {

    private final Object value;

    public StopSimulation(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    public static void callback(Event event) {
        if (event.ok()) {
            throw new StopSimulation(event.value());
        } else {
            Throwable t = (Throwable) event.value();
            if (t instanceof RuntimeException rt) {
                throw rt;
            }
            RuntimeException wrapped = new RuntimeException(t.getMessage(), t);
            throw wrapped;
        }
    }

}
