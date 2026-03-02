package com.semi.jSimul.collections;

import com.semi.jSimul.core.Environment;

/**
 * Container for continuous or discrete matter up to capacity, supporting put/get of quantities.
 *
 * @author waiting
 * @date 2025/10/29
 */
public class Container {

    private final BaseResource<PutEvent, GetEvent> core;

    private double level;

    public Container(Environment env, double capacity, double initial) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        if (initial < 0 || initial > capacity) throw new IllegalArgumentException("invalid initial");
        this.level = initial;
        this.core = new BaseResource<>(
                env,
                (int) Math.ceil(capacity),
                (event, res) -> {
                    if (level + event.amount <= capacity) {
                        level += event.amount;
                        event.asEvent().succeed(null);
                    }
                    return true;
                },
                (event, res) -> {
                    if (level >= event.amount) {
                        level -= event.amount;
                        event.asEvent().succeed(event.amount);
                    }
                    return true;
                }
        );
    }

    public double level() {
        return level;
    }

    public PutEvent put(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("put amount must be > 0");
        }
        return new PutEvent(core, amount);
    }

    public GetEvent get(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("get amount must be > 0");
        }
        return new GetEvent(core, amount);
    }

}
