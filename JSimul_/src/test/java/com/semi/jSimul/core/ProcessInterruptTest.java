package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests around interrupt callback cleanup and defused failure semantics.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ProcessInterruptTest {

    @Test
    void interruptRemovesResumeCallback() {
        Environment env = new Environment();
        Process proc =
                env.process(
                        ctx -> {
                            // await a long timeout; will be interrupted
                            ctx.await(ctx.env().timeout(10));
                            return "done";
                        });

        // interrupt at time 0 before timeout fires
        proc.interrupt("stop");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> env.run(proc));
        assertTrue(ex.getCause() instanceof Interrupt || ex instanceof Interrupt);

        // After interrupt, the original timeout should not resume the process
        // If callbacks were left attached, the timeout would attempt a second resume.
        assertThrows(RuntimeException.class, env::step); // processing timeout should surface interrupt failure
    }

    @Test
    void failedEventDefusedByCallbackPreventsCrash() {
        Environment env = new Environment();
        Event failing = env.event();
        failing.addCallback(ev -> ev.setDefused(true));
        failing.fail(new RuntimeException("boom"));
        Process proc =
                env.process(
                        ctx -> {
                            try {
                                ctx.await(failing);
                                return "ok";
                            } catch (Exception ex) {
                                return ex.getClass().getSimpleName();
                            }
                        });

        Object result = env.run(proc);
        assertEquals("RuntimeException", result);
    }
}
