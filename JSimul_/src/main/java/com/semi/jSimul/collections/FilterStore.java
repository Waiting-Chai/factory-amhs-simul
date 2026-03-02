package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Store supporting filtered get requests.
 *
 * @param <T> the type of items stored
 * @author waiting
 * @date 2025/10/29
 */
public class FilterStore<T> {

    private final BaseResource<StorePut<T>, FilterStoreGet<T>> core;

    protected final List<T> items = new ArrayList<>();

    public FilterStore(Environment env, int capacity) {
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
                    if (event.filter == null) {
                        event.asEvent().fail(new IllegalArgumentException("filter cannot be null"));
                        return true;
                    }
                    for (int i = 0; i < items.size(); i++) {
                        T item = items.get(i);
                        Predicate<T> filter = event.filter;
                        if (filter.test(item)) {
                            items.remove(i);
                            event.asEvent().succeed(item);
                            break;
                        }
                    }
                    return true;
                }
        );
    }

    public StorePut<T> put(T item) {
        return new StorePut<>(core, item);
    }

    public FilterStoreGet<T> get(Predicate<T> filter) {
        return new FilterStoreGet<>(core, filter);
    }

}
