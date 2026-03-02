package com.semi.simlogistics.entity;

import com.semi.simlogistics.capability.Capability;
import com.semi.simlogistics.capability.TransportCapability;
import com.semi.simlogistics.core.EntityType;
import com.semi.simlogistics.core.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LogisticsEntity class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class LogisticsEntityTest {

    private static class TestEntity extends LogisticsEntity {
        public TestEntity(String id, String name, EntityType type, Position position) {
            super(id, name, type, position);
        }
    }

    @Test
    void testEntityCreation() {
        Position pos = new Position(10.0, 20.0, 30.0);
        TestEntity entity = new TestEntity("ENTITY-001", "Test Entity", EntityType.MACHINE, pos);

        assertThat(entity.id()).isEqualTo("ENTITY-001");
        assertThat(entity.name()).isEqualTo("Test Entity");
        assertThat(entity.type()).isEqualTo(EntityType.MACHINE);
        assertThat(entity.position()).isEqualTo(pos);
    }

    @Test
    void testEntityIdCannotBeNull() {
        Position pos = new Position(0.0, 0.0);

        assertThatThrownBy(() -> new TestEntity(null, "Test", EntityType.MACHINE, pos))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity id cannot be null");
    }

    @Test
    void testEntityNameCannotBeNull() {
        Position pos = new Position(0.0, 0.0);

        assertThatThrownBy(() -> new TestEntity("ID", null, EntityType.MACHINE, pos))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity name cannot be null");
    }

    @Test
    void testEntityTypeCannotBeNull() {
        Position pos = new Position(0.0, 0.0);

        assertThatThrownBy(() -> new TestEntity("ID", "Test", null, pos))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity type cannot be null");
    }

    @Test
    void testEntityPositionCannotBeNull() {
        assertThatThrownBy(() -> new TestEntity("ID", "Test", EntityType.MACHINE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Entity position cannot be null");
    }

    @Test
    void testSetPosition() {
        Position pos1 = new Position(10.0, 20.0);
        Position pos2 = new Position(30.0, 40.0);
        TestEntity entity = new TestEntity("ENTITY-001", "Test", EntityType.MACHINE, pos1);

        entity.setPosition(pos2);

        assertThat(entity.position()).isEqualTo(pos2);
    }

    @Test
    void testSetPositionCannotBeNull() {
        Position pos = new Position(0.0, 0.0);
        TestEntity entity = new TestEntity("ENTITY-001", "Test", EntityType.MACHINE, pos);

        assertThatThrownBy(() -> entity.setPosition(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Position cannot be null");
    }

    @Test
    void testCapabilityManagement() {
        Position pos = new Position(0.0, 0.0);
        TestEntity entity = new TestEntity("ENTITY-001", "Test", EntityType.MACHINE, pos);

        TransportCapability mockCapability = new TransportCapability() {
            @Override
            public void moveTo(com.semi.simlogistics.core.Position destination) {}

            @Override
            public void load(com.semi.simlogistics.core.Cargo cargo) {}

            @Override
            public void unload() {}

            @Override
            public boolean isLoaded() { return false; }

            @Override
            public com.semi.simlogistics.core.Cargo getCurrentCargo() { return null; }
        };

        entity.addCapability(TransportCapability.class, mockCapability);

        assertThat(entity.hasCapability(TransportCapability.class)).isTrue();
        assertThat(entity.getCapability(TransportCapability.class)).isSameAs(mockCapability);
    }

    @Test
    void testEntityEquals() {
        Position pos = new Position(0.0, 0.0);
        TestEntity entity1 = new TestEntity("ENTITY-001", "Name1", EntityType.MACHINE, pos);
        TestEntity entity2 = new TestEntity("ENTITY-001", "Name2", EntityType.STOCKER, pos);
        TestEntity entity3 = new TestEntity("ENTITY-002", "Name1", EntityType.MACHINE, pos);

        assertThat(entity1).isEqualTo(entity2); // Same ID
        assertThat(entity1).isNotEqualTo(entity3); // Different ID
    }

    @Test
    void testEntityHashCode() {
        Position pos = new Position(0.0, 0.0);
        TestEntity entity1 = new TestEntity("ENTITY-001", "Name1", EntityType.MACHINE, pos);
        TestEntity entity2 = new TestEntity("ENTITY-001", "Name2", EntityType.STOCKER, pos);

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    void testEntityToString() {
        Position pos = new Position(0.0, 0.0);
        TestEntity entity = new TestEntity("ENTITY-001", "Test", EntityType.MACHINE, pos);

        String str = entity.toString();

        assertThat(str).contains("MACHINE");
        assertThat(str).contains("ENTITY-001");
    }
}
