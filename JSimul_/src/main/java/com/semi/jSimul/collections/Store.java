package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * FIFO store for arbitrary items with capacity.
 *
 * @param <T> the type of items stored
 * @author waiting
 * @date 2025/10/29
 */
public class Store<T> {

    private final BaseResource<StorePut<T>, StoreGet<T>> core;

    protected final List<T> items = new ArrayList<>();

    public Store(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.core = new BaseResource<>(
                env,
                capacity,
                (event, res) -> {
                    if (items.size() < capacity) {
                        items.add(event.item);
                        event.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    if (!items.isEmpty()) {
                        T v = items.removeFirst();
                        event.asEvent().succeed(v);
                    }
                    return true;
                }
        );
    }

    public StorePut<T> put(T item) {
        return new StorePut<>(core, item);
    }

    public StoreGet<T> get() {
        return new StoreGet<>(core);
    }

    public BaseResource<StorePut<T>, StoreGet<T>> core() {
        return core;
    }

}
