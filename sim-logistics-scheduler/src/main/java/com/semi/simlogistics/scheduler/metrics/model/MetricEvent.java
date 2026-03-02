package com.semi.simlogistics.scheduler.metrics.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Metric event data model (实时采样事件).
 * <p>
 * Represents a single metric event captured during simulation.
 * Uses dual time基准:
 * <ul>
 *   <li>simulatedTime: 仿真时间（秒）</li>
 *   <li>wallClockTime: 墙钟时间</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class MetricEvent {

    private final String id;
    private final String simulationId;
    private final String eventType;
    private final double simulatedTime;  // 仿真时间（秒）
    private final LocalDateTime wallClockTime;  // 墙钟时间
    private final String entityId;
    private final String entityType;
    private final MetricData data;

    private MetricEvent(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.simulationId = builder.simulationId;
        this.eventType = builder.eventType;
        this.simulatedTime = builder.simulatedTime;
        this.wallClockTime = builder.wallClockTime != null
                ? builder.wallClockTime
                : LocalDateTime.now();
        this.entityId = builder.entityId;
        this.entityType = builder.entityType;
        this.data = builder.data;
    }

    public String getId() {
        return id;
    }

    public String getSimulationId() {
        return simulationId;
    }

    public String getEventType() {
        return eventType;
    }

    public double getSimulatedTime() {
        return simulatedTime;
    }

    public LocalDateTime getWallClockTime() {
        return wallClockTime;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public MetricData getData() {
        return data;
    }

    /**
     * Common event types.
     */
    public static class EventTypes {
        public static final String TASK_COMPLETED = "task.completed";
        public static final String TASK_FAILED = "task.failed";
        public static final String TASK_ASSIGNED = "task.assigned";
        public static final String TASK_STARTED = "task.started";
        public static final String VEHICLE_STATE_CHANGED = "vehicle.state_changed";
        public static final String VEHICLE_MOVED = "vehicle.moved";
        public static final String CONFLICT_DETECTED = "conflict.detected";
        public static final String CONFLICT_RESOLVED = "conflict.resolved";
        public static final String WIP_CHANGED = "wip.changed";
        public static final String ENERGY_CONSUMED = "energy.consumed";
        public static final String BOTTLENECK_DETECTED = "bottleneck.detected";
    }

    /**
     * Builder for MetricEvent.
     */
    public static class Builder {
        private String id;
        private String simulationId;
        private String eventType;
        private double simulatedTime;
        private LocalDateTime wallClockTime;
        private String entityId;
        private String entityType;
        private MetricData data;

        public Builder(String simulationId, String eventType, double simulatedTime) {
            this.simulationId = simulationId;
            this.eventType = eventType;
            this.simulatedTime = simulatedTime;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder wallClockTime(LocalDateTime wallClockTime) {
            this.wallClockTime = wallClockTime;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder data(MetricData data) {
            this.data = data;
            return this;
        }

        public MetricEvent build() {
            return new MetricEvent(this);
        }
    }
}
