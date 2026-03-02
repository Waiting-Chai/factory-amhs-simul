package com.semi.jSimul.core;

/**
 * Base interface for environments, mirroring SimPy's BaseEnvironment.
 *
 * @author waiting
 * @date 2025/10/29
 */
public interface BaseEnvironment {

    double now();

    Process activeProcess();

    void schedule(Event event, int priority, double delay);

    void step();

    Object run(Object until);

}