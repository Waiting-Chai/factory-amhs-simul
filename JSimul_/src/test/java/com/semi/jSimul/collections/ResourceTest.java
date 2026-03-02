package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Test class for Resource functionality.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class ResourceTest {
    @Test
    void requestRelease() {
        Environment env = new Environment();
        Resource r = new Resource(env, 1);
        Request req = r.request();
        env.step();
        assertEquals(1, r.count());
        Release rel = r.release(req);
        env.step();
        assertEquals(0, r.count());
    }

    @Test
    void releasingNonHolderThrows() {
        Environment env = new Environment();
        Resource r = new Resource(env, 1);
        Request holder = r.request();
        Request stranger = r.request();
        env.step(); // grant holder
        assertThrows(IllegalArgumentException.class, () -> r.release(stranger));
        stranger.cancel(); // ensure queued stranger won't re-acquire once capacity frees
        // holder can still release
        r.release(holder);
        env.step();
        assertEquals(0, r.count());
    }
}
