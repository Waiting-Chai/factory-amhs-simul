package com.semi.jSimul.core;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Execution environment for an event-based simulation, modeled after SimPy's Environment.
 *
 * <p>Time advances by stepping through scheduled events. Events are scheduled with a time and
 * priority, and processed in order. Failed events will crash the environment unless defused.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Environment implements BaseEnvironment {

    public static final double Infinity = Double.POSITIVE_INFINITY;

    private volatile double now;

    private final PriorityQueue<Scheduled> queue;
    
    private final Object queueLock = new Object();

    private final AtomicLong eid;

    private final AtomicInteger pendingAsync = new AtomicInteger(0);

    private Process activeProcess;

    public Environment() {
        this(0.0);
    }

    public Environment(double initialTime) {
        this.now = initialTime;
        this.queue = new PriorityQueue<>();
        this.eid = new AtomicLong();
    }

    public void incPending() {
        pendingAsync.incrementAndGet();
    }

    public void decPending() {
        pendingAsync.decrementAndGet();
        synchronized (queueLock) {
            queueLock.notifyAll();
        }
    }

    @Override
    public double now() {
        return now;
    }

    @Override
    public Process activeProcess() {
        return activeProcess;
    }

    void setActiveProcess(Process p) {
        this.activeProcess = p;
    }

    @Override
    public void schedule(Event event, int priority, double delay) {
        double time = now + delay;
        synchronized (queueLock) {
            queue.add(new Scheduled(time, priority, eid.incrementAndGet(), event));
            queueLock.notifyAll();
        }
    }

    /**
     * Create and start a {@link Process} using the supplied function. Matches
     * the ergonomics of SimPy's {@code env.process(generator)} helper so that
     * callers do not need to interact with {@link Process} constructors
     * directly.
     *
     * @param function user logic to execute inside the process
     * @return newly created process instance
     */
    public Process process(Process.ProcessFunction function) {
        return new Process(this, function);
    }

    /**
     * Create and start a named {@link Process}.
     *
     * @param function user logic to execute inside the process
     * @param name     name of the process
     * @return newly created process instance
     */
    public Process process(Process.ProcessFunction function, String name) {
        return new Process(this, function, name);
    }

    /**
     * Create a {@link Timeout} that fires after the given delay.
     */
    public Timeout timeout(double delay) {
        return new Timeout(this, delay);
    }

    /**
     * Create a {@link Timeout} with a payload that will be produced when the
     * timeout fires.
     */
    public Timeout timeout(double delay, Object value) {
        return new Timeout(this, delay, value);
    }

    /**
     * Create a bare {@link Event} bound to this environment, mirroring
     * SimPy's {@code env.event()} helper.
     */
    public Event event() {
        return new Event(this);
    }

    /**
     * Create an {@link AllOf} compositional event for the provided operands.
     */
    public SimEvent allOf(Object... events) {
        return new AllOf(this, normalizeArgs(events));
    }

    /**
     * Create an {@link AnyOf} compositional event for the provided operands.
     */
    public SimEvent anyOf(Object... events) {
        return new AnyOf(this, normalizeArgs(events));
    }

    /**
     * Terminate the currently active process successfully with the given
     * return value. This emulates SimPy's {@code env.exit(value)} convenience
     * for users who prefer early exits instead of {@code return}.
     *
     * @param value return value to propagate from the active process
     */
    public void exit(Object value) {
        if (activeProcess == null) {
            throw new IllegalStateException("No active process to exit");
        }
        throw new ProcessExit(value);
    }

    /**
     * Convenience overload that terminates the active process without a
     * result value.
     */
    public void exit() {
        exit(null);
    }

    public double peek() {
        synchronized (queueLock) {
            Scheduled head = queue.peek();
            return head == null ? Infinity : head.time();
        }
    }

    /**
     * @return number of events currently scheduled (for observability/testing).
     */
    public int scheduledCount() {
        synchronized (queueLock) {
            return queue.size();
        }
    }

    /**
     * Step through the next scheduled event (used internally or for debugging).
     * Prefer using run() to advance simulation.
     */
    @Override
    public void step() {
        Scheduled s;
        synchronized (queueLock) {
            s = queue.poll();
        }
        if (s == null) throw new EmptySchedule();
        processEvent(s);
    }

    @Override
    public Object run(Object until) {
        return switch (until) {
            case null -> run();
            case Event event -> run(event);
            case SimEvent simEvent -> run(simEvent);
            default -> run(((Number) until).doubleValue());
        };
    }

    /**
     * Run until no events are left.
     */
    public Object run() {
        return runInternal(null);
    }

    /**
     * Run until the given Event is processed.
     */
    public Object run(Event untilEvent) {
        if (untilEvent == null) return run();
        if (untilEvent.isProcessed()) return untilEvent.value();
        untilEvent.addCallback(StopSimulation::callback);
        return runInternal(untilEvent);
    }

    /**
     * Run until the given compositional event is processed.
     */
    public Object run(SimEvent untilEvent) {
        return run(untilEvent == null ? null : untilEvent.asEvent());
    }

    /**
     * Run until the given absolute time is reached.
     */
    public Object run(double untilTime) {
        if (untilTime <= now) throw new IllegalArgumentException("until must be > now");
        // Use a Timeout event to ensure time advances
        Timeout t = timeout(untilTime - now);
        Event untilEvent = t.asEvent();
        untilEvent.addCallback(StopSimulation::callback);
        return runInternal(untilEvent);
    }

    /**
     * Core run loop shared by all run overloads.
     */
    private Object runInternal(Event untilEvent) {
        while ( true ) {
            try {
                // Optimized wait logic: check conditions before blocking
                if (untilEvent != null && untilEvent.isProcessed()) {
                     return untilEvent.value(); // fast path exit
                }
                
                Scheduled s;
                synchronized (queueLock) {
                    while (queue.isEmpty() || pendingAsync.get() > 0) {
                        // If we are waiting for an event that hasn't happened, and queue is empty,
                        // we must wait for producers (e.g. async threads) to schedule something.
                        // If no untilEvent is set, an empty queue means simulation end.
                        // BUT if pendingAsync > 0, we MUST wait regardless of queue state (race prevention).
                        if (untilEvent == null && queue.isEmpty() && pendingAsync.get() == 0) {
                             // Double check: some async producer might have just added something before we locked?
                             // If truly empty and no untilEvent, we are done.
                             return null; 
                        }

                        // If untilEvent is NOT null, and queue is empty, and pendingAsync is 0,
                        // it means we are waiting for an event but nothing is scheduled and nothing is running.
                        // This is a deadlock or "end of simulation before target reached".
                        if (untilEvent != null && queue.isEmpty() && pendingAsync.get() == 0) {
                            // Check if untilEvent is already processed (handled by loop condition, but safe to check)
                            if (untilEvent.isProcessed()) return untilEvent.value();
                            
                            // If not processed, we are stuck. Throw EmptySchedule to indicate failure to reach target.
                            // This matches SimPy's behavior where running until an event that never happens 
                            // (and schedule empties) raises an exception.
                            // However, for runUntilEventWithoutScheduleThrows test, it expects RuntimeException with message.
                            throw new RuntimeException("No scheduled events left before until condition is met");
                        }
                        
                        // If untilEvent is present but not triggered, and queue is empty,
                        // we MUST wait for something to be scheduled (potentially by other threads).
                        // However, to avoid infinite deadlocks if producers die, we keep a timeout or rely on user interrupt.
                        // For this fix, we'll use a loop with wait() to avoid busy spinning.
                        try {
                            // Wait for schedule() to notify us
                            queueLock.wait(100); 
                            
                            // Re-check exit conditions after wake-up
                            if (untilEvent != null && untilEvent.isProcessed()) {
                                return untilEvent.value();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during simulation wait", e);
                        }
                    }
                    // Queue not empty, proceed to process
                    s = queue.poll();
                }
                
                if (s != null) {
                    processEvent(s);
                }
                
                // Check untilEvent after processing one step
                if (untilEvent != null && untilEvent.isProcessed()) {
                    return untilEvent.value();
                }
                
            } catch ( StopSimulation e ) {
                return e.value();
            }
        }
    }

    private void processEvent(Scheduled s) {
        this.now = s.time();
        Event event = s.event();
        var callbacks = event.detachCallbacks();
        for (Event.Callback cb : callbacks) {
            cb.call(event);
        }
        if (!event.ok() && !event.isDefused()) {
            throw event.failureAsRuntime();
        }
    }

    private List<Object> normalizeArgs(Object[] events) {
        return Arrays.asList(events == null ? new Object[0] : events);
    }

}
