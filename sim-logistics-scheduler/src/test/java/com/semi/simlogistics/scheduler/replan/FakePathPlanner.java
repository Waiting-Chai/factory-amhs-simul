package com.semi.simlogistics.scheduler.replan;

import com.semi.simlogistics.control.path.Path;
import com.semi.simlogistics.control.path.PathPlanner;
import com.semi.simlogistics.core.TransportType;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake PathPlanner for testing (replaces Mockito).
 * <p>
 * Provides deterministic path planning behavior for tests without
 * requiring Mockito or other mocking frameworks.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class FakePathPlanner implements PathPlanner {

    private final Map<String, Path> predefinedPaths;
    private Path defaultPath;
    private int callCount;

    public FakePathPlanner() {
        this.predefinedPaths = new HashMap<>();
        this.defaultPath = Path.empty();
    }

    /**
     * Set a predefined path for a specific route.
     *
     * @param startNodeId start node
     * @param endNodeId end node
     * @param path path to return
     */
    public void setPath(String startNodeId, String endNodeId, Path path) {
        String key = startNodeId + "->" + endNodeId;
        predefinedPaths.put(key, path);
    }

    /**
     * Set the default path to return when no predefined path exists.
     *
     * @param path default path
     */
    public void setDefaultPath(Path path) {
        this.defaultPath = path;
    }

    /**
     * Get the number of times planPath was called.
     *
     * @return call count
     */
    public int getCallCount() {
        return callCount;
    }

    /**
     * Reset the call counter.
     */
    public void resetCallCount() {
        callCount = 0;
    }

    @Override
    public Path planPath(String startNodeId, String endNodeId, TransportType transportType) {
        callCount++;

        // Check predefined paths first
        String key = startNodeId + "->" + endNodeId;
        Path path = predefinedPaths.get(key);
        if (path != null) {
            return path;
        }

        // Return default path
        return defaultPath;
    }

    @Override
    public void onGraphChanged() {
        // No-op for fake
    }

    @Override
    public int getCacheSize() {
        return predefinedPaths.size();
    }

    /**
     * Create a fake path planner with simple linear paths.
     * <p>
     * For routes P1->P5, returns P1->P2->P3->P5.
     *
     * @return configured fake path planner
     */
    public static FakePathPlanner withLinearPaths() {
        FakePathPlanner planner = new FakePathPlanner();
        planner.setPath("P1", "P5", Path.of("P1", "P2", "P3", "P5"));
        planner.setPath("P1", "P7", Path.of("P1", "P4", "P7"));
        planner.setPath("P1", "STANDBY", Path.of("P1", "P2", "STANDBY"));
        planner.setPath("P2", "P5", Path.of("P2", "P7", "P9", "P5"));
        return planner;
    }

    /**
     * Create a fake path planner that always returns empty paths (no route).
     *
     * @return fake path planner with no routes
     */
    public static FakePathPlanner withNoRoutes() {
        FakePathPlanner planner = new FakePathPlanner();
        planner.setDefaultPath(Path.empty());
        return planner;
    }
}
