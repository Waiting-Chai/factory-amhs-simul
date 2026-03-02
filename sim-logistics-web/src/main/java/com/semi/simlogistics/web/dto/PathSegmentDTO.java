package com.semi.simlogistics.web.dto;

/**
 * Path segment definition.
 * Supports LINEAR and BEZIER segment types.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class PathSegmentDTO {

    private String id;
    private String type; // LINEAR or BEZIER
    private String from; // References point ID
    private String to;   // References point ID
    private PositionDTO c1; // Control point 1 for BEZIER (absolute coordinates, meters)
    private PositionDTO c2; // Control point 2 for BEZIER (absolute coordinates, meters)

    public PathSegmentDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public PositionDTO getC1() {
        return c1;
    }

    public void setC1(PositionDTO c1) {
        this.c1 = c1;
    }

    public PositionDTO getC2() {
        return c2;
    }

    public void setC2(PositionDTO c2) {
        this.c2 = c2;
    }

    @Override
    public String toString() {
        return "PathSegmentDTO{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                '}';
    }
}
