package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Resource request event.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Request implements SimEvent {

    final BaseResource<Request, Release> resource;

    private final Event inner;

    Request(BaseResource<Request, Release> resource) {
        this.resource = resource;
        this.inner = new Event(resource.env);
        resource.putQueue.add(this);
        this.inner.addCallback(resource::triggerGet);
        resource.triggerPut(null);
    }

    public void cancel() {
        if (!inner.triggered()) resource.putQueue.remove(this);
    }

    @Override
    public Event asEvent() {
        return inner;
    }

}
