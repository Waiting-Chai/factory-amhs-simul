package com.semi.jSimul.core;

/**
 * Marker interface exposing the underlying {@link Condition} used to realise a
 * compositional {@link SimEvent}. It enables nested conditions to propagate
 * their contributing events when building {@link ConditionValue} instances.
 *
 * @author waiting
 * @date 2025/11/05
 */
interface ConditionCarrier extends SimEvent {

    /**
     * @return the backing {@link Condition} that orchestrates this composite event
     */
    Condition condition();

}
