package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Additional parity checks derived from SimPy's event unit tests to
 * validate callback handling, trigger propagation, and empty condition
 * semantics.
 */
class SimPyEventParityTest {

    @Test
    void removingCallbacksPreventsInvocation() {
        Environment env = new Environment();
        Event event = env.event();

        StringBuilder calls = new StringBuilder();
        Event.Callback keep = e -> calls.append("keep");
        Event.Callback remove = e -> calls.append("remove");

        event.addCallback(keep);
        event.addCallback(remove);
        event.removeCallback(remove);

        event.succeed("done");
        env.step();

        assertEquals("keep", calls.toString(), "Removed callback must not run");
    }

    @Test
    void callbacksAddedAfterSchedulingStillRun() {
        Environment env = new Environment();
        Event event = env.event();

        StringBuilder calls = new StringBuilder();
        event.succeed("payload");
        event.addCallback(e -> calls.append(e.value()));

        env.step();

        assertEquals("payload", calls.toString(), "Late-added callback should still fire");
        assertTrue(event.isProcessed(), "Event should be processed after step");
    }

    @Test
    void callbacksIgnoredAfterProcessing() {
        Environment env = new Environment();
        Event event = env.event().succeed("done");
        env.step();

        StringBuilder calls = new StringBuilder();
        event.addCallback(e -> calls.append("late"));

        // Run another harmless step to prove the callback will never execute
        env.schedule(env.event().markOk(null), Event.NORMAL, 0.0);
        env.step();

        assertEquals("", calls.toString(), "Callbacks added post-processing must be ignored");
    }

    @Test
    void triggerCopiesOutcomeFromOriginEvent() {
        Environment env = new Environment();
        Event origin = env.event().succeed("source");
        env.step();

        Event target = env.event();
        target.trigger(origin);

        env.step();

        assertTrue(target.ok(), "Target should mirror origin success");
        assertEquals("source", target.value(), "Target value should copy origin");
    }

    @Test
    void emptyConditionsCompleteImmediatelyWithEmptyValue() {
        Environment env = new Environment();

        SimEvent all = env.allOf();
        SimEvent any = env.anyOf();

        env.step();
        env.step();

        ConditionValue allValue = (ConditionValue) all.asEvent().value();
        ConditionValue anyValue = (ConditionValue) any.asEvent().value();

        assertTrue(all.asEvent().ok());
        assertTrue(any.asEvent().ok());
        assertTrue(allValue.toMap().isEmpty(), "AllOf([]) should carry empty mapping");
        assertTrue(anyValue.toMap().isEmpty(), "AnyOf([]) should carry empty mapping");
    }

    @Test
    void runReturnsValueWhenUntilAlreadyProcessed() {
        Environment env = new Environment();
        Timeout event = env.timeout(1.0, "result");

        env.run();
        Object value = env.run(event.asEvent());

        assertEquals("result", value, "run(until) should return processed event value");
        assertEquals(1.0, env.now(), 1e-9);
    }
}
