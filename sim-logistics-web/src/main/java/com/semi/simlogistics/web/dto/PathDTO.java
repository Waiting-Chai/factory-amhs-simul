package com.semi.simlogistics.web.dto;

import java.util.List;

/**
 * Path definition in scene.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class PathDTO {

    private String id;
    private String type; // OHT_TRACK or AGV_NETWORK
    private String name;
    private List<PathPointDTO> points;
    private List<PathSegmentDTO> segments;
    private List<ControlPointDTO> controlPoints;

    public PathDTO() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PathPointDTO> getPoints() {
        return points;
    }

    public void setPoints(List<PathPointDTO> points) {
        this.points = points;
    }

    public List<PathSegmentDTO> getSegments() {
        return segments;
    }

    public void setSegments(List<PathSegmentDTO> segments) {
        this.segments = segments;
    }

    public List<ControlPointDTO> getControlPoints() {
        return controlPoints;
    }

    public void setControlPoints(List<ControlPointDTO> controlPoints) {
        this.controlPoints = controlPoints;
    }

    @Override
    public String toString() {
        return "PathDTO{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
