package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for Environment functionality.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class EnvironmentTest {
    @Test
    void timeoutAdvancesTimeInOrder() {
        Environment env = new Environment();
        Timeout t1 = new Timeout(env, 5);
        Timeout t2 = new Timeout(env, 1);
        
        // t2 trigger (at t=1.0)
        env.step(); 
        // t2 inner scheduled at t=1.0
        env.step(); 
        
        assertEquals(1.0, env.now());
        
        // t1 trigger (at t=5.0)
        env.step();
        // t1 inner scheduled at t=5.0
        env.step();
        
        assertEquals(5.0, env.now());
    }

    @Test
    void runUntilNumberStops() {
        Environment env = new Environment();
        new Timeout(env, 3);
        Object ret = env.run(3);
        assertNull(ret);
        assertEquals(3.0, env.now());
    }

    @Test
    void failedEventCrashesUnlessDefused() {
        Environment env = new Environment();
        Event e = new Event(env);
        e.fail(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, env::step);

        Environment env2 = new Environment();
        Event e2 = new Event(env2);
        e2.addCallback(ev -> ev.setDefused(true));
        e2.fail(new RuntimeException("boom"));
        assertDoesNotThrow(env2::step);
    }

    @Test
    void runUntilEventWithoutScheduleThrows() {
        Environment env = new Environment();
        Event until = env.event();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> env.run(until));
        assertTrue(ex.getMessage().contains("No scheduled events left"));
        assertEquals(0.0, env.now());
    }
}
