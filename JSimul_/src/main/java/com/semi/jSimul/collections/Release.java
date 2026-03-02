package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Resource release event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Release implements SimEvent {

    final BaseResource<Request, Release> resource;

    final Request request;

    private final Event inner;

    Release(BaseResource<Request, Release> resource, Request request) {
        this.resource = resource;
        this.request = request;
        this.inner = new Event(resource.env);
        resource.getQueue.add(this);
        this.inner.addCallback(resource::triggerPut);
        resource.triggerGet(null);
    }

    public void cancel() {
        if (!inner.triggered()) resource.getQueue.remove(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}
