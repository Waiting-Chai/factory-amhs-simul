package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Tests for priority-based resource allocation.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class PriorityResourceTest {

    @Test
    void lowerPriorityServedFirst() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);

        PriorityRequest high = res.request(0); // highest priority
        PriorityRequest low = res.request(5);

        env.step(); // grant first request
        assertTrue(high.asEvent().ok());
        assertFalse(low.asEvent().triggered());

        res.release(high);
        env.step(); // release succeeds
        env.step(); // grant queued request
        assertTrue(low.asEvent().ok());
    }

    @Test
    void fifoWithinSamePriority() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);

        PriorityRequest r1 = res.request(1);
        PriorityRequest r2 = res.request(1);
        PriorityRequest r3 = res.request(0); // higher priority should jump the queue

        env.step(); // grant r1
        res.release(r1);
        env.step(); // release r1
        env.step(); // grant r3 (higher priority)
        assertTrue(r3.asEvent().ok());
        res.release(r3);
        env.step(); // release r3
        env.step(); // grant r2 last
        assertTrue(r2.asEvent().ok());
    }

    @Test
    void releasingNonUserFails() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);

        PriorityRequest r1 = res.request(1);
        PriorityRequest r2 = res.request(1);
        env.step(); // r1 granted

        PriorityRelease bogus = res.release(r2);
        assertThrows(RuntimeException.class, env::step, "Releasing non-user must fail");
    }
}
