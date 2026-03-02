package com.semi.jSimul.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base Event type mirroring SimPy's Event.
 *
 * <p>Events are bound to an Environment, can be triggered successfully or with failure, and
 * invoke registered callbacks when processed by the environment.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Event {
    public static final Object PENDING = new Object();

    public static final int URGENT = 0;

    public static final int NORMAL = 1;

    /**
     * Simple callback interface.
     */
    public interface Callback {
        void call(Event event);
    }

    protected final Environment env;

    protected List<Callback> callbacks = new ArrayList<>();

    protected Object value = PENDING;

    protected boolean ok;

    protected boolean defused;

    protected String name;

    public Event(Environment env) {
        this.env = env;
    }

    public Event(Environment env, String name) {
        this.env = env;
        this.name = name;
    }

    public Environment env() {
        return env;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return getClass().getSimpleName() + "(" + (name != null ? name : "") + ")";
    }

    public boolean triggered() {
        // Triggered means the event has been completed (value set),
        // aligning with common past-tense semantics.
        return value != PENDING;
    }

    public boolean isProcessed() {
        return callbacks == null;
    }

    public boolean ok() {
        return ok;
    }

    public boolean isDefused() {
        return defused;
    }

    public void setDefused(boolean v) {
        this.defused = v;
    }

    public Object value() {
        if (value == PENDING) throw new IllegalStateException("Event value not yet available");
        return value;
    }
    
    public boolean hasValue() {
        return value != PENDING;
    }

    public synchronized void addCallback(Callback cb) {
        if (callbacks != null) {
            callbacks.add(cb);
        }
        // If callbacks is null, the event has already been processed.
        // According to SimPy parity requirements (and SimPy 2/3 behavior in some contexts),
        // adding a callback to a processed event might be ignored or raise error.
        // The unit test `callbacksIgnoredAfterProcessing` requires it to be ignored.
    }

    /**
     * Remove a callback if still registered (no-op if already processed).
     */
    public synchronized void removeCallback(Callback cb) {
        if (callbacks != null) {
            callbacks.remove(cb);
        }
    }

    /**
     * Detach current callbacks for processing; set callbacks to null.
     */
    synchronized List<Callback> detachCallbacks() {
        List<Callback> cbs = callbacks;
        callbacks = null;
        return cbs == null ? Collections.emptyList() : cbs;
    }

    /**
     * Trigger with another event's state and value.
     */
    public Event trigger(Event other) {
        this.ok = other.ok;
        this.value = other.value;
        env.schedule(this, NORMAL, 0);
        return this;
    }

    /**
     * Succeed with value.
     */
    public Event succeed(Object v) {
        if (value != PENDING) throw new RuntimeException(this + " has already been triggered");
        this.ok = true;
        this.value = v;
        env.schedule(this, NORMAL, 0);
        return this;
    }

    /**
     * Mark OK with value without scheduling (internal use).
     */
    Event markOk(Object v) {
        this.ok = true;
        this.value = v;
        return this;
    }

    /**
     * Fail with exception.
     */
    public Event fail(Throwable ex) {
        if (value != PENDING) throw new RuntimeException(this + " has already been triggered");
        if (ex == null) throw new IllegalArgumentException("not an exception");
        this.ok = false;
        this.value = ex;
        env.schedule(this, NORMAL, 0);
        return this;
    }

    /**
     * Compose this event with additional operands using logical AND semantics.
     * The returned {@link SimEvent} succeeds once every operand has completed
     * successfully.
     *
     * @param others further events or {@link SimEvent} instances
     * @return a composite event mirroring SimPy's {@code event & other}
     */
    public SimEvent and(Object... others) {
        return new Condition(env, Condition::allEvents, composeArgs("and", others));
    }

    /**
     * Compose this event with additional operands using logical OR semantics.
     * The returned {@link SimEvent} succeeds once any operand has completed
     * successfully.
     *
     * @param others further events or {@link SimEvent} instances
     * @return a composite event mirroring SimPy's {@code event | other}
     */
    public SimEvent or(Object... others) {
        return new Condition(env, Condition::anyEvents, composeArgs("or", others));
    }

    private List<Object> composeArgs(String opName, Object... others) {
        if (others == null || others.length == 0) {
            throw new IllegalArgumentException("Operator '" + opName + "' requires at least one operand");
        }
        List<Object> args = new ArrayList<>(others.length + 1);
        args.add(this);
        for (Object other : others) {
            if (other == null) {
                throw new IllegalArgumentException("Null operand is not allowed for '" + opName + "'");
            }
            args.add(other);
        }
        return args;
    }

    RuntimeException failureAsRuntime() {
        Throwable t = (Throwable) value;
        if (t instanceof RuntimeException rt) {
            return rt;
        }
        return new RuntimeException(t.getMessage(), t);
    }
}
