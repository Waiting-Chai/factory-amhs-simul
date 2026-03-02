package com.semi.jSimul.collections;

import static org.junit.jupiter.api.Assertions.*;

import com.semi.jSimul.core.Environment;
import org.junit.jupiter.api.Test;
import java.util.Comparator;

/**
 * Test class for PriorityStore functionality.
 *
 * @author waiting
 * @date 2025/12/02
 */
public class PriorityStoreTest {

    @Test
    void naturalOrder() {
        Environment env = new Environment();
        PriorityStore<Integer> ps = new PriorityStore<>(env, 10);
        
        ps.put(3);
        ps.put(1);
        ps.put(2);
        
        env.step(); // Process puts
        env.step();
        env.step();
        
        StoreGet g1 = ps.get();
        env.step();
        assertEquals(1, g1.asEvent().value());
        
        StoreGet g2 = ps.get();
        env.step();
        assertEquals(2, g2.asEvent().value());
        
        StoreGet g3 = ps.get();
        env.step();
        assertEquals(3, g3.asEvent().value());
    }

    @Test
    void customComparator() {
        Environment env = new Environment();
        // Priority by string length (shorter first)
        PriorityStore<String> ps = new PriorityStore<>(env, 10, Comparator.comparingInt(String::length));
        
        ps.put("banana");
        ps.put("apple");
        ps.put("kiwi");
        
        env.step();
        env.step();
        env.step();
        
        StoreGet g1 = ps.get();
        env.step();
        assertEquals("kiwi", g1.asEvent().value()); // length 4
        
        StoreGet g2 = ps.get();
        env.step();
        assertEquals("apple", g2.asEvent().value()); // length 5
        
        StoreGet g3 = ps.get();
        env.step();
        assertEquals("banana", g3.asEvent().value()); // length 6
    }
    
    static class Task {
        int priority;
        String name;
        
        Task(int p, String n) { priority = p; name = n; }
        
        @Override
        public String toString() { return name; }
    }
    
    @Test
    void customObjectComparator() {
        Environment env = new Environment();
        PriorityStore<Task> ps = new PriorityStore<>(env, 10, Comparator.comparingInt(t -> t.priority));
        
        ps.put(new Task(10, "low"));
        ps.put(new Task(1, "high"));
        
        env.step();
        env.step();
        
        StoreGet g1 = ps.get();
        env.step();
        Task t = (Task) g1.asEvent().value();
        assertEquals("high", t.name);
    }
}
