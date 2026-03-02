package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

/**
 * Test class for Store functionality.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class StoreTest {
    @Test
    void fifoPutGet() {
        Environment env = new Environment();
        Store<String> s = new Store<>(env, 10);
        s.put("a");
        s.put("b");
        env.step();
        env.step();
        StoreGet g1 = s.get();
        env.step();
        assertEquals("a", g1.asEvent().value());
        StoreGet g2 = s.get();
        env.step();
        assertEquals("b", g2.asEvent().value());
    }

    @Test
    void filterStoreGet() {
        Environment env = new Environment();
        FilterStore<Object> fs = new FilterStore<>(env, 10);
        fs.put("x");
        fs.put(42);
        env.step();
        env.step();
        FilterStoreGet g = fs.get(o -> o instanceof Integer);
        env.step();
        assertEquals(42, g.asEvent().value());
    }

    @Test
    void filterStoreTyped() {
        Environment env = new Environment();
        FilterStore<Integer> fs = new FilterStore<>(env, 10);
        fs.put(10);
        fs.put(20);
        fs.put(30);
        env.step();
        env.step();
        env.step();
        
        // Get strictly greater than 15
        FilterStoreGet g = fs.get(i -> i > 15);
        env.step();
        assertEquals(20, g.asEvent().value());
    }

    @Test
    void nullFilterRejected() {
        Environment env = new Environment();
        FilterStore<String> fs = new FilterStore<>(env, 5);
        fs.put("a");
        env.step();
        FilterStoreGet g = fs.get(null);
        RuntimeException ex = assertThrows(RuntimeException.class, env::step);
        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
