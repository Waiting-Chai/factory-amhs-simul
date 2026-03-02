package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

/**
 * Store put event carrying an item.
 *
 * @param <T> the type of item
 * @author waiting
 * @date 2025/10/29
 */
public class StorePut<T> implements SimEvent {

    final BaseResource<StorePut<T>, ?> resource;

    final T item;

    private final Event inner;

    StorePut(BaseResource<StorePut<T>, ?> resource, T item) {
        this.resource = resource;
        this.item = item;
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