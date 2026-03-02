package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.EmptySchedule;
import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Additional coverage for PreemptiveResource: multi-capacity, chained preemption, cancellation.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class PreemptiveResourceExtendedTest {

    @Test
    void higherPriorityPreemptsLowestWhenCapacityFull() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 2);

        PreemptiveRequest low1 = res.request(5);
        PreemptiveRequest low2 = res.request(4);
        env.step();
        env.step(); // grant two low users

        PreemptiveRequest high = res.request(1);
        env.step(); // should preempt low1 or low2 (worst priority)

        assertEquals(1, res.preemptionCount());
        assertTrue(high.asEvent().ok());
        assertTrue(low1.isPreempted() || low2.isPreempted());
    }

    @Test
    void queuedRequestCanCancelBeforeGrant() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest inUse = res.request(2);
        env.step(); // grant inUse

        PreemptiveRequest queued = res.request(3);
        queued.cancel();

        res.release(inUse);
        env.step(); // release
        assertEquals(0, res.waitingCount());
        assertThrows(EmptySchedule.class, env::step);
    }

    @Test
    void statsTrackGrantsAndPreemptions() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest a = res.request(5);
        env.step(); // grant a
        PreemptiveRequest b = res.request(1);
        env.step(); // preempt a, grant b

        assertEquals(2, res.grantedCount());
        assertEquals(1, res.preemptionCount());
    }

    @Test
    void nonPreemptingHighPriorityQueuesInsteadOfPreempting() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest inUse = res.request(5);
        env.step(); // grant inUse

        PreemptiveRequest highButNonPreempt = res.request(1, false);
        // should queue, not preempt
        // no step here; next step would throw EmptySchedule since nothing else is scheduled
        assertFalse(highButNonPreempt.isPreempted());
        assertFalse(highButNonPreempt.asEvent().triggered());

        res.release(inUse);
        env.step(); // release
        env.step(); // grant queued
        assertTrue(highButNonPreempt.asEvent().ok());
        assertEquals(0, res.preemptionCount());
    }
}
