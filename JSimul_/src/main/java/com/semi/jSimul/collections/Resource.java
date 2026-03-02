package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple capacity-based Resource supporting request/release semantics.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Resource {

    private final BaseResource<Request, Release> core;

    public final List<SimEvent> users = new ArrayList<>();

    public Resource(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.core = new BaseResource<>(
                env,
                capacity,
                (event, res) -> {
                    if (users.size() < capacity) {
                        users.add(event);
                        event.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    users.remove(event.request);
                    event.asEvent().succeed(null);
                    return true;
                }
        );
    }

    public int count() {
        return users.size();
    }

    BaseResource<Request, Release> core() {
        return core;
    }

    public Request request() {
        return new Request(core);
    }

    public Release release(Request req) {
        if (req == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        if (!users.contains(req)) {
            throw new IllegalArgumentException("request does not hold the resource");
        }
        return new Release(core, req);
    }

}
