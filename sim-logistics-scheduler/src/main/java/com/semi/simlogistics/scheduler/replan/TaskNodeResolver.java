package com.semi.simlogistics.scheduler.replan;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.scheduler.task.Task;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolver for mapping Task positions to network node IDs (REQ-DS-006).
 * <p>
 * Used during replanning to determine which node a vehicle should go to
 * when starting a new task. Supports explicit position-to-node mapping
 * for deterministic testing.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public interface TaskNodeResolver {

    /**
     * Resolve the network node ID for a task's source position.
     *
     * @param task the task to resolve
     * @return node ID, or null if cannot be resolved
     * @throws NullPointerException if task is null
     */
    String resolveSourceNode(Task task);

    /**
     * Resolve the network node ID for a task's destination position.
     *
     * @param task the task to resolve
     * @return node ID, or null if cannot be resolved
     * @throws NullPointerException if task is null
     */
    String resolveDestinationNode(Task task);

    /**
     * Default implementation using position-based hashing.
     * <p>
     * Maps positions to node IDs using a simple hash-based approach.
     * For production use, should be replaced with a proper spatial index.
     */
    class DefaultTaskNodeResolver implements TaskNodeResolver {

        private final Map<String, String> explicitMappings;

        public DefaultTaskNodeResolver() {
            this.explicitMappings = new ConcurrentHashMap<>();
        }

        /**
         * Create a resolver with explicit position-to-node mappings.
         *
         * @param explicitMappings map of task ID to source node ID
         */
        public DefaultTaskNodeResolver(Map<String, String> explicitMappings) {
            this.explicitMappings = new ConcurrentHashMap<>(explicitMappings);
        }

        /**
         * Add an explicit mapping for a specific task.
         *
         * @param taskId task ID
         * @param nodeId node ID to map to
         */
        public void addMapping(String taskId, String nodeId) {
            explicitMappings.put(taskId, nodeId);
        }

        @Override
        public String resolveSourceNode(Task task) {
            Objects.requireNonNull(task, "Task cannot be null");

            // Check explicit mapping first
            String explicitNode = explicitMappings.get(task.getId());
            if (explicitNode != null) {
                return explicitNode;
            }

            // Fallback: generate node ID from position hash
            // In production, this should use a proper spatial index
            Position pos = task.getSource();
            return positionToNodeId(pos);
        }

        @Override
        public String resolveDestinationNode(Task task) {
            Objects.requireNonNull(task, "Task cannot be null");

            // For destination, use position-based mapping
            Position pos = task.getDestination();
            return positionToNodeId(pos);
        }

        /**
         * Convert position to node ID using simple hash.
         *
         * @param pos position
         * @return node ID string
         */
        private String positionToNodeId(Position pos) {
            // Round to nearest meter and create node ID
            int x = (int) Math.round(pos.x());
            int y = (int) Math.round(pos.y());
            int z = (int) Math.round(pos.z());
            return "NODE_" + x + "_" + y + "_" + z;
        }
    }

    /**
     * Test stub that returns pre-configured node IDs.
     */
    class TestStubTaskNodeResolver implements TaskNodeResolver {

        private final Map<String, String> sourceNodes;
        private final Map<String, String> destinationNodes;

        public TestStubTaskNodeResolver() {
            this.sourceNodes = new ConcurrentHashMap<>();
            this.destinationNodes = new ConcurrentHashMap<>();
        }

        public void mapSourceNode(String taskId, String nodeId) {
            sourceNodes.put(taskId, nodeId);
        }

        public void mapDestinationNode(String taskId, String nodeId) {
            destinationNodes.put(taskId, nodeId);
        }

        @Override
        public String resolveSourceNode(Task task) {
            return sourceNodes.get(task.getId());
        }

        @Override
        public String resolveDestinationNode(Task task) {
            return destinationNodes.get(task.getId());
        }
    }
}
