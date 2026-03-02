package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Timeout behavior for PriorityResource requests.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class PriorityResourceTimeoutTest {

    @Test
    void queuedRequestTimesOutIfNotGrantedInTime() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);

        PriorityRequest holder = res.request(5);
        env.step(); // grant holder

        PriorityRequest queued = res.request(1, 0.5);
        // advance time beyond timeout without releasing holder
        env.timeout(1.0);
        assertThrows(RuntimeException.class, () -> env.run(1.0));

        assertTrue(queued.asEvent().triggered());
        assertFalse(queued.asEvent().ok());
        assertTrue(queued.asEvent().value().toString().contains("timeout"));
        // holder still holds
        assertEquals(1, res.count());
    }
}
