package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Timeout behavior for PreemptiveResource requests.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class PreemptiveResourceTimeoutTest {

    @Test
    void queuedRequestTimesOutWithoutPreemption() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest inUse = res.request(5);
        env.step(); // grant inUse

        PreemptiveRequest queued = res.request(1, false, 0.5); // higher priority but preempt disabled
        env.timeout(1.0);
        assertThrows(RuntimeException.class, () -> env.run(1.0));

        assertTrue(queued.asEvent().triggered());
        assertFalse(queued.asEvent().ok());
        assertTrue(queued.asEvent().value().toString().contains("timeout"));
        // inUse still holds; preemption count unchanged
        assertEquals(0, res.preemptionCount());
    }
}
