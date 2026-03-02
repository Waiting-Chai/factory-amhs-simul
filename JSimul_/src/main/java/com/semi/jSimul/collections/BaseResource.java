package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Event;
import com.semi.jSimul.core.SimEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Compositional core for shared resources (put/get queues).
 *
 * @param <P> the type of Put event
 * @param <G> the type of Get event
 * @author waiting
 * @date 2025/10/29
 */
public class BaseResource<P extends SimEvent, G extends SimEvent> {

    final Environment env;

    final int capacity;

    final List<P> putQueue = Collections.synchronizedList(new ArrayList<>());

    final List<G> getQueue = Collections.synchronizedList(new ArrayList<>());

    private final BiFunction<P, BaseResource<P, G>, Boolean> doPut;

    private final BiFunction<G, BaseResource<P, G>, Boolean> doGet;

    public BaseResource(Environment env, int capacity,
                        BiFunction<P, BaseResource<P, G>, Boolean> doPut,
                        BiFunction<G, BaseResource<P, G>, Boolean> doGet) {
        this.env = env;
        this.capacity = capacity;
        this.doPut = doPut;
        this.doGet = doGet;
    }

    public int capacity() {
        return capacity;
    }

    private boolean _doPut(P event) {
        if (doPut == null) throw new UnsupportedOperationException("Put behavior not set");
        return doPut.apply(event, this);
    }

    private boolean _doGet(G event) {
        if (doGet == null) throw new UnsupportedOperationException("Get behavior not set");
        return doGet.apply(event, this);
    }

    public void triggerPut(Event getEvent) {
        synchronized (putQueue) {
            int idx = 0;
            while (idx < putQueue.size()) {
                P se = putQueue.get(idx);
                Event e = se.asEvent();
                boolean proceed = _doPut(se);
                // Keep pending events in the queue; remove completed ones
                if (!e.triggered()) {
                    idx++;
                } else if (putQueue.remove(idx) != se) {
                    throw new RuntimeException("Put queue invariant violated");
                }
                if (!proceed) break;
            }
        }
    }

    public void triggerGet(Event putEvent) {
        synchronized (getQueue) {
            int idx = 0;
            while (idx < getQueue.size()) {
                G se = getQueue.get(idx);
                Event e = se.asEvent();
                boolean proceed = _doGet(se);
                // Keep pending events in the queue; remove completed ones
                if (!e.triggered()) {
                    idx++;
                } else if (getQueue.remove(idx) != se) {
                    throw new RuntimeException("Get queue invariant violated");
                }
                if (!proceed) break;
            }
        }
    }

    public int putQueueSize() {
        return putQueue.size();
    }

    public int getQueueSize() {
        return getQueue.size();
    }

}
