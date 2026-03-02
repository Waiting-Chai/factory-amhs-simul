package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Request event for {@link PreemptiveResource}.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PreemptiveRequest implements SimEvent, Comparable<PreemptiveRequest> {

    final PreemptiveResource resource;
    final int priority;
    final long order;
    final boolean preempt;
    private final Event inner;
    private volatile boolean preempted;
    private final double created;

    PreemptiveRequest(PreemptiveResource resource, int priority, boolean preempt, long order) {
        this.resource = resource;
        this.priority = priority;
        this.order = order;
        this.preempt = preempt;
        this.created = resource.env().now();
        this.inner = new Event(resource.env());
        resource.onRequest(this);
    }

    public void cancel() {
        if (!inner.triggered()) {
            resource.cancelRequest(this);
        }
    }

    @Override
    public Event asEvent() {
        return inner;
    }

    public boolean isPreempted() {
        return preempted;
    }

    void markPreempted() {
        this.preempted = true;
    }

    public boolean isPreempt() {
        return preempt;
    }

    double createdTime() {
        return created;
    }

    @Override
    public int compareTo(PreemptiveRequest other) {
        int p = Integer.compare(this.priority, other.priority);
        if (p != 0) return p;
        return Long.compare(this.order, other.order);
    }
}
