package com.semi.simlogistics.scheduler.metrics.port;

import com.semi.simlogistics.scheduler.metrics.model.MetricAggregate;
import com.semi.simlogistics.scheduler.metrics.model.MetricEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of MetricsRepositoryPort for testing.
 * <p>
 * This implementation stores all data in memory and is suitable for:
 * <ul>
 *   <li>Unit tests</li>
 *   <li>Development without database</li>
 *   <li>Fast prototyping</li>
 * </ul>
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class InMemoryMetricsRepository implements MetricsRepositoryPort {

    private final Map<String, List<MetricEvent>> eventsBySimulation = new ConcurrentHashMap<>();
    private final Map<String, List<MetricAggregate>> aggregatesBySimulation = new ConcurrentHashMap<>();
    private final Map<String, MetricEvent> eventsById = new ConcurrentHashMap<>();
    private final Map<String, MetricAggregate> aggregatesById = new ConcurrentHashMap<>();

    @Override
    public void saveEvent(MetricEvent event) {
        eventsBySimulation.computeIfAbsent(event.getSimulationId(), k -> new ArrayList<>()).add(event);
        eventsById.put(event.getId(), event);
    }

    @Override
    public void saveAggregate(MetricAggregate aggregate) {
        aggregatesBySimulation.computeIfAbsent(aggregate.getSimulationId(), k -> new ArrayList<>()).add(aggregate);
        aggregatesById.put(aggregate.getId(), aggregate);
    }

    @Override
    public void saveEventsBatch(List<MetricEvent> events) {
        for (MetricEvent event : events) {
            saveEvent(event);
        }
    }

    @Override
    public List<MetricAggregate> queryBySimulationIdAndTimeRange(
            String simulationId,
            double fromTime,
            double toTime
    ) {
        List<MetricAggregate> aggregates = aggregatesBySimulation.get(simulationId);
        if (aggregates == null) {
            return Collections.emptyList();
        }

        return aggregates.stream()
                .filter(a -> a.getRecordedAt() >= fromTime && a.getRecordedAt() <= toTime)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MetricAggregate> getLatestAggregate(String simulationId) {
        List<MetricAggregate> aggregates = aggregatesBySimulation.get(simulationId);
        if (aggregates == null || aggregates.isEmpty()) {
            return Optional.empty();
        }

        return aggregates.stream()
                .max(Comparator.comparingDouble(MetricAggregate::getRecordedAt));
    }

    @Override
    public List<MetricEvent> queryByEntityId(String entityId) {
        return eventsById.values().stream()
                .filter(e -> entityId.equals(e.getEntityId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MetricEvent> queryByEventType(String eventType) {
        return eventsById.values().stream()
                .filter(e -> eventType.equals(e.getEventType()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBySimulationId(String simulationId) {
        List<MetricEvent> events = eventsBySimulation.remove(simulationId);
        if (events != null) {
            for (MetricEvent event : events) {
                eventsById.remove(event.getId());
            }
        }

        List<MetricAggregate> aggregates = aggregatesBySimulation.remove(simulationId);
        if (aggregates != null) {
            for (MetricAggregate aggregate : aggregates) {
                aggregatesById.remove(aggregate.getId());
            }
        }
    }

    @Override
    public Optional<MetricsSummary> getSummary(String simulationId) {
        List<MetricAggregate> aggregates = aggregatesBySimulation.get(simulationId);
        if (aggregates == null || aggregates.isEmpty()) {
            return Optional.empty();
        }

        // Sum up all aggregates
        int totalTasks = 0;
        double tasksPerHour = 0.0;
        double materialThroughput = 0.0;
        double vehicleUtilization = 0.0;
        double equipmentUtilization = 0.0;
        int wipTotal = 0;
        double energyTotal = 0.0;
        int count = 0;

        for (MetricAggregate aggregate : aggregates) {
            totalTasks += aggregate.getTasksCompleted();
            tasksPerHour += aggregate.getTasksPerHour();
            materialThroughput += aggregate.getMaterialThroughput();
            vehicleUtilization += aggregate.getVehicleUtilization();
            equipmentUtilization += aggregate.getEquipmentUtilization();
            wipTotal = Math.max(wipTotal, aggregate.getWipTotal());  // Use max WIP
            energyTotal += aggregate.getEnergyTotal();
            count++;
        }

        // Average rate metrics
        if (count > 0) {
            tasksPerHour /= count;
            materialThroughput /= count;
            vehicleUtilization /= count;
            equipmentUtilization /= count;
        }

        return Optional.of(new MetricsSummary(
                simulationId,
                totalTasks,
                tasksPerHour,
                materialThroughput,
                vehicleUtilization,
                equipmentUtilization,
                wipTotal,
                energyTotal
        ));
    }

    /**
     * Clear all stored data (useful for testing).
     */
    public void clear() {
        eventsBySimulation.clear();
        aggregatesBySimulation.clear();
        eventsById.clear();
        aggregatesById.clear();
    }

    /**
     * Get the number of events stored.
     */
    public int getEventCount() {
        return eventsById.size();
    }

    /**
     * Get the number of aggregates stored.
     */
    public int getAggregateCount() {
        return aggregatesById.size();
    }

    /**
     * Get all events for a simulation (for testing).
     */
    public List<MetricEvent> getEventsBySimulationId(String simulationId) {
        List<MetricEvent> events = eventsBySimulation.get(simulationId);
        return events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    /**
     * Get all aggregates for a simulation (for testing).
     */
    public List<MetricAggregate> getAggregatesBySimulationId(String simulationId) {
        List<MetricAggregate> aggregates = aggregatesBySimulation.get(simulationId);
        return aggregates != null ? new ArrayList<>(aggregates) : new ArrayList<>();
    }
}
