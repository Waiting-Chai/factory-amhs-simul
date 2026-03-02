package com.semi.simlogistics.core;

import java.util.Objects;

/**
 * Material cargo transported by vehicles in logistics simulation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-08
 */
public class Cargo {

    private final String id;
    private final CargoType type;
    private final double weight;

    public Cargo(String id, CargoType type, double weight) {
        this.id = Objects.requireNonNull(id, "Cargo id cannot be null");
        this.type = Objects.requireNonNull(type, "Cargo type cannot be null");
        this.weight = weight;
    }

    public String id() {
        return id;
    }

    public CargoType type() {
        return type;
    }

    public double weight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cargo cargo = (Cargo) o;
        return Objects.equals(id, cargo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Cargo(%s, %s, %.2fkg)", id, type, weight);
    }

    /**
     * Cargo type enumeration.
     */
    public enum CargoType {
        FOUP,    // Front Opening Unified Pod
        CASSETTE,
        BOX,
        PALLET
    }
}
