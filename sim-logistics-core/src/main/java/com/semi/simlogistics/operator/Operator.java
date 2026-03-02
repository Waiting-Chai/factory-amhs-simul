package com.semi.simlogistics.operator;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.entity.LogisticsEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Human operator for manual tasks.
 * <p>
 * Operators have skills and work shifts.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Operator extends LogisticsEntity {

    private final Set<String> skills;
    private boolean onShift;
    private String currentStationId; // ID of station where operator is working

    public Operator(String id, String name, Position position) {
        super(id, name, EntityType.OPERATOR, position);
        this.skills = new HashSet<>();
        this.onShift = false;
        this.currentStationId = null;
    }

    /**
     * Add a skill to this operator.
     *
     * @param skill skill name
     */
    public void addSkill(String skill) {
        skills.add(skill);
    }

    /**
     * Remove a skill from this operator.
     *
     * @param skill skill name
     * @return true if skill was removed
     */
    public boolean removeSkill(String skill) {
        return skills.remove(skill);
    }

    /**
     * Check if operator has a specific skill.
     *
     * @param skill skill name
     * @return true if operator has the skill
     */
    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }

    /**
     * Get all skills of this operator.
     *
     * @return set of skills (unmodifiable)
     */
    public Set<String> getSkills() {
        return new HashSet<>(skills);
    }

    /**
     * Check if operator is currently on shift.
     *
     * @return true if on shift
     */
    public boolean isOnShift() {
        return onShift;
    }

    /**
     * Set operator shift status.
     *
     * @param onShift true if starting shift, false if ending shift
     */
    public void setOnShift(boolean onShift) {
        this.onShift = onShift;
    }

    /**
     * Get current station where operator is working.
     *
     * @return station ID, or null if not at any station
     */
    public String getCurrentStationId() {
        return currentStationId;
    }

    /**
     * Assign operator to a station.
     *
     * @param stationId station ID
     */
    public void assignToStation(String stationId) {
        this.currentStationId = stationId;
    }

    /**
     * Remove operator from current station.
     */
    public void leaveStation() {
        this.currentStationId = null;
    }
}
