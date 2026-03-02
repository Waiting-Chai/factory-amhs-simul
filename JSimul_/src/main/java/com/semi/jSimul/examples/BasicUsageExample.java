package com.semi.jSimul.examples;

import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;
import com.semi.jSimul.core.Timeout;

import java.util.logging.Logger;

/**
 * Smallest runnable example: two processes printing timestamps while waiting on timeouts.
 *
 * @author waiting
 * @date 2025/12/02
 */
public final class BasicUsageExample {

    private static final Logger LOG = Logger.getLogger(BasicUsageExample.class.getName());

    public static void main(String[] args) {
        Environment env = new Environment();

        Process p1 = env.process(ctx -> {
            LOG.info("p1 start @ " + ctx.env().now());
            ctx.await(new Timeout(ctx.env(), 2, null));
            LOG.info("p1 after 2s @ " + ctx.env().now());
            return "p1 done";
        });

        Process p2 = env.process(ctx -> {
            LOG.info("p2 start @ " + ctx.env().now());
            ctx.await(new Timeout(ctx.env(), 1, null));
            LOG.info("p2 after 1s @ " + ctx.env().now());
            ctx.await(new Timeout(ctx.env(), 1.5, null));
            LOG.info("p2 complete @ " + ctx.env().now());
            return "p2 done";
        });

        // Wait for both processes to finish to avoid exiting early while tasks are still running.
        env.run(env.allOf(p1, p2));
        LOG.info("Simulation finished at time=" + env.now());
    }
}
