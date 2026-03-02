package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test class for callback functionality in compositional events.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class CallbackTest {

    @Test
    void timeoutAddCallbackWorks() {
        Environment env = new Environment();
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        AtomicReference<String> callbackValue = new AtomicReference<>();

        // Create a timeout with a callback
        Timeout timeout = new Timeout(env, 1, "test_value");
        timeout.asEvent().addCallback(event -> {
            callbackInvoked.set(true);
            callbackValue.set((String) event.value());
        });

        // Run the simulation
        env.run(timeout.asEvent());

        // Verify callback was invoked
        assertTrue(callbackInvoked.get());
        assertEquals("test_value", callbackValue.get());
    }

    @Test
    void multipleCallbacksWork() {
        Environment env = new Environment();
        AtomicBoolean callback1Invoked = new AtomicBoolean(false);
        AtomicBoolean callback2Invoked = new AtomicBoolean(false);

        // Create a timeout with multiple callbacks
        Timeout timeout = new Timeout(env, 1, "test");
        timeout.asEvent().addCallback(event -> callback1Invoked.set(true));
        timeout.asEvent().addCallback(event -> callback2Invoked.set(true));

        // Run the simulation
        env.run(timeout.asEvent());

        // Verify both callbacks were invoked
        assertTrue(callback1Invoked.get());
        assertTrue(callback2Invoked.get());
    }
}