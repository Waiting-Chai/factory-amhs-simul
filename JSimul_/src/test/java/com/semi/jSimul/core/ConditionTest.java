package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for Condition functionality.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class ConditionTest {
    @Test
    void allOfTriggersWhenAllProcessed() {
        Environment env = new Environment();
        Timeout a = new Timeout(env, 2, "A");
        Timeout b = new Timeout(env, 1, "B");
        Event all = new AllOf(env, a, b).asEvent();
        env.run(all);
        assertTrue(all.ok());
        ConditionValue cv = (ConditionValue) all.value();
        assertEquals(2, cv.toMap().size());
        assertTrue(cv.contains(a.asEvent()));
        assertTrue(cv.contains(b.asEvent()));
    }

    @Test
    void anyOfTriggersWhenAnyProcessed() {
        Environment env = new Environment();
        Timeout a = new Timeout(env, 5, "A");
        Timeout b = new Timeout(env, 1, "B");
        Event any = new AnyOf(env, a, b).asEvent();
        env.run(any);
        assertTrue(any.ok());
        ConditionValue cv = (ConditionValue) any.value();
        assertEquals(1, cv.toMap().size());
        assertTrue(cv.contains(b.asEvent()));
    }
}