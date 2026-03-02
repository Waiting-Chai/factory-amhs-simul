package com.semi.jSimul.collections;

import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

import java.util.function.Predicate;

/**
 * Filtered get event for FilterStore.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class FilterStoreGet<T> implements SimEvent {

    final BaseResource<?, FilterStoreGet<T>> resource;

    final Predicate<T> filter;

    private final Event inner;

    FilterStoreGet(BaseResource<?, FilterStoreGet<T>> resource, Predicate<T> filter) {
        this.resource = resource;
        this.filter = filter;
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