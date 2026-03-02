package com.semi.simlogistics.scheduler.test;

import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.scheduler.rule.DispatchContext;
import com.semi.simlogistics.scheduler.rule.DispatchRule;
import com.semi.simlogistics.scheduler.rule.TaskSelectionRule;
import com.semi.simlogistics.scheduler.task.Task;
import com.semi.simlogistics.scheduler.task.TaskPriority;
import com.semi.simlogistics.scheduler.task.TaskType;
import com.semi.simlogistics.vehicle.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Test implementations of dispatch rules for testing.
 * These are simple deterministic implementations for unit testing.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class TestDispatchRules {

    /**
     * Test dispatch rule that selects the first vehicle in the list.
     * Returns empty if list is null or empty.
     */
    public static class FirstVehicleRule implements DispatchRule {

        @Override
        public Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context) {
            if (candidates == null || candidates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(candidates.getFirst());
        }
    }

    /**
     * Test dispatch rule that selects a vehicle by ID.
     * Returns empty if no vehicle with the given ID is found.
     */
    public static class VehicleByIdRule implements DispatchRule {

        private final String targetVehicleId;

        public VehicleByIdRule(String targetVehicleId) {
            this.targetVehicleId = targetVehicleId;
        }

        @Override
        public Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context) {
            if (candidates == null) {
                return Optional.empty();
            }
            return candidates.stream()
                    .filter(v -> v.id().equals(targetVehicleId))
                    .findFirst();
        }
    }

    /**
     * Test dispatch rule that never selects a vehicle.
     */
    public static class NoVehicleRule implements DispatchRule {

        @Override
        public Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context) {
            return Optional.empty();
        }
    }

    /**
     * Test task selection rule that selects the first task in the list.
     */
    public static class FirstTaskRule implements TaskSelectionRule {

        @Override
        public Optional<Task> selectTask(List<Task> tasks, Vehicle vehicle, DispatchContext context) {
            if (tasks == null || tasks.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(tasks.getFirst());
        }
    }

    /**
     * Test task selection rule that selects by task ID.
     */
    public static class TaskByIdRule implements TaskSelectionRule {

        private final String targetTaskId;

        public TaskByIdRule(String targetTaskId) {
            this.targetTaskId = targetTaskId;
        }

        @Override
        public Optional<Task> selectTask(List<Task> tasks, Vehicle vehicle, DispatchContext context) {
            if (tasks == null) {
                return Optional.empty();
            }
            return tasks.stream()
                    .filter(t -> t.getId().equals(targetTaskId))
                    .findFirst();
        }
    }

    /**
     * Test task selection rule that selects by priority (highest first).
     */
    public static class HighestPriorityTaskRule implements TaskSelectionRule {

        @Override
        public Optional<Task> selectTask(List<Task> tasks, Vehicle vehicle, DispatchContext context) {
            if (tasks == null || tasks.isEmpty()) {
                return Optional.empty();
            }
            return tasks.stream()
                    .max((a, b) -> Integer.compare(
                            b.getPriority().getLevel(),
                            a.getPriority().getLevel()
                    ));
        }
    }

    /**
     * Simple test dispatch context.
     */
    public static class SimpleDispatchContext implements DispatchContext {

        @Override
        public double getDistance(Position from, Position to) {
            double dx = from.x() - to.x();
            double dy = from.y() - to.y();
            double dz = from.z() - to.z();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        @Override
        public double getUtilization(String vehicleId) {
            return 0.0;
        }

        @Override
        public long getCurrentTime() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Test notification port that does nothing.
     */
    public static class NoOpNotificationPort implements com.semi.simlogistics.scheduler.port.DispatchNotificationPort {

        @Override
        public void notifyTaskAssigned(Task task, Vehicle vehicle) {
            // Do nothing
        }
    }

    /**
     * Test notification port that throws exception for specific tasks.
     */
    public static class FailingNotificationPort implements com.semi.simlogistics.scheduler.port.DispatchNotificationPort {

        private final String failingTaskId;

        public FailingNotificationPort(String failingTaskId) {
            this.failingTaskId = failingTaskId;
        }

        @Override
        public void notifyTaskAssigned(Task task, Vehicle vehicle) {
            if (task.getId().equals(failingTaskId)) {
                throw new RuntimeException("Test failure for task: " + failingTaskId);
            }
        }
    }

    /**
     * Test dispatch rule that captures the DispatchContext time for verification.
     * This is used to verify that the override simulation time is actually used.
     */
    public static class TimeCapturingRule implements DispatchRule {

        private Long capturedTimeMillis = null;

        @Override
        public Optional<Vehicle> selectVehicle(Task task, List<Vehicle> candidates, DispatchContext context) {
            // Capture the current time from DispatchContext for verification
            capturedTimeMillis = context.getCurrentTime();
            // Delegate to first vehicle selection for actual dispatch
            if (candidates == null || candidates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(candidates.getFirst());
        }

        /**
         * Get the captured time in milliseconds from the last dispatch cycle.
         *
         * @return captured time in milliseconds, or null if no dispatch occurred
         */
        public Long getCapturedTimeMillis() {
            return capturedTimeMillis;
        }

        /**
         * Reset the captured time.
         */
        public void reset() {
            capturedTimeMillis = null;
        }
    }
}
