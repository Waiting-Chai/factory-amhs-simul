package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated tests for FilterStore type safety and functionality.
 */
public class FilterStoreTest {

    @Test
    public void testStringFilter() {
        Environment env = new Environment();
        FilterStore<String> store = new FilterStore<>(env, 2);
        
        store.put("apple");
        store.put("banana");
        store.put("cherry");
        // Queue is size 2, so this will wait in putQueue or fail?
        // BaseResource put logic: if items.size < capacity, add.
        // put() creates StorePut which adds to putQueue.
        // triggerPut processes queue.
        // So "cherry" stays in putQueue until space is available.
        
        env.step(); // process apple
        env.step(); // process banana
        // cherry is pending
        
        // Get strings starting with 'b'
        FilterStoreGet<String> getB = store.get(s -> s.startsWith("b"));
        env.step(); // process get
        
        assertTrue(getB.asEvent().triggered());
        assertEquals("banana", getB.asEvent().value());
        
        // Now space is available, cherry should be added?
        // BaseResource triggerPut/triggerGet logic:
        // When get succeeds (item removed), it calls triggerPut.
        // So cherry should be added now.
        
        env.step(); // process cherry put
        
        // Get apple
        FilterStoreGet<String> getA = store.get(s -> s.equals("apple"));
        env.step();
        assertEquals("apple", getA.asEvent().value());
        
        // Get cherry
        FilterStoreGet<String> getC = store.get(s -> s.equals("cherry"));
        env.step();
        assertEquals("cherry", getC.asEvent().value());
    }

    @Test
    public void testIntegerFilter() {
        Environment env = new Environment();
        FilterStore<Integer> store = new FilterStore<>(env, 10);
        
        store.put(1);
        store.put(2);
        store.put(3);
        
        env.run(); // process all puts
        
        // Get even number
        FilterStoreGet<Integer> getEven = store.get(i -> i % 2 == 0);
        env.run();
        
        assertEquals(2, getEven.asEvent().value());
    }
    
    @Test
    public void testMixedTypesInObjectStore() {
        Environment env = new Environment();
        FilterStore<Object> store = new FilterStore<>(env, 10);
        
        store.put("hello");
        store.put(123);
        
        env.run();
        
        // Get Integer
        FilterStoreGet<Object> getInt = store.get(o -> o instanceof Integer);
        env.run();
        
        assertEquals(123, getInt.asEvent().value());
        
        // Get String
        FilterStoreGet<Object> getStr = store.get(o -> o instanceof String);
        env.run();
        
        assertEquals("hello", getStr.asEvent().value());
    }
}
