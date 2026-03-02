package com.semi.jSimul.core;

import java.util.Arrays;
import java.util.List;

/**
 * AllOf condition: succeeds when all events are processed successfully (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class AllOf implements ConditionCarrier {

    private final Condition condition;

    private final Event inner;

    public AllOf(Environment env, List<?> events) {
        this.condition = new Condition(env, Condition::allEvents, events);
        this.inner = condition.asEvent();
    }

    public AllOf(Environment env, Object... events) {
        this(env, Arrays.asList(events));
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    @Override
    public Condition condition() {
        return condition;
    }

}
