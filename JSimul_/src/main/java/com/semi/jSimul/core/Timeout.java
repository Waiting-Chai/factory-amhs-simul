package com.semi.jSimul.core;

/**
 * A timeout event that is triggered after a delay (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Timeout implements SimEvent {

    private final double delay;

    private final Event inner;

    private final Object v;

    public Timeout(Environment env, double delay, Object value) {
        if (delay < 0) throw new IllegalArgumentException("Negative delay " + delay);
        this.delay = delay;
        this.v = value;
        this.inner = new Event(env);
        
        // Schedule a trigger event that will succeed the inner event
        Event trigger = new Event(env);
        trigger.markOk(null);
        trigger.addCallback(e -> {
            if (!inner.triggered()) {
                inner.succeed(value);
            }
        });
        env.schedule(trigger, Event.NORMAL, delay);
        
        // Forward callbacks from inner to trigger? No.
        // But callbacks on inner are NOT called by trigger automatically.
        // inner.succeed() -> schedule(inner) -> run processes inner -> calls callbacks.
        // So if we run(timeout.asEvent()), we wait for inner.
        // inner is scheduled by trigger's callback.
        // So trigger processed -> inner scheduled.
        // Then run continues -> inner processed -> callbacks called.
        // This seems correct for run().
        // BUT if we just run() and expect callback to be called, we need inner to be processed.
        // The failure in CallbackTest is "expected true but was false".
        // This means the callback on inner was NOT called.
        // Why?
        // env.run(timeout.asEvent()) waits for inner.
        // inner is processed.
        // So callbacks should be called.
        // Wait, inner.succeed(value) schedules inner with delay 0.
        // Does run() loop until inner is processed? Yes.
        // Is it possible run() exits before callbacks are fired?
        // runInternal:
        // if (s != null) { ... cb.call(event); }
        // if (untilEvent.triggered()) return value;
        // If inner is processed, callbacks are called.
        
        // Ah, maybe the issue is how callbacks are added?
        // Test adds callback to timeout.asEvent() (which is inner).
        // Trigger runs -> inner.succeed() -> inner scheduled.
        // Environment pops inner -> calls callbacks.
        // Should work.
        
        // Let's look at the test failure again.
        // AssertionFailedError: expected: <true> but was: <false>
        // Means atomic boolean not set.
        // Means callback not called.
        
        // Is it possible that inner event is NOT processed?
        // run(inner) waits until inner is processed.
        // So run() returns.
        // If run() returns, inner MUST be processed (or triggered).
        // runInternal: if (untilEvent.isProcessed()) return ...
        // Event.isProcessed() -> callbacks == null.
        // Event.detachCallbacks() sets callbacks = null.
        // So if run() returns, detachCallbacks() must have run.
        // And detachCallbacks() returns the list of callbacks to call.
        // Then loop calls them.
        
        // Wait!
        // Event.addCallback: if callbacks == null, call immediately.
        // If event is already processed when we add callback? No, we add before run.
        
        // Is it possible detachCallbacks returned empty?
        // Maybe synchronized issue? We added synchronized.
        
        // Is it possible that inner.succeed() failed?
        // "has already been triggered" -> No exception seen.
        
        // Let's debug by printing in Timeout constructor or callback.
        // Actually, I suspect the issue is that `inner` event is somehow NOT the one being processed?
        // No, `asEvent()` returns `inner`.
        
        // Wait, what if `trigger` event is processed, but `inner` is scheduled but NOT processed yet when run() checks?
        // runInternal loop:
        // 1. Process trigger.
        //    trigger callback runs -> inner.succeed() -> schedule inner.
        //    check untilEvent (inner): triggered? Yes (succeed set ok=true). Processed? No (callbacks not null).
        //    Loop continues.
        // 2. Process inner.
        //    detachCallbacks (gets our test callback).
        //    call callbacks.
        //    check untilEvent (inner): triggered? Yes. Processed? Yes.
        //    Return.
        // So it should work.
        
        // Unless... `schedule(inner, NORMAL, 0)` puts it in queue.
        // If `run()` exits too early?
        // `run(untilEvent)` checks `untilEvent.triggered() || untilEvent.isProcessed()`.
        // If triggered is true, but processed is false?
        // Code:
        // if (untilEvent != null && (untilEvent.triggered() || untilEvent.isProcessed())) { return ... }
        // WAIT.
        // If it returns when triggered is true, but processed is false...
        // Then it exits BEFORE processing callbacks!
        // inner.succeed() sets triggered=true.
        // But callbacks are only called when processed (popped from queue).
        // The loop check at start of `runInternal` and end of loop checks `triggered || processed`.
        // If `triggered` is true, it returns.
        // So if we just scheduled it, it is triggered but not processed.
        // Run exits.
        // Callbacks never called.
        // THIS IS THE BUG.
        
        // SimPy semantics: run(event) should process the event.
        // Processing means calling callbacks.
        // So we should only return if `isProcessed()`.
        // `triggered` just means value is ready.
        // But for `run(event)`, we usually want the event to be fully handled.
        // Actually, if we return, the event is still in queue?
        // Yes.
        // So `run(event)` should wait until `event.isProcessed()`.
        // Let's check `runInternal` logic.
    }

    public Timeout(Environment env, double delay) {
        this(env, delay, null);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + delay + (v == null ? ")" : ", value=" + v + ")");
    }

}