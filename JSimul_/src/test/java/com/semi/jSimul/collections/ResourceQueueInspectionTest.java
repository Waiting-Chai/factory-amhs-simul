package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Verifies queue inspection helpers on BaseResource-backed collections.
 *
 * @author waiting
 * @date 2025/11/29
 */
public class ResourceQueueInspectionTest {

    @Test
    void resourceQueueSizesReflectWaitingRequests() {
        Environment env = new Environment();
        Resource res = new Resource(env, 1);

        Request holder = res.request();
        Request waiter = res.request();
        env.step(); // grant holder

        assertEquals(1, res.count());
        assertEquals(1, res.core().putQueueSize()); // waiter still queued

        res.release(holder);
        env.step(); // process release and grant waiter
        assertEquals(1, res.count());
        assertEquals(0, res.core().putQueueSize());
    }

    @Test
    void storeQueueSizesReflectWaitingGets() {
        Environment env = new Environment();
        Store store = new Store(env, 1);

        StoreGet get = store.get();
        assertEquals(1, store.core().getQueueSize());

        store.put("x");
        env.step(); // process put
        env.step(); // grant get
        assertEquals(0, store.core().getQueueSize());
    }
}
