package com.semi.simlogistics.scheduler.engine;

/**
 * Provider for simulation time (REQ-DS-006).
 * <p>
 * Abstraction for obtaining the current simulation time, allowing
 * deterministic time control for replay and testing.
 * <p>
 * Default implementation uses wall-clock time, but production
 * environments should inject a provider that returns env.now().
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@FunctionalInterface
public interface SimulationTimeProvider {

    /**
     * Get the current simulation time in seconds.
     * <p>
     * This value is passed to replanning strategies and should represent
     * the simulation clock (env.now()), not wall-clock time.
     *
     * @return current simulation time in seconds as double
     */
    double getCurrentSimulationTimeSeconds();

    /**
     * Default implementation using wall-clock time.
     * <p>
     * NOTE: This is provided for compatibility only. Production environments
     * should use a proper simulation time source (env.now()).
     */
    SimulationTimeProvider DEFAULT = () -> System.currentTimeMillis() / 1000.0;
}
