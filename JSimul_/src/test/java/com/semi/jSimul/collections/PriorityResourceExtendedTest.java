package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.EmptySchedule;
import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Additional coverage for PriorityResource with cancellation, capacity>1, and stats.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class PriorityResourceExtendedTest {

    @Test
    void cancelQueuedRequestPreventsGrant() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);
        PriorityRequest a = res.request(1);
        PriorityRequest b = res.request(2);

        env.step(); // grant a
        b.cancel();
        res.release(a);
        env.step(); // release a
        assertEquals(0, res.waitingCount());
        assertThrows(EmptySchedule.class, env::step);
    }

    @Test
    void capacityTwoGrantsTopPriorities() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 2);

        PriorityRequest p1 = res.request(3);
        PriorityRequest p2 = res.request(1);
        PriorityRequest p3 = res.request(2);

        env.step(); // process p1 grant event
        env.step(); // process p2 grant event

        assertTrue(p1.asEvent().ok());
        assertTrue(p2.asEvent().ok());
        assertFalse(p3.asEvent().triggered()); // still waiting

        res.release(p2);
        env.step(); // process release
        env.step(); // grant p3
        assertTrue(p3.asEvent().ok());
        assertEquals(3, res.grantedCount());
    }
}
