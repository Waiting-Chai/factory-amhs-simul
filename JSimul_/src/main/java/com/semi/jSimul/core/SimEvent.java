package com.semi.jSimul.core;

/**
 * Compositional event interface that exposes an underlying Event instance.
 * <p>
 * This allows high-level types (e.g., Timeout, Condition, AllOf, AnyOf) to avoid inheritance
 * while remaining compatible with APIs that operate on Event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public interface SimEvent {

    Event asEvent();

    /**
     * Add a callback to be invoked when this event is processed.
     * This is a convenience method that delegates to the underlying Event.
     */
    default void addCallback(Event.Callback callback) {
        asEvent().addCallback(callback);
    }

    /**
     * Combine this event with additional operands using logical AND semantics.
     *
     * @param others subsequent events or {@link SimEvent} instances
     * @return composite event representing the AND of all operands
     */
    default SimEvent and(Object... others) {
        return asEvent().and(others);
    }

    /**
     * Combine this event with additional operands using logical OR semantics.
     *
     * @param others subsequent events or {@link SimEvent} instances
     * @return composite event representing the OR of all operands
     */
    default SimEvent or(Object... others) {
        return asEvent().or(others);
    }

}
