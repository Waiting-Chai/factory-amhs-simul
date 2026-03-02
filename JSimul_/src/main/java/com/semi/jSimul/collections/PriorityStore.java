package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Store that maintains items in priority order (min-heap) using composition.
 *
 * @param <T> the type of items stored
 * @author waiting
 * @date 2025/10/29
 */
public class PriorityStore<T> {

    private final BaseResource<StorePut<T>, StoreGet<T>> core;

    private final PriorityQueue<T> heap;

    public PriorityStore(Environment env, int capacity) {
        this(env, capacity, null);
    }

    public PriorityStore(Environment env, int capacity, Comparator<? super T> comparator) {
        this.heap = comparator == null ? new PriorityQueue<>() : new PriorityQueue<>(comparator);
        this.core = new BaseResource<>(
                env,
                capacity,
                (event, res) -> {
                    if (heap.size() < res.capacity) {
                        heap.add(event.item);
                        event.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    if (!heap.isEmpty()) {
                        T v = heap.poll();
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

}