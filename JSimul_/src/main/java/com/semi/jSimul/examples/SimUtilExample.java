package com.semi.jSimul.examples;

import com.semi.jSimul.collections.Resource;
import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;
import com.semi.jSimul.simutil.SimFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Comprehensive example demonstrating the usage of SimFunctions utilities.
 * <p>
 * This scenario simulates a server system where:
 * 1. A monitoring service collects server load statistics periodically.
 * 2. A background maintenance task runs at fixed intervals.
 * 3. A "delayed" task starts after the simulation has warmed up.
 * 4. A supervisor waits for a specific condition (high load) to trigger an alert.
 * </p>
 *
 * @author waiting
 * @date 2025/12/02
 */
public class SimUtilExample {

    private static final Logger LOG = Logger.getLogger(SimUtilExample.class.getName());

    public static void main(String[] args) {
        // 1. Initialize the environment
        Environment env = new Environment();
        
        // 2. Create a shared resource representing server capacity (e.g., 2 CPU cores)
        Resource server = new Resource(env, 2);
        
        // List to store monitoring data
        List<Integer> loadHistory = new ArrayList<>();

        LOG.info(">>> Simulation Started at t=" + env.now());

        // --- Usage 1: Monitor ---
        // Periodically record the number of users currently using the server.
        // This runs every 1.0 time unit.
        // LOG.info("Monitor: Current load = " + load);
        SimFunctions.monitor(env, 1.0, server::count, loadHistory::add);

        // --- Usage 2: Loop ---
        // Simulate a background maintenance task that runs every 5.0 time units.
        SimFunctions.loop(env, 5.0, () -> {
            LOG.info(String.format("[Maintenance] System check at t=%.1f", env.now()));
        });

        // --- Usage 3: Start Delayed ---
        // Start a heavy traffic generator after 3.0 time units of "warm-up".
        SimFunctions.startDelayed(env, ctx -> {
            LOG.info(String.format("[Delayed Task] Heavy traffic starting at t=%.1f", env.now()));
            
            // Simulate occupying the server for a while
            var req1 = server.request();
            ctx.await(req1); // Wait for resource
            LOG.info("[Delayed Task] Acquired server core 1");
            
            var req2 = server.request(); 
            ctx.await(req2); // Wait for second resource
            LOG.info("[Delayed Task] Acquired server core 2 (System Full)");
            
            // Hold resources for 4 units
            ctx.await(env.timeout(4.0));
            
            server.release(req2);
            server.release(req1);
            LOG.info("[Delayed Task] Released resources at t=" + env.now());
            return null;
        }, 3.0);

        // --- Usage 4: WaitFor ---
        // A supervisor process that waits until the server is fully utilized (load == 2).
        // It checks the condition every 0.5 time units.
        Process supervisor = SimFunctions.waitFor(env, () -> server.count() == 2, 0.5);
        
        // We wrap the supervisor logic to print when it detects the condition
        env.process(ctx -> {
            LOG.info("[Supervisor] Waiting for full load...");
            ctx.await(supervisor);
            LOG.info(String.format("[Supervisor] ALERT: System reached full capacity at t=%.1f!", env.now()));
            return null;
        });

        // Run the simulation for 15 time units
        env.run(15.0);

        LOG.info(">>> Simulation Finished at t=" + env.now());
        LOG.info("Load History (every 1.0s): " + loadHistory);
    }
}
