package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.EmptySchedule;
import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for preemptive resource semantics.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class PreemptiveResourceTest {

    @Test
    void higherPriorityPreemptsAndFailsVictim() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest low = res.request(5);
        env.step(); // grant low
        assertTrue(low.asEvent().ok());

        PreemptiveRequest high = res.request(0);
        env.step(); // preempt low and grant high

        assertTrue(high.asEvent().ok());
        assertTrue(low.isPreempted());
        assertTrue(low.asEvent().ok());
    }

    @Test
    void equalPriorityDoesNotPreemptAndQueues() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest first = res.request(1);
        PreemptiveRequest second = res.request(1);

        env.step(); // grant first
        assertTrue(first.asEvent().ok());
        assertFalse(second.asEvent().triggered());

        res.release(first);
        env.step(); // release
        env.step(); // grant second
        assertTrue(second.asEvent().ok());
    }

    @Test
    void queuedRequestCanBeCancelled() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest first = res.request(1);
        PreemptiveRequest queued = res.request(2);

        env.step(); // grant first
        queued.cancel();
        res.release(first);
        env.step(); // release first
        // no queued request should be granted; expect next step to see EmptySchedule
        assertThrows(EmptySchedule.class, env::step);
    }

    @Test
    void releasingNonHolderFails() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest r1 = res.request(1);
        PreemptiveRequest r2 = res.request(0); // may preempt
        env.step(); // grant one of them

        // Create a new request that never held the resource; releasing it must fail
        PreemptiveRequest neverGranted = new PreemptiveRequest(res, 2, true, Long.MAX_VALUE);
        PreemptiveRelease bogus = res.release(neverGranted);
        // First step may drain a pending granted request; the next step must surface the failure
        env.step();
        assertThrows(RuntimeException.class, env::step);
    }
}
