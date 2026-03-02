package com.semi.jSimul.simutil;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimFunctionsTest {

    @Test
    void startDelayed() {
        Environment env = new Environment();
        
        SimFunctions.startDelayed(env, ctx -> {
            assertEquals(10.0, env.now());
            return "ok";
        }, 10.0);

        env.run();
        assertEquals(10.0, env.now());
    }

    @Test
    void loopRunsPeriodically() {
        Environment env = new Environment();
        AtomicInteger count = new AtomicInteger(0);
        
        SimFunctions.loop(env, 2.0, count::incrementAndGet);
        
        env.run(5.0); // Should run at 0, 2, 4
        
        assertEquals(3, count.get());
        assertEquals(5.0, env.now());
    }

    @Test
    void monitorCollectsValues() {
        Environment env = new Environment();
        List<Double> recorded = new ArrayList<>();
        
        SimFunctions.monitor(env, 1.0, env::now, recorded::add);
        
        env.run(3.5); // Should record at 0.0, 1.0, 2.0, 3.0
        
        assertEquals(4, recorded.size());
        assertEquals(0.0, recorded.get(0));
        assertEquals(1.0, recorded.get(1));
        assertEquals(2.0, recorded.get(2));
        assertEquals(3.0, recorded.get(3));
    }

    @Test
    void waitForBlocksUntilTrue() {
        Environment env = new Environment();
        AtomicInteger state = new AtomicInteger(0);
        
        // Process that changes state at t=3.0
        env.process(ctx -> {
            ctx.await(env.timeout(3.0));
            state.set(1);
            return null;
        });

        // WaitFor checks every 0.5s
        Process waiter = SimFunctions.waitFor(env, () -> state.get() == 1, 0.5);
        
        long start = System.currentTimeMillis();
        env.run(waiter);
        
        assertEquals(3.0, env.now());
        assertEquals(1, state.get());
    }
}
