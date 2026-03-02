package com.semi.simlogistics.web.dto;

/**
 * Path point definition.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class PathPointDTO {

    private String id;
    private PositionDTO position;

    public PathPointDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public void setPosition(PositionDTO position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "PathPointDTO{" +
                "id='" + id + '\'' +
                ", position=" + position +
                '}';
    }
}
