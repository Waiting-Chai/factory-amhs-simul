package com.semi.simlogistics.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Position class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class PositionTest {

    @Test
    void testPositionCreation() {
        Position pos = new Position(10.0, 20.0, 30.0);

        assertThat(pos.x()).isEqualTo(10.0);
        assertThat(pos.y()).isEqualTo(20.0);
        assertThat(pos.z()).isEqualTo(30.0);
    }

    @Test
    void testPositionCreationWithoutZ() {
        Position pos = new Position(10.0, 20.0);

        assertThat(pos.x()).isEqualTo(10.0);
        assertThat(pos.y()).isEqualTo(20.0);
        assertThat(pos.z()).isEqualTo(0.0);
    }

    @Test
    void testDistanceTo() {
        Position pos1 = new Position(0.0, 0.0, 0.0);
        Position pos2 = new Position(3.0, 4.0, 0.0);

        double distance = pos1.distanceTo(pos2);

        // 3-4-5 triangle
        assertThat(distance).isEqualTo(5.0, within(0.001));
    }

    @Test
    void testDistanceTo3D() {
        Position pos1 = new Position(0.0, 0.0, 0.0);
        Position pos2 = new Position(1.0, 2.0, 2.0);

        double distance = pos1.distanceTo(pos2);

        // sqrt(1 + 4 + 4) = sqrt(9) = 3
        assertThat(distance).isEqualTo(3.0, within(0.001));
    }

    @Test
    void testEquals() {
        Position pos1 = new Position(10.0, 20.0, 30.0);
        Position pos2 = new Position(10.0, 20.0, 30.0);
        Position pos3 = new Position(10.0, 20.0, 31.0);

        assertThat(pos1).isEqualTo(pos2);
        assertThat(pos1).isNotEqualTo(pos3);
    }

    @Test
    void testHashCode() {
        Position pos1 = new Position(10.0, 20.0, 30.0);
        Position pos2 = new Position(10.0, 20.0, 30.0);

        assertThat(pos1.hashCode()).isEqualTo(pos2.hashCode());
    }

    @Test
    void testToString() {
        Position pos = new Position(10.5, 20.3, 30.1);

        String str = pos.toString();

        assertThat(str).contains("10.50");
        assertThat(str).contains("20.30");
        assertThat(str).contains("30.10");
    }
}
