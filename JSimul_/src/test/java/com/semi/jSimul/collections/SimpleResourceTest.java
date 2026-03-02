package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.SimEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SimpleResource and type safety refactoring of BaseResource.
 */
public class SimpleResourceTest {

    @Test
    public void testPutGetWithCorrectTypes() {
        Environment env = new Environment();
        // Use SimpleResource which supports put() and get() safely
        SimpleResource base = new SimpleResource(
                env,
                1,
                (p, res) -> {
                    p.asEvent().succeed(null);
                    return true;
                },
                (g, res) -> {
                    g.asEvent().succeed(null);
                    return true;
                }
        );

        // These calls are now type-safe and part of SimpleResource
        SimEvent putEvent = base.put();
        SimEvent getEvent = base.get();
        
        // Events are processed immediately because capacity allows it and doPut/doGet succeed immediately
        assertTrue(putEvent.asEvent().triggered());
        assertTrue(getEvent.asEvent().triggered());
        
        assertEquals(0, base.putQueueSize());
        assertEquals(0, base.getQueueSize());

        // Run environment to process events (though they are already triggered, callbacks might run)
        env.step(); 
        env.step();
    }
}
