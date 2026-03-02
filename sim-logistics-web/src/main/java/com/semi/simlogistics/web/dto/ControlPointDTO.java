package com.semi.simlogistics.web.dto;

/**
 * Control point on path.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ControlPointDTO {

    private String id;
    private String at;     // References point ID
    private int capacity;
    private Integer priority;

    public ControlPointDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAt() {
        return at;
    }

    public void setAt(String at) {
        this.at = at;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "ControlPointDTO{" +
                "id='" + id + '\'' +
                ", at='" + at + '\'' +
                ", capacity=" + capacity +
                ", priority=" + priority +
                '}';
    }
}
