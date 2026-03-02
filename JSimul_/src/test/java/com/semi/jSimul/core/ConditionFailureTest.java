package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Coverage for mixed success/failure and defuse behaviours of conditions.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ConditionFailureTest {

    @Test
    void conditionFailsWhenOperandFails() {
        Environment env = new Environment();
        Event ok = env.event().succeed("ok");
        Event fail = env.event().fail(new RuntimeException("boom"));

        SimEvent all = env.allOf(ok, fail);
        // failing operand defused by Condition should not crash but mark ok=false
        env.step(); // process failing
        env.step(); // process condition
        assertFalse(all.asEvent().ok());
    }

    @Test
    void defusedConditionDoesNotCrashEnvironmentStep() {
        Environment env = new Environment();
        Event fail = env.event().fail(new RuntimeException("boom"));
        SimEvent any = env.anyOf(fail);
        any.asEvent().addCallback(ev -> ev.setDefused(true));

        // process failing operand
        env.step();
        // process condition; defused callback should prevent crash
        assertDoesNotThrow(env::step);
        assertFalse(any.asEvent().ok());
        assertTrue(any.asEvent().isDefused());
    }

    @Test
    void nestedConditionPropagatesFailure() {
        Environment env = new Environment();
        Event fail = env.event().fail(new RuntimeException("inner"));
        Timeout ok = env.timeout(1.0, "ok");

        SimEvent inner = env.anyOf(fail, ok);
        SimEvent outer = env.allOf(ok, inner);

        assertThrows(RuntimeException.class, () -> env.run(outer.asEvent()));
    }
}
