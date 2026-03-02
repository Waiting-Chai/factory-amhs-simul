package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resource that grants requests by priority (lower first), FIFO within same priority.
 *
 * <p>Preemption is not implemented here; higher-priority requests wait until
 * capacity is available.
 *
 * @author waiting
 * @date 2025/11/29
 */
public final class PriorityResource {

    private final Environment env;
    private final int capacity;
    private final List<PriorityRequest> users = new ArrayList<>();
    private final PriorityQueue<PriorityRequest> waiters = new PriorityQueue<>();
    private final AtomicLong order = new AtomicLong();
    private final AtomicLong granted = new AtomicLong();
    private double totalWait = 0.0;
    private double busyTime = 0.0;
    private double lastUpdate;
    private final double startTime;

    public PriorityResource(Environment env, int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.env = env;
        this.capacity = capacity;
        this.startTime = env.now();
        this.lastUpdate = startTime;
    }

    Environment env() {
        return env;
    }

    public int capacity() {
        return capacity;
    }

    public int count() {
        return users.size();
    }

    public int waitingCount() {
        return waiters.size();
    }

    public long grantedCount() {
        return granted.get();
    }

    public double totalWaitTime() {
        return totalWait;
    }

    public double averageWaitTime() {
        long count = granted.get();
        return count == 0 ? 0.0 : totalWait / count;
    }

    public double utilization() {
        double elapsed = env.now() - startTime;
        if (elapsed <= 0) return 0.0;
        return Math.min(1.0, busyTime / (capacity * elapsed));
    }

    public PriorityRequest request(int priority) {
        return new PriorityRequest(this, priority, order.getAndIncrement());
    }

    public PriorityRequest request(int priority, double timeout) {
        PriorityRequest req = request(priority);
        if (timeout > 0) {
            env.timeout(timeout).addCallback(ev -> {
                if (!req.asEvent().triggered()) {
                    cancelRequest(req);
                    req.asEvent().fail(new RequestTimeout("PriorityRequest timeout"));
                    env.schedule(req.asEvent(), Event.NORMAL, 0);
                }
            });
        }
        return req;
    }

    public PriorityRelease release(PriorityRequest req) {
        return new PriorityRelease(this, req);
    }

    void onRequest(PriorityRequest req) {
        updateBusyTime();
        if (users.size() < capacity) {
            grant(req);
        } else {
            waiters.add(req);
        }
    }

    void cancelRequest(PriorityRequest req) {
        waiters.remove(req);
    }

    void onRelease(PriorityRelease release) {
        updateBusyTime();
        PriorityRequest req = release.request;
        if (!users.remove(req)) {
            release.asEvent().fail(new IllegalArgumentException("Request not using this resource"));
            return;
        }
        release.asEvent().succeed(null);
        grantAvailable();
    }

    private void grantAvailable() {
        while (users.size() < capacity && !waiters.isEmpty()) {
            PriorityRequest next = waiters.poll();
            if (next == null) break;
            if (next.asEvent().triggered()) {
                continue;
            }
            grant(next);
        }
    }

    private void grant(PriorityRequest req) {
        totalWait += Math.max(0.0, env.now() - req.createdTime());
        users.add(req);
        granted.incrementAndGet();
        req.asEvent().succeed(null);
    }

    private void updateBusyTime() {
        double now = env.now();
        busyTime += (now - lastUpdate) * users.size();
        lastUpdate = now;
    }
}
