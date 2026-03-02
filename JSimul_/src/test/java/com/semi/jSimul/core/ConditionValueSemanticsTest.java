package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

/**
 * Broader semantics coverage for ConditionValue and Condition failure/defuse.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ConditionValueSemanticsTest {

    @Test
    void getThrowsWhenEventMissing() {
        Environment env = new Environment();
        Timeout present = env.timeout(1.0, "yes");
        Timeout missing = env.timeout(2.0, "no");

        SimEvent only = env.allOf(present);
        env.run(only);

        ConditionValue cv = (ConditionValue) only.asEvent().value();
        assertThrows(NoSuchElementException.class, () -> cv.get(missing.asEvent()));
    }

    @Test
    void conditionFailurePropagatesDefuseFlag() {
        Environment env = new Environment();
        Event failing = env.event();
        failing.addCallback(ev -> ev.setDefused(true));
        failing.fail(new RuntimeException("boom"));

        SimEvent any = env.anyOf(failing);
        // first step processes failing event
        env.step();
        // second step processes condition; should be defused and not crash
        assertDoesNotThrow(env::step);
        assertTrue(any.asEvent().isDefused());
        assertFalse(any.asEvent().ok());
    }

    @Test
    void conditionFailureWithoutDefuseCrashes() {
        Environment env = new Environment();
        Event failing = env.event();
        failing.fail(new RuntimeException("boom"));
        SimEvent all = env.allOf(failing);

        assertDoesNotThrow(env::step); // Condition defuses failing operand
        assertFalse(all.asEvent().ok());
        assertTrue(all.asEvent().isDefused());
    }
}
