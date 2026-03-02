package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Tests for resource statistics: wait times and utilization.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ResourceStatsTest {

    @Test
    void priorityResourceWaitAndUtilization() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);

        PriorityRequest r1 = res.request(1);
        env.step(); // grant r1 at t=0

        PriorityRequest r2 = res.request(1);
        // r2 is queued; no additional step required

        res.release(r1);
        env.step(); // process release
        env.step(); // grant r2

        // Advance time to accumulate utilization (simulate work under r2)
        env.timeout(5.0);
        env.run(5.0);

        assertEquals(2, res.grantedCount());
        assertTrue(res.totalWaitTime() >= 0.0);
        assertTrue(res.utilization() >= 0.0);
    }

    @Test
    void preemptiveResourceWaitAndPreemptionStats() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);

        PreemptiveRequest low = res.request(5);
        env.step(); // grant

        PreemptiveRequest high = res.request(1);
        env.step(); // preempt low, grant high

        assertEquals(2, res.grantedCount());
        assertEquals(1, res.preemptionCount());
        assertTrue(res.totalWaitTime() >= 0.0);
    }
}
