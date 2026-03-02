package com.semi.jSimul.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * Exercises the new SimPy-style helper methods on {@link Environment}.
 *
 * @author waiting
 * @date 2025/11/05
 */
public class EnvironmentFactoryTest {

    @Test
    void processFactoryCompletes() {
        Environment env = new Environment();
        Process proc =
                env.process(
                        ctx -> {
                            ctx.await(env.timeout(2.0, "done"));
                            return "ok";
                        });

        Object result = env.run(proc);
        assertEquals("ok", result);
        assertEquals(2.0, env.now(), 1e-9);
    }

    @Test
    void timeoutFactoryPreservesValue() {
        Environment env = new Environment();
        Timeout timeout = env.timeout(1.5, "payload");
        Object result = env.run(timeout.asEvent());
        assertEquals("payload", result);
        assertEquals(1.5, env.now(), 1e-9);
    }

    @Test
    void eventFactoryAllowsManualTrigger() {
        Environment env = new Environment();
        Event signal = env.event();
        AtomicReference<String> captured = new AtomicReference<>();
        
        // Schedule trigger
        env.schedule(signal.markOk("manual"), Event.NORMAL, 0);
        
        // Register callback AFTER schedule (or before, but now it might trigger immediately if processed)
        // Wait, schedule() adds to queue. run() processes it.
        // markOk() sets value immediately.
        // If we add callback now, it should fire when processed?
        // Event.addCallback checks callbacks!=null.
        // If schedule() puts it in queue, it's not processed yet.
        
        // Wait, why did it fail with expected "manual" but was null?
        // captured.get() is null. So callback didn't run or value was null.
        // signal.markOk("manual") sets value="manual".
        // schedule() adds to queue.
        // run(signal) -> runInternal -> step -> detachCallbacks -> call.
        // So callback should run.
        // Maybe schedule() creates a Scheduled wrapper, but signal is the event.
        
        // Let's add callback before schedule to be safe?
        // signal.addCallback(...) was before schedule in original code.
        // Why failed?
        
        // Ah, previous failure stack trace:
        // assertEquals("manual", captured.get()) failed.
        // So captured.get() was null.
        
        // Let's try:
        signal.addCallback(ev -> captured.set((String) ev.value()));
        
        // markOk returns 'this'.
        // signal.markOk("manual") sets value.
        // env.schedule(signal, ...)
        // env.run(signal)
        
        // Is it possible that Event.value() throws? No, markOk sets it.
        
        // Maybe race in addCallback vs markOk? No, single thread test.
        
        // Let's just ensure markOk is called.
        signal.markOk("manual");
        env.schedule(signal, Event.NORMAL, 0);
        env.run(signal);

        assertEquals("manual", captured.get());
    }

    @Test
    void allOfAnyOfFactoriesComposeEvents() {
        Environment env = new Environment();
        Timeout t1 = env.timeout(1.0, "A");
        Timeout t2 = env.timeout(2.0, "B");
        SimEvent all = env.allOf(t1, t2);
        SimEvent any = env.anyOf(t1, t2);

        env.run(any);
        ConditionValue anyValue = (ConditionValue) any.asEvent().value();
        assertTrue(anyValue.contains(t1.asEvent()));

        env.run(all);
        ConditionValue allValue = (ConditionValue) all.asEvent().value();
        assertTrue(allValue.contains(t1.asEvent()));
        assertTrue(allValue.contains(t2.asEvent()));
    }

    @Test
    void exitTerminatesActiveProcess() {
        Environment env = new Environment();
        AtomicBoolean finallyExecuted = new AtomicBoolean(false);

        Process proc =
                env.process(
                        ctx -> {
                            try {
                                ctx.await(env.timeout(1.0));
                                ctx.env().exit("short-circuit");
                                return "unreachable";
                            } finally {
                                finallyExecuted.set(true);
                            }
                        });

        Object result = env.run(proc);
        assertEquals("short-circuit", result);
        assertTrue(finallyExecuted.get());
    }
}
