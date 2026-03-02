package com.semi.simlogistics.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Cargo class.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
class CargoTest {

    @Test
    void testCargoCreation() {
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);

        assertThat(cargo.id()).isEqualTo("CARGO-001");
        assertThat(cargo.type()).isEqualTo(Cargo.CargoType.FOUP);
        assertThat(cargo.weight()).isEqualTo(250.0);
    }

    @Test
    void testCargoIdCannotBeNull() {
        assertThatThrownBy(() -> new Cargo(null, Cargo.CargoType.FOUP, 250.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Cargo id cannot be null");
    }

    @Test
    void testCargoTypeCannotBeNull() {
        assertThatThrownBy(() -> new Cargo("CARGO-001", null, 250.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Cargo type cannot be null");
    }

    @Test
    void testCargoEquals() {
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-001", Cargo.CargoType.CASSETTE, 200.0);
        Cargo cargo3 = new Cargo("CARGO-002", Cargo.CargoType.FOUP, 250.0);

        assertThat(cargo1).isEqualTo(cargo2); // Same ID
        assertThat(cargo1).isNotEqualTo(cargo3); // Different ID
    }

    @Test
    void testCargoHashCode() {
        Cargo cargo1 = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.0);
        Cargo cargo2 = new Cargo("CARGO-001", Cargo.CargoType.CASSETTE, 200.0);

        assertThat(cargo1.hashCode()).isEqualTo(cargo2.hashCode());
    }

    @Test
    void testCargoToString() {
        Cargo cargo = new Cargo("CARGO-001", Cargo.CargoType.FOUP, 250.5);

        String str = cargo.toString();

        assertThat(str).contains("CARGO-001");
        assertThat(str).contains("FOUP");
        assertThat(str).contains("250.50");
    }
}
