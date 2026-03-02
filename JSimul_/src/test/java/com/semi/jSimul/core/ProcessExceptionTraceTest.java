package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for process exception propagation and interrupt handling.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ProcessExceptionTraceTest {

    @Test
    void interruptBeforeAwaitFailsProcess() {
        Environment env = new Environment();
        Process proc =
                env.process(
                        ctx -> {
                            ctx.await(ctx.env().timeout(10.0));
                            return "done";
                        });

        proc.interrupt("boom");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> env.run(proc));
        assertTrue(ex instanceof Interrupt || ex.getCause() instanceof Interrupt);
    }

    @Test
    void processExceptionStripsSelfCause() {
        Environment env = new Environment();
        Process proc =
                env.process(
                        ctx -> {
                            throw new RuntimeException("fail");
                        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> env.run(proc));
        // cause should be null or not self-causation
        assertNotSame(ex.getCause(), ex);
    }
}
