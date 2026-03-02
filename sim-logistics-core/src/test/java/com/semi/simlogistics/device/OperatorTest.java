package com.semi.simlogistics.device;

import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import com.semi.simlogistics.operator.Operator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Operator class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class OperatorTest {

    @Test
    void testOperatorCreation() {
        Position pos = new Position(10.0, 20.0);

        Operator operator = new Operator("OPERATOR-001", "John Doe", pos);

        assertThat(operator.id()).isEqualTo("OPERATOR-001");
        assertThat(operator.name()).isEqualTo("John Doe");
        assertThat(operator.type()).isEqualTo(EntityType.OPERATOR);
        assertThat(operator.position()).isEqualTo(pos);
        assertThat(operator.getSkills()).isEmpty();
        assertThat(operator.isOnShift()).isFalse();
        assertThat(operator.getCurrentStationId()).isNull();
    }

    @Test
    void testAddSkill() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        operator.addSkill("welding");
        operator.addSkill("assembly");

        assertThat(operator.hasSkill("welding")).isTrue();
        assertThat(operator.hasSkill("assembly")).isTrue();
        assertThat(operator.hasSkill("painting")).isFalse();
    }

    @Test
    void testRemoveSkill() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        operator.addSkill("welding");
        operator.addSkill("assembly");

        boolean removed = operator.removeSkill("welding");

        assertThat(removed).isTrue();
        assertThat(operator.hasSkill("welding")).isFalse();
        assertThat(operator.hasSkill("assembly")).isTrue();
    }

    @Test
    void testRemoveNonExistentSkill() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        boolean removed = operator.removeSkill("welding");

        assertThat(removed).isFalse();
    }

    @Test
    void testGetSkillsReturnsUnmodifiableCopy() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        operator.addSkill("welding");

        Set<String> skills1 = operator.getSkills();
        Set<String> skills2 = operator.getSkills();

        assertThat(skills1).isNotSameAs(skills2); // Different instances
        assertThat(skills1).contains("welding");

        // Modifying returned set should not affect operator
        try {
            skills1.add("painting");
            // If set was modifiable, it would still not affect the operator
            // because we return a copy
        } catch (UnsupportedOperationException e) {
            // If unmodifiable, that's also fine
        }

        assertThat(operator.hasSkill("painting")).isFalse();
    }

    @Test
    void testShiftManagement() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        assertThat(operator.isOnShift()).isFalse();

        operator.setOnShift(true);
        assertThat(operator.isOnShift()).isTrue();

        operator.setOnShift(false);
        assertThat(operator.isOnShift()).isFalse();
    }

    @Test
    void testStationAssignment() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        assertThat(operator.getCurrentStationId()).isNull();

        operator.assignToStation("STATION-001");
        assertThat(operator.getCurrentStationId()).isEqualTo("STATION-001");

        operator.leaveStation();
        assertThat(operator.getCurrentStationId()).isNull();
    }

    @Test
    void testReassignStation() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        operator.assignToStation("STATION-001");
        operator.assignToStation("STATION-002");

        assertThat(operator.getCurrentStationId()).isEqualTo("STATION-002");
    }

    @Test
    void testMultipleSkills() {
        Position pos = new Position(0.0, 0.0);
        Operator operator = new Operator("OPERATOR-001", "Test", pos);

        operator.addSkill("welding");
        operator.addSkill("assembly");
        operator.addSkill("painting");
        operator.addSkill("quality-check");

        assertThat(operator.getSkills()).hasSize(4);
        assertThat(operator.hasSkill("welding")).isTrue();
        assertThat(operator.hasSkill("assembly")).isTrue();
        assertThat(operator.hasSkill("painting")).isTrue();
        assertThat(operator.hasSkill("quality-check")).isTrue();
    }
}
