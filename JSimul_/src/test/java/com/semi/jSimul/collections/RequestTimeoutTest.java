package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Tests RequestTimeout exception usage.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class RequestTimeoutTest {

    @Test
    void priorityResourceTimeoutThrowsRequestTimeout() {
        Environment env = new Environment();
        PriorityResource res = new PriorityResource(env, 1);
        res.request(1);
        env.step(); // grant first

        PriorityRequest queued = res.request(2, 1.0);
        RequestTimeout ex = assertThrows(RequestTimeout.class, env::run);
        assertTrue(queued.asEvent().triggered());
        assertFalse(queued.asEvent().ok());
        assertTrue(ex.getMessage().contains("PriorityRequest timeout"));
    }

    @Test
    void preemptiveResourceTimeoutThrowsRequestTimeout() {
        Environment env = new Environment();
        PreemptiveResource res = new PreemptiveResource(env, 1);
        res.request(5);
        env.step(); // grant in-use

        PreemptiveRequest queued = res.request(1, false, 0.5);
        RequestTimeout ex = assertThrows(RequestTimeout.class, env::run);
        assertTrue(queued.asEvent().triggered());
        assertFalse(queued.asEvent().ok());
        assertTrue(ex.getMessage().contains("PreemptiveRequest timeout"));
    }
}
