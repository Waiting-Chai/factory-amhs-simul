package com.semi.jSimul.core;

/**
 * Internal scheduled entry used by Environment's priority queue.
 *
 * @author waiting
 * @date 2025/10/29
 */
record Scheduled(double time, int priority, long id, Event event) implements Comparable<Scheduled> {

    @Override
    public int compareTo(Scheduled o) {
        int c = Double.compare(this.time, o.time);
        if (c != 0) return c;
        c = Integer.compare(this.priority, o.priority);
        if (c != 0) return c;
        return Long.compare(this.id, o.id);
    }

}