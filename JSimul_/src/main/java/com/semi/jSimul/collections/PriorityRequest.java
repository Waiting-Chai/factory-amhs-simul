package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Priority-based resource request event.
 *
 * <p>Lower priority values are served first; FIFO within same priority.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PriorityRequest implements SimEvent, Comparable<PriorityRequest> {

    final PriorityResource resource;
    final int priority;
    final long order;
    private final Event inner;
    private final double created;

    PriorityRequest(PriorityResource resource, int priority, long order) {
        this.resource = resource;
        this.priority = priority;
        this.order = order;
        this.created = resource.env().now();
        this.inner = new Event(resource.env());
        resource.onRequest(this);
    }

    /**
     * Cancel this request if it has not yet been granted.
     */
    public void cancel() {
        if (!inner.triggered()) {
            resource.cancelRequest(this);
        }
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    @Override
    public int compareTo(PriorityRequest other) {
        int p = Integer.compare(this.priority, other.priority);
        if (p != 0) return p;
        return Long.compare(this.order, other.order);
    }

    double createdTime() {
        return created;
    }
}
