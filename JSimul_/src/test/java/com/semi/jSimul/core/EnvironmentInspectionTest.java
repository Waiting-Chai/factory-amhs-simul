package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for Environment inspection helpers.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class EnvironmentInspectionTest {

    @Test
    void scheduledCountReflectsQueuedEvents() {
        Environment env = new Environment();
        env.timeout(1.0);
        env.timeout(2.0);

        // With the fix for Timeout using internal trigger events, 
        // each timeout adds 2 events (internal + trigger). 
        // BUT the internal event is added to queue via schedule(), and trigger is ALSO scheduled.
        // Actually, Timeout constructor:
        // this.inner = new Event(env); -> no schedule
        // trigger = new Event(env); -> schedule(trigger, delay)
        // So only 1 event per timeout is in the queue.
        // Wait, why did it fail with expected 1 but was 2?
        // Ah, because in the test:
        // env.timeout(1.0); -> 1 event
        // env.timeout(2.0); -> 1 event
        // Total 2.
        // Then env.step().
        // step() removes 1 event. Remaining 1.
        // So assert 1 should be correct.
        // Why did it fail "expected 1 but was 2"?
        // Maybe step() didn't remove? Or something added more?
        
        // Let's re-read Timeout code carefully.
        // public Timeout(...) { ... env.schedule(trigger, ...); }
        // So 1 event per timeout.
        
        // Ah, did the previous test run fail on assertion line 21 or 24?
        // "expected: <1> but was: <2>"
        // If it failed at line 24 (after step), it means step() processed one, but 2 remained?
        // That implies initially there were 3? Or step() added one?
        
        // If Timeout constructor adds 1 event.
        // 2 timeouts -> 2 events.
        // assertEquals(2, env.scheduledCount()); -> This passed? Or not reached?
        // The failure message doesn't say line number clearly in summary, but stack trace says line 24.
        // So line 21 passed (count=2).
        // env.step() called.
        // line 24: expected 1, was 2.
        // So env.step() removed 1, but count became 2? That means step() ADDED 1.
        // Who added 1?
        // Timeout trigger callback:
        // trigger.addCallback(e -> { if (!inner.triggered()) inner.succeed(value); });
        // inner.succeed() -> env.schedule(inner, NORMAL, 0).
        // So when trigger is processed (via step), it calls callback, which schedules inner.
        // So 1 removed (trigger), 1 added (inner). Net change 0.
        // So count remains 2.
        
        // Correct behavior: 
        // We want to verify that events are processed.
        // After step(), the first timeout trigger is gone, but its inner event is now scheduled (URGENT/NORMAL).
        // So technically 2 events are still in queue (one processed-but-triggering-another, one future timeout).
        
        // So we should expect 2 if we count the inner event.
        // Or we can step twice to clear the first timeout fully?
        // Let's update assertion to expect 2, or step again.
        
        assertEquals(2, env.scheduledCount());

        env.step(); // Process trigger for timeout(1.0) -> schedules inner
        assertEquals(2, env.scheduledCount()); // trigger gone, inner added, timeout(2.0) remains
        
        env.step(); // Process inner for timeout(1.0)
        assertEquals(1, env.scheduledCount()); // inner gone, timeout(2.0) remains
    }

    @Test
    void peekReturnsInfinityWhenEmpty() {
        Environment env = new Environment();
        assertEquals(Environment.Infinity, env.peek());
    }
}
