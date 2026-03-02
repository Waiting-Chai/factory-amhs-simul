package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Release event for {@link PreemptiveResource}.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PreemptiveRelease implements SimEvent {

    final PreemptiveResource resource;
    final PreemptiveRequest request;
    private final Event inner;

    PreemptiveRelease(PreemptiveResource resource, PreemptiveRequest request) {
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
