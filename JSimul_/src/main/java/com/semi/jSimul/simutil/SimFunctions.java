package com.semi.jSimul.simutil;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility functions for JSimul, providing helpers similar to SimPy's top-level functions.
 */
public final class SimFunctions {

    private SimFunctions() {
        throw new AssertionError("No instances");
    }

    /**
     * Start a process after a certain delay.
     * Equivalent to simpy.util.start_delayed(env, generator, delay).
     *
     * @param env       the environment
     * @param generator the process generator function
     * @param delay     the delay
     * @return the created process
     */
    public static Process startDelayed(Environment env, Process.ProcessFunction generator, double delay) {
        return env.process(ctx -> {
            ctx.await(env.timeout(delay));
            return generator.run(ctx);
        });
    }

    /**
     * Run a task repeatedly at a fixed interval.
     * The task is executed immediately (at current time), then waited for interval, and so on.
     *
     * @param env      the environment
     * @param interval the time interval between executions
     * @param task     the task to run
     * @return the process driving the loop
     */
    public static Process loop(Environment env, double interval, Runnable task) {
        return env.process(ctx -> {
            while (true) {
                task.run();
                ctx.await(env.timeout(interval));
            }
        });
    }

    /**
     * Periodically monitor a value and record it.
     * Useful for collecting statistics like queue length, resource usage, etc.
     *
     * @param env      the environment
     * @param interval the polling interval
     * @param probe    function to retrieve the value
     * @param recorder function to consume/record the value
     * @param <T>      the type of the value
     * @return the monitoring process
     */
    public static <T> Process monitor(Environment env, double interval, Supplier<T> probe, Consumer<T> recorder) {
        return loop(env, interval, () -> recorder.accept(probe.get()));
    }

    /**
     * Wait until a predicate returns true by polling at a fixed interval.
     * This is less efficient than event-based waiting but useful for conditions
     * that do not trigger events.
     *
     * @param env             the environment
     * @param predicate       the condition to check
     * @param pollingInterval the interval to check the condition
     * @return the process that waits
     */
    public static Process waitFor(Environment env, Supplier<Boolean> predicate, double pollingInterval) {
        return env.process(ctx -> {
            while (!predicate.get()) {
                ctx.await(env.timeout(pollingInterval));
            }
            return null;
        });
    }
}
