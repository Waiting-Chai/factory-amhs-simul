package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Put request event for BaseResource.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Put implements SimEvent {

    final BaseResource<Put, Get> resource;

    private final Event inner;

    Put(BaseResource<Put, Get> resource) {
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