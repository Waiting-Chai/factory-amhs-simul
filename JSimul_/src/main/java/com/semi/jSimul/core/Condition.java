package com.semi.jSimul.core;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Condition event triggered when evaluate(events,count) returns true (compositional form).
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Condition implements ConditionCarrier {

    public static boolean allEvents(List<Event> events, int count) {
        return events.size() == count;
    }

    public static boolean anyEvents(List<Event> events, int count) {
        return count > 0 || events.isEmpty();
    }

    private final Event inner;

    private final BiPredicate<List<Event>, Integer> evaluate;

    private final List<Event> events = new ArrayList<>();

    private final Map<Event, Condition> nestedByEvent = new HashMap<>();

    private final List<Condition> nestedConditions = new ArrayList<>();

    private int count;

    public Condition(Environment env, BiPredicate<List<Event>, Integer> evaluate, List<?> events) {
        this.inner = new Event(env);
        this.evaluate = evaluate;
        absorb(events);

        if (this.events.isEmpty()) {
            inner.succeed(new ConditionValue());
            return;
        }

        for (Event e : this.events) {
            if (e.env() != env) {
                throw new IllegalArgumentException("Cannot mix events from different environments");
            }
        }

        for (Event e : this.events) {
            if (e.isProcessed()) {
                check(e);
            } else {
                e.addCallback(this::check);
            }
        }

        this.inner.addCallback(this::buildValue);
    }

    private void absorb(List<?> input) {
        for (Object source : input) {
            toEvent(source);
        }
    }

    private Event toEvent(Object source) {
        if (source instanceof ConditionCarrier carrier) {
            Condition condition = carrier.condition();
            registerNested(condition);
            Event event = condition.asEvent();
            recordEvent(event, condition);
            return event;
        }
        if (source instanceof Event event) {
            recordEvent(event, null);
            return event;
        }
        if (source instanceof SimEvent simEvent) {
            Event event = simEvent.asEvent();
            recordEvent(event, null);
            return event;
        }
        throw new IllegalArgumentException("Unsupported event type: " + source);
    }

    private void recordEvent(Event event, Condition nested) {
        if (!events.contains(event)) {
            events.add(event);
        }
        if (nested != null) {
            nestedByEvent.put(event, nested);
        }
    }

    private void registerNested(Condition condition) {
        if (condition == this) {
            return;
        }
        if (!nestedConditions.contains(condition)) {
            nestedConditions.add(condition);
        }
    }

    private void populateValue(ConditionValue cv) {
        harvestValues(cv);
    }

    private void harvestValues(ConditionValue cv) {
        for (Event e : events) {
            if (nestedByEvent.containsKey(e)) {
                continue;
            }
            // Only collect values from processed events that have values
            if (e.hasValue()) {
                cv.add(e);
            }
        }
        for (Condition nested : nestedConditions) {
            nested.harvestValues(cv);
        }
    }

    private void buildValue(Event event) {
        // No-op now, handled in check()
    }

    private void check(Event e) {
        // If already triggered, no further checks are needed
        if (inner.triggered()) return;
        this.count += 1;
        if (!e.ok()) {
            // Defuse the failing operand to prevent environment crash; mirror SimPy behavior.
            if (!e.isDefused()) {
                e.setDefused(true);
            }
            inner.setDefused(e.isDefused());
            inner.fail((Throwable) e.value());
            return;
        }
        if (evaluate.test(events, count)) {
            ConditionValue cv = new ConditionValue();
            populateValue(cv);
            inner.succeed(cv);
        }
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    @Override
    public Condition condition() {
        return this;
    }

}
