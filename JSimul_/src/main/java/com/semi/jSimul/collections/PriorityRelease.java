package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Release event for {@link PriorityResource}.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PriorityRelease implements SimEvent {

    final PriorityResource resource;
    final PriorityRequest request;
    private final Event inner;

    PriorityRelease(PriorityResource resource, PriorityRequest request) {
        this.resource = resource;
        this.request = request;
        this.inner = new Event(resource.env());
        resource.onRelease(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }
}
