package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for Process functionality.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class ProcessTest {
    @Test
    void processAwaitTimeout() {
        Environment env = new Environment();
        Process p = new Process(env, ctx -> ctx.await(new Timeout(ctx.env(), 2, "done")));
        Object ret = env.run(p);
        assertEquals("done", ret);
        assertFalse(p.isAlive());
    }

    @Test
    void processInterrupt() {
        Environment env = new Environment();
        Process p = new Process(env, ctx -> {
            try {
                ctx.await(new Timeout(ctx.env(), 10, null));
                return "ok";
            } catch (Interrupt ex) {
                return "interrupted:" + ex.cause();
            }
        });
        // schedule interrupt at time 1
        new Timeout(env, 1).addCallback(ev -> p.interrupt("preempt"));
        Object ret = env.run(p);
        assertEquals("interrupted:preempt", ret);
    }

    @Test
    void selfInterruptIsRejected() {
        Environment env = new Environment();
        Process p =
                env.process(
                        ctx -> {
                            ctx.env().activeProcess().interrupt("self");
                            return "unreachable";
                        });

        assertThrows(IllegalStateException.class, () -> env.run(p));
    }

    @Test
    void interruptingDeadProcessFails() {
        Environment env = new Environment();
        Process p = env.process(ctx -> "done");
        assertEquals("done", env.run(p));
        assertThrows(IllegalStateException.class, () -> p.interrupt("late"));
    }
}
